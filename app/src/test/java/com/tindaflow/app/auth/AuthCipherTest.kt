package com.tindaflow.app.auth

import org.junit.Assert.*
import org.junit.Test
import javax.crypto.KeyGenerator

class AuthCipherTest {
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val cipher = AuthCipher { key }

    @Test fun encryptedRoundTripUsesFreshNoncesAndNoPlaintext() {
        val input = "synthetic session test content".toByteArray()
        val first = cipher.encrypt(input, "project:session")
        val second = cipher.encrypt(input, "project:session")
        assertArrayEquals(input, cipher.decrypt(first, "project:session"))
        assertFalse(first.contentEquals(second))
        assertFalse(first.toString(Charsets.UTF_8).contains(input.toString(Charsets.UTF_8)))
    }

    @Test fun tamperingAndCrossProjectOrPurposeReadsFailClosed() {
        val encrypted = cipher.encrypt("synthetic".toByteArray(), "project:session")
        assertTrue(runCatching { cipher.decrypt(encrypted, "other-project:session") }.isFailure)
        assertTrue(runCatching { cipher.decrypt(encrypted, "project:pkce") }.isFailure)
        val changed = encrypted.copyOf().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() }
        assertTrue(runCatching { cipher.decrypt(changed, "project:session") }.isFailure)
    }

    @Test fun wrongKeyAndMalformedEnvelopeDoNotReturnPlaintext() {
        val otherKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val encrypted = cipher.encrypt("synthetic".toByteArray(), "project:session")
        assertTrue(runCatching { AuthCipher { otherKey }.decrypt(encrypted, "project:session") }.isFailure)
        assertTrue(runCatching { cipher.decrypt(byteArrayOf(1, 12), "project:session") }.isFailure)
        encrypted[0] = 2
        assertTrue(runCatching { cipher.decrypt(encrypted, "project:session") }.isFailure)
    }
}
