package com.lifetrace.order.platform

import com.lifetrace.order.data.entity.PlatformAccountEntity
import com.lifetrace.order.domain.PlatformErrorCode
import com.lifetrace.order.domain.PlatformFailure
import com.lifetrace.order.domain.PlatformId
import com.lifetrace.order.domain.UnifiedOrder

class WebOnlyPocAdapter(
    override val spec: PlatformSpec,
    private val sessionStore: WebViewSessionStore,
) : PlatformAdapter {
    override suspend fun checkAuth(account: PlatformAccountEntity): AuthState =
        if (sessionStore.hasAnyCookie(spec)) AuthState.UNKNOWN else AuthState.AUTH_REQUIRED

    override suspend fun fetchOrderPage(
        account: PlatformAccountEntity,
        cursor: String?,
        rangeStartEpochMs: Long,
    ): PlatformPage = throw unresolved()

    override suspend fun refreshOrders(
        account: PlatformAccountEntity,
        platformOrderIds: List<String>,
    ): List<RawOrderPayload> = if (platformOrderIds.isEmpty()) emptyList() else throw unresolved()

    override suspend fun fetchActiveDeliveries(
        account: PlatformAccountEntity,
    ): List<RawOrderPayload> = emptyList()

    override fun normalize(account: PlatformAccountEntity, raw: RawOrderPayload): UnifiedOrder =
        throw PlatformFailure(
            PlatformErrorCode.NORMALIZE_FAILED,
            "${spec.displayName} normalizer 尚未通过真机 PoC 固化",
        )

    override fun classifyError(error: Throwable): PlatformFailure = when (error) {
        is PlatformFailure -> error
        else -> PlatformFailure(
            PlatformErrorCode.UNKNOWN,
            error.message ?: "${spec.displayName} 未知错误",
            retryable = false,
            cause = error,
        )
    }

    private fun unresolved() = PlatformFailure(
        PlatformErrorCode.SOURCE_UNAVAILABLE,
        "${spec.displayName} 已完成登录容器接入；订单 Fetch Mode 需要真机正常登录后完成 PoC",
        retryable = false,
    )
}

object PlatformSpecs {
    val meituan = PlatformSpec(
        id = PlatformId.MEITUAN,
        displayName = "美团",
        loginUrl = "https://passport.meituan.com/account/unitivelogin?service=www",
        sessionProbeUrl = "https://www.meituan.com/",
        trustedHosts = setOf("meituan.com", "www.meituan.com", "passport.meituan.com"),
        notificationPackages = setOf("com.sankuai.meituan", "com.sankuai.meituan.takeoutnew"),
    )

    val jd = PlatformSpec(
        id = PlatformId.JD,
        displayName = "京东",
        loginUrl = "https://passport.jd.com/new/login.aspx",
        sessionProbeUrl = "https://www.jd.com/",
        trustedHosts = setOf("jd.com", "www.jd.com", "passport.jd.com"),
        notificationPackages = setOf("com.jingdong.app.mall"),
    )

    val taobao = PlatformSpec(
        id = PlatformId.TAOBAO,
        displayName = "淘宝 / 天猫",
        loginUrl = "https://login.taobao.com/member/login.jhtml",
        sessionProbeUrl = "https://www.taobao.com/",
        trustedHosts = setOf("taobao.com", "www.taobao.com", "login.taobao.com", "tmall.com"),
        notificationPackages = setOf("com.taobao.taobao"),
    )

    val pinduoduo = PlatformSpec(
        id = PlatformId.PINDUODUO,
        displayName = "拼多多",
        loginUrl = "https://mobile.yangkeduo.com/",
        sessionProbeUrl = "https://mobile.yangkeduo.com/",
        trustedHosts = setOf("yangkeduo.com", "mobile.yangkeduo.com", "pinduoduo.com"),
        notificationPackages = setOf("com.xunmeng.pinduoduo"),
    )

    val all = listOf(meituan, jd, taobao, pinduoduo)
}
