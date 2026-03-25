package com.x8bit.bitwarden.ui.platform.feature.settings.collections.addedit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.collections.CollectionType
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.createMockOrganization
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateCollectionResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCollectionResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCollectionResult
import com.x8bit.bitwarden.ui.platform.feature.settings.collections.model.CollectionAddEditType
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class CollectionAddEditViewModelTest : BaseViewModelTest() {

    private val mutableCollectionsStateFlow =
        MutableStateFlow<DataState<List<CollectionView>>>(DataState.Loading)

    private val vaultRepository: VaultRepository = mockk {
        every { collectionsStateFlow } returns mutableCollectionsStateFlow
    }

    private val authRepository: AuthRepository = mockk {
        every { organizations } returns listOf(DEFAULT_ORGANIZATION)
    }

    private val relayManager: SnackbarRelayManager<SnackbarRelay> = mockk {
        every { sendSnackbarData(data = any(), relay = any()) } just runs
    }

    @BeforeEach
    fun setup() {
        mockkStatic(SavedStateHandle::toCollectionAddEditArgs)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SavedStateHandle::toCollectionAddEditArgs)
    }

    @Test
    fun `initial add state should be correct`() = runTest {
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = DEFAULT_ADD_STATE,
            ),
        )
        assertEquals(DEFAULT_ADD_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial edit state should be correct`() = runTest {
        val editType = CollectionAddEditType.EditItem(
            collectionId = DEFAULT_EDIT_COLLECTION_ID,
            organizationId = DEFAULT_ORGANIZATION_ID,
        )
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = DEFAULT_ADD_STATE.copy(
                    collectionAddEditType = editType,
                ),
            ),
        )
        assertEquals(
            DEFAULT_ADD_STATE.copy(
                collectionAddEditType = editType,
                viewState = CollectionAddEditState.ViewState.Loading,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(CollectionAddEditAction.CloseClick)
            assertEquals(CollectionAddEditEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `SaveClick with empty name should show error dialog`() = runTest {
        val stateWithEmptyName = DEFAULT_ADD_STATE.copy(
            viewState = CollectionAddEditState.ViewState.Content(
                collectionName = "",
            ),
        )
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = stateWithEmptyName,
            ),
        )

        assertEquals(stateWithEmptyName, viewModel.stateFlow.value)

        viewModel.trySendAction(CollectionAddEditAction.SaveClick)

        assertEquals(
            stateWithEmptyName.copy(
                dialog = CollectionAddEditState.DialogState.Error(
                    message = BitwardenString.validation_field_required
                        .asText(BitwardenString.name.asText()),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SaveClick with slash in name should show slash error dialog`() = runTest {
        val stateWithSlash = DEFAULT_ADD_STATE.copy(
            viewState = CollectionAddEditState.ViewState.Content(
                collectionName = "test/collection",
            ),
        )
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = stateWithSlash,
            ),
        )

        viewModel.trySendAction(CollectionAddEditAction.SaveClick)

        assertEquals(
            stateWithSlash.copy(
                dialog = CollectionAddEditState.DialogState.Error(
                    message = BitwardenString.collection_name_slash_error.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode, SaveClick createCollection success should show dialog and remove it once saved`() =
        runTest {
            val stateWithDialog = DEFAULT_ADD_STATE.copy(
                dialog = CollectionAddEditState.DialogState.Loading(
                    BitwardenString.saving.asText(),
                ),
                viewState = CollectionAddEditState.ViewState.Content(
                    collectionName = DEFAULT_COLLECTION_NAME,
                ),
            )

            val stateWithoutDialog = stateWithDialog.copy(dialog = null)

            val viewModel = createViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = stateWithoutDialog,
                ),
            )

            coEvery {
                vaultRepository.createCollection(
                    organizationId = any(),
                    organizationUserId = any(),
                    collectionView = any(),
                )
            } returns CreateCollectionResult.Success(mockk())

            viewModel.stateFlow.test {
                viewModel.trySendAction(CollectionAddEditAction.SaveClick)
                assertEquals(stateWithoutDialog, awaitItem())
                assertEquals(stateWithDialog, awaitItem())
                assertEquals(stateWithoutDialog, awaitItem())
            }
            verify(exactly = 1) {
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(
                        BitwardenString.collection_created.asText(),
                    ),
                    relay = SnackbarRelay.COLLECTION_CREATED,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode, SaveClick createCollection error should show error dialog`() =
        runTest {
            val state = DEFAULT_ADD_STATE.copy(
                dialog = null,
                viewState = CollectionAddEditState.ViewState.Content(
                    collectionName = DEFAULT_COLLECTION_NAME,
                ),
            )

            val viewModel = createViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = state,
                ),
            )

            val error = Throwable("Oops")
            coEvery {
                vaultRepository.createCollection(
                    organizationId = any(),
                    organizationUserId = any(),
                    collectionView = any(),
                )
            } returns CreateCollectionResult.Error(error = error)

            viewModel.trySendAction(CollectionAddEditAction.SaveClick)

            assertEquals(
                state.copy(
                    dialog = CollectionAddEditState.DialogState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                        throwable = error,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `in edit mode, SaveClick should call updateCollection`() = runTest {
        val editType = CollectionAddEditType.EditItem(
            collectionId = DEFAULT_EDIT_COLLECTION_ID,
            organizationId = DEFAULT_ORGANIZATION_ID,
        )
        val stateWithDialog = CollectionAddEditState(
            collectionAddEditType = editType,
            dialog = CollectionAddEditState.DialogState.Loading(
                BitwardenString.saving.asText(),
            ),
            viewState = CollectionAddEditState.ViewState.Content(
                collectionName = DEFAULT_COLLECTION_NAME,
            ),
        )

        val stateWithoutDialog = stateWithDialog.copy(dialog = null)

        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = stateWithoutDialog,
            ),
        )

        mutableCollectionsStateFlow.value = DataState.Loaded(
            listOf(createMockCollectionView(number = 1)),
        )

        coEvery {
            vaultRepository.updateCollection(
                organizationId = any(),
                collectionId = any(),
                collectionView = any(),
            )
        } returns UpdateCollectionResult.Success(mockk())

        viewModel.stateFlow.test {
            viewModel.trySendAction(CollectionAddEditAction.SaveClick)
            assertEquals(stateWithoutDialog, awaitItem())
            assertEquals(stateWithDialog, awaitItem())
            assertEquals(stateWithoutDialog, awaitItem())
        }
        verify(exactly = 1) {
            relayManager.sendSnackbarData(
                data = BitwardenSnackbarData(
                    BitwardenString.collection_updated.asText(),
                ),
                relay = SnackbarRelay.COLLECTION_UPDATED,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in edit mode, SaveClick updateCollection error should show error dialog`() =
        runTest {
            val editType = CollectionAddEditType.EditItem(
                collectionId = DEFAULT_EDIT_COLLECTION_ID,
                organizationId = DEFAULT_ORGANIZATION_ID,
            )
            val state = CollectionAddEditState(
                collectionAddEditType = editType,
                dialog = null,
                viewState = CollectionAddEditState.ViewState.Content(
                    collectionName = DEFAULT_COLLECTION_NAME,
                ),
            )

            val viewModel = createViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = state,
                ),
            )

            mutableCollectionsStateFlow.value = DataState.Loaded(
                listOf(createMockCollectionView(number = 1)),
            )

            val error = Throwable("Oops")
            coEvery {
                vaultRepository.updateCollection(
                    organizationId = any(),
                    collectionId = any(),
                    collectionView = any(),
                )
            } returns UpdateCollectionResult.Error(error = error)

            viewModel.trySendAction(CollectionAddEditAction.SaveClick)

            assertEquals(
                state.copy(
                    dialog = CollectionAddEditState.DialogState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                        throwable = error,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `DeleteClick should call deleteCollection and navigate back on success`() =
        runTest {
            val editType = CollectionAddEditType.EditItem(
                collectionId = DEFAULT_EDIT_COLLECTION_ID,
                organizationId = DEFAULT_ORGANIZATION_ID,
            )
            val state = CollectionAddEditState(
                collectionAddEditType = editType,
                dialog = null,
                viewState = CollectionAddEditState.ViewState.Content(
                    collectionName = DEFAULT_COLLECTION_NAME,
                ),
            )

            val viewModel = createViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = state,
                ),
            )

            mutableCollectionsStateFlow.value = DataState.Loaded(
                listOf(createMockCollectionView(number = 1)),
            )

            coEvery {
                vaultRepository.deleteCollection(
                    organizationId = DEFAULT_ORGANIZATION_ID,
                    collectionId = DEFAULT_EDIT_COLLECTION_ID,
                )
            } returns DeleteCollectionResult.Success

            viewModel.trySendAction(CollectionAddEditAction.DeleteClick)

            viewModel.eventFlow.test {
                assertEquals(
                    CollectionAddEditEvent.NavigateBack,
                    awaitItem(),
                )
            }
            verify(exactly = 1) {
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(
                        BitwardenString.collection_deleted.asText(),
                    ),
                    relay = SnackbarRelay.COLLECTION_DELETED,
                )
            }
        }

    @Test
    fun `DeleteClick with error should show error dialog`() = runTest {
        val editType = CollectionAddEditType.EditItem(
            collectionId = DEFAULT_EDIT_COLLECTION_ID,
            organizationId = DEFAULT_ORGANIZATION_ID,
        )
        val state = CollectionAddEditState(
            collectionAddEditType = editType,
            dialog = null,
            viewState = CollectionAddEditState.ViewState.Content(
                collectionName = DEFAULT_COLLECTION_NAME,
            ),
        )

        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = state,
            ),
        )

        mutableCollectionsStateFlow.value = DataState.Loaded(
            listOf(createMockCollectionView(number = 1)),
        )

        val error = Throwable("Oops")
        coEvery {
            vaultRepository.deleteCollection(
                organizationId = DEFAULT_ORGANIZATION_ID,
                collectionId = DEFAULT_EDIT_COLLECTION_ID,
            )
        } returns DeleteCollectionResult.Error(error = error)

        viewModel.trySendAction(CollectionAddEditAction.DeleteClick)

        assertEquals(
            state.copy(
                dialog = CollectionAddEditState.DialogState.Error(
                    message = BitwardenString.generic_error_message.asText(),
                    throwable = error,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `DeleteClick should not call deleteCollection if no collectionId`() =
        runTest {
            val viewModel = createViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = DEFAULT_ADD_STATE.copy(
                        viewState = CollectionAddEditState.ViewState.Content(
                            collectionName = DEFAULT_COLLECTION_NAME,
                        ),
                    ),
                ),
            )

            viewModel.trySendAction(CollectionAddEditAction.DeleteClick)

            coVerify(exactly = 0) {
                vaultRepository.deleteCollection(any(), any())
            }
        }

    @Test
    fun `DismissDialog should clear dialog state`() = runTest {
        val stateWithDialog = DEFAULT_ADD_STATE.copy(
            dialog = CollectionAddEditState.DialogState.Error(
                message = BitwardenString.generic_error_message.asText(),
            ),
            viewState = CollectionAddEditState.ViewState.Content(
                collectionName = "",
            ),
        )

        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = stateWithDialog,
            ),
        )

        viewModel.trySendAction(CollectionAddEditAction.DismissDialog)

        assertEquals(
            stateWithDialog.copy(dialog = null),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `NameTextChange should update content state`() = runTest {
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = DEFAULT_ADD_STATE.copy(
                    viewState = CollectionAddEditState.ViewState.Content(
                        collectionName = "",
                    ),
                ),
            ),
        )

        viewModel.trySendAction(
            CollectionAddEditAction.NameTextChange("NewName"),
        )

        assertEquals(
            DEFAULT_ADD_STATE.copy(
                viewState = CollectionAddEditState.ViewState.Content(
                    collectionName = "NewName",
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CreateCollectionResultReceive Success should send snackbar and navigate back`() =
        runTest {
            val state = DEFAULT_ADD_STATE.copy(
                dialog = null,
                viewState = CollectionAddEditState.ViewState.Content(
                    collectionName = DEFAULT_COLLECTION_NAME,
                ),
            )

            val viewModel = createViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = state,
                ),
            )

            coEvery {
                vaultRepository.createCollection(
                    organizationId = any(),
                    organizationUserId = any(),
                    collectionView = any(),
                )
            } returns CreateCollectionResult.Success(mockk())

            viewModel.trySendAction(CollectionAddEditAction.SaveClick)

            viewModel.eventFlow.test {
                assertEquals(
                    CollectionAddEditEvent.NavigateBack,
                    awaitItem(),
                )
            }
            verify(exactly = 1) {
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(
                        BitwardenString.collection_created.asText(),
                    ),
                    relay = SnackbarRelay.COLLECTION_CREATED,
                )
            }
        }

    @Test
    fun `CreateCollectionResultReceive Error should show error dialog`() =
        runTest {
            val state = DEFAULT_ADD_STATE.copy(
                dialog = null,
                viewState = CollectionAddEditState.ViewState.Content(
                    collectionName = DEFAULT_COLLECTION_NAME,
                ),
            )

            val viewModel = createViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = state,
                ),
            )

            val error = Throwable("Oops")
            coEvery {
                vaultRepository.createCollection(
                    organizationId = any(),
                    organizationUserId = any(),
                    collectionView = any(),
                )
            } returns CreateCollectionResult.Error(error = error)

            viewModel.trySendAction(CollectionAddEditAction.SaveClick)

            assertEquals(
                state.copy(
                    dialog = CollectionAddEditState.DialogState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                        throwable = error,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `DeleteCollectionResultReceive Success should send snackbar and navigate back`() =
        runTest {
            val editType = CollectionAddEditType.EditItem(
                collectionId = DEFAULT_EDIT_COLLECTION_ID,
                organizationId = DEFAULT_ORGANIZATION_ID,
            )
            val state = CollectionAddEditState(
                collectionAddEditType = editType,
                dialog = null,
                viewState = CollectionAddEditState.ViewState.Content(
                    collectionName = DEFAULT_COLLECTION_NAME,
                ),
            )

            val viewModel = createViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = state,
                ),
            )

            mutableCollectionsStateFlow.value = DataState.Loaded(
                listOf(createMockCollectionView(number = 1)),
            )

            coEvery {
                vaultRepository.deleteCollection(
                    organizationId = DEFAULT_ORGANIZATION_ID,
                    collectionId = DEFAULT_EDIT_COLLECTION_ID,
                )
            } returns DeleteCollectionResult.Success

            viewModel.trySendAction(CollectionAddEditAction.DeleteClick)

            viewModel.eventFlow.test {
                assertEquals(
                    CollectionAddEditEvent.NavigateBack,
                    awaitItem(),
                )
            }
            verify(exactly = 1) {
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(
                        BitwardenString.collection_deleted.asText(),
                    ),
                    relay = SnackbarRelay.COLLECTION_DELETED,
                )
            }
        }

    @Test
    fun `VaultDataReceive should not overwrite Content state`() {
        val editType = CollectionAddEditType.EditItem(
            collectionId = DEFAULT_EDIT_COLLECTION_ID,
            organizationId = DEFAULT_ORGANIZATION_ID,
        )
        val contentState = CollectionAddEditState(
            collectionAddEditType = editType,
            dialog = null,
            viewState = CollectionAddEditState.ViewState.Content(
                collectionName = "user-edited-name",
            ),
        )

        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = contentState,
            ),
        )

        // Emit new data from vault — should NOT overwrite the Content state.
        mutableCollectionsStateFlow.tryEmit(
            DataState.Loaded(
                listOf(
                    createMockCollectionView(
                        number = 1,
                        name = "server-name",
                    ),
                ),
            ),
        )

        // State should still have the user-edited name, not the server name.
        assertEquals(
            contentState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `VaultDataReceive Loaded should update edit state to Content`() {
        val editType = CollectionAddEditType.EditItem(
            collectionId = DEFAULT_EDIT_COLLECTION_ID,
            organizationId = DEFAULT_ORGANIZATION_ID,
        )
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = DEFAULT_ADD_STATE.copy(
                    collectionAddEditType = editType,
                    viewState = CollectionAddEditState.ViewState.Loading,
                ),
            ),
        )

        mutableCollectionsStateFlow.tryEmit(
            DataState.Loaded(
                listOf(
                    createMockCollectionView(
                        number = 1,
                        name = DEFAULT_COLLECTION_NAME,
                    ),
                ),
            ),
        )

        assertEquals(
            DEFAULT_ADD_STATE.copy(
                collectionAddEditType = editType,
                viewState = CollectionAddEditState.ViewState.Content(
                    collectionName = DEFAULT_COLLECTION_NAME,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `VaultDataReceive Error should update edit state to Error`() {
        val editType = CollectionAddEditType.EditItem(
            collectionId = DEFAULT_EDIT_COLLECTION_ID,
            organizationId = DEFAULT_ORGANIZATION_ID,
        )
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = DEFAULT_ADD_STATE.copy(
                    collectionAddEditType = editType,
                    viewState = CollectionAddEditState.ViewState.Loading,
                ),
            ),
        )

        mutableCollectionsStateFlow.tryEmit(
            DataState.Error(
                data = emptyList(),
                error = IllegalStateException(),
            ),
        )

        assertEquals(
            DEFAULT_ADD_STATE.copy(
                collectionAddEditType = editType,
                viewState = CollectionAddEditState.ViewState.Error(
                    message = BitwardenString.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    private fun createSavedStateHandleWithState(
        state: CollectionAddEditState? = DEFAULT_ADD_STATE,
    ) = SavedStateHandle().apply {
        val collectionAddEditType = state?.collectionAddEditType
            ?: CollectionAddEditType.AddItem(
                organizationId = DEFAULT_ORGANIZATION_ID,
            )
        set("state", state)
        every { toCollectionAddEditArgs() } returns CollectionAddEditArgs(
            collectionAddEditType = collectionAddEditType,
        )
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = createSavedStateHandleWithState(),
    ): CollectionAddEditViewModel = CollectionAddEditViewModel(
        savedStateHandle = savedStateHandle,
        authRepository = authRepository,
        vaultRepository = vaultRepository,
        relayManager = relayManager,
    )
}

private const val DEFAULT_ORGANIZATION_ID = "mockId-1"
private const val DEFAULT_EDIT_COLLECTION_ID = "mockId-1"
private const val DEFAULT_COLLECTION_NAME = "test_collection"

private val DEFAULT_ORGANIZATION = createMockOrganization(
    number = 1,
    id = DEFAULT_ORGANIZATION_ID,
    role = OrganizationType.ADMIN,
    organizationUserId = "mockOrgUserId-1",
)

private val DEFAULT_ADD_STATE = CollectionAddEditState(
    collectionAddEditType = CollectionAddEditType.AddItem(
        organizationId = DEFAULT_ORGANIZATION_ID,
    ),
    viewState = CollectionAddEditState.ViewState.Loading,
    dialog = null,
)
