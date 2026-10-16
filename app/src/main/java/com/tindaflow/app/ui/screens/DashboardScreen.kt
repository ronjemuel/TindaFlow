package com.tindaflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tindaflow.app.MainTab
import com.tindaflow.app.R
import com.tindaflow.app.data.*
import com.tindaflow.app.ui.TindaColors
import com.tindaflow.app.ui.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(store: DemoStore, onNewSale: () -> Unit, onTab: (MainTab) -> Unit, onToast: (String) -> Unit) {
    val workspace = store.active
    val todaySales = workspace.sales.filter { it.createdAt.toLocalDate() == LocalDate.now() && it.status != PaymentStatus.REFUNDED }.sumOf { it.totalCents }
    val lowStock = workspace.products.count { it.stock <= it.lowThreshold }
    val dueToday = workspace.orders.count { it.dueDate <= LocalDate.now() && it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED }
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { WorkspaceHeader(store, onToast) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.good_morning), color = TindaColors.Muted, fontSize = 14.sp)
                Text(stringResource(R.string.business_overview), style = MaterialTheme.typography.headlineLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(15.dp), tint = TindaColors.Forest)
                    Spacer(Modifier.width(5.dp))
                    Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d")), fontSize = 12.sp, color = TindaColors.Forest, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(R.string.demo_not_synced), fontSize = 10.sp, color = TindaColors.Muted)
                }
            }
        }
        item { HeroMetric(stringResource(R.string.sales_today), money(todaySales), stringResource(R.string.demo_activity)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SurfacePanel(Modifier.weight(1f)) {
                    LittleIcon(Icons.Default.AccountBalanceWallet, TindaColors.Red)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.expenses), fontSize = 12.sp, color = TindaColors.Muted)
                    Text(money(workspace.expenseCents), fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 1)
                }
                SurfacePanel(Modifier.weight(1f)) {
                    LittleIcon(Icons.Default.ReceiptLong, TindaColors.Amber)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.unpaid), fontSize = 12.sp, color = TindaColors.Muted)
                    Text(money(workspace.dueCents), fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 1)
                }
            }
        }
        item { SectionTitle(stringResource(R.string.quick_actions)) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                DashboardAction(Icons.Default.ShoppingCart, stringResource(R.string.new_sale), true, onNewSale, Modifier.weight(1f))
                DashboardAction(Icons.Default.Assignment, stringResource(R.string.new_order), false, { onTab(MainTab.ORDERS) }, Modifier.weight(1f))
                DashboardAction(Icons.Default.Inventory2, stringResource(R.string.add_product), false, { onTab(MainTab.INVENTORY) }, Modifier.weight(1f))
            }
        }
        item { SectionTitle(stringResource(R.string.needs_attention)) }
        item {
            SurfacePanel {
                MiniActivity(Icons.Default.Inventory2, stringResource(R.string.low_stock_alert),
                    "$lowStock ${stringResource(R.string.items_to_review)}", TindaColors.Amber,
                    { onTab(MainTab.INVENTORY) })
                LineSeparator()
                MiniActivity(Icons.Default.Assignment, stringResource(R.string.orders_due),
                    "$dueToday ${stringResource(R.string.orders_due_today)}", TindaColors.Forest,
                    { onTab(MainTab.ORDERS) })
            }
        }
        item { SectionTitle(stringResource(R.string.live_activity), stringResource(R.string.see_all)) { onTab(MainTab.SALES) } }
        item {
            SurfacePanel {
                val latest = workspace.sales.firstOrNull()
                if (latest != null) {
                    MiniActivity(Icons.Default.ShoppingCart, "Sale #${latest.ref}",
                        "${latest.customer} · ${money(latest.totalCents)}", TindaColors.Forest,
                        { onTab(MainTab.SALES) })
                }
                if (workspace.orders.isNotEmpty()) {
                    LineSeparator()
                    val latestOrder = workspace.orders.first()
                    MiniActivity(Icons.Default.Assignment, "Order ${latestOrder.ref}",
                        "${latestOrder.title} · ${latestOrder.customer}", TindaColors.Forest,
                        { onTab(MainTab.ORDERS) })
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun DashboardAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, primary: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.height(102.dp).clip(RoundedCornerShape(17.dp))
            .background(if (primary) TindaColors.Forest else Color.White)
            .clickable(onClick = onClick).padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(icon, null, tint = if (primary) Color.White else TindaColors.Forest, modifier = Modifier.size(25.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            color = if (primary) Color.White else TindaColors.Ink)
    }
}

@Composable
private fun MiniActivity(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, caption: String, tint: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        LittleIcon(icon, tint)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(caption, fontSize = 11.sp, color = TindaColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TindaColors.Muted, modifier = Modifier.size(19.dp))
    }
}
