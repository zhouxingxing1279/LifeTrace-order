package com.lifetrace.order.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lifetrace.order.data.entity.PlatformAccountEntity
import com.lifetrace.order.data.entity.ShoppingSyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlatformStateDao {
    @Query("SELECT * FROM platform_accounts ORDER BY platform, displayName")
    fun observeAccounts(): Flow<List<PlatformAccountEntity>>

    @Query("SELECT * FROM platform_accounts WHERE connected = 1 ORDER BY platform, displayName")
    suspend fun getConnectedAccounts(): List<PlatformAccountEntity>

    @Query("SELECT * FROM platform_accounts WHERE platform = :platform AND accountId = :accountId LIMIT 1")
    suspend fun getAccount(platform: String, accountId: String): PlatformAccountEntity?

    @Upsert
    suspend fun upsertAccount(account: PlatformAccountEntity)

    @Query("UPDATE platform_accounts SET pendingRefresh = :pending WHERE platform = :platform")
    suspend fun setPendingRefresh(platform: String, pending: Boolean)

    @Query("SELECT * FROM shopping_sync_state WHERE platform = :platform AND accountId = :accountId LIMIT 1")
    suspend fun getSyncState(platform: String, accountId: String): ShoppingSyncStateEntity?

    @Upsert
    suspend fun upsertSyncState(state: ShoppingSyncStateEntity)
}
