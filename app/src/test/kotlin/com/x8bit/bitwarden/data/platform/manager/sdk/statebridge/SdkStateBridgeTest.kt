package com.x8bit.bitwarden.data.platform.manager.sdk.statebridge

import com.bitwarden.core.MasterPasswordUnlockData
import com.bitwarden.core.V2UpgradeToken
import com.bitwarden.core.WebAuthnPrfUnlockData
import com.bitwarden.crypto.Kdf
import com.bitwarden.network.model.KdfJson
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.MasterPasswordUnlockDataJson
import com.bitwarden.network.model.UserDecryptionOptionsJson
import com.bitwarden.network.model.V2UpgradeTokenJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.model.createMockWrappedAccountCryptographicState
import com.x8bit.bitwarden.data.auth.repository.util.updateKdf
import com.x8bit.bitwarden.data.auth.repository.util.updateMasterPasswordUnlock
import com.x8bit.bitwarden.data.vault.repository.util.toSdkMasterPasswordUnlock
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class SdkStateBridgeTest {
    private val authDiskSource = FakeAuthDiskSource()

    private val stateBridge = SdkStateBridge(
        userId = USER_ID,
        authDiskSource = authDiskSource,
    )

    @Test
    fun `setUserKey should store the user key in memory`() = runTest {
        stateBridge.setUserKey(value = "userKey")

        assertEquals("userKey", stateBridge.getUserKey())
    }

    @Test
    fun `getUserKey should return the in-memory user key`() = runTest {
        assertNull(stateBridge.getUserKey())

        stateBridge.setUserKey(value = "userKey")

        assertEquals("userKey", stateBridge.getUserKey())
    }

    @Test
    fun `clearUserKey should clear the in-memory user key`() = runTest {
        stateBridge.setUserKey(value = "userKey")

        stateBridge.clearUserKey()

        assertNull(stateBridge.getUserKey())
    }

    @Test
    fun `setUserKeyId should store the user key id`() = runTest {
        stateBridge.setUserKeyId(value = "userKeyId")

        authDiskSource.assertUserKeyId(userId = USER_ID, userKeyId = "userKeyId")
    }

    @Test
    fun `getUserKeyId should return the stored user key id`() = runTest {
        assertNull(stateBridge.getUserKeyId())

        authDiskSource.storeUserKeyId(userId = USER_ID, userKeyId = "userKeyId")

        assertEquals("userKeyId", stateBridge.getUserKeyId())
    }

    @Test
    fun `clearUserKeyId should clear the stored user key id`() = runTest {
        authDiskSource.storeUserKeyId(userId = USER_ID, userKeyId = "userKeyId")

        stateBridge.clearUserKeyId()

        authDiskSource.assertUserKeyId(userId = USER_ID, userKeyId = null)
    }

    @Test
    fun `setPersistentPinEnvelope should store the persistent pin envelope`() = runTest {
        stateBridge.setPersistentPinEnvelope(value = "pinEnvelope")

        authDiskSource.assertPersistentPinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = "pinEnvelope",
        )
    }

    @Test
    fun `getPersistentPinEnvelope should return the stored pin envelope`() = runTest {
        assertNull(stateBridge.getPersistentPinEnvelope())

        authDiskSource.storePersistentPinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = "pinEnvelope",
        )

        assertEquals("pinEnvelope", stateBridge.getPersistentPinEnvelope())
    }

    @Test
    fun `clearPersistentPinEnvelope should clear the persistent pin envelope`() = runTest {
        authDiskSource.storePersistentPinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = "pinEnvelope",
        )

        stateBridge.clearPersistentPinEnvelope()

        authDiskSource.assertPersistentPinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = null,
        )
    }

    @Test
    fun `setEphemeralPinEnvelope should store the ephemeral pin envelope`() = runTest {
        stateBridge.setEphemeralPinEnvelope(value = "pinEnvelope")

        authDiskSource.assertEphemeralPinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = "pinEnvelope",
        )
    }

    @Test
    fun `getEphemeralPinEnvelope should return the stored pin envelope`() = runTest {
        assertNull(stateBridge.getEphemeralPinEnvelope())

        authDiskSource.storeEphemeralPinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = "pinEnvelope",
        )

        assertEquals("pinEnvelope", stateBridge.getEphemeralPinEnvelope())
    }

    @Test
    fun `clearEphemeralPinEnvelope should clear the ephemeral pin envelope`() = runTest {
        authDiskSource.storeEphemeralPinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = "pinEnvelope",
        )

        stateBridge.clearEphemeralPinEnvelope()

        authDiskSource.assertEphemeralPinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = null,
        )
    }

    @Test
    fun `setEncryptedPin should store the encrypted pin`() = runTest {
        stateBridge.setEncryptedPin(value = "encryptedPin")

        authDiskSource.assertEncryptedPin(userId = USER_ID, encryptedPin = "encryptedPin")
    }

    @Test
    fun `getEncryptedPin should return the stored encrypted pin`() = runTest {
        assertNull(stateBridge.getEncryptedPin())

        authDiskSource.storeEncryptedPin(userId = USER_ID, encryptedPin = "encryptedPin")

        assertEquals("encryptedPin", stateBridge.getEncryptedPin())
    }

    @Test
    fun `clearEncryptedPin should clear the encrypted pin`() = runTest {
        authDiskSource.storeEncryptedPin(userId = USER_ID, encryptedPin = "encryptedPin")

        stateBridge.clearEncryptedPin()

        authDiskSource.assertEncryptedPin(userId = USER_ID, encryptedPin = null)
    }

    @Test
    fun `setV2UpgradeToken should store the token as a V2UpgradeTokenJson`() = runTest {
        stateBridge.setV2UpgradeToken(value = V2_UPGRADE_TOKEN)

        authDiskSource.assertV2UpgradeToken(
            userId = USER_ID,
            v2UpgradeToken = V2_UPGRADE_TOKEN_JSON,
        )
    }

    @Test
    fun `getV2UpgradeToken should return the stored token mapped to a V2UpgradeToken`() = runTest {
        assertNull(stateBridge.getV2UpgradeToken())

        authDiskSource.storeV2UpgradeToken(
            userId = USER_ID,
            v2UpgradeToken = V2_UPGRADE_TOKEN_JSON,
        )

        assertEquals(V2_UPGRADE_TOKEN, stateBridge.getV2UpgradeToken())
    }

    @Test
    fun `clearV2UpgradeToken should clear the stored token`() = runTest {
        authDiskSource.storeV2UpgradeToken(
            userId = USER_ID,
            v2UpgradeToken = V2_UPGRADE_TOKEN_JSON,
        )

        stateBridge.clearV2UpgradeToken()

        authDiskSource.assertV2UpgradeToken(userId = USER_ID, v2UpgradeToken = null)
    }

    @Test
    fun `setAccountCryptographicState should store the account cryptographic state`() = runTest {
        val state = createMockWrappedAccountCryptographicState(number = 1)

        stateBridge.setAccountCryptographicState(value = state)

        authDiskSource.assertAccountCryptographicState(
            userId = USER_ID,
            accountCryptographicState = state,
        )
    }

    @Test
    fun `getAccountCryptographicState should return the stored account cryptographic state`() =
        runTest {
            assertNull(stateBridge.getAccountCryptographicState())

            val state = createMockWrappedAccountCryptographicState(number = 1)
            authDiskSource.storeAccountCryptographicState(
                userId = USER_ID,
                accountCryptographicState = state,
            )

            assertEquals(state, stateBridge.getAccountCryptographicState())
        }

    @Test
    fun `clearAccountCryptographicState should clear the account cryptographic state`() = runTest {
        authDiskSource.storeAccountCryptographicState(
            userId = USER_ID,
            accountCryptographicState = createMockWrappedAccountCryptographicState(number = 1),
        )

        stateBridge.clearAccountCryptographicState()

        authDiskSource.assertAccountCryptographicState(
            userId = USER_ID,
            accountCryptographicState = null,
        )
    }

    @Test
    fun `setMasterpasswordUnlockData should update the user state with the unlock data`() =
        runTest {
            authDiskSource.userState = USER_STATE

            stateBridge.setMasterpasswordUnlockData(value = MASTER_PASSWORD_UNLOCK_DATA)

            assertEquals(
                USER_STATE.updateMasterPasswordUnlock(
                    userId = USER_ID,
                    masterPasswordUnlock = MASTER_PASSWORD_UNLOCK_DATA,
                ),
                authDiskSource.userState,
            )
        }

    @Test
    fun `setMasterpasswordUnlockData should do nothing when the user state is null`() = runTest {
        authDiskSource.userState = null

        stateBridge.setMasterpasswordUnlockData(value = MASTER_PASSWORD_UNLOCK_DATA)

        assertNull(authDiskSource.userState)
    }

    @Test
    fun `getMasterpasswordUnlockData should return null when there is no unlock data`() = runTest {
        authDiskSource.userState = null

        assertNull(stateBridge.getMasterpasswordUnlockData())
    }

    @Test
    fun `getMasterpasswordUnlockData should return the stored unlock data as the sdk model`() =
        runTest {
            authDiskSource.userState = USER_STATE.copy(
                accounts = mapOf(
                    USER_ID to ACCOUNT.copy(
                        profile = ACCOUNT.profile.copy(
                            userDecryptionOptions = UserDecryptionOptionsJson(
                                hasMasterPassword = true,
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = null,
                                masterPasswordUnlock = MASTER_PASSWORD_UNLOCK_DATA_JSON,
                            ),
                        ),
                    ),
                ),
            )

            assertEquals(
                MASTER_PASSWORD_UNLOCK_DATA_JSON.toSdkMasterPasswordUnlock(),
                stateBridge.getMasterpasswordUnlockData(),
            )
        }

    @Test
    fun `clearMasterpasswordUnlockData should clear the unlock data from the user state`() =
        runTest {
            authDiskSource.userState = USER_STATE.copy(
                accounts = mapOf(
                    USER_ID to ACCOUNT.copy(
                        profile = ACCOUNT.profile.copy(
                            userDecryptionOptions = UserDecryptionOptionsJson(
                                hasMasterPassword = true,
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = null,
                                masterPasswordUnlock = MASTER_PASSWORD_UNLOCK_DATA_JSON,
                            ),
                        ),
                    ),
                ),
            )

            stateBridge.clearMasterpasswordUnlockData()

            assertNull(stateBridge.getMasterpasswordUnlockData())
        }

    @Test
    fun `getKdfConfig should return null when there is no user state`() = runTest {
        authDiskSource.userState = null

        assertNull(stateBridge.getKdfConfig())
    }

    @Test
    fun `getKdfConfig should return null when the user is not present in the user state`() =
        runTest {
            val otherUserId = "otherUserId"
            authDiskSource.userState = UserStateJson(
                activeUserId = otherUserId,
                accounts = mapOf(otherUserId to ACCOUNT),
            )

            assertNull(stateBridge.getKdfConfig())
        }

    @Test
    fun `getKdfConfig should return the stored PBKDF2 params as the sdk model`() = runTest {
        authDiskSource.userState = USER_STATE

        assertEquals(Kdf.Pbkdf2(iterations = 600_000u), stateBridge.getKdfConfig())
    }

    @Test
    fun `getKdfConfig should return the stored ARGON2ID params as the sdk model`() = runTest {
        authDiskSource.userState = USER_STATE.copy(
            accounts = mapOf(
                USER_ID to ACCOUNT.copy(
                    profile = ACCOUNT.profile.copy(
                        kdfType = KdfTypeJson.ARGON2_ID,
                        kdfIterations = 3,
                        kdfMemory = 64,
                        kdfParallelism = 4,
                    ),
                ),
            ),
        )

        assertEquals(
            Kdf.Argon2id(iterations = 3u, memory = 64u, parallelism = 4u),
            stateBridge.getKdfConfig(),
        )
    }

    @Test
    fun `setKdfConfig should update the user state with the PBKDF2 params`() = runTest {
        authDiskSource.userState = USER_STATE

        stateBridge.setKdfConfig(value = Kdf.Pbkdf2(iterations = 700_000u))

        assertEquals(
            USER_STATE.updateKdf(
                userId = USER_ID,
                kdf = KdfJson(
                    kdfType = KdfTypeJson.PBKDF2_SHA256,
                    iterations = 700_000,
                    memory = null,
                    parallelism = null,
                ),
            ),
            authDiskSource.userState,
        )
    }

    @Test
    fun `setKdfConfig should update the user state with the ARGON2ID params`() = runTest {
        authDiskSource.userState = USER_STATE

        stateBridge.setKdfConfig(
            value = Kdf.Argon2id(iterations = 3u, memory = 64u, parallelism = 4u),
        )

        assertEquals(
            USER_STATE.updateKdf(
                userId = USER_ID,
                kdf = KdfJson(
                    kdfType = KdfTypeJson.ARGON2_ID,
                    iterations = 3,
                    memory = 64,
                    parallelism = 4,
                ),
            ),
            authDiskSource.userState,
        )
    }

    @Test
    fun `setKdfConfig should do nothing when the user state is null`() = runTest {
        authDiskSource.userState = null

        stateBridge.setKdfConfig(value = Kdf.Pbkdf2(iterations = 700_000u))

        assertNull(authDiskSource.userState)
    }

    @Test
    fun `clearKdfConfig should clear the kdf params from the user state`() = runTest {
        authDiskSource.userState = USER_STATE

        stateBridge.clearKdfConfig()

        assertEquals(
            USER_STATE.updateKdf(userId = USER_ID, kdf = null),
            authDiskSource.userState,
        )
    }

    @Test
    fun `clearKdfConfig should do nothing when the user state is null`() = runTest {
        authDiskSource.userState = null

        stateBridge.clearKdfConfig()

        assertNull(authDiskSource.userState)
    }

    @Test
    fun `getWebauthnPrfUnlockData should return null since unlock is unsupported`() = runTest {
        authDiskSource.userState = USER_STATE

        assertNull(stateBridge.getWebauthnPrfUnlockData())
    }

    @Test
    fun `setWebauthnPrfUnlockData should do nothing since unlock is unsupported`() = runTest {
        authDiskSource.userState = USER_STATE

        stateBridge.setWebauthnPrfUnlockData(value = WEBAUTHN_PRF_UNLOCK_DATA)

        assertNull(stateBridge.getWebauthnPrfUnlockData())
        assertEquals(USER_STATE, authDiskSource.userState)
    }

    @Test
    fun `clearWebauthnPrfUnlockData should do nothing since unlock is unsupported`() = runTest {
        authDiskSource.userState = USER_STATE

        stateBridge.clearWebauthnPrfUnlockData()

        assertNull(stateBridge.getWebauthnPrfUnlockData())
        assertEquals(USER_STATE, authDiskSource.userState)
    }
}

private const val USER_ID: String = "userId"

private val V2_UPGRADE_TOKEN: V2UpgradeToken = V2UpgradeToken(
    wrappedUserKey1 = "wrappedUserKey1",
    wrappedUserKey2 = "wrappedUserKey2",
)

private val V2_UPGRADE_TOKEN_JSON: V2UpgradeTokenJson = V2UpgradeTokenJson(
    wrappedUserKey1 = "wrappedUserKey1",
    wrappedUserKey2 = "wrappedUserKey2",
)

private val MASTER_PASSWORD_UNLOCK_DATA: MasterPasswordUnlockData = MasterPasswordUnlockData(
    kdf = Kdf.Pbkdf2(iterations = 600_000u),
    masterKeyWrappedUserKey = "masterKeyWrappedUserKey",
    salt = "salt",
)

private val MASTER_PASSWORD_UNLOCK_DATA_JSON: MasterPasswordUnlockDataJson =
    MasterPasswordUnlockDataJson(
        kdf = KdfJson(
            kdfType = KdfTypeJson.PBKDF2_SHA256,
            iterations = 600_000,
            memory = null,
            parallelism = null,
        ),
        masterKeyWrappedUserKey = "masterKeyWrappedUserKey",
        salt = "salt",
    )

private val WEBAUTHN_PRF_UNLOCK_DATA: WebAuthnPrfUnlockData = WebAuthnPrfUnlockData(
    options = emptyList(),
)

private val ACCOUNT: AccountJson = AccountJson(
    profile = AccountJson.Profile(
        userId = USER_ID,
        email = "email@bitwarden.com",
        isEmailVerified = true,
        name = "name",
        stamp = null,
        organizationId = null,
        avatarColorHex = null,
        hasPremiumPersonally = null,
        hasPremiumFromOrganization = null,
        forcePasswordResetReason = null,
        kdfType = KdfTypeJson.PBKDF2_SHA256,
        kdfIterations = 600_000,
        kdfMemory = null,
        kdfParallelism = null,
        userDecryptionOptions = null,
        isTwoFactorEnabled = false,
        creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
    ),
    tokens = mockk(),
    settings = mockk(),
)

private val USER_STATE: UserStateJson = UserStateJson(
    activeUserId = USER_ID,
    accounts = mapOf(USER_ID to ACCOUNT),
)
