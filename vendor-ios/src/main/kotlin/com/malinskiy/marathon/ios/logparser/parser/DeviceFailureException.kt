package com.malinskiy.marathon.ios.logparser.parser

enum class DeviceFailureReason { Unknown, FailedRunner, ConnectionAbort }

class DeviceFailureException(val reason: DeviceFailureReason, message: String): RuntimeException(message)
