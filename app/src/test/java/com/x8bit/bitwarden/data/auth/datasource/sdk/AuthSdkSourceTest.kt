package com.x8bit.bitwarden.data.auth.datasource.sdk

import com.bitwarden.core.AuthRequestResponse
import com.bitwarden.core.FingerprintRequest
import com.bitwarden.core.KeyConnectorResponse
import com.bitwarden.core.MasterPasswordPolicyOptions
import com.bitwarden.core.RegisterKeyResponse
import com.bitwarden.core.RegisterTdeKeyResponse
import com.bitwarden.crypto.HashPurpose
import com.bitwarden.crypto.Kdf
import com.bitwarden.sdk.Client
import com.bitwarden.sdk.ClientAuth
import com.bitwarden.sdk.ClientPlatform
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager
import com.x8bit.bitwarden.data.platform.util.asSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthSdkSourceTest {
    private val clientAuth = mockk<ClientAuth>()
    private val clientPlatform = mockk<ClientPlatform> {
        coEvery { loadFlags(any()) } just runs
    }
    private val client = mockk<Client> {
        every { auth() } returns clientAuth
        every { platform() } returns clientPlatform
    }
    private val sdkClientManager = mockk<SdkClientManager> {
        coEvery { getOrCreateClient(userId = null) } returns client
    }

    private val authSkdSource: AuthSdkSource = AuthSdkSourceImpl(
        sdkClientManager = sdkClientManager,
    )

    @Test
    fun `getNewAuthRequest should call SDK and return a Result with correct data`() = runBlocking {
        val email = "test@gmail.com"
        val expectedResult = mockk<AuthRequestResponse>()
        coEvery {
            clientAuth.newAuthRequest(email)
        } returns expectedResult

        val result = authSkdSource.getNewAuthRequest(email)

        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            clientAuth.newAuthRequest(email)
        }
    }

    @Test
    fun `getUserFingerprint should call SDK and return a Result with correct data`() = runBlocking {
        val email = "email@gmail.com"
        val publicKey = "publicKey"
        val expectedResult = "fingerprint"
        coEvery {
            clientPlatform.fingerprint(
                req = FingerprintRequest(
                    fingerprintMaterial = email,
                    publicKey = publicKey,
                ),
            )
        } returns expectedResult

        val result = authSkdSource.getUserFingerprint(email, publicKey)
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            clientPlatform.fingerprint(
                req = FingerprintRequest(
                    fingerprintMaterial = email,
                    publicKey = publicKey,
                ),
            )
        }
    }

    @Test
    fun `hashPassword should call SDK and return a Result with the correct data`() = runBlocking {
        val email = "email"
        val password = "password"
        val kdf = mockk<Kdf>()
        val purpose = mockk<HashPurpose>()
        val expectedResult = "hashedPassword"
        coEvery {
            clientAuth.hashPassword(
                email = email,
                password = password,
                kdfParams = kdf,
                purpose = purpose,
            )
        } returns expectedResult

        val result = authSkdSource.hashPassword(
            email = email,
            password = password,
            kdf = kdf,
            purpose = purpose,
        )
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            clientAuth.hashPassword(
                email = email,
                password = password,
                kdfParams = kdf,
                purpose = purpose,
            )
        }
    }

    @Test
    fun `makeKeyConnectorKeys should call SDK and return a Result with the correct data`() =
        runBlocking {
            val expectedResult = mockk<KeyConnectorResponse>()
            coEvery { clientAuth.makeKeyConnectorKeys() } returns expectedResult

            val result = authSkdSource.makeKeyConnectorKeys()

            assertEquals(expectedResult.asSuccess(), result)
            coVerify(exactly = 1) {
                clientAuth.makeKeyConnectorKeys()
            }
        }

    @Test
    fun `makeRegisterKeys should call SDK and return a Result with the correct data`() =
        runBlocking {
            val email = "email"
            val password = "password"
            val kdf = mockk<Kdf>()
            val expectedResult = mockk<RegisterKeyResponse>()
            coEvery {
                clientAuth.makeRegisterKeys(
                    email = email,
                    password = password,
                    kdf = kdf,
                )
            } returns expectedResult

            val result = authSkdSource.makeRegisterKeys(
                email = email,
                password = password,
                kdf = kdf,
            )
            assertEquals(
                expectedResult.asSuccess(),
                result,
            )
            coVerify {
                clientAuth.makeRegisterKeys(
                    email = email,
                    password = password,
                    kdf = kdf,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `makeRegisterTdeKeysAndUnlockVault should call SDK and return a Result with the correct data`() =
        runBlocking {
            val userId = "userId"
            val email = "email"
            val orgPublicKey = "orgPublicKey"
            val rememberDevice = true
            val expectedResult = mockk<RegisterTdeKeyResponse>()
            coEvery { sdkClientManager.getOrCreateClient(userId = userId) } returns client
            coEvery {
                clientAuth.makeRegisterTdeKeys(
                    email = email,
                    orgPublicKey = orgPublicKey,
                    rememberDevice = rememberDevice,
                )
            } returns expectedResult

            val result = authSkdSource.makeRegisterTdeKeysAndUnlockVault(
                userId = userId,
                email = email,
                orgPublicKey = orgPublicKey,
                rememberDevice = rememberDevice,
            )
            assertEquals(expectedResult.asSuccess(), result)
            coVerify(exactly = 1) {
                clientAuth.makeRegisterTdeKeys(
                    email = email,
                    orgPublicKey = orgPublicKey,
                    rememberDevice = rememberDevice,
                )
            }
        }

    @Test
    fun `passwordStrength should call SDK and return a Result with the correct data`() =
        runBlocking {
            val email = "email"
            val password = "password"
            val additionalInputs = listOf("test1", "test2")
            val sdkResult = 3.toUByte()
            val expectedResult = PasswordStrength.LEVEL_3
            coEvery {
                clientAuth.passwordStrength(
                    email = email,
                    password = password,
                    additionalInputs = additionalInputs,
                )
            } returns sdkResult

            val result = authSkdSource.passwordStrength(
                email = email,
                password = password,
                additionalInputs = additionalInputs,
            )
            assertEquals(
                expectedResult.asSuccess(),
                result,
            )
            coVerify {
                clientAuth.passwordStrength(
                    email = email,
                    password = password,
                    additionalInputs = additionalInputs,
                )
            }
        }

    @Test
    fun `satisfiesPolicy should call SDK and return a Result with the correct data`() =
        runBlocking {
            val password = "password"
            val passwordStrength = PasswordStrength.LEVEL_3
            val rawStrength = 3.toUByte()
            val policy = mockk<MasterPasswordPolicyOptions>()
            val expectedResult = true
            coEvery {
                clientAuth.satisfiesPolicy(
                    password = password,
                    strength = rawStrength,
                    policy = policy,
                )
            } returns expectedResult

            val result = authSkdSource.satisfiesPolicy(
                password = password,
                passwordStrength = passwordStrength,
                policy = policy,
            )
            assertEquals(
                expectedResult.asSuccess(),
                result,
            )
            coVerify {
                clientAuth.satisfiesPolicy(
                    password = password,
                    strength = rawStrength,
                    policy = policy,
                )
            }
        }
}
