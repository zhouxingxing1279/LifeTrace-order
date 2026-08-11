package com.lifetrace.order.data

import com.lifetrace.order.data.entity.FulfillmentEntity
import com.lifetrace.order.data.entity.OrderEntity
import com.lifetrace.order.data.entity.OrderItemEntity
import com.lifetrace.order.data.entity.PlatformAccountEntity
import com.lifetrace.order.data.entity.RefundEntity
import com.lifetrace.order.data.entity.ShoppingSyncStateEntity
import com.lifetrace.order.data.entity.TrackingEventEntity
import com.lifetrace.order.domain.Fulfillment
import com.lifetrace.order.domain.OrderStatus
import com.lifetrace.order.domain.PlatformId
import com.lifetrace.order.domain.UnifiedOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OrderRepository(private val db: AppDatabase) {
    private val orderDao = db.orderDao()
    private val platformDao = db.platformStateDao()

    fun observeRecentOrders(limit: Int = 100): Flow<List<UnifiedOrder>> =
        orderDao.observeRecent(limit).map { rows -> rows.map { it.toDomainSummary() } }

    fun observeAccounts(): Flow<List<PlatformAccountEntity>> = platformDao.observeAccounts()

    suspend fun getConnectedAccounts(): List<PlatformAccountEntity> = platformDao.getConnectedAccounts()

    suspend fun saveAccount(account: PlatformAccountEntity) = platformDao.upsertAccount(account)

    suspend fun setPendingRefresh(platform: PlatformId, pending: Boolean) =
        platformDao.setPendingRefresh(platform.wireValue, pending)

    suspend fun getSyncState(platform: PlatformId, accountId: String): ShoppingSyncStateEntity? =
        platformDao.getSyncState(platform.wireValue, accountId)

    suspend fun saveSyncState(state: ShoppingSyncStateEntity) = platformDao.upsertSyncState(state)

    suspend fun getActiveOrderIds(platform: PlatformId, accountId: String): List<String> =
        orderDao.getActiveOrders(platform.wireValue, accountId).map { it.platformOrderId }

    suspend fun upsertOrders(orders: List<UnifiedOrder>) {
        orders.forEach { order ->
            val key = Triple(order.platform.wireValue, order.accountId, order.platformOrderId)
            val items = order.items.map {
                OrderItemEntity(
                    platform = key.first,
                    accountId = key.second,
                    platformOrderId = key.third,
                    itemKey = it.itemKey,
                    title = it.title,
                    quantity = it.quantity,
                    amountMinor = it.amountMinor,
                    imageUrl = it.imageUrl,
                )
            }
            val fulfillments = order.fulfillments.map {
                FulfillmentEntity(
                    platform = key.first,
                    accountId = key.second,
                    platformOrderId = key.third,
                    fulfillmentId = it.fulfillmentId,
                    type = it.type.name,
                    status = it.status.name,
                    carrier = it.carrier,
                    trackingNumber = it.trackingNumber,
                    estimatedAtEpochMs = it.estimatedAtEpochMs,
                )
            }
            val events = order.fulfillments.flatMap { fulfillment: Fulfillment ->
                fulfillment.events.map { event ->
                    TrackingEventEntity(
                        platform = key.first,
                        accountId = key.second,
                        platformOrderId = key.third,
                        fulfillmentId = fulfillment.fulfillmentId,
                        eventKey = event.eventKey,
                        occurredAtEpochMs = event.occurredAtEpochMs,
                        status = event.status,
                        description = event.description,
                        location = event.location,
                    )
                }
            }
            val refunds = order.refunds.map {
                RefundEntity(
                    platform = key.first,
                    accountId = key.second,
                    platformOrderId = key.third,
                    refundId = it.refundId,
                    status = it.status,
                    amountMinor = it.amountMinor,
                    updatedAtEpochMs = it.updatedAtEpochMs,
                )
            }
            orderDao.upsertAggregate(
                order = order.toEntity(),
                items = items,
                fulfillments = fulfillments,
                trackingEvents = events,
                refunds = refunds,
            )
        }
    }
}

private fun UnifiedOrder.toEntity() = OrderEntity(
    platform = platform.wireValue,
    accountId = accountId,
    platformOrderId = platformOrderId,
    orderedAtEpochMs = orderedAtEpochMs,
    merchantName = merchantName,
    status = status.name,
    amountMinor = amountMinor,
    currency = currency,
    isActive = isActive,
    sourceUpdatedAtEpochMs = sourceUpdatedAtEpochMs,
    lastSeenAtEpochMs = lastSeenAtEpochMs,
    rawHash = rawHash,
)

private fun OrderEntity.toDomainSummary() = UnifiedOrder(
    platform = PlatformId.entries.firstOrNull { it.wireValue == platform } ?: PlatformId.TAOBAO,
    accountId = accountId,
    platformOrderId = platformOrderId,
    orderedAtEpochMs = orderedAtEpochMs,
    merchantName = merchantName,
    status = runCatching { OrderStatus.valueOf(status) }.getOrDefault(OrderStatus.UNKNOWN),
    amountMinor = amountMinor,
    currency = currency,
    sourceUpdatedAtEpochMs = sourceUpdatedAtEpochMs,
    lastSeenAtEpochMs = lastSeenAtEpochMs,
    rawHash = rawHash,
)
