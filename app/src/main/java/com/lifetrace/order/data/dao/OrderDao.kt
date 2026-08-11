package com.lifetrace.order.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.lifetrace.order.data.entity.FulfillmentEntity
import com.lifetrace.order.data.entity.OrderEntity
import com.lifetrace.order.data.entity.OrderItemEntity
import com.lifetrace.order.data.entity.RefundEntity
import com.lifetrace.order.data.entity.TrackingEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class OrderDao {
    @Query("SELECT * FROM orders ORDER BY orderedAtEpochMs DESC LIMIT :limit")
    abstract fun observeRecent(limit: Int = 100): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE platform = :platform AND accountId = :accountId AND isActive = 1 ORDER BY orderedAtEpochMs DESC")
    abstract suspend fun getActiveOrders(platform: String, accountId: String): List<OrderEntity>

    @Upsert
    abstract suspend fun upsertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItems(items: List<OrderItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertFulfillments(items: List<FulfillmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTrackingEvents(items: List<TrackingEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRefunds(items: List<RefundEntity>)

    @Query("DELETE FROM order_items WHERE platform = :platform AND accountId = :accountId AND platformOrderId = :orderId")
    abstract suspend fun deleteItems(platform: String, accountId: String, orderId: String)

    @Query("DELETE FROM fulfillments WHERE platform = :platform AND accountId = :accountId AND platformOrderId = :orderId")
    abstract suspend fun deleteFulfillments(platform: String, accountId: String, orderId: String)

    @Query("DELETE FROM tracking_events WHERE platform = :platform AND accountId = :accountId AND platformOrderId = :orderId")
    abstract suspend fun deleteTrackingEvents(platform: String, accountId: String, orderId: String)

    @Query("DELETE FROM refunds WHERE platform = :platform AND accountId = :accountId AND platformOrderId = :orderId")
    abstract suspend fun deleteRefunds(platform: String, accountId: String, orderId: String)

    @Transaction
    open suspend fun upsertAggregate(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        fulfillments: List<FulfillmentEntity>,
        trackingEvents: List<TrackingEventEntity>,
        refunds: List<RefundEntity>,
    ) {
        upsertOrder(order)
        deleteItems(order.platform, order.accountId, order.platformOrderId)
        deleteFulfillments(order.platform, order.accountId, order.platformOrderId)
        deleteTrackingEvents(order.platform, order.accountId, order.platformOrderId)
        deleteRefunds(order.platform, order.accountId, order.platformOrderId)
        if (items.isNotEmpty()) insertItems(items)
        if (fulfillments.isNotEmpty()) insertFulfillments(fulfillments)
        if (trackingEvents.isNotEmpty()) insertTrackingEvents(trackingEvents)
        if (refunds.isNotEmpty()) insertRefunds(refunds)
    }
}
