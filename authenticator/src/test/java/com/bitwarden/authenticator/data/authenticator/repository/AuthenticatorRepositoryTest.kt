package com.bitwarden.authenticator.data.authenticator.repository

import app.cash.turbine.test
import com.bitwarden.authenticator.data.authenticator.datasource.disk.util.FakeAuthenticatorDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.entity.createMockAuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.manager.FileManager
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.data.authenticator.repository.util.toAuthenticatorItems
import com.bitwarden.authenticator.data.platform.base.FakeDispatcherManager
import com.bitwarden.authenticator.data.platform.manager.FeatureFlagManager
import com.bitwarden.authenticator.data.platform.manager.imports.ImportManager
import com.bitwarden.authenticator.data.platform.manager.model.FlagKey
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.data.platform.repository.model.DataState
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import com.bitwarden.authenticatorbridge.manager.model.AccountSyncState
import com.bitwarden.authenticatorbridge.model.SharedAccountData
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthenticatorRepositoryTest {

    private val mutableAccountSyncStateFlow =
        MutableStateFlow<AccountSyncState>(AccountSyncState.Loading)
    private val fakeAuthenticatorDiskSource = FakeAuthenticatorDiskSource()
    private val mockAuthenticatorBridgeManager: AuthenticatorBridgeManager = mockk {
        every { accountSyncStateFlow } returns mutableAccountSyncStateFlow
    }
    private val mockTotpCodeManager = mockk<TotpCodeManager>()
    private val mockFileManager = mockk<FileManager>()
    private val mockImportManager = mockk<ImportManager>()
    private val mockDispatcherManager = FakeDispatcherManager()
    private val mockFeatureFlagManager = mockk<FeatureFlagManager> {
        every { getFeatureFlag(FlagKey.PasswordManagerSync) } returns true
    }
    private val settingsRepository: SettingsRepository = mockk {
        every { previouslySyncedBitwardenAccountIds } returns emptySet()
    }

    private val authenticatorRepository = AuthenticatorRepositoryImpl(
        authenticatorDiskSource = fakeAuthenticatorDiskSource,
        authenticatorBridgeManager = mockAuthenticatorBridgeManager,
        featureFlagManager = mockFeatureFlagManager,
        totpCodeManager = mockTotpCodeManager,
        fileManager = mockFileManager,
        importManager = mockImportManager,
        dispatcherManager = mockDispatcherManager,
        settingRepository = settingsRepository,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(List<SharedAccountData.Account>::toAuthenticatorItems)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(List<SharedAccountData.Account>::toAuthenticatorItems)
    }

    @Test
    fun `ciphersStateFlow initial state should be loading`() = runTest {
        authenticatorRepository.ciphersStateFlow.test {
            assertEquals(
                DataState.Loading,
                awaitItem(),
            )
        }
    }

    @Test
    fun `sharedCodesStateFlow value should be FeatureNotEnabled when feature flag is off`() {
        every {
            mockFeatureFlagManager.getFeatureFlag(FlagKey.PasswordManagerSync)
        } returns false
        val repository = AuthenticatorRepositoryImpl(
            authenticatorDiskSource = fakeAuthenticatorDiskSource,
            authenticatorBridgeManager = mockAuthenticatorBridgeManager,
            featureFlagManager = mockFeatureFlagManager,
            totpCodeManager = mockTotpCodeManager,
            fileManager = mockFileManager,
            importManager = mockImportManager,
            dispatcherManager = mockDispatcherManager,
            settingRepository = settingsRepository,
        )
        assertEquals(
            SharedVerificationCodesState.FeatureNotEnabled,
            repository.sharedCodesStateFlow.value,
        )
    }

    @Test
    fun `ciphersStateFlow should emit sorted authenticator items when disk source changes`() =
        runTest {
            val mockItem = createMockAuthenticatorItemEntity(1)
            fakeAuthenticatorDiskSource.saveItem(mockItem)
            assertEquals(
                DataState.Loaded(listOf(mockItem)),
                authenticatorRepository.ciphersStateFlow.value,
            )
        }

    @Test
    fun `sharedCodesStateFlow should emit FeatureNotEnabled when feature flag is off`() = runTest {
        every {
            mockFeatureFlagManager.getFeatureFlag(FlagKey.PasswordManagerSync)
        } returns false
        val repository = AuthenticatorRepositoryImpl(
            authenticatorDiskSource = fakeAuthenticatorDiskSource,
            authenticatorBridgeManager = mockAuthenticatorBridgeManager,
            featureFlagManager = mockFeatureFlagManager,
            totpCodeManager = mockTotpCodeManager,
            fileManager = mockFileManager,
            importManager = mockImportManager,
            dispatcherManager = mockDispatcherManager,
            settingRepository = settingsRepository,
        )
        repository.sharedCodesStateFlow.test {
            assertEquals(
                SharedVerificationCodesState.FeatureNotEnabled,
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `sharedCodesStateFlow should emit AppNotInstalled when authenticatorBridgeManager emits AppNotInstalled`() =
        runTest {
            mutableAccountSyncStateFlow.value = AccountSyncState.AppNotInstalled
            authenticatorRepository.sharedCodesStateFlow.test {
                assertEquals(SharedVerificationCodesState.AppNotInstalled, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `sharedCodesStateFlow should emit SyncNotEnabled when authenticatorBridgeManager emits SyncNotEnabled`() =
        runTest {
            mutableAccountSyncStateFlow.value = AccountSyncState.SyncNotEnabled
            authenticatorRepository.sharedCodesStateFlow.test {
                assertEquals(SharedVerificationCodesState.SyncNotEnabled, awaitItem())
            }
        }

    @Test
    fun `sharedCodesStateFlow should emit Error when authenticatorBridgeManager emits Error`() =
        runTest {
            mutableAccountSyncStateFlow.value = AccountSyncState.Error
            authenticatorRepository.sharedCodesStateFlow.test {
                assertEquals(SharedVerificationCodesState.Error, awaitItem())
            }
        }

    @Test
    fun `sharedCodesStateFlow should emit Loading when authenticatorBridgeManager emits Loading`() =
        runTest {
            mutableAccountSyncStateFlow.value = AccountSyncState.Loading
            authenticatorRepository.sharedCodesStateFlow.test {
                assertEquals(SharedVerificationCodesState.Loading, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `sharedCodesStateFlow should emit OsVersionNotSupported when authenticatorBridgeManager emits OsVersionNotSupported`() =
        runTest {
            mutableAccountSyncStateFlow.value = AccountSyncState.OsVersionNotSupported
            authenticatorRepository.sharedCodesStateFlow.test {
                assertEquals(SharedVerificationCodesState.OsVersionNotSupported, awaitItem())
            }
        }

    @Test
    fun `sharedCodesStateFlow should emit Success when authenticatorBridgeManager emits Success`() =
        runTest {
            val sharedAccounts = emptyList<SharedAccountData.Account>()
            val authenticatorItems = mockk<List<AuthenticatorItem>>()
            val verificationCodes = mockk<List<VerificationCodeItem>>()
            every { sharedAccounts.toAuthenticatorItems() } returns authenticatorItems
            every {
                mockTotpCodeManager.getTotpCodesFlow(authenticatorItems)
            } returns flowOf(verificationCodes)
            authenticatorRepository.sharedCodesStateFlow.test {
                assertEquals(SharedVerificationCodesState.Loading, awaitItem())
                mutableAccountSyncStateFlow.value = AccountSyncState.Success(sharedAccounts)
                assertEquals(SharedVerificationCodesState.Success(verificationCodes), awaitItem())
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `firstTimeAccountSyncFlow should emit the first time an account syncs and update SettingsRepository`() =
        runTest {
            every { settingsRepository.previouslySyncedBitwardenAccountIds = setOf("1") } just runs
            val sharedAccounts = listOf(
                SharedAccountData.Account(
                    userId = "1",
                    name = null,
                    email = "test@test.com",
                    environmentLabel = "bitwarden.com",
                    totpUris = emptyList(),
                ),
            )
            authenticatorRepository.firstTimeAccountSyncFlow.test {
                mutableAccountSyncStateFlow.value = AccountSyncState.Success(sharedAccounts)
                awaitItem()
            }
            verify { settingsRepository.previouslySyncedBitwardenAccountIds = setOf("1") }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `firstTimeAccountSyncFlow should not emit if a synced account is already in previouslySyncedBitwardenAccountIds`() =
        runTest {
            every { settingsRepository.previouslySyncedBitwardenAccountIds } returns setOf("1")
            val sharedAccounts = listOf(
                SharedAccountData.Account(
                    userId = "1",
                    name = null,
                    email = "test@test.com",
                    environmentLabel = "bitwarden.com",
                    totpUris = emptyList(),
                ),
            )
            authenticatorRepository.firstTimeAccountSyncFlow.test {
                mutableAccountSyncStateFlow.value = AccountSyncState.Success(sharedAccounts)
                expectNoEvents()
            }
        }
}
