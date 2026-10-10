package com.example.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

object EncryptionHelper {

    private val secureRandom = SecureRandom()

    fun hashSecret(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifySecret(input: String, storedHash: String): Boolean {
        if (storedHash.isEmpty()) return false
        val hashed = hashSecret(input)
        return hashed.equals(storedHash, ignoreCase = true)
    }

    /**
     * Generates a dynamic 6-digit cryptographic random OTP
     */
    fun generateRandomOtp(): String {
        val otp = secureRandom.nextInt(900000) + 100000
        return otp.toString()
    }

    /**
     * Generates a 6-character alphanumeric pairing token for secondary device
     */
    fun generatePairingToken(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..6)
            .map { allowedChars[Random.nextInt(allowedChars.size)] }
            .joinToString("")
    }

    /**
     * Evaluates password strength from 0 to 100
     */
    fun evaluatePasswordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength(0, "খুব দুর্বল", "Very Weak")

        var score = 0
        if (password.length >= 6) score += 20
        if (password.length >= 10) score += 20
        if (password.length >= 14) score += 10

        if (password.any { it.isDigit() }) score += 15
        if (password.any { it.isUpperCase() }) score += 15
        if (password.any { it.isLowerCase() }) score += 10
        if (password.any { !it.isLetterOrDigit() }) score += 10

        val (labelBn, labelEn) = when {
            score >= 80 -> "অত্যন্ত শক্তিশালী (মিলিটারি গ্রেড)" to "Very Strong (Military-Grade)"
            score >= 60 -> "শক্তিশালী" to "Strong"
            score >= 40 -> "মাঝারি" to "Moderate"
            else -> "দুর্বল" to "Weak"
        }

        return PasswordStrength(score.coerceIn(0, 100), labelBn, labelEn)
    }

    data class PasswordStrength(
        val score: Int,
        val labelBn: String,
        val labelEn: String
    )
}
