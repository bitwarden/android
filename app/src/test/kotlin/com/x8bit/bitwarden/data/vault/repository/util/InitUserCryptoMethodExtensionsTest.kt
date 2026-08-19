package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.core.AuthRequestMethod
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.MasterPasswordUnlockData
import com.bitwarden.crypto.Kdf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class InitUserCryptoMethodExtensionsTest {
    @Test
    fun `password returns the password for MasterPasswordUnlock`() {
        assertEquals(
            MASTER_PASSWORD,
            MASTER_PASSWORD_UNLOCK_METHOD.password,
        )
    }

    @Test
    fun `password returns null for all non-MasterPasswordUnlock methods`() {
        LOG_TAG_MAP
            .keys
            .filterNot { it is InitUserCryptoMethod.MasterPasswordUnlock }
            .forEach { method -> assertNull(method.password) }
    }

    @Test
    fun `logTag returns the correct label for each method`() {
        LOG_TAG_MAP.forEach { (method, expectedLogTag) ->
            assertEquals(expectedLogTag, method.logTag)
        }
    }
}

private const val MASTER_PASSWORD: String = "mockMasterPassword"

/**
 * Must be declared as the [InitUserCryptoMethod] supertype. Typed as the concrete
 * `MasterPasswordUnlock` subclass, `password` would resolve to that data class's own member
 * property instead of the extension under test.
 */
private val MASTER_PASSWORD_UNLOCK_METHOD: InitUserCryptoMethod =
    InitUserCryptoMethod.MasterPasswordUnlock(
        password = MASTER_PASSWORD,
        masterPasswordUnlock = MasterPasswordUnlockData(
            kdf = Kdf.Pbkdf2(iterations = 1u),
            masterKeyWrappedUserKey = "mockMasterKeyWrappedUserKey",
            salt = "mockSalt",
        ),
    )

/**
 * Every [InitUserCryptoMethod] subclass mapped to its expected log tag. Adding a subclass to the
 * SDK breaks the `when` blocks in the production code; this map must be updated alongside them.
 */
private val LOG_TAG_MAP: Map<InitUserCryptoMethod, String> = mapOf(
    MASTER_PASSWORD_UNLOCK_METHOD to "Master Password Unlock",
    InitUserCryptoMethod.AuthRequest(
        requestPrivateKey = "mockRequestPrivateKey",
        method = AuthRequestMethod.UserKey(protectedUserKey = "mockProtectedUserKey"),
    ) to "Auth Request",
    InitUserCryptoMethod.DecryptedKey(
        decryptedUserKey = "mockDecryptedUserKey",
    ) to "Decrypted Key (Never Lock/Biometrics)",
    InitUserCryptoMethod.DeviceKey(
        deviceKey = "mockDeviceKey",
        protectedDevicePrivateKey = "mockProtectedDevicePrivateKey",
        deviceProtectedUserKey = "mockDeviceProtectedUserKey",
    ) to "Device Key",
    InitUserCryptoMethod.KeyConnector(
        masterKey = "mockMasterKey",
        userKey = "mockUserKey",
    ) to "Key Connector",
    InitUserCryptoMethod.KeyConnectorUrl(
        url = "mockUrl",
        keyConnectorKeyWrappedUserKey = "mockKeyConnectorKeyWrappedUserKey",
    ) to "Key Connector Url",
    InitUserCryptoMethod.Pin(
        pin = "mockPin",
        pinProtectedUserKey = "mockPinProtectedUserKey",
    ) to "Pin",
    InitUserCryptoMethod.PinEnvelope(
        pin = "mockPin",
        pinProtectedUserKeyEnvelope = "mockPinProtectedUserKeyEnvelope",
    ) to "Pin Envelope",
    InitUserCryptoMethod.PinState(pin = "mockPin") to "Pin State",
)
