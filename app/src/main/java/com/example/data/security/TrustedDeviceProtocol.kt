package com.example.data.security

import android.content.Context
import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Collections
import java.util.UUID

/**
 * Real Cryptographic Trusted-Device Protocol for SHIELD X.
 *
 * Requirements fulfilled:
 * 1. Authenticated Pairing Ceremony exchanging Android Keystore EC P-256 public keys.
 * 2. Unpredictable challenges with cryptographically random 16-byte nonces and UUIDs.
 * 3. Digital signatures using SHA256withECDSA generated in Android Keystore.
 * 4. Strict binding to action, requestId, expiry timestamp, and target device.
 * 5. Replay attack rejection using consumed-nonce tracking.
 * 6. Explicit rejection of expired, replayed, forged, or mismatched approvals.
 * 7. Clear separation between local credentials and remote cryptographic proof.
 */
object TrustedDeviceProtocol {

    private const val PROTOCOL_HEADER = "SHIELDX_APPROVAL_V1"
    private const val CHALLENGE_VALIDITY_WINDOW_MS = 120_000L // 2 minutes

    // Cache of consumed nonces to prevent replay attacks
    private val consumedNonces = Collections.synchronizedSet(mutableSetOf<String>())

    data class DeviceIdentity(
        val deviceId: String,
        val deviceName: String,
        val publicKeyBase64: String,
        val createdTimestamp: Long
    )

    data class ApprovalChallenge(
        val challengeId: String = UUID.randomUUID().toString(),
        val action: String,
        val targetDeviceId: String,
        val requesterName: String,
        val nonce: String = EncryptionHelper.generateChallengeNonce(),
        val timestamp: Long = System.currentTimeMillis(),
        val expiryTimestamp: Long = System.currentTimeMillis() + CHALLENGE_VALIDITY_WINDOW_MS,
        val verificationCode: String = EncryptionHelper.generateRandomOtp()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expiryTimestamp

        fun canonicalPayload(): ByteArray {
            val raw = "$PROTOCOL_HEADER|$challengeId|$action|$targetDeviceId|$nonce|$expiryTimestamp"
            return raw.toByteArray(Charsets.UTF_8)
        }
    }

    data class SignedApprovalToken(
        val challengeId: String,
        val action: String,
        val targetDeviceId: String,
        val nonce: String,
        val expiryTimestamp: Long,
        val signatureBase64: String
    ) {
        fun encodeToTransmissionString(): String {
            return "$challengeId:$action:$targetDeviceId:$nonce:$expiryTimestamp:$signatureBase64"
        }

        companion object {
            fun decodeFromTransmissionString(tokenStr: String): SignedApprovalToken? {
                val parts = tokenStr.split(":")
                if (parts.size != 6) return null
                val expiry = parts[4].toLongOrNull() ?: return null
                return SignedApprovalToken(
                    challengeId = parts[0],
                    action = parts[1],
                    targetDeviceId = parts[2],
                    nonce = parts[3],
                    expiryTimestamp = expiry,
                    signatureBase64 = parts[5]
                )
            }
        }
    }

    sealed class VerificationResult {
        object Success : VerificationResult()
        data class Rejected(val reasonBn: String, val reasonEn: String) : VerificationResult()
    }

    /**
     * Retrieves or initializes this device's persistent unique identifier.
     */
    fun getOrCreateLocalDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences("shieldx_device_identity", Context.MODE_PRIVATE)
        var id = prefs.getString("local_device_id", "")
        if (id.isNullOrBlank()) {
            id = "DEV-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
            prefs.edit().putString("local_device_id", id).apply()
        }
        return id
    }

    /**
     * Generates this device's cryptographic identity profile for pairing.
     */
    fun getLocalDeviceIdentity(context: Context): DeviceIdentity {
        val deviceId = getOrCreateLocalDeviceId(context)
        val publicKeyBase64 = KeyManager.getDevicePublicKeyBase64()
        return DeviceIdentity(
            deviceId = deviceId,
            deviceName = android.os.Build.MODEL ?: "Android Device",
            publicKeyBase64 = publicKeyBase64,
            createdTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Signs an approval challenge using this device's Android Keystore EC private key.
     */
    fun signChallenge(challenge: ApprovalChallenge): SignedApprovalToken {
        val payload = challenge.canonicalPayload()
        val signatureBytes = KeyManager.signData(payload)
        val signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)

        return SignedApprovalToken(
            challengeId = challenge.challengeId,
            action = challenge.action,
            targetDeviceId = challenge.targetDeviceId,
            nonce = challenge.nonce,
            expiryTimestamp = challenge.expiryTimestamp,
            signatureBase64 = signatureBase64
        )
    }

    /**
     * Cryptographically verifies an incoming signed approval token against the active challenge
     * and the registered trusted device's public key.
     */
    fun verifyApprovalResponse(
        context: Context,
        prefs: SecurityPreferences,
        activeChallenge: ApprovalChallenge,
        token: SignedApprovalToken
    ): VerificationResult {
        // 1. Verify trusted device pairing exists
        if (!prefs.isSecondaryPaired()) {
            return VerificationResult.Rejected(
                reasonBn = "কোনো বিশ্বস্ত সেকেন্ডারি ফোন পেয়ার করা নেই।",
                reasonEn = "No trusted secondary device paired."
            )
        }

        val storedPeerPubKeyBase64 = prefs.getSecondaryPublicKey()
        if (storedPeerPubKeyBase64.isBlank()) {
            return VerificationResult.Rejected(
                reasonBn = "সেকেন্ডারি ফোনের পাবলিক কি অনুপলব্ধ।",
                reasonEn = "Secondary phone public key is missing."
            )
        }

        // 2. Target Device ID check
        val localDeviceId = getOrCreateLocalDeviceId(context)
        if (token.targetDeviceId != localDeviceId) {
            return VerificationResult.Rejected(
                reasonBn = "অনুমোদনটি অন্য ডিভাইসের জন্য ইস্যু করা হয়েছে (আইডি অমিল)।",
                reasonEn = "Approval token was issued for a different target device ID."
            )
        }

        // 3. Challenge ID & Action binding check
        if (token.challengeId != activeChallenge.challengeId) {
            return VerificationResult.Rejected(
                reasonBn = "চ্যালেঞ্জ আইডি অমিল! অনুমোদনটি এই অনুরোধের নয়।",
                reasonEn = "Challenge ID mismatch. Token does not belong to this request."
            )
        }

        if (token.action != activeChallenge.action) {
            return VerificationResult.Rejected(
                reasonBn = "অনুমোদিত অ্যাকশন অমিল!",
                reasonEn = "Action mismatch in approval token."
            )
        }

        // 4. Nonce binding and Replay attack protection
        if (token.nonce != activeChallenge.nonce) {
            return VerificationResult.Rejected(
                reasonBn = "সিকিউরিটি নন্স (Nonce) অমিল।",
                reasonEn = "Security nonce mismatch."
            )
        }

        if (consumedNonces.contains(token.nonce)) {
            return VerificationResult.Rejected(
                reasonBn = "রিপ্লে আক্রমণ শনাক্ত! এই অনুমোদন নন্স ইতিপূর্বে ব্যবহৃত হয়েছে।",
                reasonEn = "Replay attack detected! Nonce has already been consumed."
            )
        }

        // 5. Expiration check
        val now = System.currentTimeMillis()
        if (now > token.expiryTimestamp || now > activeChallenge.expiryTimestamp) {
            return VerificationResult.Rejected(
                reasonBn = "অনুমোদনটির সময়সীমা শেষ হয়ে গেছে (Expired)।",
                reasonEn = "Approval token has expired."
            )
        }

        // 6. Cryptographic ECDSA Signature Verification
        val peerPublicKey: PublicKey = try {
            val keyBytes = Base64.decode(storedPeerPubKeyBase64, Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val kf = KeyFactory.getInstance("EC")
            kf.generatePublic(keySpec)
        } catch (e: Exception) {
            return VerificationResult.Rejected(
                reasonBn = "পাবলিক কি পার্সিং ব্যর্থ: ${e.message}",
                reasonEn = "Failed to parse trusted peer public key."
            )
        }

        val canonicalBytes = activeChallenge.canonicalPayload()
        val signatureBytes = try {
            Base64.decode(token.signatureBase64, Base64.DEFAULT)
        } catch (_: Exception) {
            return VerificationResult.Rejected(
                reasonBn = "ডিজিটাল সিগনেচার ফরম্যাট ত্রুটিপূর্ণ।",
                reasonEn = "Invalid digital signature encoding."
            )
        }

        val isSigValid = try {
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(peerPublicKey)
            sig.update(canonicalBytes)
            sig.verify(signatureBytes)
        } catch (e: Exception) {
            return VerificationResult.Rejected(
                reasonBn = "সিগনেচার যাচাইয়ে ব্যর্থ: ${e.message}",
                reasonEn = "Signature verification threw exception: ${e.message}"
            )
        }

        if (!isSigValid) {
            return VerificationResult.Rejected(
                reasonBn = "সিগনেচার সঠিক নয়! অনুমোদনটি জাল বা পরিবর্তন করা হয়েছে।",
                reasonEn = "Invalid signature! Token is forged or tampered."
            )
        }

        // Consume nonce permanently
        consumedNonces.add(token.nonce)

        return VerificationResult.Success
    }
}
