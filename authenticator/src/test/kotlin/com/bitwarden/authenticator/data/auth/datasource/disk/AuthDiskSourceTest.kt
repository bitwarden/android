package com.bitwarden.authenticator.data.auth.datasource.disk

import androidx.core.content.edit
import com.bitwarden.authenticatorbridge.util.generateSecretKey
import com.bitwarden.data.datasource.disk.base.FakeSharedPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthDiskSourceTest {

    private val fakeEncryptedSharedPreferences = FakeSharedPreferences()
    private val fakeKeystoreEncryptedPreferences = FakeSharedPreferences()
    private val fakeSharedPreferences = FakeSharedPreferences()

    private val authDiskSource = AuthDiskSourceImpl(
        encryptedSharedPreferences = fakeEncryptedSharedPreferences,
        keystoreEncryptedPreferences = fakeKeystoreEncryptedPreferences,
        sharedPreferences = fakeSharedPreferences,
    )

    @Test
    @Suppress("MaxLineLength")
    fun `initialization should migrate legacy encrypted values to the keystore encrypted preferences`() {
        fakeEncryptedSharedPreferences.edit {
            putString("bwSecureStorage:userKeyBiometricUnlock", "biometricsKey")
            putString("bwSecureStorage:biometricInitializationVector", "initVector")
            putString("bwSecureStorage:authenticatorSyncSymmetricKey", "symmetricKey")
        }

        AuthDiskSourceImpl(
            encryptedSharedPreferences = fakeEncryptedSharedPreferences,
            keystoreEncryptedPreferences = fakeKeystoreEncryptedPreferences,
            sharedPreferences = fakeSharedPreferences,
        )

        // The values are moved to the keystore encrypted preferences without the legacy prefix.
        assertTrue(fakeEncryptedSharedPreferences.all.isEmpty())
        assertEquals(
            "biometricsKey",
            fakeKeystoreEncryptedPreferences.getString("userKeyBiometricUnlock", null),
        )
        assertEquals(
            "initVector",
            fakeKeystoreEncryptedPreferences.getString("biometricInitializationVector", null),
        )
        assertEquals(
            "symmetricKey",
            fakeKeystoreEncryptedPreferences.getString("authenticatorSyncSymmetricKey", null),
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `authenticatorBridgeSymmetricSyncKey should store and update from keystore encrypted preferences`() {
        val sharedPrefsKey = "authenticatorSyncSymmetricKey"

        // Shared preferences and the repository start with the same value:
        assertNull(authDiskSource.authenticatorBridgeSymmetricSyncKey)
        assertNull(fakeKeystoreEncryptedPreferences.getString(sharedPrefsKey, null))

        // Updating the repository updates shared preferences:
        val symmetricKey = generateSecretKey().getOrThrow().encoded
        authDiskSource.authenticatorBridgeSymmetricSyncKey = symmetricKey
        assertEquals(
            symmetricKey.toString(Charsets.ISO_8859_1),
            fakeKeystoreEncryptedPreferences.getString(sharedPrefsKey, null),
        )

        // Retrieving the key from repository should give same byte array despite String conversion:
        assertTrue(authDiskSource.authenticatorBridgeSymmetricSyncKey.contentEquals(symmetricKey))
    }

    @Test
    @Suppress("MaxLineLength")
    fun `userBiometricKeyInitVector should store and update from keystore encrypted preferences`() {
        val sharedPrefsKey = "biometricInitializationVector"

        // Shared preferences and the repository start with the same value:
        assertNull(authDiskSource.userBiometricKeyInitVector)
        assertNull(fakeKeystoreEncryptedPreferences.getString(sharedPrefsKey, null))

        // Updating the repository updates shared preferences:
        val userBiometricKeyInitVector = byteArrayOf(3, 4)
        authDiskSource.userBiometricKeyInitVector = userBiometricKeyInitVector
        assertEquals(
            userBiometricKeyInitVector.toString(Charsets.ISO_8859_1),
            fakeKeystoreEncryptedPreferences.getString(sharedPrefsKey, null),
        )

        // Retrieving the key from repository should give same byte array despite String conversion:
        assertTrue(
            authDiskSource.userBiometricKeyInitVector.contentEquals(userBiometricKeyInitVector),
        )
    }

    @Test
    fun `uniqueAppId should generate a new ID and update SharedPreferences if none exists`() {
        val rememberedUniqueAppIdKey = "bwPreferencesStorage:appId"

        // Assert that the SharedPreferences are empty
        assertNull(fakeSharedPreferences.getString(rememberedUniqueAppIdKey, null))

        // Generate a new uniqueAppId and retrieve it
        val newId = authDiskSource.uniqueAppId

        // Ensure that the SharedPreferences were updated
        assertEquals(
            newId,
            fakeSharedPreferences.getString(rememberedUniqueAppIdKey, null),
        )
    }

    @Test
    fun `uniqueAppId should not generate a new ID if one exists`() {
        val rememberedUniqueAppIdKey = "bwPreferencesStorage:appId"
        val testId = "testId"

        // Update preferences to hold test value
        fakeSharedPreferences.edit { putString(rememberedUniqueAppIdKey, testId) }

        assertEquals(testId, authDiskSource.uniqueAppId)
    }
}
