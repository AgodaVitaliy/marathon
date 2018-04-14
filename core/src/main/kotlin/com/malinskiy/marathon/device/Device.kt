package com.malinskiy.marathon.device

import com.malinskiy.marathon.test.TestBatch

interface Device {
    val operatingSystem: OperatingSystem
    val serialNumber: String
    val model: String
    val manufacturer: String
    val networkState: NetworkState
    val deviceFeatures: Collection<DeviceFeature>
    val healthy: Boolean

    fun execute(testBatch: TestBatch)
}

