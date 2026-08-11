package com.lifetrace.order.platform

import com.lifetrace.order.data.entity.PlatformAccountEntity
import com.lifetrace.order.domain.PlatformFailure
import com.lifetrace.order.domain.PlatformId
import com.lifetrace.order.domain.UnifiedOrder

enum class FetchMode {
    NATIVE_HTTP,
    WEBVIEW_FETCH,
    DOM_FALLBACK,
    POC_UNRESOLVED,
}

enum class AuthState {
    AUTHENTICATED,
    AUTH_REQUIRED,
    VERIFICATION_REQUIRED,
    UNKNOWN,
}

enum class BackfillRange(val days: Long) {
    ONE_MONTH(31),
    ONE_YEAR(366),
}

data class PlatformSpec(
    val id: PlatformId,
    val displayName: String,
    val loginUrl: String,
    val sessionProbeUrl: String,
    val trustedHosts: Set<String>,
    val notificationPackages: Set<String>,
    val fetchMode: FetchMode = FetchMode.POC_UNRESOLVED,
)

data class RawOrderPayload(
    val platformOrderId: String,
    val payload: String,
    val observedAtEpochMs: Long,
)

data class PlatformPage(
    val orders: List<RawOrderPayload>,
    val nextCursor: String?,
    val oldestOrderAtEpochMs: Long?,
    val reachedBoundary: Boolean = false,
)

interface PlatformAdapter {
    val spec: PlatformSpec

    suspend fun checkAuth(account: PlatformAccountEntity): AuthState

    suspend fun fetchOrderPage(
        account: PlatformAccountEntity,
        cursor: String?,
        rangeStartEpochMs: Long,
    ): PlatformPage

    suspend fun refreshOrders(
        account: PlatformAccountEntity,
        platformOrderIds: List<String>,
    ): List<RawOrderPayload> = emptyList()

    suspend fun fetchActiveDeliveries(
        account: PlatformAccountEntity,
    ): List<RawOrderPayload> = emptyList()

    fun normalize(account: PlatformAccountEntity, raw: RawOrderPayload): UnifiedOrder

    fun classifyError(error: Throwable): PlatformFailure
}
