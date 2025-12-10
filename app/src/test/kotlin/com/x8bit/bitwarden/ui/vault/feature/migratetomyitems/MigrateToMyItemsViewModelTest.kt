package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MigrateToMyItemsViewModelTest : BaseViewModelTest() {

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
        assertEquals(ORGANIZATION_NAME, viewModel.stateFlow.value.viewState.organizationName)
        assertNull(viewModel.stateFlow.value.dialog)
    }

    @Test
    fun `ContinueClicked should show loading dialog and trigger migration`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(null, awaitItem().dialog)

            viewModel.trySendAction(MigrateToMyItemsAction.ContinueClicked)

            val loadingState = awaitItem()
            assert(loadingState.dialog is MigrateToMyItemsState.DialogState.Loading)
            assertEquals(
                BitwardenString.migrating_items_to_x.asText(ORGANIZATION_NAME),
                (loadingState.dialog as MigrateToMyItemsState.DialogState.Loading).message,
            )
        }
    }

    @Test
    fun `ContinueClicked should navigate to vault on success`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(MigrateToMyItemsAction.ContinueClicked)
            assertEquals(MigrateToMyItemsEvent.NavigateToVault, awaitItem())
        }
    }

    @Test
    fun `MigrateToMyItemsResultReceived with success should clear dialog and navigate to vault`() =
        runTest {
            val viewModel = createViewModel()

            // First show the loading dialog
            viewModel.trySendAction(MigrateToMyItemsAction.ContinueClicked)

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
            assertEquals(BitwardenString.failed_to_migrate_items_to_x.asText(), errorDialog.message)
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

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): MigrateToMyItemsViewModel {
        every { savedStateHandle.toMigrateToMyItemsArgs() } returns MigrateToMyItemsArgs(
            organizationId = ORGANIZATION_ID,
            organizationName = ORGANIZATION_NAME,
        )
        return MigrateToMyItemsViewModel(
            savedStateHandle = savedStateHandle,
        )
    }
}

private const val ORGANIZATION_ID = "test-organization-id"
private const val ORGANIZATION_NAME = "Test Organization"
