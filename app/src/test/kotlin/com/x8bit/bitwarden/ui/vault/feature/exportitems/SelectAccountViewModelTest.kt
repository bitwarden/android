package com.x8bit.bitwarden.ui.vault.feature.exportitems

import app.cash.turbine.test
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.ui.vault.feature.exportitems.model.AccountSelectionListItem
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.SelectAccountAction
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.SelectAccountEvent
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.SelectAccountState
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.SelectAccountViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SelectAccountViewModelTest : BaseViewModelTest() {

    private val mutableUserUserStateFlow = MutableStateFlow(DEFAULT_USER_STATE)
    private val mutableRestrictItemTypesFlow =
        bufferedMutableSharedFlow<List<SyncResponseJson.Policy>>()
    private val mutablePersonalOwnershipPolicyFlow =
        bufferedMutableSharedFlow<List<SyncResponseJson.Policy>>()

    private val authRepository = mockk<AuthRepository> {
        every { userStateFlow } returns mutableUserUserStateFlow
    }
    private val policyManager = mockk<PolicyManager> {
        every {
            getActivePoliciesFlow(PolicyTypeJson.RESTRICT_ITEM_TYPES)
        } returns mutableRestrictItemTypesFlow
        every {
            getActivePoliciesFlow(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns mutablePersonalOwnershipPolicyFlow
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        assertEquals(
            SelectAccountState(viewState = SelectAccountState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `state is updated when single account with no organization is emitted`() = runTest {
        val viewModel = createViewModel()
        val expectedItem = AccountSelectionListItem(
            userId = DEFAULT_ACCOUNT.userId,
            email = DEFAULT_ACCOUNT.email,
            initials = "AU",
            avatarColorHex = "#FF00FF00",
            isItemRestricted = false,
        )

        viewModel.trySendAction(
            SelectAccountAction.Internal.SelectionDataReceive(
                userState = DEFAULT_USER_STATE,
                itemRestrictedOrgs = emptyList(),
                personalOwnershipOrgs = emptyList(),
            ),
        )

        assertEquals(
            SelectAccountState(
                viewState = SelectAccountState.ViewState.Content(
                    accountSelectionListItems = persistentListOf(expectedItem),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `accounts in organization with PERSONAL_VAULT policy are filtered out`() = runTest {
        val viewModel = createViewModel()
        val organizationId = "mockOrganizationId-1"
        val accountInOrg = DEFAULT_ACCOUNT.copy(
            organizations = listOf(
                Organization(
                    id = organizationId,
                    name = "organizationName",
                    shouldManageResetPassword = false,
                    shouldUseKeyConnector = false,
                    role = OrganizationType.ADMIN,
                    keyConnectorUrl = null,
                    userIsClaimedByOrganization = false,
                ),
            ),
        )

        viewModel.trySendAction(
            SelectAccountAction.Internal.SelectionDataReceive(
                userState = DEFAULT_USER_STATE.copy(accounts = listOf(accountInOrg)),
                itemRestrictedOrgs = emptyList(),
                personalOwnershipOrgs = listOf(
                    createMockPolicy(
                        number = 1,
                        id = organizationId,
                        isEnabled = true,
                    ),
                ),
            ),
        )
        assertEquals(
            SelectAccountState(viewState = SelectAccountState.ViewState.NoItems),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `accounts in organization with RESTRICT_ITEM_TYPE policy are marked as restricted`() =
        runTest {
            val viewModel = createViewModel()
            val organizationId = "mockOrganizationId-1"
            val accountInOrg = DEFAULT_ACCOUNT.copy(
                organizations = listOf(
                    Organization(
                        id = organizationId,
                        name = "organizationName",
                        shouldManageResetPassword = false,
                        shouldUseKeyConnector = false,
                        role = OrganizationType.ADMIN,
                        keyConnectorUrl = null,
                        userIsClaimedByOrganization = false,
                    ),
                ),
            )
            val expectedItem = AccountSelectionListItem(
                userId = accountInOrg.userId,
                email = accountInOrg.email,
                initials = "AU",
                avatarColorHex = "#FF00FF00",
                isItemRestricted = true,
            )

            viewModel.trySendAction(
                SelectAccountAction.Internal.SelectionDataReceive(
                    userState = DEFAULT_USER_STATE.copy(accounts = listOf(accountInOrg)),
                    itemRestrictedOrgs = listOf(
                        createMockPolicy(
                            number = 1,
                            id = organizationId,
                            isEnabled = true,
                        ),
                    ),
                    personalOwnershipOrgs = emptyList(),
                ),
            )
            assertEquals(
                SelectAccountState(
                    viewState = SelectAccountState.ViewState.Content(
                        accountSelectionListItems = persistentListOf(expectedItem),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `when CloseClick action is sent, CancelExport event is emitted`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SelectAccountAction.CloseClick)
            assertEquals(SelectAccountEvent.CancelExport, awaitItem())
        }
    }

    @Test
    fun `when AccountClick action is sent, NavigateToPasswordVerification event is emitted`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    SelectAccountAction.AccountClick(
                        DEFAULT_ACCOUNT.userId,
                    ),
                )
                assertEquals(
                    SelectAccountEvent.NavigateToPasswordVerification(
                        userId = DEFAULT_ACCOUNT.userId,
                    ),
                    awaitItem(),
                )
            }
        }

    private fun createViewModel(): SelectAccountViewModel = SelectAccountViewModel(
        authRepository = authRepository,
        policyManager = policyManager,
    )
}

private val DEFAULT_ACCOUNT = UserState.Account(
    userId = "activeUserId",
    name = "Active User",
    email = "active@bitwarden.com",
    environment = Environment.Us,
    avatarColorHex = "#FF00FF00",
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
