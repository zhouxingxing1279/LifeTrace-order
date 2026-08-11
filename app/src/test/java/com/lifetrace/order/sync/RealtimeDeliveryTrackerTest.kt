package com.lifetrace.order.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeDeliveryTrackerTest {
    @Test
    fun enablesRealtimeOnlyWithActiveDeliveries() {
        val tracker = RealtimeDeliveryTracker()
        assertFalse(tracker.current().enabled)

        val active = tracker.update(listOf("a", "b", "b"))
        assertTrue(active.enabled)
        assertEquals(setOf("a", "b"), active.activeOrderIds)
        assertEquals(15_000L, active.intervalMs)

        assertFalse(tracker.stop().enabled)
    }
}
