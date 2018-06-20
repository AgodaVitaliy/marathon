package com.malinskiy.marathon.execution.strategy.impl.flakiness

import com.malinskiy.marathon.MetricsProviderStub
import com.malinskiy.marathon.TestGenerator
import com.malinskiy.marathon.execution.TestShard
import org.amshove.kluent.shouldBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.time.Instant

class ProbabilityBasedFlakinessStrategySpek : Spek({
    describe("probability-based-strategy test") {
        val instant = Instant.now()
        group("min success rate 0.8") {
            it("should return 2 flaky tests for one with success rate = 0.5") {
                val metricsProvider = MetricsProviderStub()
                val strategy = ProbabilityBasedFlakinessStrategy(0.8, instant)
                val testShard = TestShard(TestGenerator().create(1))
                val result = strategy.process(testShard, metricsProvider)
                result.tests.size shouldBe 1
                result.flakyTests.size shouldBe 2
            }
            it("should return one flaky test for one test with success rate = 0.7") {
                val metricsProvider = MetricsProviderStub(successRate = 0.7)
                val strategy = ProbabilityBasedFlakinessStrategy(0.8, instant)
                val testShard = TestShard(TestGenerator().create(1))
                val result = strategy.process(testShard, metricsProvider)
                result.tests.size shouldBe 1
                result.flakyTests.size shouldBe 1
            }

            it("should return three flaky tests for three tests with success rate = 0.7") {
                val metricsProvider = MetricsProviderStub(successRate = 0.7)
                val strategy = ProbabilityBasedFlakinessStrategy(0.8, instant)
                val testShard = TestShard(TestGenerator().create(3))
                val result = strategy.process(testShard, metricsProvider)
                result.tests.size shouldBe 3
                result.flakyTests.size shouldBe 3
            }
        }
    }
})
