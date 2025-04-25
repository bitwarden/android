package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import android.net.Uri
import androidx.core.os.bundleOf
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.data.datasource.disk.base.FakeDispatcherManager
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.data.repository.util.baseWebSendUrl
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePinResult
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilitySelectionManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilitySelectionManagerImpl
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2OriginManager
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.data.autofill.fido2.model.UserVerificationRequirement
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2CreateCredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2GetCredentialsRequest
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManagerImpl
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFido2CredentialList
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DecryptFido2CredentialAutofillViewResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.AssertFido2CredentialResult
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.GetFido2CredentialsResult
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.RegisterFido2CredentialResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.vault.components.model.CreateVaultItemType
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.createMockPasskeyAssertionOptions
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.createMockPasskeyAttestationOptions
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.createMockDisplayItemForCipher
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toActiveAccountSummary
import com.x8bit.bitwarden.ui.vault.model.TotpData
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Suppress("LargeClass")
class VaultItemListingViewModelTest : BaseViewModelTest() {

    private val accessibilitySelectionManager: AccessibilitySelectionManager =
        AccessibilitySelectionManagerImpl()
    private val autofillSelectionManager: AutofillSelectionManager = AutofillSelectionManagerImpl()

    private var mockFilteredCiphers: List<CipherView>? = null
    private val cipherMatchingManager: CipherMatchingManager = object : CipherMatchingManager {
        // Just do no-op filtering unless we have mock filtered data
        override suspend fun filterCiphersForMatches(
            ciphers: List<CipherView>,
            matchUri: String,
        ): List<CipherView> = mockFilteredCiphers ?: ciphers
    }

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    private val clipboardManager: BitwardenClipboardManager = mockk {
        every { setText(text = any<String>(), toastDescriptorOverride = any<Text>()) } just runs
    }

    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository = mockk<AuthRepository> {
        every { activeUserId } answers { mutableUserStateFlow.value?.activeUserId }
        every { userStateFlow } returns mutableUserStateFlow
        every { logout(reason = any()) } just runs
        every { logout(userId = any(), reason = any()) } just runs
        every { switchAccount(any()) } returns SwitchAccountResult.AccountSwitched
    }
    private val mutableVaultDataStateFlow =
        MutableStateFlow<DataState<VaultData>>(DataState.Loading)
    private val defaultError = Throwable("Fail")
    private val vaultRepository: VaultRepository = mockk {
        every { vaultFilterType } returns VaultFilterType.AllVaults
        every { vaultDataStateFlow } returns mutableVaultDataStateFlow
        every { lockVault(any(), any()) } just runs
        every { sync(forced = any()) } just runs
        coEvery {
            getDecryptedFido2CredentialAutofillViews(any())
        } returns DecryptFido2CredentialAutofillViewResult.Error(error = defaultError)
    }
    private val environmentRepository: EnvironmentRepository = mockk {
        every { environment } returns Environment.Us
        every { environmentStateFlow } returns mockk()
    }

    private val mutablePullToRefreshEnabledFlow = MutableStateFlow(false)
    private val mutableIsIconLoadingDisabledFlow = MutableStateFlow(false)
    private val settingsRepository: SettingsRepository = mockk {
        every { isIconLoadingDisabled } returns false
        every { isIconLoadingDisabledFlow } returns mutableIsIconLoadingDisabledFlow
        every { getPullToRefreshEnabledFlow() } returns mutablePullToRefreshEnabledFlow
        every { isUnlockWithPinEnabled } returns false
    }

    private val mockAuthRepository = mockk<AuthRepository>(relaxed = true)
    private val specialCircumstanceManager: SpecialCircumstanceManager =
        SpecialCircumstanceManagerImpl(
            authRepository = mockAuthRepository,
            dispatcherManager = FakeDispatcherManager(),
        )
    private val policyManager: PolicyManager = mockk {
        every { getActivePolicies(type = PolicyTypeJson.DISABLE_SEND) } returns emptyList()
        every { getActivePoliciesFlow(type = PolicyTypeJson.DISABLE_SEND) } returns emptyFlow()
    }
    private val fido2CredentialManager: Fido2CredentialManager = mockk {
        every { isUserVerified } returns false
        every { isUserVerified = any() } just runs
        every { authenticationAttempts } returns 0
        every { authenticationAttempts = any() } just runs
        every { hasAuthenticationAttemptsRemaining() } returns true
        every { getPasskeyAttestationOptionsOrNull(any()) } returns mockk(relaxed = true)
        every {
            getUserVerificationRequirement(any<ProviderGetCredentialRequest>())
        } returns UserVerificationRequirement.PREFERRED
    }
    private val fido2OriginManager: Fido2OriginManager = mockk {
        coEvery { validateOrigin(any()) } returns Fido2ValidateOriginResult.Success(null)
    }

    private val organizationEventManager = mockk<OrganizationEventManager> {
        every { trackEvent(event = any()) } just runs
    }

    private val networkConnectionManager: NetworkConnectionManager = mockk {
        every { isNetworkConnected } returns true
    }

    private val initialState = createVaultItemListingState()
    private val initialSavedStateHandle = createSavedStateHandleWithVaultItemListingType(
        vaultItemListingType = VaultItemListingType.Login,
    )
    private val mockProviderGetCredentialRequest =
        mockk<ProviderGetCredentialRequest>(relaxed = true) {
            every {
                credentialOptions
            } returns listOf(
                mockk<GetPublicKeyCredentialOption>(relaxed = true),
            )
        }
    private val mockBeginGetPublicKeyCredentialOption =
        mockk<BeginGetPublicKeyCredentialOption>(relaxed = true)
    private val mockBeginGetCredentialRequest = mockk<BeginGetCredentialRequest>(relaxed = true) {
        every {
            beginGetCredentialOptions
        } returns listOf(
            mockBeginGetPublicKeyCredentialOption,
        )
    }

    @BeforeEach
    fun setUp() {
        mockkObject(
            ProviderCreateCredentialRequest.Companion,
            ProviderGetCredentialRequest.Companion,
            BeginGetCredentialRequest.Companion,
        )
        every { ProviderCreateCredentialRequest.fromBundle(any()) } returns mockk(relaxed = true)
        every {
            ProviderGetCredentialRequest.fromBundle(any())
        } returns mockProviderGetCredentialRequest
        every {
            BeginGetCredentialRequest.fromBundle(any())
        } returns mockBeginGetCredentialRequest
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(
            ProviderCreateCredentialRequest.Companion,
            ProviderGetCredentialRequest.Companion,
            BeginGetCredentialRequest.Companion,
        )
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                initialState, awaitItem(),
            )
        }
    }

    @Test
    fun `initial dialog state should be correct when Fido2CreateCredentialRequest is present`() =
        runTest {
            val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
                userId = "mockUserId",
                isUserPreVerified = false,
                requestData = bundleOf(),
            )
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = fido2CreateCredentialRequest,
            )
            every {
                ProviderCreateCredentialRequest.fromBundle(any())
            } returns mockk(relaxed = true) {
                every {
                    callingRequest
                } returns mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
                every { callingAppInfo } returns mockk(relaxed = true)
            }
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns createMockPasskeyAttestationOptions(number = 1)
            coEvery {
                fido2OriginManager.validateOrigin(any())
            } returns Fido2ValidateOriginResult.Success(null)
            val viewModel = createVaultItemListingViewModel()

            viewModel.stateFlow.test {
                assertEquals(
                    initialState.copy(
                        fido2CreateCredentialRequest = fido2CreateCredentialRequest,
                        dialogState = VaultItemListingState.DialogState.Loading(
                            message = R.string.loading.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
    }

    @Test
    fun `on LockAccountClick should call lockVault for the given account`() {
        val accountUserId = "userId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns accountUserId
        }
        val viewModel = createVaultItemListingViewModel()

        viewModel.trySendAction(VaultItemListingsAction.LockAccountClick(accountSummary))

        verify { vaultRepository.lockVault(userId = accountUserId, isUserInitiated = true) }
    }

    @Test
    fun `on LogoutAccountClick should call logout for the given account`() {
        val accountUserId = "userId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns accountUserId
        }
        val viewModel = createVaultItemListingViewModel()

        viewModel.trySendAction(VaultItemListingsAction.LogoutAccountClick(accountSummary))

        verify(exactly = 1) {
            authRepository.logout(
                userId = accountUserId,
                reason = LogoutReason.Click(source = "VaultItemListingViewModel"),
            )
        }
    }

    @Test
    fun `on SwitchAccountClick should switch to the given account`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        val updatedUserId = "updatedUserId"
        viewModel.trySendAction(
            VaultItemListingsAction.SwitchAccountClick(
                accountSummary = mockk {
                    every { userId } returns updatedUserId
                },
            ),
        )
        verify { authRepository.switchAccount(userId = updatedUserId) }
    }

    @Test
    fun `BackClick with TotpData should emit ExitApp`() = runTest {
        val totpData = TotpData(
            uri = "otpauth://totp/issuer:accountName?secret=secret",
            issuer = "issuer",
            accountName = "accountName",
            secret = "secret",
            digits = 6,
            period = 30,
            algorithm = TotpData.CryptoHashAlgorithm.SHA_1,
        )
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.AddTotpLoginItem(data = totpData)
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.BackClick)
            assertEquals(VaultItemListingEvent.ExitApp, awaitItem())
        }
    }

    @Test
    fun `BackClick with AutofillSelectionData should emit ExitApp`() = runTest {
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.AutofillSelection(
            autofillSelectionData = AutofillSelectionData(
                framework = AutofillSelectionData.Framework.ACCESSIBILITY,
                type = AutofillSelectionData.Type.LOGIN,
                uri = null,
            ),
            shouldFinishWhenComplete = false,
        )
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.BackClick)
            assertEquals(VaultItemListingEvent.ExitApp, awaitItem())
        }
    }

    @Test
    fun `BackClick should emit NavigateBack`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.BackClick)
            assertEquals(VaultItemListingEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `DismissDialogClick should clear the dialog state`() {
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(VaultItemListingsAction.DismissDialogClick)
        assertEquals(initialState.copy(dialogState = null), viewModel.stateFlow.value)
    }

    @Test
    fun `SearchIconClick should emit NavigateToVaultSearchScreen`() = runTest {
        val searchType = SearchType.Vault.Logins
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.SearchIconClick)
            assertEquals(VaultItemListingEvent.NavigateToSearchScreen(searchType), awaitItem())
        }
    }

    @Test
    fun `SearchIconClick should emit NavigateToVaultSearchScreen with all search type`() = runTest {
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.AutofillSelection(
            autofillSelectionData = AutofillSelectionData(
                framework = AutofillSelectionData.Framework.ACCESSIBILITY,
                type = AutofillSelectionData.Type.LOGIN,
                uri = null,
            ),
            shouldFinishWhenComplete = false,
        )
        val searchType = SearchType.Vault.All
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.SearchIconClick)
            assertEquals(VaultItemListingEvent.NavigateToSearchScreen(searchType), awaitItem())
        }
    }

    @Test
    fun `LockClick should call lockVaultForCurrentUser`() {
        every { vaultRepository.lockVaultForCurrentUser(any()) } just runs
        val viewModel = createVaultItemListingViewModel()

        viewModel.trySendAction(VaultItemListingsAction.LockClick)

        verify(exactly = 1) {
            vaultRepository.lockVaultForCurrentUser(isUserInitiated = true)
        }
    }

    @Test
    fun `SyncClick should display the loading dialog and call sync`() {
        val viewModel = createVaultItemListingViewModel()

        viewModel.trySendAction(VaultItemListingsAction.SyncClick)

        assertEquals(
            initialState.copy(
                dialogState = VaultItemListingState.DialogState.Loading(
                    message = R.string.syncing.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            vaultRepository.sync(forced = true)
        }
    }

    @Test
    fun `on SyncClick should show the no network dialog if no connection is available`() {
        val viewModel = createVaultItemListingViewModel()
        every {
            networkConnectionManager.isNetworkConnected
        } returns false
        viewModel.trySendAction(VaultItemListingsAction.SyncClick)
        assertEquals(
            initialState.copy(
                dialogState = VaultItemListingState.DialogState.Error(
                    R.string.internet_connection_required_title.asText(),
                    R.string.internet_connection_required_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 0) {
            vaultRepository.sync(forced = true)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ItemClick for vault item when accessibility autofill should post to the accessibilitySelectionManager`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            coEvery {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView),
                )
            } returns DecryptFido2CredentialAutofillViewResult.Error(error = defaultError)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.AutofillSelection(
                autofillSelectionData = AutofillSelectionData(
                    type = AutofillSelectionData.Type.LOGIN,
                    framework = AutofillSelectionData.Framework.ACCESSIBILITY,
                    uri = "https://www.test.com",
                ),
                shouldFinishWhenComplete = true,
            )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel()

            accessibilitySelectionManager.accessibilitySelectionFlow.test {
                viewModel.trySendAction(
                    VaultItemListingsAction.ItemClick(
                        id = "mockId-1",
                        cipherType = CipherType.LOGIN,
                    ),
                )
                assertEquals(cipherView, awaitItem())
            }
            coVerify(exactly = 1) {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView),
                )
            }
        }

    @Test
    fun `ItemClick for vault item when autofill should post to the AutofillSelectionManager`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            coEvery {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView),
                )
            } returns DecryptFido2CredentialAutofillViewResult.Error(error = defaultError)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AutofillSelection(
                    autofillSelectionData = AutofillSelectionData(
                        type = AutofillSelectionData.Type.LOGIN,
                        framework = AutofillSelectionData.Framework.AUTOFILL,
                        uri = "https://www.test.com",
                    ),
                    shouldFinishWhenComplete = true,
                )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel()

            autofillSelectionManager.autofillSelectionFlow.test {
                viewModel.trySendAction(
                    VaultItemListingsAction.ItemClick(
                        id = "mockId-1",
                        cipherType = CipherType.LOGIN,
                    ),
                )
                assertEquals(
                    cipherView,
                    awaitItem(),
                )
            }
            coVerify {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ItemClick for vault item during FIDO 2 registration should show FIDO 2 error dialog when cipherView is null`() {
        val cipherView = createMockCipherView(number = 1)
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
            fido2CreateCredentialRequest = createMockFido2CreateCredentialRequest(number = 1),
        )
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(),
                folderViewList = emptyList(),
                collectionViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(
            VaultItemListingsAction.ItemClick(
                id = cipherView.id.orEmpty(),
                cipherType = CipherType.LOGIN,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_the_selected_item_does_not_exist
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ItemClick for vault item during FIDO 2 registration should show overwrite passkey confirmation when selected cipher has existing passkey`() {
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = createMockFido2CreateCredentialRequest(number = 1),
            )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            every {
                ProviderCreateCredentialRequest.fromBundle(any())
            } returns mockk(relaxed = true) {
                every {
                    callingRequest
                } returns mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
            }
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns createMockPasskeyAttestationOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.REQUIRED,
            )
            coEvery {
                fido2CredentialManager.registerFido2Credential(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns Fido2RegisterCredentialResult.Success("mockResponse")

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.ItemClick(
                    id = cipherView.id.orEmpty(),
                    cipherType = CipherType.LOGIN,
                ),
            )

            assertEquals(
                VaultItemListingState.DialogState.OverwritePasskeyConfirmationPrompt(
                    cipherViewId = cipherView.id!!,
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ItemClick for vault item during FIDO 2 registration should show loading dialog, then request user verification when required`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1, fido2Credentials = null)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = createMockFido2CreateCredentialRequest(number = 1),
            )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            every {
                ProviderCreateCredentialRequest.fromBundle(any())
            } returns mockk(relaxed = true) {
                every {
                    callingRequest
                } returns mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
            }
            every {
                fido2CredentialManager.getUserVerificationRequirement(
                    request = any<CreatePublicKeyCredentialRequest>(),
                )
            } returns UserVerificationRequirement.REQUIRED
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns createMockPasskeyAttestationOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.REQUIRED,
            )
            coEvery {
                fido2CredentialManager.registerFido2Credential(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns Fido2RegisterCredentialResult.Success("mockResponse")

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.ItemClick(
                    id = cipherView.id.orEmpty(),
                    cipherType = CipherType.LOGIN,
                ),
            )

            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingState.DialogState.Loading(R.string.saving.asText()),
                    viewModel.stateFlow.value.dialogState,
                )
                assertEquals(
                    VaultItemListingEvent.Fido2UserVerification(
                        isRequired = true,
                        selectedCipherView = cipherView,
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ItemClick for vault item during FIDO 2 registration should skip user verification and perform registration when discouraged`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1)
            val mockFido2CredentialRequest = createMockFido2CreateCredentialRequest(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = mockFido2CredentialRequest,
            )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            every {
                ProviderCreateCredentialRequest.fromBundle(any())
            } returns mockk(relaxed = true) {
                every {
                    callingRequest
                } returns mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
            }
            every {
                fido2CredentialManager.getUserVerificationRequirement(
                    request = any<CreatePublicKeyCredentialRequest>(),
                )
            } returns UserVerificationRequirement.DISCOURAGED
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns createMockPasskeyAttestationOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.DISCOURAGED,
            )
            coEvery {
                fido2CredentialManager.registerFido2Credential(
                    userId = any(),
                    callingAppInfo = any(),
                    createPublicKeyCredentialRequest = any(),
                    selectedCipherView = any(),
                )
            } returns Fido2RegisterCredentialResult.Success("mockResponse")

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.ItemClick(
                    id = cipherView.id.orEmpty(),
                    cipherType = CipherType.LOGIN,
                ),
            )

            coVerify {
                fido2CredentialManager.registerFido2Credential(
                    userId = DEFAULT_USER_STATE.activeUserId,
                    createPublicKeyCredentialRequest = any(),
                    selectedCipherView = cipherView,
                    callingAppInfo = any(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ItemClick for vault item during FIDO 2 registration should skip user verification when user is verified`() {
        setupMockUri()
        val cipherView = createMockCipherView(number = 1)
        val mockFido2CredentialRequest = createMockFido2CreateCredentialRequest(number = 1)
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
            fido2CreateCredentialRequest = mockFido2CredentialRequest,
        )
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(cipherView),
                folderViewList = emptyList(),
                collectionViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )
        every {
            ProviderCreateCredentialRequest.fromBundle(any())
        } returns mockk(relaxed = true) {
            every {
                callingRequest
            } returns mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
        }
        every { fido2CredentialManager.isUserVerified } returns true
        every {
            fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
        } returns createMockPasskeyAttestationOptions(
            number = 1,
            userVerificationRequirement = UserVerificationRequirement.REQUIRED,
        )
        coEvery {
            fido2CredentialManager.registerFido2Credential(
                userId = any(),
                callingAppInfo = any(),
                createPublicKeyCredentialRequest = any(),
                selectedCipherView = any(),
            )
        } returns Fido2RegisterCredentialResult.Success("mockResponse")

        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(
            VaultItemListingsAction.ItemClick(
                id = cipherView.id.orEmpty(),
                cipherType = CipherType.LOGIN,
            ),
        )

        coVerify { fido2CredentialManager.isUserVerified }
        coVerify(exactly = 1) {
            fido2CredentialManager.registerFido2Credential(
                userId = DEFAULT_USER_STATE.activeUserId,
                createPublicKeyCredentialRequest = any(),
                selectedCipherView = cipherView,
                callingAppInfo = any(),
            )
        }
    }

    @Test
    fun `ItemClick for vault item should emit NavigateToVaultItem`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.ItemClick(id = "mock", cipherType = CipherType.LOGIN),
            )
            assertEquals(
                VaultItemListingEvent.NavigateToVaultItem(
                    id = "mock",
                    type = VaultItemCipherType.LOGIN,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ItemClick for send item should emit NavigateToSendItem`() = runTest {
        val viewModel = createVaultItemListingViewModel(
            createSavedStateHandleWithVaultItemListingType(VaultItemListingType.SendFile),
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.ItemClick(id = "mock", cipherType = null),
            )
            assertEquals(VaultItemListingEvent.NavigateToSendItem(id = "mock"), awaitItem())
        }
    }

    @Test
    fun `MasterPasswordRepromptSubmit for a request Error should show a generic error dialog`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1)
            val cipherId = "mockId-1"
            val password = "password"
            val error = Throwable("Fail!")
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Error(error = error)
            val initialState = createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createMockDisplayItemForCipher(number = 1)
                            .copy(secondSubtitleTestTag = "PasskeySite"),
                    ),
                    displayFolderList = emptyList(),
                ),
            )
            assertEquals(
                initialState,
                viewModel.stateFlow.value,
            )

            viewModel.trySendAction(
                VaultItemListingsAction.MasterPasswordRepromptSubmit(
                    password = password,
                    masterPasswordRepromptData = MasterPasswordRepromptData.Autofill(
                        cipherId = cipherId,
                    ),
                ),
            )

            assertEquals(
                initialState.copy(
                    dialogState = VaultItemListingState.DialogState.Error(
                        title = null,
                        message = R.string.generic_error_message.asText(),
                        throwable = error,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordRepromptSubmit for a request Success with an invalid password should show an invalid password dialog`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1)
            val cipherId = "mockId-1"
            val password = "password"
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val initialState = createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createMockDisplayItemForCipher(number = 1)
                            .copy(secondSubtitleTestTag = "PasskeySite"),
                    ),
                    displayFolderList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = false)

            assertEquals(
                initialState,
                viewModel.stateFlow.value,
            )

            viewModel.trySendAction(
                VaultItemListingsAction.MasterPasswordRepromptSubmit(
                    password = password,
                    masterPasswordRepromptData = MasterPasswordRepromptData.Autofill(
                        cipherId = cipherId,
                    ),
                ),
            )

            assertEquals(
                initialState.copy(
                    dialogState = VaultItemListingState.DialogState.Error(
                        title = null,
                        message = R.string.invalid_master_password.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordRepromptSubmit for a request Success with a valid password for autofill should post to the AutofillSelectionManager`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1)
            val cipherId = "mockId-1"
            val password = "password"
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AutofillSelection(
                    autofillSelectionData = AutofillSelectionData(
                        type = AutofillSelectionData.Type.LOGIN,
                        framework = AutofillSelectionData.Framework.AUTOFILL,
                        uri = "https://www.test.com",
                    ),
                    shouldFinishWhenComplete = true,
                )
            val viewModel = createVaultItemListingViewModel()
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)

            autofillSelectionManager.autofillSelectionFlow.test {
                viewModel.trySendAction(
                    VaultItemListingsAction.MasterPasswordRepromptSubmit(
                        password = password,
                        masterPasswordRepromptData = MasterPasswordRepromptData.Autofill(
                            cipherId = cipherId,
                        ),
                    ),
                )
                assertEquals(
                    cipherView,
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordRepromptSubmit for a request Success with a valid password for accessibility autofill should post to the AccessibilitySelectionManager`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1)
            val cipherId = "mockId-1"
            val password = "password"
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AutofillSelection(
                    autofillSelectionData = AutofillSelectionData(
                        type = AutofillSelectionData.Type.LOGIN,
                        framework = AutofillSelectionData.Framework.ACCESSIBILITY,
                        uri = "https://www.test.com",
                    ),
                    shouldFinishWhenComplete = true,
                )
            val viewModel = createVaultItemListingViewModel()
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)

            accessibilitySelectionManager.accessibilitySelectionFlow.test {
                viewModel.trySendAction(
                    VaultItemListingsAction.MasterPasswordRepromptSubmit(
                        password = password,
                        masterPasswordRepromptData = MasterPasswordRepromptData.Autofill(
                            cipherId = cipherId,
                        ),
                    ),
                )
                assertEquals(cipherView, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordRepromptSubmit for a request Success with a valid password for overflow actions should process the action`() =
        runTest {
            val cipherId = "cipherId-1234"
            val password = "password"
            val viewModel = createVaultItemListingViewModel()
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)

            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    VaultItemListingsAction.MasterPasswordRepromptSubmit(
                        password = password,
                        masterPasswordRepromptData = MasterPasswordRepromptData.OverflowItem(
                            action = ListingItemOverflowAction.VaultAction.EditClick(
                                cipherId = cipherId,
                                cipherType = CipherType.LOGIN,
                                requiresPasswordReprompt = true,
                            ),
                        ),
                    ),
                )
                // An Edit action navigates to the Edit screen
                assertEquals(
                    VaultItemListingEvent.NavigateToEditCipher(
                        cipherId = cipherId,
                        cipherType = VaultItemCipherType.LOGIN,
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordRepromptSubmit with a valid password for totp flow should emit NavigateToEditCipher`() =
        runTest {
            val cipherId = "cipherId-1234"
            val password = "password"
            val viewModel = createVaultItemListingViewModel()
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)

            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    VaultItemListingsAction.MasterPasswordRepromptSubmit(
                        password = password,
                        masterPasswordRepromptData = MasterPasswordRepromptData.Totp(
                            cipherId = cipherId,
                        ),
                    ),
                )
                // An Edit action navigates to the Edit screen
                assertEquals(
                    VaultItemListingEvent.NavigateToEditCipher(
                        cipherId = cipherId,
                        cipherType = VaultItemCipherType.LOGIN,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `AddVaultItemClick inside a folder should show item selection dialog state`() {
        val viewModel = createVaultItemListingViewModel(
            savedStateHandle = createSavedStateHandleWithVaultItemListingType(
                vaultItemListingType = VaultItemListingType.Folder(folderId = "id"),
            ),
        )
        viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick)
        assertEquals(
            createVaultItemListingState(
                itemListingType = VaultItemListingState.ItemListingType.Vault.Folder(
                    folderId = "id",
                ),
                dialogState = VaultItemListingState.DialogState.VaultItemTypeSelection(
                    excludedOptions = persistentListOf(
                        CreateVaultItemType.SSH_KEY,
                        CreateVaultItemType.FOLDER,
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `AddVaultItemClick for vault item should emit NavigateToAddVaultItem`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick)
            assertEquals(
                VaultItemListingEvent.NavigateToAddVaultItem(VaultItemCipherType.LOGIN),
                awaitItem(),
            )
        }
    }

    @Test
    fun `AddVaultItemClick for send item should emit NavigateToAddVaultItem`() = runTest {
        val viewModel = createVaultItemListingViewModel(
            createSavedStateHandleWithVaultItemListingType(VaultItemListingType.SendText),
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick)
            assertEquals(VaultItemListingEvent.NavigateToAddSendItem, awaitItem())
        }
    }

    @Test
    fun `ItemTypeToAddSelected sends NavigateToAddFolder for folder selection`() = runTest {
        val viewModel = createVaultItemListingViewModel(
            savedStateHandle = createSavedStateHandleWithVaultItemListingType(
                vaultItemListingType = VaultItemListingType.Folder(""),
            ),
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.ItemTypeToAddSelected(
                    itemType = CreateVaultItemType.FOLDER,
                ),
            )
            assertEquals(
                VaultItemListingEvent.NavigateToAddFolder(
                    parentFolderName = "",
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ItemTypeToAddSelected sends NavigateToAddFolder for any other selection`() = runTest {
        val viewModel = createVaultItemListingViewModel(
            savedStateHandle = createSavedStateHandleWithVaultItemListingType(
                vaultItemListingType = VaultItemListingType.Folder("id"),
            ),
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.ItemTypeToAddSelected(
                    itemType = CreateVaultItemType.CARD,
                ),
            )
            assertEquals(
                VaultItemListingEvent.NavigateToAddVaultItem(
                    vaultItemCipherType = VaultItemCipherType.CARD,
                    selectedFolderId = "id",
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `FolderClick for vault item should emit NavigateToFolderItem`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        val testId = "1"

        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.FolderClick(testId))
            assertEquals(VaultItemListingEvent.NavigateToFolderItem(testId), awaitItem())
        }
    }

    @Test
    fun `CollectionClick for vault item should emit NavigateToCollectionItem`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        val testId = "1"

        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.CollectionClick(testId))
            assertEquals(VaultItemListingEvent.NavigateToCollectionItem(testId), awaitItem())
        }
    }

    @Test
    fun `RefreshClick should sync`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(VaultItemListingsAction.RefreshClick)
        verify { vaultRepository.sync(forced = true) }
    }

    @Test
    fun `OverflowOptionClick Send EditClick should emit NavigateToSendItem`() = runTest {
        val sendId = "sendId"
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.SendAction.EditClick(sendId = sendId),
                ),
            )
            assertEquals(VaultItemListingEvent.NavigateToSendItem(sendId), awaitItem())
        }
    }

    @Test
    fun `OverflowOptionClick Send CopyUrlClick should call setText on clipboardManager`() =
        runTest {
            val sendUrl = "www.test.com"
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.SendAction.CopyUrlClick(sendUrl = sendUrl),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = sendUrl,
                    toastDescriptorOverride = R.string.send_link.asText(),
                )
            }
        }

    @Test
    fun `OverflowOptionClick Send DeleteClick with deleteSend error should display error dialog`() =
        runTest {
            val sendId = "sendId1234"
            val error = Throwable("Oops")
            coEvery { vaultRepository.deleteSend(sendId) } returns DeleteSendResult.Error(
                error = error,
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.stateFlow.test {
                assertEquals(initialState, awaitItem())
                viewModel.trySendAction(
                    VaultItemListingsAction.OverflowOptionClick(
                        ListingItemOverflowAction.SendAction.DeleteClick(sendId = sendId),
                    ),
                )
                assertEquals(
                    initialState.copy(
                        dialogState = VaultItemListingState.DialogState.Loading(
                            message = R.string.deleting.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    initialState.copy(
                        dialogState = VaultItemListingState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                            throwable = error,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `OverflowOptionClick Send DeleteClick with deleteSend success should emit ShowToast`() =
        runTest {
            val sendId = "sendId1234"
            coEvery { vaultRepository.deleteSend(sendId) } returns DeleteSendResult.Success

            val viewModel = createVaultItemListingViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    VaultItemListingsAction.OverflowOptionClick(
                        ListingItemOverflowAction.SendAction.DeleteClick(sendId = sendId),
                    ),
                )
                assertEquals(
                    VaultItemListingEvent.ShowToast(R.string.send_deleted.asText()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `OverflowOptionClick Send ShareUrlClick should emit ShowShareSheet`() = runTest {
        val sendUrl = "www.test.com"
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.SendAction.ShareUrlClick(sendUrl = sendUrl),
                ),
            )
            assertEquals(VaultItemListingEvent.ShowShareSheet(sendUrl), awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Send RemovePasswordClick with removePasswordSend error should display error dialog`() =
        runTest {
            val sendId = "sendId1234"
            val error = Throwable("Oops")
            coEvery {
                vaultRepository.removePasswordSend(sendId)
            } returns RemovePasswordSendResult.Error(errorMessage = null, error = error)

            val viewModel = createVaultItemListingViewModel()
            viewModel.stateFlow.test {
                assertEquals(initialState, awaitItem())
                viewModel.trySendAction(
                    VaultItemListingsAction.OverflowOptionClick(
                        ListingItemOverflowAction.SendAction.RemovePasswordClick(sendId = sendId),
                    ),
                )
                assertEquals(
                    initialState.copy(
                        dialogState = VaultItemListingState.DialogState.Loading(
                            message = R.string.removing_send_password.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    initialState.copy(
                        dialogState = VaultItemListingState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                            throwable = error,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Send RemovePasswordClick with removePasswordSend success should emit ShowToast`() =
        runTest {
            val sendId = "sendId1234"
            coEvery {
                vaultRepository.removePasswordSend(sendId)
            } returns RemovePasswordSendResult.Success(mockk())

            val viewModel = createVaultItemListingViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    VaultItemListingsAction.OverflowOptionClick(
                        ListingItemOverflowAction.SendAction.RemovePasswordClick(sendId = sendId),
                    ),
                )
                assertEquals(
                    VaultItemListingEvent.ShowToast(R.string.send_password_removed.asText()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `OverflowOptionClick Vault CopyNoteClick should call setText on the ClipboardManager`() =
        runTest {
            val notes = "notes"
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyNoteClick(notes = notes),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = notes,
                    toastDescriptorOverride = R.string.notes.asText(),
                )
            }
        }

    @Test
    fun `OverflowOptionClick Vault CopyNumberClick should call setText on the ClipboardManager`() =
        runTest {
            val number = "12345-4321-9876-6789"
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyNumberClick(
                        number = number,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = number,
                    toastDescriptorOverride = R.string.number.asText(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyPasswordClick should call setText on the ClipboardManager`() =
        runTest {
            val password = "passTheWord"
            val cipherId = "cipherId"
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                        password = password,
                        requiresPasswordReprompt = true,
                        cipherId = cipherId,
                    ),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = password,
                    toastDescriptorOverride = R.string.password.asText(),
                )
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientCopiedPassword(cipherId = cipherId),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopySecurityCodeClick should call setText on the ClipboardManager`() =
        runTest {
            val securityCode = "234"
            val cipherId = "cipherId"
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopySecurityCodeClick(
                        securityCode = securityCode,
                        cipherId = cipherId,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = securityCode,
                    toastDescriptorOverride = R.string.security_code.asText(),
                )
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientCopiedCardCode(cipherId = cipherId),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyTotpClick with GenerateTotpCode success should call setText on the ClipboardManager`() =
        runTest {
            val totpCode = "totpCode"
            val code = "Code"

            coEvery {
                vaultRepository.generateTotp(totpCode, clock.instant())
            } returns GenerateTotpResult.Success(code, 30)

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyTotpClick(totpCode),
                ),
            )

            verify(exactly = 1) {
                clipboardManager.setText(
                    text = code,
                    toastDescriptorOverride = R.string.totp.asText(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyTotpClick with GenerateTotpCode failure should not call setText on the ClipboardManager`() =
        runTest {
            val totpCode = "totpCode"

            coEvery {
                vaultRepository.generateTotp(totpCode, clock.instant())
            } returns GenerateTotpResult.Error(error = defaultError)

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyTotpClick(totpCode),
                ),
            )

            verify(exactly = 0) {
                clipboardManager.setText(
                    text = any<String>(),
                    toastDescriptorOverride = any<Text>(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyUsernameClick should call setText on the ClipboardManager`() =
        runTest {
            val username = "bitwarden"
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyUsernameClick(
                        username = username,
                    ),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = username,
                    toastDescriptorOverride = R.string.username.asText(),
                )
            }
        }

    @Test
    fun `OverflowOptionClick Vault EditClick should emit NavigateToEditCipher`() = runTest {
        val cipherId = "cipherId-1234"
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = cipherId,
                        cipherType = CipherType.LOGIN,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            assertEquals(
                VaultItemListingEvent.NavigateToEditCipher(
                    cipherId = cipherId,
                    cipherType = VaultItemCipherType.LOGIN,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `OverflowOptionClick Vault LaunchClick should emit NavigateToUrl`() = runTest {
        val url = "www.test.com"
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.LaunchClick(url = url),
                ),
            )
            assertEquals(VaultItemListingEvent.NavigateToUrl(url), awaitItem())
        }
    }

    @Test
    fun `OverflowOptionClick Vault ViewClick should emit NavigateToUrl`() = runTest {
        val cipherId = "cipherId-9876"
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.ViewClick(
                        cipherId = cipherId,
                        cipherType = CipherType.LOGIN,
                    ),
                ),
            )
            assertEquals(
                VaultItemListingEvent.NavigateToVaultItem(
                    id = cipherId,
                    type = VaultItemCipherType.LOGIN,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `vaultDataStateFlow Loaded with items should update ViewState to Content`() = runTest {
        setupMockUri()

        val dataState = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = false)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createMockDisplayItemForCipher(number = 1)
                            .copy(secondSubtitleTestTag = "PasskeySite"),
                    ),
                    displayFolderList = emptyList(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with items and autofill filtering should update ViewState to Content with filtered data`() =
        runTest {
            setupMockUri()

            val cipherView1 = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            val cipherView2 = createMockCipherView(
                number = 2,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )

            coEvery {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView1, cipherView2),
                )
            } returns DecryptFido2CredentialAutofillViewResult.Success(emptyList())

            // Set up the data to be filtered
            mockFilteredCiphers = listOf(cipherView1)

            val autofillSelectionData = AutofillSelectionData(
                type = AutofillSelectionData.Type.LOGIN,
                framework = AutofillSelectionData.Framework.AUTOFILL,
                uri = "https://www.test.com",
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AutofillSelection(
                    autofillSelectionData = autofillSelectionData,
                    shouldFinishWhenComplete = true,
                )
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView1, cipherView2),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()

            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                createVaultItemListingState(
                    viewState = VaultItemListingState.ViewState.Content(
                        displayCollectionList = emptyList(),
                        displayItemList = listOf(
                            createMockDisplayItemForCipher(number = 1).copy(
                                secondSubtitleTestTag = "PasskeySite",
                                subtitleTestTag = "PasskeyName",
                                iconData = IconData.Network(
                                    uri = "https://vault.bitwarden.com/icons/www.mockuri.com/icon.png",
                                    fallbackIconRes = R.drawable.ic_bw_passkey,
                                ),
                                isAutofill = true,
                            ),
                        ),
                        displayFolderList = emptyList(),
                    ),
                )
                    .copy(autofillSelectionData = autofillSelectionData),
                viewModel.stateFlow.value,
            )
            coVerify {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView1, cipherView2),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with items and totp data filtering should update ViewState to Content with filtered data`() =
        runTest {
            setupMockUri()

            val uri = "otpauth://totp/issuer:accountName?secret=secret"
            val totpData = TotpData(
                uri = uri,
                issuer = null,
                accountName = "Name-1",
                secret = "secret",
                digits = 6,
                period = 30,
                algorithm = TotpData.CryptoHashAlgorithm.SHA_1,
            )
            val cipherView1 = createMockCipherView(number = 1)
            val cipherView2 = createMockCipherView(number = 2)

            // Filtering comes later, so we return everything here
            mockFilteredCiphers = listOf(cipherView1, cipherView2)

            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AddTotpLoginItem(data = totpData)
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView1, cipherView2),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()

            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                createVaultItemListingState(
                    viewState = VaultItemListingState.ViewState.Content(
                        displayCollectionList = emptyList(),
                        displayItemList = listOf(
                            createMockDisplayItemForCipher(
                                number = 1,
                                secondSubtitleTestTag = "PasskeySite",
                                isTotp = true,
                            ),
                        ),
                        displayFolderList = emptyList(),
                    ),
                )
                    .copy(totpData = totpData),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with items and fido2 filtering should update ViewState to Content with filtered data`() =
        runTest {
            setupMockUri()

            val cipherView1 = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            val cipherView2 = createMockCipherView(
                number = 2,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )

            every {
                ProviderCreateCredentialRequest.fromBundle(any())
            } returns mockk(relaxed = true) {
                every {
                    callingRequest
                } returns mockk<CreatePublicKeyCredentialRequest>(relaxed = true) {
                    every { origin } returns "mockOrigin"
                }
                every { callingAppInfo } returns mockk(relaxed = true)
            }
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns createMockPasskeyAttestationOptions(number = 1)
            coEvery {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView1, cipherView2),
                )
            } returns DecryptFido2CredentialAutofillViewResult.Success(emptyList())
            coEvery {
                fido2OriginManager.validateOrigin(any())
            } returns Fido2ValidateOriginResult.Success("")

            mockFilteredCiphers = listOf(cipherView1)

            val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
                userId = "activeUserId",
                isUserPreVerified = false,
                requestData = bundleOf(),
            )

            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2Save(
                    fido2CreateCredentialRequest = fido2CreateCredentialRequest,
                )
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView1, cipherView2),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()

            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                createVaultItemListingState(
                    viewState = VaultItemListingState.ViewState.Content(
                        displayCollectionList = emptyList(),
                        displayItemList = listOf(
                            createMockDisplayItemForCipher(number = 1)
                                .copy(
                                    secondSubtitleTestTag = "PasskeySite",
                                    subtitleTestTag = "PasskeyName",
                                    iconData = IconData.Network(
                                        uri = "https://vault.bitwarden.com/icons/www.mockuri.com/icon.png",
                                        fallbackIconRes = R.drawable.ic_bw_passkey,
                                    ),
                                    isFido2Creation = true,
                                ),
                        ),
                        displayFolderList = emptyList(),
                    ),
                )
                    .copy(fido2CreateCredentialRequest = fido2CreateCredentialRequest),
                viewModel.stateFlow.value,
            )
            coVerify {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView1, cipherView2),
                )
                fido2OriginManager.validateOrigin(any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with empty items should update ViewState to NoItems content for Login ItemListingType`() =
        runTest {
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = emptyList(),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel()

            mutableVaultDataStateFlow.tryEmit(value = dataState)

            assertEquals(
                createVaultItemListingState(
                    viewState = VaultItemListingState.ViewState.NoItems(
                        header = null,
                        message = R.string.no_logins.asText(),
                        shouldShowAddButton = true,
                        buttonText = R.string.new_login.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with empty items should update ViewState to NoItems content for Card ItemListingType`() =
        runTest {
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = emptyList(),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel(
                savedStateHandle = createSavedStateHandleWithVaultItemListingType(
                    vaultItemListingType = VaultItemListingType.Card,
                ),
            )

            mutableVaultDataStateFlow.tryEmit(value = dataState)

            assertEquals(
                createVaultItemListingState(
                    itemListingType = VaultItemListingState.ItemListingType.Vault.Card,
                    viewState = VaultItemListingState.ViewState.NoItems(
                        header = null,
                        message = R.string.no_cards.asText(),
                        shouldShowAddButton = true,
                        buttonText = R.string.new_card.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with empty items should update ViewState to NoItems content for Identity ItemListingType`() =
        runTest {
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = emptyList(),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel(
                savedStateHandle = createSavedStateHandleWithVaultItemListingType(
                    vaultItemListingType = VaultItemListingType.Identity,
                ),
            )

            mutableVaultDataStateFlow.tryEmit(value = dataState)

            assertEquals(
                createVaultItemListingState(
                    itemListingType = VaultItemListingState.ItemListingType.Vault.Identity,
                    viewState = VaultItemListingState.ViewState.NoItems(
                        header = null,
                        message = R.string.no_identities.asText(),
                        shouldShowAddButton = true,
                        buttonText = R.string.new_identity.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with empty items should update ViewState to NoItems content for SecureNote ItemListingType`() =
        runTest {
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = emptyList(),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel(
                savedStateHandle = createSavedStateHandleWithVaultItemListingType(
                    vaultItemListingType = VaultItemListingType.SecureNote,
                ),
            )

            mutableVaultDataStateFlow.tryEmit(value = dataState)

            assertEquals(
                createVaultItemListingState(
                    itemListingType = VaultItemListingState.ItemListingType.Vault.SecureNote,
                    viewState = VaultItemListingState.ViewState.NoItems(
                        header = null,
                        message = R.string.no_notes.asText(),
                        shouldShowAddButton = true,
                        buttonText = R.string.new_note.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with empty items should update ViewState to NoItems content for SendFile ItemListingType`() =
        runTest {
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = emptyList(),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel(
                savedStateHandle = createSavedStateHandleWithVaultItemListingType(
                    vaultItemListingType = VaultItemListingType.SendFile,
                ),
            )

            mutableVaultDataStateFlow.tryEmit(value = dataState)

            assertEquals(
                createVaultItemListingState(
                    itemListingType = VaultItemListingState.ItemListingType.Send.SendFile,
                    viewState = VaultItemListingState.ViewState.NoItems(
                        header = null,
                        message = R.string.no_file_sends.asText(),
                        shouldShowAddButton = true,
                        buttonText = R.string.new_file_send.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with empty items should update ViewState to NoItems content for SendText ItemListingType`() =
        runTest {
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = emptyList(),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel(
                savedStateHandle = createSavedStateHandleWithVaultItemListingType(
                    vaultItemListingType = VaultItemListingType.SendText,
                ),
            )

            mutableVaultDataStateFlow.tryEmit(value = dataState)

            assertEquals(
                createVaultItemListingState(
                    itemListingType = VaultItemListingState.ItemListingType.Send.SendText,
                    viewState = VaultItemListingState.ViewState.NoItems(
                        header = null,
                        message = R.string.no_text_sends.asText(),
                        shouldShowAddButton = true,
                        buttonText = R.string.new_text_send.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `vaultDataStateFlow Loaded with trash items should update ViewState to NoItems`() =
        runTest {
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            val viewModel = createVaultItemListingViewModel()

            mutableVaultDataStateFlow.tryEmit(value = dataState)

            assertEquals(
                createVaultItemListingState(
                    viewState = VaultItemListingState.ViewState.NoItems(
                        header = null,
                        message = R.string.no_logins.asText(),
                        shouldShowAddButton = true,
                        buttonText = R.string.new_login.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `vaultDataStateFlow Loading should update state to Loading`() = runTest {
        mutableVaultDataStateFlow.tryEmit(value = DataState.Loading)

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(viewState = VaultItemListingState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Pending with data should update state to Content`() = runTest {
        setupMockUri()

        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Pending(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = false)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createMockDisplayItemForCipher(number = 1)
                            .copy(secondSubtitleTestTag = "PasskeySite"),
                    ),
                    displayFolderList = emptyList(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Pending with empty data should update state to NoItems`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Pending(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = null,
                    message = R.string.no_logins.asText(),
                    shouldShowAddButton = true,
                    buttonText = R.string.new_login.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Pending with trash data should update state to NoItems`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Pending(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = null,
                    message = R.string.no_logins.asText(),
                    shouldShowAddButton = true,
                    buttonText = R.string.new_login.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error without data should update state to Error`() = runTest {
        val dataState = DataState.Error<VaultData>(
            error = IllegalStateException(),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Error(
                    message = R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error with data should update state to Content`() = runTest {
        setupMockUri()

        val dataState = DataState.Error(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = false)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
            error = IllegalStateException(),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createMockDisplayItemForCipher(number = 1)
                            .copy(secondSubtitleTestTag = "PasskeySite"),
                    ),
                    displayFolderList = emptyList(),
                ),
            ),
            viewModel.stateFlow.value,
        )

        unmockkStatic(Uri::class)
    }

    @Test
    fun `vaultDataStateFlow Error with empty data should update state to NoItems`() = runTest {
        val dataState = DataState.Error(
            data = VaultData(
                cipherViewList = emptyList(),
                folderViewList = emptyList(),
                collectionViewList = emptyList(),
                sendViewList = emptyList(),
            ),
            error = IllegalStateException(),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = null,
                    message = R.string.no_logins.asText(),
                    shouldShowAddButton = true,
                    buttonText = R.string.new_login.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error with trash data should update state to NoItems`() = runTest {
        val dataState = DataState.Error(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
            error = IllegalStateException(),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = null,
                    message = R.string.no_logins.asText(),
                    shouldShowAddButton = true,
                    buttonText = R.string.new_login.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow NoNetwork without data should update state to Error`() = runTest {
        val dataState = DataState.NoNetwork<VaultData>()

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Error(
                    message = R.string.internet_connection_required_title
                        .asText()
                        .concat(
                            " ".asText(),
                            R.string.internet_connection_required_message.asText(),
                        ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow NoNetwork with data should update state to Content`() = runTest {
        setupMockUri()

        val dataState = DataState.NoNetwork(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = false)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createMockDisplayItemForCipher(number = 1)
                            .copy(secondSubtitleTestTag = "PasskeySite"),
                    ),
                    displayFolderList = emptyList(),
                ),
            ),
            viewModel.stateFlow.value,
        )

        unmockkStatic(Uri::class)
    }

    @Test
    fun `vaultDataStateFlow NoNetwork with empty data should update state to NoItems`() = runTest {
        val dataState = DataState.NoNetwork(
            data = VaultData(
                cipherViewList = emptyList(),
                folderViewList = emptyList(),
                collectionViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = null,
                    message = R.string.no_logins.asText(),
                    shouldShowAddButton = true,
                    buttonText = R.string.new_login.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow NoNetwork with trash data should update state to NoItems`() = runTest {
        val dataState = DataState.NoNetwork(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = null,
                    message = R.string.no_logins.asText(),
                    shouldShowAddButton = true,
                    buttonText = R.string.new_login.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow updates should do nothing when switching accounts`() {
        val viewModel = createVaultItemListingViewModel()
        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        // Log out the accounts
        mutableUserStateFlow.value = null

        // Emit fresh data
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )

        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `icon loading state updates should update isIconLoadingDisabled`() = runTest {
        val viewModel = createVaultItemListingViewModel()

        assertFalse(viewModel.stateFlow.value.isIconLoadingDisabled)

        mutableIsIconLoadingDisabledFlow.value = true
        assertTrue(viewModel.stateFlow.value.isIconLoadingDisabled)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `RefreshPull should call vault repository sync`() = runTest {
        val viewModel = createVaultItemListingViewModel()

        viewModel.trySendAction(VaultItemListingsAction.RefreshPull)
        advanceTimeBy(300)
        verify(exactly = 1) {
            vaultRepository.sync(forced = false)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `RefreshPull should show network error if no internet connection`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        every {
            networkConnectionManager.isNetworkConnected
        } returns false

        viewModel.trySendAction(VaultItemListingsAction.RefreshPull)
        advanceTimeBy(300)
        assertEquals(
            initialState.copy(
                isRefreshing = false,
                dialogState = VaultItemListingState.DialogState.Error(
                    R.string.internet_connection_required_title.asText(),
                    R.string.internet_connection_required_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 0) {
            vaultRepository.sync(forced = false)
        }
    }

    @Test
    fun `PullToRefreshEnableReceive should update isPullToRefreshEnabled`() = runTest {
        val viewModel = createVaultItemListingViewModel()

        viewModel.trySendAction(
            VaultItemListingsAction.Internal.PullToRefreshEnableReceive(
                isPullToRefreshEnabled = true,
            ),
        )

        assertEquals(
            initialState.copy(isPullToRefreshSettingEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    //region FIDO2 process handling
    @Test
    fun `Fido2CreateCredentialRequest should be evaluated before observing vault data`() {
        val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
            userId = "mockUserId",
            isUserPreVerified = false,
            requestData = bundleOf(),
        )
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
            fido2CreateCredentialRequest,
        )

        every {
            ProviderCreateCredentialRequest.fromBundle(any())
        } returns mockk(relaxed = true) {
            every { callingRequest } returns mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
        }
        every {
            fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
        } returns mockk(relaxed = true)
        coEvery {
            fido2OriginManager.validateOrigin(callingAppInfo = any())
        } returns Fido2ValidateOriginResult.Success("mockOrigin")

        createVaultItemListingViewModel()

        coVerify(ordering = Ordering.ORDERED) {
            fido2OriginManager.validateOrigin(any())
            vaultRepository.vaultDataStateFlow
        }
    }

    @Test
    fun `Fido2ValidateOriginResult should update dialog state on Unknown error`() = runTest {
        val mockFido2CreateRequest = Fido2CreateCredentialRequest(
            userId = "mockUserId",
            isUserPreVerified = false,
            requestData = bundleOf(),
        )

        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
            fido2CreateCredentialRequest = mockFido2CreateRequest,
        )

        every {
            ProviderCreateCredentialRequest.fromBundle(any())
        } returns mockk(relaxed = true) {
            every {
                callingRequest
            } returns mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
        }
        every {
            fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
        } returns mockk(relaxed = true)
        coEvery {
            fido2OriginManager.validateOrigin(any())
        } returns Fido2ValidateOriginResult.Error.Unknown

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                R.string.an_error_has_occurred.asText(),
                R.string.generic_error_message.asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2ValidateOriginResult should update dialog state on PrivilegedAppNotAllowed error`() =
        runTest {
            val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
                userId = "mockUserId",
                isUserPreVerified = false,
                requestData = bundleOf(),
            )

            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = fido2CreateCredentialRequest,
            )

            every {
                ProviderCreateCredentialRequest.fromBundle(any())
            } returns mockk(relaxed = true) {
                every {
                    callingRequest
                } returns mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
            }
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns mockk(relaxed = true)
            coEvery {
                fido2OriginManager.validateOrigin(any())
            } returns Fido2ValidateOriginResult.Error.PrivilegedAppNotAllowed

            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    R.string.an_error_has_occurred.asText(),
                    R.string.passkey_operation_failed_because_browser_is_not_privileged.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2ValidateOriginResult should update dialog state on PrivilegedAppSignatureNotFound error`() =
        runTest {
            val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
                userId = "mockUserId",
                isUserPreVerified = false,
                requestData = bundleOf(),
            )

            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = fido2CreateCredentialRequest,
            )

            every {
                ProviderCreateCredentialRequest.fromBundle(any())
            } returns mockk(relaxed = true) {
                every {
                    callingRequest
                } returns mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
            }
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns mockk(relaxed = true)
            coEvery {
                fido2OriginManager.validateOrigin(any())
            } returns Fido2ValidateOriginResult.Error.PrivilegedAppSignatureNotFound

            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    R.string.an_error_has_occurred.asText(),
                    R.string.passkey_operation_failed_because_browser_signature_does_not_match.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2ValidateOriginResult should update dialog state on PasskeyNotSupportedForApp error`() =
        runTest {
            val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
                userId = "mockUserId",
                isUserPreVerified = false,
                requestData = bundleOf(),
            )

            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = fido2CreateCredentialRequest,
            )

            every {
                ProviderCreateCredentialRequest.fromBundle(any())
            } returns mockk(relaxed = true) {
                every {
                    callingRequest
                } returns mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
            }
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns mockk(relaxed = true)
            coEvery {
                fido2OriginManager.validateOrigin(any())
            } returns Fido2ValidateOriginResult.Error.PasskeyNotSupportedForApp

            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    R.string.an_error_has_occurred.asText(),
                    R.string.passkeys_not_supported_for_this_app.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2ValidateOriginResult should update dialog state on AssetLinkNotFound error`() =
        runTest {
            val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
                userId = "mockUserId",
                isUserPreVerified = false,
                requestData = bundleOf(),
            )

            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = fido2CreateCredentialRequest,
            )

            every {
                ProviderCreateCredentialRequest.fromBundle(any())
            } returns mockk(relaxed = true) {
                every {
                    callingRequest
                } returns mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
            }
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns mockk(relaxed = true)
            coEvery {
                fido2OriginManager.validateOrigin(any())
            } returns Fido2ValidateOriginResult.Error.AssetLinkNotFound

            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    R.string.an_error_has_occurred.asText(),
                    R.string.passkey_operation_failed_because_of_missing_asset_links.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2RegisterCredentialResult Error should show toast and emit CompleteFido2Registration result`() =
        runTest {
            val mockResult = Fido2RegisterCredentialResult.Error.InternalError

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.Internal.Fido2RegisterCredentialResultReceive(
                    mockResult,
                ),
            )

            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.ShowToast(R.string.an_error_has_occurred.asText()),
                    awaitItem(),
                )

                assertEquals(
                    VaultItemListingEvent.CompleteFido2Registration(
                        RegisterFido2CredentialResult.Error(
                            R.string.passkey_registration_failed_due_to_an_internal_error.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2RegisterCredentialResult Success should show toast and emit CompleteFido2Registration result`() =
        runTest {
            val mockResult = Fido2RegisterCredentialResult.Success(
                responseJson = "mockResponse",
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.Internal.Fido2RegisterCredentialResultReceive(
                    mockResult,
                ),
            )

            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.ShowToast(R.string.item_updated.asText()),
                    awaitItem(),
                )

                assertEquals(
                    VaultItemListingEvent.CompleteFido2Registration(
                        RegisterFido2CredentialResult.Success(
                            responseJson = "mockResponse",
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissFido2ErrorDialogClick should clear the dialog state then complete FIDO 2 registration based on state`() =
        runTest {
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                createMockFido2CreateCredentialRequest(number = 1),
            )
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.DismissFido2ErrorDialogClick(
                    "".asText(),
                ),
            )
            viewModel.eventFlow.test {
                assertNull(viewModel.stateFlow.value.dialogState)
                assertEquals(
                    VaultItemListingEvent.CompleteFido2Registration(
                        result = RegisterFido2CredentialResult.Error(
                            "".asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissFido2ErrorDialogClick should clear dialog state then complete FIDO 2 assertion with error when assertion request is not null`() =
        runTest {
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                createMockFido2CredentialAssertionRequest(number = 1),
            )
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(
                createMockCipherView(
                    number = 1,
                    fido2Credentials = mockFido2CredentialList,
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.DismissFido2ErrorDialogClick("".asText()),
            )
            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.CompleteFido2Assertion(
                        result = AssertFido2CredentialResult.Error("".asText()),
                    ),
                    awaitItem(),
                )
                assertNull(viewModel.stateFlow.value.dialogState)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissFido2ErrorDialogClick should show general error dialog when no FIDO 2 request is present`() =
        runTest {
            specialCircumstanceManager.specialCircumstance = null
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.DismissFido2ErrorDialogClick("".asText()),
            )
            assertEquals(
                VaultItemListingState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = "".asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2GetCredentialsRequest should validate request and emit CompleteFido2GetCredentialsRequest event`() =
        runTest {
            setupMockUri()
            val mockGetCredentialsRequest = createMockFido2GetCredentialsRequest(number = 1)
            val mockFido2CredentialsList = createMockSdkFido2CredentialList(number = 1)
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialsList,
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2GetCredentials(
                    mockGetCredentialsRequest,
                )
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(any())
            } returns mockk(relaxed = true)
            coEvery {
                fido2OriginManager.validateOrigin(callingAppInfo = any())
            } returns Fido2ValidateOriginResult.Success("mockOrigin")
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(mockCipherView)

            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(mockCipherView),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.CompleteFido2GetCredentialsRequest(
                        result = GetFido2CredentialsResult.Success(
                            userId = "mockUserId-1",
                            option = mockBeginGetPublicKeyCredentialOption,
                            credentials = emptyList(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `Fido2GetCredentialsRequest should display error dialog when request is invalid`() =
        runTest {
            setupMockUri()
            val mockGetCredentialsRequest = createMockFido2GetCredentialsRequest(number = 1)
            val mockFido2CredentialsList = createMockSdkFido2CredentialList(number = 1)
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialsList,
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2GetCredentials(
                    mockGetCredentialsRequest,
                )

            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(mockCipherView)
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(any())
            } returns mockk(relaxed = true)
            every {
                mockBeginGetCredentialRequest.beginGetCredentialOptions
            } returns emptyList()

            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(mockCipherView),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    title = R.string.an_error_has_occurred.asText(),
                    message =
                        R.string.passkey_operation_failed_because_the_request_is_invalid.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2GetCredentialsRequest should display error dialog when relyingParty cannot be identified`() =
        runTest {
            setupMockUri()
            val mockGetCredentialsRequest = createMockFido2GetCredentialsRequest(number = 1)
            val mockFido2CredentialsList = createMockSdkFido2CredentialList(number = 1)
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialsList,
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2GetCredentials(
                    mockGetCredentialsRequest,
                )

            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(mockCipherView)
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(any())
            } returns mockk(relaxed = true) {
                every { relyingPartyId } returns null
            }

            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(mockCipherView),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    title = R.string.an_error_has_occurred.asText(),
                    message =
                        R.string.passkey_operation_failed_because_relying_party_cannot_be_identified
                            .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2GetCredentialsRequest should display error dialog when callingApp cannot be verified`() =
        runTest {
            setupMockUri()
            val mockGetCredentialsRequest = createMockFido2GetCredentialsRequest(number = 1)
            val mockFido2CredentialsList = createMockSdkFido2CredentialList(number = 1)
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialsList,
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2GetCredentials(
                    mockGetCredentialsRequest,
                )

            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(mockCipherView)
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(any())
            } returns mockk(relaxed = true)
            every {
                mockBeginGetCredentialRequest.callingAppInfo
            } returns null

            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(mockCipherView),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    title = R.string.an_error_has_occurred.asText(),
                    message =
                        R.string.passkey_operation_failed_because_app_could_not_be_verified
                            .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Test
    fun `Fido2GetCredentialsRequest should display error dialog when origin validation fails`() =
        runTest {
            setupMockUri()
            val mockGetCredentialsRequest = createMockFido2GetCredentialsRequest(number = 1)
            val mockFido2CredentialsList = createMockSdkFido2CredentialList(number = 1)
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialsList,
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2GetCredentials(
                    mockGetCredentialsRequest,
                )
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(any())
            } returns mockk(relaxed = true)
            coEvery {
                fido2OriginManager.validateOrigin(callingAppInfo = any())
            } returns Fido2ValidateOriginResult.Error.Unknown

            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(mockCipherView),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.generic_error_message.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2AssertionRequest should display loading dialog then request user verification when user is not verified and verification is REQUIRED`() =
        runTest {
            setupMockUri()
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = "mockId-1")
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialList,
            )
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every {
                fido2CredentialManager.getUserVerificationRequirement(
                    any<ProviderGetCredentialRequest>(),
                )
            } returns UserVerificationRequirement.REQUIRED
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(
                createMockCipherView(
                    number = 1,
                    fido2Credentials = mockFido2CredentialList,
                ),
            )

            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(mockCipherView),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingState.DialogState.Loading(R.string.loading.asText()),
                    viewModel.stateFlow.value.dialogState,
                )
                verify { fido2CredentialManager.isUserVerified }
                assertEquals(
                    VaultItemListingEvent.Fido2UserVerification(
                        isRequired = true,
                        selectedCipherView = mockCipherView,
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2AssertionRequest should display loading dialog then request user verification when user is not verified and verification is PREFERED`() =
        runTest {
            setupMockUri()
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = "mockId-1")
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialList,
            )
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every {
                fido2CredentialManager.getUserVerificationRequirement(
                    any<ProviderGetCredentialRequest>(),
                )
            } returns UserVerificationRequirement.PREFERRED
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(
                createMockCipherView(
                    number = 1,
                    fido2Credentials = mockFido2CredentialList,
                ),
            )

            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(mockCipherView),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState
            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingState.DialogState.Loading(R.string.loading.asText()),
                    viewModel.stateFlow.value.dialogState,
                )
                verify { fido2CredentialManager.isUserVerified }
                assertEquals(
                    VaultItemListingEvent.Fido2UserVerification(
                        isRequired = false,
                        selectedCipherView = mockCipherView,
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2AssertionRequest should skip user verification when user is not verified and verification is DISCOURAGED`() =
        runTest {
            setupMockUri()
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = "mockId-1")
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialList,
            )
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every { authRepository.activeUserId } returns "activeUserId"
            every {
                fido2CredentialManager.getUserVerificationRequirement(
                    any<ProviderGetCredentialRequest>(),
                )
            } returns UserVerificationRequirement.DISCOURAGED
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(any())
            } returns createMockPasskeyAssertionOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.DISCOURAGED,
            )
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(
                createMockCipherView(
                    number = 1,
                    fido2Credentials = mockFido2CredentialList,
                ),
            )
            coEvery {
                fido2CredentialManager.authenticateFido2Credential(
                    userId = "activeUserId",
                    request = any(),
                    selectedCipherView = mockCipherView,
                    callingAppInfo = any(),
                    origin = null,
                )
            } returns Fido2CredentialAssertionResult.Success(responseJson = "responseJson")

            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(mockCipherView),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            coVerify {
                fido2CredentialManager.isUserVerified
                fido2CredentialManager.authenticateFido2Credential(
                    userId = "activeUserId",
                    request = any(),
                    selectedCipherView = mockCipherView,
                    callingAppInfo = any(),
                    origin = null,
                )
            }
        }

    @Test
    fun `Fido2AssertionRequest should show error dialog when validateOrigin is not Success`() =
        runTest {
            setupMockUri()
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = "mockId-1")
            val mockAssertionOptions = createMockPasskeyAssertionOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.DISCOURAGED,
            )
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialList,
            )
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every { fido2CredentialManager.isUserVerified } returns true
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(mockCipherView)
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(any())
            } returns mockAssertionOptions
            coEvery {
                fido2OriginManager.validateOrigin(any())
            } returns Fido2ValidateOriginResult.Error.Unknown

            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(mockCipherView),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            viewModel.stateFlow.test {
                assertEquals(
                    VaultItemListingState.DialogState.Fido2OperationFail(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.generic_error_message.asText(),
                    ),
                    awaitItem().dialogState,
                )
            }
        }

    @Test
    fun `Fido2AssertionRequest should show error dialog when cipher state flow data is null`() =
        runTest {
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns null

            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = emptyList(),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string
                        .passkey_operation_failed_because_the_selected_item_does_not_exist
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2AssertionRequest should show error dialog when cipher state flow data has no matching cipher`() =
        runTest {
            setupMockUri()
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialList,
            )
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(mockCipherView)

            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(mockCipherView),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string
                        .passkey_operation_failed_because_the_selected_item_does_not_exist
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Test
    fun `Fido2AssertionRequest should skip user verification when user is verified`() = runTest {
        setupMockUri()
        val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
            .copy(cipherId = "mockId-1")
        val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
        val mockCipherView = createMockCipherView(
            number = 1,
            fido2Credentials = mockFido2CredentialList,
        )
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
            mockAssertionRequest,
        )
        every { fido2CredentialManager.isUserVerified } returns true
        every {
            fido2CredentialManager.getUserVerificationRequirement(
                any<ProviderGetCredentialRequest>(),
            )
        } returns UserVerificationRequirement.PREFERRED
        every {
            fido2CredentialManager.getPasskeyAssertionOptionsOrNull(any())
        } returns createMockPasskeyAssertionOptions(
            number = 1,
            userVerificationRequirement = UserVerificationRequirement.PREFERRED,
        )
        coEvery {
            fido2CredentialManager.authenticateFido2Credential(
                userId = "activeUserId",
                request = any(),
                selectedCipherView = mockCipherView,
                callingAppInfo = any(),
                origin = null,
            )
        } returns Fido2CredentialAssertionResult.Success(responseJson = "responseJson")

        every {
            vaultRepository
                .ciphersStateFlow
                .value
                .data
        } returns listOf(mockCipherView)
        every { authRepository.activeUserId } returns "activeUserId"

        val dataState = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(mockCipherView),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )
        createVaultItemListingViewModel()
        mutableVaultDataStateFlow.value = dataState

        coVerify {
            fido2CredentialManager.isUserVerified
            fido2CredentialManager.authenticateFido2Credential(
                userId = any(),
                request = any(),
                selectedCipherView = any(),
                callingAppInfo = any(),
                origin = null,
            )
        }
    }

    @Test
    fun `Fido2AssertionRequest should show error dialog when active user id is null`() = runTest {
        setupMockUri()
        val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
            .copy(cipherId = "mockId-1")
        val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
        val mockCipherView = createMockCipherView(
            number = 1,
            fido2Credentials = mockFido2CredentialList,
        )
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
            mockAssertionRequest,
        )
        every { fido2CredentialManager.isUserVerified } returns true
        every {
            fido2CredentialManager.getPasskeyAssertionOptionsOrNull("mockRequestJson")
        } returns createMockPasskeyAssertionOptions(
            number = 1,
            userVerificationRequirement = UserVerificationRequirement.PREFERRED,
        )
        every {
            vaultRepository
                .ciphersStateFlow
                .value
                .data
        } returns listOf(mockCipherView)
        every {
            fido2CredentialManager.getPasskeyAssertionOptionsOrNull("mockRequestJson")
        } returns createMockPasskeyAssertionOptions(number = 1)
        every { authRepository.activeUserId } returns null

        val dataState = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(mockCipherView),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )
        val viewModel = createVaultItemListingViewModel()
        mutableVaultDataStateFlow.value = dataState

        coVerify(exactly = 0) {
            fido2CredentialManager.authenticateFido2Credential(
                userId = any(),
                request = any(),
                selectedCipherView = any(),
                callingAppInfo = any(),
                origin = null,
            )
        }

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2AssertionRequest should prompt for master password when passkey is protected and user has master password`() {
        setupMockUri()
        val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
            .copy(cipherId = "mockId-1")
        val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
        val mockCipherView = createMockCipherView(
            number = 1,
            fido2Credentials = mockFido2CredentialList,
            repromptType = CipherRepromptType.PASSWORD,
        )
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
            mockAssertionRequest,
        )
        every { fido2CredentialManager.isUserVerified } returns true
        every {
            fido2CredentialManager.getPasskeyAssertionOptionsOrNull("mockRequestJson")
        } returns createMockPasskeyAssertionOptions(
            number = 1,
            userVerificationRequirement = UserVerificationRequirement.PREFERRED,
        )
        every {
            vaultRepository
                .ciphersStateFlow
                .value
                .data
        } returns listOf(mockCipherView)
        every {
            fido2CredentialManager.getPasskeyAssertionOptionsOrNull("mockRequestJson")
        } returns createMockPasskeyAssertionOptions(number = 1)
        every { authRepository.activeUserId } returns null

        val dataState = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(mockCipherView),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )
        val viewModel = createVaultItemListingViewModel()
        mutableVaultDataStateFlow.value = dataState

        assertEquals(
            VaultItemListingState.DialogState.Fido2MasterPasswordPrompt(
                selectedCipherId = mockAssertionRequest.cipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2AssertionRequest should not re-prompt master password when user does not have a master password`() =
        runTest {
            setupMockUri()
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = "mockId-1")
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialList,
                repromptType = CipherRepromptType.PASSWORD,
            )
            mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                accounts = listOf(
                    DEFAULT_ACCOUNT.copy(
                        vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                        trustedDevice = UserState.TrustedDevice(
                            isDeviceTrusted = true,
                            hasAdminApproval = true,
                            hasLoginApprovingDevice = true,
                            hasResetPasswordPermission = true,
                        ),
                        hasMasterPassword = false,
                    ),
                ),
            )

            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every { fido2CredentialManager.isUserVerified } returns true
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(mockCipherView)
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(any())
            } returns createMockPasskeyAssertionOptions(number = 1)
            coEvery {
                fido2CredentialManager.authenticateFido2Credential(
                    DEFAULT_USER_STATE.activeUserId,
                    request = any(),
                    selectedCipherView = mockCipherView,
                    callingAppInfo = any(),
                    origin = null,
                )
            } returns Fido2CredentialAssertionResult.Success("responseJson")

            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(mockCipherView),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            coVerify {
                fido2CredentialManager.authenticateFido2Credential(
                    userId = any(),
                    request = any(),
                    selectedCipherView = any(),
                    callingAppInfo = any(),
                    origin = null,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationLockout should display Fido2ErrorDialog and set isUserVerified to false`() {
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(VaultItemListingsAction.UserVerificationLockOut)

        verify { fido2CredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_is_locked_out.asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationCancelled should clear dialog state, set isUserVerified to false, and emit CompleteFido2Create with cancelled result`() =
        runTest {
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                createMockFido2CreateCredentialRequest(number = 1),
            )
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(VaultItemListingsAction.UserVerificationCancelled)

            verify { fido2CredentialManager.isUserVerified = false }
            assertNull(viewModel.stateFlow.value.dialogState)
            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.CompleteFido2Registration(
                        result = RegisterFido2CredentialResult.Cancelled,
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationCancelled should clear dialog state, set isUserVerified to false, and emit CompleteFido2Assertion with cancelled result`() =
        runTest {
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                createMockFido2CredentialAssertionRequest(number = 1),
            )
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(VaultItemListingsAction.UserVerificationCancelled)

            verify { fido2CredentialManager.isUserVerified = false }
            assertNull(viewModel.stateFlow.value.dialogState)
            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.CompleteFido2Assertion(
                        result = AssertFido2CredentialResult.Cancelled,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `UserVerificationFail should display Fido2ErrorDialog and set isUserVerified to false`() {
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(VaultItemListingsAction.UserVerificationFail)

        verify { fido2CredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationSuccess should display Fido2ErrorDialog when SpecialCircumstance is null`() =
        runTest {
            specialCircumstanceManager.specialCircumstance = null
            coEvery {
                fido2CredentialManager.registerFido2Credential(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns Fido2RegisterCredentialResult.Success(
                responseJson = "mockResponse",
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationSuccess(
                    createMockCipherView(number = 1),
                ),
            )

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.passkey_operation_failed_because_the_request_is_invalid
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationSuccess should display Fido2ErrorDialog when SpecialCircumstance is invalid`() =
        runTest {
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AutofillSave(
                    AutofillSaveItem.Login(
                        username = "mockUsername",
                        password = "mockPassword",
                        uri = "mockUri",
                    ),
                )
            coEvery {
                fido2CredentialManager.registerFido2Credential(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns Fido2RegisterCredentialResult.Success(
                responseJson = "mockResponse",
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationSuccess(
                    selectedCipherView = createMockCipherView(number = 1),
                ),
            )

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.passkey_operation_failed_because_the_request_is_invalid
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationSuccess should display Fido2ErrorDialog when activeUserId is null`() {
        every { authRepository.activeUserId } returns null
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.Fido2Save(createMockFido2CreateCredentialRequest(number = 1))

        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationSuccess(
                selectedCipherView = createMockCipherView(number = 1),
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationSuccess should set isUserVerified to true, and register FIDO 2 credential when verification result is received`() =
        runTest {
            val mockRequest = createMockFido2CreateCredentialRequest(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = mockRequest,
            )
            every {
                ProviderCreateCredentialRequest.fromBundle(any())
            } returns mockk(relaxed = true) {
                every {
                    callingRequest
                } returns mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
            }
            coEvery {
                fido2CredentialManager.registerFido2Credential(
                    userId = any(),
                    callingAppInfo = any(),
                    createPublicKeyCredentialRequest = any(),
                    selectedCipherView = any(),
                )
            } returns Fido2RegisterCredentialResult.Success(
                responseJson = "mockResponse",
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationSuccess(
                    selectedCipherView = createMockCipherView(number = 1),
                ),
            )

            coVerify {
                fido2CredentialManager.isUserVerified = true
                fido2CredentialManager.registerFido2Credential(
                    userId = DEFAULT_ACCOUNT.userId,
                    createPublicKeyCredentialRequest = any(),
                    selectedCipherView = any(),
                    callingAppInfo = any(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationSuccess should set isUserVerified to true, and authenticate FIDO 2 credential when verification result is received`() =
        runTest {
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = "mockId-1")
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                fido2AssertionRequest = mockAssertionRequest,
            )

            every {
                ProviderGetCredentialRequest.fromBundle(any())
            } returns mockk(relaxed = true) {
                every {
                    credentialOptions
                } returns listOf(
                    mockk<GetPublicKeyCredentialOption>(relaxed = true),
                )
            }
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(
                createMockCipherView(
                    number = 1,
                    fido2Credentials = mockFido2CredentialList,
                ),
            )
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(any())
            } returns createMockPasskeyAssertionOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.PREFERRED,
            )
            coEvery {
                fido2CredentialManager.authenticateFido2Credential(
                    userId = any(),
                    callingAppInfo = any(),
                    request = any(),
                    selectedCipherView = any(),
                    origin = any(),
                )
            } returns Fido2CredentialAssertionResult.Success(
                responseJson = "mockResponse",
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationSuccess(
                    selectedCipherView = createMockCipherView(number = 1),
                ),
            )

            coVerify {
                fido2CredentialManager.isUserVerified = true
                fido2CredentialManager.authenticateFido2Credential(
                    userId = DEFAULT_ACCOUNT.userId,
                    request = any(),
                    selectedCipherView = any(),
                    callingAppInfo = any(),
                    origin = null,
                )
            }
        }

    @Test
    fun `UserVerificationNotSupported should display Fido2CreationFail when no cipher id found`() {
        val viewModel = createVaultItemListingViewModel()

        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationNotSupported(
                selectedCipherId = null,
            ),
        )

        verify { fido2CredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationNotSupported should display Fido2CreationFail when no active account found`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        mutableUserStateFlow.value = null

        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationNotSupported(
                selectedCipherId = selectedCipherId,
            ),
        )

        verify { fido2CredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `UserVerificationNotSupported should display Fido2PinPrompt when user has pin`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(
                DEFAULT_ACCOUNT.copy(
                    vaultUnlockType = VaultUnlockType.PIN,
                ),
            ),
        )
        every { settingsRepository.isUnlockWithPinEnabled } returns true

        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationNotSupported(
                selectedCipherId = selectedCipherId,
            ),
        )

        verify { fido2CredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.Fido2PinPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationNotSupported should display Fido2MasterPasswordPrompt when user has password but no pin`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"

        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationNotSupported(
                selectedCipherId = selectedCipherId,
            ),
        )

        verify { fido2CredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.Fido2MasterPasswordPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationNotSupported should display Fido2PinSetUpPrompt when user has no password or pin`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(
                DEFAULT_ACCOUNT.copy(
                    vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                    trustedDevice = UserState.TrustedDevice(
                        isDeviceTrusted = true,
                        hasAdminApproval = true,
                        hasLoginApprovingDevice = true,
                        hasResetPasswordPermission = true,
                    ),
                    hasMasterPassword = false,
                ),
            ),
        )

        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationNotSupported(
                selectedCipherId = selectedCipherId,
            ),
        )

        verify { fido2CredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.Fido2PinSetUpPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordFido2VerificationSubmit should display Fido2ErrorDialog when password verification fails`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val password = "password"
        coEvery {
            authRepository.validatePassword(password = password)
        } returns ValidatePasswordResult.Error(error = Throwable("Fail!"))

        viewModel.trySendAction(
            VaultItemListingsAction.MasterPasswordFido2VerificationSubmit(
                password = password,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePassword(password = password)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordFido2VerificationSubmit should display Fido2MasterPasswordError when user has retries remaining`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val password = "password"
        coEvery {
            authRepository.validatePassword(password = password)
        } returns ValidatePasswordResult.Success(isValid = false)

        viewModel.trySendAction(
            VaultItemListingsAction.MasterPasswordFido2VerificationSubmit(
                password = password,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2MasterPasswordError(
                title = null,
                message = R.string.invalid_master_password.asText(),
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePassword(password = password)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordFido2VerificationSubmit should display Fido2ErrorDialog when user has no retries remaining`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val password = "password"
        every { fido2CredentialManager.hasAuthenticationAttemptsRemaining() } returns false
        coEvery {
            authRepository.validatePassword(password = password)
        } returns ValidatePasswordResult.Success(isValid = false)

        viewModel.trySendAction(
            VaultItemListingsAction.MasterPasswordFido2VerificationSubmit(
                password = password,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string
                    .passkey_operation_failed_because_user_verification_attempts_exceeded
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePassword(password = password)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordFido2VerificationSubmit should display Fido2ErrorDialog when cipher not found`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val password = "password"
        coEvery {
            authRepository.validatePassword(password = password)
        } returns ValidatePasswordResult.Success(isValid = true)

        viewModel.trySendAction(
            VaultItemListingsAction.MasterPasswordFido2VerificationSubmit(
                password = password,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_the_selected_item_does_not_exist
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePassword(password = password)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordFido2VerificationSubmit should register credential when password authenticated successfully`() =
        runTest {
            val viewModel = createVaultItemListingViewModel()
            val cipherView = createMockCipherView(number = 1)
            val selectedCipherId = cipherView.id ?: ""
            val password = "password"
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    collectionViewList = emptyList(),
                    folderViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)

            viewModel.trySendAction(
                VaultItemListingsAction.MasterPasswordFido2VerificationSubmit(
                    password = password,
                    selectedCipherId = selectedCipherId,
                ),
            )
            coVerify {
                authRepository.validatePassword(password = password)
            }
        }

    @Test
    fun `RetryFido2PasswordVerificationClick should display Fido2MasterPasswordPrompt`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"

        viewModel.trySendAction(
            VaultItemListingsAction.RetryFido2PasswordVerificationClick(
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2MasterPasswordPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PinFido2VerificationSubmit should display Fido2ErrorDialog when pin verification fails`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val pin = "PIN"
        coEvery {
            authRepository.validatePin(pin = pin)
        } returns ValidatePinResult.Error(error = Throwable("Fail!"))

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2VerificationSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePin(pin = pin)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PinFido2VerificationSubmit should display Fido2PinError when user has retries remaining`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val pin = "PIN"
        coEvery {
            authRepository.validatePin(pin = pin)
        } returns ValidatePinResult.Success(isValid = false)

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2VerificationSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2PinError(
                title = null,
                message = R.string.invalid_pin.asText(),
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePin(pin = pin)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PinFido2VerificationSubmit should display Fido2ErrorDialog when user has no retries remaining`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val pin = "PIN"
        every { fido2CredentialManager.hasAuthenticationAttemptsRemaining() } returns false
        coEvery {
            authRepository.validatePin(pin = pin)
        } returns ValidatePinResult.Success(isValid = false)

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2VerificationSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string
                    .passkey_operation_failed_because_user_verification_attempts_exceeded
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePin(pin = pin)
        }
    }

    @Test
    fun `PinFido2VerificationSubmit should display Fido2ErrorDialog when cipher not found`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val pin = "PIN"
        coEvery {
            authRepository.validatePin(pin = pin)
        } returns ValidatePinResult.Success(isValid = true)

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2VerificationSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_the_selected_item_does_not_exist
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePin(pin = pin)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PinFido2VerificationSubmit should register credential when pin authenticated successfully`() {
        setupMockUri()
        val viewModel = createVaultItemListingViewModel()
        val cipherView = createMockCipherView(number = 1)
        val selectedCipherId = cipherView.id ?: ""
        val pin = "PIN"
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(cipherView),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )
        coEvery {
            authRepository.validatePin(pin = pin)
        } returns ValidatePinResult.Success(isValid = true)

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2VerificationSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )
        coVerify {
            authRepository.validatePin(pin = pin)
        }
    }

    @Test
    fun `RetryFido2PinVerificationClick should display FidoPinPrompt`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"

        viewModel.trySendAction(
            VaultItemListingsAction.RetryFido2PinVerificationClick(
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2PinPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `PinFido2SetUpSubmit should display Fido2PinSetUpError for empty PIN`() {
        val viewModel = createVaultItemListingViewModel()
        val pin = ""
        val selectedCipherId = "selectedCipherId"

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2SetUpSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2PinSetUpError(
                title = null,
                message = R.string.validation_field_required.asText(R.string.pin.asText()),
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `PinFido2SetUpSubmit should save PIN and register credential for non-empty PIN`() {
        setupMockUri()
        val viewModel = createVaultItemListingViewModel()
        val cipherView = createMockCipherView(number = 1)
        val pin = "PIN"
        val selectedCipherId = "selectedCipherId"
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(cipherView),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )
        every {
            settingsRepository.storeUnlockPin(
                pin = pin,
                shouldRequireMasterPasswordOnRestart = false,
            )
        } just runs

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2SetUpSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        verify(exactly = 1) {
            settingsRepository.storeUnlockPin(
                pin = pin,
                shouldRequireMasterPasswordOnRestart = false,
            )
        }
    }

    @Test
    fun `PinFido2SetUpRetryClick should display Fido2PinSetUpPrompt`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2SetUpRetryClick(
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2PinSetUpPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `DismissFido2VerificationDialogClick should display Fido2ErrorDialog`() {
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(
            VaultItemListingsAction.DismissFido2VerificationDialogClick,
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_verification_was_cancelled
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `ConfirmOverwriteExistingPasskeyClick should check if user is verified`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = createMockFido2CreateCredentialRequest(number = 1),
            )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns createMockPasskeyAttestationOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.REQUIRED,
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.ConfirmOverwriteExistingPasskeyClick(
                    cipherViewId = cipherView.id!!,
                ),
            )

            verify { fido2CredentialManager.isUserVerified }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmOverwriteExistingPasskeyClick should display Fido2ErrorDialog when getSelectedCipher returns null`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = createMockFido2CreateCredentialRequest(number = 1),
            )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.ConfirmOverwriteExistingPasskeyClick(
                    cipherViewId = "invalidId",
                ),
            )

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    R.string.an_error_has_occurred.asText(),
                    R.string.passkey_operation_failed_because_the_selected_item_does_not_exist
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    //endregion FIDO2 process handling

    @Test
    fun `InternetConnectionErrorReceived should show network error if no internet connection`() =
        runTest {
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.Internal.InternetConnectionErrorReceived,
            )
            assertEquals(
                initialState.copy(
                    isRefreshing = false,
                    dialogState = VaultItemListingState.DialogState.Error(
                        R.string.internet_connection_required_title.asText(),
                        R.string.internet_connection_required_message.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("CyclomaticComplexMethod")
    private fun createSavedStateHandleWithVaultItemListingType(
        vaultItemListingType: VaultItemListingType,
    ) = SavedStateHandle().apply {
        set(
            "vault_item_listing_type",
            when (vaultItemListingType) {
                is VaultItemListingType.Card -> "card"
                is VaultItemListingType.Collection -> "collection"
                is VaultItemListingType.Folder -> "folder"
                is VaultItemListingType.Identity -> "identity"
                is VaultItemListingType.Login -> "login"
                is VaultItemListingType.SecureNote -> "secure_note"
                is VaultItemListingType.Trash -> "trash"
                is VaultItemListingType.SendFile -> "send_file"
                is VaultItemListingType.SendText -> "send_text"
                is VaultItemListingType.SshKey -> "ssh_key"
            },
        )
        set(
            "id",
            when (vaultItemListingType) {
                is VaultItemListingType.Card -> null
                is VaultItemListingType.Collection -> vaultItemListingType.collectionId
                is VaultItemListingType.Folder -> vaultItemListingType.folderId
                is VaultItemListingType.Identity -> null
                is VaultItemListingType.Login -> null
                is VaultItemListingType.SecureNote -> null
                is VaultItemListingType.Trash -> null
                is VaultItemListingType.SendFile -> null
                is VaultItemListingType.SendText -> null
                is VaultItemListingType.SshKey -> null
            },
        )
    }

    private fun setupMockUri() {
        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.mockuri.com"
    }

    private fun createVaultItemListingViewModel(
        savedStateHandle: SavedStateHandle = initialSavedStateHandle,
    ): VaultItemListingViewModel =
        VaultItemListingViewModel(
            savedStateHandle = savedStateHandle,
            clock = clock,
            clipboardManager = clipboardManager,
            authRepository = authRepository,
            vaultRepository = vaultRepository,
            environmentRepository = environmentRepository,
            settingsRepository = settingsRepository,
            accessibilitySelectionManager = accessibilitySelectionManager,
            autofillSelectionManager = autofillSelectionManager,
            cipherMatchingManager = cipherMatchingManager,
            specialCircumstanceManager = specialCircumstanceManager,
            policyManager = policyManager,
            fido2CredentialManager = fido2CredentialManager,
            organizationEventManager = organizationEventManager,
            fido2OriginManager = fido2OriginManager,
            networkConnectionManager = networkConnectionManager,
        )

    @Suppress("MaxLineLength")
    private fun createVaultItemListingState(
        itemListingType: VaultItemListingState.ItemListingType = VaultItemListingState.ItemListingType.Vault.Login,
        viewState: VaultItemListingState.ViewState = VaultItemListingState.ViewState.Loading,
        dialogState: VaultItemListingState.DialogState? = null,
    ): VaultItemListingState =
        VaultItemListingState(
            itemListingType = itemListingType,
            activeAccountSummary = DEFAULT_USER_STATE.toActiveAccountSummary(),
            accountSummaries = DEFAULT_USER_STATE.toAccountSummaries(),
            viewState = viewState,
            vaultFilterType = vaultRepository.vaultFilterType,
            baseWebSendUrl = Environment.Us.environmentUrlData.baseWebSendUrl,
            baseIconUrl = environmentRepository.environment.environmentUrlData.baseIconUrl,
            isIconLoadingDisabled = settingsRepository.isIconLoadingDisabled,
            isPullToRefreshSettingEnabled = false,
            dialogState = dialogState,
            totpData = null,
            autofillSelectionData = null,
            policyDisablesSend = false,
            hasMasterPassword = true,
            fido2CreateCredentialRequest = null,
            isPremium = true,
            isRefreshing = false,
        )
}

private val DEFAULT_ACCOUNT = UserState.Account(
    userId = "activeUserId",
    name = "Active User",
    email = "active@bitwarden.com",
    environment = Environment.Us,
    avatarColorHex = "#aa00aa",
    isPremium = true,
    isLoggedIn = true,
    isVaultUnlocked = true,
    needsPasswordReset = false,
    isBiometricsEnabled = false,
    organizations = emptyList(),
    needsMasterPassword = false,
    trustedDevice = null,
    hasMasterPassword = true,
    isUsingKeyConnector = false,
    onboardingStatus = OnboardingStatus.COMPLETE,
    firstTimeState = FirstTimeState(showImportLoginsCard = true),
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = "activeUserId",
    accounts = listOf(DEFAULT_ACCOUNT),
)
