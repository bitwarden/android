package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.vault.manager.VaultMigrationManager
import com.x8bit.bitwarden.data.vault.manager.VaultSyncManager
import com.x8bit.bitwarden.data.vault.manager.model.VaultMigrationData
import com.x8bit.bitwarden.data.vault.repository.model.MigratePersonalVaultResult
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.SocketTimeoutException

class MigrateToMyItemsViewModelTest : BaseViewModelTest() {

    private val mockOrganizationEventManager: OrganizationEventManager = mockk {
        every { trackEvent(any()) } just runs
    }
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(
        mockk {
            every { activeUserId } returns "test-user-id"
        },
    )
    private val mutableVaultMigrationDataStateFlow = MutableStateFlow<VaultMigrationData>(
        VaultMigrationData.MigrationRequired(
            organizationId = ORGANIZATION_ID,
            organizationName = ORGANIZATION_NAME,
        ),
    )
    private val mockVaultMigrationManager: VaultMigrationManager = mockk {
        coEvery { vaultMigrationDataStateFlow } returns mutableVaultMigrationDataStateFlow
        coEvery { migratePersonalVault(any(), any()) } returns MigratePersonalVaultResult.Success
        every { clearMigrationState() } just runs
    }
    private val mockVaultSyncManager: VaultSyncManager = mockk(relaxed = true)
    private val mockAuthRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }

    private val mockSnackbarRelayManager: SnackbarRelayManager<SnackbarRelay> = mockk {
        every { sendSnackbarData(data = any(), relay = any()) } just runs
    }

    @BeforeEach
    fun setup() {
        mockkConstructor(NoActiveUserException::class)
        every {
            anyConstructed<NoActiveUserException>() == any<NoActiveUserException>()
        } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(NoActiveUserException::class)
    }

    @Test
    fun `initial state should be set from organization data`() {
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
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

    @Suppress("MaxLineLength")
    @Test
    fun `AcceptClicked with valid user and migration should show success snackbar and track the event`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.trySendAction(MigrateToMyItemsAction.AcceptClicked)

            coVerify(exactly = 1) {
                mockVaultMigrationManager.migratePersonalVault(
                    userId = "test-user-id",
                    organizationId = ORGANIZATION_ID,
                )
                mockSnackbarRelayManager.sendSnackbarData(
                    relay = SnackbarRelay.VAULT_MIGRATED_TO_MY_ITEMS,
                    data = BitwardenSnackbarData(message = BitwardenString.items_transferred.asText()),
                )
                mockOrganizationEventManager.trackEvent(
                    event = OrganizationEvent.ItemOrganizationAccepted(
                        organizationId = ORGANIZATION_ID,
                    ),
                )
            }
        }

    @Test
    fun `AcceptClicked should show error dialog when userId is null`() = runTest {
        mutableUserStateFlow.value = null
        val viewModel = createViewModel()

        viewModel.trySendAction(MigrateToMyItemsAction.AcceptClicked)

        assertEquals(
            DEFAULT_STATE.copy(
                dialog = MigrateToMyItemsState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.failed_to_migrate_items_to_x.asText(
                        ORGANIZATION_NAME,
                    ),
                    throwable = NoActiveUserException(),
                ),
            ),
            viewModel.stateFlow.value,
        )

        coVerify(exactly = 0) {
            mockVaultMigrationManager.migratePersonalVault(any(), any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `MigrateToMyItemsResultReceived with valid user ID and failed migration should show error dialog`() =
        runTest {
            val viewModel = createViewModel()

            val error = Throwable("Fail")
            coEvery {
                mockVaultMigrationManager.migratePersonalVault(any(), any())
            } returns MigratePersonalVaultResult.Failure(error)

            viewModel.stateFlow.test {
                awaitItem() // Initial state

                viewModel.trySendAction(MigrateToMyItemsAction.AcceptClicked)

                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = MigrateToMyItemsState.DialogState.Loading(
                            message = BitwardenString.migrating_items_to_x.asText(ORGANIZATION_NAME),
                        ),
                    ),
                    awaitItem(),
                )

                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = MigrateToMyItemsState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.failed_to_migrate_items_to_x.asText(
                                ORGANIZATION_NAME,
                            ),
                            throwable = error,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MigrateToMyItemsResultReceived with timeout error should show DialogState NoNetwork`() =
        runTest {
            val error = SocketTimeoutException("Timeout")
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                awaitItem() // Initial state

                viewModel.trySendAction(
                    MigrateToMyItemsAction.Internal.MigrateToMyItemsResultReceived(
                        result = MigratePersonalVaultResult.Failure(error),
                    ),
                )

                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = MigrateToMyItemsState.DialogState.NoNetwork(
                            title = BitwardenString.internet_connection_required_title.asText(),
                            message = BitwardenString.internet_connection_required_message.asText(),
                            throwable = error,
                        ),
                    ),
                    awaitItem(),
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
            assertEquals(
                MigrateToMyItemsEvent.LaunchUri("https://bitwarden.com/help/transfer-ownership/"),
                awaitItem(),
            )
        }
    }

    @Test
    fun `DismissDialogClicked should clear dialog`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            dialog = MigrateToMyItemsState.DialogState.Error(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString.failed_to_migrate_items_to_x.asText(
                    ORGANIZATION_NAME,
                ),
                throwable = null,
            ),
        )
        val viewModel = createViewModel(state = initialState)

        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            // Dismiss the dialog
            viewModel.trySendAction(MigrateToMyItemsAction.DismissDialogClicked)
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `NoNetworkDismissDialogClicked should clear dialog and clear migration state`() = runTest {
        val error = SocketTimeoutException("Timeout")
        val initialState = DEFAULT_STATE.copy(
            dialog = MigrateToMyItemsState.DialogState.NoNetwork(
                title = BitwardenString.internet_connection_required_title.asText(),
                message = BitwardenString.internet_connection_required_message.asText(),
                throwable = error,
            ),
        )
        val viewModel = createViewModel(state = initialState)

        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())

            // Dismiss the dialog
            viewModel.trySendAction(MigrateToMyItemsAction.NoNetworkDismissDialogClicked)
            assertEquals(DEFAULT_STATE, awaitItem())
        }
        verify(exactly = 1) {
            mockVaultMigrationManager.clearMigrationState()
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
            snackbarRelayManager = mockSnackbarRelayManager,
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
