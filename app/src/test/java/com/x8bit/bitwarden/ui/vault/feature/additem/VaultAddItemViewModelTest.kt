package com.x8bit.bitwarden.ui.vault.feature.additem

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.additem.model.CustomFieldType
import com.x8bit.bitwarden.ui.vault.feature.additem.model.toCustomField
import com.x8bit.bitwarden.ui.vault.feature.additem.util.toViewState
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class VaultAddItemViewModelTest : BaseViewModelTest() {

    private val loginInitialState = createVaultAddLoginItemState()
    private val loginInitialSavedStateHandle = createSavedStateHandleWithState(
        state = loginInitialState,
        vaultAddEditType = VaultAddEditType.AddItem,
    )
    private val mutableVaultItemFlow = MutableStateFlow<DataState<CipherView?>>(DataState.Loading)
    private val vaultRepository: VaultRepository = mockk {
        every { getVaultItemStateFlow(DEFAULT_EDIT_ITEM_ID) } returns mutableVaultItemFlow
    }

    @BeforeEach
    fun setup() {
        mockkStatic(CIPHER_VIEW_EXTENSIONS_PATH)
        mockkStatic(UUID::randomUUID)
        every { UUID.randomUUID().toString() } returns TEST_ID
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(CIPHER_VIEW_EXTENSIONS_PATH)
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
            assertEquals(loginInitialState, awaitItem())
        }
    }

    @Test
    fun `initial add state should be correct`() = runTest {
        val vaultAddEditType = VaultAddEditType.AddItem
        val initState = createVaultAddLoginItemState(vaultAddEditType = vaultAddEditType)
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
        val initState = createVaultAddLoginItemState(vaultAddEditType = vaultAddEditType)
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = vaultAddEditType,
            ),
        )
        assertEquals(
            initState.copy(viewState = VaultAddItemState.ViewState.Loading),
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
            viewModel.actionChannel.trySend(VaultAddItemAction.CloseClick)
            assertEquals(VaultAddItemEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `in add mode, SaveClick should show dialog, and remove it once an item is saved`() =
        runTest {
            val stateWithDialog = createVaultAddLoginItemState(
                vaultAddEditType = VaultAddEditType.AddItem,
                name = "tester",
                dialogState = VaultAddItemState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
            )

            val stateWithName = createVaultAddLoginItemState(
                vaultAddEditType = VaultAddEditType.AddItem,
                name = "tester",
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
                viewModel.actionChannel.trySend(VaultAddItemAction.SaveClick)
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
        val stateWithName = createVaultAddLoginItemState(
            vaultAddEditType = VaultAddEditType.AddItem,
            name = "tester",
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
            viewModel.actionChannel.trySend(VaultAddItemAction.SaveClick)
            assertEquals(VaultAddItemEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `in add mode, SaveClick createCipher error should emit ShowToast`() = runTest {
        val stateWithName = createVaultAddLoginItemState(
            vaultAddEditType = VaultAddEditType.AddItem,
            name = "tester",
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
            viewModel.actionChannel.trySend(VaultAddItemAction.SaveClick)
            assertEquals(VaultAddItemEvent.ShowToast("Save Item Failure"), awaitItem())
        }
    }

    @Test
    fun `in edit mode, SaveClick should show dialog, and remove it once an item is saved`() =
        runTest {
            val cipherView = mockk<CipherView>()
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val stateWithDialog = createVaultAddLoginItemState(
                vaultAddEditType = vaultAddEditType,
                name = "tester",
                dialogState = VaultAddItemState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
            )

            val stateWithName = createVaultAddLoginItemState(
                vaultAddEditType = vaultAddEditType,
                name = "tester",
            )
            every { cipherView.toViewState() } returns stateWithName.viewState
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
                viewModel.actionChannel.trySend(VaultAddItemAction.SaveClick)
                assertEquals(stateWithDialog, awaitItem())
                assertEquals(stateWithName, awaitItem())
            }

            coVerify(exactly = 1) {
                cipherView.toViewState()
                vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
            }
        }

    @Test
    fun `in edit mode, SaveClick createCipher error should emit ShowToast`() = runTest {
        val cipherView = mockk<CipherView>()
        val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
        val stateWithName = createVaultAddLoginItemState(
            vaultAddEditType = vaultAddEditType,
            name = "tester",
        )

        every { cipherView.toViewState() } returns stateWithName.viewState
        coEvery {
            vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
        } returns UpdateCipherResult.Error
        mutableVaultItemFlow.value = DataState.Loaded(cipherView)

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = stateWithName,
                vaultAddEditType = vaultAddEditType,
            ),
        )

        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultAddItemAction.SaveClick)
            assertEquals(VaultAddItemEvent.ShowToast("Save Item Failure"), awaitItem())
        }

        coVerify(exactly = 1) {
            vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
        }
    }

    @Test
    fun `Saving item with an empty name field will cause a dialog to show up`() = runTest {
        val stateWithNoName = createVaultAddSecureNotesItemState(
            name = "",
            vaultAddEditType = VaultAddEditType.AddItem,
        )

        val stateWithNoNameAndDialog = createVaultAddSecureNotesItemState(
            name = "",
            dialogState = VaultAddItemState.DialogState.Error(
                R.string.validation_field_required
                    .asText(R.string.name.asText()),
            ),
            vaultAddEditType = VaultAddEditType.AddItem,
        )

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = stateWithNoName,
                vaultAddEditType = VaultAddEditType.AddItem,
            ),
        )
        coEvery { vaultRepository.createCipher(any()) } returns CreateCipherResult.Success
        viewModel.stateFlow.test {
            viewModel.actionChannel.trySend(VaultAddItemAction.SaveClick)
            assertEquals(stateWithNoName, awaitItem())
            assertEquals(stateWithNoNameAndDialog, awaitItem())
        }
    }

    @Test
    fun `HandleDialogDismiss will remove the current dialog`() = runTest {
        val errorState = createVaultAddLoginItemState(
            vaultAddEditType = VaultAddEditType.AddItem,
            dialogState = VaultAddItemState.DialogState.Error(
                R.string.validation_field_required
                    .asText(R.string.name.asText()),
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
            viewModel.actionChannel.trySend(VaultAddItemAction.DismissDialog)
            assertEquals(errorState, awaitItem())
            assertEquals(null, awaitItem().dialog)
        }
    }

    @Test
    fun `TypeOptionSelect LOGIN should switch to LoginItem`() = runTest {
        val viewModel = createAddVaultItemViewModel()
        val action = VaultAddItemAction.TypeOptionSelect(VaultAddItemState.ItemTypeOption.LOGIN)

        viewModel.actionChannel.trySend(action)

        val expectedState = loginInitialState.copy(
            viewState = VaultAddItemState.ViewState.Content.Login(),
        )

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Nested
    inner class VaultAddLoginTypeItemActions {
        private lateinit var viewModel: VaultAddItemViewModel

        @BeforeEach
        fun setup() {
            viewModel = createAddVaultItemViewModel()
        }

        @Test
        fun `NameTextChange should update name in LoginItem`() = runTest {
            val viewModel = createAddVaultItemViewModel()
            val action = VaultAddItemAction.ItemType.LoginType.NameTextChange("newName")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (loginInitialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(name = "newName")

            val expectedState = loginInitialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UsernameTextChange should update username in LoginItem`() = runTest {
            val action = VaultAddItemAction.ItemType.LoginType.UsernameTextChange("newUsername")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (loginInitialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(username = "newUsername")

            val expectedState = loginInitialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `PasswordTextChange should update password in LoginItem`() = runTest {
            val action = VaultAddItemAction.ItemType.LoginType.PasswordTextChange("newPassword")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (loginInitialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(password = "newPassword")

            val expectedState = loginInitialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `UriTextChange should update uri in LoginItem`() = runTest {
            val action = VaultAddItemAction.ItemType.LoginType.UriTextChange("newUri")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (loginInitialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(uri = "newUri")

            val expectedState = loginInitialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `FolderChange should update folder in LoginItem`() = runTest {
            val action = VaultAddItemAction.ItemType.LoginType.FolderChange("newFolder".asText())

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (loginInitialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(folderName = "newFolder".asText())

            val expectedState = loginInitialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `ToggleFavorite should update favorite in LoginItem`() = runTest {
            val action = VaultAddItemAction.ItemType.LoginType.ToggleFavorite(true)

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (loginInitialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(favorite = true)

            val expectedState = loginInitialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `ToggleMasterPasswordReprompt should update masterPasswordReprompt in LoginItem`() =
            runTest {
                val action = VaultAddItemAction.ItemType.LoginType.ToggleMasterPasswordReprompt(
                    isMasterPasswordReprompt = true,
                )

                viewModel.actionChannel.trySend(action)

                val expectedLoginItem =
                    (loginInitialState.viewState as VaultAddItemState.ViewState.Content.Login)
                        .copy(masterPasswordReprompt = true)

                val expectedState = loginInitialState.copy(viewState = expectedLoginItem)

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Test
        fun `NotesTextChange should update notes in LoginItem`() = runTest {
            val action = VaultAddItemAction.ItemType.LoginType.NotesTextChange(notes = "newNotes")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (loginInitialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(notes = "newNotes")

            val expectedState = loginInitialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `AddNewCustomFieldClick should allow a user to add a custom text field in Login item`() =
            runTest {
                assertAddNewCustomFieldClick(
                    initialState = loginInitialState,
                    type = CustomFieldType.TEXT,
                )
            }

        @Suppress("MaxLineLength")
        @Test
        fun `AddNewCustomFieldClick should allow a user to add a custom boolean field in Login item`() =
            runTest {
                assertAddNewCustomFieldClick(
                    initialState = loginInitialState,
                    type = CustomFieldType.BOOLEAN,
                )
            }

        @Suppress("MaxLineLength")
        @Test
        fun `AddNewCustomFieldClick should allow a user to add a custom hidden field in Login item`() =
            runTest {
                assertAddNewCustomFieldClick(
                    initialState = loginInitialState,
                    type = CustomFieldType.HIDDEN,
                )
            }

        @Suppress("MaxLineLength")
        @Test
        fun `AddNewCustomFieldClick should allow a user to add a custom linked field in Login item`() =
            runTest {
                assertAddNewCustomFieldClick(
                    initialState = loginInitialState,
                    type = CustomFieldType.LINKED,
                )
            }

        @Suppress("MaxLineLength")
        @Test
        fun `CustomFieldValueChange should allow a user to update a text custom field in Login item`() =
            runTest {
                val initState = createVaultAddLoginItemState(
                    vaultAddEditType = VaultAddEditType.AddItem,
                    customFieldData = listOf(
                        VaultAddItemState.Custom.TextField(
                            "TestId 1",
                            "Test Text",
                            "Test Text",
                        ),
                    ),
                )

                assertCustomFieldValueChange(
                    initState,
                    CustomFieldType.TEXT,
                )
            }

        @Test
        fun `CustomFieldValueChange should update hidden custom fields in Login item`() =
            runTest {
                val initState = createVaultAddLoginItemState(
                    vaultAddEditType = VaultAddEditType.AddItem,
                    customFieldData = listOf(
                        VaultAddItemState.Custom.HiddenField(
                            "TestId 2",
                            "Test Text",
                            "Test Text",
                        ),
                    ),
                )

                assertCustomFieldValueChange(
                    initState,
                    CustomFieldType.HIDDEN,
                )
            }

        @Suppress("MaxLineLength")
        @Test
        fun `CustomFieldValueChange should update boolean custom fields in Login item`() =
            runTest {
                val initState = createVaultAddLoginItemState(
                    vaultAddEditType = VaultAddEditType.AddItem,
                    customFieldData = listOf(
                        VaultAddItemState.Custom.BooleanField(
                            "TestId 3",
                            "Boolean Field",
                            true,
                        ),
                    ),
                )

                assertCustomFieldValueChange(
                    initState,
                    CustomFieldType.BOOLEAN,
                )
            }

        @Test
        fun `CustomFieldValueChange should update linked custom fields in Login item`() =
            runTest {
                val initState = createVaultAddLoginItemState(
                    vaultAddEditType = VaultAddEditType.AddItem,
                    customFieldData = listOf(
                        VaultAddItemState.Custom.LinkedField(
                            "TestId 4",
                            "Linked Field",
                            VaultLinkedFieldType.USERNAME,
                        ),
                    ),
                )

                assertCustomFieldValueChange(
                    initState,
                    CustomFieldType.LINKED,
                )
            }

        @Test
        fun `OwnershipChange should update ownership in LoginItem`() = runTest {
            val viewModel = createAddVaultItemViewModel()
            val action =
                VaultAddItemAction.ItemType.LoginType.OwnershipChange(ownership = "newOwner")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (loginInitialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(ownership = "newOwner")

            val expectedState = loginInitialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `OpenUsernameGeneratorClick should emit ShowToast with 'Open Username Generator' message`() =
            runTest {
                val viewModel = createAddVaultItemViewModel()

                viewModel.eventFlow.test {
                    viewModel.actionChannel.trySend(
                        VaultAddItemAction.ItemType.LoginType.OpenUsernameGeneratorClick,
                    )
                    assertEquals(
                        VaultAddItemEvent.ShowToast("Open Username Generator"),
                        awaitItem(),
                    )
                }
            }

        @Test
        fun `PasswordCheckerClick should emit ShowToast with 'Password Checker' message`() =
            runTest {
                val viewModel = createAddVaultItemViewModel()

                viewModel.eventFlow.test {
                    viewModel
                        .actionChannel
                        .trySend(VaultAddItemAction.ItemType.LoginType.PasswordCheckerClick)

                    assertEquals(VaultAddItemEvent.ShowToast("Password Checker"), awaitItem())
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `OpenPasswordGeneratorClick should emit ShowToast with 'Open Password Generator' message`() =
            runTest {
                val viewModel = createAddVaultItemViewModel()

                viewModel.eventFlow.test {
                    viewModel
                        .actionChannel
                        .trySend(VaultAddItemAction.ItemType.LoginType.OpenPasswordGeneratorClick)

                    assertEquals(
                        VaultAddItemEvent.ShowToast("Open Password Generator"),
                        awaitItem(),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `SetupTotpClick should emit ShowToast with 'Setup TOTP' message`() = runTest {
            val viewModel = createAddVaultItemViewModel()

            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(VaultAddItemAction.ItemType.LoginType.SetupTotpClick)
                assertEquals(VaultAddItemEvent.ShowToast("Setup TOTP"), awaitItem())
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UriSettingsClick should emit ShowToast with 'URI Settings' message`() = runTest {
            val viewModel = createAddVaultItemViewModel()

            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(VaultAddItemAction.ItemType.LoginType.UriSettingsClick)
                assertEquals(VaultAddItemEvent.ShowToast("URI Settings"), awaitItem())
            }
        }

        @Test
        fun `AddNewUriClick should emit ShowToast with 'Add New URI' message`() = runTest {
            val viewModel = createAddVaultItemViewModel()

            viewModel.eventFlow.test {
                viewModel
                    .actionChannel
                    .trySend(
                        VaultAddItemAction.ItemType.LoginType.AddNewUriClick,
                    )

                assertEquals(VaultAddItemEvent.ShowToast("Add New URI"), awaitItem())
            }
        }

        @Test
        fun `TooltipClick should emit ShowToast with 'Tooltip' message`() = runTest {
            val viewModel = createAddVaultItemViewModel()

            viewModel.eventFlow.test {
                viewModel
                    .actionChannel
                    .trySend(
                        VaultAddItemAction.ItemType.LoginType.TooltipClick,
                    )
                assertEquals(VaultAddItemEvent.ShowToast("Tooltip"), awaitItem())
            }
        }
    }

    @Nested
    inner class VaultAddSecureNotesTypeItemActions {
        private lateinit var viewModel: VaultAddItemViewModel
        private lateinit var secureNotesInitialState: VaultAddItemState
        private lateinit var secureNotesInitialSavedStateHandle: SavedStateHandle

        @BeforeEach
        fun setup() {
            secureNotesInitialState =
                createVaultAddSecureNotesItemState(vaultAddEditType = VaultAddEditType.AddItem)
            secureNotesInitialSavedStateHandle = createSavedStateHandleWithState(
                state = secureNotesInitialState,
                vaultAddEditType = VaultAddEditType.AddItem,
            )
            viewModel = VaultAddItemViewModel(
                savedStateHandle = secureNotesInitialSavedStateHandle,
                vaultRepository = vaultRepository,
            )
        }

        @Test
        fun `NameTextChange should update name in SecureNotesItem`() = runTest {
            val action = VaultAddItemAction.ItemType.SecureNotesType.NameTextChange("newName")

            viewModel.actionChannel.trySend(action)

            val expectedSecureNotesItem =
                (secureNotesInitialState.viewState as
                    VaultAddItemState.ViewState.Content.SecureNotes)
                    .copy(name = "newName")

            val expectedState = secureNotesInitialState.copy(viewState = expectedSecureNotesItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `FolderChange should update folder in SecureNotesItem`() = runTest {
            val action = VaultAddItemAction.ItemType.SecureNotesType.FolderChange(
                "newFolder".asText(),
            )

            viewModel.actionChannel.trySend(action)

            val expectedSecureNotesItem =
                (secureNotesInitialState.viewState as
                    VaultAddItemState.ViewState.Content.SecureNotes)
                    .copy(folderName = "newFolder".asText())

            val expectedState = secureNotesInitialState.copy(viewState = expectedSecureNotesItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `ToggleFavorite should update favorite in SecureNotesItem`() = runTest {
            val action = VaultAddItemAction.ItemType.SecureNotesType.ToggleFavorite(true)

            viewModel.actionChannel.trySend(action)

            val expectedSecureNotesItem =
                (secureNotesInitialState.viewState as
                    VaultAddItemState.ViewState.Content.SecureNotes)
                    .copy(favorite = true)

            val expectedState = secureNotesInitialState.copy(viewState = expectedSecureNotesItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `ToggleMasterPasswordReprompt should update masterPasswordReprompt in SecureNotesItem`() =
            runTest {
                val action =
                    VaultAddItemAction.ItemType.SecureNotesType.ToggleMasterPasswordReprompt(
                        isMasterPasswordReprompt = true,
                    )

                viewModel.actionChannel.trySend(action)

                val expectedSecureNotesItem =
                    (secureNotesInitialState.viewState as VaultAddItemState.ViewState.Content.SecureNotes)
                        .copy(masterPasswordReprompt = true)

                val expectedState = secureNotesInitialState.copy(viewState = expectedSecureNotesItem)

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Suppress("MaxLineLength")
        @Test
        fun `NotesTextChange should update notes in SecureNotesItem`() = runTest {
            val action =
                VaultAddItemAction.ItemType.SecureNotesType.NotesTextChange(note = "newNotes")

            viewModel.actionChannel.trySend(action)

            val expectedSecureNotesItem =
                (secureNotesInitialState.viewState as VaultAddItemState.ViewState.Content.SecureNotes)
                    .copy(notes = "newNotes")

            val expectedState = secureNotesInitialState.copy(viewState = expectedSecureNotesItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `OwnershipChange should update ownership in SecureNotesItem`() = runTest {
            val action =
                VaultAddItemAction.ItemType.SecureNotesType.OwnershipChange(ownership = "newOwner")

            viewModel.actionChannel.trySend(action)

            val expectedSecureNotesItem =
                (secureNotesInitialState.viewState as
                    VaultAddItemState.ViewState.Content.SecureNotes)
                    .copy(ownership = "newOwner")

            val expectedState = secureNotesInitialState.copy(viewState = expectedSecureNotesItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `AddNewCustomFieldClick should allow a user to add a custom boolean field in Secure notes item`() =
            runTest {
                assertAddNewCustomFieldClick(
                   initialState = secureNotesInitialState,
                    type = CustomFieldType.BOOLEAN,
                )
            }

        @Suppress("MaxLineLength")
        @Test
        fun `AddNewCustomFieldClick should allow a user to add a custom hidden field in Secure notes item`() =
            runTest {
                assertAddNewCustomFieldClick(
                    initialState = secureNotesInitialState,
                    type = CustomFieldType.HIDDEN,
                )
            }

        @Suppress("MaxLineLength")
        @Test
        fun `AddNewCustomFieldClick should allow a user to add a custom text field in Secure notes item`() =
            runTest {

                assertAddNewCustomFieldClick(
                    initialState = secureNotesInitialState,
                    type = CustomFieldType.TEXT,
                )
            }

        @Suppress("MaxLineLength")
        @Test
        fun `CustomFieldValueChange should allow a user to update a text custom field in Secure notes item`() =
            runTest {
                val initState = createVaultAddSecureNotesItemState(
                    vaultAddEditType = VaultAddEditType.AddItem,
                    customFieldData = listOf(
                        VaultAddItemState.Custom.TextField(
                            "TestId 1",
                            "Test Text",
                            "Test Text",
                        ),
                    ),
                )

                assertCustomFieldValueChange(
                    initState,
                    CustomFieldType.TEXT,
                )
            }

        @Test
        fun `CustomFieldValueChange should update hidden custom fields in Secure notes item`() =
            runTest {
                val initState = createVaultAddSecureNotesItemState(
                    vaultAddEditType = VaultAddEditType.AddItem,
                    customFieldData = listOf(
                        VaultAddItemState.Custom.HiddenField(
                            "TestId 2",
                            "Test Text",
                            "Test Text",
                        ),
                    ),
                )

                assertCustomFieldValueChange(
                    initState,
                    CustomFieldType.HIDDEN,
                )
            }

        @Suppress("MaxLineLength")
        @Test
        fun `CustomFieldValueChange should update boolean custom fields in Secure notes  item`() =
            runTest {
                val initState = createVaultAddSecureNotesItemState(
                    vaultAddEditType = VaultAddEditType.AddItem,
                    customFieldData = listOf(
                        VaultAddItemState.Custom.BooleanField(
                            "TestId 3",
                            "Boolean Field",
                            true,
                        ),
                    ),
                )

                assertCustomFieldValueChange(
                    initState,
                    CustomFieldType.BOOLEAN,
                )
            }

        @Test
        fun `TooltipClick should emit ShowToast with 'Tooltip' message`() = runTest {
            viewModel.eventFlow.test {
                viewModel
                    .actionChannel
                    .trySend(
                        VaultAddItemAction.ItemType.SecureNotesType.TooltipClick,
                    )
                assertEquals(VaultAddItemEvent.ShowToast("Not yet implemented"), awaitItem())
            }
        }
    }

    //region Helper functions

    @Suppress("LongParameterList")
    private fun createVaultAddLoginItemState(
        vaultAddEditType: VaultAddEditType = VaultAddEditType.AddItem,
        name: String = "",
        username: String = "",
        password: String = "",
        uri: String = "",
        folder: Text = R.string.folder_none.asText(),
        favorite: Boolean = false,
        masterPasswordReprompt: Boolean = false,
        notes: String = "",
        customFieldData: List<VaultAddItemState.Custom> = listOf(),
        ownership: String = "placeholder@email.com",
        dialogState: VaultAddItemState.DialogState? = null,
    ): VaultAddItemState =
        VaultAddItemState(
            vaultAddEditType = vaultAddEditType,
            viewState = VaultAddItemState.ViewState.Content.Login(
                name = name,
                username = username,
                password = password,
                uri = uri,
                folderName = folder,
                favorite = favorite,
                customFieldData = customFieldData,
                masterPasswordReprompt = masterPasswordReprompt,
                notes = notes,
                ownership = ownership,
            ),
            dialog = dialogState,
        )

    @Suppress("LongParameterList")
    private fun createVaultAddSecureNotesItemState(
        vaultAddEditType: VaultAddEditType.AddItem,
        name: String = "",
        folder: Text = "No Folder".asText(),
        favorite: Boolean = false,
        masterPasswordReprompt: Boolean = false,
        notes: String = "",
        customFieldData: List<VaultAddItemState.Custom> = listOf(),
        ownership: String = "placeholder@email.com",
        dialogState: VaultAddItemState.DialogState? = null,
    ): VaultAddItemState =
        VaultAddItemState(
            vaultAddEditType = vaultAddEditType,
            viewState = VaultAddItemState.ViewState.Content.SecureNotes(
                name = name,
                folderName = folder,
                favorite = favorite,
                masterPasswordReprompt = masterPasswordReprompt,
                notes = notes,
                customFieldData = customFieldData,
                ownership = ownership,
            ),
            dialog = dialogState,
        )

    private fun createSavedStateHandleWithState(
        state: VaultAddItemState?,
        vaultAddEditType: VaultAddEditType,
    ) = SavedStateHandle().apply {
        set("state", state)
        set(
            "vault_add_edit_type",
            when (vaultAddEditType) {
                VaultAddEditType.AddItem -> "add"
                is VaultAddEditType.EditItem -> "edit"
            },
        )
        set("vault_edit_id", (vaultAddEditType as? VaultAddEditType.EditItem)?.vaultItemId)
    }

    private fun createAddVaultItemViewModel(
        savedStateHandle: SavedStateHandle = loginInitialSavedStateHandle,
        vaultRepo: VaultRepository = vaultRepository,
    ): VaultAddItemViewModel =
        VaultAddItemViewModel(
            savedStateHandle = savedStateHandle,
            vaultRepository = vaultRepo,
        )

    /**
     * A function to test the changes in custom fields for each type.
     */
    private fun assertCustomFieldValueChange(
        initialState: VaultAddItemState,
        type: CustomFieldType,
    ) {
        lateinit var expectedCustomField: VaultAddItemState.Custom
        lateinit var action: VaultAddItemAction.ItemType
        lateinit var expectedState: VaultAddItemState.ViewState.Content

        when (type) {
            CustomFieldType.LINKED -> {
                expectedCustomField = VaultAddItemState.Custom.LinkedField(
                    "TestId 4",
                    "Linked Field",
                    VaultLinkedFieldType.PASSWORD,
                )
            }

            CustomFieldType.HIDDEN -> {
                expectedCustomField = VaultAddItemState.Custom.HiddenField(
                    "TestId 2",
                    "Test Hidden",
                    "Updated Test Text",
                )
            }

            CustomFieldType.BOOLEAN -> {
                expectedCustomField = VaultAddItemState.Custom.BooleanField(
                    "TestId 3",
                    "Boolean Field",
                    false,
                )
            }

            CustomFieldType.TEXT -> {
                expectedCustomField = VaultAddItemState.Custom.TextField(
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

        when (val state =
            viewModel.stateFlow.value.viewState as VaultAddItemState.ViewState.Content) {
            is VaultAddItemState.ViewState.Content.Login -> {
                action = VaultAddItemAction.ItemType.LoginType.CustomFieldValueChange(
                    expectedCustomField,
                )
                expectedState = state.copy(customFieldData = listOf(expectedCustomField))
            }

            is VaultAddItemState.ViewState.Content.SecureNotes -> {
                action =
                    VaultAddItemAction.ItemType.SecureNotesType.CustomFieldValueChange(
                        expectedCustomField,
                    )
                expectedState = state.copy(customFieldData = listOf(expectedCustomField))
            }
            // TODO: Create UI for card-type item creation (BIT-507)
            is VaultAddItemState.ViewState.Content.Card -> Unit
            // TODO: Create UI for identity-type item creation (BIT-667)
            is VaultAddItemState.ViewState.Content.Identity -> Unit
        }

        viewModel.actionChannel.trySend(action)

        assertEquals(expectedState, viewModel.stateFlow.value.viewState)
    }

    /**
     * A function to test the addition of new custom fields for each type.
     */
    private fun assertAddNewCustomFieldClick(
        initialState: VaultAddItemState,
        type: CustomFieldType,
    ) {
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initialState,
                vaultAddEditType = VaultAddEditType.AddItem,
            ),
        )

        var name = ""
        lateinit var expectedCustomField: VaultAddItemState.Custom
        lateinit var action: VaultAddItemAction.ItemType
        lateinit var expectedState: VaultAddItemState.ViewState.Content

        when (type) {
            CustomFieldType.LINKED -> {
                name = "Linked"
                expectedCustomField = VaultAddItemState.Custom.LinkedField(
                    itemId = TEST_ID,
                    name = name,
                    vaultLinkedFieldType = VaultLinkedFieldType.USERNAME,
                )
            }

            CustomFieldType.HIDDEN -> {
                name = "Hidden"
                expectedCustomField = VaultAddItemState.Custom.HiddenField(
                    itemId = TEST_ID,
                    name = name,
                    value = "",
                )
            }

            CustomFieldType.BOOLEAN -> {
                name = "Boolean"
                expectedCustomField = VaultAddItemState.Custom.BooleanField(
                    itemId = TEST_ID,
                    name = name,
                    value = false,
                )
            }

            CustomFieldType.TEXT -> {
                name = "Text"
                expectedCustomField = VaultAddItemState.Custom.TextField(
                    itemId = TEST_ID,
                    name = name,
                    value = "",
                )
            }
        }

        when (
            val state =
            viewModel.stateFlow.value.viewState as VaultAddItemState.ViewState.Content) {
            is VaultAddItemState.ViewState.Content.Login -> {
                action = VaultAddItemAction.ItemType.LoginType.AddNewCustomFieldClick(type, name)
                expectedState = state.copy(customFieldData = listOf(expectedCustomField))
            }

            is VaultAddItemState.ViewState.Content.SecureNotes -> {
                action =
                    VaultAddItemAction.ItemType.SecureNotesType.AddNewCustomFieldClick(
                        customFieldType = type,
                        name = name,
                    )
                expectedState = state.copy(customFieldData = listOf(expectedCustomField))
            }
            // TODO: Create UI for card-type item creation (BIT-507)
            is VaultAddItemState.ViewState.Content.Card -> Unit
            // TODO: Create UI for identity-type item creation (BIT-667)
            is VaultAddItemState.ViewState.Content.Identity -> Unit
        }

        viewModel.actionChannel.trySend(action)
        assertEquals(expectedState, viewModel.stateFlow.value.viewState)
    }

    //endregion Helper functions
}

private const val TEST_ID = "testId"

private const val CIPHER_VIEW_EXTENSIONS_PATH: String =
    "com.x8bit.bitwarden.ui.vault.feature.additem.util.CipherViewExtensionsKt"

private const val DEFAULT_EDIT_ITEM_ID: String = "edit_item_id"
