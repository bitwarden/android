package com.bitwarden.authenticator.data.auth.datasource.disk

import com.bitwarden.authenticator.data.platform.base.FakeSharedPreferences
import com.bitwarden.authenticatorbridge.util.generateSecretKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthDiskSourceTest {

    private val fakeEncryptedSharedPreferences = FakeSharedPreferences()
    private val fakeSharedPreferences = FakeSharedPreferences()

    private val authDiskSource = AuthDiskSourceImpl(
        encryptedSharedPreferences = fakeEncryptedSharedPreferences,
        sharedPreferences = fakeSharedPreferences,
    )

    @Test
    @Suppress("MaxLineLength")
    fun `authenticatorBridgeSymmetricSyncKey should store and update from EncryptedSharedPreferences`() {
        val sharedPrefsKey = "bwSecureStorage:authenticatorSyncSymmetricKey"

        // Shared preferences and the repository start with the same value:
        assertNull(authDiskSource.authenticatorBridgeSymmetricSyncKey)
        assertNull(fakeEncryptedSharedPreferences.getString(sharedPrefsKey, null))

        // Updating the repository updates shared preferences:
        val symmetricKey = generateSecretKey().getOrThrow().encoded
        authDiskSource.authenticatorBridgeSymmetricSyncKey = symmetricKey
        assertEquals(
            symmetricKey.toString(Charsets.ISO_8859_1),
            fakeEncryptedSharedPreferences.getString(sharedPrefsKey, null),
        )

        // Retrieving the key from repository should give same byte array despite String conversion:
        assertTrue(authDiskSource.authenticatorBridgeSymmetricSyncKey.contentEquals(symmetricKey))
    }
}
