package com.x8bit.bitwarden.ui.vault.feature.exportitems.reviewexport

import android.net.Uri
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import app.cash.turbine.test
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.cxf.manager.model.ExportCredentialsResult
import com.bitwarden.cxf.model.ImportCredentialsRequestData
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.DecryptCipherListResult
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCardListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockDecryptCipherListResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ReviewExportViewModelTest : BaseViewModelTest() {
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository = mockk<AuthRepository> {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val decryptCipherListResultFlow = MutableStateFlow<DataState<DecryptCipherListResult>>(
        DataState.Loaded(
            data = DecryptCipherListResult(
                successes = emptyList(),
                failures = emptyList(),
            ),
        ),
    )
    private val vaultRepository = mockk<VaultRepository> {
        every { decryptCipherListResultStateFlow } returns decryptCipherListResultFlow
    }
    private val specialCircumstanceManager = mockk<SpecialCircumstanceManager> {
        every {
            specialCircumstance
        } returns SpecialCircumstance.CredentialExchangeExport(
            data = DEFAULT_REQUEST_DATA,
        )
    }
    private val policyManager = mockk<PolicyManager> {
        every { getActivePolicies(PolicyTypeJson.RESTRICT_ITEM_TYPES) } returns emptyList()
    }

    @Nested
    inner class Initialization {
        @Test
        fun `initial state is correct when SavedStateHandle is empty`() =
            runTest {
                val viewModel = createViewModel()
                assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
            }
    }

    @Nested
    inner class Actions {
        @Suppress("MaxLineLength")
        @Test
        fun `ImportItemsClick shows loading, and calls exportVaultDataToCxf with all active items if there are no item restrictions`() =
            runTest {
                val mockActiveCipherListView = createMockCipherListView(
                    number = 1,
                    type = CipherListViewType.Card(
                        createMockCardListView(number = 1),
                    ),
                )
                val mockDeletedCipherListView = createMockCipherListView(
                    number = 1,
                    isDeleted = true,
                )
                decryptCipherListResultFlow.tryEmit(
                    DataState.Loaded(
                        createMockDecryptCipherListResult(
                            number = 1,
                            successes = listOf(
                                mockActiveCipherListView,
                                mockDeletedCipherListView,
                            ),
                        ),
                    ),
                )
                coEvery {
                    vaultRepository.exportVaultDataToCxf(listOf(mockActiveCipherListView))
                } just awaits

                val viewModel = createViewModel()
                viewModel.trySendAction(ReviewExportAction.ImportItemsClick)

                // Check for loading dialog
                assertEquals(
                    DEFAULT_STATE.copy(
                        viewState = DEFAULT_STATE.viewState.copy(
                            itemTypeCounts = DEFAULT_STATE.viewState.itemTypeCounts.copy(
                                cardCount = 1,
                            ),
                        ),
                        dialog = ReviewExportState.DialogState.Loading(
                            BitwardenString.exporting_items.asText(),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                coVerify {
                    vaultRepository.exportVaultDataToCxf(listOf(mockActiveCipherListView))
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `ImportItemsClick shows loading, and calls exportVaultDataToCxf without restricted items if there are item restrictions`() =
            runTest {
                val mockCipherListView = createMockCipherListView(
                    number = 1,
                    type = CipherListViewType.Card(
                        createMockCardListView(number = 1),
                    ),
                )
                every {
                    policyManager.getActivePolicies(PolicyTypeJson.RESTRICT_ITEM_TYPES)
                } returns listOf(
                    createMockPolicy(
                        number = 1,
                        type = PolicyTypeJson.RESTRICT_ITEM_TYPES,
                        isEnabled = true,
                    ),
                )
                coEvery { vaultRepository.exportVaultDataToCxf(any()) } just awaits
                decryptCipherListResultFlow.tryEmit(
                    DataState.Loaded(
                        createMockDecryptCipherListResult(
                            number = 1,
                            successes = listOf(mockCipherListView),
                        ),
                    ),
                )

                val viewModel = createViewModel()
                viewModel.trySendAction(ReviewExportAction.ImportItemsClick)

                coVerify {
                    vaultRepository.exportVaultDataToCxf(ciphers = emptyList())
                }
            }

        @Test
        fun `ImportItemsClick sends Success event on successful export`() = runTest {
            val viewModel = createViewModel()
            val exportResult = ExportCredentialsResult.Success(
                payload = "payload",
                uri = MOCK_URI,
            )
            coEvery {
                vaultRepository.exportVaultDataToCxf(any())
            } returns Result.success("payload")

            viewModel.eventFlow.test {
                viewModel.trySendAction(ReviewExportAction.ImportItemsClick)

                val event = awaitItem() as ReviewExportEvent.CompleteExport
                assertEquals(exportResult, event.result)
            }
        }

        @Test
        fun `ImportItemsClick sends Failure event on export error`() = runTest {
            val viewModel = createViewModel()
            val mockException = ImportCredentialsUnknownErrorException("Export failed")
            coEvery {
                vaultRepository.exportVaultDataToCxf(any())
            } returns Result.failure(
                mockException,
            )

            viewModel.eventFlow.test {
                viewModel.trySendAction(ReviewExportAction.ImportItemsClick)

                val event = awaitItem() as ReviewExportEvent.CompleteExport
                assertTrue(event.result is ExportCredentialsResult.Failure)
                assertEquals(mockException, (event.result as ExportCredentialsResult.Failure).error)
            }
        }

        @Test
        fun `CancelClicked sends NavigateBack event`() = runTest {
            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                viewModel.trySendAction(ReviewExportAction.CancelClick)
                assertEquals(ReviewExportEvent.NavigateBack, awaitItem())
            }
        }

        @Test
        fun `DismissDialog clears dialog from state`() = runTest {
            decryptCipherListResultFlow.value = DataState.Loading
            val viewModel = createViewModel()
            // Check for loading dialog
            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = ReviewExportState.DialogState.Loading(
                        BitwardenString.loading.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )

            viewModel.trySendAction(ReviewExportAction.DismissDialog)

            assertEquals(
                DEFAULT_STATE.copy(dialog = null),
                viewModel.stateFlow.value,
            )
        }

        @Test
        fun `VaultDataReceive Pending should show loading dialog and update item type counts`() =
            runTest {
                val viewModel = createViewModel()
                viewModel.trySendAction(
                    ReviewExportAction.Internal.VaultDataReceive(
                        DataState.Pending(
                            data = DecryptCipherListResult(
                                successes = listOf(createMockCipherListView(number = 1)),
                                failures = emptyList(),
                            ),
                        ),
                    ),
                )

                val expectedState = DEFAULT_STATE.copy(
                    viewState = DEFAULT_STATE.viewState.copy(
                        itemTypeCounts = DEFAULT_STATE.viewState.itemTypeCounts.copy(
                            passwordCount = 1,
                        ),
                    ),
                    dialog = ReviewExportState.DialogState.Loading(
                        BitwardenString.loading.asText(),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Test
        fun `VaultDataReceive NoNetwork should update item type counts and dismiss dialog`() =
            runTest {
                val viewModel = createViewModel()
                viewModel.trySendAction(
                    ReviewExportAction.Internal.VaultDataReceive(
                        DataState.NoNetwork(
                            data = DecryptCipherListResult(
                                successes = listOf(createMockCipherListView(number = 1)),
                                failures = emptyList(),
                            ),
                        ),
                    ),
                )

                val expectedState = DEFAULT_STATE.copy(
                    viewState = DEFAULT_STATE.viewState.copy(
                        itemTypeCounts = DEFAULT_STATE.viewState.itemTypeCounts.copy(
                            passwordCount = 1,
                        ),
                    ),
                    dialog = null,
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Test
        fun `VaultDataReceive Error should update item type counts and show error dialog`() =
            runTest {
                val throwable = Exception()
                val viewModel = createViewModel()
                viewModel.trySendAction(
                    ReviewExportAction.Internal.VaultDataReceive(
                        DataState.Error(
                            data = DecryptCipherListResult(
                                successes = listOf(createMockCipherListView(number = 1)),
                                failures = emptyList(),
                            ),
                            error = throwable,
                        ),
                    ),
                )

                val expectedState = DEFAULT_STATE.copy(
                    viewState = DEFAULT_STATE.viewState.copy(
                        itemTypeCounts = DEFAULT_STATE.viewState.itemTypeCounts.copy(
                            passwordCount = 1,
                        ),
                    ),
                    dialog = ReviewExportState.DialogState.General(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.generic_error_message.asText(),
                        error = throwable,
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Test
        fun `ExportResultReceive should clear dialog and send CompleteExport event`() = runTest {
            val viewModel = createViewModel()
            val exportResult = ExportCredentialsResult.Success(
                payload = "",
                uri = mockk(),
            )
            viewModel.trySendAction(
                ReviewExportAction.Internal.ExportResultReceive(exportResult),
            )

            assertEquals(
                DEFAULT_STATE,
                viewModel.stateFlow.value,
            )

            viewModel.eventFlow.test {
                assertEquals(
                    ReviewExportEvent.CompleteExport(exportResult),
                    awaitItem(),
                )
            }
        }
    }

    private fun createViewModel(): ReviewExportViewModel = ReviewExportViewModel(
        authRepository = authRepository,
        vaultRepository = vaultRepository,
        specialCircumstanceManager = specialCircumstanceManager,
        policyManager = policyManager,
    )
}

private val MOCK_URI = mockk<Uri>()
private val DEFAULT_REQUEST_DATA = ImportCredentialsRequestData(
    uri = MOCK_URI,
    requestJson = "mockRequestJson",
)
private val DEFAULT_STATE: ReviewExportState = ReviewExportState(
    importCredentialsRequestData = DEFAULT_REQUEST_DATA,
    viewState = ReviewExportState.ViewState(
        itemTypeCounts = ReviewExportState.ItemTypeCounts(),
    ),
)
private const val DEFAULT_USER_ID: String = "activeUserId"
private val DEFAULT_USER_STATE = UserState(
    activeUserId = DEFAULT_USER_ID,
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
            organizations = listOf(
                Organization(
                    id = "mockOrganizationId-1",
                    name = "Organization User",
                    shouldUseKeyConnector = false,
                    shouldManageResetPassword = false,
                    role = OrganizationType.USER,
                    keyConnectorUrl = null,
                    userIsClaimedByOrganization = false,
                ),
            ),
            needsMasterPassword = false,
            trustedDevice = null,
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            onboardingStatus = OnboardingStatus.COMPLETE,
            firstTimeState = FirstTimeState(showImportLoginsCard = true),
        ),
    ),
)
