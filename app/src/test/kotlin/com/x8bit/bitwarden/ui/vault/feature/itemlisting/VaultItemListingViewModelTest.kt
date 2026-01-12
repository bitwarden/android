package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import android.net.Uri
import androidx.core.os.bundleOf
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.toast.ToastManager
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.data.repository.util.baseWebSendUrl
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.send.SendType
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.account.model.AccountSummary
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.model.TotpData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
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
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManagerImpl
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.credentials.manager.BitwardenCredentialManager
import com.x8bit.bitwarden.data.credentials.manager.OriginManager
import com.x8bit.bitwarden.data.credentials.model.CreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.credentials.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.credentials.model.UserVerificationRequirement
import com.x8bit.bitwarden.data.credentials.model.ValidateOriginResult
import com.x8bit.bitwarden.data.credentials.model.createMockCreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.createMockFido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.credentials.model.createMockGetCredentialsRequest
import com.x8bit.bitwarden.data.credentials.model.createMockProviderGetPasswordCredentialRequest
import com.x8bit.bitwarden.data.credentials.parser.RelyingPartyParser
import com.x8bit.bitwarden.data.credentials.repository.PrivilegedAppRepository
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
import com.x8bit.bitwarden.data.platform.util.getSignatureFingerprintAsHexString
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCardView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockDecryptCipherListResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFido2CredentialList
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.credentials.manager.model.AssertFido2CredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.CreateCredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.GetCredentialsResult
import com.x8bit.bitwarden.ui.credentials.manager.model.GetPasswordCredentialResult
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import com.x8bit.bitwarden.ui.vault.components.model.CreateVaultItemType
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.createMockPasskeyAttestationOptions
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.createMockDisplayItemForCipher
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toActiveAccountSummary
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
import kotlinx.coroutines.flow.MutableSharedFlow
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

    private var mockFilteredCiphers: List<CipherListView>? = null
    private val cipherMatchingManager: CipherMatchingManager = object : CipherMatchingManager {
        // Just do no-op filtering unless we have mock filtered data
        override suspend fun filterCiphersForMatches(
            cipherListViews: List<CipherListView>,
            matchUri: String,
        ): List<CipherListView> = mockFilteredCiphers ?: cipherListViews
    }

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val clipboardManager: BitwardenClipboardManager = mockk {
        every { setText(text = any<String>(), toastDescriptorOverride = any<Text>()) } just runs
    }
    private val toastManager: ToastManager = mockk {
        every { show(messageId = any()) } just runs
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
        coEvery { getCipher(any()) } returns GetCipherResult.Success(createMockCipherView(1))
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
    private val mutableActivePoliciesFlow: MutableStateFlow<List<SyncResponseJson.Policy>> =
        MutableStateFlow(emptyList())
    private val policyManager: PolicyManager = mockk {
        every { getActivePolicies(type = PolicyTypeJson.DISABLE_SEND) } returns emptyList()
        every { getActivePoliciesFlow(type = PolicyTypeJson.DISABLE_SEND) } returns emptyFlow()
        every {
            getActivePoliciesFlow(type = PolicyTypeJson.RESTRICT_ITEM_TYPES)
        } returns mutableActivePoliciesFlow
    }
    private val bitwardenCredentialManager: BitwardenCredentialManager = mockk {
        every { isUserVerified } returns false
        every { isUserVerified = any() } just runs
        every { authenticationAttempts } returns 0
        every { authenticationAttempts = any() } just runs
        every { hasAuthenticationAttemptsRemaining() } returns true
        every {
            getUserVerificationRequirement(any<ProviderGetCredentialRequest>())
        } returns UserVerificationRequirement.PREFERRED
        coEvery { getCredentialEntries(any()) } returns emptyList<CredentialEntry>().asSuccess()
    }
    private val originManager: OriginManager = mockk {
        coEvery {
            validateOrigin(
                relyingPartyId = any(),
                callingAppInfo = any(),
            )
        } returns ValidateOriginResult.Success(null)
    }

    private val organizationEventManager = mockk<OrganizationEventManager> {
        every { trackEvent(event = any()) } just runs
    }

    private val networkConnectionManager: NetworkConnectionManager = mockk {
        every { isNetworkConnected } returns true
    }
    private val privilegedAppRepository = mockk<PrivilegedAppRepository> {
        coEvery { addTrustedPrivilegedApp(any(), any()) } just runs
    }

    private val initialState = createVaultItemListingState()
    private val initialSavedStateHandle
        get() = createSavedStateHandleWithVaultItemListingType(
            vaultItemListingType = VaultItemListingType.Login,
        )
    private val mockCallingAppInfo = mockk<CallingAppInfo> {
        every { packageName } returns "mockPackageName"
        every { isOriginPopulated() } returns false
    }
    private val mockGetPublicKeyCredentialOption = mockk<GetPublicKeyCredentialOption> {
        every { requestJson } returns "mockRequestJson"
    }
    private val mockCreatePublicKeyCredentialOption = mockk<CreatePublicKeyCredentialRequest> {
        every { requestJson } returns "mockRequestJson"
        every { origin } returns "mockOrigin"
    }
    private val mockProviderGetCredentialRequest = mockk<ProviderGetCredentialRequest> {
        every { credentialOptions } returns listOf(mockGetPublicKeyCredentialOption)
        every { callingAppInfo } returns mockCallingAppInfo
    }
    private val mockProviderCreateCredentialRequest = mockk<ProviderCreateCredentialRequest> {
        every { callingRequest } returns mockCreatePublicKeyCredentialOption
        every { callingAppInfo } returns mockCallingAppInfo
    }
    private val mockBeginGetPublicKeyCredentialOption = mockk<BeginGetPublicKeyCredentialOption> {
        every { requestJson } returns "mockRequestJson"
    }
    private val mockBeginGetCredentialRequest = mockk<BeginGetCredentialRequest> {
        every { beginGetCredentialOptions } returns listOf(mockBeginGetPublicKeyCredentialOption)
        every { callingAppInfo } returns mockCallingAppInfo
    }

    private val mutableSnackbarDataFlow: MutableSharedFlow<BitwardenSnackbarData> =
        bufferedMutableSharedFlow()
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay> = mockk {
        every {
            getSnackbarDataFlow(relay = any(), relays = anyVararg())
        } returns mutableSnackbarDataFlow
    }
    private val relyingPartyParser = mockk<RelyingPartyParser> {
        every { parse(any<BeginGetPublicKeyCredentialOption>()) } returns DEFAULT_RELYING_PARTY_ID
        every { parse(any<GetPublicKeyCredentialOption>()) } returns DEFAULT_RELYING_PARTY_ID
        every { parse(any<CreatePublicKeyCredentialRequest>()) } returns DEFAULT_RELYING_PARTY_ID
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(
            SavedStateHandle::toVaultItemListingArgs,
        )
        mockkObject(
            ProviderCreateCredentialRequest.Companion,
            ProviderGetCredentialRequest.Companion,
            BeginGetCredentialRequest.Companion,
        )

        every {
            ProviderGetCredentialRequest.fromBundle(any())
        } returns mockProviderGetCredentialRequest
        every {
            ProviderCreateCredentialRequest.fromBundle(any())
        } returns mockProviderCreateCredentialRequest
        every {
            BeginGetCredentialRequest.fromBundle(any())
        } returns mockBeginGetCredentialRequest
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            SavedStateHandle::toVaultItemListingArgs,
            CallingAppInfo::getSignatureFingerprintAsHexString,
        )
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
    fun `initial dialog state should be correct when CreateCredentialRequest is present`() =
        runTest {
            val createCredentialRequest = createMockCreateCredentialRequest(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = createCredentialRequest,
                )
            coEvery {
                originManager.validateOrigin(any(), any())
            } returns ValidateOriginResult.Success(null)

            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()

            viewModel.stateFlow.test {
                assertEquals(
                    initialState.copy(
                        createCredentialRequest = createCredentialRequest,
                        dialogState = VaultItemListingState.DialogState.Loading(
                            message = BitwardenString.loading.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `RESTRICT_ITEM_TYPES policy changes should update restrictItemTypesPolicyOrgIds accordingly`() =
        runTest {
            val viewModel = createVaultItemListingViewModel()
            assertEquals(
                initialState.copy(restrictItemTypesPolicyOrgIds = persistentListOf()),
                viewModel.stateFlow.value,
            )
            mutableActivePoliciesFlow.emit(
                listOf(
                    createMockPolicy(
                        organizationId = "Test Organization",
                        id = "testId",
                        type = PolicyTypeJson.RESTRICT_ITEM_TYPES,
                        isEnabled = true,
                        data = null,
                    ),
                ),
            )

            assertEquals(
                initialState.copy(
                    restrictItemTypesPolicyOrgIds = persistentListOf("Test Organization"),
                ),
                viewModel.stateFlow.value,
            )
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
        setupFido2CreateRequest()
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
                    message = BitwardenString.syncing.asText(),
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
                    BitwardenString.internet_connection_required_title.asText(),
                    BitwardenString.internet_connection_required_message.asText(),
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
            val cipherListView = createMockCipherListView(number = 1)
            val cipherView = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )

            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.Success(cipherView)

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
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
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
                        type = VaultItemListingState.DisplayItem.ItemType.Vault(
                            type = CipherType.LOGIN,
                        ),
                    ),
                )
                assertEquals(cipherView, awaitItem())
            }
        }

    @Test
    fun `ItemClick for vault item when autofill should post to the AutofillSelectionManager`() =
        runTest {
            setupMockUri()
            val type = CipherListViewType.Login(
                createMockLoginListView(number = 1, hasFido2 = true),
            )
            val cipherListView = createMockCipherListView(
                number = 1,
                type = type,
            )
            val cipherView = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.Success(cipherView)
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
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
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
                        type = VaultItemListingState.DisplayItem.ItemType.Vault(
                            type = CipherType.LOGIN,
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
    fun `ItemClick for vault item during credential registration should show credential error dialog when cipherView is null`() {
        val cipherView = createMockCipherView(number = 1)
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.ProviderCreateCredential(
                createCredentialRequest = createMockCreateCredentialRequest(number = 1),
            )
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = listOf(),
                ),
                folderViewList = emptyList(),
                collectionViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )
        coEvery {
            vaultRepository.getCipher("mockId-1")
        } returns GetCipherResult.CipherNotFound
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(
            VaultItemListingsAction.ItemClick(
                id = cipherView.id.orEmpty(),
                type = VaultItemListingState.DisplayItem.ItemType.Vault(
                    type = CipherType.LOGIN,
                ),
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString
                    .credential_operation_failed_because_the_selected_item_does_not_exist
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
            val cipherListView = createMockCipherListView(number = 1)
            val cipherView = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = createMockCreateCredentialRequest(number = 1),
                )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.Success(cipherView)
            coEvery {
                bitwardenCredentialManager.registerFido2Credential(
                    userId = any(),
                    callingAppInfo = any(),
                    createPublicKeyCredentialRequest = any(),
                    selectedCipherView = any(),
                )
            } returns Fido2RegisterCredentialResult.Success("mockResponse")

            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.ItemClick(
                    id = cipherView.id.orEmpty(),
                    type = VaultItemListingState.DisplayItem.ItemType.Vault(
                        type = CipherType.LOGIN,
                    ),
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
            val cipherListView = createMockCipherListView(number = 1)
            val cipherView = createMockCipherView(number = 1, fido2Credentials = null)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = createMockCreateCredentialRequest(number = 1),
                )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            every {
                bitwardenCredentialManager.getUserVerificationRequirement(
                    request = any<CreatePublicKeyCredentialRequest>(),
                )
            } returns UserVerificationRequirement.REQUIRED
            coEvery {
                bitwardenCredentialManager.registerFido2Credential(
                    userId = any(),
                    callingAppInfo = any(),
                    createPublicKeyCredentialRequest = any(),
                    selectedCipherView = any(),
                )
            } returns Fido2RegisterCredentialResult.Success("mockResponse")

            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.ItemClick(
                    id = cipherView.id.orEmpty(),
                    type = VaultItemListingState.DisplayItem.ItemType.Vault(
                        type = CipherType.LOGIN,
                    ),
                ),
            )

            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingState.DialogState.Loading(BitwardenString.saving.asText()),
                    viewModel.stateFlow.value.dialogState,
                )
                assertEquals(
                    VaultItemListingEvent.CredentialManagerUserVerification(
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
            val cipherListView = createMockCipherListView(number = 1)
            val cipherView = createMockCipherView(number = 1)
            val mockFido2CredentialRequest = createMockCreateCredentialRequest(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = mockFido2CredentialRequest,
                )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            every {
                bitwardenCredentialManager.getUserVerificationRequirement(
                    request = any<CreatePublicKeyCredentialRequest>(),
                )
            } returns UserVerificationRequirement.DISCOURAGED
            every {
                bitwardenCredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns createMockPasskeyAttestationOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.DISCOURAGED,
            )
            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.Success(cipherView)
            coEvery {
                bitwardenCredentialManager.registerFido2Credential(
                    userId = any(),
                    callingAppInfo = any(),
                    createPublicKeyCredentialRequest = any(),
                    selectedCipherView = any(),
                )
            } returns Fido2RegisterCredentialResult.Success("mockResponse")

            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.ItemClick(
                    id = cipherView.id.orEmpty(),
                    type = VaultItemListingState.DisplayItem.ItemType.Vault(
                        type = CipherType.LOGIN,
                    ),
                ),
            )

            coVerify {
                bitwardenCredentialManager.registerFido2Credential(
                    userId = DEFAULT_USER_STATE.activeUserId,
                    createPublicKeyCredentialRequest = any(),
                    selectedCipherView = cipherView,
                    callingAppInfo = any(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ItemClick for vault item during FIDO 2 registration should skip user verification when user is verified`() =
        runTest {
            setupMockUri()
            val cipherListView = createMockCipherListView(number = 1)
            val cipherView = createMockCipherView(number = 1)
            val mockFido2CredentialRequest = createMockCreateCredentialRequest(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = mockFido2CredentialRequest,
                )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            every { bitwardenCredentialManager.isUserVerified } returns true
            coEvery {
                bitwardenCredentialManager.registerFido2Credential(
                    userId = any(),
                    callingAppInfo = any(),
                    createPublicKeyCredentialRequest = any(),
                    selectedCipherView = any(),
                )
            } returns Fido2RegisterCredentialResult.Success("mockResponse")

            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.ItemClick(
                    id = cipherView.id.orEmpty(),
                    type = VaultItemListingState.DisplayItem.ItemType.Vault(
                        type = CipherType.LOGIN,
                    ),
                ),
            )

            coVerify { bitwardenCredentialManager.isUserVerified }
            coVerify(exactly = 1) {
                bitwardenCredentialManager.registerFido2Credential(
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
                VaultItemListingsAction.ItemClick(
                    id = "mock",
                    type = VaultItemListingState.DisplayItem.ItemType.Vault(
                        type = CipherType.LOGIN,
                    ),
                ),
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
    fun `ItemClick for send item should emit NavigateToViewSendItem`() = runTest {
        val viewModel = createVaultItemListingViewModel(
            createSavedStateHandleWithVaultItemListingType(VaultItemListingType.SendFile),
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.ItemClick(
                    id = "mock",
                    type = VaultItemListingState.DisplayItem.ItemType.Sends(type = SendType.FILE),
                ),
            )
            assertEquals(
                VaultItemListingEvent.NavigateToViewSendItem(
                    id = "mock",
                    sendType = SendItemType.FILE,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ItemClick should show alert if ItemType is DecryptionError`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        val itemId = "54321"
        val itemType = VaultItemListingState.DisplayItem.ItemType.DecryptionError

        viewModel.trySendAction(VaultItemListingsAction.ItemClick(itemId, itemType))

        assertEquals(
            createVaultItemListingState().copy(
                dialogState = VaultItemListingState.DialogState.CipherDecryptionError(
                    title = BitwardenString.decryption_error.asText(),
                    message = BitwardenString
                        .bitwarden_could_not_decrypt_this_vault_item_description_long.asText(),
                    selectedCipherId = itemId,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on ShareCipherDecryptionErrorClick should send ShowShareSheet`() = runTest {
        val viewModel = createVaultItemListingViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(
                action = VaultItemListingsAction.ShareCipherDecryptionErrorClick(
                    selectedCipherId = "1",
                ),
            )
            assertEquals(
                VaultItemListingEvent.ShowShareSheet("1"),
                awaitItem(),
            )
        }
    }

    @Test
    fun `MasterPasswordRepromptSubmit for a request Error should show a generic error dialog`() =
        runTest {
            setupMockUri()
            val cipherListView = createMockCipherListView(number = 1)
            val cipherId = "mockId-1"
            val password = "password"
            val error = Throwable("Fail!")
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
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
                        createMockDisplayItemForCipher(
                            number = 1,
                            secondSubtitleTestTag = "PasskeySite",
                            subtitle = "mockSubtitle-1",
                        ),
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
                        message = BitwardenString.generic_error_message.asText(),
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
            val cipherListView = createMockCipherListView(number = 1)
            val cipherId = "mockId-1"
            val password = "password"
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val initialState = createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createMockDisplayItemForCipher(
                            number = 1,
                            secondSubtitleTestTag = "PasskeySite",
                            subtitle = "mockSubtitle-1",
                        ),
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
                        message = BitwardenString.invalid_master_password.asText(),
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
            val cipherListView = createMockCipherListView(number = 1)
            val cipherView = createMockCipherView(number = 1)
            val cipherId = "mockId-1"
            val password = "password"
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
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
            val cipherListView = createMockCipherListView(number = 1)
            val cipherView = createMockCipherView(number = 1)
            val cipherId = "mockId-1"
            val password = "password"
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
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
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.AddTotpLoginItem(
                data = TotpData(
                    uri = "uri",
                    issuer = "issuer",
                    accountName = "Name-1",
                    secret = "secret",
                    digits = 6,
                    period = 30,
                    algorithm = TotpData.CryptoHashAlgorithm.SHA_1,
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)

            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    VaultItemListingsAction.MasterPasswordRepromptSubmit(
                        password = password,
                        masterPasswordRepromptData = MasterPasswordRepromptData.ViewItem(
                            id = cipherId,
                            itemType = VaultItemListingState.DisplayItem.ItemType.Vault(
                                type = CipherType.LOGIN,
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
    fun `AddVaultItemClick inside a collection should show item selection dialog state`() {
        val viewModel = createVaultItemListingViewModel(
            savedStateHandle = createSavedStateHandleWithVaultItemListingType(
                vaultItemListingType = VaultItemListingType.Collection(collectionId = "id"),
            ),
        )
        viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick)
        assertEquals(
            createVaultItemListingState(
                itemListingType = VaultItemListingState.ItemListingType.Vault.Collection(
                    collectionId = "id",
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

    @Suppress("MaxLineLength")
    @Test
    fun `AddVaultItemClick inside a folder should hide card item selection dialog state when RESTRICT_ITEM_TYPES policy is enabled`() =
        runTest {
            val viewModel = createVaultItemListingViewModel(
                savedStateHandle = createSavedStateHandleWithVaultItemListingType(
                    vaultItemListingType = VaultItemListingType.Folder(folderId = "id"),
                ),
            )
            mutableActivePoliciesFlow.emit(
                listOf(
                    createMockPolicy(
                        organizationId = "Test Organization",
                        id = "testId",
                        type = PolicyTypeJson.RESTRICT_ITEM_TYPES,
                        isEnabled = true,
                        data = null,
                    ),
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
                            CreateVaultItemType.CARD,
                            CreateVaultItemType.FOLDER,
                            CreateVaultItemType.SSH_KEY,
                        ),
                    ),
                ).copy(restrictItemTypesPolicyOrgIds = persistentListOf("Test Organization")),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `AddVaultItemClick inside a collection should hide card item selection dialog state when RESTRICT_ITEM_TYPES policy is enabled`() =
        runTest {
            val viewModel = createVaultItemListingViewModel(
                savedStateHandle = createSavedStateHandleWithVaultItemListingType(
                    vaultItemListingType = VaultItemListingType.Collection(collectionId = "id"),
                ),
            )
            mutableActivePoliciesFlow.emit(
                listOf(
                    createMockPolicy(
                        organizationId = "Test Organization",
                        id = "testId",
                        type = PolicyTypeJson.RESTRICT_ITEM_TYPES,
                        isEnabled = true,
                        data = null,
                    ),
                ),
            )
            viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick)
            assertEquals(
                createVaultItemListingState(
                    itemListingType = VaultItemListingState.ItemListingType.Vault.Collection(
                        collectionId = "id",
                    ),
                    dialogState = VaultItemListingState.DialogState.VaultItemTypeSelection(
                        excludedOptions = persistentListOf(
                            CreateVaultItemType.CARD,
                            CreateVaultItemType.FOLDER,
                            CreateVaultItemType.SSH_KEY,
                        ),
                    ),
                ).copy(restrictItemTypesPolicyOrgIds = persistentListOf("Test Organization")),
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
    fun `AddVaultItemClick for text send item should emit NavigateToAddVaultItem`() = runTest {
        val viewModel = createVaultItemListingViewModel(
            createSavedStateHandleWithVaultItemListingType(VaultItemListingType.SendText),
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick)
            assertEquals(
                VaultItemListingEvent.NavigateToAddSendItem(sendType = SendItemType.TEXT),
                awaitItem(),
            )
        }
    }

    @Test
    fun `AddVaultItemClick for file send item with premium should emit NavigateToAddVaultItem`() =
        runTest {
            val viewModel = createVaultItemListingViewModel(
                createSavedStateHandleWithVaultItemListingType(VaultItemListingType.SendFile),
            )
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick)
                assertEquals(
                    VaultItemListingEvent.NavigateToAddSendItem(sendType = SendItemType.FILE),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `AddVaultItemClick for file send item without premium should display error dialog`() =
        runTest {
            mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                accounts = listOf(DEFAULT_ACCOUNT.copy(isPremium = false)),
            )
            val viewModel = createVaultItemListingViewModel(
                createSavedStateHandleWithVaultItemListingType(VaultItemListingType.SendFile),
            )
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick)
                expectNoEvents()
            }
            assertEquals(
                createVaultItemListingState(
                    itemListingType = VaultItemListingState.ItemListingType.Send.SendFile,
                    dialogState = VaultItemListingState.DialogState.Error(
                        title = BitwardenString.send.asText(),
                        message = BitwardenString.send_file_premium_required.asText(),
                    ),
                    isPremium = false,
                ),
                viewModel.stateFlow.value,
            )
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
    fun `OverflowOptionClick Send ViewClick should emit NavigateToViewSendItem`() = runTest {
        val sendId = "sendId"
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.SendAction.ViewClick(
                        sendId = sendId,
                        sendType = SendType.TEXT,
                    ),
                ),
            )
            assertEquals(
                VaultItemListingEvent.NavigateToViewSendItem(
                    id = sendId,
                    sendType = SendItemType.TEXT,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `OverflowOptionClick Send EditClick should emit NavigateToEditSendItem`() = runTest {
        val sendId = "sendId"
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.SendAction.EditClick(
                        sendId = sendId,
                        sendType = SendType.FILE,
                    ),
                ),
            )
            assertEquals(
                VaultItemListingEvent.NavigateToEditSendItem(
                    id = sendId,
                    sendType = SendItemType.FILE,
                ),
                awaitItem(),
            )
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
                    toastDescriptorOverride = BitwardenString.send_link.asText(),
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
                            message = BitwardenString.deleting.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    initialState.copy(
                        dialogState = VaultItemListingState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            throwable = error,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `OverflowOptionClick Send DeleteClick with deleteSend success should emit ShowSnackbar`() =
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
                    VaultItemListingEvent.ShowSnackbar(BitwardenString.send_deleted.asText()),
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
                            message = BitwardenString.removing_send_password.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    initialState.copy(
                        dialogState = VaultItemListingState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            throwable = error,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Send RemovePasswordClick with removePasswordSend success should emit ShowSnackbar`() =
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
                    VaultItemListingEvent.ShowSnackbar(BitwardenString.password_removed.asText()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `OverflowOptionClick Vault CopyNoteClick should call setText on the ClipboardManager`() =
        runTest {
            val notes = "notes"
            val viewModel = createVaultItemListingViewModel()
            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.Success(
                createMockCipherView(
                    number = 1,
                    notes = notes,
                ),
            )
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyNoteClick(
                        requiresPasswordReprompt = false,
                        cipherId = "mockId-1",
                    ),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = notes,
                    toastDescriptorOverride = BitwardenString.notes.asText(),
                )
            }
        }

    @Test
    fun `OverflowOptionClick Vault CopyNumberClick should call setText on the ClipboardManager`() =
        runTest {
            val number = "12345-4321-9876-6789"
            val viewModel = createVaultItemListingViewModel()
            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.Success(
                createMockCipherView(
                    number = 1,
                    cipherType = CipherType.CARD,
                    card = createMockCardView(
                        number = 1,
                        cardNumber = number,
                    ),
                ),
            )
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyNumberClick(
                        cipherId = "mockId-1",
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = number,
                    toastDescriptorOverride = BitwardenString.number.asText(),
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
            coEvery {
                vaultRepository.getCipher(cipherId)
            } returns GetCipherResult.Success(
                createMockCipherView(
                    number = 1,
                    password = password,
                ),
            )
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                        requiresPasswordReprompt = true,
                        cipherId = cipherId,
                    ),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = password,
                    toastDescriptorOverride = BitwardenString.password.asText(),
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
            coEvery {
                vaultRepository.getCipher(cipherId)
            } returns GetCipherResult.Success(
                createMockCipherView(
                    number = 1,
                    cipherType = CipherType.CARD,
                    card = createMockCardView(
                        number = 1,
                        code = securityCode,
                    ),
                ),
            )
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopySecurityCodeClick(
                        cipherId = cipherId,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = securityCode,
                    toastDescriptorOverride = BitwardenString.security_code.asText(),
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
                    ListingItemOverflowAction.VaultAction.CopyTotpClick(
                        cipherId = totpCode,
                        requiresPasswordReprompt = false,
                    ),
                ),
            )

            verify(exactly = 1) {
                clipboardManager.setText(
                    text = code,
                    toastDescriptorOverride = BitwardenString.totp.asText(),
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
                    ListingItemOverflowAction.VaultAction.CopyTotpClick(
                        cipherId = totpCode,
                        requiresPasswordReprompt = false,
                    ),
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
                    toastDescriptorOverride = BitwardenString.username.asText(),
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
                        requiresPasswordReprompt = true,
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
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = listOf(createMockCipherListView(number = 1, isDeleted = false)),
                ),
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
                        createMockDisplayItemForCipher(
                            number = 1,
                            secondSubtitleTestTag = "PasskeySite",
                            subtitle = "mockSubtitle-1",
                        ),
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

            val cipherView1 = createMockCipherListView(
                number = 1,
                type = CipherListViewType.Login(
                    createMockLoginListView(
                        number = 1,
                        hasFido2 = true,
                    ),
                ),
            )
            val cipherView2 = createMockCipherListView(
                number = 2,
                type = CipherListViewType.Login(
                    createMockLoginListView(
                        number = 2,
                        hasFido2 = true,
                    ),
                ),
            )
            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.Success(
                createMockCipherView(
                    number = 1,
                    fido2Credentials = createMockSdkFido2CredentialList(number = 1),
                ),
            )

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
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherView1, cipherView2),
                    ),
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
                                cipherType = CipherType.LOGIN,
                                subtitle = "mockSubtitle-1",
                                secondSubtitleTestTag = "PasskeySite",
                                secondSubtitle = "mockRpId-1",
                                subtitleTestTag = "PasskeyName",
                                iconData = IconData.Network(
                                    uri = "https://icons.bitwarden.net/www.mockuri.com/icon.png",
                                    fallbackIconRes = BitwardenDrawable.ic_bw_passkey,
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
            val cipherView1 = createMockCipherListView(number = 1)
            val cipherView2 = createMockCipherListView(number = 2)

            // Filtering comes later, so we return everything here
            mockFilteredCiphers = listOf(cipherView1, cipherView2)

            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AddTotpLoginItem(data = totpData)
            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherView1, cipherView2),
                    ),
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
                                subtitle = "mockSubtitle-1",
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

            val cipherView1 = createMockCipherListView(
                number = 1,
                type = CipherListViewType.Login(
                    createMockLoginListView(
                        number = 1,
                        hasFido2 = true,
                    ),
                ),
            )
            val cipherView2 = createMockCipherListView(
                number = 2,
                type = CipherListViewType.Login(
                    createMockLoginListView(
                        number = 2,
                        hasFido2 = true,
                    ),
                ),
            )
            coEvery {
                originManager.validateOrigin(any(), any())
            } returns ValidateOriginResult.Success("")

            mockFilteredCiphers = listOf(cipherView1)

            val providerCreateCredentialRequest = CreateCredentialRequest(
                userId = "activeUserId",
                isUserPreVerified = false,
                requestData = bundleOf(),
            )

            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = providerCreateCredentialRequest,
                )
            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherView1, cipherView2),
                    ),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()

            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                createVaultItemListingState(
                    viewState = VaultItemListingState.ViewState.Content(
                        displayCollectionList = emptyList(),
                        displayItemList = listOf(
                            createMockDisplayItemForCipher(
                                number = 1,
                                cipherType = CipherType.LOGIN,
                                subtitle = "mockSubtitle-1",
                                secondSubtitle = "mockRpId-1",
                                secondSubtitleTestTag = "PasskeySite",
                                subtitleTestTag = "PasskeyName",
                                iconData = IconData.Network(
                                    uri = "https://icons.bitwarden.net/www.mockuri.com/icon.png",
                                    fallbackIconRes = BitwardenDrawable.ic_bw_passkey,
                                ),
                                isCredentialCreation = true,
                            ),
                        ),
                        displayFolderList = emptyList(),
                    ),
                )
                    .copy(createCredentialRequest = providerCreateCredentialRequest),
                viewModel.stateFlow.value,
            )
            coVerify {
                originManager.validateOrigin(any(), any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with empty items should update ViewState to NoItems content for Login ItemListingType`() =
        runTest {
            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = emptyList(),
                    ),
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
                        message = BitwardenString.no_logins.asText(),
                        shouldShowAddButton = true,
                        buttonText = BitwardenString.new_login.asText(),
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
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = emptyList(),
                    ),
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
                        message = BitwardenString.no_cards.asText(),
                        shouldShowAddButton = true,
                        buttonText = BitwardenString.new_card.asText(),
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
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = emptyList(),
                    ),
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
                        message = BitwardenString.no_identities.asText(),
                        shouldShowAddButton = true,
                        buttonText = BitwardenString.new_identity.asText(),
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
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = emptyList(),
                    ),
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
                        message = BitwardenString.no_notes.asText(),
                        shouldShowAddButton = true,
                        buttonText = BitwardenString.new_note.asText(),
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
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = emptyList(),
                    ),
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
                        message = BitwardenString.no_file_sends.asText(),
                        shouldShowAddButton = true,
                        buttonText = BitwardenString.new_file_send.asText(),
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
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = emptyList(),
                    ),
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
                        message = BitwardenString.no_text_sends.asText(),
                        shouldShowAddButton = true,
                        buttonText = BitwardenString.new_text_send.asText(),
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
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(createMockCipherListView(number = 1, isDeleted = true)),
                    ),
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
                        message = BitwardenString.no_logins.asText(),
                        shouldShowAddButton = true,
                        buttonText = BitwardenString.new_login.asText(),
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
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(createMockCipherListView(number = 1, isDeleted = false)),
                    ),
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
                        createMockDisplayItemForCipher(
                            number = 1,
                            secondSubtitleTestTag = "PasskeySite",
                            subtitle = "mockSubtitle-1",
                        ),
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
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(createMockCipherListView(number = 1, isDeleted = true)),
                    ),
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
                    message = BitwardenString.no_logins.asText(),
                    shouldShowAddButton = true,
                    buttonText = BitwardenString.new_login.asText(),
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
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(createMockCipherListView(number = 1, isDeleted = true)),
                    ),
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
                    message = BitwardenString.no_logins.asText(),
                    shouldShowAddButton = true,
                    buttonText = BitwardenString.new_login.asText(),
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
                    message = BitwardenString.generic_error_message.asText(),
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
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = listOf(createMockCipherListView(number = 1, isDeleted = false)),
                ),
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
                        createMockDisplayItemForCipher(
                            number = 1,
                            secondSubtitleTestTag = "PasskeySite",
                            subtitle = "mockSubtitle-1",
                        ),
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
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = emptyList(),
                ),
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
                    message = BitwardenString.no_logins.asText(),
                    shouldShowAddButton = true,
                    buttonText = BitwardenString.new_login.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error with trash data should update state to NoItems`() = runTest {
        val dataState = DataState.Error(
            data = VaultData(
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = listOf(createMockCipherListView(number = 1, isDeleted = true)),
                ),
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
                    message = BitwardenString.no_logins.asText(),
                    shouldShowAddButton = true,
                    buttonText = BitwardenString.new_login.asText(),
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
                    message = BitwardenString.internet_connection_required_title
                        .asText()
                        .concat(
                            " ".asText(),
                            BitwardenString.internet_connection_required_message.asText(),
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
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = listOf(createMockCipherListView(number = 1, isDeleted = false)),
                ),
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
                        createMockDisplayItemForCipher(
                            number = 1,
                            secondSubtitleTestTag = "PasskeySite",
                            subtitle = "mockSubtitle-1",
                        ),
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
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = emptyList(),
                ),
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
                    message = BitwardenString.no_logins.asText(),
                    shouldShowAddButton = true,
                    buttonText = BitwardenString.new_login.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow NoNetwork with trash data should update state to NoItems`() = runTest {
        val dataState = DataState.NoNetwork(
            data = VaultData(
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = listOf(createMockCipherListView(number = 1, isDeleted = true)),
                ),
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
                    message = BitwardenString.no_logins.asText(),
                    shouldShowAddButton = true,
                    buttonText = BitwardenString.new_login.asText(),
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
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = listOf(createMockCipherListView(number = 1)),
                ),
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
        setupFido2CreateRequest()
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
                    BitwardenString.internet_connection_required_title.asText(),
                    BitwardenString.internet_connection_required_message.asText(),
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

    //region CredentialManager request handling
    @Test
    fun `CreateCredentialRequest should be evaluated before observing vault data`() {
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.ProviderCreateCredential(
                createMockCreateCredentialRequest(number = 1),
            )
        coEvery {
            originManager.validateOrigin(
                relyingPartyId = DEFAULT_RELYING_PARTY_ID,
                callingAppInfo = mockCallingAppInfo,
            )
        } returns ValidateOriginResult.Success("mockOrigin")

        setupFido2CreateRequest()
        createVaultItemListingViewModel()

        coVerify(ordering = Ordering.ORDERED) {
            originManager.validateOrigin(any(), any())
            vaultRepository.vaultDataStateFlow
        }
    }

    @Test
    fun `ValidateOriginResult should update dialog state on Unknown error`() = runTest {
        val mockCredentialsRequest = createMockCreateCredentialRequest(number = 1)
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.ProviderCreateCredential(
                mockCredentialsRequest,
            )
        coEvery {
            originManager.validateOrigin(
                relyingPartyId = DEFAULT_RELYING_PARTY_ID,
                callingAppInfo = mockCallingAppInfo,
            )
        } returns ValidateOriginResult.Error.Unknown

        setupFido2CreateRequest()
        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                BitwardenString.an_error_has_occurred.asText(),
                BitwardenString.generic_error_message.asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `ValidateOriginResult should show TrustPrivilegedAddPrompt dialog`() =
        runTest {
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = createMockCreateCredentialRequest(number = 1),
                )
            coEvery {
                originManager.validateOrigin(
                    relyingPartyId = DEFAULT_RELYING_PARTY_ID,
                    callingAppInfo = mockCallingAppInfo,
                )
            } returns ValidateOriginResult.Error.PrivilegedAppNotAllowed

            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                VaultItemListingState.DialogState.TrustPrivilegedAddPrompt(
                    message = BitwardenString
                        .passkey_operation_failed_because_browser_x_is_not_trusted
                        .asText("mockPackageName"),
                    selectedCipherId = null,
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ValidateOriginResult should update dialog state on PrivilegedAppSignatureNotFound error`() =
        runTest {
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = createMockCreateCredentialRequest(number = 1),
                )
            coEvery {
                originManager.validateOrigin(
                    relyingPartyId = DEFAULT_RELYING_PARTY_ID,
                    callingAppInfo = mockCallingAppInfo,
                )
            } returns ValidateOriginResult.Error.PrivilegedAppSignatureNotFound

            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    BitwardenString.an_error_has_occurred.asText(),
                    BitwardenString.passkey_operation_failed_because_browser_signature_does_not_match.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ValidateOriginResult should update dialog state on PasskeyNotSupportedForApp error`() =
        runTest {
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = createMockCreateCredentialRequest(number = 1),
                )
            coEvery {
                originManager.validateOrigin(
                    relyingPartyId = DEFAULT_RELYING_PARTY_ID,
                    callingAppInfo = mockCallingAppInfo,
                )
            } returns ValidateOriginResult.Error.PasskeyNotSupportedForApp

            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    BitwardenString.an_error_has_occurred.asText(),
                    BitwardenString.passkeys_not_supported_for_this_app.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ValidateOriginResult should update dialog state on AssetLinkNotFound error`() =
        runTest {
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = createMockCreateCredentialRequest(number = 1),
                )
            coEvery {
                originManager.validateOrigin(
                    relyingPartyId = DEFAULT_RELYING_PARTY_ID,
                    callingAppInfo = mockCallingAppInfo,
                )
            } returns ValidateOriginResult.Error.AssetLinkNotFound

            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    BitwardenString.an_error_has_occurred.asText(),
                    BitwardenString.passkey_operation_failed_because_of_missing_asset_links.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2RegisterCredentialResult Error should show toast and emit CompleteCredentialRegistration result`() =
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
                    VaultItemListingEvent.CompleteCredentialRegistration(
                        CreateCredentialResult.Error(
                            BitwardenString.passkey_registration_failed_due_to_an_internal_error.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
            verify {
                toastManager.show(messageId = BitwardenString.an_error_has_occurred)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2RegisterCredentialResult Success should show toast and emit CompleteCredentialRegistration result`() =
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
                    VaultItemListingEvent.CompleteCredentialRegistration(
                        CreateCredentialResult.Success.Fido2CredentialRegistered(
                            responseJson = "mockResponse",
                        ),
                    ),
                    awaitItem(),
                )
            }
            verify {
                toastManager.show(messageId = BitwardenString.item_updated)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissCredentialManagerErrorDialogClick should clear the dialog state then complete FIDO 2 registration based on state`() =
        runTest {
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createMockCreateCredentialRequest(number = 1),
                )
            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.DismissCredentialManagerErrorDialogClick(
                    "".asText(),
                ),
            )
            viewModel.eventFlow.test {
                assertNull(viewModel.stateFlow.value.dialogState)
                assertEquals(
                    VaultItemListingEvent.CompleteCredentialRegistration(
                        result = CreateCredentialResult.Error(
                            "".asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissCredentialManagerErrorDialogClick should clear dialog state then complete FIDO 2 assertion with error when assertion request is not null`() =
        runTest {
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                createMockFido2CredentialAssertionRequest(number = 1),
            )
            every {
                vaultRepository
                    .decryptCipherListResultStateFlow
                    .value
                    .data
                    ?.successes
            } returns listOf(createMockCipherListView(number = 1))
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.DismissCredentialManagerErrorDialogClick("".asText()),
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
    fun `DismissCredentialManagerErrorDialogClick should show general error dialog when no FIDO 2 or ProviderGetPasswordRequest request is present`() =
        runTest {
            specialCircumstanceManager.specialCircumstance = null
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.DismissCredentialManagerErrorDialogClick("".asText()),
            )
            assertEquals(
                VaultItemListingState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = "".asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissCredentialManagerErrorDialogClick should clear dialog state then complete ProviderGetPasswordRequest with error when request is not null`() =
        runTest {
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderGetPasswordRequest(
                    createMockProviderGetPasswordCredentialRequest(1),
                )
            val mockPasswordCredential = createMockLoginListView(1)
            every {
                vaultRepository.decryptCipherListResultStateFlow.value.data
            } returns createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(
                    createMockCipherListView(
                        number = 1,
                        type = CipherListViewType.Login(mockPasswordCredential),
                    ),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.DismissCredentialManagerErrorDialogClick("".asText()),
            )
            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.CompleteProviderGetPasswordCredentialRequest(
                        result = GetPasswordCredentialResult.Error("".asText()),
                    ),
                    awaitItem(),
                )
                assertNull(viewModel.stateFlow.value.dialogState)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `GetCredentialsRequest should validate request and emit CompleteProviderGetCredentialsRequest event`() =
        runTest {
            setupMockUri()
            val mockGetCredentialsRequest = createMockGetCredentialsRequest(number = 1)
            val mockCipherView = createMockCipherListView(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderGetCredentials(
                    mockGetCredentialsRequest,
                )
            coEvery {
                bitwardenCredentialManager.getCredentialEntries(any())
            } returns emptyList<PublicKeyCredentialEntry>().asSuccess()
            coEvery {
                originManager.validateOrigin(relyingPartyId = any(), callingAppInfo = any())
            } returns ValidateOriginResult.Success("mockOrigin")
            every {
                vaultRepository
                    .decryptCipherListResultStateFlow
                    .value
                    .data
                    ?.successes
            } returns listOf(mockCipherView)

            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(mockCipherView),
                    ),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.CompleteProviderGetCredentialsRequest(
                        result = GetCredentialsResult.Success(
                            credentialEntries = emptyList(),
                            userId = "mockUserId-1",
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `GetCredentialsRequest should emit GetCredentialEntriesResultReceive when result is received`() =
        runTest {
            setupMockUri()
            val mockGetCredentialsRequest = createMockGetCredentialsRequest(number = 1)
            val mockCipherView = createMockCipherListView(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderGetCredentials(
                    mockGetCredentialsRequest,
                )

            every {
                vaultRepository
                    .decryptCipherListResultStateFlow
                    .value
                    .data
                    ?.successes
            } returns listOf(mockCipherView)
            every {
                mockBeginGetCredentialRequest.beginGetCredentialOptions
            } returns emptyList()
            coEvery {
                bitwardenCredentialManager.getCredentialEntries(any())
            } returns GetCredentialUnknownException("Internal error").asFailure()

            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(mockCipherView),
                    ),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.generic_error_message.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `GetCredentialsRequest should display error dialog when callingApp cannot be verified`() =
        runTest {
            setupMockUri()
            val mockGetCredentialsRequest = createMockGetCredentialsRequest(number = 1)
            val mockCipherView = createMockCipherListView(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderGetCredentials(
                    mockGetCredentialsRequest,
                )

            every {
                vaultRepository
                    .decryptCipherListResultStateFlow
                    .value
                    .data
                    ?.successes
            } returns listOf(mockCipherView)
            every {
                mockBeginGetCredentialRequest.callingAppInfo
            } returns null

            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(mockCipherView),
                    ),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message =
                        BitwardenString.passkey_operation_failed_because_app_could_not_be_verified
                            .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Test
    fun `GetCredentialsRequest should display error dialog when origin validation fails`() =
        runTest {
            setupMockUri()
            val mockGetCredentialsRequest = createMockGetCredentialsRequest(number = 1)
            val mockCipherView = createMockCipherListView(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderGetCredentials(
                    mockGetCredentialsRequest,
                )
            coEvery {
                originManager.validateOrigin(relyingPartyId = any(), callingAppInfo = any())
            } returns ValidateOriginResult.Error.Unknown

            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(mockCipherView),
                    ),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.generic_error_message.asText(),
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
            val mockCipherListView = createMockCipherListView(number = 1)
            val mockCipherView = createMockCipherView(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every {
                bitwardenCredentialManager.getUserVerificationRequirement(
                    any<ProviderGetCredentialRequest>(),
                )
            } returns UserVerificationRequirement.REQUIRED
            every {
                vaultRepository
                    .decryptCipherListResultStateFlow
                    .value
                    .data
                    ?.successes
            } returns listOf(
                createMockCipherListView(number = 1),
            )

            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(mockCipherListView),
                    ),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingState.DialogState.Loading(BitwardenString.loading.asText()),
                    viewModel.stateFlow.value.dialogState,
                )
                verify { bitwardenCredentialManager.isUserVerified }
                assertEquals(
                    VaultItemListingEvent.CredentialManagerUserVerification(
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
            val mockCipherListView = createMockCipherListView(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every {
                bitwardenCredentialManager.getUserVerificationRequirement(
                    any<ProviderGetCredentialRequest>(),
                )
            } returns UserVerificationRequirement.PREFERRED
            every {
                vaultRepository
                    .decryptCipherListResultStateFlow
                    .value
                    .data
                    ?.successes
            } returns listOf(mockCipherListView)
            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.Success(mockCipherView)

            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(mockCipherListView),
                    ),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState
            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingState.DialogState.Loading(BitwardenString.loading.asText()),
                    viewModel.stateFlow.value.dialogState,
                )
                verify { bitwardenCredentialManager.isUserVerified }
                assertEquals(
                    VaultItemListingEvent.CredentialManagerUserVerification(
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
            val mockCipherListView = createMockCipherListView(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every { authRepository.activeUserId } returns "activeUserId"
            every {
                bitwardenCredentialManager.getUserVerificationRequirement(
                    any<ProviderGetCredentialRequest>(),
                )
            } returns UserVerificationRequirement.DISCOURAGED
            every {
                vaultRepository
                    .decryptCipherListResultStateFlow
                    .value
                    .data
                    ?.successes
            } returns listOf(
                createMockCipherListView(number = 1),
            )
            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.Success(mockCipherView)
            coEvery {
                bitwardenCredentialManager.authenticateFido2Credential(
                    userId = "activeUserId",
                    request = any(),
                    selectedCipherView = mockCipherView,
                    callingAppInfo = any(),
                    origin = null,
                )
            } returns Fido2CredentialAssertionResult.Success(responseJson = "responseJson")

            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(mockCipherListView),
                    ),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            coVerify {
                bitwardenCredentialManager.isUserVerified
                bitwardenCredentialManager.authenticateFido2Credential(
                    userId = "activeUserId",
                    request = any(),
                    selectedCipherView = mockCipherView,
                    callingAppInfo = any(),
                    origin = null,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2Assertion should show error dialog when relying party cannot be identified`() =
        runTest {
            setupMockUri()
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = "mockId-1")
            val mockCipherListView = createMockCipherListView(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every { bitwardenCredentialManager.isUserVerified } returns true
            every {
                vaultRepository
                    .decryptCipherListResultStateFlow
                    .value
                    .data
                    ?.successes
            } returns listOf(mockCipherListView)
            every {
                relyingPartyParser.parse(mockGetPublicKeyCredentialOption)
            } returns null

            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(mockCipherListView),
                    ),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            coVerify(exactly = 0) {
                originManager.validateOrigin(any(), any())
            }
            viewModel.stateFlow.test {
                assertEquals(
                    VaultItemListingState.DialogState.CredentialManagerOperationFail(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString
                            .passkey_operation_failed_because_relying_party_cannot_be_identified
                            .asText(),
                    ),
                    awaitItem().dialogState,
                )
            }
        }

    @Test
    fun `Fido2AssertionRequest should show error dialog when validateOrigin is not Success`() =
        runTest {
            setupMockUri()
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = "mockId-1")
            val mockCipherListView = createMockCipherListView(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every { bitwardenCredentialManager.isUserVerified } returns true
            every {
                vaultRepository
                    .decryptCipherListResultStateFlow
                    .value
                    .data
                    ?.successes
            } returns listOf(mockCipherListView)
            coEvery {
                originManager.validateOrigin(any(), any())
            } returns ValidateOriginResult.Error.Unknown

            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(mockCipherListView),
                    ),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            viewModel.stateFlow.test {
                assertEquals(
                    VaultItemListingState.DialogState.CredentialManagerOperationFail(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                    awaitItem().dialogState,
                )
            }
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
        val mockCipherListView = createMockCipherListView(number = 1)
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
            mockAssertionRequest,
        )
        every { bitwardenCredentialManager.isUserVerified } returns true
        every {
            bitwardenCredentialManager.getUserVerificationRequirement(
                any<ProviderGetCredentialRequest>(),
            )
        } returns UserVerificationRequirement.PREFERRED
        coEvery {
            vaultRepository.getCipher("mockId-1")
        } returns GetCipherResult.Success(mockCipherView)
        coEvery {
            bitwardenCredentialManager.authenticateFido2Credential(
                userId = "activeUserId",
                request = any(),
                selectedCipherView = mockCipherView,
                callingAppInfo = any(),
                origin = null,
            )
        } returns Fido2CredentialAssertionResult.Success(responseJson = "responseJson")

        every {
            vaultRepository
                .decryptCipherListResultStateFlow
                .value
                .data
                ?.successes
        } returns listOf(mockCipherListView)
        every { authRepository.activeUserId } returns "activeUserId"

        val dataState = DataState.Loaded(
            data = VaultData(
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = listOf(mockCipherListView),
                ),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )
        createVaultItemListingViewModel()
        mutableVaultDataStateFlow.value = dataState

        coVerify {
            bitwardenCredentialManager.isUserVerified
            bitwardenCredentialManager.authenticateFido2Credential(
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
        val mockCipherListView = createMockCipherListView(number = 1)
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
            mockAssertionRequest,
        )
        every { bitwardenCredentialManager.isUserVerified } returns true
        every {
            vaultRepository
                .decryptCipherListResultStateFlow
                .value
                .data
                ?.successes
        } returns listOf(mockCipherListView)
        every { authRepository.activeUserId } returns null

        val dataState = DataState.Loaded(
            data = VaultData(
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = listOf(mockCipherListView),
                ),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )
        val viewModel = createVaultItemListingViewModel()
        mutableVaultDataStateFlow.value = dataState

        coVerify(exactly = 0) {
            bitwardenCredentialManager.authenticateFido2Credential(
                userId = any(),
                request = any(),
                selectedCipherView = any(),
                callingAppInfo = any(),
                origin = null,
            )
        }

        assertEquals(
            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString
                    .passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2AssertionRequest should prompt for master password when passkey is protected and user has master password`() {
        setupMockUri()
        val mockAssertionRequest = createMockFido2CredentialAssertionRequest(
            number = 1,
            cipherId = "mockId-1",
        )
        val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
        val mockCipherView = createMockCipherView(
            number = 1,
            fido2Credentials = mockFido2CredentialList,
            repromptType = CipherRepromptType.PASSWORD,
        )
        val mockCipherListView = createMockCipherListView(
            number = 1,
            reprompt = CipherRepromptType.PASSWORD,
            type = CipherListViewType.Login(
                createMockLoginListView(number = 1, hasFido2 = true),
            ),
        )
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
            mockAssertionRequest,
        )
        every { bitwardenCredentialManager.isUserVerified } returns true
        every {
            vaultRepository
                .decryptCipherListResultStateFlow
                .value
                .data
                ?.successes
        } returns listOf(mockCipherListView)
        coEvery {
            vaultRepository.getCipher("mockId-1")
        } returns GetCipherResult.Success(mockCipherView)
        every { authRepository.activeUserId } returns null

        val dataState = DataState.Loaded(
            data = VaultData(
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = listOf(mockCipherListView),
                ),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )
        val viewModel = createVaultItemListingViewModel()
        mutableVaultDataStateFlow.value = dataState

        assertEquals(
            VaultItemListingState.DialogState.UserVerificationMasterPasswordPrompt(
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
            val mockCipherListView = createMockCipherListView(
                number = 1,
                reprompt = CipherRepromptType.PASSWORD,
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
            every { bitwardenCredentialManager.isUserVerified } returns true
            every {
                vaultRepository
                    .decryptCipherListResultStateFlow
                    .value
                    .data
                    ?.successes
            } returns listOf(mockCipherListView)
            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.Success(mockCipherView)
            coEvery {
                bitwardenCredentialManager.authenticateFido2Credential(
                    DEFAULT_USER_STATE.activeUserId,
                    request = any(),
                    selectedCipherView = mockCipherView,
                    callingAppInfo = any(),
                    origin = null,
                )
            } returns Fido2CredentialAssertionResult.Success("responseJson")

            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(mockCipherListView),
                    ),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            coVerify {
                bitwardenCredentialManager.authenticateFido2Credential(
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
    fun `UserVerificationLockout should display CredentialManagerOperationFail and set isUserVerified to false`() {
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(VaultItemListingsAction.UserVerificationLockOut)

        verify { bitwardenCredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString
                    .credential_operation_failed_because_user_is_locked_out
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationCancelled should clear dialog state, set isUserVerified to false, and emit CompleteCredentialRegistration with cancelled result`() =
        runTest {
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createMockCreateCredentialRequest(number = 1),
                )
            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(VaultItemListingsAction.UserVerificationCancelled)

            verify { bitwardenCredentialManager.isUserVerified = false }
            assertNull(viewModel.stateFlow.value.dialogState)
            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.CompleteCredentialRegistration(
                        result = CreateCredentialResult.Cancelled,
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

            verify { bitwardenCredentialManager.isUserVerified = false }
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

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationFail should display CredentialManagerOperationFail and set isUserVerified to false`() {
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(VaultItemListingsAction.UserVerificationFail)

        verify { bitwardenCredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString
                    .credential_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationSuccess should display CredentialManagerOperationFail when SpecialCircumstance is null`() =
        runTest {
            specialCircumstanceManager.specialCircumstance = null
            coEvery {
                bitwardenCredentialManager.registerFido2Credential(
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
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString
                        .passkey_operation_failed_because_the_request_is_invalid
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationSuccess should display CredentialManagerOperationFail when SpecialCircumstance is invalid`() =
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
                bitwardenCredentialManager.registerFido2Credential(
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
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString
                        .passkey_operation_failed_because_the_request_is_invalid
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationSuccess should display CredentialManagerOperationFail when activeUserId is null`() {
        every { authRepository.activeUserId } returns null
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.ProviderCreateCredential(
                createMockCreateCredentialRequest(
                    number = 1,
                ),
            )

        setupFido2CreateRequest()
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationSuccess(
                selectedCipherView = createMockCipherView(number = 1),
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString
                    .passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationSuccess should set isUserVerified to true, and register FIDO 2 credential when verification result is received`() =
        runTest {
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = createMockCreateCredentialRequest(number = 1),
                )
            coEvery {
                bitwardenCredentialManager.registerFido2Credential(
                    userId = any(),
                    callingAppInfo = any(),
                    createPublicKeyCredentialRequest = any(),
                    selectedCipherView = any(),
                )
            } returns Fido2RegisterCredentialResult.Success(
                responseJson = "mockResponse",
            )

            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationSuccess(
                    selectedCipherView = createMockCipherView(number = 1),
                ),
            )

            coVerify {
                bitwardenCredentialManager.isUserVerified = true
                bitwardenCredentialManager.registerFido2Credential(
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
                    .decryptCipherListResultStateFlow
                    .value
                    .data
                    ?.successes
            } returns listOf(
                createMockCipherListView(number = 1),
            )
            coEvery {
                bitwardenCredentialManager.authenticateFido2Credential(
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
                bitwardenCredentialManager.isUserVerified = true
                bitwardenCredentialManager.authenticateFido2Credential(
                    userId = DEFAULT_ACCOUNT.userId,
                    request = any(),
                    selectedCipherView = any(),
                    callingAppInfo = any(),
                    origin = null,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationNotSupported should display CredentialManagerOperationFail when no cipher id found`() {
        val viewModel = createVaultItemListingViewModel()

        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationNotSupported(
                selectedCipherId = null,
            ),
        )

        verify { bitwardenCredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString
                    .credential_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationNotSupported should display CredentialManagerOperationFail when no active account found`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        mutableUserStateFlow.value = null

        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationNotSupported(
                selectedCipherId = selectedCipherId,
            ),
        )

        verify { bitwardenCredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString
                    .credential_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationNotSupported should display UserVerificationPinPrompt when user has pin`() {
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

        verify { bitwardenCredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.UserVerificationPinPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationNotSupported should display UserVerificationMasterPasswordPrompt when user has password but no pin`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"

        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationNotSupported(
                selectedCipherId = selectedCipherId,
            ),
        )

        verify { bitwardenCredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.UserVerificationMasterPasswordPrompt(
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

        verify { bitwardenCredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.UserVerificationPinSetUpPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordUserVerificationSubmit should display CredentialManagerOperationFail when password verification fails`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val password = "password"
        coEvery {
            authRepository.validatePassword(password = password)
        } returns ValidatePasswordResult.Error(error = Throwable("Fail!"))

        viewModel.trySendAction(
            VaultItemListingsAction.MasterPasswordUserVerificationSubmit(
                password = password,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString
                    .credential_operation_failed_because_user_could_not_be_verified
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
    fun `MasterPasswordUserVerificationSubmit should display Fido2MasterPasswordError when user has retries remaining`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val password = "password"
        coEvery {
            authRepository.validatePassword(password = password)
        } returns ValidatePasswordResult.Success(isValid = false)

        viewModel.trySendAction(
            VaultItemListingsAction.MasterPasswordUserVerificationSubmit(
                password = password,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.UserVerificationMasterPasswordError(
                title = null,
                message = BitwardenString.invalid_master_password.asText(),
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
    fun `MasterPasswordUserVerificationSubmit should display CredentialManagerOperationFail when user has no retries remaining`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val password = "password"
        every { bitwardenCredentialManager.hasAuthenticationAttemptsRemaining() } returns false
        coEvery {
            authRepository.validatePassword(password = password)
        } returns ValidatePasswordResult.Success(isValid = false)

        viewModel.trySendAction(
            VaultItemListingsAction.MasterPasswordUserVerificationSubmit(
                password = password,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString
                    .credential_operation_failed_because_user_verification_attempts_exceeded
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
    fun `MasterPasswordUserVerificationSubmit should display CredentialManagerOperationFail when cipher not found`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val password = "password"
        coEvery {
            authRepository.validatePassword(password = password)
        } returns ValidatePasswordResult.Success(isValid = true)
        coEvery {
            vaultRepository.getCipher("selectedCipherId")
        } returns GetCipherResult.CipherNotFound

        viewModel.trySendAction(
            VaultItemListingsAction.MasterPasswordUserVerificationSubmit(
                password = password,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString
                    .credential_operation_failed_because_the_selected_item_does_not_exist
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
    fun `MasterPasswordUserVerificationSubmit should register credential when password authenticated successfully`() =
        runTest {
            val viewModel = createVaultItemListingViewModel()
            val cipherListView = createMockCipherListView(number = 1)
            val selectedCipherId = cipherListView.id ?: ""
            val password = "password"
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
                    collectionViewList = emptyList(),
                    folderViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)

            viewModel.trySendAction(
                VaultItemListingsAction.MasterPasswordUserVerificationSubmit(
                    password = password,
                    selectedCipherId = selectedCipherId,
                ),
            )
            coVerify {
                authRepository.validatePassword(password = password)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `RetryUserVerificationPasswordVerificationClick should display UserVerificationMasterPasswordPrompt`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"

        viewModel.trySendAction(
            VaultItemListingsAction.RetryUserVerificationPasswordVerificationClick(
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.UserVerificationMasterPasswordPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PinUserVerificationSubmit should display CredentialManagerOperationFail when pin verification fails`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val pin = "PIN"
        coEvery {
            authRepository.validatePinUserKey(pin = pin)
        } returns ValidatePinResult.Error(error = Throwable("Fail!"))

        viewModel.trySendAction(
            VaultItemListingsAction.PinUserVerificationSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString
                    .credential_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePinUserKey(pin = pin)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PinUserVerificationSubmit should display UserVerificationPinError when user has retries remaining`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val pin = "PIN"
        coEvery {
            authRepository.validatePinUserKey(pin = pin)
        } returns ValidatePinResult.Success(isValid = false)

        viewModel.trySendAction(
            VaultItemListingsAction.PinUserVerificationSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.UserVerificationPinError(
                title = null,
                message = BitwardenString.invalid_pin.asText(),
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePinUserKey(pin = pin)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PinUserVerificationSubmit should display CredentialManagerOperationFail when user has no retries remaining`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val pin = "PIN"
        every { bitwardenCredentialManager.hasAuthenticationAttemptsRemaining() } returns false
        coEvery {
            authRepository.validatePinUserKey(pin = pin)
        } returns ValidatePinResult.Success(isValid = false)

        viewModel.trySendAction(
            VaultItemListingsAction.PinUserVerificationSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString
                    .credential_operation_failed_because_user_verification_attempts_exceeded
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePinUserKey(pin = pin)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PinUserVerificationSubmit should display CredentialManagerOperationFail when cipher not found`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val pin = "PIN"
        coEvery {
            authRepository.validatePinUserKey(pin = pin)
        } returns ValidatePinResult.Success(isValid = true)
        coEvery {
            vaultRepository.getCipher("selectedCipherId")
        } returns GetCipherResult.CipherNotFound

        viewModel.trySendAction(
            VaultItemListingsAction.PinUserVerificationSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString
                    .credential_operation_failed_because_the_selected_item_does_not_exist
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePinUserKey(pin = pin)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PinUserVerificationSubmit should register credential when pin authenticated successfully`() {
        setupMockUri()
        val viewModel = createVaultItemListingViewModel()
        val cipherListView = createMockCipherListView(number = 1)
        val selectedCipherId = cipherListView.id ?: ""
        val pin = "PIN"
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = listOf(cipherListView),
                ),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )
        coEvery {
            authRepository.validatePinUserKey(pin = pin)
        } returns ValidatePinResult.Success(isValid = true)

        viewModel.trySendAction(
            VaultItemListingsAction.PinUserVerificationSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )
        coVerify {
            authRepository.validatePinUserKey(pin = pin)
        }
    }

    @Test
    fun `RetryUserVerificationPinVerificationClick should display FidoPinPrompt`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"

        viewModel.trySendAction(
            VaultItemListingsAction.RetryUserVerificationPinVerificationClick(
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.UserVerificationPinPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `UserVerificationPinSetUpSubmit should display Fido2PinSetUpError for empty PIN`() {
        val viewModel = createVaultItemListingViewModel()
        val pin = ""
        val selectedCipherId = "selectedCipherId"

        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationPinSetUpSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.UserVerificationPinSetUpError(
                title = null,
                message = BitwardenString.validation_field_required
                    .asText(BitwardenString.pin.asText()),
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationPinSetUpSubmit should save PIN and register credential for non-empty PIN`() {
        setupMockUri()
        val viewModel = createVaultItemListingViewModel()
        val cipherListView = createMockCipherListView(number = 1)
        val pin = "PIN"
        val selectedCipherId = "selectedCipherId"
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = listOf(cipherListView),
                ),
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
            VaultItemListingsAction.UserVerificationPinSetUpSubmit(
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
    fun `UserVerificationPinSetUpRetryClick should display Fido2PinSetUpPrompt`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"

        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationPinSetUpRetryClick(
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.UserVerificationPinSetUpPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `DismissUserVerificationDialogClick should display CredentialManagerOperationFail`() {
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(
            VaultItemListingsAction.DismissUserVerificationDialogClick,
        )

        assertEquals(
            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                title = BitwardenString.an_error_has_occurred.asText(),
                message =
                    BitwardenString
                        .credential_operation_failed_because_user_verification_was_cancelled
                        .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `ConfirmOverwriteExistingPasskeyClick should check if user is verified`() =
        runTest {
            setupMockUri()
            val cipherListView = createMockCipherListView(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createMockCreateCredentialRequest(number = 1),
                )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            coEvery {
                bitwardenCredentialManager.getUserVerificationRequirement(
                    request = any<CreatePublicKeyCredentialRequest>(),
                )
            } returns UserVerificationRequirement.REQUIRED

            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.ConfirmOverwriteExistingPasskeyClick(
                    cipherViewId = cipherListView.id!!,
                ),
            )

            verify { bitwardenCredentialManager.isUserVerified }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmOverwriteExistingPasskeyClick should display CredentialManagerOperationFail when getCipher returns null`() =
        runTest {
            setupMockUri()
            val cipherListView = createMockCipherListView(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = createMockCreateCredentialRequest(number = 1),
                )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            coEvery {
                vaultRepository.getCipher("invalidId")
            } returns GetCipherResult.CipherNotFound
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.ConfirmOverwriteExistingPasskeyClick(
                    cipherViewId = "invalidId",
                ),
            )

            assertEquals(
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    BitwardenString.an_error_has_occurred.asText(),
                    BitwardenString
                        .credential_operation_failed_because_the_selected_item_does_not_exist
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissCredentialManagerErrorDialogClick should clear dialog state then complete GetPassword Request with error when password request is not null`() =
        runTest {
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderGetPasswordRequest(
                    createMockProviderGetPasswordCredentialRequest(1),
                )

            every {
                vaultRepository
                    .decryptCipherListResultStateFlow
                    .value
                    .data
            } returns createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(
                    createMockCipherListView(
                        number = 1,
                    ),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.DismissCredentialManagerErrorDialogClick("".asText()),
            )
            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.CompleteProviderGetPasswordCredentialRequest(
                        result = GetPasswordCredentialResult.Error("".asText()),
                    ),
                    awaitItem(),
                )
                assertNull(viewModel.stateFlow.value.dialogState)
            }
        }

    @Test
    fun `GetPasswordRequest should show error dialog when cipher state flow data is null`() =
        runTest {
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderGetPasswordRequest(
                    createMockProviderGetPasswordCredentialRequest(1),
                )
            every {
                vaultRepository
                    .decryptCipherListResultStateFlow
                    .value
                    .data
            } returns null

            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = emptyList(),
                    ),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString
                        .credential_operation_failed_because_the_selected_item_does_not_exist
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
            coVerify(exactly = 0) { vaultRepository.getCipher(any()) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `GetPasswordRequest should show error dialog when cipher state flow data has no matching cipher`() =
        runTest {
            setupMockUri()
            val mockGetPasswordRequest = createMockProviderGetPasswordCredentialRequest(1)
            val mockCipherListView = createMockCipherListView(
                number = 1,
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderGetPasswordRequest(
                    mockGetPasswordRequest,
                )
            every {
                vaultRepository
                    .decryptCipherListResultStateFlow
                    .value
                    .data
            } returns createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(mockCipherListView),
            )
            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.CipherNotFound

            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(mockCipherListView),
                    ),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            mutableVaultDataStateFlow.value = dataState
            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString
                        .credential_operation_failed_because_the_selected_item_does_not_exist
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `PasswordGetCredentialRequest should prompt for master password when password is protected and user has master password`() {
        setupMockUri()
        val mockGetPasswordRequest = createMockProviderGetPasswordCredentialRequest(1)
        val mockCipherListView = createMockCipherListView(
            number = 1,
            reprompt = CipherRepromptType.PASSWORD,
        )
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.ProviderGetPasswordRequest(
                mockGetPasswordRequest,
            )
        every { bitwardenCredentialManager.isUserVerified } returns true
        every {
            vaultRepository
                .decryptCipherListResultStateFlow
                .value
                .data
        } returns createMockDecryptCipherListResult(
            number = 1,
            successes = listOf(mockCipherListView),
        )
        coEvery {
            vaultRepository.getCipher("mockId-1")
        } returns GetCipherResult.Success(
            createMockCipherView(
                number = 1,
                repromptType = CipherRepromptType.PASSWORD,
            ),
        )
        every { authRepository.activeUserId } returns null

        val dataState = DataState.Loaded(
            data = VaultData(
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = listOf(mockCipherListView),
                ),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )
        val viewModel = createVaultItemListingViewModel()
        mutableVaultDataStateFlow.value = dataState

        assertEquals(
            VaultItemListingState.DialogState.UserVerificationMasterPasswordPrompt(
                selectedCipherId = mockGetPasswordRequest.cipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationSuccess should set isUserVerified to true, and return password credential when verification result is received`() =
        runTest {
            val mockLoginListView = createMockLoginListView(
                number = 1,
                totp = "mockTotp-1",
            )
            val mockLoginView = createMockLoginView(
                number = 1,
                totp = "mockTotp-1",
                clock = clock,
                password = "mockPassword-1",
                fido2Credentials = null,
            )
            val mockGetPasswordRequest = createMockProviderGetPasswordCredentialRequest(1)
                .copy(cipherId = "mockId-1")
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderGetPasswordRequest(
                    passwordGetRequest = mockGetPasswordRequest,
                )
            every {
                ProviderGetCredentialRequest.fromBundle(any())
            } returns mockk(relaxed = true) {
                every {
                    credentialOptions
                } returns listOf(
                    mockk<GetPasswordOption>(relaxed = true),
                )
            }
            every {
                vaultRepository
                    .decryptCipherListResultStateFlow
                    .value
                    .data
            } returns createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(
                    createMockCipherListView(
                        number = 1,
                        type = CipherListViewType.Login(mockLoginListView),
                    ),
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationSuccess(
                    selectedCipherView = createMockCipherView(number = 1),
                ),
            )

            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.CompleteProviderGetPasswordCredentialRequest(
                        result = GetPasswordCredentialResult.Success(mockLoginView),
                    ),
                    awaitItem(),
                )
                assertNull(viewModel.stateFlow.value.dialogState)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationCancelled should clear dialog state, set isUserVerified to false, and emit CompleteGetPassword with cancelled result`() =
        runTest {
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderGetPasswordRequest(
                    createMockProviderGetPasswordCredentialRequest(1),
                )
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(VaultItemListingsAction.UserVerificationCancelled)

            verify { bitwardenCredentialManager.isUserVerified = false }
            assertNull(viewModel.stateFlow.value.dialogState)
            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.CompleteProviderGetPasswordCredentialRequest(
                        result = GetPasswordCredentialResult.Cancelled,
                    ),
                    awaitItem(),
                )
            }
        }

    //endregion CredentialManager request handling

    @Test
    fun `InternetConnectionErrorReceived should show network error if no internet connection`() =
        runTest {
            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.Internal.InternetConnectionErrorReceived,
            )
            assertEquals(
                initialState.copy(
                    isRefreshing = false,
                    dialogState = VaultItemListingState.DialogState.Error(
                        BitwardenString.internet_connection_required_title.asText(),
                        BitwardenString.internet_connection_required_message.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `TrustPrivilegedAppClick during CreateCredentialRequest should clear dialog, trust privileged app, and wait`() =
        runTest {
            mockkStatic(CallingAppInfo::getSignatureFingerprintAsHexString)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = createMockCreateCredentialRequest(number = 1),
                )

            every {
                mockCallingAppInfo.getSignatureFingerprintAsHexString()
            } returns "mockSignature"
            coEvery {
                originManager.validateOrigin(any(), any())
            } returns ValidateOriginResult.Error.PrivilegedAppNotAllowed

            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()

            viewModel.trySendAction(
                VaultItemListingsAction.TrustPrivilegedAppClick(selectedCipherId = null),
            )

            // Verify the dialog is cleared
            assertNull(viewModel.stateFlow.value.dialogState)

            coVerify {
                privilegedAppRepository.addTrustedPrivilegedApp(
                    packageName = "mockPackageName",
                    signature = "mockSignature",
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `TrustPrivilegedAppClick during CreateCredentialRequest should show CredentialManagerOperationFail dialog when signature is invalid`() =
        runTest {
            mockkStatic(CallingAppInfo::getSignatureFingerprintAsHexString)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = createMockCreateCredentialRequest(number = 1),
                )

            every {
                mockCallingAppInfo.getSignatureFingerprintAsHexString()
            } returns null

            setupFido2CreateRequest()
            val viewModel = createVaultItemListingViewModel()

            viewModel.trySendAction(
                VaultItemListingsAction.TrustPrivilegedAppClick(selectedCipherId = null),
            )

            assertEquals(
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString
                        .passkey_operation_failed_because_the_request_is_invalid
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )

            coVerify(exactly = 0) {
                privilegedAppRepository.addTrustedPrivilegedApp(
                    packageName = any(),
                    signature = any(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `TrustPrivilegedAppClick during BeginGetCredentials should clear dialog, trust privileged app, and get credential entries`() =
        runTest {
            mockkStatic(CallingAppInfo::getSignatureFingerprintAsHexString)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderGetCredentials(
                    getCredentialsRequest = createMockGetCredentialsRequest(number = 1),
                )

            every {
                mockCallingAppInfo.getSignatureFingerprintAsHexString()
            } returns "mockSignature"
            coEvery {
                originManager.validateOrigin(
                    relyingPartyId = DEFAULT_RELYING_PARTY_ID,
                    callingAppInfo = mockCallingAppInfo,
                )
            } returns ValidateOriginResult.Error.PrivilegedAppNotAllowed
            coEvery {
                bitwardenCredentialManager.getCredentialEntries(any())
            } returns emptyList<CredentialEntry>().asSuccess()

            val viewModel = createVaultItemListingViewModel()

            viewModel.trySendAction(
                VaultItemListingsAction.TrustPrivilegedAppClick(selectedCipherId = null),
            )

            // Verify the dialog is cleared
            assertNull(viewModel.stateFlow.value.dialogState)

            coVerify {
                privilegedAppRepository.addTrustedPrivilegedApp(
                    packageName = "mockPackageName",
                    signature = "mockSignature",
                )
                bitwardenCredentialManager.getCredentialEntries(
                    getCredentialsRequest = any(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `TrustPrivilegedAppClick during BeginGetCredentials should show CredentialManagerOperationFail dialog when signature is null`() =
        runTest {
            mockkStatic(CallingAppInfo::getSignatureFingerprintAsHexString)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderGetCredentials(
                    getCredentialsRequest = createMockGetCredentialsRequest(number = 1),
                )

            every {
                mockCallingAppInfo.getSignatureFingerprintAsHexString()
            } returns null

            val viewModel = createVaultItemListingViewModel()

            viewModel.trySendAction(
                VaultItemListingsAction.TrustPrivilegedAppClick(selectedCipherId = null),
            )

            assertEquals(
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString
                        .passkey_operation_failed_because_the_request_is_invalid
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )

            every { BeginGetCredentialRequest.fromBundle(any()) } returns null
            viewModel.trySendAction(
                VaultItemListingsAction.TrustPrivilegedAppClick(selectedCipherId = null),
            )

            coVerify(exactly = 0) {
                privilegedAppRepository.addTrustedPrivilegedApp(
                    packageName = any(),
                    signature = any(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `TrustPrivilegedAppClick during BeginGetCredentials should show CredentialManagerOperationFail dialog when callingAppInfo is null`() =
        runTest {
            mockkStatic(CallingAppInfo::getSignatureFingerprintAsHexString)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderGetCredentials(
                    getCredentialsRequest = createMockGetCredentialsRequest(number = 1),
                )

            every { BeginGetCredentialRequest.fromBundle(any()) } returns null

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.TrustPrivilegedAppClick(selectedCipherId = null),
            )

            assertEquals(
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString
                        .passkey_operation_failed_because_the_request_is_invalid
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )

            coVerify(exactly = 0) {
                privilegedAppRepository.addTrustedPrivilegedApp(
                    packageName = any(),
                    signature = any(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `TrustPrivilegedAppClick during Fido2CredentialAssertion should clear dialog, trust privileged app, and authenticate passkey`() =
        runTest {
            setupMockUri()
            mockkStatic(CallingAppInfo::getSignatureFingerprintAsHexString)

            val cipherListView = createMockCipherListView(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2Assertion(
                    fido2AssertionRequest = createMockFido2CredentialAssertionRequest(
                        number = 1,
                        cipherId = cipherListView.id!!,
                    ),
                )
            every {
                mockCallingAppInfo.getSignatureFingerprintAsHexString()
            } returns "mockSignature"
            coEvery {
                bitwardenCredentialManager.authenticateFido2Credential(
                    userId = any(),
                    callingAppInfo = mockCallingAppInfo,
                    request = any(),
                    selectedCipherView = any(),
                    origin = any(),
                )
            } returns Fido2CredentialAssertionResult.Success("")
            every {
                vaultRepository.decryptCipherListResultStateFlow
            } returns MutableStateFlow(
                DataState.Loaded(
                    data = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
                ),
            )
            every { bitwardenCredentialManager.isUserVerified } returns true
            coEvery {
                originManager.validateOrigin(
                    relyingPartyId = DEFAULT_RELYING_PARTY_ID,
                    callingAppInfo = mockCallingAppInfo,
                )
            } returns ValidateOriginResult.Success("mockOrigin")

            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.TrustPrivilegedAppClick(
                    selectedCipherId = cipherListView.id,
                ),
            )

            // Verify the dialog is cleared
            assertNull(viewModel.stateFlow.value.dialogState)

            coVerify {
                privilegedAppRepository.addTrustedPrivilegedApp(
                    packageName = "mockPackageName",
                    signature = "mockSignature",
                )
                bitwardenCredentialManager.authenticateFido2Credential(
                    userId = any(),
                    callingAppInfo = mockCallingAppInfo,
                    request = any(),
                    selectedCipherView = any(),
                    origin = any(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `TrustPrivilegedAppClick during Fido2CredentialAssertion should show CredentialManagerOperationFail dialog when signature is null`() =
        runTest {
            setupMockUri()
            mockkStatic(CallingAppInfo::getSignatureFingerprintAsHexString)

            val cipherListView = createMockCipherListView(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2Assertion(
                    fido2AssertionRequest = createMockFido2CredentialAssertionRequest(
                        number = 1,
                        cipherId = cipherListView.id!!,
                    ),
                )

            every {
                vaultRepository.decryptCipherListResultStateFlow
            } returns MutableStateFlow(
                DataState.Loaded(
                    data = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
                ),
            )
            every {
                mockCallingAppInfo.getSignatureFingerprintAsHexString()
            } returns null

            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.TrustPrivilegedAppClick(
                    selectedCipherId = cipherListView.id,
                ),
            )

            assertEquals(
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString
                        .passkey_operation_failed_because_the_request_is_invalid
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )

            coVerify(exactly = 0) {
                privilegedAppRepository.addTrustedPrivilegedApp(
                    packageName = any(),
                    signature = any(),
                )
                bitwardenCredentialManager.authenticateFido2Credential(
                    userId = any(),
                    callingAppInfo = any(),
                    request = any(),
                    selectedCipherView = any(),
                    origin = any(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `TrustPrivilegedAppClick during Fido2CredentialAssertion should show CredentialManagerOperationFail dialog when no matching cipher exists`() =
        runTest {
            setupMockUri()
            mockkStatic(CallingAppInfo::getSignatureFingerprintAsHexString)

            val cipherListView = createMockCipherListView(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2Assertion(
                    fido2AssertionRequest = createMockFido2CredentialAssertionRequest(
                        number = 1,
                        cipherId = cipherListView.id!!,
                    ),
                )

            coEvery {
                vaultRepository.getCipher(any())
            } returns GetCipherResult.CipherNotFound
            every {
                vaultRepository.decryptCipherListResultStateFlow
            } returns MutableStateFlow(
                DataState.Loaded(
                    data = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
                ),
            )
            every {
                mockCallingAppInfo.getSignatureFingerprintAsHexString()
            } returns "mockSignature"

            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(cipherListView),
                    ),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.TrustPrivilegedAppClick(
                    selectedCipherId = "noMatchingCipher",
                ),
            )

            assertEquals(
                VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString
                        .credential_operation_failed_because_the_selected_item_does_not_exist
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )

            coVerify(exactly = 0) {
                privilegedAppRepository.addTrustedPrivilegedApp(
                    packageName = any(),
                    signature = any(),
                )
                bitwardenCredentialManager.authenticateFido2Credential(
                    userId = any(),
                    callingAppInfo = any(),
                    request = any(),
                    selectedCipherView = any(),
                    origin = any(),
                )
            }
        }

    @Test
    fun `SnackbarDataReceive should update emit ShowSnackbar`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        val snackbarData = BitwardenSnackbarData(message = "Test".asText())
        viewModel.eventFlow.test {
            mutableSnackbarDataFlow.tryEmit(snackbarData)
            assertEquals(VaultItemListingEvent.ShowSnackbar(data = snackbarData), awaitItem())
        }
    }

    private fun setupFido2CreateRequest(
        mockCallingAppInfo: CallingAppInfo = this.mockCallingAppInfo,
        mockCreatePublicKeyCredentialRequest: CreatePublicKeyCredentialRequest =
            mockk<CreatePublicKeyCredentialRequest> {
                every { requestJson } returns "mockRequestJson"
                every { origin } returns "mockOrigin"
            },
        mockProviderCreateCredentialRequest: ProviderCreateCredentialRequest =
            mockk<ProviderCreateCredentialRequest> {
                every { callingAppInfo } returns mockCallingAppInfo
                every { callingRequest } returns mockCreatePublicKeyCredentialRequest
            },
    ) {
        every {
            ProviderCreateCredentialRequest.fromBundle(any())
        } returns mockProviderCreateCredentialRequest
    }

    private fun createSavedStateHandleWithVaultItemListingType(
        vaultItemListingType: VaultItemListingType,
    ): SavedStateHandle = SavedStateHandle().apply {
        every {
            toVaultItemListingArgs()
        } returns VaultItemListingArgs(vaultItemListingType = vaultItemListingType)
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
            bitwardenCredentialManager = bitwardenCredentialManager,
            organizationEventManager = organizationEventManager,
            originManager = originManager,
            networkConnectionManager = networkConnectionManager,
            privilegedAppRepository = privilegedAppRepository,
            snackbarRelayManager = snackbarRelayManager,
            toastManager = toastManager,
            relyingPartyParser = relyingPartyParser,
        )

    @Suppress("MaxLineLength")
    private fun createVaultItemListingState(
        itemListingType: VaultItemListingState.ItemListingType = VaultItemListingState.ItemListingType.Vault.Login,
        viewState: VaultItemListingState.ViewState = VaultItemListingState.ViewState.Loading,
        dialogState: VaultItemListingState.DialogState? = null,
        isPremium: Boolean = true,
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
            createCredentialRequest = null,
            isPremium = isPremium,
            isRefreshing = false,
            restrictItemTypesPolicyOrgIds = persistentListOf(),
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
    isExportable = true,
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = "activeUserId",
    accounts = listOf(DEFAULT_ACCOUNT),
)

private const val DEFAULT_RELYING_PARTY_ID = "www.bitwarden.com"
