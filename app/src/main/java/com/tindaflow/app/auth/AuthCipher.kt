package com.tindaflow.app.auth

import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Versioned AES-GCM envelope; AAD binds each blob to its project and purpose. */
internal class AuthCipher(private val key: () -> SecretKey) {
    fun encrypt(value: ByteArray, purpose: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(purpose.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(value)
        return ByteBuffer.allocate(2 + cipher.iv.size + encrypted.size)
            .put(1.toByte()).put(cipher.iv.size.toByte()).put(cipher.iv).put(encrypted).array()
    }

    fun decrypt(value: ByteArray, purpose: String): ByteArray {
        require(value.size >= 30 && value[0] == 1.toByte() && value[1] == 12.toByte()) { "Invalid auth storage envelope." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, value.copyOfRange(2, 14)))
        cipher.updateAAD(purpose.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(value.copyOfRange(14, value.size))
    }
}
