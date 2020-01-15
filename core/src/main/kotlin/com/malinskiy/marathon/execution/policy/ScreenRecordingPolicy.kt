package com.malinskiy.marathon.execution.policy

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Defines when screen recordings should be kept.
 */
enum class ScreenRecordingPolicy {
    /**
     * Keep screen recording only if the test failed. (Default)
     */
    ON_FAILURE,
    /**
     * Keep screen recording in any case.
     */
    ON_ANY;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(key: String?): ScreenRecordingPolicy? {
            return when (key) {
                ON_ANY.name -> ON_ANY
                ON_FAILURE.name -> ON_FAILURE
                null -> null // in null case return nothing
                else -> throw Exception("Wrong screen recording policy. Use one of ${values()}")
            }
        }
    }
}
