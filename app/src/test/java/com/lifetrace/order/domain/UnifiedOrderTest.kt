package com.lifetrace.order.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedOrderTest {
    @Test
    fun stableKeyUsesPlatformAccountAndOrderId() {
        val order = sample(OrderStatus.IN_TRANSIT)
        assertEquals("jd:primary:123", order.stableKey)
    }

    @Test
    fun terminalOrdersAreNotActive() {
        assertFalse(sample(OrderStatus.COMPLETED).isActive)
        assertFalse(sample(OrderStatus.CANCELLED).isActive)
        assertFalse(sample(OrderStatus.REFUNDED).isActive)
        assertTrue(sample(OrderStatus.IN_TRANSIT).isActive)
    }

    private fun sample(status: OrderStatus) = UnifiedOrder(
        platform = PlatformId.JD,
        accountId = "primary",
        platformOrderId = "123",
        orderedAtEpochMs = 1,
        merchantName = "test",
        status = status,
        amountMinor = 100,
        lastSeenAtEpochMs = 1,
    )
}
