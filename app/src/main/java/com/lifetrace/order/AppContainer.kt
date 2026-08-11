package com.lifetrace.order

import android.content.Context
import com.lifetrace.order.data.AppDatabase
import com.lifetrace.order.data.OrderRepository
import com.lifetrace.order.platform.PlatformRegistry
import com.lifetrace.order.platform.WebViewSessionStore
import com.lifetrace.order.sync.OrderSyncManager

class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.create(context)
    val repository = OrderRepository(database)
    val sessionStore = WebViewSessionStore()
    val platformRegistry = PlatformRegistry.createDefault(sessionStore)
    val syncManager = OrderSyncManager(repository, platformRegistry)
}
