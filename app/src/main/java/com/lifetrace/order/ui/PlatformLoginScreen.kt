package com.lifetrace.order.ui

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.lifetrace.order.BuildConfig
import com.lifetrace.order.platform.PlatformSpec
import com.lifetrace.order.platform.WebViewSessionStore

@Composable
fun PlatformLoginScreen(
    spec: PlatformSpec,
    sessionStore: WebViewSessionStore,
    onDone: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(spec.loginUrl) }
    var loading by remember { mutableStateOf(true) }

    BackHandler {
        val view = webView
        if (view?.canGoBack() == true) view.goBack() else onClose()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onClose) { Text("关闭") }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(spec.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    Uri.parse(currentUrl).host ?: currentUrl,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
            Button(onClick = {
                sessionStore.flush()
                onDone()
            }) {
                Text("完成登录")
            }
        }
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                WebView(context).apply {
                    webView = this
                    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            loading = true
                            currentUrl = url ?: currentUrl
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                            currentUrl = url ?: currentUrl
                            sessionStore.flush()
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url ?: return false
                            // Keep platform authentication inside WebView; block non-web custom schemes.
                            return url.scheme != "https"
                        }
                    }
                    loadUrl(spec.loginUrl)
                }
            },
            update = { webView = it },
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            sessionStore.flush()
            webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
            webView = null
        }
    }
}
