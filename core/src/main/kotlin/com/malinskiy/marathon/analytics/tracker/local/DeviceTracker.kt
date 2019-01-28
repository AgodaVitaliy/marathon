package com.malinskiy.marathon.analytics.tracker.local

import com.malinskiy.marathon.analytics.tracker.NoOpTracker
import com.malinskiy.marathon.device.DeviceInfo
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.report.internal.DeviceInfoReporter

internal class DeviceTracker(private val deviceInfoSerializer: DeviceInfoReporter) : NoOpTracker() {
    override fun trackDeviceConnected(poolId: DevicePoolId, device: DeviceInfo) {
        deviceInfoSerializer.saveDeviceInfo(poolId, device)
    }
}
