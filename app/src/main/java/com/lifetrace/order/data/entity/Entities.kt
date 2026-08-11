package com.lifetrace.order.data.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "orders",
    primaryKeys = ["platform", "accountId", "platformOrderId"],
    indices = [
        Index(value = ["orderedAtEpochMs"]),
        Index(value = ["platform", "accountId", "isActive"]),
    ],
)
data class OrderEntity(
    val platform: String,
    val accountId: String,
    val platformOrderId: String,
    val orderedAtEpochMs: Long,
    val merchantName: String,
    val status: String,
    val amountMinor: Long,
    val currency: String,
    val isActive: Boolean,
    val sourceUpdatedAtEpochMs: Long?,
    val lastSeenAtEpochMs: Long,
    val rawHash: String?,
)

@Entity(
    tableName = "order_items",
    primaryKeys = ["platform", "accountId", "platformOrderId", "itemKey"],
)
data class OrderItemEntity(
    val platform: String,
    val accountId: String,
    val platformOrderId: String,
    val itemKey: String,
    val title: String,
    val quantity: Int,
    val amountMinor: Long?,
    val imageUrl: String?,
)

@Entity(
    tableName = "fulfillments",
    primaryKeys = ["platform", "accountId", "platformOrderId", "fulfillmentId"],
)
data class FulfillmentEntity(
    val platform: String,
    val accountId: String,
    val platformOrderId: String,
    val fulfillmentId: String,
    val type: String,
    val status: String,
    val carrier: String?,
    val trackingNumber: String?,
    val estimatedAtEpochMs: Long?,
)

@Entity(
    tableName = "tracking_events",
    primaryKeys = ["platform", "accountId", "platformOrderId", "fulfillmentId", "eventKey"],
)
data class TrackingEventEntity(
    val platform: String,
    val accountId: String,
    val platformOrderId: String,
    val fulfillmentId: String,
    val eventKey: String,
    val occurredAtEpochMs: Long,
    val status: String,
    val description: String,
    val location: String?,
)

@Entity(
    tableName = "refunds",
    primaryKeys = ["platform", "accountId", "platformOrderId", "refundId"],
)
data class RefundEntity(
    val platform: String,
    val accountId: String,
    val platformOrderId: String,
    val refundId: String,
    val status: String,
    val amountMinor: Long?,
    val updatedAtEpochMs: Long?,
)

@Entity(
    tableName = "shopping_sync_state",
    primaryKeys = ["platform", "accountId"],
)
data class ShoppingSyncStateEntity(
    val platform: String,
    val accountId: String,
    val initialSyncCompleted: Boolean = false,
    val initialRangeStartEpochMs: Long? = null,
    val initialCursor: String? = null,
    val sourceCursor: String? = null,
    val lastSuccessAtEpochMs: Long? = null,
    val lastAttemptAtEpochMs: Long? = null,
    val overlapDays: Int = 14,
    val lastErrorCode: String? = null,
    val lastErrorMessage: String? = null,
)

@Entity(
    tableName = "platform_accounts",
    primaryKeys = ["platform", "accountId"],
)
data class PlatformAccountEntity(
    val platform: String,
    val accountId: String,
    val displayName: String,
    val connected: Boolean,
    val lastAuthState: String,
    val lastAuthCheckAtEpochMs: Long?,
    val pendingRefresh: Boolean = false,
)
