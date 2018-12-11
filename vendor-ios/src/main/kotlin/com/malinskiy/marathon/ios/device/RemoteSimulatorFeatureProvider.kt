package com.malinskiy.marathon.ios.device

import com.malinskiy.marathon.device.DeviceFeature
import com.malinskiy.marathon.exceptions.DeviceLostException
import com.malinskiy.marathon.ios.IOSDevice

object RemoteSimulatorFeatureProvider {
    fun deviceFeatures(device: IOSDevice): Collection<DeviceFeature> {
        return enumValues<DeviceFeature>().filter { featureIsAvailable(device, it) }
    }

    private fun featureIsAvailable(device: IOSDevice, feature: DeviceFeature): Boolean = when (feature) {
            DeviceFeature.SCREENSHOT -> true
            DeviceFeature.VIDEO -> {
                device.hostCommandExecutor.exec(
                        "/usr/sbin/system_profiler -detailLevel mini -xml SPDisplaysDataType"
                ).let {
                    it.exitStatus == 0
                            && it.stdout.contains("spdisplays_metalfeatureset")
            }
        }
    }

    fun availablePort(device: IOSDevice): Int {
        val commandResult = device.hostCommandExecutor.exec(
                """ruby -e 'require "socket"; puts Addrinfo.tcp("", 0).bind {|s| s.local_address.ip_port }'"""
        )
        return when {
            commandResult.exitStatus == 0 -> commandResult.stdout.trim().toIntOrNull()
            else -> null
        } ?: throw DeviceLostException(commandResult.stderr)
    }
}
