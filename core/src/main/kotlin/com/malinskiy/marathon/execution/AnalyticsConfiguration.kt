package com.malinskiy.marathon.execution

sealed class AnalyticsConfiguration {
    object DisabledAnalytics : AnalyticsConfiguration()
    data class InfluxDbConfiguration(val url: String,
                                     val user: String,
                                     val password: String,
                                     val dbName: String,
                                     val retentionPolicyConfiguration: RetentionPolicyConfiguration,
                                     val logLevel: LogLevel = LogLevel.VERBOSE) : AnalyticsConfiguration() {

        enum class LogLevel { NONE, VERBOSE }

        data class RetentionPolicyConfiguration(val name: String,
                                                val duration: String,
                                                val shardDuration: String,
                                                val replicationFactor: Int,
                                                val isDefault: Boolean) {
            companion object {
                val default: RetentionPolicyConfiguration = RetentionPolicyConfiguration("rpMarathon", "30d", "30m", 2, true)
            }
        }
    }
}
