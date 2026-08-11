package com.lifetrace.order.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifetrace.order.AppContainer
import com.lifetrace.order.data.entity.PlatformAccountEntity
import com.lifetrace.order.domain.UnifiedOrder
import com.lifetrace.order.platform.AuthState
import com.lifetrace.order.platform.BackfillRange
import com.lifetrace.order.platform.PlatformSpec
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderApp(container: AppContainer) {
    val orders by container.repository.observeRecentOrders().collectAsState(initial = emptyList())
    val accounts by container.repository.observeAccounts().collectAsState(initial = emptyList())
    val syncState by container.syncManager.state.collectAsState()
    val scope = rememberCoroutineScope()
    var loginSpec by remember { mutableStateOf<PlatformSpec?>(null) }

    val activeLogin = loginSpec
    if (activeLogin != null) {
        PlatformLoginScreen(
            spec = activeLogin,
            sessionStore = container.sessionStore,
            onClose = { loginSpec = null },
            onDone = {
                scope.launch(Dispatchers.IO) {
                    container.repository.saveAccount(
                        PlatformAccountEntity(
                            platform = activeLogin.id.wireValue,
                            accountId = "primary",
                            displayName = "默认账户",
                            connected = true,
                            lastAuthState = AuthState.UNKNOWN.name,
                            lastAuthCheckAtEpochMs = System.currentTimeMillis(),
                        ),
                    )
                    container.syncManager.requestImmediateRefresh(activeLogin.id)
                }
                loginSpec = null
            },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("LifeTrace Order")
                        Text(
                            syncState.lastMessage,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { container.syncManager.requestImmediateRefresh() }) {
                        Text("刷新")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SyncStatusCard(
                    foreground = syncState.foreground,
                    realtimeCount = syncState.activeRealtimeDeliveries,
                    lastSuccessAt = syncState.lastSuccessAtEpochMs,
                )
            }

            item {
                Text("平台连接", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            items(container.platformRegistry.all(), key = { it.spec.id.wireValue }) { adapter ->
                val account = accounts.firstOrNull { it.platform == adapter.spec.id.wireValue && it.connected }
                PlatformCard(
                    spec = adapter.spec,
                    connected = account != null,
                    onLogin = { loginSpec = adapter.spec },
                    onRefresh = { container.syncManager.requestImmediateRefresh(adapter.spec.id) },
                    onMonthBackfill = {
                        account?.let { container.syncManager.startBackfill(adapter.spec.id, it.accountId, BackfillRange.ONE_MONTH) }
                    },
                    onYearBackfill = {
                        account?.let { container.syncManager.startBackfill(adapter.spec.id, it.accountId, BackfillRange.ONE_YEAR) }
                    },
                )
            }

            item { NotificationAccessCard() }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("最近订单", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${orders.size} 条", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (orders.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "还没有本地订单。先连接平台；真实订单 Fetch Mode 会在真机 PoC 后逐个平台固化。",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                items(orders, key = { it.stableKey }) { order -> OrderRow(order) }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SyncStatusCard(foreground: Boolean, realtimeCount: Int, lastSuccessAt: Long?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (foreground) "前台同步已启用" else "后台主动轮询已停止", fontWeight = FontWeight.Bold)
            Text(if (realtimeCount > 0) "外卖实时追踪：$realtimeCount 个订单（15 秒）" else "普通订单：前台 5 分钟级刷新")
            Text("最近成功：${formatTime(lastSuccessAt)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PlatformCard(
    spec: PlatformSpec,
    connected: Boolean,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
    onMonthBackfill: () -> Unit,
    onYearBackfill: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(spec.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (connected) "已配置本地登录容器" else "未连接", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = onLogin) { Text(if (connected) "重新验证" else "登录") }
            }
            if (connected) {
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRefresh) { Text("立即刷新") }
                    OutlinedButton(onClick = onMonthBackfill) { Text("回填 1 月") }
                    OutlinedButton(onClick = onYearBackfill) { Text("回填 1 年") }
                }
            }
        }
    }
}

@Composable
private fun NotificationAccessCard() {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("通知驱动刷新", fontWeight = FontWeight.Bold)
            Text("通知仅作为“可能有订单变化”的信号；不会直接把通知文案写成订单状态。")
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }) {
                Text("打开通知访问设置")
            }
        }
    }
}

@Composable
private fun OrderRow(order: UnifiedOrder) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.merchantName.ifBlank { order.platform.wireValue }, fontWeight = FontWeight.Bold)
                Text(formatMoney(order.amountMinor, order.currency))
            }
            Text("${order.platform.wireValue} · ${order.status.name}", style = MaterialTheme.typography.bodySmall)
            Text("订单 ${order.platformOrderId}", style = MaterialTheme.typography.bodySmall)
            Text(formatTime(order.orderedAtEpochMs), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatMoney(amountMinor: Long, currency: String): String =
    if (currency == "CNY") String.format(Locale.CHINA, "¥%.2f", amountMinor / 100.0)
    else "$currency ${String.format(Locale.US, "%.2f", amountMinor / 100.0)}"

private fun formatTime(epochMs: Long?): String =
    epochMs?.let { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it)) } ?: "—"
