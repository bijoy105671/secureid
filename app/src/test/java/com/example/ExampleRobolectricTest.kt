package com.example

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.DeviceLockdownService
import com.example.data.security.EncryptionHelper
import com.example.data.security.FileVaultManager
import com.example.data.security.KeyManager
import com.example.data.security.SecurityPreferences
import com.example.data.security.TrustedDeviceProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    @Test
    fun testAppNameMatchesShieldX() {
        val appName = context.getString(R.string.app_name)
        assertEquals("SHIELD X", appName)
    }

    @Test
    fun testRandomOtpGenerationLengthAndDigits() {
        val otp = EncryptionHelper.generateRandomOtp()
        assertEquals(6, otp.length)
        assertTrue(otp.all { it.isDigit() })
    }

    @Test
    fun testPbkdf2HashAndVerification() {
        val pin = "849201"
        val hash = EncryptionHelper.hashSecret(pin)
        assertTrue("Stored hash must contain iteration count and salt separator", hash.contains(":"))
        assertTrue("Valid PIN must verify correctly", EncryptionHelper.verifySecret("849201", hash))
        assertFalse("Invalid PIN must fail verification", EncryptionHelper.verifySecret("123456", hash))
        assertFalse("Default 1234 must fail verification", EncryptionHelper.verifySecret("1234", hash))
    }

    @Test
    fun testPasswordStrengthEvaluation() {
        val weak = EncryptionHelper.evaluatePasswordStrength("")
        assertFalse(weak.isValid)

        val strong = EncryptionHelper.evaluatePasswordStrength("ShieldX#Secured2026")
        assertTrue(strong.isValid)
        assertTrue(strong.score >= 80)
    }

    @Test
    fun testSecurityPreferencesNoInsecureDefaultsAndThrottling() {
        val secPrefs = SecurityPreferences(context)

        // 1. New installation must not be setup by default
        secPrefs.setSetupDone(false)
        assertFalse("New installation must not have setup completed by default", secPrefs.isSetupDone())
        assertFalse("New installation must not have paired secondary device", secPrefs.isSecondaryPaired())

        // 2. Set master PIN
        secPrefs.setMasterPin("998811")
        assertTrue("Setup must now be marked done", secPrefs.isSetupDone())
        assertTrue("Correct PIN verification must succeed", secPrefs.verifyPin("998811"))

        // 3. Test Throttling on repeated failed attempts
        secPrefs.resetThrottling()
        assertEquals(0, secPrefs.getFailedAttempts())

        for (i in 1..3) {
            val failResult = secPrefs.verifyPin("000000")
            assertFalse("Wrong PIN must fail", failResult)
        }
        assertEquals(3, secPrefs.getFailedAttempts())
        assertTrue("Lockout seconds should be active after 3 failures (30s delay)", secPrefs.getRemainingLockoutSeconds() > 0)

        // Attempting during lockout is rejected immediately even with correct PIN
        assertFalse("Attempt during active lockout must fail", secPrefs.verifyPin("998811"))

        // Reset throttling
        secPrefs.resetThrottling()
        assertEquals(0, secPrefs.getFailedAttempts())
        assertEquals(0L, secPrefs.getRemainingLockoutSeconds())
        assertTrue("Verification works after reset", secPrefs.verifyPin("998811"))
    }

    @Test
    fun testKeyManagerMasterKeyWrappingAndEcdsa() {
        // Master Key & DEK wrapping
        val masterKey = KeyManager.getOrCreateMasterKey()
        assertNotNull("Master key must not be null", masterKey)
        assertTrue(KeyManager.isMasterKeyAvailable())

        val dek = KeyManager.generateDataEncryptionKey()
        val (wrappedIv, wrappedBytes) = KeyManager.wrapDataEncryptionKey(dek)
        val unwrappedDek = KeyManager.unwrapDataEncryptionKey(wrappedIv, wrappedBytes)
        assertEquals(dek, unwrappedDek)

        // Device Identity & ECDSA digital signature
        val pubKeyBase64 = KeyManager.getDevicePublicKeyBase64()
        assertTrue("Public key must be exported in Base64", pubKeyBase64.isNotBlank())

        val testPayload = "TEST_SECURITY_CHALLENGE_PAYLOAD_2026".toByteArray(Charsets.UTF_8)
        val signature = KeyManager.signData(testPayload)
        assertNotNull("Signature must be generated", signature)

        val keyBytes = Base64.decode(pubKeyBase64, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val pubKey = KeyFactory.getInstance("EC").generatePublic(keySpec)

        val isValid = KeyManager.verifySignature(pubKey, testPayload, signature)
        assertTrue("Valid signature must verify", isValid)

        val tamperedPayload = "TEST_SECURITY_CHALLENGE_PAYLOAD_TAMPERED".toByteArray(Charsets.UTF_8)
        val isTamperedValid = KeyManager.verifySignature(pubKey, tamperedPayload, signature)
        assertFalse("Tampered payload signature verification must fail", isTamperedValid)
    }

    @Test
    fun testFileVaultManagerAesGcmEncryptionDecryptionAndTamperProof() {
        val testContent = "Top Secret Document Content for SHIELD X Vault 2026"
        val plainBytes = testContent.toByteArray(Charsets.UTF_8)

        // Encrypt and save
        val result = FileVaultManager.encryptAndSaveBytes(context, plainBytes)
        assertNotNull(result)
        assertTrue(FileVaultManager.fileExists(context, result.relativePath))

        // Decrypt and verify
        val decryptedBytes = FileVaultManager.readAndDecryptFile(context, result.relativePath)
        val decryptedText = String(decryptedBytes, Charsets.UTF_8)
        assertEquals("Decrypted content must match original text", testContent, decryptedText)

        // Tamper test: Corrupt ciphertext on disk
        val vaultDir = File(context.filesDir, "vault_encrypted")
        val fileOnDisk = File(vaultDir, result.relativePath)
        val diskBytes = fileOnDisk.readBytes()
        diskBytes[diskBytes.size - 2] = (diskBytes[diskBytes.size - 2].toInt() xor 0xFF).toByte()
        fileOnDisk.writeBytes(diskBytes)

        // Reading tampered file must throw SecurityException
        var tamperDetected = false
        try {
            FileVaultManager.readAndDecryptFile(context, result.relativePath)
        } catch (_: SecurityException) {
            tamperDetected = true
        }
        assertTrue("Tampered ciphertext must cause SecurityException", tamperDetected)

        // Secure delete
        FileVaultManager.secureDeleteFile(context, result.relativePath)
        assertFalse("File must be deleted after secure delete", FileVaultManager.fileExists(context, result.relativePath))
    }

    @Test
    fun testTrustedDeviceProtocolCeremonyAndChallengeVerification() {
        val secPrefs = SecurityPreferences(context)
        val localDeviceId = TrustedDeviceProtocol.getOrCreateLocalDeviceId(context)
        assertTrue("Device ID must be generated", localDeviceId.isNotBlank())

        val localIdentity = TrustedDeviceProtocol.getLocalDeviceIdentity(context)
        assertEquals(localDeviceId, localIdentity.deviceId)

        // Register peer trusted device in preferences
        secPrefs.savePairedDevice("Trusted Sentinel", localDeviceId, localIdentity.publicKeyBase64)
        assertTrue(secPrefs.isSecondaryPaired())

        // Create challenge
        val challenge = TrustedDeviceProtocol.ApprovalChallenge(
            action = "DEVICE_SHUTDOWN",
            targetDeviceId = localDeviceId,
            requesterName = "SHIELD X Primary"
        )
        assertFalse("Challenge must not be expired upon creation", challenge.isExpired())

        // Sign challenge
        val token = TrustedDeviceProtocol.signChallenge(challenge)
        assertEquals(challenge.challengeId, token.challengeId)
        assertEquals(challenge.nonce, token.nonce)

        // Verify valid response
        val verifyResult = TrustedDeviceProtocol.verifyApprovalResponse(context, secPrefs, challenge, token)
        assertTrue("Valid token verification must succeed", verifyResult is TrustedDeviceProtocol.VerificationResult.Success)

        // Replay attack test: using same token again with consumed nonce must be rejected
        val replayResult = TrustedDeviceProtocol.verifyApprovalResponse(context, secPrefs, challenge, token)
        assertTrue("Replayed token with consumed nonce must be rejected", replayResult is TrustedDeviceProtocol.VerificationResult.Rejected)
    }

    @Test
    fun testDeviceLockdownTriggerAndOtpAuthentication() {
        DeviceLockdownService.triggerLockdown(context, "Unauthorized Shutdown Attempt")
        assertTrue("Lockdown must be active", DeviceLockdownService.isLockdownActive.value)
        val challenge = DeviceLockdownService.activeChallenge.value
        assertNotNull("Lockdown challenge must be present", challenge)

        // Test incorrect OTP
        val wrongResult = DeviceLockdownService.verifyOtp(context, "000000")
        assertFalse("Wrong OTP must be rejected", wrongResult)
        assertTrue("Lockdown must still be active after wrong OTP", DeviceLockdownService.isLockdownActive.value)

        // Test correct OTP
        val correctResult = DeviceLockdownService.verifyOtp(context, challenge!!.otp)
        assertTrue("Correct OTP must be accepted", correctResult)
        assertFalse("Lockdown must be dismissed after correct OTP", DeviceLockdownService.isLockdownActive.value)
    }
}
