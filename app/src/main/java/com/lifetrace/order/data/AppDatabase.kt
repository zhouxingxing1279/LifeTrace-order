package com.lifetrace.order.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lifetrace.order.data.dao.OrderDao
import com.lifetrace.order.data.dao.PlatformStateDao
import com.lifetrace.order.data.entity.FulfillmentEntity
import com.lifetrace.order.data.entity.OrderEntity
import com.lifetrace.order.data.entity.OrderItemEntity
import com.lifetrace.order.data.entity.PlatformAccountEntity
import com.lifetrace.order.data.entity.RefundEntity
import com.lifetrace.order.data.entity.ShoppingSyncStateEntity
import com.lifetrace.order.data.entity.TrackingEventEntity

@Database(
    entities = [
        OrderEntity::class,
        OrderItemEntity::class,
        FulfillmentEntity::class,
        TrackingEventEntity::class,
        RefundEntity::class,
        ShoppingSyncStateEntity::class,
        PlatformAccountEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun platformStateDao(): PlatformStateDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "lifetrace-order.db",
        ).build()
    }
}
