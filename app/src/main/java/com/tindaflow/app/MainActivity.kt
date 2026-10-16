package com.tindaflow.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.viewModels
import com.tindaflow.app.auth.AuthViewModel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tindaflow.app.data.DemoStore
import com.tindaflow.app.ui.TindaColors
import com.tindaflow.app.ui.TindaFlowTheme
import com.tindaflow.app.ui.screens.*
import com.tindaflow.app.navigation.TindaRoot
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val authModel: AuthViewModel by viewModels { AuthViewModel.factory(applicationContext) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authModel.start()
        consumeAuthIntent(intent)
        setContent { TindaFlowTheme { TindaRoot(authModel) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeAuthIntent(intent)
    }

    private fun consumeAuthIntent(incoming: Intent) {
        val callback = incoming.dataString ?: return
        incoming.data = null // Do not retain credential-bearing URIs in Activity state.
        if (callback.startsWith("tindaflow:")) authModel.handleCallback(callback)
    }
}

enum class MainTab { HOME, SALES, ORDERS, INVENTORY, MORE }

@Composable
fun TindaApp(store: DemoStore = remember { DemoStore() }, onProfile: (() -> Unit)? = null) {
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var checkout by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val announce: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) } }

    BackHandler(enabled = checkout) { checkout = false }

    Scaffold(
        containerColor = TindaColors.Canvas,
        topBar = {
            if (onProfile != null) {
                Surface(color = TindaColors.LightGreen) {
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(stringResource(R.string.m2_demo_banner), modifier = Modifier.weight(1f),
                            color = TindaColors.Forest, style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = onProfile) { Text(stringResource(R.string.m2_profile)) }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!checkout) {
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    val tabs = listOf(
                        Triple(MainTab.HOME, stringResource(R.string.home), Icons.Default.Home),
                        Triple(MainTab.SALES, stringResource(R.string.sales), Icons.Default.BarChart),
                        Triple(MainTab.ORDERS, stringResource(R.string.orders), Icons.Default.Assignment),
                        Triple(MainTab.INVENTORY, stringResource(R.string.inventory), Icons.Default.Inventory2),
                        Triple(MainTab.MORE, stringResource(R.string.more), Icons.Default.MoreHoriz)
                    )
                    tabs.forEach { (page, label, icon) ->
                        NavigationBarItem(
                            selected = tab == page,
                            onClick = { if (page == MainTab.MORE) {
                                if (onProfile != null) onProfile() else announce("More screens are next · design milestone")
                            } else tab = page },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TindaColors.Deep,
                                selectedTextColor = TindaColors.Deep,
                                indicatorColor = TindaColors.LightGreen,
                                unselectedIconColor = TindaColors.Muted,
                                unselectedTextColor = TindaColors.Muted
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (checkout) {
                NewSaleScreen(
                    store = store,
                    onBack = { checkout = false },
                    onSaved = {
                        checkout = false
                        tab = MainTab.SALES
                        announce("Demo sale saved · inventory updated (not synced)")
                    },
                    onToast = announce
                )
            } else {
                when (tab) {
                    MainTab.HOME -> DashboardScreen(store,
                        onNewSale = { store.resetCart(); checkout = true },
                        onTab = { tab = it }, onToast = announce)
                    MainTab.SALES -> SalesScreen(store,
                        onNewSale = { store.resetCart(); checkout = true }, onToast = announce)
                    MainTab.ORDERS -> OrdersScreen(store, onToast = announce)
                    MainTab.INVENTORY -> InventoryScreen(store, onToast = announce)
                    MainTab.MORE -> DashboardScreen(store,
                        onNewSale = { checkout = true }, onTab = { tab = it }, onToast = announce)
                }
            }
        }
    }
}
