package com.x8bit.bitwarden.data.vault.repository

import app.cash.turbine.test
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.DateTime
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.MasterPasswordUnlockData
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.repository.error.MissingPropertyException
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.exporters.ExportFormat
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.network.model.CipherTypeJson
import com.bitwarden.network.model.MasterPasswordUnlockDataJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.UserDecryptionOptionsJson
import com.bitwarden.network.model.createMockCipher
import com.bitwarden.network.model.createMockFolder
import com.bitwarden.network.model.createMockOrganizationKeys
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.send.SendView
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.DecryptCipherListResult
import com.bitwarden.vault.FolderView
import com.bitwarden.vault.TotpResponse
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toKdfRequestModel
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockAccount
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFolder
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkSend
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.manager.CredentialExchangeImportManager
import com.x8bit.bitwarden.data.vault.manager.PinProtectedUserKeyManager
import com.x8bit.bitwarden.data.vault.manager.TotpCodeManager
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.manager.VaultSyncManager
import com.x8bit.bitwarden.data.vault.manager.model.ImportCxfPayloadResult
import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.data.vault.repository.model.DomainsData
import com.x8bit.bitwarden.data.vault.repository.model.ExportVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.ImportCredentialsResult
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.createWrappedAccountCryptographicState
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import com.x8bit.bitwarden.data.vault.repository.util.toSdkMasterPasswordUnlock
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.util.createVerificationCodeItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkConstructor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.GeneralSecurityException
import java.time.ZonedDateTime
import javax.crypto.BadPaddingException
import javax.crypto.Cipher

@Suppress("LargeClass")
class VaultRepositoryTest {
    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val mutableGetCiphersFlow: MutableStateFlow<List<SyncResponseJson.Cipher>> =
        MutableStateFlow(listOf(createMockCipher(1)))
    private val vaultDiskSource: VaultDiskSource = mockk {
        every { getCiphersFlow(any()) } returns mutableGetCiphersFlow
    }
    private val totpCodeManager: TotpCodeManager = mockk()
    private val vaultSdkSource: VaultSdkSource = mockk()
    private val vaultLockManager: VaultLockManager = mockk()
    private val mutableVaultDataStateFlow =
        MutableStateFlow<DataState<VaultData>>(DataState.Loading)
    private val mutableCollectionsStateFlow =
        MutableStateFlow<DataState<List<CollectionView>>>(DataState.Loading)
    private val mutableDecryptCipherListResultStateFlow =
        MutableStateFlow<DataState<DecryptCipherListResult>>(DataState.Loading)
    private val mutableDomainsStateFlow =
        MutableStateFlow<DataState<DomainsData>>(DataState.Loading)
    private val mutableFoldersStateFlow =
        MutableStateFlow<DataState<List<FolderView>>>(DataState.Loading)
    private val mutableSendDataStateFlow = MutableStateFlow<DataState<SendData>>(DataState.Loading)
    private val vaultSyncManager: VaultSyncManager = mockk {
        every { vaultDataStateFlow } returns mutableVaultDataStateFlow
        every { collectionsStateFlow } returns mutableCollectionsStateFlow
        every { decryptCipherListResultStateFlow } returns mutableDecryptCipherListResultStateFlow
        every { domainsStateFlow } returns mutableDomainsStateFlow
        every { foldersStateFlow } returns mutableFoldersStateFlow
        every { sendDataStateFlow } returns mutableSendDataStateFlow
    }
    private val credentialExchangeImportManager: CredentialExchangeImportManager = mockk()
    private val pinProtectedUserKeyManager: PinProtectedUserKeyManager = mockk {
        coEvery { deriveTemporaryPinProtectedUserKeyIfNecessary(userId = any()) } just runs
    }

    private val vaultRepository = VaultRepositoryImpl(
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = fakeAuthDiskSource,
        vaultLockManager = vaultLockManager,
        dispatcherManager = dispatcherManager,
        totpCodeManager = totpCodeManager,
        cipherManager = mockk(),
        folderManager = mockk(),
        sendManager = mockk(),
        vaultSyncManager = vaultSyncManager,
        credentialExchangeImportManager = credentialExchangeImportManager,
        pinProtectedUserKeyManager = pinProtectedUserKeyManager,
    )

    @BeforeEach
    fun setup() {
        mockkConstructor(NoActiveUserException::class)
        mockkConstructor(MissingPropertyException::class)
        every {
            anyConstructed<NoActiveUserException>() == any<NoActiveUserException>()
        } returns true
        every {
            anyConstructed<MissingPropertyException>() == any<MissingPropertyException>()
        } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(NoActiveUserException::class)
        unmockkConstructor(MissingPropertyException::class)
    }

    @Test
    fun `deleteVaultData should call deleteVaultData on VaultDiskSource`() {
        val userId = "userId-1234"
        coEvery { vaultDiskSource.deleteVaultData(userId) } just runs

        vaultRepository.deleteVaultData(userId = userId)

        coVerify(exactly = 1) {
            vaultDiskSource.deleteVaultData(userId)
        }
    }

    @Test
    fun `unlockVaultWithBiometrics with missing user state should return InvalidStateError`() =
        runTest {
            fakeAuthDiskSource.userState = null
            val cipher = mockk<Cipher>()

            val result = vaultRepository.unlockVaultWithBiometrics(cipher = cipher)

            assertEquals(
                VaultUnlockResult.InvalidStateError(error = NoActiveUserException()),
                result,
            )
        }

    @Test
    fun `unlockVaultWithBiometrics with missing biometrics key should return InvalidStateError`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val cipher = mockk<Cipher>()
            val userId = MOCK_USER_STATE.activeUserId
            fakeAuthDiskSource.storeUserBiometricUnlockKey(userId = userId, biometricsKey = null)

            val result = vaultRepository.unlockVaultWithBiometrics(cipher = cipher)

            assertEquals(
                VaultUnlockResult.InvalidStateError(error = MissingPropertyException("Foo")),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithBiometrics with failure to decode biometrics key should return BiometricDecodingError`() =
        runTest {
            val userId = MOCK_USER_STATE.activeUserId
            val privateKey = "mockPrivateKey-1"
            val biometricsKey = "asdf1234"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val initVector = byteArrayOf(2, 2)
            val cipher = mockk<Cipher> {
                every { doFinal(any()) } throws BadPaddingException()
            }
            fakeAuthDiskSource.apply {
                storeUserBiometricInitVector(userId = userId, iv = initVector)
                storeUserBiometricUnlockKey(userId = userId, biometricsKey = biometricsKey)
                storePrivateKey(userId = userId, privateKey = privateKey)
            }

            val result = vaultRepository.unlockVaultWithBiometrics(cipher = cipher)

            assertEquals(
                VaultUnlockResult.BiometricDecodingError(
                    error = MissingPropertyException("Foo"),
                ),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithBiometrics with failure to encode biometrics key should return BiometricDecodingError`() =
        runTest {
            val userId = MOCK_USER_STATE.activeUserId
            val biometricsKey = "asdf1234"
            val error = GeneralSecurityException()
            val cipher = mockk<Cipher> {
                every { doFinal(any()) } throws error
            }
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            fakeAuthDiskSource.storeUserBiometricUnlockKey(
                userId = userId,
                biometricsKey = biometricsKey,
            )

            val result = vaultRepository.unlockVaultWithBiometrics(cipher = cipher)

            assertEquals(
                VaultUnlockResult.BiometricDecodingError(error = error),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithBiometrics with an IV and VaultLockManager Success should store the updated key and IV and unlock for the current user and return Success`() =
        runTest {
            val userId = MOCK_USER_STATE.activeUserId
            val privateKey = "mockPrivateKey-1"
            val biometricsKey = "asdf1234"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val encryptedBytes = byteArrayOf(1, 1)
            val initVector = byteArrayOf(2, 2)
            val cipher = mockk<Cipher> {
                every { doFinal(any()) } returns encryptedBytes
            }
            coEvery {
                vaultLockManager.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = privateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    email = "email",
                    kdf = MOCK_PROFILE.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = encryptedBytes.toString(Charsets.ISO_8859_1),
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            fakeAuthDiskSource.apply {
                storeUserBiometricInitVector(userId = userId, iv = initVector)
                storeUserBiometricUnlockKey(userId = userId, biometricsKey = biometricsKey)
                storePrivateKey(userId = userId, privateKey = privateKey)
            }

            val result = vaultRepository.unlockVaultWithBiometrics(cipher = cipher)

            assertEquals(VaultUnlockResult.Success, result)
            coVerify(exactly = 1) {
                vaultLockManager.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = privateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    email = "email",
                    kdf = MOCK_PROFILE.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = encryptedBytes.toString(Charsets.ISO_8859_1),
                    ),
                    organizationKeys = null,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithBiometrics with VaultLockManager Success and a stored encrypted pin should unlock for the current user, derive a new pin-protected key, and return Success`() =
        runTest {
            val userId = MOCK_USER_STATE.activeUserId
            val privateKey = "mockPrivateKey-1"
            val biometricsKey = "asdf1234"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val encryptedBytes = byteArrayOf(1, 1)
            val initVector = byteArrayOf(2, 2)
            val cipher = mockk<Cipher> {
                every { doFinal(any()) } returns encryptedBytes
                every { iv } returns initVector
            }
            coEvery {
                vaultLockManager.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = privateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    email = "email",
                    kdf = MOCK_PROFILE.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = biometricsKey,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            fakeAuthDiskSource.apply {
                storeUserBiometricInitVector(userId = userId, iv = null)
                storeUserBiometricUnlockKey(userId = userId, biometricsKey = biometricsKey)
                storePrivateKey(userId = userId, privateKey = privateKey)
            }

            val result = vaultRepository.unlockVaultWithBiometrics(cipher = cipher)

            assertEquals(VaultUnlockResult.Success, result)
            coVerify {
                vaultLockManager.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = "mockPrivateKey-1",
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    email = "email",
                    kdf = MOCK_PROFILE.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = biometricsKey,
                    ),
                    organizationKeys = null,
                )
                pinProtectedUserKeyManager.deriveTemporaryPinProtectedUserKeyIfNecessary(
                    userId = userId,
                )
            }
            fakeAuthDiskSource.apply {
                assertBiometricsKey(
                    userId = userId,
                    biometricsKey = encryptedBytes.toString(Charsets.ISO_8859_1),
                )
                assertBiometricInitVector(userId = userId, iv = initVector)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `unlockVaultWithDecryptedUserKey with VaultLockManager Success should return Success`() =
        runTest {
            val userId = MOCK_USER_STATE.activeUserId
            val authenticatorSyncUnlockKey = "asdf1234"
            val privateKey = "mockPrivateKey-1"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultLockManager.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = privateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    email = "email",
                    kdf = MOCK_PROFILE.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = authenticatorSyncUnlockKey,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            fakeAuthDiskSource.apply {
                storeAuthenticatorSyncUnlockKey(
                    userId = userId,
                    authenticatorSyncUnlockKey = authenticatorSyncUnlockKey,
                )
                storePrivateKey(userId = userId, privateKey = privateKey)
            }

            val result = vaultRepository.unlockVaultWithDecryptedUserKey(
                userId = userId,
                decryptedUserKey = authenticatorSyncUnlockKey,
            )
            assertEquals(VaultUnlockResult.Success, result)
            coVerify {
                vaultLockManager.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = "mockPrivateKey-1",
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    email = "email",
                    kdf = MOCK_PROFILE.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = authenticatorSyncUnlockKey,
                    ),
                    organizationKeys = null,
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `unlockVaultWithDecryptedUserKey with VaultLockManager InvalidStateError should return InvalidStateError`() =
        runTest {
            val userId = MOCK_USER_STATE.activeUserId
            val authenticatorSyncUnlockKey = "asdf1234"
            val privateKey = "mockPrivateKey-1"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val error = Throwable("Fail")
            coEvery {
                vaultLockManager.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = privateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    email = "email",
                    kdf = MOCK_PROFILE.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = authenticatorSyncUnlockKey,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.InvalidStateError(error = error)
            fakeAuthDiskSource.apply {
                storeAuthenticatorSyncUnlockKey(
                    userId = userId,
                    authenticatorSyncUnlockKey = authenticatorSyncUnlockKey,
                )
                storePrivateKey(userId = userId, privateKey = privateKey)
            }

            val result = vaultRepository.unlockVaultWithDecryptedUserKey(
                userId = userId,
                decryptedUserKey = authenticatorSyncUnlockKey,
            )
            assertEquals(VaultUnlockResult.InvalidStateError(error = error), result)
            coVerify {
                vaultLockManager.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = "mockPrivateKey-1",
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    email = "email",
                    kdf = MOCK_PROFILE.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = authenticatorSyncUnlockKey,
                    ),
                    organizationKeys = null,
                )
            }
        }

    @Test
    fun `unlockVaultWithMasterPassword with missing user state should return InvalidStateError`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = vaultRepository.unlockVaultWithMasterPassword(masterPassword = "")

            assertEquals(
                VaultUnlockResult.InvalidStateError(error = NoActiveUserException()),
                result,
            )
        }

    @Test
    fun `unlockVaultWithMasterPassword with missing user key should return InvalidStateError`() =
        runTest {
            val result = vaultRepository.unlockVaultWithMasterPassword(masterPassword = "")
            fakeAuthDiskSource.storeUserKey(
                userId = "mockId-1",
                userKey = null,
            )
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockId-1",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            assertEquals(
                VaultUnlockResult.InvalidStateError(error = MissingPropertyException("Foo")),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithMasterPassword with missing private key should return InvalidStateError`() =
        runTest {
            val result = vaultRepository.unlockVaultWithMasterPassword(masterPassword = "")
            fakeAuthDiskSource.storeUserKey(
                userId = "mockId-1",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockId-1",
                privateKey = null,
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            assertEquals(
                VaultUnlockResult.InvalidStateError(error = MissingPropertyException("Foo")),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithMasterPassword with VaultLockManager Success and a stored encrypted pin should unlock for the current user, derive a new pin-protected key, and return Success`() =
        runTest {
            val userId = "mockId-1"
            val mockVaultUnlockResult = VaultUnlockResult.Success
            prepareStateForUnlocking(unlockResult = mockVaultUnlockResult)

            val result = vaultRepository.unlockVaultWithMasterPassword(
                masterPassword = "mockPassword-1",
            )

            assertEquals(
                mockVaultUnlockResult,
                result,
            )
            coVerify {
                vaultLockManager.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = "mockPrivateKey-1",
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    email = "email",
                    kdf = MOCK_PROFILE.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = "mockPassword-1",
                        masterPasswordUnlock = MasterPasswordUnlockData(
                            kdf = MOCK_PROFILE.toSdkParams(),
                            masterKeyWrappedUserKey = "mockKey-1",
                            salt = "mockSalt-1",
                        ),
                    ),
                    organizationKeys = createMockOrganizationKeys(number = 1),
                )
                pinProtectedUserKeyManager.deriveTemporaryPinProtectedUserKeyIfNecessary(
                    userId = userId,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithMasterPassword with masterPasswordUnlock data should use MasterPasswordUnlock method`() =
        runTest {
            val userId = "mockId-1"
            val masterPassword = "mockPassword-1"
            val masterPasswordUnlockData = MOCK_MASTER_PASSWORD_UNLOCK_DATA
                .toSdkMasterPasswordUnlock()
            val userState = MOCK_USER_STATE.copy(
                accounts = mapOf(
                    "mockId-1" to MOCK_ACCOUNT.copy(
                        profile = MOCK_PROFILE.copy(
                            userDecryptionOptions = UserDecryptionOptionsJson(
                                hasMasterPassword = true,
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = null,
                                masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                            ),
                        ),
                    ),
                ),
            )

            fakeAuthDiskSource.storeUserKey(
                userId = userId,
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.userState = userState
            fakeAuthDiskSource.storePrivateKey(userId = userId, privateKey = "mockPrivateKey-1")

            coEvery {
                vaultLockManager.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = "mockPrivateKey-1",
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    email = "email",
                    kdf = MOCK_PROFILE.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = masterPassword,
                        masterPasswordUnlock = masterPasswordUnlockData,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success

            val result = vaultRepository.unlockVaultWithMasterPassword(
                masterPassword = masterPassword,
            )

            assertEquals(VaultUnlockResult.Success, result)
            coVerify {
                vaultLockManager.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = "mockPrivateKey-1",
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    email = "email",
                    kdf = MOCK_PROFILE.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = masterPassword,
                        masterPasswordUnlock = masterPasswordUnlockData,
                    ),
                    organizationKeys = null,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithMasterPassword without masterPasswordUnlock data should return InvalidStateError`() =
        runTest {
            val userId = "mockId-1"
            val masterPassword = "mockPassword-1"
            val userKey = "mockUserKey-1"
            val userState = MOCK_USER_STATE.copy(
                accounts = mapOf(
                    "mockId-1" to MOCK_ACCOUNT.copy(
                        profile = MOCK_PROFILE.copy(
                            userDecryptionOptions = null,
                        ),
                    ),
                ),
            )
            fakeAuthDiskSource.userState = userState
            fakeAuthDiskSource.storePrivateKey(userId = userId, privateKey = "mockPrivateKey-1")
            fakeAuthDiskSource.storeUserKey(userId = userId, userKey = userKey)

            val result = vaultRepository.unlockVaultWithMasterPassword(
                masterPassword = masterPassword,
            )

            assertTrue(result is VaultUnlockResult.InvalidStateError)
            coVerify(exactly = 0) {
                vaultLockManager.unlockVault(any(), any(), any(), any(), any(), any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithMasterPassword with VaultLockManager non-Success should unlock for the current user and return the error`() =
        runTest {
            val userId = "mockId-1"
            val mockVaultUnlockResult = VaultUnlockResult.InvalidStateError(error = null)
            prepareStateForUnlocking(unlockResult = mockVaultUnlockResult)

            val result = vaultRepository.unlockVaultWithMasterPassword(
                masterPassword = "mockPassword-1",
            )

            assertEquals(
                mockVaultUnlockResult,
                result,
            )
            coVerify {
                vaultLockManager.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = "mockPrivateKey-1",
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    email = "email",
                    kdf = MOCK_PROFILE.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = "mockPassword-1",
                        masterPasswordUnlock = MasterPasswordUnlockData(
                            kdf = MOCK_PROFILE.toSdkParams(),
                            masterKeyWrappedUserKey = "mockKey-1",
                            salt = "mockSalt-1",
                        ),
                    ),
                    organizationKeys = createMockOrganizationKeys(number = 1),
                )
            }
        }

    @Test
    fun `unlockVaultWithPin with missing user state should return InvalidStateError`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = vaultRepository.unlockVaultWithPin(pin = "1234")

        assertEquals(
            VaultUnlockResult.InvalidStateError(error = NoActiveUserException()),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithPin with missing pin-protected user key should return InvalidStateError`() =
        runTest {
            val result = vaultRepository.unlockVaultWithPin(pin = "1234")
            fakeAuthDiskSource.storePinProtectedUserKey(
                userId = "mockId-1",
                pinProtectedUserKey = null,
            )
            fakeAuthDiskSource.storePinProtectedUserKeyEnvelope(
                userId = "mockId-1",
                pinProtectedUserKeyEnvelope = null,
            )
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockId-1",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            assertEquals(
                VaultUnlockResult.InvalidStateError(error = MissingPropertyException("Foo")),
                result,
            )
        }

    @Test
    fun `unlockVaultWithPin with missing private key should return InvalidStateError`() = runTest {
        val result = vaultRepository.unlockVaultWithPin(pin = "1234")
        fakeAuthDiskSource.storePinProtectedUserKey(
            userId = "mockId-1",
            pinProtectedUserKey = "mockKey-1",
        )
        fakeAuthDiskSource.storePinProtectedUserKeyEnvelope(
            userId = "mockId-1",
            pinProtectedUserKeyEnvelope = null,
        )
        fakeAuthDiskSource.storePrivateKey(
            userId = "mockId-1",
            privateKey = null,
        )
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        assertEquals(
            VaultUnlockResult.InvalidStateError(error = MissingPropertyException("Foo")),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithPin with VaultLockManager Success should unlock for the current user and return Success`() =
        runTest {
            val userId = "mockId-1"
            val mockVaultUnlockResult = VaultUnlockResult.Success
            prepareStateForUnlocking(unlockResult = mockVaultUnlockResult)

            val result = vaultRepository.unlockVaultWithPin(pin = "1234")

            assertEquals(
                mockVaultUnlockResult,
                result,
            )
            coVerify {
                vaultLockManager.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = "mockPrivateKey-1",
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    email = "email",
                    kdf = MOCK_PROFILE.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.PinEnvelope(
                        pin = "1234",
                        pinProtectedUserKeyEnvelope = "mockKey-1",
                    ),
                    organizationKeys = createMockOrganizationKeys(number = 1),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithPin with PinProtectedUserKeyEnvelope null and VaultLockManager Success should unlock with pin for the current user and return Success`() =
        runTest {
            val userId = "mockId-1"
            val mockVaultUnlockResult = VaultUnlockResult.Success
            prepareStateForUnlocking(unlockResult = mockVaultUnlockResult)

            fakeAuthDiskSource.storePinProtectedUserKeyEnvelope(
                userId = userId,
                pinProtectedUserKeyEnvelope = null,
            )

            val result = vaultRepository.unlockVaultWithPin(pin = "1234")
            assertEquals(
                mockVaultUnlockResult,
                result,
            )
            coVerify {
                vaultLockManager.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = "mockPrivateKey-1",
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    email = "email",
                    kdf = MOCK_PROFILE.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.Pin(
                        pin = "1234",
                        pinProtectedUserKey = "mockKey-1",
                    ),
                    organizationKeys = createMockOrganizationKeys(number = 1),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithPin with VaultLockManager non-Success should unlock for the current user and return the error`() =
        runTest {
            val userId = "mockId-1"
            val mockVaultUnlockResult = VaultUnlockResult.InvalidStateError(error = null)
            prepareStateForUnlocking(unlockResult = mockVaultUnlockResult)

            val result = vaultRepository.unlockVaultWithPin(pin = "1234")

            assertEquals(
                mockVaultUnlockResult,
                result,
            )
            coVerify {
                vaultLockManager.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = "mockPrivateKey-1",
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    email = "email",
                    kdf = MOCK_PROFILE.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.PinEnvelope(
                        pin = "1234",
                        pinProtectedUserKeyEnvelope = "mockKey-1",
                    ),
                    organizationKeys = createMockOrganizationKeys(number = 1),
                )
            }
        }

    @Test
    fun `getVaultItemStateFlow should update to Error when error state is emitted`() =
        runTest {
            val folderId = 1234
            val folderIdString = "mockId-$folderId"
            val throwable = Throwable("Fail")
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.getVaultItemStateFlow(folderIdString).test {
                assertEquals(DataState.Loading, awaitItem())
                mutableVaultDataStateFlow.value = DataState.Error(throwable)
                assertEquals(DataState.Error<CipherView>(throwable), awaitItem())
            }
        }

    @Test
    fun `getVaultItemStateFlow should update to NoNetwork when a NoNetwork value is emitted`() =
        runTest {
            val itemId = 1234
            val itemIdString = "mockId-$itemId"
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.getVaultItemStateFlow(itemIdString).test {
                assertEquals(DataState.Loading, awaitItem())
                mutableVaultDataStateFlow.value = DataState.NoNetwork()
                assertEquals(DataState.NoNetwork<CipherView>(), awaitItem())
            }
        }

    @Test
    fun `getVaultFolderStateFlow should update to NoNetwork when no network value is emitted`() =
        runTest {
            val folderId = 1234
            val folderIdString = "mockId-$folderId"
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.getVaultFolderStateFlow(folderIdString).test {
                assertEquals(DataState.Loading, awaitItem())
                mutableVaultDataStateFlow.value = DataState.NoNetwork()
                assertEquals(DataState.NoNetwork<FolderView>(), awaitItem())
            }
        }

    @Test
    fun `getVaultFolderStateFlow should update to Error when an error is emitted`() =
        runTest {
            val folderId = 1234
            val folderIdString = "mockId-$folderId"
            val throwable = Throwable("Fail")
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.getVaultFolderStateFlow(folderIdString).test {
                assertEquals(DataState.Loading, awaitItem())
                mutableVaultDataStateFlow.value = DataState.Error(throwable)
                assertEquals(DataState.Error<FolderView>(throwable), awaitItem())
            }
        }

    @Test
    fun `getSendStateFlow should update emit SendView when present`() = runTest {
        val sendId = 1
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val sendView = createMockSendView(number = sendId)

        vaultRepository.getSendStateFlow("mockId-$sendId").test {
            assertEquals(DataState.Loading, awaitItem())
            mutableSendDataStateFlow.value = DataState.Loaded(SendData(emptyList()))
            assertEquals(DataState.Loaded<SendView?>(null), awaitItem())
            mutableSendDataStateFlow.value = DataState.Loaded(SendData(listOf(sendView)))
            assertEquals(DataState.Loaded<SendView?>(sendView), awaitItem())
        }
    }

    @Test
    fun `getSendStateFlow should update to NoNetwork when NoNetwork value is emitted`() =
        runTest {
            val sendId = 1234
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.getSendStateFlow("mockId-$sendId").test {
                assertEquals(DataState.Loading, awaitItem())
                mutableSendDataStateFlow.value = DataState.NoNetwork()
                assertEquals(DataState.NoNetwork<SendView?>(), awaitItem())
            }
        }

    @Test
    fun `getSendStateFlow should update to Error when an error is emitted`() =
        runTest {
            val sendId = 1234
            val throwable = Throwable("Fail")
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.getSendStateFlow("mockId-$sendId").test {
                assertEquals(DataState.Loading, awaitItem())
                mutableSendDataStateFlow.value = DataState.Error(throwable)
                assertEquals(DataState.Error<SendView?>(throwable), awaitItem())
            }
        }

    @Test
    fun `generateTotp with no active user should return GenerateTotpResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = vaultRepository.generateTotp(
                cipherId = "totpCode",
                time = DateTime.now(),
            )

            assertEquals(
                GenerateTotpResult.Error(error = NoActiveUserException()),
                result,
            )
        }

    @Test
    fun `generateTotp should return a success result on getting a code`() = runTest {
        val totpResponse = TotpResponse("Testcode", 30u)
        coEvery {
            vaultSdkSource.generateTotpForCipherListView(any(), any(), any())
        } returns totpResponse.asSuccess()
        mutableDecryptCipherListResultStateFlow.value = DataState.Loaded(
            DecryptCipherListResult(
                successes = listOf(
                    createMockCipherListView(number = 1),
                ),
                failures = emptyList(),
            ),
        )
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        val result = vaultRepository.generateTotp(
            cipherId = "mockId-1",
            time = DateTime.now(),
        )

        assertEquals(
            GenerateTotpResult.Success(
                code = totpResponse.code,
                periodSeconds = totpResponse.period.toInt(),
            ),
            result,
        )
    }

    @Test
    fun `getAuthCodeFlow with no active user should emit an error`() = runTest {
        fakeAuthDiskSource.userState = null
        assertTrue(vaultRepository.getAuthCodeFlow(cipherId = "cipherId").value is DataState.Error)
    }

    @Test
    fun `getAuthCodeFlow for a single cipher should update data state when state changes`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val stateFlow = MutableStateFlow<DataState<VerificationCodeItem?>>(DataState.Loading)

            every {
                totpCodeManager.getTotpCodeStateFlow(userId = userId, cipherListView = any())
            } returns stateFlow
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = DecryptCipherListResult(
                        successes = listOf(createMockCipherListView(number = 1)),
                        failures = emptyList(),
                    ),
                    collectionViewList = emptyList(),
                    folderViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )

            vaultRepository.getAuthCodeFlow(userId).test {
                assertEquals(
                    DataState.Loading,
                    awaitItem(),
                )

                stateFlow.tryEmit(DataState.Loaded(createVerificationCodeItem()))

                assertEquals(
                    DataState.Loaded(createVerificationCodeItem()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `getAuthCodesFlow with no active user should emit an error`() = runTest {
        fakeAuthDiskSource.userState = null
        assertTrue(vaultRepository.getAuthCodesFlow().value is DataState.Error)
    }

    @Test
    fun `getAuthCodesFlow should update data state when state changes`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val userId = "mockId-1"
        val stateFlow = MutableStateFlow<DataState<List<VerificationCodeItem>>>(DataState.Loading)

        every {
            totpCodeManager.getTotpCodesForCipherListViewsStateFlow(
                userId = userId,
                cipherListViews = any(),
            )
        } returns stateFlow
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                decryptCipherListResult = DecryptCipherListResult(
                    successes = listOf(createMockCipherListView(number = 1)),
                    failures = emptyList(),
                ),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )

        vaultRepository.getAuthCodesFlow().test {
            assertEquals(
                DataState.Loading,
                awaitItem(),
            )

            stateFlow.tryEmit(DataState.Loaded(listOf(createVerificationCodeItem())))

            assertEquals(
                DataState.Loaded(listOf(createVerificationCodeItem())),
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `exportVaultDataToString should return a success result when data is successfully converted for export`() =
        runTest {
            val format = ExportFormat.Json
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"

            val userCipher = createMockCipher(1).copy(
                collectionIds = null,
                deletedDate = null,
            )
            val deletedCipher = createMockCipher(2).copy(collectionIds = null)
            val orgCipher = createMockCipher(3).copy(deletedDate = null)

            coEvery {
                vaultDiskSource.getCiphersFlow(userId)
            } returns flowOf(listOf(userCipher, deletedCipher, orgCipher))

            coEvery {
                vaultDiskSource.getFolders(userId)
            } returns flowOf(listOf(createMockFolder(1)))

            coEvery {
                vaultSdkSource.exportVaultDataToString(userId, any(), any(), format)
            } returns "TestResult".asSuccess()

            val expected = ExportVaultDataResult.Success(vaultData = "TestResult")
            val result = vaultRepository.exportVaultDataToString(
                format = format,
                restrictedTypes = emptyList(),
            )

            coVerify {
                vaultSdkSource.exportVaultDataToString(
                    userId = userId,
                    ciphers = listOf(userCipher.toEncryptedSdkCipher()),
                    folders = listOf(createMockSdkFolder(1)),
                    format = ExportFormat.Json,
                )
            }

            assertEquals(
                expected,
                result,
            )
        }

    @Test
    fun `exportVaultDataToString with restrictedTypes should filter out restricted cipher types`() =
        runTest {
            val format = ExportFormat.Json
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"

            val userCipher = createMockCipher(1).copy(
                type = CipherTypeJson.LOGIN,
                collectionIds = null,
                deletedDate = null,
            )
            val userCipherCard = createMockCipher(2).copy(
                type = CipherTypeJson.CARD,
                collectionIds = null,
                deletedDate = null,
            )
            val deletedCipher = createMockCipher(2).copy(collectionIds = null)
            val orgCipher = createMockCipher(3).copy(deletedDate = null)

            coEvery {
                vaultDiskSource.getCiphersFlow(userId)
            } returns flowOf(listOf(userCipher, userCipherCard, deletedCipher, orgCipher))

            coEvery {
                vaultDiskSource.getFolders(userId)
            } returns flowOf(listOf(createMockFolder(1)))

            coEvery {
                vaultSdkSource.exportVaultDataToString(userId, any(), any(), format)
            } returns "TestResult".asSuccess()

            val expected = ExportVaultDataResult.Success(vaultData = "TestResult")
            val result = vaultRepository.exportVaultDataToString(
                format = format,
                restrictedTypes = listOf(CipherType.CARD),
            )

            coVerify {
                vaultSdkSource.exportVaultDataToString(
                    userId = userId,
                    ciphers = listOf(userCipher.toEncryptedSdkCipher()),
                    folders = listOf(createMockSdkFolder(1)),
                    format = ExportFormat.Json,
                )
            }

            assertEquals(
                expected,
                result,
            )
        }

    @Test
    fun `exportVaultDataToString should return a failure result when the data conversion fails`() =
        runTest {
            val format = ExportFormat.Json
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"

            coEvery {
                vaultDiskSource.getCiphersFlow(userId)
            } returns flowOf(listOf(createMockCipher(1)))

            coEvery {
                vaultDiskSource.getFolders(userId)
            } returns flowOf(listOf(createMockFolder(1)))
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.exportVaultDataToString(userId, any(), any(), format)
            } returns error.asFailure()

            val expected = ExportVaultDataResult.Error(error = error)
            val result = vaultRepository.exportVaultDataToString(
                format = format,
                restrictedTypes = emptyList(),
            )

            assertEquals(
                expected,
                result,
            )
        }

    @Test
    fun `importCxfPayload should return success result when payload is successfully imported`() =
        runTest {
            val userId = "mockId-1"
            val payload = "payload"
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            coEvery {
                credentialExchangeImportManager.importCxfPayload(
                    userId = userId,
                    payload = payload,
                )
            } returns ImportCxfPayloadResult.Success(itemCount = 1)
            val result = vaultRepository.importCxfPayload(payload)

            assertEquals(
                ImportCredentialsResult.Success(itemCount = 1),
                result,
            )
        }

    @Test
    fun `importCxfPayload should return error result when activeUserId is null`() = runTest {
        val result = vaultRepository.importCxfPayload("")
        assertTrue(
            (result as? ImportCredentialsResult.Error)?.error is NoActiveUserException,
        )
    }

    @Test
    fun `importCxfPayload should return error result when payload import fails`() = runTest {
        val userId = "mockId-1"
        val payload = "payload"
        val expected = Throwable()
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        coEvery {
            credentialExchangeImportManager.importCxfPayload(
                userId = userId,
                payload = payload,
            )
        } returns ImportCxfPayloadResult.Error(expected)

        val result = vaultRepository.importCxfPayload(payload)

        assertEquals(
            ImportCredentialsResult.Error(expected),
            result,
        )
    }

    @Test
    fun `importCxfPayload should return NoItems when payload contains no credentials`() = runTest {
        val userId = "mockId-1"
        val payload = "payload"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        coEvery {
            credentialExchangeImportManager.importCxfPayload(
                userId = userId,
                payload = payload,
            )
        } returns ImportCxfPayloadResult.NoItems

        val result = vaultRepository.importCxfPayload(payload)

        assertEquals(
            ImportCredentialsResult.NoItems,
            result,
        )
    }

    @Test
    fun `importCxfPayload should return SyncFailed when sync fails`() = runTest {
        val userId = "mockId-1"
        val payload = "payload"
        val throwable = Throwable()
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        coEvery {
            credentialExchangeImportManager.importCxfPayload(
                userId = userId,
                payload = payload,
            )
        } returns ImportCxfPayloadResult.SyncFailed(throwable)

        val result = vaultRepository.importCxfPayload(payload)

        assertEquals(
            ImportCredentialsResult.SyncFailed(throwable),
            result,
        )
    }

    @Test
    fun `exportVaultDataToCxf should return success result`() = runTest {
        val userId = "mockId-1"
        val account = createMockAccount(number = 1, email = "email", name = null)
        val cipherListViews = listOf(createMockCipherListView(number = 1))

        fakeAuthDiskSource.userState = MOCK_USER_STATE

        coEvery {
            vaultSdkSource.exportVaultDataToCxf(
                userId = userId,
                account = account,
                ciphers = any(),
            )
        } returns "TestResult".asSuccess()
        coEvery {
            vaultDiskSource.getSelectedCiphers(
                userId = userId,
                cipherIds = cipherListViews.mapNotNull { it.id },
            )
        } returns listOf(createMockCipher(number = 1))

        val result = vaultRepository.exportVaultDataToCxf(ciphers = cipherListViews)

        assertEquals("TestResult".asSuccess(), result)
    }

    @Test
    fun `exportVaultDataToCxf should return error result when exportVaultDataToCxf fails`() =
        runTest {
            val userId = "mockId-1"
            val account = createMockAccount(number = 1, email = "email", name = null)
            val cipherListViews = listOf(createMockCipherListView(number = 1))
            val throwable = Throwable()
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            coEvery {
                vaultSdkSource.exportVaultDataToCxf(
                    userId = userId,
                    account = account,
                    ciphers = any(),
                )
            } returns throwable.asFailure()

            coEvery {
                vaultDiskSource.getSelectedCiphers(
                    userId = userId,
                    cipherIds = cipherListViews.mapNotNull { it.id },
                )
            } returns listOf(createMockCipher(number = 1))

            val result = vaultRepository.exportVaultDataToCxf(ciphers = cipherListViews)

            assertEquals(throwable.asFailure(), result)
        }

    @Test
    fun `silentlyDiscoverCredentials should return result`() = runTest {
        val userId = "userId"
        val fido2CredentialStore: Fido2CredentialStore = mockk()
        val relyingPartyId = "relyingPartyId"
        val userHandle = "mockUserHandle"
        val expected: List<Fido2CredentialAutofillView> = mockk()
        coEvery {
            vaultSdkSource.silentlyDiscoverCredentials(
                userId = userId,
                fido2CredentialStore = fido2CredentialStore,
                relyingPartyId = relyingPartyId,
                userHandle = userHandle,
            )
        } returns expected.asSuccess()

        val result = vaultRepository.silentlyDiscoverCredentials(
            userId = userId,
            fido2CredentialStore = fido2CredentialStore,
            relyingPartyId = relyingPartyId,
            userHandle = userHandle,
        )

        assertEquals(expected.asSuccess(), result)

        coVerify(exactly = 1) {
            vaultSdkSource.silentlyDiscoverCredentials(
                userId = userId,
                fido2CredentialStore = fido2CredentialStore,
                relyingPartyId = relyingPartyId,
                userHandle = userHandle,
            )
        }
    }

    @Test
    fun `hasPersonalVaultItems returns false when vault data is loading`() {
        mutableVaultDataStateFlow.value = DataState.Loading

        val result = vaultRepository.hasPersonalVaultItems()

        assertEquals(false, result)
    }

    @Test
    fun `hasPersonalVaultItems returns false when all items belong to organizations`() {
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                decryptCipherListResult = DecryptCipherListResult(
                    successes = listOf(
                        createMockCipherListView(number = 1, organizationId = "org-1"),
                        createMockCipherListView(number = 2, organizationId = "org-2"),
                    ),
                    failures = emptyList(),
                ),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )

        val result = vaultRepository.hasPersonalVaultItems()

        assertEquals(false, result)
    }

    @Test
    fun `hasPersonalVaultItems returns true when there are items without organization ID`() {
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                decryptCipherListResult = DecryptCipherListResult(
                    successes = listOf(
                        createMockCipherListView(number = 1, organizationId = null),
                        createMockCipherListView(number = 2, organizationId = "org-2"),
                    ),
                    failures = emptyList(),
                ),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )

        val result = vaultRepository.hasPersonalVaultItems()

        assertEquals(true, result)
    }

    @Test
    fun `hasPersonalVaultItems returns true when there are items with empty organization ID`() {
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                decryptCipherListResult = DecryptCipherListResult(
                    successes = listOf(
                        createMockCipherListView(number = 1, organizationId = ""),
                        createMockCipherListView(number = 2, organizationId = "org-2"),
                    ),
                    failures = emptyList(),
                ),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )

        val result = vaultRepository.hasPersonalVaultItems()

        assertEquals(true, result)
    }

    @Test
    fun `hasPersonalVaultItems returns false when successes list is empty`() {
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                decryptCipherListResult = DecryptCipherListResult(
                    successes = emptyList(),
                    failures = emptyList(),
                ),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )

        val result = vaultRepository.hasPersonalVaultItems()

        assertEquals(false, result)
    }

    //region Helper functions

    /**
     * Prepares for an unlock call with the given [unlockResult].
     */
    private fun prepareStateForUnlocking(
        unlockResult: VaultUnlockResult,
        mockMasterPassword: String = "mockPassword-1",
        mockPin: String = "1234",
    ) {
        val userId = "mockId-1"
        coEvery {
            vaultSyncManager.syncForResult(forced = any())
        } returns SyncVaultDataResult.Success(itemsAvailable = true)
        coEvery {
            vaultSdkSource.decryptSendList(
                userId = userId,
                sendList = listOf(createMockSdkSend(number = 1)),
            )
        } returns listOf(createMockSendView(number = 1)).asSuccess()
        fakeAuthDiskSource.storePrivateKey(
            userId = userId,
            privateKey = "mockPrivateKey-1",
        )
        fakeAuthDiskSource.storeUserKey(
            userId = userId,
            userKey = "mockKey-1",
        )
        fakeAuthDiskSource.storePinProtectedUserKey(
            userId = userId,
            pinProtectedUserKey = "mockKey-1",
        )
        fakeAuthDiskSource.storePinProtectedUserKeyEnvelope(
            userId = userId,
            pinProtectedUserKeyEnvelope = "mockKey-1",
        )
        fakeAuthDiskSource.storeOrganizationKeys(
            userId = userId,
            organizationKeys = createMockOrganizationKeys(number = 1),
        )
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Master password unlock
        coEvery {
            vaultLockManager.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = "mockPrivateKey-1",
                    securityState = null,
                    signedPublicKey = null,
                    signingKey = null,
                ),
                userId = userId,
                email = "email",
                kdf = MOCK_PROFILE.toSdkParams(),
                initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                    password = mockMasterPassword,
                    masterPasswordUnlock = MasterPasswordUnlockData(
                        kdf = MOCK_PROFILE.toSdkParams(),
                        masterKeyWrappedUserKey = "mockKey-1",
                        salt = "mockSalt-1",
                    ),
                ),
                organizationKeys = createMockOrganizationKeys(number = 1),
            )
        } returns unlockResult

        // PIN unlock
        coEvery {
            vaultLockManager.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = "mockPrivateKey-1",
                    securityState = null,
                    signedPublicKey = null,
                    signingKey = null,
                ),
                userId = userId,
                email = "email",
                kdf = MOCK_PROFILE.toSdkParams(),
                initUserCryptoMethod = InitUserCryptoMethod.Pin(
                    pin = mockPin,
                    pinProtectedUserKey = "mockKey-1",
                ),
                organizationKeys = createMockOrganizationKeys(number = 1),
            )
        } returns unlockResult

        // PIN ENVELOPE unlock
        coEvery {
            vaultLockManager.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = "mockPrivateKey-1",
                    securityState = null,
                    signedPublicKey = null,
                    signingKey = null,
                ),
                userId = userId,
                email = "email",
                kdf = MOCK_PROFILE.toSdkParams(),
                initUserCryptoMethod = InitUserCryptoMethod.PinEnvelope(
                    pin = mockPin,
                    pinProtectedUserKeyEnvelope = "mockKey-1",
                ),
                organizationKeys = createMockOrganizationKeys(number = 1),
            )
        } returns unlockResult
    }
    //endregion Helper functions
}

private val MOCK_BASE_PROFILE = AccountJson.Profile(
    userId = "mockId-1",
    email = "email",
    isEmailVerified = true,
    name = null,
    stamp = "mockSecurityStamp-1",
    organizationId = null,
    avatarColorHex = null,
    hasPremium = false,
    forcePasswordResetReason = null,
    kdfType = null,
    kdfIterations = null,
    kdfMemory = null,
    kdfParallelism = null,
    userDecryptionOptions = null,
    isTwoFactorEnabled = false,
    creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
)

private val MOCK_PROFILE = MOCK_BASE_PROFILE.copy(
    userDecryptionOptions = UserDecryptionOptionsJson(
        hasMasterPassword = true,
        trustedDeviceUserDecryptionOptions = null,
        keyConnectorUserDecryptionOptions = null,
        masterPasswordUnlock = MasterPasswordUnlockDataJson(
            kdf = MOCK_BASE_PROFILE.toSdkParams().toKdfRequestModel(),
            masterKeyWrappedUserKey = "mockKey-1",
            salt = "mockSalt-1",
        ),
    ),
)

private val MOCK_ACCOUNT = AccountJson(
    profile = MOCK_PROFILE,
    tokens = AccountTokensJson(
        accessToken = "accessToken",
        refreshToken = "refreshToken",
    ),
    settings = AccountJson.Settings(
        environmentUrlData = null,
    ),
)

private val MOCK_USER_STATE = UserStateJson(
    activeUserId = "mockId-1",
    accounts = mapOf(
        "mockId-1" to MOCK_ACCOUNT,
    ),
)

private val MOCK_MASTER_PASSWORD_UNLOCK_DATA = MasterPasswordUnlockDataJson(
    salt = "mockSalt",
    kdf = MOCK_ACCOUNT.profile.toSdkParams().toKdfRequestModel(),
    masterKeyWrappedUserKey = "masterKeyWrappedUserKeyMock",
)
