package com.x8bit.bitwarden.data.platform.manager.keyrotation

import app.cash.turbine.test
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KeyRotationManagerTest {

    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val vaultSdkSource: VaultSdkSource = mockk {
        every { getKeyId(userKey = AUTHENTICATOR_SYNC_KEY) } returns STALE_KEY_ID.asSuccess()
        every { getKeyId(userKey = AUTO_UNLOCK_KEY) } returns STALE_KEY_ID.asSuccess()
        every { getKeyId(userKey = BIOMETRICS_KEY) } returns STALE_KEY_ID.asSuccess()
        every { getKeyId(userKey = USER_ENCRYPTION_KEY) } returns CURRENT_KEY_ID.asSuccess()
        coEvery { getUserEncryptionKey(userId = USER_ID) } returns USER_ENCRYPTION_KEY.asSuccess()
    }

    private val keyRotationManager: KeyRotationManager = KeyRotationManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        vaultSdkSource = vaultSdkSource,
    )

    //region rotateAuthenticatorSyncKey

    @Test
    fun `rotateAuthenticatorSyncKey should do nothing when there is no stored auto-unlock key`() =
        runTest {
            keyRotationManager.rotateAuthenticatorSyncKey(userId = USER_ID)

            fakeAuthDiskSource.assertAuthenticatorSyncKey(
                userId = USER_ID,
                authenticatorSyncKey = null,
            )
            verify(exactly = 0) { vaultSdkSource.getKeyId(userKey = any()) }
            coVerify(exactly = 0) { vaultSdkSource.getUserEncryptionKey(userId = any()) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `rotateAuthenticatorSyncKey should store the user encryption key when the key IDs do not match`() =
        runTest {
            fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
                userId = USER_ID,
                authenticatorSyncUnlockKey = AUTHENTICATOR_SYNC_KEY,
            )

            keyRotationManager.rotateAuthenticatorSyncKey(userId = USER_ID)

            fakeAuthDiskSource.assertAuthenticatorSyncKey(
                userId = USER_ID,
                authenticatorSyncKey = USER_ENCRYPTION_KEY,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `rotateAuthenticatorSyncKey should not store the user encryption key when the key IDs match`() =
        runTest {
            every {
                vaultSdkSource.getKeyId(userKey = AUTHENTICATOR_SYNC_KEY)
            } returns CURRENT_KEY_ID.asSuccess()
            fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
                userId = USER_ID,
                authenticatorSyncUnlockKey = AUTHENTICATOR_SYNC_KEY,
            )

            keyRotationManager.rotateAuthenticatorSyncKey(userId = USER_ID)

            fakeAuthDiskSource.assertAuthenticatorSyncKey(
                userId = USER_ID,
                authenticatorSyncKey = AUTHENTICATOR_SYNC_KEY,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `rotateAuthenticatorSyncKey should not store the user encryption key when getting the ID of the stored key fails`() =
        runTest {
            every {
                vaultSdkSource.getKeyId(userKey = AUTHENTICATOR_SYNC_KEY)
            } returns ERROR.asFailure()
            fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
                userId = USER_ID,
                authenticatorSyncUnlockKey = AUTHENTICATOR_SYNC_KEY,
            )

            keyRotationManager.rotateAuthenticatorSyncKey(userId = USER_ID)

            fakeAuthDiskSource.assertAuthenticatorSyncKey(
                userId = USER_ID,
                authenticatorSyncKey = AUTHENTICATOR_SYNC_KEY,
            )
            coVerify(exactly = 0) { vaultSdkSource.getUserEncryptionKey(userId = any()) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `rotateAuthenticatorSyncKey should not store the user encryption key when getting the user encryption key fails`() =
        runTest {
            coEvery {
                vaultSdkSource.getUserEncryptionKey(userId = USER_ID)
            } returns ERROR.asFailure()
            fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
                userId = USER_ID,
                authenticatorSyncUnlockKey = AUTHENTICATOR_SYNC_KEY,
            )

            keyRotationManager.rotateAuthenticatorSyncKey(userId = USER_ID)

            fakeAuthDiskSource.assertAuthenticatorSyncKey(
                userId = USER_ID,
                authenticatorSyncKey = AUTHENTICATOR_SYNC_KEY,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `rotateAuthenticatorSyncKey should not store the user encryption key when getting the ID of the user encryption key fails`() =
        runTest {
            every {
                vaultSdkSource.getKeyId(userKey = USER_ENCRYPTION_KEY)
            } returns ERROR.asFailure()
            fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
                userId = USER_ID,
                authenticatorSyncUnlockKey = AUTHENTICATOR_SYNC_KEY,
            )

            keyRotationManager.rotateAuthenticatorSyncKey(userId = USER_ID)

            fakeAuthDiskSource.assertAuthenticatorSyncKey(
                userId = USER_ID,
                authenticatorSyncKey = AUTHENTICATOR_SYNC_KEY,
            )
        }

    //endregion rotateAuthenticatorSyncKey

    //region rotateAutoUnlockKey

    @Test
    fun `rotateAutoUnlockKey should do nothing when there is no stored auto-unlock key`() =
        runTest {
            keyRotationManager.rotateAutoUnlockKey(userId = USER_ID)

            fakeAuthDiskSource.assertUserAutoUnlockKey(userId = USER_ID, userAutoUnlockKey = null)
            verify(exactly = 0) { vaultSdkSource.getKeyId(userKey = any()) }
            coVerify(exactly = 0) { vaultSdkSource.getUserEncryptionKey(userId = any()) }
        }

    @Test
    fun `rotateAutoUnlockKey should store the user encryption key when the key IDs do not match`() =
        runTest {
            fakeAuthDiskSource.storeUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = AUTO_UNLOCK_KEY,
            )

            keyRotationManager.rotateAutoUnlockKey(userId = USER_ID)

            fakeAuthDiskSource.assertUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = USER_ENCRYPTION_KEY,
            )
        }

    @Test
    fun `rotateAutoUnlockKey should not store the user encryption key when the key IDs match`() =
        runTest {
            every {
                vaultSdkSource.getKeyId(userKey = AUTO_UNLOCK_KEY)
            } returns CURRENT_KEY_ID.asSuccess()
            fakeAuthDiskSource.storeUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = AUTO_UNLOCK_KEY,
            )

            keyRotationManager.rotateAutoUnlockKey(userId = USER_ID)

            fakeAuthDiskSource.assertUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = AUTO_UNLOCK_KEY,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `rotateAutoUnlockKey should not store the user encryption key when getting the ID of the stored key fails`() =
        runTest {
            every { vaultSdkSource.getKeyId(userKey = AUTO_UNLOCK_KEY) } returns ERROR.asFailure()
            fakeAuthDiskSource.storeUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = AUTO_UNLOCK_KEY,
            )

            keyRotationManager.rotateAutoUnlockKey(userId = USER_ID)

            fakeAuthDiskSource.assertUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = AUTO_UNLOCK_KEY,
            )
            coVerify(exactly = 0) { vaultSdkSource.getUserEncryptionKey(userId = any()) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `rotateAutoUnlockKey should not store the user encryption key when getting the user encryption key fails`() =
        runTest {
            coEvery {
                vaultSdkSource.getUserEncryptionKey(userId = USER_ID)
            } returns ERROR.asFailure()
            fakeAuthDiskSource.storeUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = AUTO_UNLOCK_KEY,
            )

            keyRotationManager.rotateAutoUnlockKey(userId = USER_ID)

            fakeAuthDiskSource.assertUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = AUTO_UNLOCK_KEY,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `rotateAutoUnlockKey should not store the user encryption key when getting the ID of the user encryption key fails`() =
        runTest {
            every {
                vaultSdkSource.getKeyId(userKey = USER_ENCRYPTION_KEY)
            } returns ERROR.asFailure()
            fakeAuthDiskSource.storeUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = AUTO_UNLOCK_KEY,
            )

            keyRotationManager.rotateAutoUnlockKey(userId = USER_ID)

            fakeAuthDiskSource.assertUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = AUTO_UNLOCK_KEY,
            )
        }

    //endregion rotateAutoUnlockKey

    //region rotateBiometricsKey

    @Test
    fun `rotateBiometricsKey should emit shouldRotateBiometricKey when the key IDs do not match`() =
        runTest {
            keyRotationManager.shouldRotateBiometricKey.test {
                expectNoEvents()
                keyRotationManager.rotateBiometricsKey(
                    userId = USER_ID,
                    decryptedBiometricsUserKey = BIOMETRICS_KEY,
                )
                assertEquals(Unit, awaitItem())
            }
        }

    @Test
    fun `rotateBiometricsKey should not emit shouldRotateBiometricKey when the key IDs match`() =
        runTest {
            every { vaultSdkSource.getKeyId(userKey = BIOMETRICS_KEY) } returns
                CURRENT_KEY_ID.asSuccess()

            keyRotationManager.shouldRotateBiometricKey.test {
                keyRotationManager.rotateBiometricsKey(
                    userId = USER_ID,
                    decryptedBiometricsUserKey = BIOMETRICS_KEY,
                )
                expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `rotateBiometricsKey should not emit shouldRotateBiometricKey when getting the ID of the biometrics key fails`() =
        runTest {
            every { vaultSdkSource.getKeyId(userKey = BIOMETRICS_KEY) } returns ERROR.asFailure()

            keyRotationManager.shouldRotateBiometricKey.test {
                keyRotationManager.rotateBiometricsKey(
                    userId = USER_ID,
                    decryptedBiometricsUserKey = BIOMETRICS_KEY,
                )
                expectNoEvents()
            }
            coVerify(exactly = 0) { vaultSdkSource.getUserEncryptionKey(userId = any()) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `rotateBiometricsKey should not emit shouldRotateBiometricKey when getting the user encryption key fails`() =
        runTest {
            coEvery {
                vaultSdkSource.getUserEncryptionKey(userId = USER_ID)
            } returns ERROR.asFailure()

            keyRotationManager.shouldRotateBiometricKey.test {
                keyRotationManager.rotateBiometricsKey(
                    userId = USER_ID,
                    decryptedBiometricsUserKey = BIOMETRICS_KEY,
                )
                expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `rotateBiometricsKey should not emit shouldRotateBiometricKey when getting the ID of the user encryption key fails`() =
        runTest {
            every {
                vaultSdkSource.getKeyId(userKey = USER_ENCRYPTION_KEY)
            } returns ERROR.asFailure()

            keyRotationManager.shouldRotateBiometricKey.test {
                keyRotationManager.rotateBiometricsKey(
                    userId = USER_ID,
                    decryptedBiometricsUserKey = BIOMETRICS_KEY,
                )
                expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `rotateBiometricsKey should not store the rotated key since it requires user interaction`() =
        runTest {
            keyRotationManager.shouldRotateBiometricKey.test {
                keyRotationManager.rotateBiometricsKey(
                    userId = USER_ID,
                    decryptedBiometricsUserKey = BIOMETRICS_KEY,
                )
                assertEquals(Unit, awaitItem())
            }

            fakeAuthDiskSource.assertUserAutoUnlockKey(userId = USER_ID, userAutoUnlockKey = null)
        }

    //endregion rotateBiometricsKey
}

private const val USER_ID: String = "userId"
private const val AUTHENTICATOR_SYNC_KEY: String = "authenticatorSyncKey"
private const val AUTO_UNLOCK_KEY: String = "autoUnlockKey"
private const val BIOMETRICS_KEY: String = "biometricsKey"
private const val USER_ENCRYPTION_KEY: String = "userEncryptionKey"
private const val STALE_KEY_ID: String = "staleKeyId"
private const val CURRENT_KEY_ID: String = "currentKeyId"
private val ERROR: Throwable = Throwable("Fail!")
