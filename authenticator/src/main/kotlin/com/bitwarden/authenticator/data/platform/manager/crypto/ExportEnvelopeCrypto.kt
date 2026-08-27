package com.bitwarden.authenticator.data.platform.manager.crypto

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val HMAC_ALGORITHM: String = "HmacSHA256"
private const val AES_KEY_ALGORITHM: String = "AES"
private const val AES_CBC_TRANSFORMATION: String = "AES/CBC/PKCS5Padding"
private const val ENC_STRING_TYPE: String = "2"
private const val ENC_STRING_PART_COUNT: Int = 3
private const val ENC_STRING_PART_SEPARATOR: Char = '|'
private const val ENC_STRING_TYPE_SEPARATOR: Char = '.'
private const val KEY_SIZE_BYTES: Int = 32
private const val IV_SIZE_BYTES: Int = 16
private const val MAC_SIZE_BYTES: Int = 32
private const val ENC_KEY_INFO: String = "enc"
private const val MAC_KEY_INFO: String = "mac"

/**
 * The pair of keys a Bitwarden `EncString` of type 2 is built from.
 *
 * @property encKey The AES-256-CBC encryption key.
 * @property macKey The HMAC-SHA256 authentication key.
 */
internal class StretchedKey(
    val encKey: ByteArray,
    val macKey: ByteArray,
)

/**
 * An `EncString` of type 2 that has been split into its parts but not yet authenticated.
 *
 * @property iv The initialization vector the ciphertext was produced with.
 * @property ciphertext The AES-256-CBC ciphertext.
 * @property mac The HMAC-SHA256 over `iv || ciphertext`.
 */
internal class ParsedEncString(
    val iv: ByteArray,
    val ciphertext: ByteArray,
    val mac: ByteArray,
)

/**
 * The cryptographic primitives behind a password-protected Bitwarden export.
 *
 * Mirrors `bitwarden-crypto`'s `PinKey` derivation and type 2 `EncString` construction. Any change
 * here changes which files other Bitwarden clients can read, so each step is pinned by a test
 * vector taken from the SDK.
 */
internal object ExportEnvelopeCrypto {

    /**
     * Derives the encryption and authentication keys for an export from the given [password].
     *
     * [salt] is the base64 text from the envelope, used verbatim rather than decoded, matching
     * every other Bitwarden client.
     */
    fun deriveStretchedKey(
        password: String,
        salt: String,
        iterations: Int,
    ): StretchedKey {
        val keyMaterial = pbkdf2HmacSha256(
            password = password.toByteArray(Charsets.UTF_8),
            salt = salt.toByteArray(Charsets.UTF_8),
            iterations = iterations,
        )
        return StretchedKey(
            encKey = hkdfExpand(prk = keyMaterial, info = ENC_KEY_INFO),
            macKey = hkdfExpand(prk = keyMaterial, info = MAC_KEY_INFO),
        )
    }

    /**
     * Encrypts [plaintext] with [key] and the given [iv], returning a type 2 `EncString`.
     */
    fun encrypt(
        plaintext: String,
        iv: ByteArray,
        key: StretchedKey,
    ): String {
        val cipher = Cipher.getInstance(AES_CBC_TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key.encKey, AES_KEY_ALGORITHM),
            IvParameterSpec(iv),
        )
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val mac = calculateMac(iv = iv, ciphertext = ciphertext, macKey = key.macKey)
        val encoder = Base64.getEncoder()
        return ENC_STRING_TYPE +
            ENC_STRING_TYPE_SEPARATOR +
            encoder.encodeToString(iv) +
            ENC_STRING_PART_SEPARATOR +
            encoder.encodeToString(ciphertext) +
            ENC_STRING_PART_SEPARATOR +
            encoder.encodeToString(mac)
    }

    /**
     * Splits [value] into the parts of a type 2 `EncString`, or returns `null` when it is not one.
     */
    fun parse(value: String): ParsedEncString? {
        val typeSeparatorIndex = value.indexOf(ENC_STRING_TYPE_SEPARATOR)
        if (typeSeparatorIndex == -1) return null
        if (value.substring(startIndex = 0, endIndex = typeSeparatorIndex) != ENC_STRING_TYPE) {
            return null
        }
        val parts = value
            .substring(startIndex = typeSeparatorIndex + 1)
            .split(ENC_STRING_PART_SEPARATOR)
        if (parts.size != ENC_STRING_PART_COUNT) return null
        val decoder = Base64.getDecoder()
        val decoded = try {
            parts.map { decoder.decode(it) }
        } catch (_: IllegalArgumentException) {
            return null
        }
        val (iv, ciphertext, mac) = decoded
        if (iv.size != IV_SIZE_BYTES || mac.size != MAC_SIZE_BYTES) return null
        if (ciphertext.isEmpty()) return null
        return ParsedEncString(iv = iv, ciphertext = ciphertext, mac = mac)
    }

    /**
     * Authenticates and decrypts [encString] with [key], or returns `null` when the MAC does not
     * match or the plaintext is not valid UTF-8.
     */
    fun decrypt(encString: ParsedEncString, key: StretchedKey): String? {
        val expectedMac = calculateMac(
            iv = encString.iv,
            ciphertext = encString.ciphertext,
            macKey = key.macKey,
        )
        if (!MessageDigest.isEqual(expectedMac, encString.mac)) return null
        val cipher = Cipher.getInstance(AES_CBC_TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key.encKey, AES_KEY_ALGORITHM),
            IvParameterSpec(encString.iv),
        )
        return cipher.doFinal(encString.ciphertext).toString(Charsets.UTF_8)
    }

    private fun calculateMac(
        iv: ByteArray,
        ciphertext: ByteArray,
        macKey: ByteArray,
    ): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(macKey, HMAC_ALGORITHM))
        mac.update(iv)
        mac.update(ciphertext)
        return mac.doFinal()
    }

    /**
     * PBKDF2-HMAC-SHA256 over the UTF-8 bytes of the password.
     *
     * Hand-rolled rather than using `PBEKeySpec`, whose providers disagree on how password
     * characters become bytes; a mismatch there would silently produce files other clients and
     * other Android versions cannot open.
     */
    private fun pbkdf2HmacSha256(
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(password, HMAC_ALGORITHM))
        mac.update(salt)
        // A single block, because the derived key is exactly one HMAC-SHA256 output wide.
        mac.update(byteArrayOf(0, 0, 0, 1))
        var block = mac.doFinal()
        val result = block.copyOf()
        repeat(iterations - 1) {
            block = mac.doFinal(block)
            for (index in result.indices) {
                result[index] = (result[index].toInt() xor block[index].toInt()).toByte()
            }
        }
        return result
    }

    /**
     * The HKDF-Expand half of RFC 5869, taking the KDF output as the pseudorandom key.
     */
    private fun hkdfExpand(prk: ByteArray, info: String): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(prk, HMAC_ALGORITHM))
        val infoBytes = info.toByteArray(Charsets.UTF_8)
        val result = ByteArray(KEY_SIZE_BYTES)
        var previous = ByteArray(0)
        var offset = 0
        var counter = 1
        while (offset < result.size) {
            mac.update(previous)
            mac.update(infoBytes)
            mac.update(counter.toByte())
            previous = mac.doFinal()
            val length = minOf(previous.size, result.size - offset)
            previous.copyInto(destination = result, destinationOffset = offset, endIndex = length)
            offset += length
            counter++
        }
        return result
    }
}
