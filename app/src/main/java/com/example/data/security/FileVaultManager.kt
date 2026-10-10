package com.example.data.security

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * Real Authenticated AES-256-GCM Encrypted File Vault Storage for SHIELD X.
 *
 * Requirements fulfilled:
 * - App-private internal storage: context.filesDir / "vault_encrypted"
 * - AES-256-GCM authenticated encryption (128-bit authentication tag)
 * - Unique cryptographically random 12-byte IV (nonce) for every single operation
 * - Per-file random 256-bit AES Data Encryption Key (DEK), protected by Android Keystore Master Key
 * - Verifies GCM authentication tag before releasing content; throws SecurityException on corruption/tampering
 * - Secure deletion with zero/random wipe
 * - No plaintext leakage in uncontrolled caches
 */
object FileVaultManager {

    private const val VAULT_DIR_NAME = "vault_encrypted"
    private val MAGIC_HEADER = byteArrayOf(0x53, 0x58, 0x56, 0x31) // "SXV1" (Shield X Vault v1)
    private const val HEADER_VERSION: Byte = 1
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128 // bits
    private const val GCM_TAG_LENGTH_BYTES: Byte = 16

    private val secureRandom = SecureRandom()

    private fun getVaultDirectory(context: Context): File {
        val dir = File(context.filesDir, VAULT_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    data class EncryptedFileResult(
        val relativePath: String,
        val totalCiphertextSize: Long,
        val sha256Checksum: String
    )

    /**
     * Encrypts byte array content and writes it safely to app-private storage.
     */
    fun encryptAndSaveBytes(
        context: Context,
        plainBytes: ByteArray
    ): EncryptedFileResult {
        return encryptAndSaveStream(context, ByteArrayInputStream(plainBytes), plainBytes.size.toLong())
    }

    /**
     * Encrypts input stream content using AES-256-GCM with per-file DEK wrapped by Keystore Master Key.
     */
    fun encryptAndSaveStream(
        context: Context,
        inputStream: InputStream,
        expectedLength: Long = -1L
    ): EncryptedFileResult {
        val vaultDir = getVaultDirectory(context)
        val fileName = "sxv_${UUID.randomUUID()}.dat"
        val outputFile = File(vaultDir, fileName)

        // 1. Read input and calculate plaintext SHA-256 checksum
        val plainBytes = inputStream.readBytes()
        val md = MessageDigest.getInstance("SHA-256")
        val sha256Checksum = md.digest(plainBytes).joinToString("") { "%02x".format(it) }

        // 2. Generate per-file AES-256 DEK
        val dek = KeyManager.generateDataEncryptionKey()

        // 3. Wrap DEK with Android Keystore Master Key
        val (wrappedDekIv, wrappedDekCiphertext) = KeyManager.wrapDataEncryptionKey(dek)

        // 4. Generate unique 12-byte IV for the file ciphertext
        val fileIv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(fileIv)

        // 5. Encrypt file content with AES-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, fileIv)
        cipher.init(Cipher.ENCRYPT_MODE, dek, gcmSpec)
        val fileCiphertext = cipher.doFinal(plainBytes)

        // 6. Write formatted binary container to disk
        FileOutputStream(outputFile).use { fos ->
            DataOutputStream(fos).use { dos ->
                // Magic header (4 bytes)
                dos.write(MAGIC_HEADER)
                // Version (1 byte)
                dos.writeByte(HEADER_VERSION.toInt())
                // Master Key IV (12 bytes)
                dos.write(wrappedDekIv)
                // Wrapped DEK length (2 bytes Short)
                dos.writeShort(wrappedDekCiphertext.size)
                // Wrapped DEK bytes
                dos.write(wrappedDekCiphertext)
                // File IV (12 bytes)
                dos.write(fileIv)
                // GCM Tag length (1 byte)
                dos.writeByte(GCM_TAG_LENGTH_BYTES.toInt())
                // Ciphertext with appended GCM tag
                dos.write(fileCiphertext)
                dos.flush()
            }
        }

        // Verify written file exists and has content
        if (!outputFile.exists() || outputFile.length() == 0L) {
            throw IllegalStateException("Failed to write encrypted file to private vault storage.")
        }

        return EncryptedFileResult(
            relativePath = fileName,
            totalCiphertextSize = outputFile.length(),
            sha256Checksum = sha256Checksum
        )
    }

    /**
     * Reads, verifies authentication tags, and decrypts ciphertext into memory.
     * Throws SecurityException if file has been tampered with or corrupted.
     */
    fun readAndDecryptFile(context: Context, relativePath: String): ByteArray {
        val vaultDir = getVaultDirectory(context)
        val file = File(vaultDir, relativePath)

        if (!file.exists()) {
            // Check if full path was given
            val altFile = File(relativePath)
            if (!altFile.exists()) {
                throw SecurityException("Vault file does not exist: $relativePath")
            }
            return readAndDecryptFromFile(altFile)
        }

        return readAndDecryptFromFile(file)
    }

    private fun readAndDecryptFromFile(file: File): ByteArray {
        FileInputStream(file).use { fis ->
            DataInputStream(fis).use { dis ->
                // 1. Verify Magic Header
                val magic = ByteArray(MAGIC_HEADER.size)
                dis.readFully(magic)
                if (!magic.contentEquals(MAGIC_HEADER)) {
                    throw SecurityException("Invalid vault file format: magic header mismatch.")
                }

                // 2. Verify Version
                val version = dis.readByte()
                if (version != HEADER_VERSION) {
                    throw SecurityException("Unsupported vault container version: $version")
                }

                // 3. Read Master Key IV (12 bytes)
                val masterKeyIv = ByteArray(GCM_IV_LENGTH)
                dis.readFully(masterKeyIv)

                // 4. Read Wrapped DEK
                val wrappedDekLen = dis.readShort().toInt()
                if (wrappedDekLen <= 0 || wrappedDekLen > 4096) {
                    throw SecurityException("Invalid wrapped key length: $wrappedDekLen")
                }
                val wrappedDekBytes = ByteArray(wrappedDekLen)
                dis.readFully(wrappedDekBytes)

                // 5. Unwrap DEK using Android Keystore Master Key
                val dek = KeyManager.unwrapDataEncryptionKey(masterKeyIv, wrappedDekBytes)

                // 6. Read File IV (12 bytes)
                val fileIv = ByteArray(GCM_IV_LENGTH)
                dis.readFully(fileIv)

                // 7. Read Tag Length
                val tagLengthBytes = dis.readByte()

                // 8. Read remaining ciphertext (with GCM auth tag)
                val remainingLen = file.length() - (4 + 1 + 12 + 2 + wrappedDekLen + 12 + 1)
                if (remainingLen <= 0) {
                    throw SecurityException("Corrupted vault file: ciphertext is missing or truncated.")
                }
                val ciphertext = ByteArray(remainingLen.toInt())
                dis.readFully(ciphertext)

                // 9. Decrypt and verify GCM authentication tag
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, fileIv)
                cipher.init(Cipher.DECRYPT_MODE, dek, spec)

                return try {
                    cipher.doFinal(ciphertext)
                } catch (e: Exception) {
                    throw SecurityException("Ciphertext authentication failed! File has been tampered with or corrupted.", e)
                }
            }
        }
    }

    /**
     * Securely wipes and deletes an encrypted file from disk.
     */
    fun secureDeleteFile(context: Context, relativePath: String): Boolean {
        val vaultDir = getVaultDirectory(context)
        val file = File(vaultDir, relativePath)
        val target = if (file.exists()) file else File(relativePath)

        if (!target.exists()) return true

        try {
            val length = target.length()
            if (length > 0) {
                // Overwrite with random bytes before deleting
                val randomBytes = ByteArray(4096)
                var remaining = length
                FileOutputStream(target).use { fos ->
                    while (remaining > 0) {
                        val toWrite = minOf(remaining, randomBytes.size.toLong()).toInt()
                        secureRandom.nextBytes(randomBytes)
                        fos.write(randomBytes, 0, toWrite)
                        remaining -= toWrite
                    }
                    fos.flush()
                }
            }
        } catch (_: Exception) {}

        return target.delete()
    }

    /**
     * Checks if a vault file exists on disk.
     */
    fun fileExists(context: Context, relativePath: String): Boolean {
        val vaultDir = getVaultDirectory(context)
        val file = File(vaultDir, relativePath)
        return file.exists() || File(relativePath).exists()
    }
}
