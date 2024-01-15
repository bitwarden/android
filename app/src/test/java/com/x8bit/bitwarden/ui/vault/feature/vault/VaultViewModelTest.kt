package com.x8bit.bitwarden.ui.vault.feature.vault

import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.UserState.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toViewState
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class VaultViewModelTest : BaseViewModelTest() {

    private val mutablePullToRefreshEnabledFlow = MutableStateFlow(false)

    private val mutableUserStateFlow =
        MutableStateFlow<UserState?>(DEFAULT_USER_STATE)

    private val mutableVaultDataStateFlow =
        MutableStateFlow<DataState<VaultData>>(DataState.Loading)

    private var switchAccountResult: SwitchAccountResult = SwitchAccountResult.NoChange

    private val authRepository: AuthRepository =
        mockk {
            every { userStateFlow } returns mutableUserStateFlow
            every { specialCircumstance } returns null
            every { specialCircumstance = any() } just runs
            every { logout(any()) } just runs
            every { switchAccount(any()) } answers { switchAccountResult }
        }

    private val settingsRepository: SettingsRepository = mockk {
        every { getPullToRefreshEnabledFlow() } returns mutablePullToRefreshEnabledFlow
    }

    private val vaultRepository: VaultRepository =
        mockk {
            every { vaultDataStateFlow } returns mutableVaultDataStateFlow
            every { sync() } just runs
            every { lockVaultForCurrentUser() } just runs
            every { lockVault(any()) } just runs
        }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
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
                accountSummary = mockk() {
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
    fun `UserState updates with a non-null value when not switching accounts should update the account information in the state`() {
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
                        organizations = listOf(
                            Organization(
                                id = "organiationId",
                                name = "Test Organization",
                            ),
                        ),
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
    fun `on AddAccountClick should update the SpecialCircumstance of the AuthRepository to PendingAccountAddition`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultAction.AddAccountClick)
        verify {
            authRepository.specialCircumstance = SpecialCircumstance.PendingAccountAddition
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
            vaultRepository.sync()
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
            ),
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

        assertEquals(
            initialState.copy(
                vaultFilterData = VAULT_FILTER_DATA.copy(
                    selectedVaultFilterType = VaultFilterType.MyVault,
                ),
                viewState = vaultData.toViewState(
                    isPremium = true,
                    vaultFilterType = VaultFilterType.MyVault,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Loaded with items should update state to Content`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Loaded(
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
                    totpItemsCount = 1,
                ),
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
                    totpItemsCount = 1,
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
                assertEquals(VaultEvent.DismissPullToRefresh, awaitItem())
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
                assertEquals(VaultEvent.DismissPullToRefresh, awaitItem())
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
                    totpItemsCount = 1,
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
                        totpItemsCount = 1,
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
    fun `vaultDataStateFlow NoNetwork without data should update state to Error`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.NoNetwork(),
        )

        val viewModel = createViewModel()

        assertEquals(
            createMockVaultState(
                viewState = VaultState.ViewState.Error(
                    message = R.string.internet_connection_required_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow NoNetwork with items should update state to Content and show an error dialog`() =
        runTest {
            mutableVaultDataStateFlow.tryEmit(
                value = DataState.NoNetwork(
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
                        totpItemsCount = 1,
                    ),
                    dialog = VaultState.DialogState.Error(
                        title = R.string.internet_connection_required_title.asText(),
                        message = R.string.internet_connection_required_message.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow NoNetwork with empty items should update state to NoItems and show an error dialog`() =
        runTest {
            mutableVaultDataStateFlow.tryEmit(
                value = DataState.NoNetwork(
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
                        title = R.string.internet_connection_required_title.asText(),
                        message = R.string.internet_connection_required_message.asText(),
                    ),
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
                accountSummary = mockk() {
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

        verify { vaultRepository.sync() }
    }

    @Test
    fun `DialogDismiss should clear the active dialog`() {
        // Show the No Network error dialog
        val viewModel = createViewModel()
        mutableVaultDataStateFlow.value = DataState.NoNetwork(
            data = VaultData(
                cipherViewList = emptyList(),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )
        val initialState = DEFAULT_STATE.copy(
            viewState = VaultState.ViewState.NoItems,
            dialog = VaultState.DialogState.Error(
                title = R.string.internet_connection_required_title.asText(),
                message = R.string.internet_connection_required_message.asText(),
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
            vaultRepository.sync()
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

    private fun createViewModel(): VaultViewModel =
        VaultViewModel(
            authRepository = authRepository,
            settingsRepository = settingsRepository,
            vaultRepository = vaultRepository,
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
            organizations = emptyList(),
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
            organizations = emptyList(),
        ),
    ),
)

private fun createMockVaultState(
    viewState: VaultState.ViewState,
    dialog: VaultState.DialogState? = null,
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
    )
