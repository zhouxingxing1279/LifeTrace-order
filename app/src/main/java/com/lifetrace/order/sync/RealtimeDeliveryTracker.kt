package com.lifetrace.order.sync

private const val REALTIME_INTERVAL_MS = 15_000L

data class RealtimeDeliveryState(
    val activeOrderIds: Set<String> = emptySet(),
    val intervalMs: Long = REALTIME_INTERVAL_MS,
) {
    val enabled: Boolean get() = activeOrderIds.isNotEmpty()
}

class RealtimeDeliveryTracker {
    private var state = RealtimeDeliveryState()

    fun update(activeOrderIds: Collection<String>): RealtimeDeliveryState {
        state = RealtimeDeliveryState(activeOrderIds = activeOrderIds.toSet())
        return state
    }

    fun stop(): RealtimeDeliveryState {
        state = RealtimeDeliveryState()
        return state
    }

    fun current(): RealtimeDeliveryState = state
}
