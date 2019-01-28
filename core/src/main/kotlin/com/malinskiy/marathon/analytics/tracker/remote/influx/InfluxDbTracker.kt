package com.malinskiy.marathon.analytics.tracker.remote.influx

import com.malinskiy.marathon.analytics.tracker.NoOpTracker
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.test.toSafeTestName
import org.influxdb.InfluxDB
import org.influxdb.dto.Point
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class InfluxDbTracker(private val influxDb: InfluxDB) : NoOpTracker() {

    private companion object {
        val runUUID= UUID.randomUUID().toString()
    }

    override fun trackRawTestRun(poolId: DevicePoolId, device: Device, testResult: TestResult) {
        influxDb.write(Point.measurement("tests")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .tag("testname", testResult.test.toSafeTestName())
                .tag("package", testResult.test.pkg)
                .tag("class", testResult.test.clazz)
                .tag("method", testResult.test.method)
                .tag("deviceSerial", device.serialNumber)
                .tag("run", InfluxDbTracker.runUUID)
                .addField("ignored", if (testResult.isIgnored) 1.0 else 0.0)
                .addField("success", if (testResult.status == TestStatus.PASSED) 1.0 else 0.0)
                .addField("duration", testResult.durationMillis())
                .build())
    }

    override fun terminate() {
        influxDb.close()
        super.terminate()
    }
}
