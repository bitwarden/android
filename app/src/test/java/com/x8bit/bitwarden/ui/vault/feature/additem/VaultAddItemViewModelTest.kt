package com.x8bit.bitwarden.ui.vault.feature.additem

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VaultAddItemViewModelTest : BaseViewModelTest() {

    private val initialState = createVaultAddLoginItemState()
    private val initialSavedStateHandle = createSavedStateHandleWithState(initialState)

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = VaultAddItemViewModel(initialSavedStateHandle)
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
        }
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = VaultAddItemViewModel(initialSavedStateHandle)
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultAddItemAction.CloseClick)
            assertEquals(VaultAddItemEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `SaveClick should emit ShowToast`() = runTest {
        val viewModel = VaultAddItemViewModel(initialSavedStateHandle)
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultAddItemAction.SaveClick)
            assertEquals(VaultAddItemEvent.ShowToast("Save Item"), awaitItem())
        }
    }

    @Test
    fun `TypeOptionSelect LOGIN should switch to LoginItem`() = runTest {
        val viewModel = VaultAddItemViewModel(initialSavedStateHandle)
        val action = VaultAddItemAction.TypeOptionSelect(VaultAddItemState.ItemTypeOption.LOGIN)

        viewModel.actionChannel.trySend(action)

        val expectedState = initialState.copy(selectedType = VaultAddItemState.ItemType.Login())

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Nested
    inner class VaultAddLoginTypeItemActions {
        private lateinit var viewModel: VaultAddItemViewModel

        @BeforeEach
        fun setup() {
            viewModel = VaultAddItemViewModel(initialSavedStateHandle)
        }

        @Test
        fun `NameTextChange should update name in LoginItem`() = runTest {
            val viewModel = VaultAddItemViewModel(initialSavedStateHandle)
            val action = VaultAddItemAction.ItemType.LoginType.NameTextChange("newName")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.selectedType as VaultAddItemState.ItemType.Login)
                    .copy(name = "newName")

            val expectedState = initialState.copy(selectedType = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UsernameTextChange should update username in LoginItem`() = runTest {
            val viewModel = VaultAddItemViewModel(initialSavedStateHandle)
            val action = VaultAddItemAction.ItemType.LoginType.UsernameTextChange("newUsername")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.selectedType as VaultAddItemState.ItemType.Login)
                    .copy(username = "newUsername")

            val expectedState = initialState.copy(selectedType = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `PasswordTextChange should update password in LoginItem`() = runTest {
            val viewModel = VaultAddItemViewModel(initialSavedStateHandle)
            val action = VaultAddItemAction.ItemType.LoginType.PasswordTextChange("newPassword")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.selectedType as VaultAddItemState.ItemType.Login)
                    .copy(password = "newPassword")

            val expectedState = initialState.copy(selectedType = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `UriTextChange should update uri in LoginItem`() = runTest {
            val viewModel = VaultAddItemViewModel(initialSavedStateHandle)
            val action = VaultAddItemAction.ItemType.LoginType.UriTextChange("newUri")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.selectedType as VaultAddItemState.ItemType.Login)
                    .copy(uri = "newUri")

            val expectedState = initialState.copy(selectedType = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `FolderChange should update folder in LoginItem`() = runTest {
            val viewModel = VaultAddItemViewModel(initialSavedStateHandle)
            val action = VaultAddItemAction.ItemType.LoginType.FolderChange("newFolder")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.selectedType as VaultAddItemState.ItemType.Login)
                    .copy(folder = "newFolder")

            val expectedState = initialState.copy(selectedType = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `ToggleFavorite should update favorite in LoginItem`() = runTest {
            val viewModel = VaultAddItemViewModel(initialSavedStateHandle)
            val action = VaultAddItemAction.ItemType.LoginType.ToggleFavorite(true)

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.selectedType as VaultAddItemState.ItemType.Login)
                    .copy(favorite = true)

            val expectedState = initialState.copy(selectedType = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `ToggleMasterPasswordReprompt should update masterPasswordReprompt in LoginItem`() =
            runTest {
                val viewModel = VaultAddItemViewModel(initialSavedStateHandle)
                val action = VaultAddItemAction.ItemType.LoginType.ToggleMasterPasswordReprompt(
                    isMasterPasswordReprompt = true,
                )

                viewModel.actionChannel.trySend(action)

                val expectedLoginItem =
                    (initialState.selectedType as VaultAddItemState.ItemType.Login)
                        .copy(masterPasswordReprompt = true)

                val expectedState = initialState.copy(selectedType = expectedLoginItem)

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Test
        fun `NotesTextChange should update notes in LoginItem`() = runTest {
            val viewModel = VaultAddItemViewModel(initialSavedStateHandle)
            val action = VaultAddItemAction.ItemType.LoginType.NotesTextChange(notes = "newNotes")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.selectedType as VaultAddItemState.ItemType.Login)
                    .copy(notes = "newNotes")

            val expectedState = initialState.copy(selectedType = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `OwnershipChange should update ownership in LoginItem`() = runTest {
            val viewModel = VaultAddItemViewModel(initialSavedStateHandle)
            val action =
                VaultAddItemAction.ItemType.LoginType.OwnershipChange(ownership = "newOwner")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.selectedType as VaultAddItemState.ItemType.Login)
                    .copy(ownership = "newOwner")

            val expectedState = initialState.copy(selectedType = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `OpenUsernameGeneratorClick should emit ShowToast with 'Open Username Generator' message`() =
            runTest {
                val viewModel = VaultAddItemViewModel(initialSavedStateHandle)

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
                val viewModel = VaultAddItemViewModel(initialSavedStateHandle)

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
                val viewModel = VaultAddItemViewModel(initialSavedStateHandle)

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
            val viewModel = VaultAddItemViewModel(initialSavedStateHandle)

            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(VaultAddItemAction.ItemType.LoginType.SetupTotpClick)
                assertEquals(VaultAddItemEvent.ShowToast("Setup TOTP"), awaitItem())
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UriSettingsClick should emit ShowToast with 'URI Settings' message`() = runTest {
            val viewModel = VaultAddItemViewModel(initialSavedStateHandle)

            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(VaultAddItemAction.ItemType.LoginType.UriSettingsClick)
                assertEquals(VaultAddItemEvent.ShowToast("URI Settings"), awaitItem())
            }
        }

        @Test
        fun `AddNewUriClick should emit ShowToast with 'Add New URI' message`() = runTest {
            val viewModel = VaultAddItemViewModel(initialSavedStateHandle)

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
            val viewModel = VaultAddItemViewModel(initialSavedStateHandle)

            viewModel.eventFlow.test {
                viewModel
                    .actionChannel
                    .trySend(
                        VaultAddItemAction.ItemType.LoginType.TooltipClick,
                    )
                assertEquals(VaultAddItemEvent.ShowToast("Tooltip"), awaitItem())
            }
        }

        @Test
        fun `AddNewCustomFieldClick should emit ShowToast with 'Add New Custom Field' message`() =
            runTest {
                val viewModel = VaultAddItemViewModel(initialSavedStateHandle)

                viewModel.eventFlow.test {
                    viewModel
                        .actionChannel
                        .trySend(
                            VaultAddItemAction.ItemType.LoginType.AddNewCustomFieldClick,
                        )
                    assertEquals(VaultAddItemEvent.ShowToast("Add New Custom Field"), awaitItem())
                }
            }
    }

    @Suppress("LongParameterList")
    private fun createVaultAddLoginItemState(
        name: String = "",
        username: String = "",
        password: String = "",
        uri: String = "",
        folder: String = "No Folder",
        favorite: Boolean = false,
        masterPasswordReprompt: Boolean = false,
        notes: String = "",
        ownership: String = "placeholder@email.com",
    ): VaultAddItemState =
        VaultAddItemState(
            selectedType = VaultAddItemState.ItemType.Login(
                name = name,
                username = username,
                password = password,
                uri = uri,
                folder = folder,
                favorite = favorite,
                masterPasswordReprompt = masterPasswordReprompt,
                notes = notes,
                ownership = ownership,
            ),
        )

    private fun createSavedStateHandleWithState(state: VaultAddItemState) =
        SavedStateHandle().apply {
            set("state", state)
        }
}
