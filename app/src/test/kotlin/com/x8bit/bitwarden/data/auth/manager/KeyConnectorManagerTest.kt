package com.x8bit.bitwarden.data.auth.manager

import com.bitwarden.auth.KeyConnectorRegistrationResult
import com.bitwarden.core.KeyConnectorResponse
import com.bitwarden.core.WrappedAccountCryptographicState
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.crypto.Kdf
import com.bitwarden.crypto.RsaKeyPair
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.KeyConnectorKeyRequestJson
import com.bitwarden.network.model.KeyConnectorMasterKeyResponseJson
import com.bitwarden.network.model.createMockAccountKeysJson
import com.bitwarden.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.manager.model.MigrateExistingUserToKeyConnectorResult
import com.x8bit.bitwarden.data.auth.manager.model.MigrateNewUserToKeyConnectorResult
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.DeriveKeyConnectorResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class KeyConnectorManagerTest {
    private val accountsService: AccountsService = mockk()
    private val authSdkSource: AuthSdkSource = mockk()
    private val vaultSdkSource: VaultSdkSource = mockk()
    private val featureFlagManager: FeatureFlagManager = mockk {
        every { getFeatureFlag(FlagKey.V2EncryptionKeyConnector) } returns true
    }

    private val keyConnectorManager: KeyConnectorManager = KeyConnectorManagerImpl(
        accountsService = accountsService,
        authSdkSource = authSdkSource,
        vaultSdkSource = vaultSdkSource,
        featureFlagManager = featureFlagManager,
        dispatcherManager = FakeDispatcherManager(),
    )

    @Test
    fun `getMasterKeyFromKeyConnector with service failure should return failure`() = runTest {
        val expectedResult = Throwable("Fail").asFailure()
        coEvery {
            accountsService.getMasterKeyFromKeyConnector(url = URL, accessToken = ACCESS_TOKEN)
        } returns expectedResult

        val result = keyConnectorManager.getMasterKeyFromKeyConnector(
            url = URL,
            accessToken = ACCESS_TOKEN,
        )

        assertEquals(expectedResult, result)
    }

    @Test
    fun `getMasterKeyFromKeyConnector with service success should return success`() = runTest {
        val expectedResult = mockk<KeyConnectorMasterKeyResponseJson>().asSuccess()
        coEvery {
            accountsService.getMasterKeyFromKeyConnector(url = URL, accessToken = ACCESS_TOKEN)
        } returns expectedResult

        val result = keyConnectorManager.getMasterKeyFromKeyConnector(
            url = URL,
            accessToken = ACCESS_TOKEN,
        )

        assertEquals(expectedResult, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `migrateExistingUserToKeyConnector with deriveKeyConnector failure should return failure`() =
        runTest {
            val expectedResult = Throwable("Fail").asFailure()
            coEvery {
                vaultSdkSource.deriveKeyConnector(
                    userId = USER_ID,
                    userKeyEncrypted = ENCRYPTED_USER_KEY,
                    email = EMAIL,
                    password = MASTER_PASSWORD,
                    kdf = KDF,
                )
            } returns expectedResult

            val result = keyConnectorManager.migrateExistingUserToKeyConnector(
                userId = USER_ID,
                url = URL,
                userKeyEncrypted = ENCRYPTED_USER_KEY,
                email = EMAIL,
                masterPassword = MASTER_PASSWORD,
                kdf = KDF,
            )

            assertEquals(expectedResult, result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `migrateExistingUserToKeyConnector with storeMasterKeyToKeyConnector failure should return failure`() =
        runTest {
            val expectedResult = Throwable("Fail")
            coEvery {
                vaultSdkSource.deriveKeyConnector(
                    userId = USER_ID,
                    userKeyEncrypted = ENCRYPTED_USER_KEY,
                    email = EMAIL,
                    password = MASTER_PASSWORD,
                    kdf = KDF,
                )
            } returns DeriveKeyConnectorResult.Success(MASTER_KEY).asSuccess()
            coEvery {
                accountsService.storeMasterKeyToKeyConnector(url = URL, masterKey = MASTER_KEY)
            } returns expectedResult.asFailure()

            val result = keyConnectorManager.migrateExistingUserToKeyConnector(
                userId = USER_ID,
                url = URL,
                userKeyEncrypted = ENCRYPTED_USER_KEY,
                email = EMAIL,
                masterPassword = MASTER_PASSWORD,
                kdf = KDF,
            )

            assertEquals(
                MigrateExistingUserToKeyConnectorResult.Error(error = expectedResult),
                result.getOrNull(),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `migrateExistingUserToKeyConnector with wrong password error should return WrongPasswordError`() =
        runTest {
            coEvery {
                vaultSdkSource.deriveKeyConnector(
                    userId = USER_ID,
                    userKeyEncrypted = ENCRYPTED_USER_KEY,
                    email = EMAIL,
                    password = MASTER_PASSWORD,
                    kdf = KDF,
                )
            } returns DeriveKeyConnectorResult.WrongPasswordError.asSuccess()

            val result = keyConnectorManager.migrateExistingUserToKeyConnector(
                userId = USER_ID,
                url = URL,
                userKeyEncrypted = ENCRYPTED_USER_KEY,
                email = EMAIL,
                masterPassword = MASTER_PASSWORD,
                kdf = KDF,
            )

            assertEquals(
                MigrateExistingUserToKeyConnectorResult.WrongPasswordError,
                result.getOrNull(),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `migrateExistingUserToKeyConnector with convertToKeyConnector failure should return failure`() =
        runTest {
            val expectedResult = Throwable("Fail")
            coEvery {
                vaultSdkSource.deriveKeyConnector(
                    userId = USER_ID,
                    userKeyEncrypted = ENCRYPTED_USER_KEY,
                    email = EMAIL,
                    password = MASTER_PASSWORD,
                    kdf = KDF,
                )
            } returns DeriveKeyConnectorResult.Success(MASTER_KEY).asSuccess()
            coEvery {
                accountsService.storeMasterKeyToKeyConnector(url = URL, masterKey = MASTER_KEY)
            } returns Unit.asSuccess()
            coEvery { accountsService.convertToKeyConnector() } returns expectedResult.asFailure()

            val result = keyConnectorManager.migrateExistingUserToKeyConnector(
                userId = USER_ID,
                url = URL,
                userKeyEncrypted = ENCRYPTED_USER_KEY,
                email = EMAIL,
                masterPassword = MASTER_PASSWORD,
                kdf = KDF,
            )

            assertEquals(
                MigrateExistingUserToKeyConnectorResult.Error(error = expectedResult),
                result.getOrNull(),
            )
        }

    @Test
    fun `migrateExistingUserToKeyConnector should return success`() = runTest {
        coEvery {
            vaultSdkSource.deriveKeyConnector(
                userId = USER_ID,
                userKeyEncrypted = ENCRYPTED_USER_KEY,
                email = EMAIL,
                password = MASTER_PASSWORD,
                kdf = KDF,
            )
        } returns DeriveKeyConnectorResult.Success(MASTER_KEY).asSuccess()
        coEvery {
            accountsService.storeMasterKeyToKeyConnector(url = URL, masterKey = MASTER_KEY)
        } returns Unit.asSuccess()
        coEvery { accountsService.convertToKeyConnector() } returns Unit.asSuccess()

        val result = keyConnectorManager.migrateExistingUserToKeyConnector(
            userId = USER_ID,
            url = URL,
            userKeyEncrypted = ENCRYPTED_USER_KEY,
            email = EMAIL,
            masterPassword = MASTER_PASSWORD,
            kdf = KDF,
        )

        assertEquals(MigrateExistingUserToKeyConnectorResult.Success, result.getOrNull())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `migrateNewUserToKeyConnector with makeKeyConnectorKeys failure should return failure with v1 encryption`() =
        runTest {
            every {
                featureFlagManager.getFeatureFlag(FlagKey.V2EncryptionKeyConnector)
            } returns false
            val expectedResult = Throwable("Fail").asFailure()
            coEvery { authSdkSource.makeKeyConnectorKeys() } returns expectedResult

            val result = keyConnectorManager.migrateNewUserToKeyConnector(
                userId = USER_ID,
                accountKeys = createMockAccountKeysJson(number = 1),
                url = URL,
                accessToken = ACCESS_TOKEN,
                kdfType = KDF_TYPE,
                kdfIterations = KDF_ITERATIONS,
                kdfMemory = KDF_MEMORY,
                kdfParallelism = KDF_PARALLELISM,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )

            assertEquals(expectedResult, result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `migrateNewUserToKeyConnector with storeMasterKeyToKeyConnector failure should return failure with v1 encryption`() =
        runTest {
            every {
                featureFlagManager.getFeatureFlag(FlagKey.V2EncryptionKeyConnector)
            } returns false
            val keyConnectorResponse: KeyConnectorResponse = mockk {
                every { masterKey } returns MASTER_KEY
            }
            val expectedResult = Throwable("Fail").asFailure()
            coEvery {
                authSdkSource.makeKeyConnectorKeys()
            } returns keyConnectorResponse.asSuccess()
            coEvery {
                accountsService.storeMasterKeyToKeyConnector(
                    url = URL,
                    accessToken = ACCESS_TOKEN,
                    masterKey = MASTER_KEY,
                )
            } returns expectedResult

            val result = keyConnectorManager.migrateNewUserToKeyConnector(
                userId = USER_ID,
                accountKeys = createMockAccountKeysJson(number = 1),
                url = URL,
                accessToken = ACCESS_TOKEN,
                kdfType = KDF_TYPE,
                kdfIterations = KDF_ITERATIONS,
                kdfMemory = KDF_MEMORY,
                kdfParallelism = KDF_PARALLELISM,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )

            assertEquals(expectedResult, result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `migrateNewUserToKeyConnector with setKeyConnectorKey failure should return failure with v1 encryption`() =
        runTest {
            every {
                featureFlagManager.getFeatureFlag(FlagKey.V2EncryptionKeyConnector)
            } returns false
            val keyConnectorResponse = KeyConnectorResponse(
                masterKey = MASTER_KEY,
                encryptedUserKey = ENCRYPTED_USER_KEY,
                keys = RsaKeyPair(public = PUBLIC_KEY, private = PRIVATE_KEY),
            )
            val expectedResult = Throwable("Fail").asFailure()
            coEvery {
                authSdkSource.makeKeyConnectorKeys()
            } returns keyConnectorResponse.asSuccess()
            coEvery {
                accountsService.storeMasterKeyToKeyConnector(
                    url = URL,
                    accessToken = ACCESS_TOKEN,
                    masterKey = MASTER_KEY,
                )
            } returns Unit.asSuccess()
            coEvery {
                accountsService.setKeyConnectorKey(
                    accessToken = ACCESS_TOKEN,
                    body = KeyConnectorKeyRequestJson(
                        userKey = ENCRYPTED_USER_KEY,
                        keys = KeyConnectorKeyRequestJson.Keys(
                            publicKey = PUBLIC_KEY,
                            encryptedPrivateKey = PRIVATE_KEY,
                        ),
                        kdfType = KDF_TYPE,
                        kdfIterations = KDF_ITERATIONS,
                        kdfMemory = KDF_MEMORY,
                        kdfParallelism = KDF_PARALLELISM,
                        organizationIdentifier = ORGANIZATION_IDENTIFIER,
                    ),
                )
            } returns expectedResult

            val result = keyConnectorManager.migrateNewUserToKeyConnector(
                userId = USER_ID,
                accountKeys = createMockAccountKeysJson(number = 1),
                url = URL,
                accessToken = ACCESS_TOKEN,
                kdfType = KDF_TYPE,
                kdfIterations = KDF_ITERATIONS,
                kdfMemory = KDF_MEMORY,
                kdfParallelism = KDF_PARALLELISM,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )

            assertEquals(expectedResult, result)
        }

    @Test
    fun `migrateNewUserToKeyConnector should return success with v1 encryption`() = runTest {
        every {
            featureFlagManager.getFeatureFlag(FlagKey.V2EncryptionKeyConnector)
        } returns false
        val keyConnectorResponse = KeyConnectorResponse(
            masterKey = MASTER_KEY,
            encryptedUserKey = ENCRYPTED_USER_KEY,
            keys = RsaKeyPair(public = PUBLIC_KEY, private = PRIVATE_KEY),
        )
        coEvery {
            authSdkSource.makeKeyConnectorKeys()
        } returns keyConnectorResponse.asSuccess()
        coEvery {
            accountsService.storeMasterKeyToKeyConnector(
                url = URL,
                accessToken = ACCESS_TOKEN,
                masterKey = MASTER_KEY,
            )
        } returns Unit.asSuccess()
        coEvery {
            accountsService.setKeyConnectorKey(
                accessToken = ACCESS_TOKEN,
                body = KeyConnectorKeyRequestJson(
                    userKey = ENCRYPTED_USER_KEY,
                    keys = KeyConnectorKeyRequestJson.Keys(
                        publicKey = PUBLIC_KEY,
                        encryptedPrivateKey = PRIVATE_KEY,
                    ),
                    kdfType = KDF_TYPE,
                    kdfIterations = KDF_ITERATIONS,
                    kdfMemory = KDF_MEMORY,
                    kdfParallelism = KDF_PARALLELISM,
                    organizationIdentifier = ORGANIZATION_IDENTIFIER,
                ),
            )
        } returns Unit.asSuccess()

        val result = keyConnectorManager.migrateNewUserToKeyConnector(
            userId = USER_ID,
            accountKeys = null,
            url = URL,
            accessToken = ACCESS_TOKEN,
            kdfType = KDF_TYPE,
            kdfIterations = KDF_ITERATIONS,
            kdfMemory = KDF_MEMORY,
            kdfParallelism = KDF_PARALLELISM,
            organizationIdentifier = ORGANIZATION_IDENTIFIER,
        )

        assertEquals(
            MigrateNewUserToKeyConnectorResult(
                privateKey = PRIVATE_KEY,
                masterKey = MASTER_KEY,
                encryptedUserKey = ENCRYPTED_USER_KEY,
                accountCryptographicState = WrappedAccountCryptographicState.V1(
                    privateKey = PRIVATE_KEY,
                ),
            ),
            result.getOrThrow(),
        )
    }

    @Test
    fun `migrateNewUserToKeyConnector with SDK failure should return failure`() = runTest {
        val expected = Throwable("Fail").asFailure()
        coEvery {
            authSdkSource.postKeysForKeyConnectorRegistration(
                userId = USER_ID,
                accessToken = ACCESS_TOKEN,
                keyConnectorUrl = URL,
                ssoOrganizationIdentifier = ORGANIZATION_IDENTIFIER,
            )
        } returns expected

        val result = keyConnectorManager.migrateNewUserToKeyConnector(
            userId = USER_ID,
            accountKeys = createMockAccountKeysJson(number = 1),
            url = URL,
            accessToken = ACCESS_TOKEN,
            kdfType = KDF_TYPE,
            kdfIterations = KDF_ITERATIONS,
            kdfMemory = KDF_MEMORY,
            kdfParallelism = KDF_PARALLELISM,
            organizationIdentifier = ORGANIZATION_IDENTIFIER,
        )

        assertEquals(expected, result)
    }

    @Test
    fun `migrateNewUserToKeyConnector should return succeed`() = runTest {
        val accountCryptographicState = WrappedAccountCryptographicState.V2(
            privateKey = PRIVATE_KEY,
            signedPublicKey = SIGNED_PUBLIC_KEY,
            signingKey = SIGNING_KEY,
            securityState = SECURITY_STATE,
        )
        val keyConnectorRegistrationResult = KeyConnectorRegistrationResult(
            accountCryptographicState = accountCryptographicState,
            keyConnectorKey = MASTER_KEY,
            keyConnectorKeyWrappedUserKey = ENCRYPTED_USER_KEY,
            userKey = USER_KEY,
        )
        coEvery {
            authSdkSource.postKeysForKeyConnectorRegistration(
                userId = USER_ID,
                accessToken = ACCESS_TOKEN,
                keyConnectorUrl = URL,
                ssoOrganizationIdentifier = ORGANIZATION_IDENTIFIER,
            )
        } returns keyConnectorRegistrationResult.asSuccess()

        val result = keyConnectorManager.migrateNewUserToKeyConnector(
            userId = USER_ID,
            accountKeys = createMockAccountKeysJson(number = 1),
            url = URL,
            accessToken = ACCESS_TOKEN,
            kdfType = KDF_TYPE,
            kdfIterations = KDF_ITERATIONS,
            kdfMemory = KDF_MEMORY,
            kdfParallelism = KDF_PARALLELISM,
            organizationIdentifier = ORGANIZATION_IDENTIFIER,
        )

        assertEquals(
            MigrateNewUserToKeyConnectorResult(
                privateKey = PRIVATE_KEY,
                masterKey = MASTER_KEY,
                encryptedUserKey = ENCRYPTED_USER_KEY,
                accountCryptographicState = accountCryptographicState,
            ),
            result.getOrThrow(),
        )
    }
}

private const val ACCESS_TOKEN: String = "token"
private const val USER_ID: String = "userId"
private const val URL: String = "www.example.com"
private const val USER_KEY: String = "userKey"
private const val ENCRYPTED_USER_KEY: String = "userKeyEncrypted"
private const val EMAIL: String = "email@email.com"
private const val MASTER_PASSWORD: String = "masterPassword"
private const val MASTER_KEY: String = "masterKey"
private const val PUBLIC_KEY: String = "publicKey"
private const val PRIVATE_KEY: String = "privateKey"
private const val SIGNED_PUBLIC_KEY: String = "mockSignedPublicKey-1"
private const val SIGNING_KEY: String = "mockWrappedSigningKey-1"
private const val SECURITY_STATE: String = "mockSecurityState-1"
private const val ORGANIZATION_IDENTIFIER: String = "org_identifier"
private val KDF: Kdf = mockk()
private val KDF_TYPE: KdfTypeJson = KdfTypeJson.ARGON2_ID
private const val KDF_ITERATIONS: Int = 1
private const val KDF_MEMORY: Int = 2
private const val KDF_PARALLELISM: Int = 3
