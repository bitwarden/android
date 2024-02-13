package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.bitwarden.core.CipherView
import com.bitwarden.core.CollectionView
import com.bitwarden.core.FolderView
import com.bitwarden.core.SendView
import com.bitwarden.core.UriMatchType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.util.FakeGeneratorRepository
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldType
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.toCustomField
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toDefaultAddTypeContent
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toViewState
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import com.x8bit.bitwarden.ui.vault.model.VaultCollection
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

@Suppress("LargeClass")
class VaultAddEditViewModelTest : BaseViewModelTest() {

    private val settingsRepository: SettingsRepository = mockk {
        every { initialAutofillDialogShown = any() } just runs
        every { initialAutofillDialogShown } returns true
    }
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(createUserState())
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }

    private val loginInitialState = createVaultAddItemState(
        typeContentViewState = createLoginTypeContentViewState(),
    )
    private val loginInitialSavedStateHandle = createSavedStateHandleWithState(
        state = loginInitialState,
        vaultAddEditType = VaultAddEditType.AddItem,
    )

    private val totpTestCodeFlow: MutableSharedFlow<TotpCodeResult> = bufferedMutableSharedFlow()

    private val mutableVaultDataFlow = MutableStateFlow<DataState<VaultData>>(DataState.Loading)
    private val resourceManager: ResourceManager = mockk {
        every { getString(R.string.folder_none) } returns "No Folder"
    }
    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val policyManager: PolicyManager = mockk {
        every {
            getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns emptyList()
    }
    private val vaultRepository: VaultRepository = mockk {
        every { vaultDataStateFlow } returns mutableVaultDataFlow
        every { totpCodeFlow } returns totpTestCodeFlow
    }
    private val specialCircumstanceManager: SpecialCircumstanceManager =
        SpecialCircumstanceManagerImpl()

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
                createVaultAddItemState(
                    commonContentViewState = VaultAddEditState.ViewState.Content.Common(),
                    typeContentViewState = createLoginTypeContentViewState(),
                ),
                awaitItem(),
            )
        }
        verify {
            policyManager.getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        }
    }

    @Test
    fun `initial add state should be correct when not autofill`() = runTest {
        val vaultAddEditType = VaultAddEditType.AddItem
        val initState = createVaultAddItemState(vaultAddEditType = vaultAddEditType)
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = vaultAddEditType,
            ),
        )
        assertEquals(
            initState,
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            vaultRepository.vaultDataStateFlow
        }
    }

    @Test
    fun `initial add state should be correct with individual vault disabled`() = runTest {
        every {
            policyManager.getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(
            SyncResponseJson.Policy(
                organizationId = "Test Org",
                id = "testId",
                type = PolicyTypeJson.PERSONAL_OWNERSHIP,
                isEnabled = true,
                data = null,
            ),
        )
        val vaultAddEditType = VaultAddEditType.AddItem
        mutableVaultDataFlow.value = DataState.Loaded(
            data = createVaultData(),
        )
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = null,
                vaultAddEditType = vaultAddEditType,
            ),
        )
        assertEquals(
            VaultAddEditState(
                vaultAddEditType = vaultAddEditType,
                viewState = VaultAddEditState.ViewState.Content(
                    common = createCommonContentViewState(
                        availableOwners = listOf(
                            VaultAddEditState.Owner(
                                id = "organizationId",
                                name = "organizationName",
                                collections = emptyList(),
                            ),
                        ),
                    ),
                    isIndividualVaultDisabled = true,
                    type = VaultAddEditState.ViewState.Content.ItemType.Login(),
                ),
                dialog = null,
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            vaultRepository.vaultDataStateFlow
        }
        verify {
            policyManager.getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        }
    }

    @Test
    fun `initial add state should be correct when autofill selection`() = runTest {
        val autofillSelectionData = AutofillSelectionData(
            type = AutofillSelectionData.Type.LOGIN,
            uri = "https://www.test.com",
        )
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.AutofillSelection(
            autofillSelectionData = autofillSelectionData,
            shouldFinishWhenComplete = true,
        )
        val autofillContentState = autofillSelectionData.toDefaultAddTypeContent(
            isIndividualVaultDisabled = false,
        )
        val vaultAddEditType = VaultAddEditType.AddItem
        val initState = createVaultAddItemState(
            vaultAddEditType = vaultAddEditType,
            commonContentViewState = autofillContentState.common,
            typeContentViewState = autofillContentState.type,
        )
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = vaultAddEditType,
            ),
        )
        assertEquals(
            initState,
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            vaultRepository.vaultDataStateFlow
        }
    }

    @Test
    fun `initial add state should be correct when autofill save`() = runTest {
        val autofillSaveItem = AutofillSaveItem.Login(
            username = "username",
            password = "password",
            uri = "https://www.test.com",
        )
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.AutofillSave(
            autofillSaveItem = autofillSaveItem,
        )
        val autofillContentState = autofillSaveItem.toDefaultAddTypeContent(false)
        val vaultAddEditType = VaultAddEditType.AddItem
        val initState = createVaultAddItemState(
            vaultAddEditType = vaultAddEditType,
            commonContentViewState = autofillContentState.common,
            typeContentViewState = autofillContentState.type,
        )
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = vaultAddEditType,
            ),
        )
        assertEquals(
            initState,
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            vaultRepository.vaultDataStateFlow
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
            vaultRepository.vaultDataStateFlow
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
            vaultRepository.vaultDataStateFlow
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
            val cipherView = createMockCipherView(1)
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val initState = createVaultAddItemState(vaultAddEditType = vaultAddEditType)
            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(cipherView = cipherView),
            )
            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = initState,
                    vaultAddEditType = vaultAddEditType,
                ),
            )

            coEvery {
                vaultRepository.softDeleteCipher(
                    cipherId = "mockId-1",
                    cipherView = cipherView,
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
            val cipherView = createMockCipherView(1)
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val initState = createVaultAddItemState(vaultAddEditType = vaultAddEditType)
            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(cipherView = cipherView),
            )

            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = initState,
                    vaultAddEditType = vaultAddEditType,
                ),
            )

            coEvery {
                vaultRepository.softDeleteCipher(
                    cipherId = "mockId-1",
                    cipherView = cipherView,
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
                        originalCipher = createMockCipherView(number = 1),
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
                        canViewPassword = true,
                    )
                        .copy(totp = "mockTotp-1"),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode, SaveClick should show dialog, remove it once an item is saved, and emit NavigateBack`() =
        runTest {
            val stateWithDialog = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem,
                dialogState = VaultAddEditState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem,
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(),
            )
            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                ),
            )
            coEvery {
                vaultRepository.createCipherInOrganization(any(), any())
            } returns CreateCipherResult.Success

            turbineScope {
                val stateTurbine = viewModel.stateFlow.testIn(backgroundScope)
                val eventTurbine = viewModel.eventFlow.testIn(backgroundScope)

                viewModel.actionChannel.trySend(VaultAddEditAction.Common.SaveClick)

                assertEquals(stateWithName, stateTurbine.awaitItem())
                assertEquals(stateWithDialog, stateTurbine.awaitItem())
                assertEquals(stateWithName, stateTurbine.awaitItem())

                assertEquals(
                    VaultAddEditEvent.ShowToast(
                        R.string.new_item_created.asText(),
                    ),
                    eventTurbine.awaitItem(),
                )
                assertEquals(
                    VaultAddEditEvent.NavigateBack,
                    eventTurbine.awaitItem(),
                )
            }
            coVerify(exactly = 1) {
                vaultRepository.createCipherInOrganization(any(), any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode during autofill, SaveClick should show dialog, remove it once an item is saved, and emit ExitApp`() =
        runTest {
            val autofillSaveItem = AutofillSaveItem.Login(
                username = null,
                password = null,
                uri = null,
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AutofillSave(
                    autofillSaveItem = autofillSaveItem,
                )
            val stateWithDialog = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem,
                dialogState = VaultAddEditState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
                .copy(shouldExitOnSave = true)
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem,
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
                .copy(shouldExitOnSave = true)
            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(),
            )
            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                ),
            )
            coEvery {
                vaultRepository.createCipherInOrganization(any(), any())
            } returns CreateCipherResult.Success

            turbineScope {
                val stateTurbine = viewModel.stateFlow.testIn(backgroundScope)
                val eventTurbine = viewModel.eventFlow.testIn(backgroundScope)

                viewModel.actionChannel.trySend(VaultAddEditAction.Common.SaveClick)

                assertEquals(stateWithName, stateTurbine.awaitItem())
                assertEquals(stateWithDialog, stateTurbine.awaitItem())
                assertEquals(stateWithName, stateTurbine.awaitItem())

                assertEquals(
                    VaultAddEditEvent.ExitApp,
                    eventTurbine.awaitItem(),
                )
            }
            assertNull(specialCircumstanceManager.specialCircumstance)
            coVerify(exactly = 1) {
                vaultRepository.createCipherInOrganization(any(), any())
            }
        }

    @Test
    fun `in add mode, createCipherInOrganization success should ShowToast and NavigateBack`() =
        runTest {
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem,
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )

            mutableVaultDataFlow.value = DataState.Loaded(createVaultData())

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                ),
            )

            coEvery {
                vaultRepository.createCipherInOrganization(any(), any())
            } returns CreateCipherResult.Success
            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(VaultAddEditAction.Common.SaveClick)
                assertEquals(
                    VaultAddEditEvent.ShowToast(
                        R.string.new_item_created.asText(),
                    ),
                    awaitItem(),
                )
                assertEquals(VaultAddEditEvent.NavigateBack, awaitItem())
            }
        }

    @Test
    fun `in edit mode, updateCipher success should ShowToast and NavigateBack`() =
        runTest {
            val cipherView = createMockCipherView(1)
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )

            mutableVaultDataFlow.value = DataState.Loaded(createVaultData(cipherView = cipherView))

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                ),
            )

            coEvery {
                vaultRepository.updateCipher(any(), any())
            } returns UpdateCipherResult.Success
            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(VaultAddEditAction.Common.SaveClick)
                assertEquals(
                    VaultAddEditEvent.ShowToast(
                        R.string.item_updated.asText(),
                    ),
                    awaitItem(),
                )
                assertEquals(VaultAddEditEvent.NavigateBack, awaitItem())
            }
        }

    @Test
    fun `in add mode, SaveClick createCipher error should show error dialog`() = runTest {

        val stateWithName = createVaultAddItemState(
            vaultAddEditType = VaultAddEditType.AddItem,
            commonContentViewState = createCommonContentViewState(
                name = "mockName-1",
            ),
        )
        mutableVaultDataFlow.value = DataState.Loaded(createVaultData())

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = stateWithName,
                vaultAddEditType = VaultAddEditType.AddItem,
            ),
        )

        coEvery {
            vaultRepository.createCipherInOrganization(any(), any())
        } returns CreateCipherResult.Error
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
    }

    @Test
    fun `in edit mode, SaveClick should show dialog, and remove it once an item is saved`() =
        runTest {
            val cipherView = createMockCipherView(1)
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val stateWithDialog = createVaultAddItemState(
                vaultAddEditType = vaultAddEditType,
                dialogState = VaultAddEditState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                    originalCipher = cipherView,
                    customFieldData = listOf(
                        VaultAddEditState.Custom.HiddenField(
                            itemId = "testId",
                            name = "mockName-1",
                            value = "mockValue-1",
                        ),
                    ),
                    notes = "mockNotes-1",
                ),
            )

            val stateWithName = createVaultAddItemState(
                vaultAddEditType = vaultAddEditType,
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                    originalCipher = cipherView,
                    customFieldData = listOf(
                        VaultAddEditState.Custom.HiddenField(
                            itemId = "testId",
                            name = "mockName-1",
                            value = "mockValue-1",
                        ),
                    ),
                    notes = "mockNotes-1",
                ),
            )
            every {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    resourceManager = resourceManager,
                )
            } returns stateWithName.viewState
            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(cipherView = cipherView),
            )

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
                    isIndividualVaultDisabled = false,
                    resourceManager = resourceManager,
                )
                vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in edit mode, SaveClick updateCipher error with a null message should show an error dialog with a generic message`() =
        runTest {
            val cipherView = createMockCipherView(1)
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = vaultAddEditType,
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                    originalCipher = cipherView,
                    customFieldData = listOf(
                        VaultAddEditState.Custom.HiddenField(
                            itemId = "testId",
                            name = "mockName-1",
                            value = "mockValue-1",
                        ),
                    ),
                    notes = "mockNotes-1",
                ),
            )

            every {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    resourceManager = resourceManager,
                )
            } returns stateWithName.viewState
            coEvery {
                vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
            } returns UpdateCipherResult.Error(errorMessage = null)
            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(cipherView = cipherView),
            )

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
            val cipherView = createMockCipherView(1)
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = vaultAddEditType,
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                    originalCipher = cipherView,
                    customFieldData = listOf(
                        VaultAddEditState.Custom.HiddenField(
                            itemId = "testId",
                            name = "mockName-1",
                            value = "mockValue-1",
                        ),
                    ),
                    notes = "mockNotes-1",
                ),
            )
            val errorMessage = "You do not have permission to edit this."

            every {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    resourceManager = resourceManager,
                )
            } returns stateWithName.viewState
            coEvery {
                vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
            } returns UpdateCipherResult.Error(errorMessage = errorMessage)
            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(cipherView = cipherView),
            )

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
        mutableVaultDataFlow.value = DataState.Loaded(
            createVaultData(cipherView = createMockCipherView(1)),
        )
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
        mutableVaultDataFlow.value = DataState.Loaded(
            createVaultData(cipherView = createMockCipherView(1)),
        )
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
        mutableVaultDataFlow.value = DataState.Loaded(
            createVaultData(cipherView = createMockCipherView(1)),
        )
        val viewModel = createAddVaultItemViewModel()
        val action = VaultAddEditAction.Common.TypeOptionSelect(
            VaultAddEditState.ItemTypeOption.LOGIN,
        )

        viewModel.actionChannel.trySend(action)

        val expectedState = loginInitialState.copy(
            viewState = VaultAddEditState.ViewState.Content(
                common = createCommonContentViewState(),
                isIndividualVaultDisabled = false,
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
        mutableVaultDataFlow.value = DataState.Loaded(
            createVaultData(cipherView = createMockCipherView(1)),
        )
        val viewModel = createAddVaultItemViewModel()
        val action = VaultAddEditAction.Common.TypeOptionSelect(
            VaultAddEditState.ItemTypeOption.CARD,
        )

        viewModel.actionChannel.trySend(action)

        val expectedState = loginInitialState.copy(
            viewState = VaultAddEditState.ViewState.Content(
                common = createCommonContentViewState(),
                isIndividualVaultDisabled = false,
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
        mutableVaultDataFlow.value = DataState.Loaded(
            createVaultData(cipherView = createMockCipherView(1)),
        )
        val viewModel = createAddVaultItemViewModel()
        val action = VaultAddEditAction.Common.TypeOptionSelect(
            VaultAddEditState.ItemTypeOption.IDENTITY,
        )

        viewModel.actionChannel.trySend(action)

        val expectedState = loginInitialState.copy(
            viewState = VaultAddEditState.ViewState.Content(
                common = createCommonContentViewState(),
                isIndividualVaultDisabled = false,
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
        mutableVaultDataFlow.value = DataState.Loaded(
            createVaultData(cipherView = createMockCipherView(1)),
        )
        val viewModel = createAddVaultItemViewModel()
        val action = VaultAddEditAction.Common.TypeOptionSelect(
            VaultAddEditState.ItemTypeOption.SECURE_NOTES,
        )

        viewModel.actionChannel.trySend(action)

        val expectedState = loginInitialState.copy(
            viewState = VaultAddEditState.ViewState.Content(
                common = createCommonContentViewState(),
                isIndividualVaultDisabled = false,
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
            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(cipherView = createMockCipherView(1)),
            )

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
                    isIndividualVaultDisabled = false,
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

            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(
                    cipherView = cipherView,
                ),
            )

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
                    isIndividualVaultDisabled = false,
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
                    isIndividualVaultDisabled = false,
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
        fun `UriValueChange should update URI value in state`() = runTest {
            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = createVaultAddItemState(
                        typeContentViewState = createLoginTypeContentViewState(
                            uri = listOf(UriItem("testID", null, null)),
                        ),
                    ),
                    vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID),
                ),
            )
            val expectedState = loginInitialState.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = createCommonContentViewState(),
                    isIndividualVaultDisabled = false,
                    type = createLoginTypeContentViewState(
                        uri = listOf(UriItem("testID", "Test", null)),
                    ),
                ),
            )

            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.UriValueChange(
                    uriItem = UriItem("testID", "Test", null),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `RemoveUriClick should remove URI value in state`() = runTest {
            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = createVaultAddItemState(
                        typeContentViewState = createLoginTypeContentViewState(
                            uri = listOf(UriItem("testID", null, null)),
                        ),
                    ),
                    vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID),
                ),
            )

            val expectedState = loginInitialState.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = createCommonContentViewState(),
                    isIndividualVaultDisabled = false,
                    type = createLoginTypeContentViewState(
                        uri = listOf(),
                    ),
                ),
            )

            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.RemoveUriClick(
                    uriItem = UriItem("testID", null, null),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
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
            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(cipherView = createMockCipherView(1)),
            )
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
            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(cipherView = createMockCipherView(1)),
            )
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
            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(cipherView = createMockCipherView(1)),
            )
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
                specialCircumstanceManager = specialCircumstanceManager,
                policyManager = policyManager,
                resourceManager = resourceManager,
                authRepository = authRepository,
                settingsRepository = settingsRepository,
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
                    isIndividualVaultDisabled = false,
                    type = createLoginTypeContentViewState(),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `FolderChange should update folder`() = runTest {
            val action = VaultAddEditAction.Common.FolderChange(
                VaultAddEditState.Folder(
                    id = "mockId-1",
                    name = "Folder 1",
                ),
            )

            viewModel.actionChannel.trySend(action)

            val expectedState = vaultAddItemInitialState.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = createCommonContentViewState()
                        .copy(selectedFolderId = "mockId-1"),
                    isIndividualVaultDisabled = false,
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
                    isIndividualVaultDisabled = false,
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
                    isIndividualVaultDisabled = false,
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
                    isIndividualVaultDisabled = false,
                    type = createLoginTypeContentViewState(),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `OwnershipChange should update ownership`() = runTest {
            val action = VaultAddEditAction.Common.OwnershipChange(
                ownership = VaultAddEditState.Owner(
                    id = "mockId-1",
                    name = "a@b.com",
                    collections = emptyList(),
                ),
            )

            viewModel.actionChannel.trySend(action)

            val expectedState = vaultAddItemInitialState.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = createCommonContentViewState()
                        .copy(selectedOwnerId = "mockId-1"),
                    isIndividualVaultDisabled = false,
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
        fun `TooltipClick should emit NavigateToToolTipUri`() = runTest {
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAddEditAction.Common.TooltipClick)
                assertEquals(
                    VaultAddEditEvent.NavigateToTooltipUri,
                    awaitItem(),
                )
            }
        }

        @Test
        fun `InitialAutofillDialogDismissed should update the settings value to true`() {
            viewModel.trySendAction(VaultAddEditAction.Common.InitialAutofillDialogDismissed)

            verify {
                settingsRepository.initialAutofillDialogShown = true
            }
        }

        @Test
        fun `CollectionSelect should update availableOwners collections`() = runTest {
            viewModel.actionChannel.trySend(ownershipChangeAction())

            val action = collectionSelectAction()
            viewModel.actionChannel.trySend(action)

            val expectedState = vaultAddItemInitialState.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = createCommonContentViewState(
                        availableOwners = createOwnerList(isCollectionSelected = true),
                        selectedOwnerId = "organizationId",
                    ),
                    isIndividualVaultDisabled = false,
                    type = createLoginTypeContentViewState(),
                ),
            )
            assertEquals(expectedState, viewModel.stateFlow.value)
        }
    }
    //region Helper functions

    @Suppress("MaxLineLength")
    private fun createVaultAddItemState(
        vaultAddEditType: VaultAddEditType = VaultAddEditType.AddItem,
        commonContentViewState: VaultAddEditState.ViewState.Content.Common = createCommonContentViewState(),
        isIndividualVaultDisabled: Boolean = false,
        typeContentViewState: VaultAddEditState.ViewState.Content.ItemType = createLoginTypeContentViewState(),
        dialogState: VaultAddEditState.DialogState? = null,
    ): VaultAddEditState =
        VaultAddEditState(
            vaultAddEditType = vaultAddEditType,
            viewState = VaultAddEditState.ViewState.Content(
                common = commonContentViewState,
                isIndividualVaultDisabled = isIndividualVaultDisabled,
                type = typeContentViewState,
            ),
            dialog = dialogState,
        )

    @Suppress("LongParameterList")
    private fun createCommonContentViewState(
        name: String = "",
        favorite: Boolean = false,
        masterPasswordReprompt: Boolean = false,
        notes: String = "",
        customFieldData: List<VaultAddEditState.Custom> = listOf(),
        originalCipher: CipherView? = null,
        availableFolders: List<VaultAddEditState.Folder> = listOf(
            VaultAddEditState.Folder(
                id = null,
                name = "No Folder",
            ),
        ),
        availableOwners: List<VaultAddEditState.Owner> = createOwnerList(),
        selectedOwnerId: String? = null,
    ): VaultAddEditState.ViewState.Content.Common =
        VaultAddEditState.ViewState.Content.Common(
            name = name,
            selectedFolderId = null,
            favorite = favorite,
            customFieldData = customFieldData,
            masterPasswordReprompt = masterPasswordReprompt,
            notes = notes,
            selectedOwnerId = selectedOwnerId,
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
            specialCircumstanceManager = specialCircumstanceManager,
            policyManager = policyManager,
            resourceManager = bitwardenResourceManager,
            authRepository = authRepository,
            settingsRepository = settingsRepository,
        )

    private fun createVaultData(
        cipherView: CipherView? = null,
        collectionViewList: List<CollectionView> = emptyList(),
        folderViewList: List<FolderView> = emptyList(),
        sendViewList: List<SendView> = emptyList(),
    ): VaultData =
        VaultData(
            cipherViewList = cipherView?.let { listOf(it) } ?: emptyList(),
            collectionViewList = collectionViewList,
            folderViewList = folderViewList,
            sendViewList = sendViewList,
        )

    private fun createUserState(): UserState =
        UserState(
            activeUserId = "activeUserId",
            accounts = listOf(
                UserState.Account(
                    userId = "activeUserId",
                    name = "activeName",
                    email = "activeEmail",
                    avatarColorHex = "#ffecbc49",
                    environment = Environment.Eu,
                    isPremium = true,
                    isLoggedIn = false,
                    isVaultUnlocked = false,
                    needsPasswordReset = false,
                    organizations = listOf(
                        Organization(
                            id = "organizationId",
                            name = "organizationName",
                        ),
                    ),
                    isBiometricsEnabled = true,
                    vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                ),
            ),
            hasPendingAccountAddition = false,
        )

    private fun createOwnerList(
        hasCollection: Boolean = false,
        isCollectionSelected: Boolean = false,
    ): List<VaultAddEditState.Owner> =
        listOf(
            VaultAddEditState.Owner(
                id = null,
                name = "activeEmail",
                collections = emptyList(),
            ),
            VaultAddEditState.Owner(
                id = "organizationId",
                name = "organizationName",
                collections = if (hasCollection) {
                    listOf(
                        VaultCollection(
                            id = "mockId-1",
                            name = "mockName-1",
                            isSelected = isCollectionSelected,
                        ),
                    )
                } else {
                    emptyList()
                },
            ),
        )

    private fun ownershipChangeAction(): VaultAddEditAction.Common.OwnershipChange =
        VaultAddEditAction.Common.OwnershipChange(
            ownership = VaultAddEditState.Owner(
                id = "organizationId",
                name = "organizationName",
                collections = listOf(
                    VaultCollection(
                        id = "mockId-1",
                        name = "mockName-1",
                        isSelected = false,
                    ),
                ),
            ),
        )

    private fun collectionSelectAction(): VaultAddEditAction.Common.CollectionSelect =
        VaultAddEditAction.Common.CollectionSelect(
            VaultCollection(
                id = "mockId-1",
                name = "mockName-1",
                isSelected = false,
            ),
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

private const val DEFAULT_EDIT_ITEM_ID: String = "mockId-1"
