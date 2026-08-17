package com.bitwarden.data.datasource.disk

import android.app.Application
import android.content.Context
import com.bitwarden.core.data.manager.encryption.EncryptionManager
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.data.datasource.disk.base.FakeSharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64

class KeystoreEncryptedSharedPreferencesTest {

    private val fakeSharedPreferences = FakeSharedPreferences()
    private val mockApplication = mockk<Application> {
        every { packageName } returns PACKAGE_NAME
        every {
            getSharedPreferences(
                "${PACKAGE_NAME}_keystore_encrypted_preferences",
                Context.MODE_PRIVATE,
            )
        } returns fakeSharedPreferences
    }

    // The EncryptionManager is mocked with a deterministic, self-inverting byte transformation
    // that, like real ciphertext, produces bytes that are not valid UTF-8.
    private val mockEncryptionManager = mockk<EncryptionManager> {
        every { encrypt(alias = ALIAS, bytes = any()) } answers {
            (IV + secondArg<ByteArray>().toggleBits()).asSuccess()
        }
        every { decrypt(alias = ALIAS, bytes = any()) } answers {
            secondArg<ByteArray>()
                .let { it.copyOfRange(fromIndex = IV.size, toIndex = it.size) }
                .toggleBits()
                .asSuccess()
        }
    }

    private val keystoreEncryptedSharedPreferences = KeystoreEncryptedSharedPreferences(
        app = mockApplication,
        encryptionManager = mockEncryptionManager,
    )

    @Test
    fun `putString followed by getString should return the original value`() {
        keystoreEncryptedSharedPreferences.edit().putString(KEY, VALUE).apply()

        assertEquals(
            VALUE,
            keystoreEncryptedSharedPreferences.getString(KEY, null),
        )
    }

    @Test
    fun `putString should store a base64 encoded encrypted value instead of the plaintext value`() {
        keystoreEncryptedSharedPreferences.edit().putString(KEY, VALUE).apply()

        val storedValue = requireNotNull(fakeSharedPreferences.getString(KEY, null))
        assertNotEquals(VALUE, storedValue)
        assertArrayEquals(
            IV + VALUE.encodeToByteArray().toggleBits(),
            Base64.getDecoder().decode(storedValue),
        )
    }

    @Test
    fun `putString with a null value should remove the stored value`() {
        keystoreEncryptedSharedPreferences.edit().putString(KEY, VALUE).apply()

        keystoreEncryptedSharedPreferences.edit().putString(KEY, null).apply()

        assertFalse(keystoreEncryptedSharedPreferences.contains(KEY))
        assertNull(keystoreEncryptedSharedPreferences.getString(KEY, null))
    }

    @Test
    fun `getString should return the default value when no value is stored`() {
        assertEquals(
            "mockDefault",
            keystoreEncryptedSharedPreferences.getString(KEY, "mockDefault"),
        )
        assertNull(keystoreEncryptedSharedPreferences.getString(KEY, null))
    }

    @Test
    fun `getString should return the default value when the value cannot be decrypted`() {
        keystoreEncryptedSharedPreferences.edit().putString(KEY, VALUE).apply()
        every {
            mockEncryptionManager.decrypt(alias = ALIAS, bytes = any())
        } returns Throwable().asFailure()

        assertEquals(
            "mockDefault",
            keystoreEncryptedSharedPreferences.getString(KEY, "mockDefault"),
        )
    }

    @Test
    fun `contains should reflect the underlying preferences`() {
        assertFalse(keystoreEncryptedSharedPreferences.contains(KEY))

        keystoreEncryptedSharedPreferences.edit().putString(KEY, VALUE).apply()

        assertTrue(keystoreEncryptedSharedPreferences.contains(KEY))
    }

    @Test
    fun `getAll should return the decrypted values`() {
        keystoreEncryptedSharedPreferences.edit().putString(KEY, VALUE).apply()

        val all = keystoreEncryptedSharedPreferences.all

        assertEquals(setOf(KEY), all.keys)
        assertEquals(VALUE, all[KEY] as String)
    }

    @Test
    fun `getAll should return a null value when the value cannot be decrypted`() {
        keystoreEncryptedSharedPreferences.edit().putString(KEY, VALUE).apply()
        every {
            mockEncryptionManager.decrypt(alias = ALIAS, bytes = any())
        } returns Throwable().asFailure()

        val all = keystoreEncryptedSharedPreferences.all

        assertEquals(setOf(KEY), all.keys)
        assertNull(all[KEY])
    }

    @Test
    fun `unsupported get operations should throw UnsupportedOperationException`() {
        assertThrows<UnsupportedOperationException> {
            keystoreEncryptedSharedPreferences.getBoolean(KEY, false)
        }
        assertThrows<UnsupportedOperationException> {
            keystoreEncryptedSharedPreferences.getInt(KEY, 0)
        }
        assertThrows<UnsupportedOperationException> {
            keystoreEncryptedSharedPreferences.getLong(KEY, 0L)
        }
        assertThrows<UnsupportedOperationException> {
            keystoreEncryptedSharedPreferences.getFloat(KEY, 0f)
        }
        assertThrows<UnsupportedOperationException> {
            keystoreEncryptedSharedPreferences.getStringSet(KEY, null)
        }
    }

    @Test
    fun `unsupported put operations should throw UnsupportedOperationException`() {
        val editor = keystoreEncryptedSharedPreferences.edit()
        assertThrows<UnsupportedOperationException> { editor.putBoolean(KEY, false) }
        assertThrows<UnsupportedOperationException> { editor.putInt(KEY, 0) }
        assertThrows<UnsupportedOperationException> { editor.putLong(KEY, 0L) }
        assertThrows<UnsupportedOperationException> { editor.putFloat(KEY, 0f) }
        assertThrows<UnsupportedOperationException> { editor.putStringSet(KEY, null) }
    }
}

/**
 * A self-inverting stand-in for encryption that, like real ciphertext, produces bytes that are
 * not valid UTF-8.
 */
private fun ByteArray.toggleBits(): ByteArray = this
    .map { (it.toInt() xor 0xAA).toByte() }
    .toByteArray()

private const val PACKAGE_NAME: String = "com.mock.app"
private const val ALIAS: String = "KeystoreEncryptedSharedPreferences"
private const val KEY: String = "mockKey"
private const val VALUE: String = "mockValue with unicode 🔐 and quotes “”"

// 0x80–0x8F are UTF-8 continuation bytes, which are invalid as leading bytes and would be
// destroyed by a UTF-8 decode/encode round trip.
private val IV: ByteArray = ByteArray(16) { (0x80 + it).toByte() }
