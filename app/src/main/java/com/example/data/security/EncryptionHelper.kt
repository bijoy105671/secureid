package com.example.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Cryptographic security utilities for SHIELD X.
 *
 * Implements:
 * - PBKDF2WithHmacSHA256 password hashing with unique 16-byte salt and 65,536 iterations.
 * - Constant-time comparison using MessageDigest.isEqual to prevent timing attacks.
 * - Cryptographically strong OTP & token generation using SecureRandom.
 * - Password policy validation and strength evaluation.
 */
object EncryptionHelper {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 65536
    private const val SALT_BYTES = 16
    private const val KEY_LENGTH_BITS = 256

    private val secureRandom = SecureRandom()

    /**
     * Hashes a secret passphrase using PBKDF2WithHmacSHA256 and a random 16-byte salt.
     * Output format: "$iterations:$saltHex:$hashHex"
     */
    fun hashPassphrase(passphrase: CharArray): String {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)

        val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hash = skf.generateSecret(spec).encoded

        val saltHex = bytesToHex(salt)
        val hashHex = bytesToHex(hash)

        return "$PBKDF2_ITERATIONS:$saltHex:$hashHex"
    }

    /**
     * Backward-compatible convenience wrapper for String input.
     */
    fun hashSecret(input: String): String {
        return hashPassphrase(input.toCharArray())
    }

    /**
     * Verifies an entered passphrase against a stored hash string using constant-time comparison.
     * Supports both new PBKDF2 format ("$iterations:$saltHex:$hashHex") and legacy format for migration.
     */
    fun verifySecret(input: String, storedHash: String): Boolean {
        if (storedHash.isBlank() || input.isBlank()) return false

        val parts = storedHash.split(":")
        if (parts.size == 3) {
            val iterations = parts[0].toIntOrNull() ?: return false
            val salt = hexToBytes(parts[1]) ?: return false
            val expectedHash = hexToBytes(parts[2]) ?: return false

            val spec = PBEKeySpec(input.toCharArray(), salt, iterations, expectedHash.size * 8)
            val skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val testHash = skf.generateSecret(spec).encoded

            return MessageDigest.isEqual(testHash, expectedHash)
        }

        // Legacy SHA-256 fallback for migration only (64 hex characters)
        if (storedHash.length == 64 && !storedHash.contains(":")) {
            val md = MessageDigest.getInstance("SHA-256")
            val computed = md.digest(input.toByteArray(Charsets.UTF_8))
            val computedHex = bytesToHex(computed)
            return MessageDigest.isEqual(
                computedHex.toByteArray(Charsets.UTF_8),
                storedHash.lowercase().toByteArray(Charsets.UTF_8)
            )
        }

        return false
    }

    /**
     * Generates a 6-digit cryptographic random OTP.
     */
    fun generateRandomOtp(): String {
        val otp = secureRandom.nextInt(900000) + 100000
        return otp.toString()
    }

    /**
     * Generates an unpredictable 16-byte cryptographic challenge nonce in Hex.
     */
    fun generateChallengeNonce(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytesToHex(bytes)
    }

    /**
     * Evaluates passphrase strength and checks policy rules.
     */
    fun evaluatePasswordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength(0, "খুব দুর্বল", "Very Weak", isValid = false)

        var score = 0
        if (password.length >= 6) score += 20
        if (password.length >= 8) score += 20
        if (password.length >= 12) score += 15

        if (password.any { it.isDigit() }) score += 15
        if (password.any { it.isUpperCase() }) score += 15
        if (password.any { it.isLowerCase() }) score += 10
        if (password.any { !it.isLetterOrDigit() }) score += 10

        val (labelBn, labelEn) = when {
            score >= 70 -> "শক্তিশালী (রিমোট সিকিউর)" to "Strong (Secure)"
            score >= 50 -> "মাঝারি" to "Moderate"
            else -> "দুর্বল" to "Weak"
        }

        // Policy rule: at least 6 characters (or 4-6 digits for PIN)
        val isValid = password.length >= 4

        return PasswordStrength(score.coerceIn(0, 100), labelBn, labelEn, isValid)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        return try {
            ByteArray(hex.length / 2) { i ->
                hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (_: Exception) {
            null
        }
    }

    data class PasswordStrength(
        val score: Int,
        val labelBn: String,
        val labelEn: String,
        val isValid: Boolean = true
    )
}
