package com.malinskiy.marathon.execution

import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.execution.strategy.PoolingStrategy
import com.malinskiy.marathon.test.Test
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.sendBlocking
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Phaser

class DynamicPoolFactory(deviceProvider: DeviceProvider,
                         private val poolingStrategy: PoolingStrategy,
                         private val configuration: Configuration,
                         private val list: Collection<Test>) {

    private val logger = KotlinLogging.logger("DynamicPoolFactory")

    private val channel = deviceProvider.subscribe()

    private val actors = mutableMapOf<String, SendChannel<PoolMessage>>()

    fun execute(testTestTest: Phaser) {
        launch {
            for(msg in channel){
                when (msg) {
                    is DeviceProvider.DeviceEvent.DeviceConnected -> {
                        logger.warn { "DeviceConnected ${msg.device.serialNumber}" }
                        onDeviceConnected(msg, testTestTest)
                    }
                    is DeviceProvider.DeviceEvent.DeviceDisconnected -> {
                        logger.warn { "DeviceDisconnected ${msg.device.serialNumber}" }
                        onDeviceDisconnected(msg, testTestTest)
                    }
                }
            }
        }
    }

    fun terminate() {
        actors.values.forEach {
            runBlocking {
                it.send(PoolMessage.Terminate)
            }
        }
    }

    private fun onDeviceDisconnected(item: DeviceProvider.DeviceEvent.DeviceDisconnected, testTestTest: Phaser) {
        runBlocking {
            actors.values.forEach {
                it.send(PoolMessage.RemoveDevice(item.device,testTestTest))
            }
        }
    }

    private fun onDeviceConnected(item: DeviceProvider.DeviceEvent.DeviceConnected, testTestTest: Phaser) {
        val pools = poolingStrategy.createPools(listOf(item.device))
        pools.forEach {
            actors.computeIfAbsent(it.name, { _ -> PoolTestExecutor(configuration, list) })
            actors.computeIfPresent(it.name, { _, u ->
                runBlocking {
                    u.send(PoolMessage.AddDevice(item.device,testTestTest))
                }
                u
            })
        }
    }
}