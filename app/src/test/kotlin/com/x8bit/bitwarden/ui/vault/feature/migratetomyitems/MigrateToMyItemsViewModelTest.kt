package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MigrateToMyItemsViewModelTest : BaseViewModelTest() {

    private val mutableUserStateFlow = MutableStateFlow<UserState?>(MOCK_USER_STATE)
    private val authRepository = mockk<AuthRepository> {
        every { userStateFlow } returns mutableUserStateFlow
    }

    private val policyManager = mockk<PolicyManager> {
        every { getPersonalOwnershipPolicyOrganizationId() } returns ORGANIZATION_ID
    }

    @Test
    fun `initial state should be set from organization data`() {
        val viewModel = createViewModel()
        assertEquals(ORGANIZATION_NAME, viewModel.stateFlow.value.organizationName)
        assertNull(viewModel.stateFlow.value.dialog)
    }

    @Test
    fun `AcceptClicked should show loading dialog and trigger migration`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(null, awaitItem().dialog)

            viewModel.trySendAction(MigrateToMyItemsAction.AcceptClicked)

            val loadingState = awaitItem()
            assert(loadingState.dialog is MigrateToMyItemsState.DialogState.Loading)
            assertEquals(
                BitwardenString.migrating_items_to_x.asText(ORGANIZATION_NAME),
                (loadingState.dialog as MigrateToMyItemsState.DialogState.Loading).message,
            )
        }
    }

    @Test
    fun `AcceptClicked should navigate to vault on success`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(MigrateToMyItemsAction.AcceptClicked)
            assertEquals(MigrateToMyItemsEvent.NavigateToVault, awaitItem())
        }
    }

    @Test
    fun `MigrateToMyItemsResultReceived with success should clear dialog and navigate to vault`() =
        runTest {
            val viewModel = createViewModel()

            // First show the loading dialog
            viewModel.trySendAction(MigrateToMyItemsAction.AcceptClicked)

            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    MigrateToMyItemsAction.Internal.MigrateToMyItemsResultReceived(
                        success = true,
                    ),
                )
                assertEquals(MigrateToMyItemsEvent.NavigateToVault, awaitItem())
            }

            assertNull(viewModel.stateFlow.value.dialog)
        }

    @Test
    fun `MigrateToMyItemsResultReceived with failure should show error dialog`() = runTest {
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            awaitItem() // Initial state

            viewModel.trySendAction(
                MigrateToMyItemsAction.Internal.MigrateToMyItemsResultReceived(
                    success = false,
                ),
            )

            val errorState = awaitItem()
            assert(errorState.dialog is MigrateToMyItemsState.DialogState.Error)
            val errorDialog = errorState.dialog as MigrateToMyItemsState.DialogState.Error
            assertEquals(BitwardenString.an_error_has_occurred.asText(), errorDialog.title)
            assertEquals(
                BitwardenString.failed_to_migrate_items_to_x.asText(ORGANIZATION_NAME),
                errorDialog.message,
            )
        }
    }

    @Test
    fun `DeclineAndLeaveClicked sends NavigateToLeaveOrganization event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(MigrateToMyItemsAction.DeclineAndLeaveClicked)
            assertEquals(MigrateToMyItemsEvent.NavigateToLeaveOrganization, awaitItem())
        }
    }

    @Test
    fun `HelpLinkClicked sends LaunchUri event with help URL`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(MigrateToMyItemsAction.HelpLinkClicked)
            val event = awaitItem()
            assert(event is MigrateToMyItemsEvent.LaunchUri)
            assertEquals(
                "https://bitwarden.com/help/transfer-ownership/",
                (event as MigrateToMyItemsEvent.LaunchUri).uri,
            )
        }
    }

    @Test
    fun `DismissDialogClicked should clear dialog`() = runTest {
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            awaitItem() // Initial state

            // First show an error dialog
            viewModel.trySendAction(
                MigrateToMyItemsAction.Internal.MigrateToMyItemsResultReceived(
                    success = false,
                ),
            )
            val errorState = awaitItem()
            assert(errorState.dialog is MigrateToMyItemsState.DialogState.Error)

            // Dismiss the dialog
            viewModel.trySendAction(MigrateToMyItemsAction.DismissDialogClicked)
            val clearedState = awaitItem()
            assertNull(clearedState.dialog)
        }
    }

    @Test
    fun `initial state should handle null organization ID`() {
        every { policyManager.getPersonalOwnershipPolicyOrganizationId() } returns null
        val viewModel = createViewModel()

        assertEquals("", viewModel.stateFlow.value.organizationId)
        assertEquals("", viewModel.stateFlow.value.organizationName)
        assertNull(viewModel.stateFlow.value.dialog)
    }

    @Test
    fun `initial state should handle null organization name`() {
        mutableUserStateFlow.value = MOCK_USER_STATE.copy(
            accounts = listOf(
                MOCK_USER_STATE.activeAccount.copy(organizations = emptyList()),
            ),
        )
        val viewModel = createViewModel()

        assertEquals(ORGANIZATION_ID, viewModel.stateFlow.value.organizationId)
        assertEquals("", viewModel.stateFlow.value.organizationName)
        assertNull(viewModel.stateFlow.value.dialog)
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): MigrateToMyItemsViewModel =
        MigrateToMyItemsViewModel(
            authRepository = authRepository,
            policyManager = policyManager,
            savedStateHandle = savedStateHandle,
        )
}

private const val ORGANIZATION_ID = "test-organization-id"
private const val ORGANIZATION_NAME = "Test Organization"

private val MOCK_USER_STATE = UserState(
    activeUserId = "activeUserId",
    accounts = listOf(
        UserState.Account(
            userId = "activeUserId",
            name = "Test User",
            email = "test@example.com",
            avatarColorHex = "#FF0000",
            environment = Environment.Us,
            isPremium = false,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            needsMasterPassword = false,
            hasMasterPassword = true,
            trustedDevice = null,
            organizations = listOf(
                Organization(
                    id = ORGANIZATION_ID,
                    name = ORGANIZATION_NAME,
                    shouldManageResetPassword = false,
                    shouldUseKeyConnector = false,
                    role = OrganizationType.USER,
                    keyConnectorUrl = null,
                    userIsClaimedByOrganization = false,
                ),
            ),
            isBiometricsEnabled = false,
            vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
            isUsingKeyConnector = false,
            onboardingStatus = OnboardingStatus.COMPLETE,
            firstTimeState = FirstTimeState(showImportLoginsCard = false),
            isExportable = true,
        ),
    ),
)
