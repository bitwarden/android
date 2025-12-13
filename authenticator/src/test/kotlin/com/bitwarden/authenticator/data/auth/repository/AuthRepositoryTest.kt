package com.bitwarden.authenticator.data.auth.repository

import app.cash.turbine.test
import com.bitwarden.authenticator.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.bitwarden.authenticator.data.platform.manager.BiometricsEncryptionManager
import com.bitwarden.authenticator.data.platform.repository.model.BiometricsKeyResult
import com.bitwarden.authenticator.data.platform.repository.model.BiometricsUnlockResult
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.realtime.RealtimeManager
import com.bitwarden.core.data.repository.error.MissingPropertyException
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.GeneralSecurityException
import javax.crypto.Cipher

class AuthRepositoryTest {
    private val authDiskSource: FakeAuthDiskSource = FakeAuthDiskSource()
    private val authenticatorSdkSource: AuthenticatorSdkSource = mockk()
    private val biometricsEncryptionManager: BiometricsEncryptionManager = mockk()
    private val realtimeManager: RealtimeManager = mockk()

    private val authRepository: AuthRepository = AuthRepositoryImpl(
        authDiskSource = authDiskSource,
        authenticatorSdkSource = authenticatorSdkSource,
        biometricsEncryptionManager = biometricsEncryptionManager,
        realtimeManager = realtimeManager,
        dispatcherManager = FakeDispatcherManager(),
    )

    @BeforeEach
    fun setup() {
        mockkConstructor(MissingPropertyException::class)
        every {
            anyConstructed<MissingPropertyException>() == any<MissingPropertyException>()
        } returns true
    }

    @Test
    fun `isUnlockWithBiometricsEnabled should update based on AuthDiskSource`() {
        assertFalse(authRepository.isUnlockWithBiometricsEnabled)
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")
        assertTrue(authRepository.isUnlockWithBiometricsEnabled)
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = null)
        assertFalse(authRepository.isUnlockWithBiometricsEnabled)
    }

    @Test
    fun `isUnlockWithBiometricsEnabledFlow should react to changes in AuthDiskSource`() = runTest {
        authRepository.isUnlockWithBiometricsEnabledFlow.test {
            assertFalse(awaitItem())
            authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")
            assertTrue(awaitItem())
            authDiskSource.storeUserBiometricUnlockKey(biometricsKey = null)
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `setupBiometricsKey on generateBiometricsKey error returns Error`() = runTest {
        val error = Throwable("Fail!")
        coEvery { authenticatorSdkSource.generateBiometricsKey() } returns error.asFailure()

        val result = authRepository.setupBiometricsKey(cipher = CIPHER)

        assertEquals(BiometricsKeyResult.Error(error = error), result)
        coVerify(exactly = 1) {
            authenticatorSdkSource.generateBiometricsKey()
        }
    }

    @Test
    fun `setupBiometricsKey on cipher encryption failure returns Error`() = runTest {
        val biometricsKey = "biometricsKey"
        val error = GeneralSecurityException("Fail!")
        coEvery { authenticatorSdkSource.generateBiometricsKey() } returns biometricsKey.asSuccess()
        every { CIPHER.doFinal(any()) } throws error

        val result = authRepository.setupBiometricsKey(cipher = CIPHER)

        assertEquals(BiometricsKeyResult.Error(error = error), result)
        coVerify(exactly = 1) {
            authenticatorSdkSource.generateBiometricsKey()
        }
    }

    @Test
    fun `setupBiometricsKey on cipher encryption success returns Success`() = runTest {
        val biometricsKey = "biometricsKey"
        val encryptedBytes = byteArrayOf(1, 1)
        val iv = byteArrayOf(2, 2)
        coEvery { authenticatorSdkSource.generateBiometricsKey() } returns biometricsKey.asSuccess()
        every { CIPHER.doFinal(any()) } returns encryptedBytes
        every { CIPHER.iv } returns iv

        val result = authRepository.setupBiometricsKey(cipher = CIPHER)

        assertEquals(BiometricsKeyResult.Success, result)
        authDiskSource.assertUserBiometricUnlockKey(encryptedBytes.toString(Charsets.ISO_8859_1))
        authDiskSource.assertUserBiometricKeyInitVector(iv)
        coVerify(exactly = 1) {
            authenticatorSdkSource.generateBiometricsKey()
        }
    }

    @Test
    fun `unlockWithBiometrics without stored biometrics key returns InvalidStateError`() = runTest {
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = null)

        val result = authRepository.unlockWithBiometrics(cipher = CIPHER)

        assertEquals(
            BiometricsUnlockResult.InvalidStateError(
                error = MissingPropertyException("Biometric key"),
            ),
            result,
        )
    }

    @Test
    fun `unlockWithBiometrics with iv and decryption error returns BiometricDecodingError`() =
        runTest {
            val biometricsKey = "biometricsKey"
            val initVector = byteArrayOf(1, 2)
            val error = GeneralSecurityException("Fail!")
            authDiskSource.storeUserBiometricUnlockKey(biometricsKey = biometricsKey)
            authDiskSource.userBiometricKeyInitVector = initVector
            every { CIPHER.doFinal(any()) } throws error

            val result = authRepository.unlockWithBiometrics(cipher = CIPHER)

            assertEquals(BiometricsUnlockResult.BiometricDecodingError(error), result)
        }

    @Test
    fun `unlockWithBiometrics with iv and decryption success returns Success`() = runTest {
        val biometricsKey = "biometricsKey"
        val initVector = byteArrayOf(1, 2)
        val encryptedBytes = byteArrayOf(1, 1)
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = biometricsKey)
        authDiskSource.userBiometricKeyInitVector = initVector
        every { CIPHER.doFinal(any()) } returns encryptedBytes

        val result = authRepository.unlockWithBiometrics(cipher = CIPHER)

        assertEquals(BiometricsUnlockResult.Success, result)
    }

    @Test
    fun `unlockWithBiometrics without iv and encryption failure returns BiometricDecodingError`() =
        runTest {
            val biometricsKey = "biometricsKey"
            val error = GeneralSecurityException("Fail!")
            authDiskSource.storeUserBiometricUnlockKey(biometricsKey = biometricsKey)
            authDiskSource.userBiometricKeyInitVector = null
            every { CIPHER.doFinal(any()) } throws error

            val result = authRepository.unlockWithBiometrics(cipher = CIPHER)

            assertEquals(BiometricsUnlockResult.BiometricDecodingError(error), result)
        }

    @Test
    fun `unlockWithBiometrics without iv and encryption success returns Success`() = runTest {
        val biometricsKey = "biometricsKey"
        val encryptedBytes = byteArrayOf(2, 2)
        val initVector = byteArrayOf(3, 3)
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = biometricsKey)
        authDiskSource.userBiometricKeyInitVector = null
        every { CIPHER.doFinal(any()) } returns encryptedBytes
        every { CIPHER.iv } returns initVector

        val result = authRepository.unlockWithBiometrics(cipher = CIPHER)

        assertEquals(BiometricsUnlockResult.Success, result)
        authDiskSource.assertUserBiometricUnlockKey(encryptedBytes.toString(Charsets.ISO_8859_1))
        authDiskSource.assertUserBiometricKeyInitVector(initVector)
    }

    @Test
    fun `updateLastActiveTime should store the current elapsedRealtimeMs`() {
        val elapsedMs = 1234L
        every { realtimeManager.elapsedRealtimeMs } returns elapsedMs

        authRepository.updateLastActiveTime()

        authDiskSource.assertLastActiveTimeMillis(elapsedMs)
        coVerify(exactly = 1) {
            realtimeManager.elapsedRealtimeMs
        }
    }
}

private val CIPHER: Cipher = mockk()
