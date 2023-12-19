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

    private val loginInitialState = createVaultAddItemState(
        typeContentViewState = createLoginTypeContentViewState(),
    )
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
            viewModel.actionChannel.trySend(VaultAddItemAction.Common.CloseClick)
            assertEquals(VaultAddItemEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `in add mode, SaveClick should show dialog, and remove it once an item is saved`() =
        runTest {
            val stateWithDialog = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem,
                dialogState = VaultAddItemState.DialogState.Loading(
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
                viewModel.actionChannel.trySend(VaultAddItemAction.Common.SaveClick)
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
            viewModel.actionChannel.trySend(VaultAddItemAction.Common.SaveClick)
            assertEquals(VaultAddItemEvent.NavigateBack, awaitItem())
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
            viewModel.actionChannel.trySend(VaultAddItemAction.Common.SaveClick)
            assertEquals(VaultAddItemEvent.ShowToast("Save Item Failure"), awaitItem())
        }
    }

    @Test
    fun `in edit mode, SaveClick should show dialog, and remove it once an item is saved`() =
        runTest {
            val cipherView = mockk<CipherView>()
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val stateWithDialog = createVaultAddItemState(
                vaultAddEditType = vaultAddEditType,
                dialogState = VaultAddItemState.DialogState.Loading(
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
                viewModel.actionChannel.trySend(VaultAddItemAction.Common.SaveClick)
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
        val stateWithName = createVaultAddItemState(
            vaultAddEditType = vaultAddEditType,
            commonContentViewState = createCommonContentViewState(
                name = "mockName",
            ),
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
            viewModel.actionChannel.trySend(VaultAddItemAction.Common.SaveClick)
            assertEquals(VaultAddItemEvent.ShowToast("Save Item Failure"), awaitItem())
        }

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
            dialogState = VaultAddItemState.DialogState.Error(
                R.string.validation_field_required
                    .asText(R.string.name.asText()),
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
            viewModel.actionChannel.trySend(VaultAddItemAction.Common.SaveClick)
            assertEquals(stateWithNoName, awaitItem())
            assertEquals(stateWithNoNameAndDialog, awaitItem())
        }
    }

    @Test
    fun `HandleDialogDismiss will remove the current dialog`() = runTest {
        val errorState = createVaultAddItemState(
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
            viewModel.actionChannel.trySend(VaultAddItemAction.Common.DismissDialog)
            assertEquals(errorState, awaitItem())
            assertEquals(null, awaitItem().dialog)
        }
    }

    @Test
    fun `TypeOptionSelect LOGIN should switch to LoginItem`() = runTest {
        val viewModel = createAddVaultItemViewModel()
        val action = VaultAddItemAction.Common.TypeOptionSelect(
            VaultAddItemState.ItemTypeOption.LOGIN,
        )

        viewModel.actionChannel.trySend(action)

        val expectedState = loginInitialState.copy(
            viewState = VaultAddItemState.ViewState.Content(
                common = createCommonContentViewState(),
                type = createLoginTypeContentViewState(),
            ),
        )

        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Nested
    inner class VaultAddLoginTypeItemActions {
        private lateinit var viewModel: VaultAddItemViewModel

        @BeforeEach
        fun setup() {
            viewModel = createAddVaultItemViewModel()
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UsernameTextChange should update username in LoginItem`() = runTest {
            val action = VaultAddItemAction.ItemType.LoginType.UsernameTextChange("newUsername")
            val expectedState = createVaultAddItemState(
                typeContentViewState = createLoginTypeContentViewState(
                    username = "newUsername",
                ),
            )
            viewModel.actionChannel.trySend(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `PasswordTextChange should update password in LoginItem`() = runTest {
            val action = VaultAddItemAction.ItemType.LoginType.PasswordTextChange("newPassword")

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
            val action = VaultAddItemAction.ItemType.LoginType.UriTextChange("newUri")

            viewModel.actionChannel.trySend(action)

            val expectedState = createVaultAddItemState(
                typeContentViewState = createLoginTypeContentViewState(
                    uri = "newUri",
                ),
            )

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
        fun `SetupTotpClick should emit ShowToast with permission granted when isGranted is true`() = runTest {
            val viewModel = createAddVaultItemViewModel()

            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(
                    VaultAddItemAction.ItemType.LoginType.SetupTotpClick(
                        true,
                    ),
                )
                assertEquals(
                    VaultAddItemEvent.ShowToast("Permission Granted, QR Code Scanner Not Implemented"),
                    awaitItem(),
                )
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `SetupTotpClick should emit ShowToast with permission not granted when isGranted is false`() = runTest {
            val viewModel = createAddVaultItemViewModel()

            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(
                    VaultAddItemAction.ItemType.LoginType.SetupTotpClick(
                        false,
                    ),
                )
                assertEquals(
                    VaultAddItemEvent.ShowToast("Permission Not Granted, Manual QR Code Entry Not Implemented"),
                    awaitItem(),
                )
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
    }

    @Nested
    inner class VaultAddItemCommonActions {
        private lateinit var viewModel: VaultAddItemViewModel
        private lateinit var vaultAddItemInitialState: VaultAddItemState
        private lateinit var secureNotesInitialSavedStateHandle: SavedStateHandle

        @BeforeEach
        fun setup() {
            vaultAddItemInitialState = createVaultAddItemState()
            secureNotesInitialSavedStateHandle = createSavedStateHandleWithState(
                state = vaultAddItemInitialState,
                vaultAddEditType = VaultAddEditType.AddItem,
            )
            viewModel = VaultAddItemViewModel(
                savedStateHandle = secureNotesInitialSavedStateHandle,
                vaultRepository = vaultRepository,
            )
        }

        @Test
        fun `NameTextChange should update name`() = runTest {
            val action = VaultAddItemAction.Common.NameTextChange("newName")

            viewModel.actionChannel.trySend(action)

            val expectedState = vaultAddItemInitialState.copy(
                viewState = VaultAddItemState.ViewState.Content(
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
            val action = VaultAddItemAction.Common.FolderChange(
                "newFolder".asText(),
            )

            viewModel.actionChannel.trySend(action)

            val expectedState = vaultAddItemInitialState.copy(
                viewState = VaultAddItemState.ViewState.Content(
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
            val action = VaultAddItemAction.Common.ToggleFavorite(true)

            viewModel.actionChannel.trySend(action)

            val expectedState = vaultAddItemInitialState.copy(
                viewState = VaultAddItemState.ViewState.Content(
                    common = createCommonContentViewState(
                        favorite = true,
                    ),
                    type = createLoginTypeContentViewState(),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `ToggleMasterPasswordReprompt should update masterPasswordReprompt`() =
            runTest {
                val action =
                    VaultAddItemAction.Common.ToggleMasterPasswordReprompt(
                        isMasterPasswordReprompt = true,
                    )

                viewModel.actionChannel.trySend(action)

                val expectedState = vaultAddItemInitialState.copy(
                    viewState = VaultAddItemState.ViewState.Content(
                        common = createCommonContentViewState(
                            masterPasswordReprompt = true,
                        ),
                        type = createLoginTypeContentViewState(),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Suppress("MaxLineLength")
        @Test
        fun `NotesTextChange should update notes`() = runTest {
            val action =
                VaultAddItemAction.Common.NotesTextChange(notes = "newNotes")

            viewModel.actionChannel.trySend(action)

            val expectedState = vaultAddItemInitialState.copy(
                viewState = VaultAddItemState.ViewState.Content(
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
            val action = VaultAddItemAction.Common.OwnershipChange(ownership = "newOwner")

            viewModel.actionChannel.trySend(action)

            val expectedState = vaultAddItemInitialState.copy(
                viewState = VaultAddItemState.ViewState.Content(
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

        @Suppress("MaxLineLength")
        @Test
        fun `CustomFieldValueChange should allow a user to update a text custom field`() =
            runTest {

                val initState =
                    createVaultAddItemState(
                        vaultAddEditType = VaultAddEditType.AddItem,
                        typeContentViewState = VaultAddItemState.ViewState.Content.ItemType.SecureNotes,
                        commonContentViewState = VaultAddItemState.ViewState.Content.Common(
                            customFieldData = listOf(
                                VaultAddItemState.Custom.TextField(
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
        @Suppress("MaxLineLength")
        fun `CustomFieldValueChange should update hidden custom fields`() =
            runTest {
                val initState =
                    createVaultAddItemState(
                        typeContentViewState = VaultAddItemState.ViewState.Content.ItemType.SecureNotes,
                        commonContentViewState = VaultAddItemState.ViewState.Content.Common(
                            customFieldData = listOf(
                                VaultAddItemState.Custom.HiddenField(
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

        @Suppress("MaxLineLength")
        @Test
        fun `CustomFieldValueChange should update boolean custom fields`() =
            runTest {
                val initState =
                    createVaultAddItemState(
                        typeContentViewState = VaultAddItemState.ViewState.Content.ItemType.SecureNotes,
                        commonContentViewState = VaultAddItemState.ViewState.Content.Common(
                            customFieldData = listOf(
                                VaultAddItemState.Custom.BooleanField(
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
        fun `TooltipClick should emit ShowToast with 'Tooltip' message`() = runTest {
            viewModel.eventFlow.test {
                viewModel
                    .actionChannel
                    .trySend(
                        VaultAddItemAction.Common.TooltipClick,
                    )
                assertEquals(VaultAddItemEvent.ShowToast("Not yet implemented"), awaitItem())
            }
        }
    }

    //region Helper functions

    @Suppress("MaxLineLength")
    private fun createVaultAddItemState(
        vaultAddEditType: VaultAddEditType = VaultAddEditType.AddItem,
        commonContentViewState: VaultAddItemState.ViewState.Content.Common = createCommonContentViewState(),
        typeContentViewState: VaultAddItemState.ViewState.Content.ItemType = createLoginTypeContentViewState(),
        dialogState: VaultAddItemState.DialogState? = null,
    ): VaultAddItemState =
        VaultAddItemState(
            vaultAddEditType = vaultAddEditType,
            viewState = VaultAddItemState.ViewState.Content(
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
        customFieldData: List<VaultAddItemState.Custom> = listOf(),
        ownership: String = "placeholder@email.com",
    ): VaultAddItemState.ViewState.Content.Common =
        VaultAddItemState.ViewState.Content.Common(
            name = name,
            folderName = folder,
            favorite = favorite,
            customFieldData = customFieldData,
            masterPasswordReprompt = masterPasswordReprompt,
            notes = notes,
            ownership = ownership,
        )

    private fun createLoginTypeContentViewState(
        username: String = "",
        password: String = "",
        uri: String = "",
    ): VaultAddItemState.ViewState.Content.ItemType.Login =
        VaultAddItemState.ViewState.Content.ItemType.Login(
            username = username,
            password = password,
            uri = uri,
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
        lateinit var action: VaultAddItemAction.Common
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

        val currentContentState =
            (viewModel.stateFlow.value.viewState as VaultAddItemState.ViewState.Content)
        action = VaultAddItemAction.Common.CustomFieldValueChange(expectedCustomField)
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
        lateinit var action: VaultAddItemAction.Common
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

        val currentContentState =
            (viewModel.stateFlow.value.viewState as VaultAddItemState.ViewState.Content)
        action = VaultAddItemAction.Common.AddNewCustomFieldClick(type, name)
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

private const val CIPHER_VIEW_EXTENSIONS_PATH: String =
    "com.x8bit.bitwarden.ui.vault.feature.additem.util.CipherViewExtensionsKt"

private const val DEFAULT_EDIT_ITEM_ID: String = "edit_item_id"
