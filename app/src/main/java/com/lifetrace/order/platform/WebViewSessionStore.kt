package com.lifetrace.order.platform

import android.webkit.CookieManager
import android.webkit.WebStorage

class WebViewSessionStore(
    private val cookieManager: CookieManager = CookieManager.getInstance(),
) {
    init {
        cookieManager.setAcceptCookie(true)
    }

    fun hasAnyCookie(spec: PlatformSpec): Boolean =
        !cookieManager.getCookie(spec.sessionProbeUrl).isNullOrBlank()

    fun flush() {
        cookieManager.flush()
    }

    /**
     * Android WebView does not expose a reliable per-profile cookie deletion API for every
     * domain/path combination. Clearing all shopping sessions is therefore explicit rather than
     * pretending a partial deletion is complete.
     */
    fun clearAllSessions(onComplete: (Boolean) -> Unit = {}) {
        cookieManager.removeAllCookies { success ->
            WebStorage.getInstance().deleteAllData()
            cookieManager.flush()
            onComplete(success)
        }
    }
}
