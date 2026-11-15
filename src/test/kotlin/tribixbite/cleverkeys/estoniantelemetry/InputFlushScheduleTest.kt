package tribixbite.cleverkeys.estoniantelemetry

import org.junit.Assert.*
import org.junit.Test

class InputFlushScheduleTest {
    @Test fun heldSignalsBoundScheduledWorkWhileTheProducerOutrunsStorage() {
        val signals = InputFlushSchedule()
        assertTrue(signals.requestDelayed())
        assertFalse(signals.requestImmediate(63))
        assertTrue(signals.requestImmediate(64))
        repeat(10_000) {
            assertFalse(signals.requestDelayed())
            assertFalse(signals.requestImmediate(128))
        }
        // An immediate flush draining the queue does not unschedule the pending timer.
        signals.finishImmediate()
        assertFalse(signals.requestDelayed())
        assertTrue(signals.requestImmediate(64))
        signals.finishImmediate()
        signals.finishDelayed()
        assertTrue(signals.requestDelayed())
    }
}
