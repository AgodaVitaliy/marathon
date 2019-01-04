package com.malinskiy.marathon.ios

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.Configurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.spi.ContextAwareBase
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.common.KeyType
import net.schmizz.sshj.transport.kex.Curve25519SHA256
import net.schmizz.sshj.transport.random.BouncyCastleRandom

class IOSLogConfigurator: ContextAwareBase(), Configurator  {
    override fun configure(loggerContext: LoggerContext?) {
        loggerContext?.let {
            addInfo("Setting up default configuration.")

            val shorterOutput = System.getProperty("sun.java.command")?.contains("--compact-output") ?: false
            val layout = PatternLayout()
            layout.pattern = if (shorterOutput) {
                "%highlight(%.-1level) [%thread] <%logger{40}> %msg%n"
            } else {
                "%highlight(%.-1level %d{HH:mm:ss.SSS} [%thread] <%logger{40}> %msg%n)"
            }
            layout.context = loggerContext
            layout.start();

            val encoder = LayoutWrappingEncoder<ILoggingEvent>()
            encoder.context = loggerContext
            encoder.layout = layout

            val consoleAppender = ConsoleAppender<ILoggingEvent>()
            consoleAppender.isWithJansi = true
            consoleAppender.context = loggerContext
            consoleAppender.name = "console"
            consoleAppender.encoder = encoder
            consoleAppender.start()

            loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(consoleAppender)

            listOf(
                Curve25519SHA256::class.java.name,
                BouncyCastleRandom::class.java.name,
                DefaultConfig::class.java.name,
                KeyType::class.java.name,
                "net.schmizz.sshj.common.ECDSAVariationsAdapter"
            ).forEach {
                loggerContext.getLogger(it).level = Level.ERROR
            }
        }
    }
}
