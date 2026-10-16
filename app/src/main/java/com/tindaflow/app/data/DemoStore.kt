package com.tindaflow.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ProductArt { FABRIC, FOAM, STAPLES, GLUE, WOOD, PACKAGE }
enum class PaymentStatus { PAID, PARTIAL, REFUNDED }
enum class OrderStatus { QUOTATION, CONFIRMED, IN_PROGRESS, READY, COMPLETED, CANCELLED }

data class Product(
    val id: Int,
    val name: String,
    val sku: String,
    val priceCents: Long,
    val stock: Int,
    val unit: String = "pcs",
    val lowThreshold: Int = 3,
    val art: ProductArt = ProductArt.PACKAGE
)

data class Sale(
    val ref: String,
    val customer: String,
    val totalCents: Long,
    val status: PaymentStatus,
    val createdAt: LocalDateTime,
    val items: Int,
    val paymentMethod: String = "Cash"
)

data class ServiceOrder(
    val ref: String,
    val title: String,
    val customer: String,
    val amountCents: Long,
    val status: OrderStatus,
    val dueDate: java.time.LocalDate,
    val progress: Int = 0
)

class BusinessWorkspace(
    val id: Int,
    val name: String,
    val kind: String,
    val initials: String,
    products: List<Product>,
    sales: List<Sale>,
    orders: List<ServiceOrder>,
    val expenseCents: Long,
    val dueCents: Long
) {
    val products = mutableStateListOf<Product>().apply { addAll(products) }
    val sales = mutableStateListOf<Sale>().apply { addAll(sales) }
    val orders = mutableStateListOf<ServiceOrder>().apply { addAll(orders) }
}

fun money(cents: Long): String = "₱" + DecimalFormat("#,##0.00").format(cents / 100.0)

fun parseMoneyCents(value: String): Long? = try {
    val amount = BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP)
    if (amount.signum() < 0) null else amount.movePointRight(2).longValueExact()
} catch (_: Exception) { null }

fun localTimeLabel(value: LocalDateTime): String = value.format(
    DateTimeFormatter.ofPattern("MMM d · h:mm a", Locale.ENGLISH)
)

class DemoStore {
    private val now = LocalDateTime.now()
    val workspaces = mutableStateListOf(
        BusinessWorkspace(
            id = 1, name = "Seatzwins Upholstery", kind = "Retail + Services", initials = "SU",
            products = listOf(
                Product(1, "Premium Velvet Fabric", "FAB-001", 52000, 12, "meters", art = ProductArt.FABRIC),
                Product(2, "High-density Foam", "FOM-008", 48000, 3, "pcs", lowThreshold = 4, art = ProductArt.FOAM),
                Product(3, "Staple Pins 10mm", "MAT-020", 9500, 48, "boxes", art = ProductArt.STAPLES),
                Product(4, "Wood Glue", "MAT-018", 16000, 0, "bottles", art = ProductArt.GLUE),
                Product(5, "Dining Chair Legs", "PAR-009", 76000, 8, "sets", art = ProductArt.WOOD),
                Product(6, "Upholstery Fabric", "FAB-002", 35000, 30, "meters", art = ProductArt.FABRIC),
                Product(7, "Foam Cushion", "FOM-009", 48000, 14, "pcs", art = ProductArt.FOAM)
            ),
            sales = listOf(
                Sale("SL-1042", "Walk-in purchase", 125000, PaymentStatus.PAID, now.minusMinutes(22), 2),
                Sale("SL-1041", "Maria Santos", 380000, PaymentStatus.PARTIAL, now.minusHours(1), 4, "Bank transfer"),
                Sale("SL-1040", "Jose Reyes", 98000, PaymentStatus.PAID, now.minusHours(3), 1),
                Sale("SL-1039", "Online order", 210000, PaymentStatus.REFUNDED, now.minusDays(1), 3),
                Sale("SL-1038", "Walk-in purchase", 54000, PaymentStatus.PAID, now.minusDays(2), 2)
            ),
            orders = listOf(
                ServiceOrder("ORD-2084", "Sofa reupholstery", "Maria Santos", 800000, OrderStatus.IN_PROGRESS, now.toLocalDate().plusDays(3), 60),
                ServiceOrder("ORD-2083", "Dining chair restoration", "Jose Reyes", 450000, OrderStatus.READY, now.toLocalDate(), 100),
                ServiceOrder("ORD-2082", "Car seat repair", "Alvin Cruz", 220000, OrderStatus.QUOTATION, now.toLocalDate().plusDays(5)),
                ServiceOrder("ORD-2081", "Bed frame upholstery", "Carla Dizon", 690000, OrderStatus.COMPLETED, now.toLocalDate().minusDays(3), 100)
            ),
            expenseCents = 325000,
            dueCents = 460000
        ),
        BusinessWorkspace(
            id = 2, name = "Tinda Mart", kind = "Retail", initials = "TM",
            products = listOf(
                Product(101, "Coffee Sachets", "GRC-101", 1200, 96, "pcs"),
                Product(102, "Bottled Water", "DRK-102", 2000, 38, "bottles"),
                Product(103, "Rice 1kg", "GRC-103", 5800, 3, "bags", lowThreshold = 5),
                Product(104, "Laundry Soap", "HOM-104", 3500, 17, "pcs")
            ),
            sales = listOf(
                Sale("SL-0201", "Walk-in purchase", 25900, PaymentStatus.PAID, now.minusMinutes(49), 5),
                Sale("SL-0200", "Walk-in purchase", 7600, PaymentStatus.PAID, now.minusHours(2), 3)
            ),
            orders = emptyList(), expenseCents = 27000, dueCents = 0
        )
    )

    var activeIndex by mutableIntStateOf(0)
        private set
    val active: BusinessWorkspace get() = workspaces[activeIndex]
    val cart = mutableStateMapOf<Int, Int>()
    var discountCents by androidx.compose.runtime.mutableLongStateOf(0)
    var selectedCustomer by androidx.compose.runtime.mutableStateOf("Walk-in customer")
    var selectedPayment by androidx.compose.runtime.mutableStateOf("Cash")

    fun switchBusiness(index: Int) {
        if (index !in workspaces.indices) return
        activeIndex = index
        resetCart()
    }

    fun resetCart() {
        cart.clear()
        discountCents = 0
        selectedCustomer = "Walk-in customer"
        selectedPayment = "Cash"
    }

    val subtotalCents: Long get() = cart.entries.sumOf { (id, qty) ->
        (active.products.find { it.id == id }?.priceCents ?: 0L) * qty
    }

    val totalCents: Long get() = (subtotalCents - discountCents).coerceAtLeast(0)
    val cartCount: Int get() = cart.values.sum()

    fun updateQuantity(id: Int, change: Int): String? {
        val product = active.products.find { it.id == id } ?: return "Product unavailable"
        val next = (cart[id] ?: 0) + change
        if (next > product.stock) return "Only ${product.stock} ${product.unit} available"
        if (next <= 0) cart.remove(id) else cart[id] = next
        if (discountCents > subtotalCents) discountCents = subtotalCents
        return null
    }

    /** Purely in-memory demonstration: not persisted and not an actual financial transaction. */
    fun completeDemoSale(): String? {
        if (cart.isEmpty()) return "Add at least one product"
        if (discountCents > subtotalCents) return "Discount exceeds subtotal"
        val business = active
        for ((id, qty) in cart) {
            val product = business.products.find { it.id == id } ?: return "Product is missing"
            if (qty > product.stock) return "Insufficient stock for ${product.name}"
        }
        for ((id, qty) in cart) {
            val index = business.products.indexOfFirst { it.id == id }
            business.products[index] = business.products[index].copy(stock = business.products[index].stock - qty)
        }
        val maxRef = business.sales.mapNotNull { it.ref.removePrefix("SL-").toIntOrNull() }.maxOrNull() ?: 1000
        business.sales.add(0, Sale(
            ref = "SL-${maxRef + 1}", customer = selectedCustomer,
            totalCents = totalCents, status = PaymentStatus.PAID,
            createdAt = LocalDateTime.now(), items = cartCount, paymentMethod = selectedPayment
        ))
        resetCart()
        return null
    }

    fun addProduct(name: String, sku: String, priceCents: Long, stock: Int) {
        val nextId = (active.products.maxOfOrNull { it.id } ?: 0) + 1
        active.products.add(0, Product(nextId, name, sku, priceCents, stock))
    }

    fun adjustDemoStock(productId: Int, delta: Int): Boolean {
        val index = active.products.indexOfFirst { it.id == productId }
        if (index < 0) return false
        val item = active.products[index]
        if (item.stock + delta < 0) return false
        active.products[index] = item.copy(stock = item.stock + delta)
        return true
    }

    fun addOrder(title: String, customer: String, amountCents: Long) {
        val maxRef = active.orders.mapNotNull { it.ref.removePrefix("ORD-").toIntOrNull() }.maxOrNull() ?: 2000
        active.orders.add(0, ServiceOrder("ORD-${maxRef + 1}", title, customer, amountCents, OrderStatus.QUOTATION, java.time.LocalDate.now().plusDays(7)))
    }
}
