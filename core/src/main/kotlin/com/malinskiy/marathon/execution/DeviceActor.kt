package com.malinskiy.marathon.execution

import com.malinskiy.marathon.actor.Actor
import com.malinskiy.marathon.analytics.Analytics
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.DevicePoolMessage.FromDevice.*
import com.malinskiy.marathon.test.TestBatch
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.channels.SendChannel

class DeviceActor(private val devicePoolId: DevicePoolId,
                  private val pool: Actor<DevicePoolMessage>,
                  private val configuration: Configuration,
                  private val device: Device,
                  private val analytics: Analytics,
                  private val queueChannel: SendChannel<QueueMessage.FromDevice>) : Actor<DeviceMessage>() {

    private var status = DeviceStatus.CONNECTED

    override suspend fun receive(msg: DeviceMessage) {
        when (msg) {
            is DeviceMessage.Initialize -> initialize()
            is DeviceMessage.ExecuteTestBatch -> executeBatch(msg.batch)
            is DeviceMessage.Terminate -> terminate()
            is DeviceMessage.GetStatus -> msg.deferred.complete(status)
            is DeviceMessage.WakeUp -> wakeUp()
            is DeviceMessage.Wait -> status = DeviceStatus.WAITING
        }
    }

    private suspend fun wakeUp() {
        if (status == DeviceStatus.WAITING) {
            pool.send(RequestNextBatch(device, this))
        } else {
            println("WakeUp status $status")
        }
    }

    private suspend fun initialize() {
        device.prepare(configuration)
        status = DeviceStatus.WAITING
        pool.send(RequestNextBatch(device, this))
    }

    private suspend fun executeBatch(batch: TestBatch) {
        status = DeviceStatus.RUNNING
        device.execute(configuration, devicePoolId, batch, analytics, queueChannel)
        status = DeviceStatus.WAITING
        pool.send(RequestNextBatch(device, this))
    }

    private fun terminate() {
        close()
    }
}

sealed class DeviceMessage {
    data class ExecuteTestBatch(val batch: TestBatch) : DeviceMessage()
    data class GetStatus(val deferred: CompletableDeferred<DeviceStatus>) : DeviceMessage()
    object Initialize : DeviceMessage()
    object Terminate : DeviceMessage()
    object Wait : DeviceMessage()
    object WakeUp : DeviceMessage()
}
