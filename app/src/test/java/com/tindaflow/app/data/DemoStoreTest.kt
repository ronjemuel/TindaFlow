package com.tindaflow.app.data

import org.junit.Assert.*
import org.junit.Test

class DemoStoreTest {
    @Test fun changingBusinessClearsCartAndSeparatesProducts() {
        val store = DemoStore()
        assertNull(store.updateQuantity(6, 2))
        assertEquals(2, store.cartCount)
        store.switchBusiness(1)
        assertEquals(0, store.cartCount)
        assertNull(store.active.products.find { it.id == 6 })
        store.switchBusiness(0)
        assertNotNull(store.active.products.find { it.id == 6 })
    }

    @Test fun checkoutDecrementsOnlySoldProducts() {
        val store = DemoStore()
        val startingStock = store.active.products.first { it.id == 6 }.stock
        val originalSalesCount = store.active.sales.size
        assertNull(store.updateQuantity(6, 2))
        assertNull(store.completeDemoSale())
        assertEquals(startingStock - 2, store.active.products.first { it.id == 6 }.stock)
        assertEquals(originalSalesCount + 1, store.active.sales.size)
        assertEquals(0, store.cartCount)
    }

    @Test fun checkoutDoesNotRunTwiceForEmptyCart() {
        val store = DemoStore()
        assertNull(store.updateQuantity(6, 1))
        assertNull(store.completeDemoSale())
        val count = store.active.sales.size
        assertNotNull(store.completeDemoSale())
        assertEquals(count, store.active.sales.size)
    }

    @Test fun cannotOversell() {
        val store = DemoStore()
        assertNotNull(store.updateQuantity(4, 1))
        assertEquals(0, store.cartCount)
        assertNotNull(store.updateQuantity(2, 4))
        assertEquals(0, store.cartCount)
    }

    @Test fun discountMustBeNoMoreThanSubtotal() {
        val store = DemoStore()
        assertNull(store.updateQuantity(6, 1))
        store.discountCents = store.subtotalCents + 1
        assertNotNull(store.completeDemoSale())
    }

    @Test fun negativeStockIsRejected() {
        val store = DemoStore()
        assertFalse(store.adjustDemoStock(4, -1))
        assertEquals(0, store.active.products.first { it.id == 4 }.stock)
    }

    @Test fun currencyParserUsesIntegerCentValues() {
        assertEquals(125099L, parseMoneyCents("1250.99"))
        assertNull(parseMoneyCents("-1"))
        assertNull(parseMoneyCents("abc"))
    }
}
