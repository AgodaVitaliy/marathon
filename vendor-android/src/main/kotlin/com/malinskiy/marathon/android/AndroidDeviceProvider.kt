package com.malinskiy.marathon.android

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.DdmPreferences
import com.android.ddmlib.IDevice
import com.android.ddmlib.TimeoutException
import com.malinskiy.marathon.actor.unboundedChannel
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.device.DeviceProvider.DeviceEvent.DeviceConnected
import com.malinskiy.marathon.device.DeviceProvider.DeviceEvent.DeviceDisconnected
import com.malinskiy.marathon.exceptions.NoDevicesException
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.vendor.VendorConfiguration
import kotlinx.coroutines.experimental.NonCancellable.isActive
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

private const val DEFAULT_DDM_LIB_TIMEOUT = 30000
private const val DEFAULT_DDM_LIB_SLEEP_TIME = 500

class AndroidDeviceProvider : DeviceProvider {

    private val logger = MarathonLogging.logger("AndroidDeviceProvider")

    private lateinit var adb: AndroidDebugBridge

    private val channel: Channel<DeviceProvider.DeviceEvent> = unboundedChannel()
    private val devices: ConcurrentMap<String, AndroidDevice> = ConcurrentHashMap()
    private val bootWaitContext = newFixedThreadPoolContext(4, "AndroidDeviceProvider-BootWait")

    override fun initialize(vendorConfiguration: VendorConfiguration) {
        if (vendorConfiguration !is AndroidConfiguration) {
            throw IllegalStateException("Invalid configuration $vendorConfiguration passed")
        }
        DdmPreferences.setTimeOut(DEFAULT_DDM_LIB_TIMEOUT)
        AndroidDebugBridge.initIfNeeded(false)

        val absolutePath = Paths.get(vendorConfiguration.androidSdk.absolutePath, "platform-tools", "adb").toFile().absolutePath

        val listener = object : AndroidDebugBridge.IDeviceChangeListener {
            override fun deviceChanged(device: IDevice?, changeMask: Int) {
                device?.let {
                    launch(context = bootWaitContext) {
                        val maybeNewAndroidDevice = AndroidDevice(it)
                        val healthy = maybeNewAndroidDevice.healthy

                        logger.debug { "Device ${device.serialNumber} changed state. Healthy = $healthy" }
                        if (healthy) {
                            verifyBooted(maybeNewAndroidDevice)
                            val androidDevice = getDeviceOrPut(maybeNewAndroidDevice)
                            notifyConnected(androidDevice)
                        } else {
                            //This shouldn't have any side effects even if device was previously removed
                            notifyDisconnected(maybeNewAndroidDevice)
                        }
                    }
                }
            }

            override fun deviceConnected(device: IDevice?) {
                device?.let {
                    launch(context = bootWaitContext) {
                        val maybeNewAndroidDevice = AndroidDevice(it)
                        val healthy = maybeNewAndroidDevice.healthy
                        logger.debug { "Device ${maybeNewAndroidDevice.serialNumber} connected channel.isFull = ${channel.isFull}. Healthy = $healthy" }

                        if (healthy) {
                            verifyBooted(maybeNewAndroidDevice)
                            val androidDevice = getDeviceOrPut(maybeNewAndroidDevice)
                            notifyConnected(androidDevice)
                        }
                    }
                }
            }

            override fun deviceDisconnected(device: IDevice?) {
                device?.let {
                    logger.debug { "Device ${device.serialNumber} disconnected" }
                    matchDdmsToDevice(it)?.let {
                        notifyDisconnected(it)
                    }
                }
            }

            private fun verifyBooted(device: AndroidDevice) {
                if (!waitForBoot(device)) throw TimeoutException("Timeout waiting for device ${device.serialNumber} to boot")
            }

            private fun waitForBoot(device: AndroidDevice): Boolean {
                var booted = false
                for (i in 1..30) {
                    if (device.booted) {
                        logger.debug { "Device ${device.serialNumber} booted!" }
                        booted = true
                        break
                    } else {
                        Thread.sleep(1000)
                        logger.debug { "Device ${device.serialNumber} is still booting..." }
                    }

                    if (Thread.interrupted() || !isActive) return true
                }

                return booted
            }

            private fun notifyConnected(device: AndroidDevice) {
                launch {
                    channel.send(DeviceConnected(device))
                }
            }

            private fun notifyDisconnected(device: AndroidDevice) {
                launch {
                    channel.send(DeviceDisconnected(device))
                }
            }
        }
        AndroidDebugBridge.addDeviceChangeListener(listener)
        adb = AndroidDebugBridge.createBridge(absolutePath, false)

        var getDevicesCountdown = DEFAULT_DDM_LIB_TIMEOUT
        val sleepTime = DEFAULT_DDM_LIB_SLEEP_TIME
        while (!adb.hasInitialDeviceList() || !adb.hasDevices() && getDevicesCountdown >= 0) {
            try {
                Thread.sleep(sleepTime.toLong())
            } catch (e: InterruptedException) {
                throw TimeoutException("Timeout getting device list", e)
            }
            getDevicesCountdown -= sleepTime
        }
        if (!adb.hasInitialDeviceList() || !adb.hasDevices()) {
            throw NoDevicesException("No devices found.")
        }
    }

    private fun getDeviceOrPut(androidDevice: AndroidDevice): AndroidDevice {
        return devices.getOrPut(androidDevice.serialNumber) {
            androidDevice
        }
    }

    private fun matchDdmsToDevice(device: IDevice): AndroidDevice? {
        val observedDevices = devices.values
        return observedDevices.findLast {
            device == it.ddmsDevice ||
                    device.serialNumber == it.ddmsDevice.serialNumber
        }
    }

    private fun AndroidDebugBridge.hasDevices(): Boolean = devices.isNotEmpty()

    override fun terminate() {
        AndroidDebugBridge.disconnectBridge()
        AndroidDebugBridge.terminate()
    }

    override fun subscribe() = channel

    override fun lockDevice(device: Device): Boolean {
        TODO("not implemented")
    }

    override fun unlockDevice(device: Device): Boolean {
        TODO("not implemented")
    }
}
