package com.malinskiy.marathon.cli

import com.malinskiy.marathon.cli.args.MarathonCliConfiguration
import com.malinskiy.marathon.cli.config.ConfigFactory
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) = mainBody {
    ArgParser(args).parseInto(::MarathonCliConfiguration).run {
        logger.info { "Starting marathon" }

        val configuration = ConfigFactory().create(marathonfile, androidSdkDir)
    }
}