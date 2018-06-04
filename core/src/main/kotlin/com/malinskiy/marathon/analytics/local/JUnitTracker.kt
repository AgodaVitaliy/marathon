package com.malinskiy.marathon.analytics.local

import com.malinskiy.marathon.analytics.NoOpTracker
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.report.junit.JUnitReporter

class JUnitTracker(private val jUnitReporter: JUnitReporter) : NoOpTracker() {
    override fun trackTestResult(poolId: DevicePoolId, device: Device, testResult: TestResult) {
        jUnitReporter.testFinished(poolId, device, testResult)
    }
}
