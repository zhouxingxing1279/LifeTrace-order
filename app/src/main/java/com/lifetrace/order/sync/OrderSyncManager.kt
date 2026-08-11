package com.lifetrace.order.sync

import com.lifetrace.order.data.OrderRepository
import com.lifetrace.order.data.entity.PlatformAccountEntity
import com.lifetrace.order.data.entity.ShoppingSyncStateEntity
import com.lifetrace.order.domain.PlatformErrorCode
import com.lifetrace.order.domain.PlatformFailure
import com.lifetrace.order.domain.PlatformId
import com.lifetrace.order.platform.AuthState
import com.lifetrace.order.platform.BackfillRange
import com.lifetrace.order.platform.PlatformAdapter
import com.lifetrace.order.platform.PlatformRegistry
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val NORMAL_REFRESH_INTERVAL_MS = 5 * 60_000L
private const val DEFAULT_REALTIME_INTERVAL_MS = 15_000L
private const val DAY_MS = 24 * 60 * 60_000L

data class SyncUiState(
    val foreground: Boolean = false,
    val running: Boolean = false,
    val lastSuccessAtEpochMs: Long? = null,
    val activeRealtimeDeliveries: Int = 0,
    val lastMessage: String = "等待前台同步",
)

class OrderSyncManager(
    private val repository: OrderRepository,
    private val registry: PlatformRegistry,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val refreshMutex = Mutex()
    private val backfillJobs = ConcurrentHashMap<String, Job>()
    private val realtimeTracker = RealtimeDeliveryTracker()
    private var foregroundJob: Job? = null

    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    fun onForeground() {
        if (foregroundJob?.isActive == true) return
        _state.value = _state.value.copy(foreground = true, lastMessage = "进入前台，立即刷新")
        foregroundJob = scope.launch {
            while (isActive) {
                refreshAllConnectedAccounts()
                val delayMs = if (realtimeTracker.current().enabled) {
                    realtimeTracker.current().intervalMs
                } else {
                    NORMAL_REFRESH_INTERVAL_MS
                }
                delay(delayMs)
            }
        }
    }

    fun onBackground() {
        _state.value = _state.value.copy(
            foreground = false,
            running = false,
            activeRealtimeDeliveries = 0,
            lastMessage = "后台：主动轮询已停止",
        )
        foregroundJob?.cancel()
        foregroundJob = null
        realtimeTracker.stop()
        backfillJobs.values.forEach { it.cancel() }
        backfillJobs.clear()
    }

    fun requestImmediateRefresh(platform: PlatformId? = null) {
        if (!_state.value.foreground) {
            if (platform != null) scope.launch { repository.setPendingRefresh(platform, true) }
            return
        }
        scope.launch {
            if (platform == null) refreshAllConnectedAccounts() else refreshPlatform(platform)
        }
    }

    fun startBackfill(platform: PlatformId, accountId: String, range: BackfillRange) {
        val key = "${platform.wireValue}:$accountId"
        if (backfillJobs[key]?.isActive == true) return
        backfillJobs[key] = scope.launch {
            try {
                runBackfill(platform, accountId, range)
            } finally {
                backfillJobs.remove(key)
            }
        }
    }

    fun cancelBackfill(platform: PlatformId, accountId: String) {
        backfillJobs.remove("${platform.wireValue}:$accountId")?.cancel()
    }

    private suspend fun refreshAllConnectedAccounts() {
        refreshMutex.withLock {
            val accounts = repository.getConnectedAccounts()
            if (accounts.isEmpty()) {
                _state.value = _state.value.copy(running = false, lastMessage = "尚未连接购物平台")
                return
            }
            _state.value = _state.value.copy(running = true, lastMessage = "正在刷新 ${accounts.size} 个平台账户")
            val activeDeliveries = mutableSetOf<String>()
            accounts.forEach { account ->
                val platform = platformFrom(account.platform) ?: return@forEach
                val adapter = registry.get(platform)
                runCatching { refreshAccount(adapter, account) }
                    .onSuccess { activeDeliveries += it }
                    .onFailure { recordFailure(adapter, account, it) }
            }
            realtimeTracker.update(activeDeliveries)
            _state.value = _state.value.copy(
                running = false,
                activeRealtimeDeliveries = activeDeliveries.size,
                lastSuccessAtEpochMs = if (activeDeliveries.isNotEmpty() || accounts.isNotEmpty()) now() else _state.value.lastSuccessAtEpochMs,
                lastMessage = if (activeDeliveries.isEmpty()) "前台增量刷新完成" else "外卖实时模式：${activeDeliveries.size} 个活跃订单",
            )
        }
    }

    private suspend fun refreshPlatform(platform: PlatformId) {
        refreshMutex.withLock {
            val accounts = repository.getConnectedAccounts().filter { it.platform == platform.wireValue }
            accounts.forEach { account ->
                val adapter = registry.get(platform)
                runCatching { refreshAccount(adapter, account) }
                    .onFailure { recordFailure(adapter, account, it) }
            }
        }
    }

    private suspend fun refreshAccount(
        adapter: PlatformAdapter,
        account: PlatformAccountEntity,
    ): Set<String> {
        when (val auth = adapter.checkAuth(account)) {
            AuthState.AUTH_REQUIRED -> throw PlatformFailure(PlatformErrorCode.AUTH_REQUIRED, "${adapter.spec.displayName} 需要重新登录")
            AuthState.VERIFICATION_REQUIRED -> throw PlatformFailure(PlatformErrorCode.VERIFICATION_REQUIRED, "${adapter.spec.displayName} 需要用户完成安全验证")
            AuthState.AUTHENTICATED, AuthState.UNKNOWN -> Unit
        }

        val oldState = repository.getSyncState(adapter.spec.id, account.accountId)
        val overlapDays = oldState?.overlapDays ?: 14
        val rangeStart = now() - overlapDays * DAY_MS
        val page = adapter.fetchOrderPage(account, cursor = null, rangeStartEpochMs = rangeStart)
        val normalized = page.orders.map { adapter.normalize(account, it) }
        repository.upsertOrders(normalized)

        val activeOrderIds = repository.getActiveOrderIds(adapter.spec.id, account.accountId)
        if (activeOrderIds.isNotEmpty()) {
            val refreshed = adapter.refreshOrders(account, activeOrderIds).map { adapter.normalize(account, it) }
            repository.upsertOrders(refreshed)
        }

        val activeDeliveries = adapter.fetchActiveDeliveries(account).map { adapter.normalize(account, it) }
        repository.upsertOrders(activeDeliveries)
        repository.setPendingRefresh(adapter.spec.id, false)
        repository.saveSyncState(
            (oldState ?: ShoppingSyncStateEntity(adapter.spec.id.wireValue, account.accountId)).copy(
                sourceCursor = page.nextCursor ?: oldState?.sourceCursor,
                lastSuccessAtEpochMs = now(),
                lastAttemptAtEpochMs = now(),
                lastErrorCode = null,
                lastErrorMessage = null,
            ),
        )
        return activeDeliveries.mapTo(mutableSetOf()) { it.platformOrderId }
    }

    private suspend fun runBackfill(platform: PlatformId, accountId: String, range: BackfillRange) {
        if (!_state.value.foreground) return
        val account = repository.getConnectedAccounts().firstOrNull {
            it.platform == platform.wireValue && it.accountId == accountId
        } ?: return
        val adapter = registry.get(platform)
        val rangeStart = now() - range.days * DAY_MS
        var state = repository.getSyncState(platform, accountId)
            ?: ShoppingSyncStateEntity(platform.wireValue, accountId)
        var cursor = state.initialCursor

        while (scope.isActive && _state.value.foreground) {
            val page = adapter.fetchOrderPage(account, cursor, rangeStart)
            repository.upsertOrders(page.orders.map { adapter.normalize(account, it) })
            cursor = page.nextCursor
            val complete = page.reachedBoundary ||
                cursor == null ||
                (page.oldestOrderAtEpochMs != null && page.oldestOrderAtEpochMs <= rangeStart)
            state = state.copy(
                initialRangeStartEpochMs = rangeStart,
                initialCursor = cursor,
                initialSyncCompleted = complete,
                lastAttemptAtEpochMs = now(),
                lastSuccessAtEpochMs = now(),
                lastErrorCode = null,
                lastErrorMessage = null,
            )
            repository.saveSyncState(state)
            _state.value = _state.value.copy(
                lastMessage = if (complete) "${adapter.spec.displayName} 历史回填完成" else "${adapter.spec.displayName} 历史回填继续：cursor=${cursor ?: "end"}",
            )
            if (complete) break
        }
    }

    private suspend fun recordFailure(
        adapter: PlatformAdapter,
        account: PlatformAccountEntity,
        error: Throwable,
    ) {
        if (error is CancellationException) throw error
        val failure = adapter.classifyError(error)
        val old = repository.getSyncState(adapter.spec.id, account.accountId)
            ?: ShoppingSyncStateEntity(adapter.spec.id.wireValue, account.accountId)
        repository.saveSyncState(
            old.copy(
                lastAttemptAtEpochMs = now(),
                lastErrorCode = failure.code.name,
                lastErrorMessage = failure.message,
            ),
        )
        _state.value = _state.value.copy(running = false, lastMessage = failure.message)
        if (failure.code in setOf(
                PlatformErrorCode.AUTH_REQUIRED,
                PlatformErrorCode.VERIFICATION_REQUIRED,
                PlatformErrorCode.RATE_LIMITED,
            )
        ) {
            realtimeTracker.stop()
        }
    }

    private fun platformFrom(value: String): PlatformId? =
        PlatformId.entries.firstOrNull { it.wireValue == value }
}
