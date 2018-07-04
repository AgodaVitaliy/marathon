package com.malinskiy.marathon.device

import com.malinskiy.marathon.analytics.Analytics
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.TestRunResults
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.test.TestBatch
import kotlinx.coroutines.experimental.channels.Channel

interface Device {
    val operatingSystem: OperatingSystem
    val serialNumber: String
    val model: String
    val manufacturer: String
    val networkState: NetworkState
    val deviceFeatures: Collection<DeviceFeature>
    val healthy: Boolean
    val abi: String

    suspend fun execute(configuration: Configuration,
                        devicePoolId: DevicePoolId,
                        testBatch: TestBatch,
                        tracker: Analytics,
                        retryChannel: Channel<TestRunResults>,
                        progressReporter: ProgressReporter)

    suspend fun prepare(configuration: Configuration)
}

