package com.lifetrace.order.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.lifetrace.order.OrderApplication
import com.lifetrace.order.platform.PlatformSpecs

class OrderNotificationListenerService : NotificationListenerService() {
    private val classifier = NotificationClassifier(PlatformSpecs.all)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val platform = classifier.classify(sbn.packageName, title, text) ?: return

        // Notification is only a signal. The source of truth remains the platform adapter refresh.
        val app = application as? OrderApplication ?: return
        app.container.syncManager.requestImmediateRefresh(platform)
    }
}
