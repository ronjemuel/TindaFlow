package com.tindaflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tindaflow.app.R
import com.tindaflow.app.data.*
import com.tindaflow.app.ui.TindaColors
import com.tindaflow.app.ui.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class OrderFilter { ALL, IN_PROGRESS, READY }

@Composable
fun OrdersScreen(store: DemoStore, onToast: (String) -> Unit) {
    var filter by remember { mutableStateOf(OrderFilter.ALL) }
    var search by remember { mutableStateOf("") }
    var create by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val orders = store.active.orders.filter { order ->
        (filter == OrderFilter.ALL || (filter == OrderFilter.IN_PROGRESS && order.status == OrderStatus.IN_PROGRESS) ||
            (filter == OrderFilter.READY && order.status == OrderStatus.READY)) &&
            (search.isBlank() || order.title.contains(search, true) || order.customer.contains(search, true) || order.ref.contains(search, true))
    }
    val opened = store.active.orders.filter { it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED }
    val openValue = opened.sumOf(ServiceOrder::amountCents)

    if (create) {
        CreateOrderDialog(onDismiss = { create = false }, onCreate = { title, customer, price ->
            store.addOrder(title, customer, price)
            create = false
            onToast("Demo quotation saved (not synced)")
        })
    }
    val selected = store.active.orders.find { it.ref == selectedId }
    if (selected != null) {
        AlertDialog(onDismissRequest = { selectedId = null },
            title = { Text(selected.title) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(selected.ref, color = TindaColors.Muted)
                Text("${stringResource(R.string.customer)}: ${selected.customer}")
                Text("${stringResource(R.string.total)}: ${money(selected.amountCents)}")
                Text("${stringResource(R.string.status)}: ${orderStatusLabel(selected.status)}")
                Text("${stringResource(R.string.due)}: ${selected.dueDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}")
                Text(stringResource(R.string.order_demo_note), color = TindaColors.Muted, fontSize = 11.sp)
                if (selected.status != OrderStatus.COMPLETED && selected.status != OrderStatus.CANCELLED) {
                    Text(stringResource(R.string.move_to), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    listOf(OrderStatus.CONFIRMED, OrderStatus.IN_PROGRESS, OrderStatus.READY, OrderStatus.COMPLETED).forEach { status ->
                        if (status != selected.status) TextButton(onClick = {
                            val idx = store.active.orders.indexOfFirst { it.ref == selected.ref }
                            if (idx >= 0) store.active.orders[idx] = selected.copy(status = status,
                                progress = if (status == OrderStatus.READY || status == OrderStatus.COMPLETED) 100 else if (status == OrderStatus.IN_PROGRESS) 60 else 0)
                            selectedId = null
                        }) { Text(orderStatusLabel(status)) }
                    }
                }
            } },
            confirmButton = { TextButton(onClick = { selectedId = null }) { Text(stringResource(R.string.close)) } }
        )
    }

    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { WorkspaceHeader(store, onToast) }
        item { PageTitle(stringResource(R.string.orders), stringResource(R.string.new_order), { create = true }) }
        item { HeroMetric(stringResource(R.string.open_orders_value), money(openValue), "${opened.size} ${stringResource(R.string.open_orders)}", chart = false) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("${stringResource(R.string.all)} ${store.active.orders.size}", filter == OrderFilter.ALL, { filter = OrderFilter.ALL })
                ChoiceChip("${stringResource(R.string.in_progress)} ${store.active.orders.count { it.status == OrderStatus.IN_PROGRESS }}",
                    filter == OrderFilter.IN_PROGRESS, { filter = OrderFilter.IN_PROGRESS })
                ChoiceChip("${stringResource(R.string.ready)} ${store.active.orders.count { it.status == OrderStatus.READY }}",
                    filter == OrderFilter.READY, { filter = OrderFilter.READY })
            }
        }
        item { SearchBox(search, { search = it }, stringResource(R.string.search_jobs)) }
        if (orders.isEmpty()) item { SurfacePanel { Text(stringResource(R.string.no_orders_found), color = TindaColors.Muted) } }
        items(orders, key = ServiceOrder::ref) { order ->
            SurfacePanel(Modifier.fillMaxWidth().clickable { selectedId = order.ref }) {
                Row(verticalAlignment = Alignment.Top) {
                    LittleIcon(Icons.Default.Assignment, TindaColors.Forest, Modifier.padding(top = 3.dp))
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(order.ref, fontSize = 10.sp, color = TindaColors.Muted)
                        Text(order.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(order.customer, fontSize = 12.sp, color = TindaColors.Muted, maxLines = 1)
                        Spacer(Modifier.height(6.dp))
                        Text(if (order.dueDate == LocalDate.now()) stringResource(R.string.due_today) else
                            "${stringResource(R.string.due)} ${order.dueDate.format(DateTimeFormatter.ofPattern("MMM d"))}",
                            fontSize = 11.sp,
                            color = if (order.dueDate <= LocalDate.now() && order.status != OrderStatus.COMPLETED) TindaColors.Red else TindaColors.Muted)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusPill(orderStatusLabel(order.status), orderStatusKind(order.status))
                        Text(money(order.amountCents), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (order.status == OrderStatus.IN_PROGRESS) {
                            Box(Modifier.width(85.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(TindaColors.Border)) {
                                Box(Modifier.fillMaxWidth(order.progress / 100f).fillMaxHeight().background(TindaColors.Forest))
                            }
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = TindaColors.Muted)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun orderStatusLabel(status: OrderStatus): String = when (status) {
    OrderStatus.QUOTATION -> stringResource(R.string.quotation)
    OrderStatus.CONFIRMED -> stringResource(R.string.confirmed)
    OrderStatus.IN_PROGRESS -> stringResource(R.string.in_progress)
    OrderStatus.READY -> stringResource(R.string.ready)
    OrderStatus.COMPLETED -> stringResource(R.string.completed)
    OrderStatus.CANCELLED -> stringResource(R.string.cancelled)
}

private fun orderStatusKind(status: OrderStatus): String = when (status) {
    OrderStatus.QUOTATION -> "amber"
    OrderStatus.CANCELLED -> "red"
    OrderStatus.COMPLETED -> "gray"
    else -> "green"
}

@Composable
private fun CreateOrderDialog(onDismiss: () -> Unit, onCreate: (String, String, Long) -> Unit) {
    var title by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.new_order)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.job_title)) }, singleLine = true)
            OutlinedTextField(customer, { customer = it }, label = { Text(stringResource(R.string.customer)) }, singleLine = true)
            OutlinedTextField(price, { price = it }, label = { Text(stringResource(R.string.price_in_pesos)) }, singleLine = true)
            if (error) Text(stringResource(R.string.check_input), color = TindaColors.Red, fontSize = 11.sp)
            Text(stringResource(R.string.new_order_note), color = TindaColors.Muted, fontSize = 11.sp)
        } },
        confirmButton = { TextButton(onClick = {
            val amount = parseMoneyCents(price)
            if (title.isBlank() || customer.isBlank() || amount == null || amount == 0L) error = true
            else onCreate(title.trim(), customer.trim(), amount)
        }) { Text(stringResource(R.string.create)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
