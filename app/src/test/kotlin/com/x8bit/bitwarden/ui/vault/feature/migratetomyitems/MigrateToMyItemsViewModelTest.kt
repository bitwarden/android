package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.repository.error.MissingPropertyException
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.vault.manager.VaultMigrationManager
import com.x8bit.bitwarden.data.vault.manager.VaultSyncManager
import com.x8bit.bitwarden.data.vault.repository.model.MigratePersonalVaultResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MigrateToMyItemsViewModelTest : BaseViewModelTest() {

    private val mockOrganizationEventManager: OrganizationEventManager = mockk {
        every { trackEvent(any()) } just runs
    }
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(
        mockk {
            every { activeUserId } returns "test-user-id"
        },
    )
    private val mockVaultMigrationManager: VaultMigrationManager = mockk {
        coEvery {
            migratePersonalVault(any(), any())
        } returns MigratePersonalVaultResult.Success
    }
    private val mockVaultSyncManager: VaultSyncManager = mockk(relaxed = true)
    private val mockAuthRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }

    @BeforeEach
    fun setup() {
        mockkStatic(SavedStateHandle::toMigrateToMyItemsArgs)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SavedStateHandle::toMigrateToMyItemsArgs)
    }

    @Test
    fun `initial state should be set from organization data`() {
        val viewModel = createViewModel()
        assertEquals(ORGANIZATION_NAME, viewModel.stateFlow.value.organizationName)
        assertEquals(ORGANIZATION_ID, viewModel.stateFlow.value.organizationId)
        assertNull(viewModel.stateFlow.value.dialog)
    }

    @Test
    fun `AcceptClicked should show loading dialog and trigger migration`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())

            viewModel.trySendAction(MigrateToMyItemsAction.AcceptClicked)

            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = MigrateToMyItemsState.DialogState.Loading(
                        message = BitwardenString.migrating_items_to_x.asText(ORGANIZATION_NAME),
                    ),
                ),
                awaitItem(),
            )

            // Migration completes successfully and clears the dialog
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
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
    fun `AcceptClicked should show error dialog when userId is null`() = runTest {
        mutableUserStateFlow.value = null
        val viewModel = createViewModel()
        viewModel.trySendAction(MigrateToMyItemsAction.AcceptClicked)

        val dialog = viewModel.stateFlow.value.dialog as MigrateToMyItemsState.DialogState.Error
        val throwableReference = dialog.throwable

        assert(throwableReference is MissingPropertyException)
        assertEquals(
            "Missing the required UserId property",
            throwableReference?.message,
        )

        assertEquals(
            MigrateToMyItemsState.DialogState.Error(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString.failed_to_migrate_items_to_x.asText(
                    ORGANIZATION_NAME,
                ),
                throwable = throwableReference,
            ),
            viewModel.stateFlow.value.dialog,
        )

        coVerify(exactly = 0) {
            mockVaultMigrationManager.migratePersonalVault(any(), any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `MigrateToMyItemsResultReceived with success should track ItemOrganizationAccepted event, clear dialog, and navigate to vault`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                // First show the loading dialog
                viewModel.trySendAction(MigrateToMyItemsAction.AcceptClicked)

                // Migration completes and sends NavigateToVault event
                assertEquals(MigrateToMyItemsEvent.NavigateToVault, awaitItem())

                viewModel.trySendAction(
                    MigrateToMyItemsAction.Internal.MigrateToMyItemsResultReceived(
                        result = MigratePersonalVaultResult.Success,
                    ),
                )
                assertEquals(MigrateToMyItemsEvent.NavigateToVault, awaitItem())
            }

            assertNull(viewModel.stateFlow.value.dialog)

            verify {
                mockOrganizationEventManager.trackEvent(
                    event = OrganizationEvent.ItemOrganizationAccepted,
                )
            }
        }

    @Test
    fun `MigrateToMyItemsResultReceived with failure should show error dialog`() = runTest {
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            awaitItem() // Initial state

            viewModel.trySendAction(
                MigrateToMyItemsAction.Internal.MigrateToMyItemsResultReceived(
                    result = MigratePersonalVaultResult.Failure(null),
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
            assertEquals(
                MigrateToMyItemsEvent.NavigateToLeaveOrganization(
                    organizationId = ORGANIZATION_ID,
                    organizationName = ORGANIZATION_NAME,
                ),
                awaitItem(),
            )
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
                    result = MigratePersonalVaultResult.Failure(null),
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

    private fun createViewModel(
        state: MigrateToMyItemsState = DEFAULT_STATE,
    ): MigrateToMyItemsViewModel {
        return MigrateToMyItemsViewModel(
            organizationEventManager = mockOrganizationEventManager,
            vaultMigrationManager = mockVaultMigrationManager,
            vaultSyncManager = mockVaultSyncManager,
            authRepository = mockAuthRepository,
            savedStateHandle = SavedStateHandle(mapOf("state" to state)),
        )
    }
}

private const val ORGANIZATION_ID = "test-organization-id"
private const val ORGANIZATION_NAME = "Test Organization"

private val DEFAULT_STATE = MigrateToMyItemsState(
    organizationId = "test-organization-id",
    organizationName = "Test Organization",
    dialog = null,
)
