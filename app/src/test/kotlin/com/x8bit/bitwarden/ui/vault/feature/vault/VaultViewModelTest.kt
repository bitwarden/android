package com.x8bit.bitwarden.ui.vault.feature.vault

import app.cash.turbine.test
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.account.model.AccountSummary
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UpdateKdfMinimumsResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserAutofillDialogManager
import com.x8bit.bitwarden.data.platform.manager.CredentialExchangeRegistryManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.manager.model.RegisterExportResult
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.manager.model.UnregisterExportResult
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCardListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCardView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockDecryptCipherListResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCipher
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import com.x8bit.bitwarden.ui.vault.components.model.CreateVaultItemType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toSnackbarData
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toViewState
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Suppress("LargeClass")
class VaultViewModelTest : BaseViewModelTest() {
    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    private val mutableSnackbarDataFlow = bufferedMutableSharedFlow<BitwardenSnackbarData>()
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay> = mockk {
        // We return an empty flow here to avoid confusion in the tests.
        // Everything should be tested via the mutableSnackbarDataFlow.
        every { getSnackbarDataFlow(SnackbarRelay.LOGIN_SUCCESS) } returns emptyFlow()
        every {
            getSnackbarDataFlow(relay = any(), relays = anyVararg())
        } returns mutableSnackbarDataFlow
    }

    private val clipboardManager: BitwardenClipboardManager = mockk {
        every { setText(text = any<String>(), toastDescriptorOverride = any<Text>()) } just runs
    }

    private val mutableActivePoliciesFlow: MutableStateFlow<List<SyncResponseJson.Policy>> =
        MutableStateFlow(emptyList())
    private val policyManager: PolicyManager = mockk {
        every {
            getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns emptyList()
        every {
            getActivePoliciesFlow(type = PolicyTypeJson.RESTRICT_ITEM_TYPES)
        } returns mutableActivePoliciesFlow
    }

    private val mutablePullToRefreshEnabledFlow = MutableStateFlow(false)
    private val mutableIsIconLoadingDisabledFlow = MutableStateFlow(false)

    private val mutableUserStateFlow =
        MutableStateFlow<UserState?>(DEFAULT_USER_STATE)

    private val mutableVaultDataStateFlow =
        MutableStateFlow<DataState<VaultData>>(DataState.Loading)

    private var switchAccountResult: SwitchAccountResult = SwitchAccountResult.NoChange

    private val mutableFirstTimeStateFlow = MutableStateFlow(FirstTimeState())
    private val firstTimeActionManager: FirstTimeActionManager = mockk {
        every { firstTimeStateFlow } returns mutableFirstTimeStateFlow
        every { storeShowImportLogins(any()) } just runs
        every { storeShowImportLoginsSettingsBadge(any()) } just runs
    }

    private val authRepository: AuthRepository =
        mockk {
            every { userStateFlow } returns mutableUserStateFlow
            every { hasPendingAccountAddition } returns false
            every { hasPendingAccountAddition = any() } just runs
            every { logout(userId = any(), reason = any()) } just runs
            every { switchAccount(any()) } answers { switchAccountResult }
            every { needsKdfUpdateToMinimums() } returns false
            coEvery {
                updateKdfToMinimumsIfNeeded(password = any())
            } returns UpdateKdfMinimumsResult.Success
        }

    private var mutableFlightRecorderDataFlow =
        MutableStateFlow(FlightRecorderDataSet(data = emptySet()))
    private val settingsRepository: SettingsRepository = mockk {
        every { getPullToRefreshEnabledFlow() } returns mutablePullToRefreshEnabledFlow
        every { isIconLoadingDisabledFlow } returns mutableIsIconLoadingDisabledFlow
        every { isIconLoadingDisabled } returns false
        every { flightRecorderData } returns FlightRecorderDataSet(data = emptySet())
        every { flightRecorderDataFlow } returns mutableFlightRecorderDataFlow
        every { dismissFlightRecorderBanner() } just runs
        every { isAutofillEnabledStateFlow } returns MutableStateFlow(false)
    }

    private val vaultRepository: VaultRepository =
        mockk {
            every { vaultFilterType = any() } just runs
            every { vaultDataStateFlow } returns mutableVaultDataStateFlow
            every { sync(forced = any()) } just runs
            every { syncIfNecessary() } just runs
            every { lockVaultForCurrentUser(any()) } just runs
            every { lockVault(any(), any()) } just runs
            coEvery {
                getCipher(any())
            } returns GetCipherResult.Success(createMockCipherView(number = 1))
        }

    private val organizationEventManager = mockk<OrganizationEventManager> {
        every { trackEvent(event = any()) } just runs
    }

    private val reviewPromptManager: ReviewPromptManager = mockk()

    private val specialCircumstanceManager: SpecialCircumstanceManager = mockk {
        every { specialCircumstance } returns null
    }

    private val networkConnectionManager: NetworkConnectionManager = mockk {
        every { isNetworkConnected } returns true
    }
    private val browserAutofillDialogManager: BrowserAutofillDialogManager = mockk {
        every { shouldShowDialog } returns false
        every { browserCount } returns 1
        every { delayDialog() } just runs
    }

    private val credentialExchangeRegistryManager: CredentialExchangeRegistryManager = mockk {
        coEvery { register() } returns RegisterExportResult.Success
        coEvery { unregister() } returns UnregisterExportResult.Success
    }
    private val mutableCxpExportFeatureFlagFlow = MutableStateFlow(false)
    private val featureFlagManager: FeatureFlagManager = mockk {
        every {
            getFeatureFlagFlow(FlagKey.CredentialExchangeProtocolExport)
        } returns mutableCxpExportFeatureFlagFlow
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(FlightRecorderDataSet::toSnackbarData)
    }

    @Test
    fun `initial state should be correct and should trigger a syncIfNecessary call`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        verify {
            vaultRepository.syncIfNecessary()
            policyManager.getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        }
    }

    @Test
    fun `UserState updates with a null value should do nothing`() {
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )

        mutableUserStateFlow.value = null

        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `UserState updates with a non-null value when switching accounts should do nothing`() {
        val viewModel = createViewModel()

        // Ensure we are currently switching accounts
        val initialState = DEFAULT_STATE.copy(isSwitchingAccounts = true)
        switchAccountResult = SwitchAccountResult.AccountSwitched
        val updatedUserId = "lockedUserId"
        viewModel.trySendAction(
            VaultAction.SwitchAccountClick(
                accountSummary = mockk {
                    every { userId } returns updatedUserId
                },
            ),
        )
        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(activeUserId = updatedUserId)

        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserState updates with a non-null value when not switching accounts should update the account information in the state when personal ownership enabled`() {
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )

        mutableUserStateFlow.value =
            DEFAULT_USER_STATE.copy(
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "Other User",
                        email = "active@bitwarden.com",
                        avatarColorHex = "#00aaaa",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        needsMasterPassword = false,
                        organizations = listOf(
                            Organization(
                                id = "organiationId",
                                name = "Test Organization",
                                shouldManageResetPassword = false,
                                shouldUseKeyConnector = false,
                                role = OrganizationType.ADMIN,
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = false,
                            ),
                        ),
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = DEFAULT_FIRST_TIME_STATE,
                        isExportable = true,
                    ),
                ),
            )

        assertEquals(
            DEFAULT_STATE.copy(
                appBarTitle = BitwardenString.vaults.asText(),
                avatarColorString = "#00aaaa",
                initials = "OU",
                accountSummaries = listOf(
                    AccountSummary(
                        userId = "activeUserId",
                        name = "Other User",
                        email = "active@bitwarden.com",
                        avatarColorHex = "#00aaaa",
                        environmentLabel = "bitwarden.com",
                        isActive = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                    ),
                ),
                vaultFilterData = VaultFilterData(
                    selectedVaultFilterType = VaultFilterType.AllVaults,
                    vaultFilterTypes = listOf(
                        VaultFilterType.AllVaults,
                        VaultFilterType.MyVault,
                        VaultFilterType.OrganizationVault(
                            organizationId = "organiationId",
                            organizationName = "Test Organization",
                        ),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserState updates with a non-null value when not switching accounts should update the account information in the state when personal ownership disabled`() {
        every {
            policyManager.getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(
            createMockPolicy(
                organizationId = "Test Organization",
                id = "testId",
                type = PolicyTypeJson.PERSONAL_OWNERSHIP,
                isEnabled = true,
                data = null,
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )

        mutableUserStateFlow.value =
            DEFAULT_USER_STATE.copy(
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "Other User",
                        email = "active@bitwarden.com",
                        avatarColorHex = "#00aaaa",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        needsMasterPassword = false,
                        organizations = listOf(
                            Organization(
                                id = "organizationId",
                                name = "Test Organization",
                                shouldManageResetPassword = false,
                                shouldUseKeyConnector = false,
                                role = OrganizationType.ADMIN,
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = false,
                            ),
                        ),
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = DEFAULT_FIRST_TIME_STATE,
                        isExportable = true,
                    ),
                ),
            )

        assertEquals(
            DEFAULT_STATE.copy(
                appBarTitle = BitwardenString.vaults.asText(),
                avatarColorString = "#00aaaa",
                initials = "OU",
                accountSummaries = listOf(
                    AccountSummary(
                        userId = "activeUserId",
                        name = "Other User",
                        email = "active@bitwarden.com",
                        avatarColorHex = "#00aaaa",
                        environmentLabel = "bitwarden.com",
                        isActive = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                    ),
                ),
                vaultFilterData = VaultFilterData(
                    selectedVaultFilterType = VaultFilterType.AllVaults,
                    vaultFilterTypes = listOf(
                        VaultFilterType.AllVaults,
                        VaultFilterType.OrganizationVault(
                            organizationId = "organizationId",
                            organizationName = "Test Organization",
                        ),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `RESTRICT_ITEM_TYPES policy changes should update restrictItemTypesPolicyOrgIds accordingly`() =
        runTest {
            val viewModel = createViewModel()
            assertEquals(
                DEFAULT_STATE,
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
                DEFAULT_STATE.copy(restrictItemTypesPolicyOrgIds = listOf("Test Organization")),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `Flight Recorder changes should update flightRecorderSnackbar accordingly`() = runTest {
        mockkStatic(FlightRecorderDataSet::toSnackbarData)
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(flightRecorderSnackBar = null), awaitItem())

            val snackbarData = mockk<BitwardenSnackbarData>()
            mutableFlightRecorderDataFlow.value = mockk<FlightRecorderDataSet> {
                every { toSnackbarData(clock = clock) } returns snackbarData
            }
            assertEquals(DEFAULT_STATE.copy(flightRecorderSnackBar = snackbarData), awaitItem())

            mutableFlightRecorderDataFlow.value = mockk<FlightRecorderDataSet> {
                every { toSnackbarData(clock = clock) } returns null
            }
            assertEquals(DEFAULT_STATE.copy(flightRecorderSnackBar = null), awaitItem())
        }
    }

    @Test
    fun `on DismissFlightRecorderSnackbar should call dismissFlightRecorderBanner`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(VaultAction.DismissFlightRecorderSnackbar)

        verify(exactly = 1) {
            settingsRepository.dismissFlightRecorderBanner()
        }
    }

    @Test
    fun `on EnableThirdPartyAutofillClick should send NavigateToAutofillSettings`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.EnableThirdPartyAutofillClick)
            assertEquals(VaultEvent.NavigateToAutofillSettings, awaitItem())
        }
        verify(exactly = 1) {
            browserAutofillDialogManager.delayDialog()
        }
    }

    @Test
    fun `on DismissThirdPartyAutofillDialogClick should call delay dialog`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(VaultAction.DismissThirdPartyAutofillDialogClick)

        verify(exactly = 1) {
            browserAutofillDialogManager.delayDialog()
        }
    }

    @Test
    fun `on ShareCipherDecryptionErrorClick should send ShowShareSheet`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(
                action = VaultAction.ShareCipherDecryptionErrorClick(selectedCipherId = "1"),
            )
            assertEquals(VaultEvent.ShowShareSheet("1"), awaitItem())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `on ShareAllCipherDecryptionErrorsClick should send ShowShareSheet`() = runTest {
        val viewModel = createViewModel()
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(number = 1)
                .copy(
                    failures = listOf(
                        createMockSdkCipher(number = 1),
                        createMockSdkCipher(number = 2),
                    ),
                ),
            collectionViewList = listOf(createMockCollectionView(number = 1)),
            folderViewList = listOf(createMockFolderView(number = 1)),
            sendViewList = listOf(createMockSendView(number = 1)),
        )
        viewModel.eventFlow.test {
            mutableVaultDataStateFlow.tryEmit(
                value = DataState.Loaded(
                    data = vaultData,
                ),
            )
            advanceTimeBy(1500)
            viewModel.trySendAction(
                action = VaultAction.ShareAllCipherDecryptionErrorsClick,
            )
            assertEquals(
                VaultEvent.ShowShareSheet("mockId-1\nmockId-2"),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on FlightRecorderGoToSettingsClick should send NavigateToAbout`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.FlightRecorderGoToSettingsClick)
            assertEquals(VaultEvent.NavigateToAbout, awaitItem())
        }
    }

    @Test
    fun `on LockAccountClick should call lockVault for the given account`() {
        val accountUserId = "userId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns accountUserId
            every { isActive } returns false
        }
        val viewModel = createViewModel()

        viewModel.trySendAction(VaultAction.LockAccountClick(accountSummary))

        verify { vaultRepository.lockVault(userId = accountUserId, isUserInitiated = true) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on LogoutAccountClick for an active account should call logout for the given account and set isSwitchingAccounts to true`() {
        val accountUserId = "userId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns accountUserId
            every { isActive } returns true
        }
        val viewModel = createViewModel()

        viewModel.trySendAction(VaultAction.LogoutAccountClick(accountSummary))

        assertEquals(
            DEFAULT_STATE.copy(
                isSwitchingAccounts = true,
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            authRepository.logout(
                userId = accountUserId,
                reason = LogoutReason.Click(source = "VaultViewModel"),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on LogoutAccountClick for an inactive account should call logout for the given account and set isSwitchingAccounts to false`() {
        val accountUserId = "userId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns accountUserId
            every { isActive } returns false
        }
        val viewModel = createViewModel()

        viewModel.trySendAction(VaultAction.LogoutAccountClick(accountSummary))

        assertEquals(
            DEFAULT_STATE.copy(
                isSwitchingAccounts = false,
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            authRepository.logout(
                userId = accountUserId,
                reason = LogoutReason.Click(source = "VaultViewModel"),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on SwitchAccountClick when result is NoChange should try to switch to the given account and set isSwitchingAccounts to false`() =
        runTest {
            val viewModel = createViewModel()
            switchAccountResult = SwitchAccountResult.NoChange
            val updatedUserId = "lockedUserId"
            viewModel.trySendAction(
                VaultAction.SwitchAccountClick(
                    accountSummary = mockk {
                        every { userId } returns updatedUserId
                    },
                ),
            )
            verify { authRepository.switchAccount(userId = updatedUserId) }
            assertEquals(
                DEFAULT_STATE.copy(isSwitchingAccounts = false),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on SwitchAccountClick when result is AccountSwitched should switch to the given account and set isSwitchingAccounts to true`() =
        runTest {
            val viewModel = createViewModel()
            switchAccountResult = SwitchAccountResult.AccountSwitched
            val updatedUserId = "lockedUserId"
            viewModel.trySendAction(
                VaultAction.SwitchAccountClick(
                    accountSummary = mockk {
                        every { userId } returns updatedUserId
                    },
                ),
            )
            verify { authRepository.switchAccount(userId = updatedUserId) }
            assertEquals(
                DEFAULT_STATE.copy(isSwitchingAccounts = true),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on AddAccountClick should set hasPendingAccountAddition to true on the AuthRepository`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultAction.AddAccountClick)
        verify {
            authRepository.hasPendingAccountAddition = true
        }
    }

    @Test
    fun `on SyncClick should call sync on the VaultRepository and show the syncing dialog`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultAction.SyncClick)
        assertEquals(
            DEFAULT_STATE.copy(
                dialog = VaultState.DialogState.Syncing,
            ),
            viewModel.stateFlow.value,
        )
        verify {
            vaultRepository.sync(forced = true)
        }
    }

    @Test
    fun `on SyncClick should show the no network dialog if not connection is available`() {
        val viewModel = createViewModel()
        every {
            networkConnectionManager.isNetworkConnected
        } returns false
        viewModel.trySendAction(VaultAction.SyncClick)
        assertEquals(
            DEFAULT_STATE.copy(
                dialog = VaultState.DialogState.Error(
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

    @Test
    fun `on LockClick should call lockVaultForCurrentUser on the VaultRepository`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultAction.LockClick)
        verify {
            vaultRepository.lockVaultForCurrentUser(isUserInitiated = true)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on VaultFilterTypeSelect should update the selected filter type and re-filter any existing data`() {
        // Update to state with filters and content
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(number = 1),
            collectionViewList = listOf(createMockCollectionView(number = 1)),
            folderViewList = listOf(createMockFolderView(number = 1)),
            sendViewList = listOf(createMockSendView(number = 1)),
        )
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Loaded(
                data = vaultData,
            ),
        )
        mutableUserStateFlow.value = DEFAULT_USER_STATE
            .copy(
                accounts = listOf(
                    DEFAULT_USER_STATE.accounts[0].copy(
                        organizations = listOf(
                            Organization(
                                id = "testOrganizationId",
                                name = "Test Organization",
                                shouldManageResetPassword = false,
                                shouldUseKeyConnector = false,
                                role = OrganizationType.ADMIN,
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = false,
                            ),
                        ),
                    ),
                    DEFAULT_USER_STATE.accounts[1],
                ),
            )
        val viewModel = createViewModel()
        val initialState = createMockVaultState(
            viewState = vaultData.toViewState(
                isPremium = true,
                vaultFilterType = VaultFilterType.AllVaults,
                isIconLoadingDisabled = viewModel.stateFlow.value.isIconLoadingDisabled,
                baseIconUrl = viewModel.stateFlow.value.baseIconUrl,
                hasMasterPassword = true,
                restrictItemTypesPolicyOrgIds = emptyList(),
            ),
        )
            .copy(
                appBarTitle = BitwardenString.vaults.asText(),
                vaultFilterData = VAULT_FILTER_DATA,
            )
        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(VaultAction.VaultFilterTypeSelect(VaultFilterType.MyVault))

        assertEquals(
            initialState.copy(
                vaultFilterData = VAULT_FILTER_DATA.copy(
                    selectedVaultFilterType = VaultFilterType.MyVault,
                ),
                viewState = vaultData.toViewState(
                    isPremium = true,
                    vaultFilterType = VaultFilterType.MyVault,
                    isIconLoadingDisabled = viewModel.stateFlow.value.isIconLoadingDisabled,
                    baseIconUrl = viewModel.stateFlow.value.baseIconUrl,
                    hasMasterPassword = true,
                    restrictItemTypesPolicyOrgIds = emptyList(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        verify { vaultRepository.vaultFilterType = VaultFilterType.MyVault }
    }

    @Test
    fun `vaultDataStateFlow Loaded with items should update state to Content`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(
                            createMockCipherListView(
                                number = 1,
                                type = CipherListViewType.Login(
                                    createMockLoginListView(number = 1),
                                ),
                            ),
                            createMockCipherListView(
                                number = 2,
                                type = CipherListViewType.Card(
                                    createMockCardListView(number = 2),
                                ),
                            ),
                            createMockCipherListView(
                                number = 3,
                                type = CipherListViewType.Identity,
                            ),
                            createMockCipherListView(
                                number = 4,
                                type = CipherListViewType.SecureNote,
                            ),
                            createMockCipherListView(
                                number = 5,
                                type = CipherListViewType.SshKey,
                            ),
                        ),
                    ),
                    collectionViewList = listOf(
                        createMockCollectionView(number = 1),
                        createMockCollectionView(number = 2),
                        createMockCollectionView(number = 3),
                        createMockCollectionView(number = 4),
                        createMockCollectionView(number = 5),
                    ),
                    folderViewList = listOf(
                        createMockFolderView(number = 1),
                        createMockFolderView(number = 2),
                        createMockFolderView(number = 3),
                        createMockFolderView(number = 4),
                        createMockFolderView(number = 5),
                    ),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            ),
        )

        val viewModel = createViewModel()

        assertEquals(
            createMockVaultState(
                viewState = VaultState.ViewState.Content(
                    loginItemsCount = 1,
                    cardItemsCount = 1,
                    identityItemsCount = 1,
                    secureNoteItemsCount = 1,
                    favoriteItems = listOf(),
                    folderItems = listOf(
                        VaultState.ViewState.FolderItem(
                            id = "mockId-1",
                            name = "mockName-1".asText(),
                            itemCount = 1,
                        ),
                        VaultState.ViewState.FolderItem(
                            id = "mockId-2",
                            name = "mockName-2".asText(),
                            itemCount = 1,
                        ),
                        VaultState.ViewState.FolderItem(
                            id = "mockId-3",
                            name = "mockName-3".asText(),
                            itemCount = 1,
                        ),
                        VaultState.ViewState.FolderItem(
                            id = "mockId-4",
                            name = "mockName-4".asText(),
                            itemCount = 1,
                        ),
                        VaultState.ViewState.FolderItem(
                            id = "mockId-5",
                            name = "mockName-5".asText(),
                            itemCount = 1,
                        ),
                        VaultState.ViewState.FolderItem(
                            id = null,
                            name = BitwardenString.folder_none.asText(),
                            itemCount = 0,
                        ),
                    ),
                    collectionItems = listOf(
                        VaultState.ViewState.CollectionItem(
                            id = "mockId-1",
                            name = "mockName-1",
                            itemCount = 1,
                        ),
                        VaultState.ViewState.CollectionItem(
                            id = "mockId-2",
                            name = "mockName-2",
                            itemCount = 1,
                        ),
                        VaultState.ViewState.CollectionItem(
                            id = "mockId-3",
                            name = "mockName-3",
                            itemCount = 1,
                        ),
                        VaultState.ViewState.CollectionItem(
                            id = "mockId-4",
                            name = "mockName-4",
                            itemCount = 1,
                        ),
                        VaultState.ViewState.CollectionItem(
                            id = "mockId-5",
                            name = "mockName-5",
                            itemCount = 1,
                        ),
                    ),
                    noFolderItems = listOf(),
                    trashItemsCount = 0,
                    totpItemsCount = 1,
                    itemTypesCount = CipherType.entries.size,
                    sshKeyItemsCount = 1,
                    showCardGroup = true,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with items when manually syncing with the sync button should update state to Content, show a success Snackbar, and dismiss pull to refresh`() =
        runTest {
            val expectedState = createMockVaultState(
                viewState = VaultState.ViewState.Content(
                    loginItemsCount = 1,
                    cardItemsCount = 0,
                    identityItemsCount = 0,
                    secureNoteItemsCount = 0,
                    favoriteItems = listOf(),
                    folderItems = listOf(),
                    collectionItems = listOf(),
                    noFolderItems = listOf(),
                    trashItemsCount = 0,
                    totpItemsCount = 1,
                    itemTypesCount = 5,
                    sshKeyItemsCount = 0,
                    showCardGroup = true,
                ),
            )
            val viewModel = createViewModel()
            viewModel.trySendAction(VaultAction.SyncClick)

            viewModel.eventFlow.test {
                mutableVaultDataStateFlow.tryEmit(
                    value = DataState.Loaded(
                        data = VaultData(
                            decryptCipherListResult = createMockDecryptCipherListResult(
                                number = 1,
                                successes = listOf(createMockCipherListView(number = 1)),
                            ),
                            collectionViewList = emptyList(),
                            folderViewList = emptyList(),
                            sendViewList = emptyList(),
                        ),
                    ),
                )
                // Allow time for state to update
                advanceTimeBy(1500)
                assertEquals(expectedState, viewModel.stateFlow.value)
                assertEquals(
                    VaultEvent.ShowSnackbar(BitwardenString.syncing_complete.asText()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `vaultDataStateFlow Loaded with empty items should update state to NoItems`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = emptyList(),
                    ),
                    collectionViewList = emptyList(),
                    folderViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            createMockVaultState(viewState = VaultState.ViewState.NoItems),
            viewModel.stateFlow.value,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with empty items when manually syncing with the sync button should update state to NoItems, show a success Snackbar, and dismiss pull to refresh`() =
        runTest {
            val expectedState = createMockVaultState(
                viewState = VaultState.ViewState.NoItems,
            )
            val viewModel = createViewModel()
            viewModel.trySendAction(VaultAction.SyncClick)

            viewModel.eventFlow.test {
                mutableVaultDataStateFlow.value = DataState.Loaded(
                    data = VaultData(
                        decryptCipherListResult = createMockDecryptCipherListResult(
                            number = 1,
                            successes = emptyList(),
                        ),
                        collectionViewList = emptyList(),
                        folderViewList = emptyList(),
                        sendViewList = emptyList(),
                    ),
                )
                // Allow time for state to update
                advanceTimeBy(1500)
                assertEquals(expectedState, viewModel.stateFlow.value)
                assertEquals(
                    VaultEvent.ShowSnackbar(BitwardenString.syncing_complete.asText()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `vaultDataStateFlow Pending with items should update state to Content`() {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Pending(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(createMockCipherListView(number = 1)),
                    ),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            ),
        )

        val viewModel = createViewModel()

        assertEquals(
            createMockVaultState(
                viewState = VaultState.ViewState.Content(
                    loginItemsCount = 1,
                    cardItemsCount = 0,
                    identityItemsCount = 0,
                    secureNoteItemsCount = 0,
                    favoriteItems = listOf(),
                    folderItems = listOf(
                        VaultState.ViewState.FolderItem(
                            id = "mockId-1",
                            name = "mockName-1".asText(),
                            itemCount = 1,
                        ),
                        VaultState.ViewState.FolderItem(
                            id = null,
                            name = BitwardenString.folder_none.asText(),
                            itemCount = 0,
                        ),
                    ),
                    collectionItems = listOf(
                        VaultState.ViewState.CollectionItem(
                            id = "mockId-1",
                            name = "mockName-1",
                            itemCount = 1,
                        ),
                    ),
                    noFolderItems = listOf(),
                    trashItemsCount = 0,
                    totpItemsCount = 1,
                    itemTypesCount = 5,
                    sshKeyItemsCount = 0,
                    showCardGroup = true,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Pending with empty items should update state to NoItems`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Pending(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = emptyList(),
                    ),
                    collectionViewList = emptyList(),
                    folderViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            createMockVaultState(viewState = VaultState.ViewState.NoItems),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Loading should update state to Loading`() = runTest {
        mutableVaultDataStateFlow.tryEmit(value = DataState.Loading)

        val viewModel = createViewModel()

        assertEquals(
            createMockVaultState(viewState = VaultState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error without data should update state to Error`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Error(error = IllegalStateException()),
        )

        val viewModel = createViewModel()

        assertEquals(
            createMockVaultState(
                viewState = VaultState.ViewState.Error(
                    message = BitwardenString.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Error with items should update state to Content and show an error dialog`() =
        runTest {
            mutableVaultDataStateFlow.tryEmit(
                value = DataState.Error(
                    error = IllegalStateException(),
                    data = VaultData(
                        decryptCipherListResult = createMockDecryptCipherListResult(
                            number = 1,
                            successes = listOf(createMockCipherListView(number = 1)),
                        ),
                        collectionViewList = listOf(createMockCollectionView(number = 1)),
                        folderViewList = listOf(createMockFolderView(number = 1)),
                        sendViewList = listOf(createMockSendView(number = 1)),
                    ),
                ),
            )

            val viewModel = createViewModel()

            assertEquals(
                createMockVaultState(
                    viewState = VaultState.ViewState.Content(
                        loginItemsCount = 1,
                        cardItemsCount = 0,
                        identityItemsCount = 0,
                        secureNoteItemsCount = 0,
                        favoriteItems = listOf(),
                        folderItems = listOf(
                            VaultState.ViewState.FolderItem(
                                id = "mockId-1",
                                name = "mockName-1".asText(),
                                itemCount = 1,
                            ),
                            VaultState.ViewState.FolderItem(
                                id = null,
                                name = BitwardenString.folder_none.asText(),
                                itemCount = 0,
                            ),
                        ),
                        collectionItems = listOf(
                            VaultState.ViewState.CollectionItem(
                                id = "mockId-1",
                                name = "mockName-1",
                                itemCount = 1,
                            ),
                        ),
                        noFolderItems = listOf(),
                        trashItemsCount = 0,
                        totpItemsCount = 1,
                        itemTypesCount = 5,
                        sshKeyItemsCount = 0,
                        showCardGroup = true,
                    ),
                    dialog = VaultState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Error with empty items should update state to NoItems and show an error dialog`() =
        runTest {
            mutableVaultDataStateFlow.tryEmit(
                value = DataState.Error(
                    error = IllegalStateException(),
                    data = VaultData(
                        decryptCipherListResult = createMockDecryptCipherListResult(
                            number = 1,
                            successes = emptyList(),
                        ),
                        collectionViewList = emptyList(),
                        folderViewList = emptyList(),
                        sendViewList = emptyList(),
                    ),
                ),
            )
            val viewModel = createViewModel()
            assertEquals(
                createMockVaultState(
                    viewState = VaultState.ViewState.NoItems,
                    dialog = VaultState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `vaultDataStateFlow NoNetwork without data should update state to NoItems`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.NoNetwork(),
        )

        val viewModel = createViewModel()

        assertEquals(
            createMockVaultState(
                viewState = VaultState.ViewState.NoItems,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow NoNetwork with items should update state to Content`() =
        runTest {
            mutableVaultDataStateFlow.tryEmit(
                value = DataState.NoNetwork(
                    data = VaultData(
                        decryptCipherListResult = createMockDecryptCipherListResult(
                            number = 1,
                            successes = listOf(createMockCipherListView(number = 1)),
                        ),
                        collectionViewList = listOf(createMockCollectionView(number = 1)),
                        folderViewList = listOf(createMockFolderView(number = 1)),
                        sendViewList = listOf(createMockSendView(number = 1)),
                    ),
                ),
            )

            val viewModel = createViewModel()

            assertEquals(
                createMockVaultState(
                    viewState = VaultState.ViewState.Content(
                        loginItemsCount = 1,
                        cardItemsCount = 0,
                        identityItemsCount = 0,
                        secureNoteItemsCount = 0,
                        favoriteItems = listOf(),
                        folderItems = listOf(
                            VaultState.ViewState.FolderItem(
                                id = "mockId-1",
                                name = "mockName-1".asText(),
                                itemCount = 1,
                            ),
                            VaultState.ViewState.FolderItem(
                                id = null,
                                name = BitwardenString.folder_none.asText(),
                                itemCount = 0,
                            ),
                        ),
                        collectionItems = listOf(
                            VaultState.ViewState.CollectionItem(
                                id = "mockId-1",
                                name = "mockName-1",
                                itemCount = 1,
                            ),
                        ),
                        noFolderItems = listOf(),
                        trashItemsCount = 0,
                        totpItemsCount = 1,
                        itemTypesCount = 5,
                        sshKeyItemsCount = 0,
                        showCardGroup = true,
                    ),
                    dialog = null,
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `vaultDataStateFlow updates should do nothing when switching accounts`() {
        val viewModel = createViewModel()

        // Ensure we are currently switching accounts
        val initialState = DEFAULT_STATE.copy(isSwitchingAccounts = true)
        switchAccountResult = SwitchAccountResult.AccountSwitched
        val updatedUserId = "lockedUserId"
        viewModel.trySendAction(
            VaultAction.SwitchAccountClick(
                accountSummary = mockk {
                    every { userId } returns updatedUserId
                },
            ),
        )
        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = emptyList(),
                ),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )

        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Loaded should include SSH key vault items`() =
        runTest {
            mutableVaultDataStateFlow.tryEmit(
                value = DataState.Loaded(
                    data = VaultData(
                        decryptCipherListResult = createMockDecryptCipherListResult(
                            number = 1,
                            successes = listOf(
                                createMockCipherListView(number = 1),
                                createMockCipherListView(
                                    number = 1,
                                    type = CipherListViewType.SshKey,
                                ),
                            ),
                        ),
                        collectionViewList = listOf(),
                        folderViewList = listOf(),
                        sendViewList = listOf(),
                    ),
                ),
            )

            val viewModel = createViewModel()

            assertEquals(
                createMockVaultState(
                    viewState = VaultState.ViewState.Content(
                        loginItemsCount = 1,
                        cardItemsCount = 0,
                        identityItemsCount = 0,
                        secureNoteItemsCount = 0,
                        favoriteItems = listOf(),
                        folderItems = listOf(),
                        collectionItems = listOf(),
                        noFolderItems = listOf(),
                        trashItemsCount = 0,
                        totpItemsCount = 1,
                        itemTypesCount = CipherType.entries.size,
                        sshKeyItemsCount = 1,
                        showCardGroup = true,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `VerificationCodesClick should emit NavigateToVerificationCodeScreen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.VerificationCodesClick)
            assertEquals(VaultEvent.NavigateToVerificationCodeScreen, awaitItem())
        }
    }

    @Test
    fun `AddItemClick should emit NavigateToAddItemScreen with correct type`() = runTest {
        val viewModel = createViewModel()
        val cipherType = CreateVaultItemType.CARD
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.AddItemClick(cipherType))
            assertEquals(VaultEvent.NavigateToAddItemScreen(VaultItemCipherType.CARD), awaitItem())
        }
    }

    @Test
    fun `CardGroupClick should emit NavigateToItemListing event with Card type`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.CardGroupClick)
            assertEquals(
                VaultEvent.NavigateToItemListing(VaultItemListingType.Card),
                awaitItem(),
            )
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `FolderClick should emit NavigateToItemListing event for Folder type with correct folder ID`() =
        runTest {
            val viewModel = createViewModel()
            val folderId = "12345"
            val folder = mockk<VaultState.ViewState.FolderItem> {
                every { id } returns folderId
            }
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAction.FolderClick(folder))
                assertEquals(
                    VaultEvent.NavigateToItemListing(VaultItemListingType.Folder(folderId)),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `CollectionClick should emit NavigateToItemListing event with Collection type with the correct collection ID`() =
        runTest {
            val viewModel = createViewModel()
            val collectionId = "12345"
            val collection = mockk<VaultState.ViewState.CollectionItem> {
                every { id } returns collectionId
            }
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAction.CollectionClick(collection))
                assertEquals(
                    VaultEvent.NavigateToItemListing(VaultItemListingType.Collection(collectionId)),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `IdentityGroupClick should emit NavigateToItemListing event with Identity type`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAction.IdentityGroupClick)
                assertEquals(
                    VaultEvent.NavigateToItemListing(VaultItemListingType.Identity),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `LoginGroupClick should emit NavigateToItemListing event with Login type`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.LoginGroupClick)
            assertEquals(
                VaultEvent.NavigateToItemListing(VaultItemListingType.Login),
                awaitItem(),
            )
        }
    }

    @Test
    fun `SearchIconClick should emit NavigateToVaultSearchScreen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.SearchIconClick)
            assertEquals(VaultEvent.NavigateToVaultSearchScreen, awaitItem())
        }
    }

    @Test
    fun `SecureNoteGroupClick should emit NavigateToItemListing event with SecureNote type`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAction.SecureNoteGroupClick)
                assertEquals(
                    VaultEvent.NavigateToItemListing(VaultItemListingType.SecureNote),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `SshKeyGroupClick should emit NavigateToItemListing event with SshKey type`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.SshKeyGroupClick)
            assertEquals(
                VaultEvent.NavigateToItemListing(VaultItemListingType.SshKey),
                awaitItem(),
            )
        }
    }

    @Test
    fun `TrashClick should emit NavigateToItemListing event with Trash type`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.TrashClick)
            assertEquals(
                VaultEvent.NavigateToItemListing(VaultItemListingType.Trash),
                awaitItem(),
            )
        }
    }

    @Test
    fun `VaultItemClick should emit NavigateToVaultItem with the correct item ID`() = runTest {
        val viewModel = createViewModel()
        val itemId = "54321"
        val item = mockk<VaultState.ViewState.VaultItem> {
            every { id } returns itemId
            every { type } returns VaultItemCipherType.LOGIN
            every { hasDecryptionError } returns false
        }
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.VaultItemClick(item))
            assertEquals(
                VaultEvent.NavigateToVaultItem(itemId = itemId, type = VaultItemCipherType.LOGIN),
                awaitItem(),
            )
        }
    }

    @Test
    fun `VaultItemClick should show alert if hasDecryptionError is true`() = runTest {
        val viewModel = createViewModel()
        val itemId = "54321"
        val item = mockk<VaultState.ViewState.VaultItem> {
            every { id } returns itemId
            every { type } returns VaultItemCipherType.LOGIN
            every { hasDecryptionError } returns true
        }

        viewModel.trySendAction(VaultAction.VaultItemClick(item))
        assertEquals(
            DEFAULT_STATE.copy(
                dialog = VaultState.DialogState.CipherDecryptionError(
                    title = BitwardenString.decryption_error.asText(),
                    message = BitwardenString
                        .bitwarden_could_not_decrypt_this_vault_item_description_long.asText(),
                    selectedCipherId = itemId,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with decryption failures should show VaultLoadCipherDecryptionError dialog when hasShownDecryptionFailureAlert is false`() =
        runTest {
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = emptyList(),
                        failures = listOf(
                            createMockSdkCipher(number = 1).copy(
                                deletedDate = null,
                            ),
                            createMockSdkCipher(number = 2).copy(
                                deletedDate = null,
                            ),
                        ),
                    ),
                    collectionViewList = emptyList(),
                    folderViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createViewModel()

            assertEquals(
                createMockVaultState(
                    viewState = VaultState.ViewState.Content(
                        loginItemsCount = 2,
                        cardItemsCount = 0,
                        identityItemsCount = 0,
                        secureNoteItemsCount = 0,
                        favoriteItems = listOf(),
                        folderItems = listOf(),
                        collectionItems = listOf(),
                        noFolderItems = listOf(),
                        trashItemsCount = 0,
                        totpItemsCount = 0,
                        itemTypesCount = 5,
                        sshKeyItemsCount = 0,
                        showCardGroup = true,
                    ),
                    dialog = VaultState.DialogState.VaultLoadCipherDecryptionError(
                        title = BitwardenString.decryption_error.asText(),
                        cipherCount = 2,
                    ),
                ).copy(
                    hasShownDecryptionFailureAlert = true,
                    cipherDecryptionFailureIds = persistentListOf(
                        "mockId-1",
                        "mockId-2",
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with decryption failures should not show dialog when hasShownDecryptionFailureAlert is true`() =
        runTest {
            val dataState = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(number = 1)
                        .copy(
                            failures = listOf(
                                createMockSdkCipher(number = 1).copy(
                                    deletedDate = null,
                                ),
                                createMockSdkCipher(number = 2).copy(
                                    deletedDate = null,
                                ),
                            ),
                        ),
                    collectionViewList = emptyList(),
                    folderViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            mutableVaultDataStateFlow.value = dataState
            val viewModel = createViewModel()

            // Clear the dialog by dismissing it
            viewModel.trySendAction(VaultAction.DialogDismiss)

            // Emit new data with failures - should not show dialog again
            mutableVaultDataStateFlow.tryEmit(value = dataState)

            // Advance time to allow state updates
            advanceTimeBy(1500)

            assertEquals(
                createMockVaultState(
                    viewState = VaultState.ViewState.Content(
                        loginItemsCount = 3,
                        cardItemsCount = 0,
                        identityItemsCount = 0,
                        secureNoteItemsCount = 0,
                        favoriteItems = listOf(),
                        folderItems = listOf(),
                        collectionItems = listOf(),
                        noFolderItems = listOf(),
                        trashItemsCount = 0,
                        totpItemsCount = 1,
                        itemTypesCount = 5,
                        sshKeyItemsCount = 0,
                        showCardGroup = true,
                    ),
                    dialog = null,
                ).copy(
                    hasShownDecryptionFailureAlert = true,
                    cipherDecryptionFailureIds = persistentListOf(
                        "mockId-1",
                        "mockId-2",
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `TryAgainClick should sync the vault data`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(VaultAction.TryAgainClick)

        verify { vaultRepository.sync(forced = true) }
    }

    @Test
    fun `DialogDismiss should clear the active dialog`() {

        mutableVaultDataStateFlow.value = DataState.Error(
            error = IllegalStateException(),
            data = VaultData(
                decryptCipherListResult = createMockDecryptCipherListResult(
                    number = 1,
                    successes = emptyList(),
                ),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )
        val viewModel = createViewModel()
        val initialState = DEFAULT_STATE.copy(
            viewState = VaultState.ViewState.NoItems,
            dialog = VaultState.DialogState.Error(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString.generic_error_message.asText(),
            ),
        )
        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(VaultAction.DialogDismiss)

        assertEquals(
            initialState.copy(dialog = null),
            viewModel.stateFlow.value,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `RefreshPull should call vault repository sync`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultAction.RefreshPull)
        advanceTimeBy(300)
        verify(exactly = 1) {
            vaultRepository.sync(forced = false)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `RefreshPull should show network error if no internet connection`() = runTest {
        val viewModel = createViewModel()
        every {
            networkConnectionManager.isNetworkConnected
        } returns false

        viewModel.trySendAction(VaultAction.RefreshPull)
        advanceTimeBy(300)
        assertEquals(
            DEFAULT_STATE.copy(
                isRefreshing = false,
                dialog = VaultState.DialogState.Error(
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
        val viewModel = createViewModel()

        viewModel.trySendAction(
            VaultAction.Internal.PullToRefreshEnableReceive(isPullToRefreshEnabled = true),
        )

        assertEquals(
            DEFAULT_STATE.copy(isPullToRefreshSettingEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `icon loading state updates should update isIconLoadingDisabled`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.stateFlow.value.isIconLoadingDisabled)

        mutableIsIconLoadingDisabledFlow.value = true
        assertTrue(viewModel.stateFlow.value.isIconLoadingDisabled)
    }

    @Test
    fun `OverflowOptionClick Vault CopyNoteClick should call setText on the ClipboardManager`() =
        runTest {
            val notes = "mockNotes-1"
            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyNoteClick(
                        cipherId = "mockId-1",
                        requiresPasswordReprompt = false,
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

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyNoteClick should not call setText on the ClipboardManager when decryption fails`() =
        runTest {
            val cipherId = "cipherId"
            val throwable = Throwable("Decryption failed")
            coEvery {
                vaultRepository.getCipher(cipherId = cipherId)
            } returns GetCipherResult.Failure(error = throwable)

            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyNoteClick(
                        cipherId = cipherId,
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
            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = VaultState.DialogState.Error(
                        title = BitwardenString.decryption_error.asText(),
                        message = BitwardenString.failed_to_decrypt_cipher_contact_support.asText(),
                        error = throwable,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyNoteClick should not call setText on the ClipboardManager when cipher is not found`() =
        runTest {
            val cipherId = "cipherId"
            coEvery {
                vaultRepository.getCipher(cipherId = cipherId)
            } returns GetCipherResult.CipherNotFound

            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyNoteClick(
                        cipherId = cipherId,
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
            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = VaultState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.generic_error_message.asText(),
                        error = null,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `OverflowOptionClick Vault CopyNumberClick should call setText on the ClipboardManager`() =
        runTest {
            val number = "12345-4321-9876-6789"
            val viewModel = createViewModel()
            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.Success(
                createMockCipherView(
                    number = 1,
                    cipherType = CipherType.CARD,
                    card = createMockCardView(number = 1, cardNumber = number),
                ),
            )
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
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
    fun `OverflowOptionClick Vault CopyNumberClick should not call setText on the ClipboardManager when decryption fails`() =
        runTest {
            val cipherId = "cipherId"
            val throwable = Throwable("Decryption failed")
            coEvery {
                vaultRepository.getCipher(cipherId = cipherId)
            } returns GetCipherResult.Failure(error = throwable)

            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyNumberClick(
                        cipherId = cipherId,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            verify(exactly = 0) {
                clipboardManager.setText(
                    text = any<String>(),
                    toastDescriptorOverride = any<Text>(),
                )
            }
            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = VaultState.DialogState.Error(
                        title = BitwardenString.decryption_error.asText(),
                        message = BitwardenString.failed_to_decrypt_cipher_contact_support.asText(),
                        error = throwable,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyNumberClick should not call setText on the ClipboardManager when cipher is not found`() =
        runTest {
            val cipherId = "cipherId"
            coEvery {
                vaultRepository.getCipher(cipherId = cipherId)
            } returns GetCipherResult.CipherNotFound

            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyNumberClick(
                        cipherId = cipherId,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            verify(exactly = 0) {
                clipboardManager.setText(
                    text = any<String>(),
                    toastDescriptorOverride = any<Text>(),
                )
            }
            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = VaultState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.generic_error_message.asText(),
                        error = null,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyPasswordClick should call setText on the ClipboardManager`() =
        runTest {
            val password = "mockPassword-1"
            val cipherId = "cipherId"
            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
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
    fun `OverflowOptionClick Vault CopyPasswordClick should not call setText on the ClipboardManager when decryption fails`() =
        runTest {
            val cipherId = "cipherId"
            val throwable = Throwable("Decryption failed")
            coEvery {
                vaultRepository.getCipher(cipherId = cipherId)
            } returns GetCipherResult.Failure(error = throwable)

            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                        requiresPasswordReprompt = true,
                        cipherId = cipherId,
                    ),
                ),
            )
            verify(exactly = 0) {
                clipboardManager.setText(
                    text = any<String>(),
                    toastDescriptorOverride = any<Text>(),
                )
                organizationEventManager.trackEvent(event = any())
            }
            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = VaultState.DialogState.Error(
                        title = BitwardenString.decryption_error.asText(),
                        message = BitwardenString.failed_to_decrypt_cipher_contact_support.asText(),
                        error = throwable,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyPasswordClick should not call setText on the ClipboardManager when cipher is not found`() =
        runTest {
            val cipherId = "cipherId"
            coEvery {
                vaultRepository.getCipher(cipherId = cipherId)
            } returns GetCipherResult.CipherNotFound

            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                        requiresPasswordReprompt = true,
                        cipherId = cipherId,
                    ),
                ),
            )
            verify(exactly = 0) {
                clipboardManager.setText(
                    text = any<String>(),
                    toastDescriptorOverride = any<Text>(),
                )
                organizationEventManager.trackEvent(event = any())
            }
            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = VaultState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.generic_error_message.asText(),
                        error = null,
                    ),
                ),
                viewModel.stateFlow.value,
            )
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

            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
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
            } returns GenerateTotpResult.Error(error = Throwable("Fail"))

            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
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
    fun `OverflowOptionClick Vault CopySecurityCodeClick should call setText on the ClipboardManager`() =
        runTest {
            val securityCode = "234"
            val cipherId = "cipherId"
            val viewModel = createViewModel()

            coEvery {
                vaultRepository.getCipher(cipherId = cipherId)
            } returns GetCipherResult.Success(
                createMockCipherView(
                    number = 1,
                    cipherType = CipherType.CARD,
                    card = createMockCardView(number = 1, code = securityCode),
                ),
            )

            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
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
    fun `OverflowOptionClick Vault CopySecurityCodeClick should send DecryptionErrorReceive when decryption fails`() =
        runTest {
            val cipherId = "cipherId"
            val throwable = Throwable("Decryption failed")
            coEvery {
                vaultRepository.getCipher(cipherId = cipherId)
            } returns GetCipherResult.Failure(error = throwable)

            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopySecurityCodeClick(
                        cipherId = cipherId,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            verify(exactly = 0) {
                clipboardManager.setText(
                    text = any<String>(),
                    toastDescriptorOverride = any<Text>(),
                )
                organizationEventManager.trackEvent(event = any())
            }
            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = VaultState.DialogState.Error(
                        title = BitwardenString.decryption_error.asText(),
                        message = BitwardenString.failed_to_decrypt_cipher_contact_support.asText(),
                        error = throwable,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopySecurityCodeClick should send DecryptionErrorReceive when cipher is not found`() =
        runTest {
            val cipherId = "cipherId"
            coEvery {
                vaultRepository.getCipher(cipherId = cipherId)
            } returns GetCipherResult.CipherNotFound

            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopySecurityCodeClick(
                        cipherId = cipherId,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            verify(exactly = 0) {
                clipboardManager.setText(
                    text = any<String>(),
                    toastDescriptorOverride = any<Text>(),
                )
                organizationEventManager.trackEvent(event = any())
            }
            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = VaultState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.generic_error_message.asText(),
                        error = null,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyUsernameClick should call setText on the ClipboardManager`() =
        runTest {
            val username = "bitwarden"
            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
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
    fun `OverflowOptionClick Vault EditClick should emit NavigateToEditVaultItem`() = runTest {
        val cipherId = "cipherId-1234"
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = cipherId,
                        cipherType = CipherType.LOGIN,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            assertEquals(
                VaultEvent.NavigateToEditVaultItem(
                    itemId = cipherId,
                    type = VaultItemCipherType.LOGIN,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `OverflowOptionClick Vault LaunchClick should emit NavigateToUrl`() = runTest {
        val url = "www.test.com"
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.LaunchClick(url = url),
                ),
            )
            assertEquals(VaultEvent.NavigateToUrl(url), awaitItem())
        }
    }

    @Test
    fun `OverflowOptionClick Vault ViewClick without reprompt should emit NavigateToUrl`() =
        runTest {
            val cipherId = "cipherId-9876"
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    VaultAction.OverflowOptionClick(
                        ListingItemOverflowAction.VaultAction.ViewClick(
                            cipherId = cipherId,
                            cipherType = CipherType.LOGIN,
                            requiresPasswordReprompt = false,
                        ),
                    ),
                )
                assertEquals(
                    VaultEvent.NavigateToVaultItem(
                        itemId = cipherId,
                        type = VaultItemCipherType.LOGIN,
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowMasterPasswordRepromptSubmit for a request Error should show a generic error dialog`() =
        runTest {
            val password = "password"
            val error = Throwable("Fail!")
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Error(error = error)

            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE,
                    awaitItem(),
                )

                viewModel.trySendAction(
                    VaultAction.OverflowMasterPasswordRepromptSubmit(
                        overflowAction = ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                            requiresPasswordReprompt = true,
                            cipherId = "cipherId",
                        ),
                        password = password,
                    ),
                )

                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = VaultState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            error = error,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowMasterPasswordRepromptSubmit for a request Success with an invalid password should show an invalid password dialog`() =
        runTest {
            val password = "password"
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = false)

            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE,
                    awaitItem(),
                )

                viewModel.trySendAction(
                    VaultAction.OverflowMasterPasswordRepromptSubmit(
                        overflowAction = ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                            requiresPasswordReprompt = true,
                            cipherId = "cipherId",
                        ),
                        password = password,
                    ),
                )

                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = VaultState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.invalid_master_password.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowMasterPasswordRepromptSubmit for a request Success with a valid password should continue the action`() =
        runTest {
            val password = "password"
            val cipherId = "cipherId"
            val cipherView = createMockCipherView(
                number = 1,
                login = createMockLoginView(number = 1, password = password),
            )
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)
            coEvery {
                vaultRepository.getCipher(cipherId)
            } returns GetCipherResult.Success(cipherView)

            val viewModel = createViewModel()

            viewModel.trySendAction(
                VaultAction.OverflowMasterPasswordRepromptSubmit(
                    overflowAction = ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                        requiresPasswordReprompt = true,
                        cipherId = cipherId,
                    ),
                    password = password,
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

    @Test
    fun `MasterPasswordRepromptSubmit for a request Error should show a generic error dialog`() =
        runTest {
            val password = "password"
            val error = Throwable("Fail!")
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Error(error = error)

            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE,
                    awaitItem(),
                )

                viewModel.trySendAction(
                    VaultAction.MasterPasswordRepromptSubmit(
                        item = VaultState.ViewState.VaultItem.Login(
                            id = "cipherId",
                            name = "name".asText(),
                            shouldShowMasterPasswordReprompt = true,
                            username = null,
                            overflowOptions = persistentListOf(),
                            hasDecryptionError = false,
                        ),
                        password = password,
                    ),
                )

                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = VaultState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            error = error,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordRepromptSubmit for a request Success with an invalid password should show an invalid password dialog`() =
        runTest {
            val password = "password"
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = false)

            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE,
                    awaitItem(),
                )

                viewModel.trySendAction(
                    VaultAction.MasterPasswordRepromptSubmit(
                        item = VaultState.ViewState.VaultItem.Card(
                            id = "cipherId",
                            name = "name".asText(),
                            shouldShowMasterPasswordReprompt = true,
                            overflowOptions = persistentListOf(),
                            hasDecryptionError = false,
                        ),
                        password = password,
                    ),
                )

                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = VaultState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.invalid_master_password.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordRepromptSubmit for a request Success with a valid password should continue the action`() =
        runTest {
            val password = "password"
            val item = VaultState.ViewState.VaultItem.Identity(
                id = "cipherId",
                name = "name".asText(),
                shouldShowMasterPasswordReprompt = true,
                fullName = null,
                overflowOptions = persistentListOf(),
                hasDecryptionError = false,
            )
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)

            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    VaultAction.MasterPasswordRepromptSubmit(
                        item = item,
                        password = password,
                    ),
                )
                assertEquals(
                    VaultEvent.NavigateToVaultItem(itemId = item.id, type = item.type),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `when user first time state updates, vault state is updated`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
            mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                accounts = DEFAULT_USER_STATE.accounts.map {
                    it.copy(
                        firstTimeState = DEFAULT_FIRST_TIME_STATE.copy(
                            showImportLoginsCard = false,
                        ),
                    )
                },
            )

            assertEquals(
                DEFAULT_STATE.copy(
                    showImportActionCard = false,
                ),
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when DismissImportActionCard is sent, repository called to showImportLogins to false and storeShowImportLoginsBadge to true`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultAction.DismissImportActionCard)
        verify(exactly = 1) {
            firstTimeActionManager.storeShowImportLogins(false)
            firstTimeActionManager.storeShowImportLoginsSettingsBadge(true)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when DismissImportActionCard is sent, repository is not called if value is already false`() {
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = DEFAULT_USER_STATE.accounts.map {
                it.copy(
                    firstTimeState = DEFAULT_FIRST_TIME_STATE.copy(
                        showImportLoginsCard = false,
                    ),
                )
            },
        )
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultAction.DismissImportActionCard)
        verify(exactly = 0) {
            firstTimeActionManager.storeShowImportLogins(false)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when ImportActionCardClick is sent, NavigateToImportLogins event is sent`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAction.ImportActionCardClick)
                assertEquals(VaultEvent.NavigateToImportLogins, awaitItem())
            }
            verify(exactly = 0) {
                firstTimeActionManager.storeShowImportLogins(false)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when ImportActionCardClick is sent, repository is not called if value is already false`() {
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = DEFAULT_USER_STATE.accounts.map {
                it.copy(
                    firstTimeState = DEFAULT_FIRST_TIME_STATE.copy(
                        showImportLoginsCard = false,
                    ),
                )
            },
        )
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultAction.ImportActionCardClick)
        verify(exactly = 0) {
            firstTimeActionManager.storeShowImportLogins(false)
        }
    }

    @Test
    fun `when SnackbarRelay flow updates, snackbar is shown`() = runTest {
        val viewModel = createViewModel()
        val expectedSnackbarData = BitwardenSnackbarData(message = "test message".asText())
        viewModel.eventFlow.test {
            mutableSnackbarDataFlow.tryEmit(expectedSnackbarData)
            assertEquals(VaultEvent.ShowSnackbar(expectedSnackbarData), awaitItem())
        }
    }

    @Test
    fun `when account switch action is handled, clear snackbar relay buffer should be called`() =
        runTest {
            val viewModel = createViewModel()
            switchAccountResult = SwitchAccountResult.AccountSwitched
            viewModel.trySendAction(
                VaultAction.SwitchAccountClick(
                    accountSummary = mockk {
                        every { userId } returns "updatedUserId"
                    },
                ),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when LifecycleResumed action is handled, PromptForAppReview is sent if criteria is met`() =
        runTest {
            every { reviewPromptManager.shouldPromptForAppReview() } returns true
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAction.LifecycleResumed)
                assertEquals(VaultEvent.PromptForAppReview, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when LifecycleResumed action is handled, PromptForAppReview is not sent if criteria is not met`() =
        runTest {
            every { reviewPromptManager.shouldPromptForAppReview() } returns false
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAction.LifecycleResumed)
                expectNoEvents()
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `init should send NavigateToVerificationCodeScreen when special circumstance is VerificationCodeShortcut`() =
        runTest {
            every {
                specialCircumstanceManager.specialCircumstance
            } returns SpecialCircumstance.VerificationCodeShortcut
            every { specialCircumstanceManager.specialCircumstance = null } just runs
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAction.LifecycleResumed)
                assertEquals(
                    VaultEvent.NavigateToVerificationCodeScreen, awaitItem(),
                )
            }
            verify { specialCircumstanceManager.specialCircumstance = null }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `init should send NavigateToVaultSearchScreen when special circumstance is SearchShortcut`() =
        runTest {
            every {
                specialCircumstanceManager.specialCircumstance
            } returns SpecialCircumstance.SearchShortcut("")
            every { specialCircumstanceManager.specialCircumstance = null } just runs
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAction.LifecycleResumed)
                assertEquals(
                    VaultEvent.NavigateToVaultSearchScreen, awaitItem(),
                )
            }
        }

    @Test
    fun `SelectAddItemType action should set dialog state to SelectVaultAddItemType`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultAction.SelectAddItemType)
        val expectedState = DEFAULT_STATE.copy(
            dialog = VaultState.DialogState.SelectVaultAddItemType(
                excludedOptions = persistentListOf(CreateVaultItemType.SSH_KEY),
            ),
        )
        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `SelectAddItemType action should set dialog state to SelectVaultAddItemType accordingly when RESTRICT_ITEM_TYPES is enabled`() =
        runTest {
            val viewModel = createViewModel()
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

            viewModel.trySendAction(VaultAction.SelectAddItemType)
            val expectedState = DEFAULT_STATE.copy(
                dialog = VaultState.DialogState.SelectVaultAddItemType(
                    excludedOptions = persistentListOf(
                        CreateVaultItemType.SSH_KEY,
                        CreateVaultItemType.CARD,
                    ),
                ),
                restrictItemTypesPolicyOrgIds = listOf("Test Organization"),
            )
            assertEquals(
                expectedState,
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `InternetConnectionErrorReceived should show network error if no internet connection`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(VaultAction.Internal.InternetConnectionErrorReceived)
            assertEquals(
                DEFAULT_STATE.copy(
                    isRefreshing = false,
                    dialog = VaultState.DialogState.Error(
                        BitwardenString.internet_connection_required_title.asText(),
                        BitwardenString.internet_connection_required_message.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `DecryptionErrorReceived should show decryption error dialog`() = runTest {
        val viewModel = createViewModel()
        val throwable = Throwable("Decryption failed")
        viewModel.trySendAction(
            VaultAction.Internal.DecryptionErrorReceive(
                title = BitwardenString.decryption_error.asText(),
                message = BitwardenString.failed_to_decrypt_cipher_contact_support.asText(),
                error = throwable,
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                dialog = VaultState.DialogState.Error(
                    title = BitwardenString.decryption_error.asText(),
                    message = BitwardenString.failed_to_decrypt_cipher_contact_support.asText(),
                    error = throwable,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `UpdatedKdfToMinimumsReceived with Success should clear dialog and send a ShowSnackbar event`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    action = VaultAction.Internal.UpdatedKdfToMinimumsReceived(
                        result = UpdateKdfMinimumsResult.Success,
                    ),
                )
                assertEquals(
                    DEFAULT_STATE.copy(dialog = null),
                    viewModel.stateFlow.value,
                )
                assertEquals(
                    VaultEvent.ShowSnackbar(BitwardenString.encryption_settings_updated.asText()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `UpdatedKdfToMinimumsReceived with ActiveAccountNotFound should show error dialog`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(
                action = VaultAction.Internal.UpdatedKdfToMinimumsReceived(
                    result = UpdateKdfMinimumsResult.ActiveAccountNotFound,
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = VaultState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString
                            .kdf_update_failed_active_account_not_found
                            .asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `UpdatedKdfToMinimumsReceived with Error should show error dialog`() = runTest {
        val testError = Exception("Test error")
        val viewModel = createViewModel()
        viewModel.trySendAction(
            action = VaultAction.Internal.UpdatedKdfToMinimumsReceived(
                result = UpdateKdfMinimumsResult.Error(testError),
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                dialog = VaultState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString
                        .an_error_occurred_while_trying_to_update_your_kdf_settings
                        .asText(),
                    error = testError,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `vaultDataStateFlow Loaded with needsKdfUpdateToMinimums true should show KdfUpdateRequired dialog`() =
        runTest {
            coEvery { authRepository.needsKdfUpdateToMinimums() } returns true
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(
                            createMockCipherListView(
                                number = 1,
                                type = CipherListViewType.Login(
                                    createMockLoginListView(number = 1),
                                ),
                            ),
                            createMockCipherListView(
                                number = 2,
                                type = CipherListViewType.Login(
                                    createMockLoginListView(number = 2),
                                ),
                            ),
                        ),
                        failures = emptyList(),
                    ),
                    collectionViewList = emptyList(),
                    folderViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createViewModel()

            assertEquals(
                createMockVaultState(
                    viewState = VaultState.ViewState.Content(
                        loginItemsCount = 2,
                        cardItemsCount = 0,
                        identityItemsCount = 0,
                        secureNoteItemsCount = 0,
                        favoriteItems = listOf(),
                        folderItems = listOf(),
                        collectionItems = listOf(),
                        noFolderItems = listOf(),
                        trashItemsCount = 0,
                        totpItemsCount = 2,
                        itemTypesCount = 5,
                        sshKeyItemsCount = 0,
                        showCardGroup = true,
                    ),
                    dialog = VaultState.DialogState.VaultLoadKdfUpdateRequired(
                        title = BitwardenString.update_your_encryption_settings.asText(),
                        message = BitwardenString.the_new_recommended_encryption_settings_will_improve_your_account_desc_long.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `vaultDataStateFlow Loaded with needsKdfUpdateToMinimums false should not show KdfUpdateRequired dialog`() =
        runTest {
            coEvery { authRepository.needsKdfUpdateToMinimums() } returns false
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = createMockDecryptCipherListResult(
                        number = 1,
                        successes = listOf(
                            createMockCipherListView(
                                number = 1,
                                type = CipherListViewType.Login(
                                    createMockLoginListView(number = 1),
                                ),
                            ),
                            createMockCipherListView(
                                number = 2,
                                type = CipherListViewType.Login(
                                    createMockLoginListView(number = 2),
                                ),
                            ),
                        ),
                        failures = emptyList(),
                    ),
                    collectionViewList = emptyList(),
                    folderViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createViewModel()

            assertEquals(
                createMockVaultState(
                    viewState = VaultState.ViewState.Content(
                        loginItemsCount = 2,
                        cardItemsCount = 0,
                        identityItemsCount = 0,
                        secureNoteItemsCount = 0,
                        favoriteItems = listOf(),
                        folderItems = listOf(),
                        collectionItems = listOf(),
                        noFolderItems = listOf(),
                        trashItemsCount = 0,
                        totpItemsCount = 2,
                        itemTypesCount = 5,
                        sshKeyItemsCount = 0,
                        showCardGroup = true,
                    ),
                    dialog = null,
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `on KdfUpdatePasswordRepromptSubmit should call updateKdfToMinimumsIfNeeded`() = runTest {
        val password = "mock_password"
        coEvery {
            authRepository.updateKdfToMinimumsIfNeeded(password)
        } returns UpdateKdfMinimumsResult.Success

        val viewModel = createViewModel()

        viewModel.trySendAction(
            action = VaultAction.KdfUpdatePasswordRepromptSubmit(password = password),
        )

        coVerify(exactly = 1) {
            authRepository.updateKdfToMinimumsIfNeeded(password)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CredentialExchangeProtocolExportFlagUpdateReceive should register for export when flag is enabled`() =
        runTest {
            mutableCxpExportFeatureFlagFlow.value = false
            coEvery { credentialExchangeRegistryManager.register() } just awaits

            val viewModel = createViewModel()

            viewModel.trySendAction(
                VaultAction.Internal.CredentialExchangeProtocolExportFlagUpdateReceive(
                    isCredentialExchangeProtocolExportEnabled = true,
                ),
            )

            coVerify {
                credentialExchangeRegistryManager.register()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `CredentialExchangeProtocolExportFlagUpdateReceive should unregister when flag is disabled`() =
        runTest {
            mutableCxpExportFeatureFlagFlow.value = true
            every { settingsRepository.isAppRegisteredForExport() } returns true
            coEvery { credentialExchangeRegistryManager.unregister() } just awaits

            val viewModel = createViewModel()

            viewModel.trySendAction(
                VaultAction.Internal.CredentialExchangeProtocolExportFlagUpdateReceive(
                    isCredentialExchangeProtocolExportEnabled = false,
                ),
            )

            coVerify {
                credentialExchangeRegistryManager.unregister()
            }
        }

    private fun createViewModel(): VaultViewModel =
        VaultViewModel(
            authRepository = authRepository,
            clipboardManager = clipboardManager,
            policyManager = policyManager,
            clock = clock,
            settingsRepository = settingsRepository,
            vaultRepository = vaultRepository,
            organizationEventManager = organizationEventManager,
            firstTimeActionManager = firstTimeActionManager,
            snackbarRelayManager = snackbarRelayManager,
            reviewPromptManager = reviewPromptManager,
            specialCircumstanceManager = specialCircumstanceManager,
            networkConnectionManager = networkConnectionManager,
            browserAutofillDialogManager = browserAutofillDialogManager,
            credentialExchangeRegistryManager = credentialExchangeRegistryManager,
            featureFlagManager = featureFlagManager,
        )
}

private val ORGANIZATION_VAULT_FILTER = VaultFilterType.OrganizationVault(
    organizationId = "testOrganizationId",
    organizationName = "Test Organization",
)

private val VAULT_FILTER_DATA = VaultFilterData(
    selectedVaultFilterType = VaultFilterType.AllVaults,
    vaultFilterTypes = listOf(
        VaultFilterType.AllVaults,
        VaultFilterType.MyVault,
        ORGANIZATION_VAULT_FILTER,
    ),
)

private val DEFAULT_STATE: VaultState =
    createMockVaultState(viewState = VaultState.ViewState.Loading)

private val DEFAULT_FIRST_TIME_STATE = FirstTimeState(
    showImportLoginsCard = true,
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = "activeUserId",
    accounts = listOf(
        UserState.Account(
            userId = "activeUserId",
            name = "Active User",
            email = "active@bitwarden.com",
            avatarColorHex = "#aa00aa",
            environment = Environment.Us,
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
            firstTimeState = DEFAULT_FIRST_TIME_STATE,
            isExportable = true,
        ),
        UserState.Account(
            userId = "lockedUserId",
            name = "Locked User",
            email = "locked@bitwarden.com",
            avatarColorHex = "#00aaaa",
            environment = Environment.Us,
            isPremium = false,
            isLoggedIn = true,
            isVaultUnlocked = false,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = emptyList(),
            needsMasterPassword = false,
            trustedDevice = null,
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            onboardingStatus = OnboardingStatus.COMPLETE,
            firstTimeState = DEFAULT_FIRST_TIME_STATE,
            isExportable = true,
        ),
    ),
)

private fun createMockVaultState(
    viewState: VaultState.ViewState,
    dialog: VaultState.DialogState? = null,
): VaultState =
    VaultState(
        appBarTitle = BitwardenString.my_vault.asText(),
        avatarColorString = "#aa00aa",
        initials = "AU",
        accountSummaries = listOf(
            AccountSummary(
                userId = "activeUserId",
                name = "Active User",
                email = "active@bitwarden.com",
                avatarColorHex = "#aa00aa",
                environmentLabel = "bitwarden.com",
                isActive = true,
                isLoggedIn = true,
                isVaultUnlocked = true,
            ),
            AccountSummary(
                userId = "lockedUserId",
                name = "Locked User",
                email = "locked@bitwarden.com",
                avatarColorHex = "#00aaaa",
                environmentLabel = "bitwarden.com",
                isActive = false,
                isLoggedIn = true,
                isVaultUnlocked = false,
            ),
        ),
        viewState = viewState,
        dialog = dialog,
        isSwitchingAccounts = false,
        isPremium = true,
        isPullToRefreshSettingEnabled = false,
        baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
        isIconLoadingDisabled = false,
        hasMasterPassword = true,
        showImportActionCard = true,
        isRefreshing = false,
        flightRecorderSnackBar = null,
        cipherDecryptionFailureIds = persistentListOf(),
        hasShownDecryptionFailureAlert = false,
        restrictItemTypesPolicyOrgIds = emptyList(),
    )
