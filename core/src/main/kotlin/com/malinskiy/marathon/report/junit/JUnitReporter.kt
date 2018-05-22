package com.malinskiy.marathon.report.junit

import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.io.FileType
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

class JUnitReporter(private val fileManager: FileManager) {
    fun testFinished(devicePoolId: DevicePoolId, device: Device, testResult: TestResult) {
        val test = testResult.test
        val duration = testResult.endTime - testResult.startTime

        val failures = if (testResult.status == TestStatus.FAILURE) 1 else 0
        val ignored = if (testResult.status == TestStatus.IGNORED) 1 else 0

        fun Long.toJunitSeconds(): String = (TimeUnit.NANOSECONDS.toMillis(this) / 1000.0).toString()

        val file = fileManager.createFile(FileType.TEST, devicePoolId, device, testResult.test)
        val writer = XMLOutputFactory.newFactory().createXMLStreamWriter(FileWriter(file))

        writer.document {
            element("testsuite") {
                attribute("name", devicePoolId.name)
                attribute("tests", "1")
                attribute("failures", "$failures")
                attribute("errors", "0")
                attribute("skipped", "$ignored")
                attribute("time", duration.toJunitSeconds())
                attribute("timestamp", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(testResult.endTime)))
                element("properties") {}
                element("testcase") {
                    attribute("classname", "${test.pkg}.${test.clazz}")
                    attribute("name", test.method)
                    attribute("time", duration.toJunitSeconds())
                    when (testResult.status) {
                        TestStatus.IGNORED -> {
                            element("skipped") {
                                testResult.stacktrace?.takeIf { it.isNotEmpty() }?.let {
                                    writeCData(it)
                                }
                            }
                        }
                        TestStatus.FAILURE -> {
                            element("failure") {
                                writeCData(testResult.stacktrace!!)
                            }
                        }
                        else -> {
                        }
                    }
                }
            }
        }

        writer.flush()
        writer.close()
    }
}

fun XMLStreamWriter.document(init: XMLStreamWriter.() -> Unit): XMLStreamWriter {
    this.writeStartDocument()
    this.init()
    this.writeEndDocument()
    return this
}

fun XMLStreamWriter.element(name: String, init: XMLStreamWriter.() -> Unit): XMLStreamWriter {
    this.writeStartElement(name)
    this.init()
    this.writeEndElement()
    return this
}

fun XMLStreamWriter.element(name: String, content: String) {
    element(name) {
        writeCharacters(content)
    }
}

fun XMLStreamWriter.attribute(name: String, value: String) = writeAttribute(name, value)