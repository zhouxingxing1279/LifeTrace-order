package com.lifetrace.order.notification

import com.lifetrace.order.domain.PlatformId
import com.lifetrace.order.platform.PlatformSpecs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationClassifierTest {
    private val classifier = NotificationClassifier(PlatformSpecs.all)

    @Test
    fun meituanDeliveryNotificationTriggersRefresh() {
        assertEquals(
            PlatformId.MEITUAN,
            classifier.classify("com.sankuai.meituan", "订单配送中", "骑手正在送餐"),
        )
    }

    @Test
    fun marketingNotificationIsIgnored() {
        assertNull(classifier.classify("com.sankuai.meituan", "限时优惠", "领券立减"))
    }

    @Test
    fun unknownPackageIsIgnored() {
        assertNull(classifier.classify("example.app", "订单已发货", "查看物流"))
    }
}
