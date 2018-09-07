package com.malinskiy.marathon.analytics.tracker

import com.malinskiy.marathon.actor.StateMachine
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.queue.TestAction
import com.malinskiy.marathon.execution.queue.TestEvent
import com.malinskiy.marathon.execution.queue.TestState
import com.nhaarman.mockito_kotlin.mock
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class NoOpTrackerSpek : Spek({
    describe("NoOpTracker test") {
        it("should not do anything") {
            val tracker = NoOpTracker()
            val poolId = DevicePoolId("id")
            val device: Device = mock()
            val transition: StateMachine.Transition<TestState, TestEvent, TestAction> = mock()
            tracker.trackDeviceConnected(poolId, device)
            tracker.trackTestTransition(poolId, transition)
        }
    }
})
