package com.malinskiy.marathon.analytics.tracker

import com.google.gson.Gson
import com.malinskiy.marathon.analytics.tracker.local.DeviceTracker
import com.malinskiy.marathon.analytics.tracker.local.JUnitTracker
import com.malinskiy.marathon.analytics.tracker.local.RawTestResultTracker
import com.malinskiy.marathon.analytics.tracker.local.TestRusultsTracker
import com.malinskiy.marathon.analytics.tracker.remote.influx.InfluxDbProvider
import com.malinskiy.marathon.analytics.tracker.remote.influx.InfluxDbTracker
import com.malinskiy.marathon.execution.AnalyticsConfiguration.InfluxDbConfiguration
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.report.internal.DeviceInfoReporter
import com.malinskiy.marathon.report.internal.TestResultReporter
import com.malinskiy.marathon.report.junit.JUnitReporter

internal class TrackerFactory(private val configuration: Configuration,
                              private val fileManager: FileManager,
                              private val deviceInfoReporter: DeviceInfoReporter,
                              private val testResultReporter: TestResultReporter,
                              private val gson: Gson) {
    fun create(): Tracker {
        val defaultTrackers = listOf(
                JUnitTracker(JUnitReporter(fileManager)),
                DeviceTracker(deviceInfoReporter),
                TestRusultsTracker(testResultReporter),
                RawTestResultTracker(fileManager, gson)
        )
        return when {
            configuration.analyticsConfiguration is InfluxDbConfiguration -> {
                val config = configuration.analyticsConfiguration
                DelegatingTracker(defaultTrackers + InfluxDbTracker(InfluxDbProvider(config).createDb()))
            }
            else -> DelegatingTracker(defaultTrackers)
        }
    }
}
