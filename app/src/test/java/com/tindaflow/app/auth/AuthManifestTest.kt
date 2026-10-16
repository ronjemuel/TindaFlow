package com.tindaflow.app.auth

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AuthManifestTest {
    @Test fun androidFilterAcceptsOnlyTheAgreedSchemeHostAndPath() {
        val manifest = File("src/main/AndroidManifest.xml").takeIf { it.exists() }
            ?: File("app/src/main/AndroidManifest.xml")
        val xml = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(manifest)
        val filters = xml.getElementsByTagName("intent-filter")
        val callbackFilters = (0 until filters.length).map { filters.item(it) as org.w3c.dom.Element }
            .filter { it.getElementsByTagName("data").length > 0 }
        assertEquals(1, callbackFilters.size)
        val filter = callbackFilters.single()
        val data = filter.getElementsByTagName("data").item(0) as org.w3c.dom.Element
        val android = "http://schemas.android.com/apk/res/android"
        assertEquals("tindaflow", data.getAttributeNS(android, "scheme"))
        assertEquals("auth", data.getAttributeNS(android, "host"))
        assertEquals("/callback", data.getAttributeNS(android, "path"))
        assertFalse(data.hasAttributeNS(android, "pathPrefix"))
        assertFalse(data.hasAttributeNS(android, "pathPattern"))
        val categories = filter.getElementsByTagName("category")
        val names = (0 until categories.length).map {
            (categories.item(it) as org.w3c.dom.Element).getAttributeNS(android, "name")
        }
        assertTrue(names.containsAll(listOf("android.intent.category.DEFAULT", "android.intent.category.BROWSABLE")))
    }
}
