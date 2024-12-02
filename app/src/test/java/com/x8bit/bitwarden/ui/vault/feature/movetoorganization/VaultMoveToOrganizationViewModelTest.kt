package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.CollectionView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.datasource.network.model.OrganizationType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.ShareCipherResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.vault.feature.movetoorganization.util.createMockOrganizationList
import com.x8bit.bitwarden.ui.vault.model.VaultCollection
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultMoveToOrganizationViewModelTest : BaseViewModelTest() {

    private val initialState = createVaultMoveToOrganizationState()
    private val initialSavedStateHandle = createSavedStateHandleWithState(
        state = initialState,
    )

    private val mutableVaultItemFlow = MutableStateFlow<DataState<CipherView?>>(DataState.Loading)

    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)

    private val mutableCollectionFlow =
        MutableStateFlow<DataState<List<CollectionView>>>(DataState.Loading)

    private val vaultRepository: VaultRepository = mockk {
        every { getVaultItemStateFlow(DEFAULT_ITEM_ID) } returns mutableVaultItemFlow
        every { collectionsStateFlow } returns mutableCollectionFlow
    }

    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }

    @Test
    fun `initial state should be correct when state is null`() = runTest {
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = null,
            ),
        )
        assertEquals(
            initialState.copy(viewState = VaultMoveToOrganizationState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initialState,
            ),
        )

        assertEquals(initialState, viewModel.stateFlow.value)
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel(
            savedStateHandle = initialSavedStateHandle,
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultMoveToOrganizationAction.BackClick)
            assertEquals(VaultMoveToOrganizationEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `OrganizationSelect should update selected Organization`() = runTest {
        val viewModel = createViewModel()
        mutableCollectionFlow.tryEmit(value = DataState.Loaded(DEFAULT_COLLECTIONS))
        mutableVaultItemFlow.tryEmit(value = DataState.Loaded(createMockCipherView(number = 1)))
        val action = VaultMoveToOrganizationAction.OrganizationSelect(
            VaultMoveToOrganizationState.ViewState.Content.Organization(
                id = "mockOrganizationId-3",
                name = "mockOrganizationName-3",
                collections = emptyList(),
            ),
        )
        val expectedState = createVaultMoveToOrganizationState(
            viewState = VaultMoveToOrganizationState.ViewState.Content(
                organizations = createMockOrganizationList(),
                selectedOrganizationId = "mockOrganizationId-3",
                cipherToMove = createMockCipherView(number = 1),
            ),
        )

        viewModel.trySendAction(action)

        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `CollectionSelect should update selected Collections`() = runTest {
        val viewModel = createViewModel()
        mutableCollectionFlow.tryEmit(value = DataState.Loaded(DEFAULT_COLLECTIONS))
        mutableVaultItemFlow.tryEmit(value = DataState.Loaded(createMockCipherView(number = 1)))
        val unselectCollection1Action = VaultMoveToOrganizationAction.CollectionSelect(
            VaultCollection(
                id = "mockId-1",
                name = "mockName-1",
                isSelected = true,
            ),
        )
        val expectedState = createVaultMoveToOrganizationState(
            viewState = VaultMoveToOrganizationState.ViewState.Content(
                organizations = createMockOrganizationList().map { organization ->
                    organization.copy(
                        collections = if (organization.id == "mockOrganizationId-1") {
                            organization.collections.map {
                                it.copy(isSelected = false)
                            }
                        } else {
                            organization.collections
                        },
                    )
                },
                cipherToMove = createMockCipherView(number = 1),
                selectedOrganizationId = "mockOrganizationId-1",
            ),
        )

        viewModel.trySendAction(unselectCollection1Action)

        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `DataState Loading should show loading state`() = runTest {
        val viewModel = createViewModel()
        mutableCollectionFlow.tryEmit(value = DataState.Loaded(DEFAULT_COLLECTIONS))
        mutableVaultItemFlow.tryEmit(value = DataState.Loading)
        viewModel.stateFlow.test {
            assertEquals(
                initialState.copy(viewState = VaultMoveToOrganizationState.ViewState.Loading),
                awaitItem(),
            )
        }
    }

    @Test
    fun `DataState Pending should show content state`() = runTest {
        val viewModel = createViewModel()
        mutableCollectionFlow.tryEmit(value = DataState.Loaded(DEFAULT_COLLECTIONS))
        mutableVaultItemFlow.tryEmit(
            value = DataState.Pending(
                data = createMockCipherView(number = 1),
            ),
        )
        viewModel.stateFlow.test {
            assertEquals(
                initialState.copy(
                    viewState = VaultMoveToOrganizationState.ViewState.Content(
                        organizations = createMockOrganizationList(),
                        selectedOrganizationId = "mockOrganizationId-1",
                        cipherToMove = createMockCipherView(number = 1),
                    ),
                    dialogState = null,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `DataState Error should show Error State`() = runTest {
        val viewModel = createViewModel()
        mutableCollectionFlow.tryEmit(value = DataState.Loaded(DEFAULT_COLLECTIONS))
        mutableVaultItemFlow.tryEmit(
            value = DataState.Error(
                error = IllegalStateException(),
                data = null,
            ),
        )
        viewModel.stateFlow.test {
            assertEquals(
                initialState.copy(
                    viewState = VaultMoveToOrganizationState.ViewState.Error(
                        message = R.string.generic_error_message.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `DataState NoNetwork should show Error Dialog`() = runTest {
        val viewModel = createViewModel()
        mutableCollectionFlow.tryEmit(value = DataState.Loaded(DEFAULT_COLLECTIONS))
        mutableVaultItemFlow.tryEmit(
            value = DataState.NoNetwork(),
        )
        viewModel.stateFlow.test {
            assertEquals(
                initialState.copy(
                    viewState = VaultMoveToOrganizationState.ViewState.Error(
                        message = R.string.internet_connection_required_title
                            .asText()
                            .concat(
                                " ".asText(),
                                R.string.internet_connection_required_message.asText(),
                            ),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `MoveClick with shareCipher success should show loading dialog, and remove it`() = runTest {
        val viewModel = createViewModel()
        mutableCollectionFlow.tryEmit(value = DataState.Loaded(DEFAULT_COLLECTIONS))
        mutableVaultItemFlow.tryEmit(value = DataState.Loaded(createMockCipherView(number = 1)))
        coEvery {
            vaultRepository.shareCipher(
                cipherId = "mockCipherId",
                organizationId = "mockOrganizationId-1",
                cipherView = createMockCipherView(number = 1),
                collectionIds = listOf("mockId-1"),
            )
        } returns ShareCipherResult.Success
        viewModel.stateFlow.test {
            assertEquals(
                initialState.copy(
                    viewState = VaultMoveToOrganizationState.ViewState.Content(
                        organizations = createMockOrganizationList(),
                        selectedOrganizationId = "mockOrganizationId-1",
                        cipherToMove = createMockCipherView(number = 1),
                    ),
                ),
                awaitItem(),
            )
            viewModel.trySendAction(VaultMoveToOrganizationAction.MoveClick)
            assertEquals(
                initialState.copy(
                    dialogState = VaultMoveToOrganizationState.DialogState.Loading(
                        message = R.string.saving.asText(),
                    ),
                    viewState = VaultMoveToOrganizationState.ViewState.Content(
                        organizations = createMockOrganizationList(),
                        selectedOrganizationId = "mockOrganizationId-1",
                        cipherToMove = createMockCipherView(number = 1),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                initialState.copy(
                    dialogState = null,
                    viewState = VaultMoveToOrganizationState.ViewState.Content(
                        organizations = createMockOrganizationList(),
                        selectedOrganizationId = "mockOrganizationId-1",
                        cipherToMove = createMockCipherView(number = 1),
                    ),
                ),
                awaitItem(),
            )
        }
        coVerify {
            vaultRepository.shareCipher(
                cipherId = "mockCipherId",
                organizationId = "mockOrganizationId-1",
                cipherView = createMockCipherView(number = 1),
                collectionIds = listOf("mockId-1"),
            )
        }
    }

    @Test
    fun `MoveClick with shareCipher error should show error dialog`() = runTest {
        val viewModel = createViewModel()
        mutableCollectionFlow.tryEmit(value = DataState.Loaded(DEFAULT_COLLECTIONS))
        mutableVaultItemFlow.tryEmit(value = DataState.Loaded(createMockCipherView(number = 1)))
        coEvery {
            vaultRepository.shareCipher(
                cipherId = "mockCipherId",
                organizationId = "mockOrganizationId-1",
                cipherView = createMockCipherView(number = 1),
                collectionIds = listOf("mockId-1"),
            )
        } returns ShareCipherResult.Error
        viewModel.stateFlow.test {
            assertEquals(
                initialState.copy(
                    viewState = VaultMoveToOrganizationState.ViewState.Content(
                        organizations = createMockOrganizationList(),
                        selectedOrganizationId = "mockOrganizationId-1",
                        cipherToMove = createMockCipherView(number = 1),
                    ),
                ),
                awaitItem(),
            )
            viewModel.trySendAction(VaultMoveToOrganizationAction.MoveClick)
            assertEquals(
                initialState.copy(
                    dialogState = VaultMoveToOrganizationState.DialogState.Loading(
                        message = R.string.saving.asText(),
                    ),
                    viewState = VaultMoveToOrganizationState.ViewState.Content(
                        organizations = createMockOrganizationList(),
                        selectedOrganizationId = "mockOrganizationId-1",
                        cipherToMove = createMockCipherView(number = 1),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                initialState.copy(
                    dialogState = VaultMoveToOrganizationState.DialogState.Error(
                        message = R.string.generic_error_message.asText(),
                    ),
                    viewState = VaultMoveToOrganizationState.ViewState.Content(
                        organizations = createMockOrganizationList(),
                        selectedOrganizationId = "mockOrganizationId-1",
                        cipherToMove = createMockCipherView(number = 1),
                    ),
                ),
                awaitItem(),
            )
        }
        coVerify {
            vaultRepository.shareCipher(
                cipherId = "mockCipherId",
                organizationId = "mockOrganizationId-1",
                cipherView = createMockCipherView(number = 1),
                collectionIds = listOf("mockId-1"),
            )
        }
    }

    @Test
    fun `MoveClick with shareCipher success should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        mutableCollectionFlow.tryEmit(value = DataState.Loaded(DEFAULT_COLLECTIONS))
        mutableVaultItemFlow.tryEmit(value = DataState.Loaded(createMockCipherView(number = 1)))
        coEvery {
            vaultRepository.shareCipher(
                cipherId = "mockCipherId",
                organizationId = "mockOrganizationId-1",
                cipherView = createMockCipherView(number = 1),
                collectionIds = listOf("mockId-1"),
            )
        } returns ShareCipherResult.Success
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultMoveToOrganizationAction.MoveClick)
            assertEquals(
                VaultMoveToOrganizationEvent.NavigateBack,
                awaitItem(),
            )
            assertEquals(
                VaultMoveToOrganizationEvent.ShowToast(
                    text = R.string.moved_item_to_org.asText(
                        "mockName-1",
                        "mockOrganizationName-1",
                    ),
                ),
                awaitItem(),
            )
        }
        coVerify {
            vaultRepository.shareCipher(
                cipherId = "mockCipherId",
                organizationId = "mockOrganizationId-1",
                cipherView = createMockCipherView(number = 1),
                collectionIds = listOf("mockId-1"),
            )
        }
    }

    @Test
    fun `MoveClick with onlyShowCollections true should invoke updateCipherCollections`() =
        runTest {
            val viewModel = createViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = createVaultMoveToOrganizationState(
                        onlyShowCollections = true,
                    ),
                ),
            )
            mutableCollectionFlow.tryEmit(value = DataState.Loaded(DEFAULT_COLLECTIONS))
            mutableVaultItemFlow.tryEmit(value = DataState.Loaded(createMockCipherView(number = 1)))
            coEvery {
                vaultRepository.updateCipherCollections(
                    cipherId = "mockCipherId",
                    cipherView = createMockCipherView(number = 1),
                    collectionIds = listOf("mockId-1"),
                )
            } returns ShareCipherResult.Success
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultMoveToOrganizationAction.MoveClick)
                assertEquals(
                    VaultMoveToOrganizationEvent.NavigateBack,
                    awaitItem(),
                )
                assertEquals(
                    VaultMoveToOrganizationEvent.ShowToast(R.string.item_updated.asText()),
                    awaitItem(),
                )
            }
            coVerify {
                vaultRepository.updateCipherCollections(
                    cipherId = "mockCipherId",
                    cipherView = createMockCipherView(number = 1),
                    collectionIds = listOf("mockId-1"),
                )
            }
        }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = initialSavedStateHandle,
        vaultRepo: VaultRepository = vaultRepository,
        authRepo: AuthRepository = authRepository,
    ): VaultMoveToOrganizationViewModel = VaultMoveToOrganizationViewModel(
        savedStateHandle = savedStateHandle,
        authRepository = authRepo,
        vaultRepository = vaultRepo,
    )

    private fun createSavedStateHandleWithState(
        state: VaultMoveToOrganizationState? = null,
        vaultItemId: String = "mockCipherId",
        showOnlyCollections: Boolean = false,
    ) = SavedStateHandle().apply {
        set("state", state)
        set("vault_move_to_organization_id", vaultItemId)
        set("vault_move_to_organization_only_collections", "$showOnlyCollections")
    }

    @Suppress("MaxLineLength")
    private fun createVaultMoveToOrganizationState(
        viewState: VaultMoveToOrganizationState.ViewState = VaultMoveToOrganizationState.ViewState.Loading,
        vaultItemId: String = "mockCipherId",
        dialogState: VaultMoveToOrganizationState.DialogState? = null,
        onlyShowCollections: Boolean = false,
    ): VaultMoveToOrganizationState = VaultMoveToOrganizationState(
        vaultItemId = vaultItemId,
        viewState = viewState,
        dialogState = dialogState,
        onlyShowCollections = onlyShowCollections,
    )
}

private const val DEFAULT_ITEM_ID: String = "mockCipherId"

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
            needsMasterPassword = false,
            organizations = listOf(
                Organization(
                    id = "mockOrganizationId-1",
                    name = "mockOrganizationName-1",
                    shouldManageResetPassword = false,
                    shouldUseKeyConnector = false,
                    role = OrganizationType.ADMIN,
                    shouldUsersGetPremium = false,
                ),
                Organization(
                    id = "mockOrganizationId-2",
                    name = "mockOrganizationName-2",
                    shouldManageResetPassword = false,
                    shouldUseKeyConnector = false,
                    role = OrganizationType.ADMIN,
                    shouldUsersGetPremium = false,
                ),
                Organization(
                    id = "mockOrganizationId-3",
                    name = "mockOrganizationName-3",
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
            firstTimeState = FirstTimeState(showImportLoginsCard = true),
        ),
    ),
)

private val DEFAULT_COLLECTIONS = listOf(
    createMockCollectionView(number = 1),
    createMockCollectionView(number = 2),
    createMockCollectionView(number = 3),
    createMockCollectionView(number = 4),
)
