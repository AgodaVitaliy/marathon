package com.malinskiy.marathon.execution

@Suppress("TooGenericExceptionCaught")
inline fun withRetry(attempts: Int, f: () -> Unit) {
    var attempt = 1
    while (true) {
        try {
            f()
            return
        } catch (th: Throwable) {
            if (attempt == attempts) {
                throw th
            }
        }
        ++attempt
    }
}
