package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.CipherView
import com.bitwarden.core.UriMatchType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.util.FakeGeneratorRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldType
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.toCustomField
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toViewState
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import com.x8bit.bitwarden.ui.vault.model.VaultIdentityTitle
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

@Suppress("LargeClass")
class VaultAddEditViewModelTest : BaseViewModelTest() {

    private val authRepository: AuthRepository = mockk()

    private val loginInitialState = createVaultAddItemState(
        typeContentViewState = createLoginTypeContentViewState(),
    )
    private val loginInitialSavedStateHandle = createSavedStateHandleWithState(
        state = loginInitialState,
        vaultAddEditType = VaultAddEditType.AddItem,
    )

    private val totpTestCodeFlow: MutableSharedFlow<TotpCodeResult> = bufferedMutableSharedFlow()

    private val mutableVaultItemFlow = MutableStateFlow<DataState<CipherView?>>(DataState.Loading)
    private val resourceManager: ResourceManager = mockk()
    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val vaultRepository: VaultRepository = mockk {
        every { getVaultItemStateFlow(DEFAULT_EDIT_ITEM_ID) } returns mutableVaultItemFlow
        every { totpCodeFlow } returns totpTestCodeFlow
    }

    private val generatorRepository: GeneratorRepository = FakeGeneratorRepository()

    @BeforeEach
    fun setup() {
        mockkStatic(CipherView::toViewState)
        mockkStatic(UUID::randomUUID)
        every { UUID.randomUUID().toString() } returns TEST_ID
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(CipherView::toViewState)
        unmockkStatic(CustomFieldType::toCustomField)
    }

    @Test
    fun `initial state should be correct when state is null`() = runTest {
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = null,
                vaultAddEditType = VaultAddEditType.AddItem,
            ),
        )
        viewModel.stateFlow.test {
            assertEquals(
                loginInitialState,
                awaitItem(),
            )
        }
    }

    @Test
    fun `initial add state should be correct`() = runTest {
        val vaultAddEditType = VaultAddEditType.AddItem
        val initState = createVaultAddItemState(vaultAddEditType = vaultAddEditType)
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = vaultAddEditType,
            ),
        )
        assertEquals(initState, viewModel.stateFlow.value)
        verify(exactly = 0) {
            vaultRepository.getVaultItemStateFlow(DEFAULT_EDIT_ITEM_ID)
        }
    }

    @Test
    fun `initial edit state should be correct`() = runTest {
        val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
        val initState = createVaultAddItemState(vaultAddEditType = vaultAddEditType)
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = vaultAddEditType,
            ),
        )
        assertEquals(
            initState.copy(viewState = VaultAddEditState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            vaultRepository.getVaultItemStateFlow(DEFAULT_EDIT_ITEM_ID)
        }
    }

    @Test
    fun `initial clone state should be correct`() = runTest {
        val vaultAddEditType = VaultAddEditType.CloneItem(DEFAULT_EDIT_ITEM_ID)
        val initState = createVaultAddItemState(vaultAddEditType = vaultAddEditType)
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = vaultAddEditType,
            ),
        )
        assertEquals(
            initState.copy(viewState = VaultAddEditState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            vaultRepository.getVaultItemStateFlow(DEFAULT_EDIT_ITEM_ID)
        }
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createAddVaultItemViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultAddEditAction.Common.CloseClick)
            assertEquals(VaultAddEditEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `AttachmentsClick should emit NavigateToAttachments`() = runTest {
        val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
        val initState = createVaultAddItemState(vaultAddEditType = vaultAddEditType)
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = vaultAddEditType,
            ),
        )
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultAddEditAction.Common.AttachmentsClick)
            assertEquals(
                VaultAddEditEvent.NavigateToAttachments(DEFAULT_EDIT_ITEM_ID),
                awaitItem(),
            )
        }
    }

    @Test
    fun `MoveToOrganizationClick should emit NavigateToMoveToOrganization`() = runTest {
        val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
        val initState = createVaultAddItemState(vaultAddEditType = vaultAddEditType)
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = vaultAddEditType,
            ),
        )
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultAddEditAction.Common.MoveToOrganizationClick)
            assertEquals(
                VaultAddEditEvent.NavigateToMoveToOrganization(DEFAULT_EDIT_ITEM_ID),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CollectionsClick should emit NavigateToCollections`() = runTest {
        val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
        val initState = createVaultAddItemState(vaultAddEditType = vaultAddEditType)
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = vaultAddEditType,
            ),
        )
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultAddEditAction.Common.CollectionsClick)
            assertEquals(
                VaultAddEditEvent.NavigateToCollections(DEFAULT_EDIT_ITEM_ID),
                awaitItem(),
            )
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `ConfirmDeleteClick with DeleteCipherResult Success should emit ShowToast and NavigateBack`() =
        runTest {
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val initState = createVaultAddItemState(vaultAddEditType = vaultAddEditType)
            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = initState,
                    vaultAddEditType = vaultAddEditType,
                ),
            )
            mutableVaultItemFlow.value = DataState.Loaded(data = createMockCipherView(number = 1))

            coEvery {
                vaultRepository.softDeleteCipher(
                    cipherId = "mockId-1",
                    cipherView = createMockCipherView(number = 1),
                )
            } returns DeleteCipherResult.Success

            viewModel.trySendAction(VaultAddEditAction.Common.ConfirmDeleteClick)

            viewModel.eventFlow.test {
                assertEquals(
                    VaultAddEditEvent.ShowToast(R.string.item_soft_deleted.asText()),
                    awaitItem(),
                )
                assertEquals(
                    VaultAddEditEvent.NavigateBack,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `ConfirmDeleteClick with DeleteCipherResult Failure should show generic error`() =
        runTest {
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val initState = createVaultAddItemState(vaultAddEditType = vaultAddEditType)
            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = initState,
                    vaultAddEditType = vaultAddEditType,
                ),
            )
            mutableVaultItemFlow.value = DataState.Loaded(data = createMockCipherView(number = 1))

            coEvery {
                vaultRepository.softDeleteCipher(
                    cipherId = "mockId-1",
                    cipherView = createMockCipherView(number = 1),
                )
            } returns DeleteCipherResult.Error

            viewModel.trySendAction(VaultAddEditAction.Common.ConfirmDeleteClick)

            assertEquals(
                createVaultAddItemState(
                    vaultAddEditType = vaultAddEditType,
                    dialogState = VaultAddEditState.DialogState.Generic(
                        message = R.string.generic_error_message.asText(),
                    ),
                    commonContentViewState = createCommonContentViewState(
                        name = "mockName-1",
                        folder = "mockId-1".asText(),
                        ownership = "",
                        originalCipher = createMockCipherView(number = 1),
                        availableFolders = emptyList(),
                        availableOwners = emptyList(),
                        notes = "mockNotes-1",
                        customFieldData = listOf(
                            VaultAddEditState.Custom.HiddenField(
                                itemId = "testId",
                                name = "mockName-1",
                                value = "mockValue-1",
                            ),
                        ),
                    ),
                    typeContentViewState = createLoginTypeContentViewState(
                        username = "mockUsername-1",
                        password = "mockPassword-1",
                        uri = listOf(UriItem("testId", "www.mockuri1.com", UriMatchType.HOST)),
                        totpCode = "mockTotp-1",
                        canViewPassword = false,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `in add mode, SaveClick should show dialog, and remove it once an item is saved`() =
        runTest {
            val stateWithDialog = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem,
                dialogState = VaultAddEditState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName",
                ),
            )

            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem,
                commonContentViewState = createCommonContentViewState(
                    name = "mockName",
                ),
            )

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                ),
            )

            coEvery {
                vaultRepository.createCipher(any())
            } returns CreateCipherResult.Success

            viewModel.stateFlow.test {
                viewModel.actionChannel.trySend(VaultAddEditAction.Common.SaveClick)
                assertEquals(stateWithName, awaitItem())
                assertEquals(stateWithDialog, awaitItem())
                assertEquals(stateWithName, awaitItem())
            }

            coVerify(exactly = 1) {
                vaultRepository.createCipher(any())
            }
        }

    @Test
    fun `in add mode, SaveClick should update value to loading`() = runTest {
        val stateWithName = createVaultAddItemState(
            vaultAddEditType = VaultAddEditType.AddItem,
            commonContentViewState = createCommonContentViewState(
                name = "mockName",
            ),
        )

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = stateWithName,
                vaultAddEditType = VaultAddEditType.AddItem,
            ),
        )

        coEvery {
            vaultRepository.createCipher(any())
        } returns CreateCipherResult.Success
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultAddEditAction.Common.SaveClick)
            assertEquals(VaultAddEditEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `in add mode, SaveClick createCipher error should emit ShowToast`() = runTest {
        val stateWithName = createVaultAddItemState(
            vaultAddEditType = VaultAddEditType.AddItem,
            commonContentViewState = createCommonContentViewState(
                name = "mockName",
            ),
        )

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = stateWithName,
                vaultAddEditType = VaultAddEditType.AddItem,
            ),
        )

        coEvery {
            vaultRepository.createCipher(any())
        } returns CreateCipherResult.Error
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultAddEditAction.Common.SaveClick)
            assertEquals(VaultAddEditEvent.ShowToast("Save Item Failure".asText()), awaitItem())
        }
    }

    @Test
    fun `in edit mode, SaveClick should show dialog, and remove it once an item is saved`() =
        runTest {
            val cipherView = mockk<CipherView>()
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val stateWithDialog = createVaultAddItemState(
                vaultAddEditType = vaultAddEditType,
                dialogState = VaultAddEditState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName",
                ),
            )

            val stateWithName = createVaultAddItemState(
                vaultAddEditType = vaultAddEditType,
                commonContentViewState = createCommonContentViewState(
                    name = "mockName",
                ),
            )
            every {
                cipherView.toViewState(
                    isClone = false,
                    resourceManager = resourceManager,
                )
            } returns stateWithName.viewState
            mutableVaultItemFlow.value = DataState.Loaded(cipherView)

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                ),
            )

            coEvery {
                vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
            } returns UpdateCipherResult.Success

            viewModel.stateFlow.test {
                assertEquals(stateWithName, awaitItem())
                viewModel.actionChannel.trySend(VaultAddEditAction.Common.SaveClick)
                assertEquals(stateWithDialog, awaitItem())
                assertEquals(stateWithName, awaitItem())
            }

            coVerify(exactly = 1) {
                cipherView.toViewState(
                    isClone = false,
                    resourceManager = resourceManager,
                )
                vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in edit mode, SaveClick updateCipher error with a null message should show an error dialog with a generic message`() =
        runTest {
            val cipherView = mockk<CipherView>()
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = vaultAddEditType,
                commonContentViewState = createCommonContentViewState(
                    name = "mockName",
                ),
            )

            every {
                cipherView.toViewState(
                    isClone = false,
                    resourceManager = resourceManager,
                )
            } returns stateWithName.viewState
            coEvery {
                vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
            } returns UpdateCipherResult.Error(errorMessage = null)
            mutableVaultItemFlow.value = DataState.Loaded(cipherView)

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                ),
            )

            viewModel.actionChannel.trySend(VaultAddEditAction.Common.SaveClick)

            assertEquals(
                stateWithName.copy(
                    dialog = VaultAddEditState.DialogState.Generic(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.generic_error_message.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
            coVerify(exactly = 1) {
                vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in edit mode, SaveClick updateCipher error with a non-null message should show an error dialog with that message`() =
        runTest {
            val cipherView = mockk<CipherView>()
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = vaultAddEditType,
                commonContentViewState = createCommonContentViewState(
                    name = "mockName",
                ),
            )
            val errorMessage = "You do not have permission to edit this."

            every {
                cipherView.toViewState(
                    isClone = false,
                    resourceManager = resourceManager,
                )
            } returns stateWithName.viewState
            coEvery {
                vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
            } returns UpdateCipherResult.Error(errorMessage = errorMessage)
            mutableVaultItemFlow.value = DataState.Loaded(cipherView)

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                ),
            )

            viewModel.actionChannel.trySend(VaultAddEditAction.Common.SaveClick)

            assertEquals(
                stateWithName.copy(
                    dialog = VaultAddEditState.DialogState.Generic(
                        title = R.string.an_error_has_occurred.asText(),
                        message = errorMessage.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
            coVerify(exactly = 1) {
                vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
            }
        }

    @Test
    fun `Saving item with an empty name field will cause a dialog to show up`() = runTest {
        val stateWithNoName = createVaultAddItemState(
            commonContentViewState = createCommonContentViewState(name = ""),
        )

        val stateWithNoNameAndDialog = createVaultAddItemState(
            commonContentViewState = createCommonContentViewState(name = ""),
            dialogState = VaultAddEditState.DialogState.Generic(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.validation_field_required.asText(R.string.name.asText()),
            ),
        )

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = stateWithNoName,
                vaultAddEditType = VaultAddEditType.AddItem,
            ),
        )
        coEvery { vaultRepository.createCipher(any()) } returns CreateCipherResult.Success
        viewModel.stateFlow.test {
            viewModel.actionChannel.trySend(VaultAddEditAction.Common.SaveClick)
            assertEquals(stateWithNoName, awaitItem())
            assertEquals(stateWithNoNameAndDialog, awaitItem())
        }
    }

    @Test
    fun `HandleDialogDismiss will remove the current dialog`() = runTest {
        val errorState = createVaultAddItemState(
            vaultAddEditType = VaultAddEditType.AddItem,
            dialogState = VaultAddEditState.DialogState.Generic(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.validation_field_required.asText(R.string.name.asText()),
            ),
        )

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = errorState,
                vaultAddEditType = VaultAddEditType.AddItem,
            ),
        )

        coEvery { vaultRepository.createCipher(any()) } returns CreateCipherResult.Success
        viewModel.stateFlow.test {
            viewModel.actionChannel.trySend(VaultAddEditAction.Common.DismissDialog)
            assertEquals(errorState, awaitItem())
            assertEquals(null, awaitItem().dialog)
        }
    }

    @Test
    fun `TypeOptionSelect LOGIN should switch to LoginItem`() = runTest {
        val viewModel = createAddVaultItemViewModel()
        val action = VaultAddEditAction.Common.TypeOptionSelect(
            VaultAddEditState.ItemTypeOption.LOGIN,
        )

        viewModel.actionChannel.trySend(action)

        val expectedState = loginInitialState.copy(
            viewState = VaultAddEditState.ViewState.Content(
                common = createCommonContentViewState(),
                type = createLoginTypeContentViewState(),
                previousItemTypes = mapOf(
                    VaultAddEditState.ItemTypeOption.LOGIN
                        to VaultAddEditState.ViewState.Content.ItemType.Login(),
                ),
            ),
        )

        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `TypeOptionSelect CARD should switch to CardItem`() = runTest {
        val viewModel = createAddVaultItemViewModel()
        val action = VaultAddEditAction.Common.TypeOptionSelect(
            VaultAddEditState.ItemTypeOption.CARD,
        )

        viewModel.actionChannel.trySend(action)

        val expectedState = loginInitialState.copy(
            viewState = VaultAddEditState.ViewState.Content(
                common = createCommonContentViewState(),
                type = VaultAddEditState.ViewState.Content.ItemType.Card(),
                previousItemTypes = mapOf(
                    VaultAddEditState.ItemTypeOption.LOGIN
                        to VaultAddEditState.ViewState.Content.ItemType.Login(),
                ),
            ),
        )

        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `TypeOptionSelect IDENTITY should switch to IdentityItem`() = runTest {
        val viewModel = createAddVaultItemViewModel()
        val action = VaultAddEditAction.Common.TypeOptionSelect(
            VaultAddEditState.ItemTypeOption.IDENTITY,
        )

        viewModel.actionChannel.trySend(action)

        val expectedState = loginInitialState.copy(
            viewState = VaultAddEditState.ViewState.Content(
                common = createCommonContentViewState(),
                type = VaultAddEditState.ViewState.Content.ItemType.Identity(),
                previousItemTypes = mapOf(
                    VaultAddEditState.ItemTypeOption.LOGIN
                        to VaultAddEditState.ViewState.Content.ItemType.Login(),
                ),
            ),
        )

        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `TypeOptionSelect SECURE_NOTES should switch to SecureNotesItem`() = runTest {
        val viewModel = createAddVaultItemViewModel()
        val action = VaultAddEditAction.Common.TypeOptionSelect(
            VaultAddEditState.ItemTypeOption.SECURE_NOTES,
        )

        viewModel.actionChannel.trySend(action)

        val expectedState = loginInitialState.copy(
            viewState = VaultAddEditState.ViewState.Content(
                common = createCommonContentViewState(),
                type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                previousItemTypes = mapOf(
                    VaultAddEditState.ItemTypeOption.LOGIN
                        to VaultAddEditState.ViewState.Content.ItemType.Login(),
                ),
            ),
        )

        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Nested
    inner class VaultAddEditLoginTypeItemActions {
        private lateinit var viewModel: VaultAddEditViewModel

        @BeforeEach
        fun setup() {
            viewModel = createAddVaultItemViewModel()
        }

        @Test
        fun `UsernameTextChange should update username in LoginItem`() = runTest {
            val action = VaultAddEditAction.ItemType.LoginType.UsernameTextChange("newUsername")
            val expectedState = createVaultAddItemState(
                typeContentViewState = createLoginTypeContentViewState(
                    username = "newUsername",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `PasswordTextChange should update password in LoginItem`() = runTest {
            val action = VaultAddEditAction.ItemType.LoginType.PasswordTextChange("newPassword")

            viewModel.actionChannel.trySend(action)

            val expectedState = createVaultAddItemState(
                typeContentViewState = createLoginTypeContentViewState(
                    password = "newPassword",
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `UriTextChange should update uri in LoginItem`() = runTest {
            val action = VaultAddEditAction.ItemType.LoginType.UriTextChange(
                UriItem("testId", "TestUri", null),
            )

            viewModel.actionChannel.trySend(action)

            val expectedState = createVaultAddItemState(
                typeContentViewState = createLoginTypeContentViewState(
                    uri = listOf(UriItem("testId", "TestUri", null)),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `OpenUsernameGeneratorClick should emit NavigateToGeneratorModal with username GeneratorMode`() =
            runTest {
                val viewModel = createAddVaultItemViewModel()

                viewModel.eventFlow.test {
                    viewModel.actionChannel.trySend(
                        VaultAddEditAction.ItemType.LoginType.OpenUsernameGeneratorClick,
                    )
                    assertEquals(
                        VaultAddEditEvent.NavigateToGeneratorModal(GeneratorMode.Modal.Username),
                        awaitItem(),
                    )
                }
            }

        @Test
        fun `on CheckForBreachClick should process a password`() = runTest {
            val cipherView = createMockCipherView(1)
            val password = "Password"

            val loginState = loginInitialState.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = createCommonContentViewState(),
                    type = createLoginTypeContentViewState(
                        password = password,
                    ),
                ),
            )

            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = loginState,
                    vaultAddEditType = VaultAddEditType.AddItem,
                ),
            )

            mutableVaultItemFlow.value = DataState.Loaded(data = cipherView)

            val breachCount = 5
            coEvery {
                authRepository.getPasswordBreachCount(password)
            } returns BreachCountResult.Success(breachCount = breachCount)

            viewModel.stateFlow.test {
                assertEquals(loginState, awaitItem())
                viewModel.trySendAction(VaultAddEditAction.ItemType.LoginType.PasswordCheckerClick)
                assertEquals(
                    loginState.copy(
                        dialog = VaultAddEditState.DialogState.Loading(
                            label = R.string.loading.asText(),
                        ),
                    ),
                    awaitItem(),
                )

                assertEquals(
                    loginState.copy(
                        dialog = VaultAddEditState.DialogState.Generic(
                            message = R.string.password_exposed.asText(breachCount),
                        ),
                    ),
                    awaitItem(),
                )
            }

            coVerify(exactly = 1) { authRepository.getPasswordBreachCount(password) }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `OpenPasswordGeneratorClick should emit NavigateToGeneratorModal with with password GeneratorMode`() =
            runTest {
                val viewModel = createAddVaultItemViewModel()

                viewModel.eventFlow.test {
                    viewModel
                        .actionChannel
                        .trySend(VaultAddEditAction.ItemType.LoginType.OpenPasswordGeneratorClick)

                    assertEquals(
                        VaultAddEditEvent.NavigateToGeneratorModal(GeneratorMode.Modal.Password),
                        awaitItem(),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `SetupTotpClick should emit NavigateToQrCodeScan when isGranted is true`() = runTest {
            val viewModel = createAddVaultItemViewModel()

            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(
                    VaultAddEditAction.ItemType.LoginType.SetupTotpClick(
                        isGranted = true,
                    ),
                )
                assertEquals(
                    VaultAddEditEvent.NavigateToQrCodeScan,
                    awaitItem(),
                )
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `SetupTotpClick should emit NavigateToManualCodeEntry when isGranted is false`() =
            runTest {
                val viewModel = createAddVaultItemViewModel()

                viewModel.eventFlow.test {
                    viewModel.actionChannel.trySend(
                        VaultAddEditAction.ItemType.LoginType.SetupTotpClick(
                            isGranted = false,
                        ),
                    )

                    assertEquals(VaultAddEditEvent.NavigateToManualCodeEntry, awaitItem())
                }
            }

        @Test
        fun `CopyTotpKeyClick should call setText on ClipboardManager`() {
            val viewModel = createAddVaultItemViewModel()
            val testKey = "TestKey"
            every { clipboardManager.setText(text = testKey) } just runs

            viewModel.actionChannel.trySend(
                VaultAddEditAction.ItemType.LoginType.CopyTotpKeyClick(
                    testKey,
                ),
            )

            verify(exactly = 1) {
                clipboardManager.setText(text = testKey)
            }
        }

        @Test
        fun `ClearTotpKeyClick call should clear the totp code`() {
            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = createVaultAddItemState(
                        typeContentViewState = createLoginTypeContentViewState(
                            totpCode = "testCode",
                        ),
                    ),
                    vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID),
                ),
            )

            val expectedState = loginInitialState.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = createCommonContentViewState(),
                    type = createLoginTypeContentViewState(
                        totpCode = null,
                    ),
                ),
            )

            viewModel.actionChannel.trySend(
                VaultAddEditAction.ItemType.LoginType.ClearTotpKeyClick,
            )

            assertEquals(
                expectedState,
                viewModel.stateFlow.value,
            )
        }

        @Test
        fun `TotpCodeReceive should update totp code in state`() = runTest {
            val viewModel = createAddVaultItemViewModel()
            val result = TotpCodeResult.Success("TestKey")

            val expectedState = loginInitialState.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = createCommonContentViewState(),
                    type = createLoginTypeContentViewState(
                        totpCode = "TestKey",
                    ),
                ),
            )

            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(
                    VaultAddEditAction.Internal.TotpCodeReceive(
                        result,
                    ),
                )

                assertEquals(
                    VaultAddEditEvent.ShowToast(R.string.authenticator_key_added.asText()),
                    awaitItem(),
                )

                assertEquals(
                    expectedState,
                    viewModel.stateFlow.value,
                )
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UriSettingsClick should emit ShowToast with 'URI Settings' message`() = runTest {
            val viewModel = createAddVaultItemViewModel()

            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(VaultAddEditAction.ItemType.LoginType.UriSettingsClick)
                assertEquals(VaultAddEditEvent.ShowToast("URI Settings".asText()), awaitItem())
            }
        }

        @Test
        fun `AddNewUriClick should update state with another empty UriItem`() = runTest {
            val viewModel = createAddVaultItemViewModel()
            every { UUID.randomUUID().toString() } returns "testId2"

            viewModel.trySendAction(VaultAddEditAction.ItemType.LoginType.AddNewUriClick)

            val expectedState = createVaultAddItemState(
                typeContentViewState = createLoginTypeContentViewState().copy(
                    uriList = listOf(UriItem("testId", "", null), UriItem("testId2", "", null)),
                ),
            )

            assertEquals(
                expectedState,
                viewModel.stateFlow.value,

            )
        }
    }

    @Nested
    inner class VaultAddEditIdentityTypeItemActions {
        private lateinit var viewModel: VaultAddEditViewModel
        private lateinit var vaultAddItemInitialState: VaultAddEditState
        private lateinit var identityInitialSavedStateHandle: SavedStateHandle

        @BeforeEach
        fun setup() {
            vaultAddItemInitialState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(),
            )
            identityInitialSavedStateHandle = createSavedStateHandleWithState(
                state = vaultAddItemInitialState,
                vaultAddEditType = VaultAddEditType.AddItem,
            )
            viewModel = createAddVaultItemViewModel(
                savedStateHandle = identityInitialSavedStateHandle,
            )
        }

        @Test
        fun `FirstNameTextChange should update first name`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.FirstNameTextChange(
                firstName = "newFirstName",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    firstName = "newFirstName",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `MiddleNameTextChange should update middle name`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.MiddleNameTextChange(
                middleName = "newMiddleName",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    middleName = "newMiddleName",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `LastNameTextChange should update last name`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.LastNameTextChange(
                lastName = "newLastName",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    lastName = "newLastName",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `UsernameTextChange should update username`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.UsernameTextChange(
                username = "newUsername",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    username = "newUsername",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `CompanyTextChange should update company`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.CompanyTextChange(
                company = "newCompany",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    company = "newCompany",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `SsnTextChange should update SSN`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.SsnTextChange(
                ssn = "newSsn",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    ssn = "newSsn",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `PassportNumberTextChange should update passport number`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.PassportNumberTextChange(
                passportNumber = "newPassportNumber",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    passportNumber = "newPassportNumber",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `LicenseNumberTextChange should update license number`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.LicenseNumberTextChange(
                licenseNumber = "newLicenseNumber",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    licenseNumber = "newLicenseNumber",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `EmailTextChange should update email`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.EmailTextChange(
                email = "newEmail",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    email = "newEmail",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `PhoneTextChange should update phone`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.PhoneTextChange(
                phone = "newPhone",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    phone = "newPhone",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `Address1TextChange should update address1`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.Address1TextChange(
                address1 = "newAddress1",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    address1 = "newAddress1",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `Address2TextChange should update address2`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.Address2TextChange(
                address2 = "newAddress2",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    address2 = "newAddress2",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `Address3TextChange should update address3`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.Address3TextChange(
                address3 = "newAddress3",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    address3 = "newAddress3",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `CityTextChange should update city`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.CityTextChange(
                city = "newCity",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    city = "newCity",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `StateTextChange should update state text`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.StateTextChange(
                state = "newState",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    state = "newState",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `ZipTextChange should update zip`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.ZipTextChange(
                zip = "newZip",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    zip = "newZip",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `CountryTextChange should update country`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.CountryTextChange(
                country = "newCountry",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    country = "newCountry",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `TitleSelect should update title`() = runTest {
            val action = VaultAddEditAction.ItemType.IdentityType.TitleSelect(
                title = VaultIdentityTitle.MX,
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    selectedTitle = VaultIdentityTitle.MX,
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }
    }

    @Nested
    inner class VaultAddEditCardTypeItemActions {
        private lateinit var viewModel: VaultAddEditViewModel
        private lateinit var vaultAddItemInitialState: VaultAddEditState
        private lateinit var identityInitialSavedStateHandle: SavedStateHandle

        @BeforeEach
        fun setup() {
            vaultAddItemInitialState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Card(),
            )
            identityInitialSavedStateHandle = createSavedStateHandleWithState(
                state = vaultAddItemInitialState,
                vaultAddEditType = VaultAddEditType.AddItem,
            )
            viewModel = createAddVaultItemViewModel(
                savedStateHandle = identityInitialSavedStateHandle,
            )
        }

        @Test
        fun `CardHolderNameTextChange should update card holder name`() = runTest {
            val action = VaultAddEditAction.ItemType.CardType.CardHolderNameTextChange(
                cardHolderName = "newCardHolderName",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Card(
                    cardHolderName = "newCardHolderName",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `NumberTextChange should update number`() = runTest {
            val action = VaultAddEditAction.ItemType.CardType.NumberTextChange(
                number = "newNumber",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Card(
                    number = "newNumber",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `BrandSelect should update brand`() = runTest {
            val action = VaultAddEditAction.ItemType.CardType.BrandSelect(
                brand = VaultCardBrand.VISA,
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Card(
                    brand = VaultCardBrand.VISA,
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `ExpirationMonthSelect should update expiration month`() = runTest {
            val action = VaultAddEditAction.ItemType.CardType.ExpirationMonthSelect(
                expirationMonth = VaultCardExpirationMonth.JUNE,
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Card(
                    expirationMonth = VaultCardExpirationMonth.JUNE,
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `ExpirationYearTextChange should update expiration year`() = runTest {
            val action = VaultAddEditAction.ItemType.CardType.ExpirationYearTextChange(
                expirationYear = "newExpirationYear",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Card(
                    expirationYear = "newExpirationYear",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `SecurityCodeTextChange should update security code`() = runTest {
            val action = VaultAddEditAction.ItemType.CardType.SecurityCodeTextChange(
                securityCode = "newSecurityCode",
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Card(
                    securityCode = "newSecurityCode",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }
    }

    @Nested
    inner class VaultAddEditCommonActions {
        private lateinit var viewModel: VaultAddEditViewModel
        private lateinit var vaultAddItemInitialState: VaultAddEditState
        private lateinit var secureNotesInitialSavedStateHandle: SavedStateHandle

        @BeforeEach
        fun setup() {
            vaultAddItemInitialState = createVaultAddItemState()
            secureNotesInitialSavedStateHandle = createSavedStateHandleWithState(
                state = vaultAddItemInitialState,
                vaultAddEditType = VaultAddEditType.AddItem,
            )
            viewModel = VaultAddEditViewModel(
                savedStateHandle = secureNotesInitialSavedStateHandle,
                clipboardManager = clipboardManager,
                vaultRepository = vaultRepository,
                generatorRepository = generatorRepository,
                resourceManager = resourceManager,
                authRepository = authRepository,
            )
        }

        @Test
        fun `NameTextChange should update name`() = runTest {
            val action = VaultAddEditAction.Common.NameTextChange("newName")

            viewModel.actionChannel.trySend(action)

            val expectedState = vaultAddItemInitialState.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = createCommonContentViewState(
                        name = "newName",
                    ),
                    type = createLoginTypeContentViewState(),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `FolderChange should update folder`() = runTest {
            val action = VaultAddEditAction.Common.FolderChange(
                "newFolder".asText(),
            )

            viewModel.actionChannel.trySend(action)

            val expectedState = vaultAddItemInitialState.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = createCommonContentViewState(
                        folder = "newFolder".asText(),
                    ),
                    type = createLoginTypeContentViewState(),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `ToggleFavorite should update favorite`() = runTest {
            val action = VaultAddEditAction.Common.ToggleFavorite(true)

            viewModel.actionChannel.trySend(action)

            val expectedState = vaultAddItemInitialState.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = createCommonContentViewState(
                        favorite = true,
                    ),
                    type = createLoginTypeContentViewState(),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `ToggleMasterPasswordReprompt should update masterPasswordReprompt`() = runTest {
            val action =
                VaultAddEditAction.Common.ToggleMasterPasswordReprompt(
                    isMasterPasswordReprompt = true,
                )

            viewModel.actionChannel.trySend(action)

            val expectedState = vaultAddItemInitialState.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = createCommonContentViewState(
                        masterPasswordReprompt = true,
                    ),
                    type = createLoginTypeContentViewState(),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `NotesTextChange should update notes`() = runTest {
            val action =
                VaultAddEditAction.Common.NotesTextChange(notes = "newNotes")

            viewModel.actionChannel.trySend(action)

            val expectedState = vaultAddItemInitialState.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = createCommonContentViewState(
                        notes = "newNotes",
                    ),
                    type = createLoginTypeContentViewState(),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `OwnershipChange should update ownership`() = runTest {
            val action = VaultAddEditAction.Common.OwnershipChange(ownership = "newOwner")

            viewModel.actionChannel.trySend(action)

            val expectedState = vaultAddItemInitialState.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = createCommonContentViewState(
                        ownership = "newOwner",
                    ),
                    type = createLoginTypeContentViewState(),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `AddNewCustomFieldClick should allow a user to add a custom boolean field in Secure notes item`() =
            runTest {
                assertAddNewCustomFieldClick(
                    initialState = vaultAddItemInitialState,
                    type = CustomFieldType.BOOLEAN,
                )
            }

        @Suppress("MaxLineLength")
        @Test
        fun `AddNewCustomFieldClick should allow a user to add a custom hidden field in Secure notes item`() =
            runTest {
                assertAddNewCustomFieldClick(
                    initialState = vaultAddItemInitialState,
                    type = CustomFieldType.HIDDEN,
                )
            }

        @Suppress("MaxLineLength")
        @Test
        fun `AddNewCustomFieldClick should allow a user to add a custom text field in Secure notes item`() =
            runTest {

                assertAddNewCustomFieldClick(
                    initialState = vaultAddItemInitialState,
                    type = CustomFieldType.TEXT,
                )
            }

        @Test
        fun `CustomFieldValueChange should allow a user to update a text custom field`() = runTest {
            val initState = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem,
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                commonContentViewState = VaultAddEditState.ViewState.Content.Common(
                    customFieldData = listOf(
                        VaultAddEditState.Custom.TextField(
                            "TestId 1",
                            "Test Text",
                            "Test Text",
                        ),
                    ),
                ),
            )

            assertCustomFieldValueChange(
                initState,
                CustomFieldType.TEXT,
            )
        }

        @Test
        fun `CustomFieldValueChange should update hidden custom fields`() = runTest {
            val initState =
                createVaultAddItemState(
                    typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                    commonContentViewState = VaultAddEditState.ViewState.Content.Common(
                        customFieldData = listOf(
                            VaultAddEditState.Custom.HiddenField(
                                "TestId 2",
                                "Test Text",
                                "Test Text",
                            ),
                        ),
                    ),
                )

            assertCustomFieldValueChange(
                initState,
                CustomFieldType.HIDDEN,
            )
        }

        @Test
        fun `CustomFieldValueChange should update boolean custom fields`() = runTest {
            val initState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                commonContentViewState = VaultAddEditState.ViewState.Content.Common(
                    customFieldData = listOf(
                        VaultAddEditState.Custom.BooleanField(
                            "TestId 3",
                            "Boolean Field",
                            true,
                        ),
                    ),
                ),
            )

            assertCustomFieldValueChange(
                initState,
                CustomFieldType.BOOLEAN,
            )
        }

        @Test
        fun `CustomFieldValueChange should update name field`() = runTest {
            val initState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                commonContentViewState = VaultAddEditState.ViewState.Content.Common(
                    customFieldData = listOf(
                        VaultAddEditState.Custom.BooleanField(
                            "TestId 3",
                            "Boolean Field",
                            true,
                        ),
                    ),
                ),
            )

            assertCustomFieldValueChange(
                initState,
                CustomFieldType.BOOLEAN,
            )
        }

        @Test
        fun `CustomFieldActionSelect with delete action should delete the item`() = runTest {
            val customFieldData = VaultAddEditState.Custom.BooleanField(
                "TestId 3",
                "Boolean Field",
                true,
            )

            val initState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                commonContentViewState = VaultAddEditState.ViewState.Content.Common(
                    customFieldData = listOf(customFieldData),
                ),
            )

            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = initState,
                    vaultAddEditType = VaultAddEditType.AddItem,
                ),
            )
            val currentContentState =
                (viewModel.stateFlow.value.viewState as VaultAddEditState.ViewState.Content)
            val expectedState = currentContentState
                .copy(
                    common = currentContentState.common.copy(
                        customFieldData = listOf(),
                    ),
                )

            viewModel.actionChannel.trySend(
                VaultAddEditAction.Common.CustomFieldActionSelect(
                    CustomFieldAction.DELETE,
                    customFieldData,
                ),
            )

            assertEquals(
                expectedState,
                viewModel.stateFlow.value.viewState,
            )
        }

        @Test
        fun `CustomFieldActionSelect with move up action should move the item up`() = runTest {
            val customFieldData =
                VaultAddEditState.Custom.BooleanField(
                    "TestId 3",
                    "Boolean Field",
                    true,
                )

            val initState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                commonContentViewState = VaultAddEditState.ViewState.Content.Common(
                    customFieldData = listOf(
                        VaultAddEditState.Custom.BooleanField(
                            "TestId 1",
                            "Boolean Field",
                            true,
                        ),
                        VaultAddEditState.Custom.BooleanField(
                            "TestId 3",
                            "Boolean Field",
                            true,
                        ),
                    ),
                ),
            )

            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = initState,
                    vaultAddEditType = VaultAddEditType.AddItem,
                ),
            )
            val currentContentState =
                (viewModel.stateFlow.value.viewState as VaultAddEditState.ViewState.Content)
            val expectedState = currentContentState
                .copy(
                    common = currentContentState.common.copy(
                        customFieldData = listOf(
                            VaultAddEditState.Custom.BooleanField(
                                "TestId 3",
                                "Boolean Field",
                                true,
                            ),
                            VaultAddEditState.Custom.BooleanField(
                                "TestId 1",
                                "Boolean Field",
                                true,
                            ),
                        ),
                    ),
                )

            viewModel.actionChannel.trySend(
                VaultAddEditAction.Common.CustomFieldActionSelect(
                    CustomFieldAction.MOVE_UP,
                    customFieldData,
                ),
            )

            assertEquals(
                expectedState,
                viewModel.stateFlow.value.viewState,
            )
        }

        @Test
        fun `CustomFieldActionSelect with move down action should move the item down`() = runTest {
            val customFieldData =
                VaultAddEditState.Custom.BooleanField(
                    "TestId 1",
                    "Boolean Field",
                    true,
                )

            val initState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                commonContentViewState = VaultAddEditState.ViewState.Content.Common(
                    customFieldData = listOf(
                        VaultAddEditState.Custom.BooleanField(
                            "TestId 1",
                            "Boolean Field",
                            true,
                        ),
                        VaultAddEditState.Custom.BooleanField(
                            "TestId 3",
                            "Boolean Field",
                            true,
                        ),
                    ),
                ),
            )

            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = initState,
                    vaultAddEditType = VaultAddEditType.AddItem,
                ),
            )
            val currentContentState =
                (viewModel.stateFlow.value.viewState as VaultAddEditState.ViewState.Content)
            val expectedState = currentContentState
                .copy(
                    common = currentContentState.common.copy(
                        customFieldData = listOf(
                            VaultAddEditState.Custom.BooleanField(
                                "TestId 3",
                                "Boolean Field",
                                true,
                            ),
                            VaultAddEditState.Custom.BooleanField(
                                "TestId 1",
                                "Boolean Field",
                                true,
                            ),
                        ),
                    ),
                )

            viewModel.actionChannel.trySend(
                VaultAddEditAction.Common.CustomFieldActionSelect(
                    CustomFieldAction.MOVE_DOWN,
                    customFieldData,
                ),
            )

            assertEquals(
                expectedState,
                viewModel.stateFlow.value.viewState,
            )
        }

        @Test
        fun `TooltipClick should emit ShowToast with 'Tooltip' message`() = runTest {
            viewModel.eventFlow.test {
                viewModel
                    .actionChannel
                    .trySend(
                        VaultAddEditAction.Common.TooltipClick,
                    )
                assertEquals(
                    VaultAddEditEvent.ShowToast(
                        "Not yet implemented".asText(),
                    ),
                    awaitItem(),
                )
            }
        }
    }

    //region Helper functions

    @Suppress("MaxLineLength")
    private fun createVaultAddItemState(
        vaultAddEditType: VaultAddEditType = VaultAddEditType.AddItem,
        commonContentViewState: VaultAddEditState.ViewState.Content.Common = createCommonContentViewState(),
        typeContentViewState: VaultAddEditState.ViewState.Content.ItemType = createLoginTypeContentViewState(),
        dialogState: VaultAddEditState.DialogState? = null,
    ): VaultAddEditState =
        VaultAddEditState(
            vaultAddEditType = vaultAddEditType,
            viewState = VaultAddEditState.ViewState.Content(
                common = commonContentViewState,
                type = typeContentViewState,
            ),
            dialog = dialogState,
        )

    @Suppress("LongParameterList")
    private fun createCommonContentViewState(
        name: String = "",
        folder: Text = R.string.folder_none.asText(),
        favorite: Boolean = false,
        masterPasswordReprompt: Boolean = false,
        notes: String = "",
        customFieldData: List<VaultAddEditState.Custom> = listOf(),
        ownership: String = "placeholder@email.com",
        originalCipher: CipherView? = null,
        availableFolders: List<Text> = listOf(
            "Folder 1".asText(),
            "Folder 2".asText(),
            "Folder 3".asText(),
        ),
        availableOwners: List<String> = listOf("a@b.com", "c@d.com"),
    ): VaultAddEditState.ViewState.Content.Common =
        VaultAddEditState.ViewState.Content.Common(
            name = name,
            folderName = folder,
            favorite = favorite,
            customFieldData = customFieldData,
            masterPasswordReprompt = masterPasswordReprompt,
            notes = notes,
            ownership = ownership,
            originalCipher = originalCipher,
            availableFolders = availableFolders,
            availableOwners = availableOwners,
        )

    private fun createLoginTypeContentViewState(
        username: String = "",
        password: String = "",
        uri: List<UriItem> = listOf(UriItem("testId", "", null)),
        totpCode: String? = null,
        canViewPassword: Boolean = true,
    ): VaultAddEditState.ViewState.Content.ItemType.Login =
        VaultAddEditState.ViewState.Content.ItemType.Login(
            username = username,
            password = password,
            uriList = uri,
            totp = totpCode,
            canViewPassword = canViewPassword,
        )

    private fun createSavedStateHandleWithState(
        state: VaultAddEditState?,
        vaultAddEditType: VaultAddEditType,
    ) = SavedStateHandle().apply {
        set("state", state)
        set(
            "vault_add_edit_type",
            when (vaultAddEditType) {
                VaultAddEditType.AddItem -> "add"
                is VaultAddEditType.EditItem -> "edit"
                is VaultAddEditType.CloneItem -> "clone"
            },
        )
        set("vault_edit_id", (vaultAddEditType as? VaultAddEditType.EditItem)?.vaultItemId)
    }

    private fun createAddVaultItemViewModel(
        savedStateHandle: SavedStateHandle = loginInitialSavedStateHandle,
        bitwardenClipboardManager: BitwardenClipboardManager = clipboardManager,
        vaultRepo: VaultRepository = vaultRepository,
        generatorRepo: GeneratorRepository = generatorRepository,
        bitwardenResourceManager: ResourceManager = resourceManager,
    ): VaultAddEditViewModel =
        VaultAddEditViewModel(
            savedStateHandle = savedStateHandle,
            clipboardManager = bitwardenClipboardManager,
            vaultRepository = vaultRepo,
            generatorRepository = generatorRepo,
            resourceManager = bitwardenResourceManager,
            authRepository = authRepository,
        )

    /**
     * A function to test the changes in custom fields for each type.
     */
    private fun assertCustomFieldValueChange(
        initialState: VaultAddEditState,
        type: CustomFieldType,
    ) {
        lateinit var expectedCustomField: VaultAddEditState.Custom
        lateinit var action: VaultAddEditAction.Common
        lateinit var expectedState: VaultAddEditState.ViewState.Content

        when (type) {
            CustomFieldType.LINKED -> {
                expectedCustomField = VaultAddEditState.Custom.LinkedField(
                    "TestId 4",
                    "Linked Field",
                    VaultLinkedFieldType.PASSWORD,
                )
            }

            CustomFieldType.HIDDEN -> {
                expectedCustomField = VaultAddEditState.Custom.HiddenField(
                    "TestId 2",
                    "Test Hidden",
                    "Updated Test Text",
                )
            }

            CustomFieldType.BOOLEAN -> {
                expectedCustomField = VaultAddEditState.Custom.BooleanField(
                    "TestId 3",
                    "Boolean Field",
                    false,
                )
            }

            CustomFieldType.TEXT -> {
                expectedCustomField = VaultAddEditState.Custom.TextField(
                    "TestId 1",
                    "Test Text",
                    "Updated Test Text",
                )
            }
        }

        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initialState,
                vaultAddEditType = VaultAddEditType.AddItem,
            ),
        )

        val currentContentState =
            (viewModel.stateFlow.value.viewState as VaultAddEditState.ViewState.Content)
        action = VaultAddEditAction.Common.CustomFieldValueChange(expectedCustomField)
        expectedState = currentContentState
            .copy(
                common = currentContentState.common.copy(
                    customFieldData = listOf(expectedCustomField),
                ),
            )

        viewModel.actionChannel.trySend(action)

        assertEquals(expectedState, viewModel.stateFlow.value.viewState)
    }

    /**
     * A function to test the addition of new custom fields for each type.
     */
    private fun assertAddNewCustomFieldClick(
        initialState: VaultAddEditState,
        type: CustomFieldType,
    ) {
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initialState,
                vaultAddEditType = VaultAddEditType.AddItem,
            ),
        )

        var name = ""
        lateinit var expectedCustomField: VaultAddEditState.Custom
        lateinit var action: VaultAddEditAction.Common
        lateinit var expectedState: VaultAddEditState.ViewState.Content

        when (type) {
            CustomFieldType.LINKED -> {
                name = "Linked"
                expectedCustomField = VaultAddEditState.Custom.LinkedField(
                    itemId = TEST_ID,
                    name = name,
                    vaultLinkedFieldType = VaultLinkedFieldType.USERNAME,
                )
            }

            CustomFieldType.HIDDEN -> {
                name = "Hidden"
                expectedCustomField = VaultAddEditState.Custom.HiddenField(
                    itemId = TEST_ID,
                    name = name,
                    value = "",
                )
            }

            CustomFieldType.BOOLEAN -> {
                name = "Boolean"
                expectedCustomField = VaultAddEditState.Custom.BooleanField(
                    itemId = TEST_ID,
                    name = name,
                    value = false,
                )
            }

            CustomFieldType.TEXT -> {
                name = "Text"
                expectedCustomField = VaultAddEditState.Custom.TextField(
                    itemId = TEST_ID,
                    name = name,
                    value = "",
                )
            }
        }

        val currentContentState =
            (viewModel.stateFlow.value.viewState as VaultAddEditState.ViewState.Content)
        action = VaultAddEditAction.Common.AddNewCustomFieldClick(type, name)
        expectedState = currentContentState
            .copy(
                common = currentContentState.common.copy(
                    customFieldData = listOf(expectedCustomField),
                ),
            )

        viewModel.actionChannel.trySend(action)
        assertEquals(expectedState, viewModel.stateFlow.value.viewState)
    }

    //endregion Helper functions
}

private const val TEST_ID = "testId"

private const val DEFAULT_EDIT_ITEM_ID: String = "edit_item_id"
