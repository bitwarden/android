package com.x8bit.bitwarden.data.platform.manager.sdk.statebridge

import com.bitwarden.core.MasterPasswordUnlockData
import com.bitwarden.core.V2UpgradeToken
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
    fun `setPersistentPinEnvelope should store the persistent pin envelope`() = runTest {
        stateBridge.setPersistentPinEnvelope(value = "pinEnvelope")

        authDiskSource.assertPinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = "pinEnvelope",
            inMemoryOnly = false,
        )
    }

    @Test
    fun `getPersistentPinEnvelope should return the stored pin envelope`() = runTest {
        assertNull(stateBridge.getPersistentPinEnvelope())

        authDiskSource.storePinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = "pinEnvelope",
            inMemoryOnly = false,
        )

        assertEquals("pinEnvelope", stateBridge.getPersistentPinEnvelope())
    }

    @Test
    fun `clearPersistentPinEnvelope should clear the persistent pin envelope`() = runTest {
        authDiskSource.storePinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = "pinEnvelope",
            inMemoryOnly = false,
        )

        stateBridge.clearPersistentPinEnvelope()

        authDiskSource.assertPinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = null,
            inMemoryOnly = false,
        )
    }

    @Test
    fun `setEphemeralPinEnvelope should store the ephemeral pin envelope`() = runTest {
        stateBridge.setEphemeralPinEnvelope(value = "pinEnvelope")

        authDiskSource.assertPinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = "pinEnvelope",
            inMemoryOnly = true,
        )
    }

    @Test
    fun `getEphemeralPinEnvelope should return the stored pin envelope`() = runTest {
        assertNull(stateBridge.getEphemeralPinEnvelope())

        authDiskSource.storePinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = "pinEnvelope",
            inMemoryOnly = true,
        )

        assertEquals("pinEnvelope", stateBridge.getEphemeralPinEnvelope())
    }

    @Test
    fun `clearEphemeralPinEnvelope should clear the ephemeral pin envelope`() = runTest {
        authDiskSource.storePinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = "pinEnvelope",
            inMemoryOnly = true,
        )

        stateBridge.clearEphemeralPinEnvelope()

        authDiskSource.assertPinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = null,
            inMemoryOnly = true,
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
