package com.malinskiy.marathon.ios.cmd.remote

import kotlinx.coroutines.experimental.Deferred

interface CommandExecutor {
    companion object {
        val DEFAULT_SSH_CONNECTION_TIMEOUT_MILLIS: Long
            get() = 1800000L
    }

    fun startSession(command: String, timeoutMillis: Long = DEFAULT_SSH_CONNECTION_TIMEOUT_MILLIS): CommandSession

    fun exec(command: String, testOutputTimeoutMillis: Long = 0): CommandResult
    suspend fun exec(command: String, testOutputTimeoutMillis: Long = 0, onLine: (String) -> Unit): Int?

    fun disconnect()
}
