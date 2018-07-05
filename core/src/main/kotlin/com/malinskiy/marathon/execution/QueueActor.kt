package com.malinskiy.marathon.execution

import com.malinskiy.marathon.actor.Actor
import com.malinskiy.marathon.analytics.metrics.MetricsProvider
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.DevicePoolMessage.FromQueue
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestBatch
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging
import java.util.Queue
import java.util.PriorityQueue

class QueueActor(configuration: Configuration,
                 private val testShard: TestShard,
                 metricsProvider: MetricsProvider,
                 private val pool: SendChannel<FromQueue>,
                 private val poolId: DevicePoolId,
                 private val retryChannel: Channel<RetryMessage>,
                 private val progressReporter: ProgressReporter) : Actor<QueueMessage>() {

    private val logger = KotlinLogging.logger("QueueActor[$poolId]")

    private val sorting = configuration.sortingStrategy

    private val queue: Queue<Test> = PriorityQueue<Test>(sorting.process(metricsProvider)).apply {
        addAll(testShard.tests + testShard.flakyTests)
    }
    private val batching = configuration.batchingStrategy
    private val retry = configuration.retryStrategy

    init {
        progressReporter.totalTests(poolId, queue.size)
        launch {
            for (msg in retryChannel) {
                when (msg) {
                    is RetryMessage.TestRunResults -> {
                        handleTestResults(msg.devicePoolId, msg.finished, msg.failed, msg.device)
                    }
                    is RetryMessage.ReturnTestBatch -> {
                        queue.addAll(msg.testBatch.tests)
                    }
                }
            }
        }
    }

    override suspend fun receive(msg: QueueMessage) {
        when (msg) {
            is QueueMessage.RequestNext -> {
                requestNextBatch(msg.device)
            }
            is QueueMessage.IsEmpty -> {
                msg.deferred.complete(queue.isEmpty())
            }
            is QueueMessage.Terminate -> {

            }
        }
    }


    private suspend fun handleTestResults(poolId: DevicePoolId,
                                          finished: Collection<Test>,
                                          failed: Collection<Test>,
                                          device: Device) {
        logger.debug { "handle test results ${device.serialNumber}" }
        if (finished.isNotEmpty()) {
            handleFinishedTests(finished)
        }
        if (failed.isNotEmpty()) {
            handleFailedTests(poolId, failed, device)
        }
    }

    private fun handleFinishedTests(finished: Collection<Test>) {
        finished.filter { testShard.flakyTests.contains(it) }.let {
            val size = queue.size
            queue.removeAll(it)
            progressReporter.removeTests(poolId, queue.size - size)
        }
    }

    private suspend fun handleFailedTests(poolId: DevicePoolId,
                                          failed: Collection<Test>,
                                          device: Device) {
        logger.debug { "handle failed tests ${device.serialNumber}" }
        val retryList = retry.process(poolId, failed, testShard)
        queue.addAll(retryList)
        if (retryList.isNotEmpty()) {
            pool.send(FromQueue.Notify)
        }
    }


    private suspend fun requestNextBatch(device: Device) {
        logger.debug { "request next batch for device ${device.serialNumber}" }
        val queueNotIsEmpty = queue.isNotEmpty()
        if (queueNotIsEmpty) {
            sendBatch(device)
        }
    }

    private suspend fun sendBatch(device: Device) {
        val batch = batching.process(queue)
        pool.send(FromQueue.ExecuteBatch(device, batch))
    }
}

sealed class RetryMessage {
    data class TestRunResults(val devicePoolId: DevicePoolId,
                              val finished: Collection<Test>,
                              val failed: Collection<Test>,
                              val device: Device) : RetryMessage()

    data class ReturnTestBatch(val devicePoolId: DevicePoolId,
                               val testBatch: TestBatch,
                               val device: Device) : RetryMessage()
}

sealed class QueueMessage {
    data class RequestNext(val device: Device) : QueueMessage()

    data class IsEmpty(val deferred: CompletableDeferred<Boolean>) : QueueMessage()

    object Terminate : QueueMessage()
}
