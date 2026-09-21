package com.example.harvestdistributionapp.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PasswordRecord(val hash: String, val salt: String)

object PasswordHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    fun create(password: String): PasswordRecord {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val hash = derive(password, salt)
        return PasswordRecord(
            hash = Base64.encodeToString(hash, Base64.NO_WRAP),
            salt = Base64.encodeToString(salt, Base64.NO_WRAP)
        )
    }

    fun verify(password: String, expectedHash: String, encodedSalt: String): Boolean {
        val salt = runCatching { Base64.decode(encodedSalt, Base64.NO_WRAP) }.getOrNull() ?: return false
        val expected = runCatching { Base64.decode(expectedHash, Base64.NO_WRAP) }.getOrNull() ?: return false
        val actual = derive(password, salt)
        if (actual.size != expected.size) return false
        var difference = 0
        actual.indices.forEach { index -> difference = difference or (actual[index].toInt() xor expected[index].toInt()) }
        return difference == 0
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
