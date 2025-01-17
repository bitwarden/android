package com.x8bit.bitwarden.ui.vault.feature.vault

import app.cash.turbine.test
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.vault.datasource.network.model.OrganizationType
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toViewState
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
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

    private val mutableSnackbarDataFlow = MutableStateFlow<BitwardenSnackbarData?>(null)
    private val snackbarRelayManager: SnackbarRelayManager = mockk {
        every { getSnackbarDataFlow(SnackbarRelay.MY_VAULT_RELAY) } returns mutableSnackbarDataFlow
            .filterNotNull()
        every { clearRelayBuffer(SnackbarRelay.MY_VAULT_RELAY) } just runs
    }

    private val clipboardManager: BitwardenClipboardManager = mockk {
        every { setText(any<String>()) } just runs
    }
    private val policyManager: PolicyManager = mockk {
        every {
            getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns emptyList()
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
            every { logout(any()) } just runs
            every { switchAccount(any()) } answers { switchAccountResult }
        }

    private val settingsRepository: SettingsRepository = mockk {
        every { getPullToRefreshEnabledFlow() } returns mutablePullToRefreshEnabledFlow
        every { isIconLoadingDisabledFlow } returns mutableIsIconLoadingDisabledFlow
        every { isIconLoadingDisabled } returns false
    }

    private val vaultRepository: VaultRepository =
        mockk {
            every { vaultFilterType = any() } just runs
            every { vaultDataStateFlow } returns mutableVaultDataStateFlow
            every { sync(forced = any()) } just runs
            every { syncIfNecessary() } just runs
            every { lockVaultForCurrentUser() } just runs
            every { lockVault(any()) } just runs
        }

    private val organizationEventManager = mockk<OrganizationEventManager> {
        every { trackEvent(event = any()) } just runs
    }

    private val mutableImportLoginsFeatureFlow = MutableStateFlow(true)
    private val mutableSshKeyVaultItemsEnabledFlow = MutableStateFlow(false)
    private val featureFlagManager: FeatureFlagManager = mockk {
        every {
            getFeatureFlagFlow(FlagKey.ImportLoginsFlow)
        } returns mutableImportLoginsFeatureFlow
        every {
            getFeatureFlagFlow(FlagKey.SshKeyCipherItems)
        } returns mutableSshKeyVaultItemsEnabledFlow
        every {
            getFeatureFlag(FlagKey.SshKeyCipherItems)
        } returns mutableSshKeyVaultItemsEnabledFlow.value
    }
    private val reviewPromptManager: ReviewPromptManager = mockk()

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

    @Suppress("MaxLineLength")
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
                                shouldUsersGetPremium = false,
                            ),
                        ),
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = DEFAULT_FIRST_TIME_STATE,
                    ),
                ),
            )

        assertEquals(
            DEFAULT_STATE.copy(
                appBarTitle = R.string.vaults.asText(),
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
            SyncResponseJson.Policy(
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
                                shouldUsersGetPremium = false,
                            ),
                        ),
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = DEFAULT_FIRST_TIME_STATE,
                    ),
                ),
            )

        assertEquals(
            DEFAULT_STATE.copy(
                appBarTitle = R.string.vaults.asText(),
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

    @Test
    fun `on LockAccountClick should call lockVault for the given account`() {
        val accountUserId = "userId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns accountUserId
            every { isActive } returns false
        }
        val viewModel = createViewModel()

        viewModel.trySendAction(VaultAction.LockAccountClick(accountSummary))

        verify { vaultRepository.lockVault(userId = accountUserId) }
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
        verify { authRepository.logout(userId = accountUserId) }
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
        verify { authRepository.logout(userId = accountUserId) }
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
    fun `on LockClick should call lockVaultForCurrentUser on the VaultRepository`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultAction.LockClick)
        verify {
            vaultRepository.lockVaultForCurrentUser()
        }
    }

    @Test
    fun `on ExitConfirmationClick should emit NavigateOutOfApp`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.ExitConfirmationClick)
            assertEquals(VaultEvent.NavigateOutOfApp, awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on VaultFilterTypeSelect should update the selected filter type and re-filter any existing data`() {
        // Update to state with filters and content
        val vaultData = VaultData(
            cipherViewList = listOf(createMockCipherView(number = 1)),
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
                                shouldUsersGetPremium = true,
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
                showSshKeys = false,
                organizationPremiumStatusMap = mapOf("testOrganizationId" to true),
            ),
            organizationPremiumStatusMap = mapOf("testOrganizationId" to true),
        )
            .copy(
                appBarTitle = R.string.vaults.asText(),
                vaultFilterData = VAULT_FILTER_DATA,
            )
        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(VaultAction.VaultFilterTypeSelect(VaultFilterType.MyVault))

        val resultingState = initialState.copy(
            vaultFilterData = VAULT_FILTER_DATA.copy(
                selectedVaultFilterType = VaultFilterType.MyVault,
            ),
            viewState = vaultData.toViewState(
                isPremium = true,
                vaultFilterType = VaultFilterType.MyVault,
                isIconLoadingDisabled = viewModel.stateFlow.value.isIconLoadingDisabled,
                baseIconUrl = viewModel.stateFlow.value.baseIconUrl,
                hasMasterPassword = true,
                showSshKeys = false,
                organizationPremiumStatusMap = mapOf("testOrganizationId" to true),
            ),
        )
        assertEquals(
            resultingState,
            viewModel.stateFlow.value,
        )
        verify { vaultRepository.vaultFilterType = VaultFilterType.MyVault }
    }

    @Test
    fun `vaultDataStateFlow Loaded with items should update state to Content`() = runTest {
        mutableSshKeyVaultItemsEnabledFlow.value = true
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(
                        createMockCipherView(number = 1, cipherType = CipherType.LOGIN),
                        createMockCipherView(number = 2, cipherType = CipherType.CARD),
                        createMockCipherView(number = 3, cipherType = CipherType.IDENTITY),
                        createMockCipherView(number = 4, cipherType = CipherType.SECURE_NOTE),
                        createMockCipherView(number = 5, cipherType = CipherType.SSH_KEY),
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
                    totpItemsCount = 0,
                    itemTypesCount = CipherType.entries.size,
                    sshKeyItemsCount = 1,
                ),
                showSshKeys = true,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with items when manually syncing with the sync button should update state to Content, show a success Toast, and dismiss pull to refresh`() =
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
                    totpItemsCount = 0,
                    itemTypesCount = 4,
                    sshKeyItemsCount = 0,
                ),
            )
            val viewModel = createViewModel()
            viewModel.trySendAction(VaultAction.SyncClick)

            viewModel.eventFlow.test {
                mutableVaultDataStateFlow.tryEmit(
                    value = DataState.Loaded(
                        data = VaultData(
                            cipherViewList = listOf(createMockCipherView(number = 1)),
                            collectionViewList = emptyList(),
                            folderViewList = emptyList(),
                            sendViewList = emptyList(),
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
                assertEquals(
                    VaultEvent.ShowToast(R.string.syncing_complete.asText()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `vaultDataStateFlow Loaded with empty items should update state to NoItems`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = emptyList(),
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

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with empty items when manually syncing with the sync button should update state to NoItems, show a success Toast, and dismiss pull to refresh`() =
        runTest {
            val expectedState = createMockVaultState(
                viewState = VaultState.ViewState.NoItems,
            )
            val viewModel = createViewModel()
            viewModel.trySendAction(VaultAction.SyncClick)

            viewModel.eventFlow.test {
                mutableVaultDataStateFlow.value = DataState.Loaded(
                    data = VaultData(
                        cipherViewList = emptyList(),
                        collectionViewList = emptyList(),
                        folderViewList = emptyList(),
                        sendViewList = emptyList(),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
                assertEquals(
                    VaultEvent.ShowToast(R.string.syncing_complete.asText()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `vaultDataStateFlow Pending with items should update state to Content`() {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Pending(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1)),
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
                    totpItemsCount = 0,
                    itemTypesCount = 4,
                    sshKeyItemsCount = 0,
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
                    cipherViewList = emptyList(),
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
                    message = R.string.generic_error_message.asText(),
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
                        cipherViewList = listOf(createMockCipherView(number = 1)),
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
                        totpItemsCount = 0,
                        itemTypesCount = 4,
                        sshKeyItemsCount = 0,
                    ),
                    dialog = VaultState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.generic_error_message.asText(),
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
                        cipherViewList = emptyList(),
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
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.generic_error_message.asText(),
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

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow NoNetwork with items should update state to Content`() =
        runTest {
            mutableVaultDataStateFlow.tryEmit(
                value = DataState.NoNetwork(
                    data = VaultData(
                        cipherViewList = listOf(
                            createMockCipherView(
                                number = 1,
                                organizationUsesTotp = true,
                            ),
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
                        itemTypesCount = 4,
                        sshKeyItemsCount = 0,
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
                cipherViewList = emptyList(),
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
    fun `vaultDataStateFlow Loaded should exclude SSH key vault items when showSshKeys is false`() =
        runTest {
            mutableVaultDataStateFlow.tryEmit(
                value = DataState.Loaded(
                    data = VaultData(
                        cipherViewList = listOf(
                            createMockCipherView(number = 1),
                            createMockCipherView(number = 1, cipherType = CipherType.SSH_KEY),
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
                        totpItemsCount = 0,
                        itemTypesCount = CipherType.entries.size - 1,
                        sshKeyItemsCount = 0,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `vaultDataStateFlow Loaded should include SSH key vault items when showSshKeys is true`() =
        runTest {
            mutableSshKeyVaultItemsEnabledFlow.value = true
            mutableVaultDataStateFlow.tryEmit(
                value = DataState.Loaded(
                    data = VaultData(
                        cipherViewList = listOf(
                            createMockCipherView(number = 1),
                            createMockCipherView(number = 1, cipherType = CipherType.SSH_KEY),
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
                        totpItemsCount = 0,
                        itemTypesCount = CipherType.entries.size,
                        sshKeyItemsCount = 1,
                    ),
                    showSshKeys = true,
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
    fun `AddItemClick should emit NavigateToAddItemScreen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.AddItemClick)
            assertEquals(VaultEvent.NavigateToAddItemScreen, awaitItem())
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
        }
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.VaultItemClick(item))
            assertEquals(VaultEvent.NavigateToVaultItem(itemId), awaitItem())
        }
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
                cipherViewList = emptyList(),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )
        val viewModel = createViewModel()
        val initialState = DEFAULT_STATE.copy(
            viewState = VaultState.ViewState.NoItems,
            dialog = VaultState.DialogState.Error(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.generic_error_message.asText(),
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

    @Test
    fun `RefreshPull should call vault repository sync`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(VaultAction.RefreshPull)

        verify(exactly = 1) {
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
            val notes = "notes"
            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyNoteClick(notes = notes),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(notes)
            }
        }

    @Test
    fun `OverflowOptionClick Vault CopyNumberClick should call setText on the ClipboardManager`() =
        runTest {
            val number = "12345-4321-9876-6789"
            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyNumberClick(
                        number = number,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(number)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyPasswordClick should call setText on the ClipboardManager`() =
        runTest {
            val password = "passTheWord"
            val cipherId = "cipherId"
            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                        password = password,
                        requiresPasswordReprompt = true,
                        cipherId = cipherId,
                    ),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(password)
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientCopiedPassword(cipherId = cipherId),
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

            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyTotpClick(totpCode),
                ),
            )

            verify(exactly = 1) {
                clipboardManager.setText(code)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyTotpClick with GenerateTotpCode failure should not call setText on the ClipboardManager`() =
        runTest {
            val totpCode = "totpCode"

            coEvery {
                vaultRepository.generateTotp(totpCode, clock.instant())
            } returns GenerateTotpResult.Error

            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyTotpClick(totpCode),
                ),
            )

            verify(exactly = 0) {
                clipboardManager.setText(text = any<String>())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopySecurityCodeClick should call setText on the ClipboardManager`() =
        runTest {
            val securityCode = "234"
            val cipherId = "cipherId"
            val viewModel = createViewModel()
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopySecurityCodeClick(
                        securityCode = securityCode,
                        cipherId = cipherId,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(securityCode)
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientCopiedCardCode(cipherId = cipherId),
                )
            }
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
                clipboardManager.setText(username)
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
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            assertEquals(VaultEvent.NavigateToEditVaultItem(cipherId), awaitItem())
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
    fun `OverflowOptionClick Vault ViewClick should emit NavigateToUrl`() = runTest {
        val cipherId = "cipherId-9876"
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = cipherId),
                ),
            )
            assertEquals(VaultEvent.NavigateToVaultItem(cipherId), awaitItem())
        }
    }

    @Test
    fun `MasterPasswordRepromptSubmit for a request Error should show a generic error dialog`() =
        runTest {
            val password = "password"
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Error

            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE,
                    awaitItem(),
                )

                viewModel.trySendAction(
                    VaultAction.MasterPasswordRepromptSubmit(
                        overflowAction = ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                            password = password,
                            requiresPasswordReprompt = true,
                            cipherId = "cipherId",
                        ),
                        password = password,
                    ),
                )

                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = VaultState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
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
                        overflowAction = ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                            password = password,
                            requiresPasswordReprompt = true,
                            cipherId = "cipherId",
                        ),
                        password = password,
                    ),
                )

                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = VaultState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.invalid_master_password.asText(),
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
            val cipherId = "cipherId"
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)

            val viewModel = createViewModel()

            viewModel.trySendAction(
                VaultAction.MasterPasswordRepromptSubmit(
                    overflowAction = ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                        password = password,
                        requiresPasswordReprompt = true,
                        cipherId = cipherId,
                    ),
                    password = password,
                ),
            )

            verify(exactly = 1) {
                clipboardManager.setText(password)
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientCopiedPassword(cipherId = cipherId),
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
    fun `when feature flag ImportLoginsFlow is disabled, should show action card should always be false`() =
        runTest {
            mutableImportLoginsFeatureFlow.update { false }
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE.copy(showImportActionCard = false),
                    awaitItem(),
                )
                mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                    accounts = DEFAULT_USER_STATE.accounts.map {
                        it.copy(
                            firstTimeState = DEFAULT_FIRST_TIME_STATE.copy(
                                showImportLoginsCard = true,
                            ),
                        )
                    },
                )
                expectNoEvents()
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
        mutableSnackbarDataFlow.update { expectedSnackbarData }
        viewModel.eventFlow.test {
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
            verify(exactly = 1) {
                snackbarRelayManager.clearRelayBuffer(SnackbarRelay.MY_VAULT_RELAY)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when LifecycleResumed action is handled, PromptForAppReview is sent if flag is enabled and criteria is met`() =
        runTest {
            every { featureFlagManager.getFeatureFlag(FlagKey.AppReviewPrompt) } returns true
            every { reviewPromptManager.shouldPromptForAppReview() } returns true
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAction.LifecycleResumed)
                assertEquals(VaultEvent.PromptForAppReview, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when LifecycleResumed action is handled, PromptForAppReview is not sent if flag is disabled`() =
        runTest {
            every { featureFlagManager.getFeatureFlag(FlagKey.AppReviewPrompt) } returns false
            every { reviewPromptManager.shouldPromptForAppReview() } returns true
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAction.LifecycleResumed)
                expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when LifecycleResumed action is handled, PromptForAppReview is not sent if criteria is not met`() =
        runTest {
            every { featureFlagManager.getFeatureFlag(FlagKey.AppReviewPrompt) } returns true
            every { reviewPromptManager.shouldPromptForAppReview() } returns false
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAction.LifecycleResumed)
                expectNoEvents()
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
            featureFlagManager = featureFlagManager,
            firstTimeActionManager = firstTimeActionManager,
            snackbarRelayManager = snackbarRelayManager,
            reviewPromptManager = reviewPromptManager,
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
        ),
    ),
)

private fun createMockVaultState(
    viewState: VaultState.ViewState,
    dialog: VaultState.DialogState? = null,
    showSshKeys: Boolean = false,
    organizationPremiumStatusMap: Map<String, Boolean> = emptyMap(),
): VaultState =
    VaultState(
        appBarTitle = R.string.my_vault.asText(),
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
        showSshKeys = showSshKeys,
        organizationPremiumStatusMap = organizationPremiumStatusMap,
    )
