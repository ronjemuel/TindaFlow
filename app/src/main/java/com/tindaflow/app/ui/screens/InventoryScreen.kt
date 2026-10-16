package com.tindaflow.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
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

private enum class StockFilter { ALL, LOW, OUT }

@Composable
fun InventoryScreen(store: DemoStore, onToast: (String) -> Unit) {
    var filter by remember { mutableStateOf(StockFilter.ALL) }
    var search by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<Int?>(null) }
    val workspace = store.active
    val products = workspace.products.filter { product ->
        (search.isBlank() || product.name.contains(search, true) || product.sku.contains(search, true)) && when(filter) {
            StockFilter.ALL -> true
            StockFilter.LOW -> product.stock in 1..product.lowThreshold
            StockFilter.OUT -> product.stock == 0
        }
    }
    val lowCount = workspace.products.count { it.stock in 1..it.lowThreshold }

    if (showCreate) {
        CreateProductDialog(onDismiss = { showCreate = false }, onCreate = { name, sku, cents, stock ->
            if (workspace.products.any { it.sku.equals(sku, true) }) {
                onToast("SKU already exists in this workspace")
            } else {
                store.addProduct(name, sku, cents, stock)
                showCreate = false
                onToast("Demo product added (not synced)")
            }
        })
    }
    val selected = workspace.products.firstOrNull { it.id == selectedId }
    if (selectedId != null && selected == null) selectedId = null
    if (selected != null) {
        AlertDialog(onDismissRequest = { selectedId = null },
            title = { Text(selected.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProductPhoto(selected.art, Modifier.size(66.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("SKU ${selected.sku}", fontSize = 12.sp, color = TindaColors.Muted)
                            Text(money(selected.priceCents), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                    Text("${stringResource(R.string.stock)}: ${selected.stock} ${selected.unit}")
                    Text(stringResource(R.string.adjust_demo_stock), fontSize = 12.sp, color = TindaColors.Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = {
                            if (!store.adjustDemoStock(selected.id, -1)) onToast("Stock cannot be negative")
                        }) { Text("− 1") }
                        OutlinedButton(onClick = { store.adjustDemoStock(selected.id, 1) }) { Text("+ 1") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedId = null }) { Text(stringResource(R.string.close)) } }
        )
    }

    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { WorkspaceHeader(store, onToast) }
        item { PageTitle(stringResource(R.string.inventory), stringResource(R.string.add_product), { showCreate = true }) }
        item { SearchBox(search, { search = it }, stringResource(R.string.search_inventory)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ChoiceChip(stringResource(R.string.all), filter == StockFilter.ALL, { filter = StockFilter.ALL })
                ChoiceChip(stringResource(R.string.low_stock), filter == StockFilter.LOW, { filter = StockFilter.LOW })
                ChoiceChip(stringResource(R.string.out_of_stock), filter == StockFilter.OUT, { filter = StockFilter.OUT })
            }
        }
        item {
            SurfacePanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LittleIcon(Icons.Default.Inventory2)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("${workspace.products.size} ${stringResource(R.string.products)} · $lowCount ${stringResource(R.string.low_stock).lowercase()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(stringResource(R.string.inventory_subtitle), color = TindaColors.Muted, fontSize = 11.sp)
                    }
                }
            }
        }
        if (products.isEmpty()) item { SurfacePanel { Text(stringResource(R.string.no_products_found), color = TindaColors.Muted) } }
        items(products, key = Product::id) { product ->
            SurfacePanel(Modifier.fillMaxWidth().clickable { selectedId = product.id }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProductPhoto(product.art, Modifier.size(60.dp))
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(product.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text("SKU ${product.sku}", fontSize = 11.sp, color = TindaColors.Muted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${product.stock} ${product.unit}", fontSize = 11.sp, color = TindaColors.Muted)
                        Spacer(Modifier.height(3.dp))
                        if (product.stock == 0) StatusPill(stringResource(R.string.out_of_stock), "red")
                        else if (product.stock <= product.lowThreshold) StatusPill(stringResource(R.string.low_stock), "amber")
                        Text(money(product.priceCents), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = TindaColors.Muted)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun CreateProductDialog(onDismiss: () -> Unit, onCreate: (String, String, Long, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_product)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.product_name)) }, singleLine = true)
                OutlinedTextField(sku, { sku = it }, label = { Text("SKU") }, singleLine = true)
                OutlinedTextField(price, { price = it }, label = { Text(stringResource(R.string.price_in_pesos)) }, singleLine = true)
                OutlinedTextField(stock, { stock = it }, label = { Text(stringResource(R.string.quantity)) }, singleLine = true)
                if (error) Text(stringResource(R.string.check_input), color = TindaColors.Red, fontSize = 11.sp)
                Text(stringResource(R.string.demo_not_synced), color = TindaColors.Muted, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = {
            val cents = parseMoneyCents(price)
            val qty = stock.toIntOrNull()
            if (name.isBlank() || sku.isBlank() || cents == null || qty == null || qty < 0) error = true
            else onCreate(name.trim(), sku.trim().uppercase(), cents, qty)
        }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
