package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.EnrollPinResponse
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class PinProtectedUserKeyManagerTest {

    private val fakeAuthDiskSource: FakeAuthDiskSource = FakeAuthDiskSource()
    private val vaultSdkSource: VaultSdkSource = mockk()

    private val pinProtectedUserKeyManager: PinProtectedUserKeyManager =
        PinProtectedUserKeyManagerImpl(
            authDiskSource = fakeAuthDiskSource,
            vaultSdkSource = vaultSdkSource,
        )

    @Test
    fun `deriveTemporaryPinProtectedUserKeyIfNecessary without encryptedKey does nothing`() =
        runTest {
            fakeAuthDiskSource.storeEncryptedPin(userId = USER_ID, encryptedPin = null)

            pinProtectedUserKeyManager.deriveTemporaryPinProtectedUserKeyIfNecessary(
                userId = USER_ID,
            )

            coVerify(exactly = 0) {
                vaultSdkSource.enrollPinWithEncryptedPin(userId = any(), encryptedPin = any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `deriveTemporaryPinProtectedUserKeyIfNecessary with encrypted key and existing pinProtectedUserKeyEnvelope does nothing`() =
        runTest {
            fakeAuthDiskSource.storeEncryptedPin(userId = USER_ID, encryptedPin = "encryptedPin")
            fakeAuthDiskSource.storePinProtectedUserKeyEnvelope(
                userId = USER_ID,
                pinProtectedUserKeyEnvelope = "pinProtectedUserKeyEnvelope",
            )

            pinProtectedUserKeyManager.deriveTemporaryPinProtectedUserKeyIfNecessary(
                userId = USER_ID,
            )

            coVerify(exactly = 0) {
                vaultSdkSource.enrollPinWithEncryptedPin(userId = any(), encryptedPin = any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `deriveTemporaryPinProtectedUserKeyIfNecessary with enrollment success should store new pin data`() =
        runTest {
            val encryptedPin = "encryptedPin"
            val pinProtectedUserKeyEnvelope = "pinProtectedUserKeyEnvelope"
            val userKeyEncryptedPin = "userKeyEncryptedPin"
            val enrollPinResponse = EnrollPinResponse(
                pinProtectedUserKeyEnvelope = pinProtectedUserKeyEnvelope,
                userKeyEncryptedPin = userKeyEncryptedPin,
            )
            fakeAuthDiskSource.storeEncryptedPin(userId = USER_ID, encryptedPin = encryptedPin)
            fakeAuthDiskSource.storePinProtectedUserKeyEnvelope(
                userId = USER_ID,
                pinProtectedUserKeyEnvelope = null,
            )
            coEvery {
                vaultSdkSource.enrollPinWithEncryptedPin(
                    userId = USER_ID,
                    encryptedPin = encryptedPin,
                )
            } returns enrollPinResponse.asSuccess()

            pinProtectedUserKeyManager.deriveTemporaryPinProtectedUserKeyIfNecessary(
                userId = USER_ID,
            )

            coVerify(exactly = 1) {
                vaultSdkSource.enrollPinWithEncryptedPin(
                    userId = USER_ID,
                    encryptedPin = encryptedPin,
                )
            }
            fakeAuthDiskSource.assertEncryptedPin(
                userId = USER_ID,
                encryptedPin = userKeyEncryptedPin,
            )
            fakeAuthDiskSource.assertPinProtectedUserKeyEnvelope(
                userId = USER_ID,
                pinProtectedUserKeyEnvelope = pinProtectedUserKeyEnvelope,
                inMemoryOnly = true,
            )
            fakeAuthDiskSource.assertPinProtectedUserKey(
                userId = USER_ID,
                pinProtectedUserKey = null,
                inMemoryOnly = false,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `deriveTemporaryPinProtectedUserKeyIfNecessary with enrollment failure should clear all pin data`() =
        runTest {
            val encryptedPin = "encryptedPin"
            val error = Throwable("Fail!")
            fakeAuthDiskSource.storeEncryptedPin(userId = USER_ID, encryptedPin = encryptedPin)
            fakeAuthDiskSource.storePinProtectedUserKeyEnvelope(
                userId = USER_ID,
                pinProtectedUserKeyEnvelope = null,
            )
            coEvery {
                vaultSdkSource.enrollPinWithEncryptedPin(
                    userId = USER_ID,
                    encryptedPin = encryptedPin,
                )
            } returns error.asFailure()

            pinProtectedUserKeyManager.deriveTemporaryPinProtectedUserKeyIfNecessary(
                userId = USER_ID,
            )

            coVerify(exactly = 1) {
                vaultSdkSource.enrollPinWithEncryptedPin(
                    userId = USER_ID,
                    encryptedPin = encryptedPin,
                )
            }
            fakeAuthDiskSource.assertEncryptedPin(
                userId = USER_ID,
                encryptedPin = null,
            )
            fakeAuthDiskSource.assertPinProtectedUserKeyEnvelope(
                userId = USER_ID,
                pinProtectedUserKeyEnvelope = null,
                inMemoryOnly = false,
            )
            fakeAuthDiskSource.assertPinProtectedUserKey(
                userId = USER_ID,
                pinProtectedUserKey = null,
                inMemoryOnly = false,
            )
        }

    @Test
    fun `migratePinProtectedUserKeyIfNeeded without encryptedKey does nothing`() =
        runTest {
            fakeAuthDiskSource.storeEncryptedPin(userId = USER_ID, encryptedPin = null)

            pinProtectedUserKeyManager.migratePinProtectedUserKeyIfNeeded(userId = USER_ID)

            coVerify(exactly = 0) {
                vaultSdkSource.enrollPinWithEncryptedPin(userId = any(), encryptedPin = any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `migratePinProtectedUserKeyIfNeeded with encrypted key and existing pinProtectedUserKeyEnvelope does nothing`() =
        runTest {
            fakeAuthDiskSource.storeEncryptedPin(userId = USER_ID, encryptedPin = "encryptedPin")
            fakeAuthDiskSource.storePinProtectedUserKeyEnvelope(
                userId = USER_ID,
                pinProtectedUserKeyEnvelope = "pinProtectedUserKeyEnvelope",
            )

            pinProtectedUserKeyManager.migratePinProtectedUserKeyIfNeeded(userId = USER_ID)

            coVerify(exactly = 0) {
                vaultSdkSource.enrollPinWithEncryptedPin(userId = any(), encryptedPin = any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `migratePinProtectedUserKeyIfNeeded with enrollment success should store new pin data in memory`() =
        runTest {
            val encryptedPin = "encryptedPin"
            val pinProtectedUserKeyEnvelope = "pinProtectedUserKeyEnvelope"
            val userKeyEncryptedPin = "userKeyEncryptedPin"
            val enrollPinResponse = EnrollPinResponse(
                pinProtectedUserKeyEnvelope = pinProtectedUserKeyEnvelope,
                userKeyEncryptedPin = userKeyEncryptedPin,
            )
            fakeAuthDiskSource.storeEncryptedPin(userId = USER_ID, encryptedPin = encryptedPin)
            fakeAuthDiskSource.storePinProtectedUserKeyEnvelope(
                userId = USER_ID,
                pinProtectedUserKeyEnvelope = null,
            )
            fakeAuthDiskSource.storePinProtectedUserKey(
                userId = USER_ID,
                pinProtectedUserKey = null,
            )
            coEvery {
                vaultSdkSource.enrollPinWithEncryptedPin(
                    userId = USER_ID,
                    encryptedPin = encryptedPin,
                )
            } returns enrollPinResponse.asSuccess()

            pinProtectedUserKeyManager.migratePinProtectedUserKeyIfNeeded(userId = USER_ID)

            coVerify(exactly = 1) {
                vaultSdkSource.enrollPinWithEncryptedPin(
                    userId = USER_ID,
                    encryptedPin = encryptedPin,
                )
            }
            fakeAuthDiskSource.assertEncryptedPin(
                userId = USER_ID,
                encryptedPin = userKeyEncryptedPin,
            )
            fakeAuthDiskSource.assertPinProtectedUserKeyEnvelope(
                userId = USER_ID,
                pinProtectedUserKeyEnvelope = pinProtectedUserKeyEnvelope,
                inMemoryOnly = true,
            )
            fakeAuthDiskSource.assertPinProtectedUserKey(
                userId = USER_ID,
                pinProtectedUserKey = null,
                inMemoryOnly = false,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `migratePinProtectedUserKeyIfNeeded with enrollment failure should clear all pin data at disk level`() =
        runTest {
            val encryptedPin = "encryptedPin"
            val pinProtectedUserKey = "pinProtectedUserKey"
            val error = Throwable("Fail!")
            fakeAuthDiskSource.storeEncryptedPin(userId = USER_ID, encryptedPin = encryptedPin)
            fakeAuthDiskSource.storePinProtectedUserKeyEnvelope(
                userId = USER_ID,
                pinProtectedUserKeyEnvelope = null,
            )
            fakeAuthDiskSource.storePinProtectedUserKey(
                userId = USER_ID,
                pinProtectedUserKey = pinProtectedUserKey,
            )
            coEvery {
                vaultSdkSource.enrollPinWithEncryptedPin(
                    userId = USER_ID,
                    encryptedPin = encryptedPin,
                )
            } returns error.asFailure()

            pinProtectedUserKeyManager.migratePinProtectedUserKeyIfNeeded(userId = USER_ID)

            coVerify(exactly = 1) {
                vaultSdkSource.enrollPinWithEncryptedPin(
                    userId = USER_ID,
                    encryptedPin = encryptedPin,
                )
            }
            fakeAuthDiskSource.assertEncryptedPin(
                userId = USER_ID,
                encryptedPin = null,
            )
            fakeAuthDiskSource.assertPinProtectedUserKeyEnvelope(
                userId = USER_ID,
                pinProtectedUserKeyEnvelope = null,
                inMemoryOnly = false,
            )
            fakeAuthDiskSource.assertPinProtectedUserKey(
                userId = USER_ID,
                pinProtectedUserKey = null,
                inMemoryOnly = false,
            )
        }
}

private const val USER_ID: String = "user_id"
