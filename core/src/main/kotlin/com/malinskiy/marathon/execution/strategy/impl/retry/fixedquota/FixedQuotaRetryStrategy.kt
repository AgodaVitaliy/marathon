package com.malinskiy.marathon.execution.strategy.impl.retry.fixedquota

import com.fasterxml.jackson.annotation.JsonProperty
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.execution.strategy.RetryStrategy
import com.malinskiy.marathon.log.MarathonLogging

class FixedQuotaRetryStrategy(@JsonProperty("totalAllowedRetryQuota") totalAllowedRetryQuota: Int = 200,
                              @JsonProperty("retryPerTestQuota") retryPerTestQuota: Int = 3,
                              @JsonProperty("noRetryTestMatchers") private val noRetryTestMatchers: List<TestMatcher> = emptyList()) : RetryStrategy {
    private val retryWatchdog = RetryWatchdog(totalAllowedRetryQuota, retryPerTestQuota)
    private val poolTestCaseFailureAccumulator = PoolTestFailureAccumulator()

    private val logger = MarathonLogging.logger("FixedQuotaRetryStrategy")

    override fun process(devicePoolId: DevicePoolId, tests: Collection<TestResult>, testShard: TestShard): List<TestResult> {
        return tests.filter { testResult ->
            if (noRetryTestMatchers.any { it.matches(testResult.test) }) {
                logger.debug("Not retrying test ${testResult.test}")
                false
            } else {
                poolTestCaseFailureAccumulator.record(devicePoolId, testResult.test)
                val flakinessResultCount = testShard.flakyTests.count { it == testResult.test }
                retryWatchdog.requestRetry(poolTestCaseFailureAccumulator.getCount(devicePoolId, testResult.test) + flakinessResultCount)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FixedQuotaRetryStrategy

        if (retryWatchdog != other.retryWatchdog) return false

        if (noRetryTestMatchers != other.noRetryTestMatchers) return false

        return true
    }

    override fun hashCode(): Int {
        return retryWatchdog.hashCode() + 31 * noRetryTestMatchers.hashCode()
    }

    override fun toString(): String {
        return "FixedQuotaRetryStrategy(retryWatchdog=$retryWatchdog, " +
                "poolTestCaseFailureAccumulator=$poolTestCaseFailureAccumulator, " +
                "ignoredTests=$noRetryTestMatchers)"
    }
}
