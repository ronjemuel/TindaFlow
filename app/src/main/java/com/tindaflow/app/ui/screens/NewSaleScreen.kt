package com.tindaflow.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tindaflow.app.R
import com.tindaflow.app.data.*
import com.tindaflow.app.ui.TindaColors
import com.tindaflow.app.ui.components.*

@Composable
fun NewSaleScreen(store: DemoStore, onBack: () -> Unit, onSaved: () -> Unit, onToast: (String) -> Unit) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var showDiscount by remember { mutableStateOf(false) }
    var showCustomer by remember { mutableStateOf(false) }
    var showPayment by remember { mutableStateOf(false) }
    val available = store.active.products.filter { product ->
        (category == "All" || (category == "Fabric" && product.art == ProductArt.FABRIC) ||
                (category == "Materials" && product.art != ProductArt.FABRIC)) &&
            (search.isBlank() || product.name.contains(search, true) || product.sku.contains(search, true))
    }
    val itemsInCart = store.cart.entries.mapNotNull { (id, quantity) ->
        store.active.products.find { it.id == id }?.let { it to quantity }
    }

    if (showDiscount) {
        var amount by remember { mutableStateOf((store.discountCents / 100.0).toString()) }
        AlertDialog(onDismissRequest = { showDiscount = false }, title = { Text(stringResource(R.string.discount)) },
            text = {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(stringResource(R.string.discount_amount)) }, prefix = { Text("₱") })
            },
            confirmButton = { TextButton(onClick = {
                val value = parseMoneyCents(amount)
                if (value == null || value > store.subtotalCents) onToast("Enter a valid discount below the subtotal")
                else { store.discountCents = value; showDiscount = false }
            }) { Text(stringResource(R.string.apply)) } },
            dismissButton = { TextButton(onClick = { showDiscount = false }) { Text(stringResource(R.string.cancel)) } })
    }
    if (showCustomer) {
        AlertDialog(onDismissRequest = { showCustomer = false }, title = { Text(stringResource(R.string.customer)) },
            text = { Column {
                listOf("Walk-in customer", "Maria Santos", "Jose Reyes", "Alvin Cruz").forEach { name ->
                    TextButton(onClick = { store.selectedCustomer = name; showCustomer = false }) {
                        Text(name, modifier = Modifier.fillMaxWidth(), color = TindaColors.Ink)
                    }
                }
            } }, confirmButton = { TextButton(onClick = { showCustomer = false }) { Text(stringResource(R.string.close)) } })
    }
    if (showPayment) {
        AlertDialog(onDismissRequest = { showPayment = false }, title = { Text(stringResource(R.string.payment)) },
            text = { Column {
                listOf("Cash", "Bank transfer", "Other manually recorded").forEach { method ->
                    TextButton(onClick = { store.selectedPayment = method; showPayment = false }) {
                        Text(method, modifier = Modifier.fillMaxWidth(), color = TindaColors.Ink)
                    }
                }
                Text(stringResource(R.string.manual_payment_note), color = TindaColors.Muted, fontSize = 11.sp)
            } }, confirmButton = { TextButton(onClick = { showPayment = false }) { Text(stringResource(R.string.close)) } })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back)) }
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.new_sale), style = MaterialTheme.typography.headlineMedium)
                }
            }
            item { SearchBox(search, { search = it }, stringResource(R.string.search_products)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Fabric", "Materials").forEach { name ->
                        ChoiceChip(name, name == category, { category = name })
                    }
                }
            }
            if (available.isEmpty()) item { SurfacePanel { Text(stringResource(R.string.no_products_found), color = TindaColors.Muted) } }
            items(available, key = { "product-${it.id}" }) { product ->
                SurfacePanel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProductPhoto(product.art, Modifier.size(58.dp))
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${money(product.priceCents)} / ${product.unit}", fontSize = 12.sp, color = TindaColors.Muted)
                            if (product.stock == 0) Text(stringResource(R.string.out_of_stock), fontSize = 10.sp, color = TindaColors.Red)
                        }
                        FilledIconButton(onClick = { store.updateQuantity(product.id, 1)?.let(onToast) },
                            enabled = product.stock > 0,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = TindaColors.Forest),
                            modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_product), modifier = Modifier.size(23.dp))
                        }
                    }
                }
            }
            item { SectionTitle("${stringResource(R.string.cart)} · ${store.cartCount} ${stringResource(R.string.items_label)}",
                if (store.cart.isNotEmpty()) stringResource(R.string.clear_all) else null) { store.resetCart() } }
            if (itemsInCart.isEmpty()) {
                item { SurfacePanel { Text(stringResource(R.string.cart_empty), color = TindaColors.Muted) } }
            } else {
                item {
                    SurfacePanel {
                        itemsInCart.forEachIndexed { index, (product, qty) ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                ProductPhoto(product.art, Modifier.size(45.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(money(product.priceCents * qty), fontSize = 11.sp, color = TindaColors.Muted)
                                }
                                IconButton(onClick = { store.updateQuantity(product.id, -1) }, modifier = Modifier.size(31.dp)) {
                                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.decrease), modifier = Modifier.size(17.dp))
                                }
                                Text("$qty", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                IconButton(onClick = { store.updateQuantity(product.id, 1)?.let(onToast) }, modifier = Modifier.size(31.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.increase), modifier = Modifier.size(17.dp))
                                }
                                IconButton(onClick = { store.cart.remove(product.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.remove), tint = TindaColors.Muted, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (index < itemsInCart.lastIndex) LineSeparator()
                        }
                    }
                }
            }
            item {
                SurfacePanel {
                    SummaryLine(stringResource(R.string.subtotal), money(store.subtotalCents))
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().clickable { showDiscount = true }, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.discount), fontSize = 13.sp, color = TindaColors.Muted, modifier = Modifier.weight(1f))
                        Text("−${money(store.discountCents)}", fontSize = 13.sp, color = TindaColors.Forest)
                        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = TindaColors.Muted)
                    }
                    Spacer(Modifier.height(13.dp))
                    LineSeparator()
                    Spacer(Modifier.height(12.dp))
                    SummaryLine(stringResource(R.string.total), money(store.totalCents), emphasized = true)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SurfacePanel(Modifier.weight(1f).clickable { showCustomer = true }) {
                        Text(stringResource(R.string.customer), color = TindaColors.Muted, fontSize = 10.sp)
                        Text(store.selectedCustomer, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                    SurfacePanel(Modifier.weight(1f).clickable { showPayment = true }) {
                        Text(stringResource(R.string.payment), color = TindaColors.Muted, fontSize = 10.sp)
                        Text(store.selectedPayment, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                }
            }
        }
        SurfacePanel(Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    val problem = store.completeDemoSale()
                    if (problem == null) onSaved() else onToast(problem)
                },
                modifier = Modifier.fillMaxWidth().height(53.dp),
                enabled = store.cartCount > 0,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TindaColors.Forest)
            ) {
                Icon(Icons.Default.ShoppingBag, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.confirm_demo_sale), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, amount: String, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = if (emphasized) 17.sp else 13.sp,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f), color = if (emphasized) TindaColors.Ink else TindaColors.Muted)
        Text(amount, fontSize = if (emphasized) 22.sp else 13.sp, fontWeight = FontWeight.Bold)
    }
}
