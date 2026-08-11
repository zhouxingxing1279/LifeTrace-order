package com.lifetrace.order.domain

enum class PlatformId(val wireValue: String) {
    MEITUAN("meituan"),
    JD("jd"),
    TAOBAO("taobao"),
    PINDUODUO("pinduoduo"),
}

enum class OrderStatus {
    UNKNOWN,
    PENDING_PAYMENT,
    PAID,
    PENDING_SHIPMENT,
    SHIPPED,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    READY_FOR_PICKUP,
    COMPLETED,
    CANCELLED,
    REFUNDING,
    REFUNDED,
}

enum class FulfillmentType {
    PARCEL,
    LOCAL_DELIVERY,
    PICKUP,
    VIRTUAL,
    UNKNOWN,
}

enum class FulfillmentStatus {
    UNKNOWN,
    PREPARING,
    SHIPPED,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    READY_FOR_PICKUP,
    DELIVERED,
    CANCELLED,
}

data class OrderItem(
    val itemKey: String,
    val title: String,
    val quantity: Int,
    val amountMinor: Long?,
    val imageUrl: String? = null,
)

data class TrackingEvent(
    val eventKey: String,
    val occurredAtEpochMs: Long,
    val status: String,
    val description: String,
    val location: String? = null,
)

data class Fulfillment(
    val fulfillmentId: String,
    val type: FulfillmentType,
    val status: FulfillmentStatus,
    val carrier: String? = null,
    val trackingNumber: String? = null,
    val estimatedAtEpochMs: Long? = null,
    val events: List<TrackingEvent> = emptyList(),
)

data class Refund(
    val refundId: String,
    val status: String,
    val amountMinor: Long?,
    val updatedAtEpochMs: Long?,
)

data class UnifiedOrder(
    val platform: PlatformId,
    val accountId: String,
    val platformOrderId: String,
    val orderedAtEpochMs: Long,
    val merchantName: String,
    val status: OrderStatus,
    val amountMinor: Long,
    val currency: String = "CNY",
    val items: List<OrderItem> = emptyList(),
    val fulfillments: List<Fulfillment> = emptyList(),
    val refunds: List<Refund> = emptyList(),
    val sourceUpdatedAtEpochMs: Long? = null,
    val lastSeenAtEpochMs: Long,
    val rawHash: String? = null,
) {
    val stableKey: String
        get() = "${platform.wireValue}:$accountId:$platformOrderId"

    val isActive: Boolean
        get() = status !in setOf(
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED,
            OrderStatus.REFUNDED,
        )
}
