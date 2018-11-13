package com.malinskiy.marathon.execution.strategy.impl.batching

import com.fasterxml.jackson.annotation.JsonProperty
import com.malinskiy.marathon.analytics.Analytics
import com.malinskiy.marathon.execution.strategy.BatchingStrategy
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestBatch
import java.time.Instant
import java.util.*

class FixedSizeBatchingStrategy(private val size: Int,
                                private val durationMillis: Long? = null,
                                @JsonProperty("percentile") val percentile: Double? = null,
                                @JsonProperty("timeLimit") val timeLimit: Instant? = null) : BatchingStrategy {

    override fun process(queue: Queue<Test>, analytics: Analytics): TestBatch {
        var counter = 0
        var expectedBatchDuration = 0.0
        val duplicates = mutableListOf<Test>()
        val result = mutableSetOf<Test>()
        while (counter < size && queue.isNotEmpty()) {
            counter++
            val item = queue.poll()
            if (result.contains(item)) {
                duplicates.add(item)
            } else {
                result.add(item)
            }

            if(durationMillis != null && percentile != null && timeLimit != null) {
                //Check for expected batch duration. If we hit the duration limit - break
                //Important part is to add at least one test so that if one test is longer than a batch
                //We still have at least one test
                val expectedTestDuration = analytics.metricsProvider.executionTime(item, percentile, timeLimit)
                expectedBatchDuration += expectedTestDuration
                if(expectedBatchDuration >= durationMillis) break
            }
        }
        if (duplicates.isNotEmpty()) {
            queue.addAll(duplicates)
        }
        return TestBatch(result.toList())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FixedSizeBatchingStrategy

        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        return size
    }


}
