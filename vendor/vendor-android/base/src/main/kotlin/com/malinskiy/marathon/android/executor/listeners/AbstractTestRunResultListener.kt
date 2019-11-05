package com.malinskiy.marathon.android.executor.listeners

import com.malinskiy.marathon.android.model.TestIdentifier
import com.malinskiy.marathon.android.model.TestRunResultsAccumulator

abstract class AbstractTestRunResultListener : NoOpTestRunListener() {
    private val runResult = TestRunResultsAccumulator()

    override fun testRunStarted(runName: String, testCount: Int) {
        runResult.testRunStarted(runName, testCount)
    }

    override fun testStarted(test: TestIdentifier) {
        runResult.testStarted(test)
    }

    override fun testFailed(test: TestIdentifier, trace: String) {
        runResult.testFailed(test, trace)
    }

    override fun testAssumptionFailure(test: TestIdentifier, trace: String) {
        runResult.testAssumptionFailure(test, trace)
    }

    override fun testIgnored(test: TestIdentifier) {
        runResult.testIgnored(test)
    }

    override fun testEnded(test: TestIdentifier, testMetrics: Map<String, String>) {
        runResult.testEnded(test, testMetrics)
    }

    override fun testRunFailed(errorMessage: String) {
        runResult.testRunFailed(errorMessage)
        handleTestRunResults(runResult)
    }

    override fun testRunStopped(elapsedTime: Long) {
        runResult.testRunStopped(elapsedTime)
        handleTestRunResults(runResult)
    }

    override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>) {
        runResult.testRunEnded(elapsedTime, runMetrics)
        handleTestRunResults(runResult)
    }

    abstract fun handleTestRunResults(runResult: TestRunResultsAccumulator)
}
