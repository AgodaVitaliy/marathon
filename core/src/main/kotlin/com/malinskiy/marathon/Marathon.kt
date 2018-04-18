package com.malinskiy.marathon

import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.TestParser
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

private val log = KotlinLogging.logger {}

class Marathon(val configuration: Configuration) {

    fun run(): Boolean {
        val loader = ServiceLoader.load(TestParser::class.java)
        val testParser = loader.first()

        val deviceProvider = ServiceLoader.load(DeviceProvider::class.java).first()
        deviceProvider.initialize(configuration.vendorConfiguration)

        val timeMillis = measureTimeMillis {

            val tests = testParser.extract(configuration.testApplicationOutput)

            tests.forEach { println(it) }
            deviceProvider.getDevices().forEach { println(it) }

            if (configuration.outputDir.exists()) {
                log.info { "Output ${configuration.outputDir} already exists" }
                configuration.outputDir.deleteRecursively()
            }
            configuration.outputDir.mkdirs()
        }

        val hours = TimeUnit.MICROSECONDS.toHours(timeMillis)
        val minutes = TimeUnit.MICROSECONDS.toMinutes(timeMillis)
        val seconds = TimeUnit.MICROSECONDS.toSeconds(timeMillis)

        log.info { "Total time: ${hours}H ${minutes}m ${seconds}s" }

        deviceProvider.terminate()

        return false
    }
}