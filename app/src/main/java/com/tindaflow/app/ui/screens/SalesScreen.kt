package com.tindaflow.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun SalesScreen(store: DemoStore, onNewSale: () -> Unit, onToast: (String) -> Unit) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<PaymentStatus?>(null) }
    var newest by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<Sale?>(null) }
    val workspace = store.active
    val allSales = workspace.sales.toList()
    val sales = allSales.filter { sale ->
        (filter == null || sale.status == filter) &&
            (search.isBlank() || sale.ref.contains(search, true) || sale.customer.contains(search, true))
    }.let { if (newest) it.sortedByDescending(Sale::createdAt) else it.sortedBy(Sale::createdAt) }
    val periodTotal = allSales.filter { it.createdAt.toLocalDate().month == LocalDate.now().month && it.createdAt.year == LocalDate.now().year && it.status != PaymentStatus.REFUNDED }.sumOf(Sale::totalCents)

    selected?.let { sale ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("#${sale.ref}") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(sale.customer)
                Text("${stringResource(R.string.total)}: ${money(sale.totalCents)}")
                Text("${stringResource(R.string.status)}: ${statusLabel(sale.status)}")
                Text("${stringResource(R.string.payment)}: ${sale.paymentMethod}")
                Text(localTimeLabel(sale.createdAt))
                Text(stringResource(R.string.demo_not_synced), fontSize = 12.sp, color = TindaColors.Muted)
            } },
            confirmButton = { TextButton(onClick = { selected = null }) { Text(stringResource(R.string.close)) } }
        )
    }
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { WorkspaceHeader(store, onToast) }
        item { PageTitle(stringResource(R.string.sales), stringResource(R.string.new_sale), onNewSale) }
        item { SearchBox(search, { search = it }, stringResource(R.string.search_sales)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(stringResource(R.string.all), filter == null, { filter = null })
                ChoiceChip(stringResource(R.string.paid), filter == PaymentStatus.PAID, { filter = PaymentStatus.PAID })
                ChoiceChip(stringResource(R.string.partial), filter == PaymentStatus.PARTIAL, { filter = PaymentStatus.PARTIAL })
                ChoiceChip(stringResource(R.string.refunded), filter == PaymentStatus.REFUNDED, { filter = PaymentStatus.REFUNDED })
            }
        }
        item { HeroMetric(stringResource(R.string.total_sales_this_month), money(periodTotal), "${allSales.size} ${stringResource(R.string.transactions)}") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.transactions), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { newest = !newest }) {
                    Text(if (newest) stringResource(R.string.newest_first) else stringResource(R.string.oldest_first), fontSize = 12.sp, color = TindaColors.Muted)
                }
            }
        }
        if (sales.isEmpty()) item { SurfacePanel { Text(stringResource(R.string.no_sales_found), color = TindaColors.Muted) } }
        items(sales, key = { it.ref }) { sale ->
            SurfacePanel(Modifier.fillMaxWidth().clickable { selected = sale }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LittleIcon(if (sale.status == PaymentStatus.REFUNDED) Icons.Default.ReceiptLong else Icons.Default.ShoppingCart,
                        if (sale.status == PaymentStatus.REFUNDED) TindaColors.Red else TindaColors.Forest)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("#${sale.ref}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(sale.customer, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = TindaColors.Muted)
                        Spacer(Modifier.height(4.dp))
                        Text(localTimeLabel(sale.createdAt), fontSize = 10.sp, color = TindaColors.Muted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(money(sale.totalCents), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        StatusPill(statusLabel(sale.status), when (sale.status) {
                            PaymentStatus.PAID -> "green"
                            PaymentStatus.PARTIAL -> "amber"
                            PaymentStatus.REFUNDED -> "red"
                        })
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = TindaColors.Muted, modifier = Modifier.size(20.dp))
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
fun statusLabel(status: PaymentStatus): String = when (status) {
    PaymentStatus.PAID -> stringResource(R.string.paid)
    PaymentStatus.PARTIAL -> stringResource(R.string.partial)
    PaymentStatus.REFUNDED -> stringResource(R.string.refunded)
}
