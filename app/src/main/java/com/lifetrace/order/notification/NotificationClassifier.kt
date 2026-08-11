package com.lifetrace.order.notification

import com.lifetrace.order.domain.PlatformId
import com.lifetrace.order.platform.PlatformSpec

class NotificationClassifier(specs: List<PlatformSpec>) {
    private val packageToPlatform = buildMap {
        specs.forEach { spec ->
            spec.notificationPackages.forEach { packageName -> put(packageName, spec.id) }
        }
    }

    private val orderKeywords = listOf(
        "订单",
        "配送",
        "骑手",
        "送达",
        "取餐",
        "发货",
        "物流",
        "快递",
        "退款",
        "售后",
        "收货",
    )

    fun classify(packageName: String, title: String?, text: String?): PlatformId? {
        val platform = packageToPlatform[packageName] ?: return null
        val content = buildString {
            if (!title.isNullOrBlank()) append(title)
            append(' ')
            if (!text.isNullOrBlank()) append(text)
        }
        if (content.isBlank()) return null
        return platform.takeIf { orderKeywords.any(content::contains) }
    }
}
