package com.x8bit.bitwarden.ui.vault.feature.addedit

import android.content.pm.SigningInfo
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.send.SendView
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.CollectionView
import com.bitwarden.vault.FolderView
import com.bitwarden.vault.UriMatchType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePinResult
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.model.UserVerificationRequirement
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2CredentialRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.util.FakeGeneratorRepository
import com.x8bit.bitwarden.data.vault.datasource.network.model.OrganizationType
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createEditCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createEditExceptPasswordsCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createManageCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFido2CredentialList
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createViewCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createViewExceptPasswordsCollectionView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldType
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.toCustomField
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.createMockPasskeyAttestationOptions
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toDefaultAddTypeContent
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toViewState
import com.x8bit.bitwarden.ui.vault.model.TotpData
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import com.x8bit.bitwarden.ui.vault.model.VaultCollection
import com.x8bit.bitwarden.ui.vault.model.VaultIdentityTitle
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@Suppress("LargeClass")
class VaultAddEditViewModelTest : BaseViewModelTest() {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val settingsRepository: SettingsRepository = mockk {
        every { initialAutofillDialogShown = any() } just runs
        every { initialAutofillDialogShown } returns true
        every { isUnlockWithPinEnabled } returns false
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
        vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
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
    private val fido2CredentialManager = mockk<Fido2CredentialManager> {
        every { isUserVerified } returns false
        every { isUserVerified = any() } just runs
        every { authenticationAttempts } returns 0
        every { authenticationAttempts = any() } just runs
        every { hasAuthenticationAttemptsRemaining() } returns true
    }
    private val vaultRepository: VaultRepository = mockk {
        every { vaultDataStateFlow } returns mutableVaultDataFlow
        every { totpCodeFlow } returns totpTestCodeFlow
    }

    private val mockAuthRepository = mockk<AuthRepository>(relaxed = true)
    private val specialCircumstanceManager: SpecialCircumstanceManager =
        SpecialCircumstanceManagerImpl(
            authRepository = mockAuthRepository,
            dispatcherManager = FakeDispatcherManager(),
        )

    private val generatorRepository: GeneratorRepository = FakeGeneratorRepository()
    private val organizationEventManager = mockk<OrganizationEventManager> {
        every { trackEvent(event = any()) } just runs
    }
    private val networkConnectionManager = mockk<NetworkConnectionManager> {
        every { isNetworkConnected } returns true
    }

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
        val expectedState = VaultAddEditState(
            vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                isIndividualVaultDisabled = false,
                type = VaultAddEditState.ViewState.Content.ItemType.Login(),
            ),
            dialog = null,
            totpData = null,
            shouldShowCloseButton = true,
            shouldExitOnSave = false,
            supportedItemTypes = VaultAddEditState.ItemTypeOption.entries
                .filter { it != VaultAddEditState.ItemTypeOption.SSH_KEYS },
        )
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = null,
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
            ),
        )
        viewModel.stateFlow.test {
            assertEquals(
                expectedState,
                awaitItem(),
            )
        }
        verify {
            policyManager.getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        }
    }

    @Test
    fun `initial add state should be correct when not autofill`() = runTest {
        val vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN)
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
        verify(exactly = 0) {
            organizationEventManager.trackEvent(event = any())
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
        val vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN)
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
                supportedItemTypes = VaultAddEditState.ItemTypeOption.entries
                    .filter { it != VaultAddEditState.ItemTypeOption.SSH_KEYS },
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
            framework = AutofillSelectionData.Framework.AUTOFILL,
            uri = "https://www.test.com",
        )
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.AutofillSelection(
            autofillSelectionData = autofillSelectionData,
            shouldFinishWhenComplete = true,
        )
        val autofillContentState = autofillSelectionData.toDefaultAddTypeContent(
            isIndividualVaultDisabled = false,
        )
        val vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN)
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
        val vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN)
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
    fun `initial add state should be correct when fido2 save`() = runTest {
        val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
            userId = "mockUserId-1",
            requestJson = "mockRequestJson-1",
            packageName = "mockPackageName-1",
            signingInfo = SigningInfo(),
            origin = null,
        )
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
            fido2CreateCredentialRequest = fido2CreateCredentialRequest,
        )
        val fido2ContentState = fido2CreateCredentialRequest.toDefaultAddTypeContent(
            attestationOptions = createMockPasskeyAttestationOptions(number = 1),
            isIndividualVaultDisabled = false,
        )
        val vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN)
        val initState = createVaultAddItemState(
            vaultAddEditType = vaultAddEditType,
            commonContentViewState = fido2ContentState.common,
            typeContentViewState = fido2ContentState.type,
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
            organizationEventManager.trackEvent(
                event = OrganizationEvent.CipherClientViewed(cipherId = DEFAULT_EDIT_ITEM_ID),
            )
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
        verify(exactly = 0) {
            organizationEventManager.trackEvent(event = any())
        }
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createAddVaultItemViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAddEditAction.Common.CloseClick)
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
            viewModel.trySendAction(VaultAddEditAction.Common.AttachmentsClick)
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
            viewModel.trySendAction(VaultAddEditAction.Common.MoveToOrganizationClick)
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
            viewModel.trySendAction(VaultAddEditAction.Common.CollectionsClick)
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
                        uri = listOf(
                            UriItem(
                                id = "testId",
                                uri = "www.mockuri1.com",
                                match = UriMatchType.HOST,
                                checksum = "mockUriChecksum-1",
                            ),
                        ),
                        totpCode = "mockTotp-1",
                        canViewPassword = true,
                        fido2CredentialCreationDateTime = null,
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
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                dialogState = VaultAddEditState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
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
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                ),
            )
            coEvery {
                vaultRepository.createCipherInOrganization(any(), any())
            } returns CreateCipherResult.Success

            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

                assertEquals(stateWithName, stateFlow.awaitItem())
                assertEquals(stateWithDialog, stateFlow.awaitItem())
                assertEquals(stateWithName, stateFlow.awaitItem())

                assertEquals(
                    VaultAddEditEvent.ShowToast(
                        R.string.new_item_created.asText(),
                    ),
                    eventFlow.awaitItem(),
                )
                assertEquals(
                    VaultAddEditEvent.NavigateBack,
                    eventFlow.awaitItem(),
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
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                dialogState = VaultAddEditState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
                .copy(shouldExitOnSave = true)
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
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
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                ),
            )
            coEvery {
                vaultRepository.createCipherInOrganization(any(), any())
            } returns CreateCipherResult.Success

            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

                assertEquals(stateWithName, stateFlow.awaitItem())
                assertEquals(stateWithDialog, stateFlow.awaitItem())
                assertEquals(stateWithName, stateFlow.awaitItem())

                assertEquals(
                    VaultAddEditEvent.ExitApp,
                    eventFlow.awaitItem(),
                )
            }
            assertNull(specialCircumstanceManager.specialCircumstance)
            coVerify(exactly = 1) {
                vaultRepository.createCipherInOrganization(any(), any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode during totp fill, SaveClick should show dialog, remove it once an item is saved, and emit ExitApp`() =
        runTest {
            val totpData = TotpData(
                uri = "otpauth://totp/issuer:accountName?secret=secret",
                issuer = "issuer",
                accountName = "accountName",
                secret = "secret",
                digits = 6,
                period = 30,
                algorithm = TotpData.CryptoHashAlgorithm.SHA_1,
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AddTotpLoginItem(data = totpData)
            val stateWithDialog = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                dialogState = VaultAddEditState.DialogState.Loading(R.string.saving.asText()),
                commonContentViewState = createCommonContentViewState(name = "issuer"),
                totpData = totpData,
                shouldExitOnSave = true,
            )
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                commonContentViewState = createCommonContentViewState(name = "issuer"),
                totpData = totpData,
                shouldExitOnSave = true,
            )
            mutableVaultDataFlow.value = DataState.Loaded(createVaultData())
            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                ),
            )
            coEvery {
                vaultRepository.createCipherInOrganization(any(), any())
            } returns CreateCipherResult.Success

            viewModel.stateEventFlow(backgroundScope) { stateTurbine, eventTurbine ->
                viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

                assertEquals(stateWithName, stateTurbine.awaitItem())
                assertEquals(stateWithDialog, stateTurbine.awaitItem())
                assertEquals(stateWithName, stateTurbine.awaitItem())

                assertEquals(VaultAddEditEvent.ExitApp, eventTurbine.awaitItem())
            }
            assertNull(specialCircumstanceManager.specialCircumstance)
            coVerify(exactly = 1) {
                vaultRepository.createCipherInOrganization(any(), any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode during fido2 registration, SaveClick should show saving dialog, and request user verification when required`() =
        runTest {
            val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
                userId = "mockUserId",
                requestJson = "mockRequestJson",
                packageName = "mockPackageName",
                signingInfo = mockk<SigningInfo>(),
                origin = null,
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2Save(
                    fido2CreateCredentialRequest = fido2CreateCredentialRequest,
                )
            val stateWithSavingDialog = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                dialogState = VaultAddEditState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
                .copy(shouldExitOnSave = true)

            val stateWithNewLogin = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
                .copy(shouldExitOnSave = true)

            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(),
            )

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithNewLogin,
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                ),
            )
            val mockCreateResult = Fido2RegisterCredentialResult.Success(
                registrationResponse = "mockRegistrationResponse",
            )
            val mockAttestationOptions = createMockPasskeyAttestationOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.REQUIRED,
            )

            coEvery {
                fido2CredentialManager.registerFido2Credential(
                    userId = "mockUserId",
                    selectedCipherView = any(),
                    fido2CreateCredentialRequest = fido2CreateCredentialRequest,
                )
            } returns mockCreateResult
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(
                    requestJson = fido2CreateCredentialRequest.requestJson,
                )
            } returns mockAttestationOptions
            every { authRepository.activeUserId } returns "mockUserId"

            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

                assertEquals(stateWithNewLogin, stateFlow.awaitItem())
                assertEquals(stateWithSavingDialog, stateFlow.awaitItem())
                assertEquals(
                    VaultAddEditEvent.Fido2UserVerification(isRequired = true),
                    eventFlow.awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode during fido2, SaveClick should show saving dialog, remove it once item is saved, skip user verification when not required, and emit ExitApp`() =
        runTest {
            val mockUserId = "mockUserId"
            val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
                userId = mockUserId,
                requestJson = "mockRequestJson",
                packageName = "mockPackageName",
                signingInfo = mockk<SigningInfo>(),
                origin = null,
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2Save(
                    fido2CreateCredentialRequest = fido2CreateCredentialRequest,
                )
            val stateWithSavingDialog = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                dialogState = VaultAddEditState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
                .copy(shouldExitOnSave = true)

            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
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
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                ),
            )
            val mockCreateResult = Fido2RegisterCredentialResult.Success("mockResponse")
            val mockAttestationOptions = createMockPasskeyAttestationOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.DISCOURAGED,
            )
            coEvery {
                fido2CredentialManager.registerFido2Credential(
                    userId = mockUserId,
                    selectedCipherView = any(),
                    fido2CreateCredentialRequest = fido2CreateCredentialRequest,
                )
            } returns mockCreateResult
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(
                    requestJson = fido2CreateCredentialRequest.requestJson,
                )
            } returns mockAttestationOptions
            every { authRepository.activeUserId } returns mockUserId

            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

                assertEquals(stateWithName, stateFlow.awaitItem())
                assertEquals(stateWithSavingDialog, stateFlow.awaitItem())
                assertEquals(
                    VaultAddEditEvent.ShowToast(R.string.item_updated.asText()),
                    eventFlow.awaitItem(),
                )
                assertEquals(stateWithName, stateFlow.awaitItem())
                assertEquals(
                    VaultAddEditEvent.CompleteFido2Registration(mockCreateResult),
                    eventFlow.awaitItem(),
                )
                coVerify(exactly = 1) {
                    fido2CredentialManager.registerFido2Credential(
                        userId = mockUserId,
                        selectedCipherView = any(),
                        fido2CreateCredentialRequest = fido2CreateCredentialRequest,
                    )
                }
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode during fido2, SaveClick should skip user verification when user is verified`() =
        runTest {
            val fido2CredentialRequest = createMockFido2CredentialRequest(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2Save(
                    fido2CreateCredentialRequest = fido2CredentialRequest,
                )
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
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
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                ),
            )

            every { authRepository.activeUserId } returns fido2CredentialRequest.userId
            every { fido2CredentialManager.isUserVerified } returns true
            coEvery {
                fido2CredentialManager.registerFido2Credential(
                    userId = fido2CredentialRequest.userId,
                    fido2CreateCredentialRequest = fido2CredentialRequest,
                    selectedCipherView = any(),
                )
            } returns Fido2RegisterCredentialResult.Success(registrationResponse = "mockResponse")

            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

            coVerify {
                fido2CredentialManager.registerFido2Credential(
                    userId = fido2CredentialRequest.userId,
                    fido2CreateCredentialRequest = fido2CredentialRequest,
                    selectedCipherView = any(),
                )
            }

            verify(exactly = 0) {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode during fido2, SaveClick should show fido2 error dialog when create options are null`() =
        runTest {
            val fido2CredentialRequest = createMockFido2CredentialRequest(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2Save(
                    fido2CreateCredentialRequest = fido2CredentialRequest,
                )
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
                .copy(shouldExitOnSave = true)

            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(
                    requestJson = fido2CredentialRequest.requestJson,
                )
            } returns null

            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(),
            )
            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                ),
            )

            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

            assertEquals(
                VaultAddEditState.DialogState.Fido2Error(
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode during fido2, SaveClick should emit fido user verification as optional when verification is PREFERRED`() =
        runTest {
            val fido2CredentialRequest = createMockFido2CredentialRequest(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2Save(
                    fido2CreateCredentialRequest = fido2CredentialRequest,
                )
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
                .copy(shouldExitOnSave = true)

            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(
                    requestJson = fido2CredentialRequest.requestJson,
                )
            } returns createMockPasskeyAttestationOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.PREFERRED,
            )
            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(),
            )
            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                ),
            )

            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

            viewModel.eventFlow.test {
                assertEquals(
                    VaultAddEditEvent.Fido2UserVerification(isRequired = false),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode during fido2, SaveClick should emit fido user verification as required when request user verification option is REQUIRED`() =
        runTest {
            val fido2CredentialRequest = createMockFido2CredentialRequest(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2Save(
                    fido2CreateCredentialRequest = fido2CredentialRequest,
                )
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
                .copy(shouldExitOnSave = true)

            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(
                    requestJson = fido2CredentialRequest.requestJson,
                )
            } returns createMockPasskeyAttestationOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.REQUIRED,
            )
            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(),
            )
            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                ),
            )

            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

            viewModel.eventFlow.test {
                assertEquals(
                    VaultAddEditEvent.Fido2UserVerification(isRequired = true),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `in add mode, createCipherInOrganization success should ShowToast and NavigateBack`() =
        runTest {
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )

            mutableVaultDataFlow.value = DataState.Loaded(createVaultData())

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                ),
            )

            coEvery {
                vaultRepository.createCipherInOrganization(any(), any())
            } returns CreateCipherResult.Success
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)
                assertEquals(
                    VaultAddEditEvent.ShowToast(
                        R.string.new_item_created.asText(),
                    ),
                    awaitItem(),
                )
                assertEquals(VaultAddEditEvent.NavigateBack, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in edit mode, canDelete should be false when cipher is in a collection the user cannot manage`() =
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
                    canDelete = false,
                ),
            )

            every {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = false,
                    canAssignToCollections = false,
                )
            } returns stateWithName.viewState

            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(
                    cipherView = cipherView,
                    collectionViewList = listOf(
                        createEditCollectionView(number = 1),
                    ),
                ),
            )

            createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                ),
            )

            verify {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = false,
                    canAssignToCollections = false,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in edit mode, canDelete should be true when cipher is in a collection the user can manage`() =
        runTest {
            val cipherView = createMockCipherView(1)
                .copy(collectionIds = listOf("mockId-1", "mockId-2"))
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
                    canDelete = true,
                    canAssociateToCollections = true,
                ),
            )

            every {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns stateWithName.viewState

            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(
                    cipherView = cipherView,
                    collectionViewList = listOf(
                        createManageCollectionView(number = 1),
                        createViewCollectionView(number = 2),
                    ),
                ),
            )

            createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                ),
            )

            verify {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in edit mode, canAssociateToCollections should be false when cipher is in a collection with view permission`() =
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
                    canDelete = false,
                    canAssociateToCollections = false,
                ),
            )

            every {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = false,
                    canAssignToCollections = false,
                )
            } returns stateWithName.viewState

            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(
                    cipherView = cipherView,
                    collectionViewList = listOf(
                        createViewCollectionView(number = 1),
                    ),
                ),
            )

            createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                ),
            )

            verify {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = false,
                    canAssignToCollections = false,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in edit mode, canAssociateToCollections should be false when cipher is in a collection with manage permission and a collection with edit, except password permission`() =
        runTest {
            val cipherView = createMockCipherView(1)
                .copy(collectionIds = listOf("mockId-1", "mockId-2"))
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
                    canDelete = false,
                    canAssociateToCollections = false,
                ),
            )

            every {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = true,
                    canAssignToCollections = false,
                )
            } returns stateWithName.viewState

            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(
                    cipherView = cipherView,
                    collectionViewList = listOf(
                        createManageCollectionView(number = 1),
                        createEditExceptPasswordsCollectionView(number = 2),
                    ),
                ),
            )

            createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                ),
            )

            verify {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = true,
                    canAssignToCollections = false,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in edit mode, canAssociateToCollections should be false when cipher is in a collection with manage permission and a collection with view, except password permission`() =
        runTest {
            val cipherView = createMockCipherView(1)
                .copy(collectionIds = listOf("mockId-1", "mockId-2"))
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
                    canDelete = true,
                    canAssociateToCollections = false,
                ),
            )

            every {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = true,
                    canAssignToCollections = false,
                )
            } returns stateWithName.viewState

            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(
                    cipherView = cipherView,
                    collectionViewList = listOf(
                        createManageCollectionView(number = 1),
                        createViewExceptPasswordsCollectionView(number = 2),
                    ),
                ),
            )

            createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                ),
            )

            verify {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = true,
                    canAssignToCollections = false,
                )
            }
        }

    @Test
    fun `in edit mode, updateCipher success should ShowToast and NavigateBack`() = runTest {
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
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
            ),
        )

        coEvery {
            vaultRepository.updateCipher(any(), any())
        } returns UpdateCipherResult.Success
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)
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
    fun `in add mode, SaveClick with no network connection error should show error dialog`() =
        runTest {
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
            mutableVaultDataFlow.value = DataState.Loaded(createVaultData())
            every { networkConnectionManager.isNetworkConnected } returns false

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                ),
            )

            coEvery {
                vaultRepository.createCipherInOrganization(any(), any())
            } returns CreateCipherResult.Error
            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

            assertEquals(
                stateWithName.copy(
                    dialog = VaultAddEditState.DialogState.Generic(
                        title = R.string.internet_connection_required_title.asText(),
                        message = R.string.internet_connection_required_message.asText(),
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
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = true,
                    canAssignToCollections = true,
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
                viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)
                assertEquals(stateWithDialog, awaitItem())
                assertEquals(stateWithName, awaitItem())
            }

            coVerify(exactly = 1) {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = true,
                    canAssignToCollections = true,
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
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = true,
                    canAssignToCollections = true,
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

            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

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
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = true,
                    canAssignToCollections = true,
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

            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

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

    @Suppress("MaxLineLength")
    @Test
    fun `in edit mode during FIDO 2 registration, SaveClick should display ConfirmOverwriteExistingPasskeyDialog when original cipher has a passkey`() =
        runTest {
            val cipherView = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val stateWithName = createVaultAddItemState(
                commonContentViewState = createCommonContentViewState(
                    name = cipherView.name,
                    originalCipher = cipherView,
                ),
                typeContentViewState = createLoginTypeContentViewState(
                    fido2CredentialCreationDateTime = R.string.created_xy.asText(
                        "05/08/24",
                        "14:30 PM",
                    ),
                ),
            )
            val mockFido2CredentialRequest = createMockFido2CredentialRequest(number = 1)

            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = mockFido2CredentialRequest,
            )
            every {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns stateWithName.viewState
            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(cipherView = cipherView),
            )

            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                ),
            )

            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

            assertEquals(
                VaultAddEditState.DialogState.OverwritePasskeyConfirmationPrompt,
                viewModel.stateFlow.value.dialog,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmOverwriteExistingPasskeyClick should register credential when user is verified`() {
        val cipherView = createMockCipherView(
            number = 1,
            fido2Credentials = createMockSdkFido2CredentialList(number = 1),
        )
        val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
        val stateWithName = createVaultAddItemState(
            vaultAddEditType = vaultAddEditType,
            commonContentViewState = createCommonContentViewState(
                name = "mockName-1",
                originalCipher = cipherView,
                notes = "mockNotes-1",
            ),
        )
        val mockFidoRequest = createMockFido2CredentialRequest(number = 1)
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
            fido2CreateCredentialRequest = mockFidoRequest,
        )
        coEvery {
            fido2CredentialManager.registerFido2Credential(
                userId = mockFidoRequest.userId,
                fido2CreateCredentialRequest = mockFidoRequest,
                selectedCipherView = any(),
            )
        } returns Fido2RegisterCredentialResult.Success("mockResponse")
        every { authRepository.activeUserId } returns mockFidoRequest.userId
        every {
            cipherView.toViewState(
                isClone = false,
                isIndividualVaultDisabled = false,
                totpData = null,
                resourceManager = resourceManager,
                clock = fixedClock,
                canDelete = true,
                canAssignToCollections = true,
            )
        } returns stateWithName.viewState
        every { fido2CredentialManager.isUserVerified } returns true

        mutableVaultDataFlow.value = DataState.Loaded(
            createVaultData(cipherView = cipherView),
        )

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = stateWithName,
                vaultAddEditType = vaultAddEditType,
            ),
        )
        viewModel.trySendAction(VaultAddEditAction.Common.ConfirmOverwriteExistingPasskeyClick)

        coVerify {
            fido2CredentialManager.isUserVerified
            fido2CredentialManager.registerFido2Credential(
                userId = mockFidoRequest.userId,
                fido2CreateCredentialRequest = mockFidoRequest,
                selectedCipherView = any(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmOverwriteExistingPasskeyClick should check if user verification is required`() =
        runTest {
            val cipherView = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
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
            val mockFidoRequest = createMockFido2CredentialRequest(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = mockFidoRequest,
            )
            every {
                cipherView.toViewState(
                    isClone = false,
                    isIndividualVaultDisabled = false,
                    totpData = null,
                    resourceManager = resourceManager,
                    clock = fixedClock,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns stateWithName.viewState
            every { fido2CredentialManager.isUserVerified } returns false
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns createMockPasskeyAttestationOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.REQUIRED,
            )

            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(cipherView = cipherView),
            )

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                ),
            )
            viewModel.trySendAction(VaultAddEditAction.Common.ConfirmOverwriteExistingPasskeyClick)

            coVerify {
                fido2CredentialManager.isUserVerified
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(mockFidoRequest.requestJson)
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
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
            ),
        )
        coEvery { vaultRepository.createCipher(any()) } returns CreateCipherResult.Success
        viewModel.stateFlow.test {
            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)
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
            vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
            dialogState = VaultAddEditState.DialogState.Generic(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.validation_field_required.asText(R.string.name.asText()),
            ),
        )

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = errorState,
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
            ),
        )

        coEvery { vaultRepository.createCipher(any()) } returns CreateCipherResult.Success
        viewModel.stateFlow.test {
            viewModel.trySendAction(VaultAddEditAction.Common.DismissDialog)
            assertEquals(errorState, awaitItem())
            assertEquals(null, awaitItem().dialog)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissFido2ErrorDialogClick should clear the dialog state then complete FIDO 2 create`() =
        runTest {
            val errorState = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                dialogState = VaultAddEditState.DialogState.Fido2Error(
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified.asText(),
                ),
            )
            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = errorState,
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                ),
            )
            viewModel.trySendAction(VaultAddEditAction.Common.Fido2ErrorDialogDismissed)
            viewModel.eventFlow.test {
                assertNull(viewModel.stateFlow.value.dialog)
                assertEquals(
                    VaultAddEditEvent.CompleteFido2Registration(
                        result = Fido2RegisterCredentialResult.Error,
                    ),
                    awaitItem(),
                )
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

        viewModel.trySendAction(action)

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

        viewModel.trySendAction(action)

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

        viewModel.trySendAction(action)

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

        viewModel.trySendAction(action)

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

    @Test
    fun `TypeOptionSelect SSH_KEYS should switch to SshKeysItem`() = runTest {
        mutableVaultDataFlow.value = DataState.Loaded(
            createVaultData(cipherView = createMockCipherView(1)),
        )
        val viewModel = createAddVaultItemViewModel()
        val action = VaultAddEditAction.Common.TypeOptionSelect(
            VaultAddEditState.ItemTypeOption.SSH_KEYS,
        )

        viewModel.trySendAction(action)

        val expectedState = loginInitialState.copy(
            viewState = VaultAddEditState.ViewState.Content(
                common = createCommonContentViewState(),
                isIndividualVaultDisabled = false,
                type = VaultAddEditState.ViewState.Content.ItemType.SshKey(),
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
            viewModel.trySendAction(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `PasswordTextChange should update password in LoginItem`() = runTest {
            val action = VaultAddEditAction.ItemType.LoginType.PasswordTextChange("newPassword")

            viewModel.trySendAction(action)

            val expectedState = createVaultAddItemState(
                typeContentViewState = createLoginTypeContentViewState(
                    password = "newPassword",
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `PasswordVisibilityChange should log an event when in edit mode and password is visible`() =
            runTest {
                val vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = "vault_item_id")
                val viewModel = createAddVaultItemViewModel(
                    savedStateHandle = loginInitialSavedStateHandle.apply {
                        set("state", loginInitialState.copy(vaultAddEditType = vaultAddEditType))
                        set("vault_add_edit_type", vaultAddEditType)
                    },
                )
                viewModel.trySendAction(
                    VaultAddEditAction.ItemType.LoginType.PasswordVisibilityChange(
                        isVisible = true,
                    ),
                )

                verify(exactly = 1) {
                    organizationEventManager.trackEvent(
                        event = OrganizationEvent.CipherClientToggledPasswordVisible(
                            cipherId = "vault_item_id",
                        ),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `OpenUsernameGeneratorClick should emit NavigateToGeneratorModal with username GeneratorMode`() =
            runTest {
                val viewModel = createAddVaultItemViewModel()

                viewModel.eventFlow.test {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LoginType.OpenUsernameGeneratorClick,
                    )
                    assertEquals(
                        VaultAddEditEvent.NavigateToGeneratorModal(
                            GeneratorMode.Modal.Username(website = ""),
                        ),
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
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
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
                viewModel.trySendAction(
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
                    viewModel.trySendAction(
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

            viewModel.trySendAction(
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

            viewModel.trySendAction(
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
                viewModel.trySendAction(
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

        @Test
        fun `UriValueChange should update URI value in state`() = runTest {
            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = createVaultAddItemState(
                        typeContentViewState = createLoginTypeContentViewState(
                            uri = listOf(
                                UriItem(id = "testID", uri = null, match = null, checksum = null),
                            ),
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
                        uri = listOf(
                            UriItem(id = "testID", uri = "Test", match = null, checksum = null),
                        ),
                    ),
                ),
            )

            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.UriValueChange(
                    uriItem = UriItem(id = "testID", uri = "Test", match = null, checksum = null),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `RemoveUriClick should remove URI value in state`() = runTest {
            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = createVaultAddItemState(
                        typeContentViewState = createLoginTypeContentViewState(
                            uri = listOf(
                                UriItem(id = "testID", uri = null, match = null, checksum = null),
                            ),
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
                    uriItem = UriItem(id = "testID", uri = null, match = null, checksum = null),
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
                    uriList = listOf(
                        UriItem(id = "testId", uri = "", match = null, checksum = null),
                        UriItem(id = "testId2", uri = "", match = null, checksum = null),
                    ),
                ),
            )

            assertEquals(
                expectedState,
                viewModel.stateFlow.value,
            )
        }

        @Test
        fun `ClearFido2CredentialClick call should clear the fido2 credential`() {
            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = createVaultAddItemState(
                        typeContentViewState = createLoginTypeContentViewState(
                            fido2CredentialCreationDateTime = R.string.created_xy.asText(
                                "05/08/24",
                                "14:30 PM",
                            ),
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
                        fido2CredentialCreationDateTime = null,
                    ),
                ),
            )

            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.ClearFido2CredentialClick,
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
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

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
            viewModel.trySendAction(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }
    }

    @Nested
    inner class VaultAddEditSshKeyTypeItemActions {
        private lateinit var viewModel: VaultAddEditViewModel
        private lateinit var vaultAddItemInitialState: VaultAddEditState
        private lateinit var sshKeyInitialSavedStateHandle: SavedStateHandle

        @BeforeEach
        fun setup() {
            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(cipherView = createMockCipherView(1)),
            )
            vaultAddItemInitialState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.SshKey(),
            )
            sshKeyInitialSavedStateHandle = createSavedStateHandleWithState(
                state = vaultAddItemInitialState,
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.SSH_KEY),
            )
            viewModel = createAddVaultItemViewModel(
                savedStateHandle = sshKeyInitialSavedStateHandle,
            )
        }

        @Test
        fun `PrivateKeyVisibilityChange should update private key visibility`() = runTest {
            val action = VaultAddEditAction.ItemType.SshKeyType.PrivateKeyVisibilityChange(
                isVisible = true,
            )
            val expectedState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.SshKey(
                    showPrivateKey = true,
                ),
            )
            viewModel.trySendAction(action)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }
    }

    @Test
    fun `NumberVisibilityChange should log an event when in edit mode and password is visible`() =
        runTest {
            val vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = "vault_item_id")
            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = loginInitialSavedStateHandle.apply {
                    set("state", loginInitialState.copy(vaultAddEditType = vaultAddEditType))
                    set("vault_add_edit_type", vaultAddEditType)
                },
            )
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.CardType.NumberVisibilityChange(isVisible = true),
            )

            verify(exactly = 1) {
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientToggledCardNumberVisible(
                        cipherId = "vault_item_id",
                    ),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `SecurityCodeVisibilityChange should log an event when in edit mode and password is visible`() =
        runTest {
            val vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = "vault_item_id")
            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = loginInitialSavedStateHandle.apply {
                    set("state", loginInitialState.copy(vaultAddEditType = vaultAddEditType))
                    set("vault_add_edit_type", vaultAddEditType)
                },
            )
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.CardType.SecurityCodeVisibilityChange(isVisible = true),
            )

            verify(exactly = 1) {
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientToggledCardCodeVisible(
                        cipherId = "vault_item_id",
                    ),
                )
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
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
            )
            viewModel = VaultAddEditViewModel(
                savedStateHandle = secureNotesInitialSavedStateHandle,
                authRepository = authRepository,
                clipboardManager = clipboardManager,
                policyManager = policyManager,
                vaultRepository = vaultRepository,
                fido2CredentialManager = fido2CredentialManager,
                generatorRepository = generatorRepository,
                settingsRepository = settingsRepository,
                specialCircumstanceManager = specialCircumstanceManager,
                resourceManager = resourceManager,
                clock = fixedClock,
                organizationEventManager = organizationEventManager,
                networkConnectionManager = networkConnectionManager,
            )
        }

        @Test
        fun `NameTextChange should update name`() = runTest {
            val action = VaultAddEditAction.Common.NameTextChange("newName")

            viewModel.trySendAction(action)

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

            viewModel.trySendAction(action)

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

            viewModel.trySendAction(action)

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

            viewModel.trySendAction(action)

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

            viewModel.trySendAction(action)

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

            viewModel.trySendAction(action)

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
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
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
        fun `CustomFieldActionSelect with remove action should remove the item`() = runTest {
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
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
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

            viewModel.trySendAction(
                VaultAddEditAction.Common.CustomFieldActionSelect(
                    CustomFieldAction.REMOVE,
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
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
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

            viewModel.trySendAction(
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
                    vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
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

            viewModel.trySendAction(
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

        @Suppress("MaxLineLength")
        @Test
        fun `HiddenFieldVisibilityChange should log an event when in edit mode and password is visible`() =
            runTest {
                val vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = "vault_item_id")
                val viewModel = createAddVaultItemViewModel(
                    savedStateHandle = loginInitialSavedStateHandle.apply {
                        set("state", loginInitialState.copy(vaultAddEditType = vaultAddEditType))
                        set("vault_add_edit_type", vaultAddEditType)
                    },
                )
                viewModel.trySendAction(
                    VaultAddEditAction.Common.HiddenFieldVisibilityChange(isVisible = true),
                )

                verify(exactly = 1) {
                    organizationEventManager.trackEvent(
                        event = OrganizationEvent.CipherClientToggledHiddenFieldVisible(
                            cipherId = "vault_item_id",
                        ),
                    )
                }
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
            viewModel.trySendAction(ownershipChangeAction())

            val action = collectionSelectAction()
            viewModel.trySendAction(action)

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

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationLockout should set isUserVerified to false and display Fido2ErrorDialog`() {
            viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationLockOut)

            verify { fido2CredentialManager.isUserVerified = false }
            assertEquals(
                VaultAddEditState.DialogState.Fido2Error(
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified.asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationCancelled should clear dialog state, set isUserVerified to false, and emit CompleteFido2Create with cancelled result`() =
            runTest {
                viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationCancelled)

                verify { fido2CredentialManager.isUserVerified = false }
                assertNull(viewModel.stateFlow.value.dialog)
                viewModel.eventFlow.test {
                    assertEquals(
                        VaultAddEditEvent.CompleteFido2Registration(
                            result = Fido2RegisterCredentialResult.Cancelled,
                        ),
                        awaitItem(),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationFail should set isUserVerified to false, and display Fido2ErrorDialog`() {
            viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationFail)

            verify { fido2CredentialManager.isUserVerified = false }
            assertEquals(
                VaultAddEditState.DialogState.Fido2Error(
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified.asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationNotSupported should display Fido2ErrorDialog when active account not found`() {
            mutableUserStateFlow.value = null
            viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationNotSupported)
            verify { fido2CredentialManager.isUserVerified = false }
            assertEquals(
                VaultAddEditState.DialogState.Fido2Error(
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified.asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationNotSupported should display Fido2PinPrompt when user has pin unlock enabled`() {
            val userState = createUserState()
            every { settingsRepository.isUnlockWithPinEnabled } returns true
            mutableUserStateFlow.value = userState.copy(
                accounts = listOf(
                    userState.accounts.first().copy(
                        vaultUnlockType = VaultUnlockType.PIN,
                    ),
                ),
            )
            viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationNotSupported)
            verify { fido2CredentialManager.isUserVerified = false }
            assertEquals(
                VaultAddEditState.DialogState.Fido2PinPrompt,
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationNotSupported should display Fido2MasterPasswordPrompt when user has password but no pin`() {
            viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationNotSupported)
            verify { fido2CredentialManager.isUserVerified = false }
            assertEquals(
                VaultAddEditState.DialogState.Fido2MasterPasswordPrompt,
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationNotSupported should display Fido2PinSetUpPrompt when user has no password or pin and vaultUnlockType is MASTER_PASSWORD`() {
            val userState = createUserState()
            mutableUserStateFlow.value = userState.copy(
                accounts = listOf(
                    userState.accounts.first().copy(
                        vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                        trustedDevice = UserState.TrustedDevice(
                            isDeviceTrusted = true,
                            hasAdminApproval = true,
                            hasLoginApprovingDevice = true,
                            hasResetPasswordPermission = true,
                        ),
                        hasMasterPassword = false,
                    ),
                ),
            )

            viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationNotSupported)

            verify { fido2CredentialManager.isUserVerified = false }
            assertEquals(
                VaultAddEditState.DialogState.Fido2PinSetUpPrompt,
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationNotSupported should display Fido2PinSetUpPrompt when user has no password or pin and vaultUnlockType is PIN`() {
            val userState = createUserState()
            mutableUserStateFlow.value = userState.copy(
                accounts = listOf(
                    userState.accounts.first().copy(
                        vaultUnlockType = VaultUnlockType.PIN,
                        trustedDevice = UserState.TrustedDevice(
                            isDeviceTrusted = true,
                            hasAdminApproval = true,
                            hasLoginApprovingDevice = true,
                            hasResetPasswordPermission = true,
                        ),
                        hasMasterPassword = false,
                    ),
                ),
            )

            viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationNotSupported)

            verify { fido2CredentialManager.isUserVerified = false }
            assertEquals(
                VaultAddEditState.DialogState.Fido2PinSetUpPrompt,
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `MasterPasswordFido2VerificationSubmit should display Fido2Error when password verification fails`() {
            val password = "password"
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Error

            viewModel.trySendAction(
                VaultAddEditAction.Common.MasterPasswordFido2VerificationSubmit(
                    password = password,
                ),
            )

            assertEquals(
                VaultAddEditState.DialogState.Fido2Error(
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
            coVerify {
                authRepository.validatePassword(password = password)
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `MasterPasswordFido2VerificationSubmit should display Fido2MasterPasswordError when user has retries remaining`() {
            val password = "password"
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = false)

            viewModel.trySendAction(
                VaultAddEditAction.Common.MasterPasswordFido2VerificationSubmit(
                    password = password,
                ),
            )

            assertEquals(
                VaultAddEditState.DialogState.Fido2MasterPasswordError,
                viewModel.stateFlow.value.dialog,
            )
            coVerify {
                authRepository.validatePassword(password = password)
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `MasterPasswordFido2VerificationSubmit should display Fido2Error when user has no retries remaining`() {
            val password = "password"
            every { fido2CredentialManager.hasAuthenticationAttemptsRemaining() } returns false
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = false)

            viewModel.trySendAction(
                VaultAddEditAction.Common.MasterPasswordFido2VerificationSubmit(
                    password = password,
                ),
            )

            assertEquals(
                VaultAddEditState.DialogState.Fido2Error(
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
            coVerify {
                authRepository.validatePassword(password = password)
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `MasterPasswordFido2VerificationSubmit should register credential when password authenticated successfully`() =
            runTest {
                val password = "password"
                coEvery {
                    authRepository.validatePassword(password = password)
                } returns ValidatePasswordResult.Success(isValid = true)

                viewModel.trySendAction(
                    VaultAddEditAction.Common.MasterPasswordFido2VerificationSubmit(
                        password = password,
                    ),
                )
                coVerify {
                    authRepository.validatePassword(password = password)
                }
            }

        @Test
        fun `RetryFido2PasswordVerificationClick should display Fido2MasterPasswordPrompt`() {
            viewModel.trySendAction(VaultAddEditAction.Common.RetryFido2PasswordVerificationClick)

            assertEquals(
                VaultAddEditState.DialogState.Fido2MasterPasswordPrompt,
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `PinFido2VerificationSubmit should display Fido2Error when Pin verification fails`() {
            val pin = "PIN"
            coEvery {
                authRepository.validatePin(pin = pin)
            } returns ValidatePinResult.Error

            viewModel.trySendAction(
                VaultAddEditAction.Common.PinFido2VerificationSubmit(
                    pin = pin,
                ),
            )

            assertEquals(
                VaultAddEditState.DialogState.Fido2Error(
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
            coVerify {
                authRepository.validatePin(pin = pin)
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `PinFido2VerificationSubmit should display Fido2PinError when user has retries remaining`() {
            val pin = "PIN"
            coEvery {
                authRepository.validatePin(pin = pin)
            } returns ValidatePinResult.Success(isValid = false)

            viewModel.trySendAction(
                VaultAddEditAction.Common.PinFido2VerificationSubmit(
                    pin = pin,
                ),
            )

            assertEquals(
                VaultAddEditState.DialogState.Fido2PinError,
                viewModel.stateFlow.value.dialog,
            )
            coVerify {
                authRepository.validatePin(pin = pin)
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `PinFido2VerificationSubmit should display Fido2Error when user has no retries remaining`() {
            val pin = "PIN"
            every { fido2CredentialManager.hasAuthenticationAttemptsRemaining() } returns false
            coEvery {
                authRepository.validatePin(pin = pin)
            } returns ValidatePinResult.Success(isValid = false)

            viewModel.trySendAction(
                VaultAddEditAction.Common.PinFido2VerificationSubmit(
                    pin = pin,
                ),
            )

            assertEquals(
                VaultAddEditState.DialogState.Fido2Error(
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
            coVerify {
                authRepository.validatePin(pin = pin)
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `PinFido2VerificationSubmit should register credential when pin authenticated successfully`() {
            val pin = "PIN"
            coEvery {
                authRepository.validatePin(pin = pin)
            } returns ValidatePinResult.Success(isValid = true)

            viewModel.trySendAction(
                VaultAddEditAction.Common.PinFido2VerificationSubmit(
                    pin = pin,
                ),
            )
            coVerify {
                authRepository.validatePin(pin = pin)
            }
        }

        @Test
        fun `RetryFido2PinVerificationClick should display Fido2PinPrompt`() {
            viewModel.trySendAction(VaultAddEditAction.Common.RetryFido2PinVerificationClick)

            assertEquals(
                VaultAddEditState.DialogState.Fido2PinPrompt,
                viewModel.stateFlow.value.dialog,
            )
        }

        @Test
        fun `PinFido2SetUpSubmit should display Fido2PinSetUpError for empty PIN`() {
            val pin = ""
            viewModel.trySendAction(VaultAddEditAction.Common.PinFido2SetUpSubmit(pin = pin))

            assertEquals(
                VaultAddEditState.DialogState.Fido2PinSetUpError,
                viewModel.stateFlow.value.dialog,
            )
        }

        @Test
        fun `PinFido2SetUpSubmit should save PIN and register credential for non-empty PIN`() {
            val pin = "PIN"
            every {
                settingsRepository.storeUnlockPin(
                    pin = pin,
                    shouldRequireMasterPasswordOnRestart = false,
                )
            } just runs

            viewModel.trySendAction(VaultAddEditAction.Common.PinFido2SetUpSubmit(pin = pin))

            verify(exactly = 1) {
                settingsRepository.storeUnlockPin(
                    pin = pin,
                    shouldRequireMasterPasswordOnRestart = false,
                )
            }
        }

        @Test
        fun `PinFido2SetUpRetryClick should display Fido2PinSetUpPrompt`() {
            viewModel.trySendAction(VaultAddEditAction.Common.PinFido2SetUpRetryClick)

            assertEquals(
                VaultAddEditState.DialogState.Fido2PinSetUpPrompt,
                viewModel.stateFlow.value.dialog,
            )
        }

        @Test
        fun `DismissFido2VerificationDialogClick should display Fido2ErrorDialog`() {
            viewModel.trySendAction(
                VaultAddEditAction.Common.DismissFido2VerificationDialogClick,
            )

            assertEquals(
                VaultAddEditState.DialogState.Fido2Error(
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationSuccess should display Fido2ErrorDialog when SpecialCircumstance is null`() =
            runTest {
                specialCircumstanceManager.specialCircumstance = null
                coEvery {
                    fido2CredentialManager.registerFido2Credential(
                        any(),
                        any(),
                        any(),
                    )
                } returns Fido2RegisterCredentialResult.Success(
                    registrationResponse = "mockResponse",
                )

                viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationSuccess)

                assertEquals(
                    VaultAddEditState.DialogState.Fido2Error(
                        message = R.string.passkey_operation_failed_because_user_could_not_be_verified.asText(),
                    ),
                    viewModel.stateFlow.value.dialog,
                )
            }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationSuccess should display Fido2ErrorDialog when Fido2Request is null`() =
            runTest {
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.AutofillSave(
                        AutofillSaveItem.Login(
                            username = "mockUsername",
                            password = "mockPassword",
                            uri = "mockUri",
                        ),
                    )
                coEvery {
                    fido2CredentialManager.registerFido2Credential(
                        any(),
                        any(),
                        any(),
                    )
                } returns Fido2RegisterCredentialResult.Success(
                    registrationResponse = "mockResponse",
                )

                viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationSuccess)

                assertEquals(
                    VaultAddEditState.DialogState.Fido2Error(
                        message = R.string.passkey_operation_failed_because_user_could_not_be_verified.asText(),
                    ),
                    viewModel.stateFlow.value.dialog,
                )
            }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationSuccess should display Fido2ErrorDialog when activeUserId is null`() {
            every { authRepository.activeUserId } returns null
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2Save(createMockFido2CredentialRequest(number = 1))

            viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationSuccess)

            assertEquals(
                VaultAddEditState.DialogState.Fido2Error(
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationSuccess should set isUserVerified to true, and register FIDO 2 credential`() =
            runTest {
                val mockRequest = createMockFido2CredentialRequest(number = 1)
                val mockResult = Fido2RegisterCredentialResult.Success(
                    registrationResponse = "mockResponse",
                )
                specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                    fido2CreateCredentialRequest = mockRequest,
                )
                every { authRepository.activeUserId } returns "activeUserId"
                coEvery {
                    fido2CredentialManager.registerFido2Credential(
                        any(),
                        any(),
                        any(),
                    )
                } returns mockResult
                every { fido2CredentialManager.isUserVerified } returns true

                viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationSuccess)

                coVerify {
                    fido2CredentialManager.isUserVerified = true
                    fido2CredentialManager.registerFido2Credential(
                        userId = any(),
                        fido2CreateCredentialRequest = mockRequest,
                        selectedCipherView = any(),
                    )
                }

                viewModel.eventFlow.test {
                    assertEquals(
                        VaultAddEditEvent.ShowToast(R.string.item_updated.asText()),
                        awaitItem(),
                    )
                    assertEquals(
                        VaultAddEditEvent.CompleteFido2Registration(mockResult),
                        awaitItem(),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `Fido2RegisterCredentialResult Error should show toast and emit CompleteFido2Registration result`() =
            runTest {
                val mockRequest = createMockFido2CredentialRequest(number = 1)
                val mockResult = Fido2RegisterCredentialResult.Error
                specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                    fido2CreateCredentialRequest = mockRequest,
                )
                every { authRepository.activeUserId } returns "activeUserId"
                coEvery {
                    fido2CredentialManager.registerFido2Credential(
                        any(),
                        any(),
                        any(),
                    )
                } returns mockResult

                viewModel.trySendAction(
                    VaultAddEditAction.Internal.Fido2RegisterCredentialResultReceive(
                        mockResult,
                    ),
                )

                viewModel.eventFlow.test {
                    assertEquals(
                        VaultAddEditEvent.ShowToast(R.string.an_error_has_occurred.asText()),
                        awaitItem(),
                    )

                    assertEquals(
                        VaultAddEditEvent.CompleteFido2Registration(mockResult),
                        awaitItem(),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `Fido2RegisterCredentialResult Success should show toast and emit CompleteFido2Registration result`() =
            runTest {
                val mockRequest = createMockFido2CredentialRequest(number = 1)
                val mockResult = Fido2RegisterCredentialResult.Success(
                    registrationResponse = "mockResponse",
                )
                specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                    fido2CreateCredentialRequest = mockRequest,
                )
                every { authRepository.activeUserId } returns "activeUserId"
                coEvery {
                    fido2CredentialManager.registerFido2Credential(
                        any(),
                        any(),
                        any(),
                    )
                } returns mockResult

                viewModel.trySendAction(
                    VaultAddEditAction.Internal.Fido2RegisterCredentialResultReceive(
                        mockResult,
                    ),
                )

                viewModel.eventFlow.test {
                    assertEquals(
                        VaultAddEditEvent.ShowToast(R.string.item_updated.asText()),
                        awaitItem(),
                    )

                    assertEquals(
                        VaultAddEditEvent.CompleteFido2Registration(mockResult),
                        awaitItem(),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `Fido2RegisterCredentialResult Cancelled should emit CompleteFido2Registration result`() =
            runTest {
                val mockRequest = createMockFido2CredentialRequest(number = 1)
                val mockResult = Fido2RegisterCredentialResult.Cancelled
                specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                    fido2CreateCredentialRequest = mockRequest,
                )
                every { authRepository.activeUserId } returns "activeUserId"
                coEvery {
                    fido2CredentialManager.registerFido2Credential(
                        any(),
                        any(),
                        any(),
                    )
                } returns mockResult

                viewModel.trySendAction(
                    VaultAddEditAction.Internal.Fido2RegisterCredentialResultReceive(
                        mockResult,
                    ),
                )

                viewModel.eventFlow.test {
                    assertEquals(
                        VaultAddEditEvent.CompleteFido2Registration(mockResult),
                        awaitItem(),
                    )
                }
            }
    }

    //region Helper functions

    @Suppress("MaxLineLength", "LongParameterList")
    private fun createVaultAddItemState(
        vaultAddEditType: VaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
        commonContentViewState: VaultAddEditState.ViewState.Content.Common = createCommonContentViewState(),
        isIndividualVaultDisabled: Boolean = false,
        shouldExitOnSave: Boolean = false,
        typeContentViewState: VaultAddEditState.ViewState.Content.ItemType = createLoginTypeContentViewState(),
        dialogState: VaultAddEditState.DialogState? = null,
        totpData: TotpData? = null,
        supportedItemTypes: List<VaultAddEditState.ItemTypeOption> = VaultAddEditState.ItemTypeOption.entries,
    ): VaultAddEditState =
        VaultAddEditState(
            vaultAddEditType = vaultAddEditType,
            viewState = VaultAddEditState.ViewState.Content(
                common = commonContentViewState,
                isIndividualVaultDisabled = isIndividualVaultDisabled,
                type = typeContentViewState,
            ),
            dialog = dialogState,
            shouldExitOnSave = shouldExitOnSave,
            totpData = totpData,
            supportedItemTypes = supportedItemTypes,
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
        hasOrganizations: Boolean = true,
        canDelete: Boolean = true,
        canAssociateToCollections: Boolean = true,
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
            hasOrganizations = hasOrganizations,
            canDelete = canDelete,
            canAssignToCollections = canAssociateToCollections,
        )

    @Suppress("LongParameterList")
    private fun createLoginTypeContentViewState(
        username: String = "",
        password: String = "",
        uri: List<UriItem> = listOf(
            UriItem(id = "testId", uri = "", match = null, checksum = null),
        ),
        totpCode: String? = null,
        canViewPassword: Boolean = true,
        fido2CredentialCreationDateTime: Text? = null,
    ): VaultAddEditState.ViewState.Content.ItemType.Login =
        VaultAddEditState.ViewState.Content.ItemType.Login(
            username = username,
            password = password,
            uriList = uri,
            totp = totpCode,
            canViewPassword = canViewPassword,
            fido2CredentialCreationDateTime = fido2CredentialCreationDateTime,
        )

    private fun createSavedStateHandleWithState(
        state: VaultAddEditState?,
        vaultAddEditType: VaultAddEditType,
    ) = SavedStateHandle().apply {
        set("state", state)
        set(
            "vault_add_edit_type",
            when (vaultAddEditType) {
                is VaultAddEditType.AddItem -> "add"
                is VaultAddEditType.EditItem -> "edit"
                is VaultAddEditType.CloneItem -> "clone"
            },
        )
        set("vault_edit_id", (vaultAddEditType as? VaultAddEditType.EditItem)?.vaultItemId)
        set(
            "vault_add_item_type",
            when ((vaultAddEditType as? VaultAddEditType.AddItem)
                ?.vaultItemCipherType) {
                VaultItemCipherType.LOGIN -> "login"
                VaultItemCipherType.CARD -> "card"
                VaultItemCipherType.IDENTITY -> "identity"
                VaultItemCipherType.SECURE_NOTE -> "secure_note"
                VaultItemCipherType.SSH_KEY -> "ssh_key"
                null -> null
            },
        )
    }

    @Suppress("LongParameterList")
    private fun createAddVaultItemViewModel(
        savedStateHandle: SavedStateHandle = loginInitialSavedStateHandle,
        bitwardenClipboardManager: BitwardenClipboardManager = clipboardManager,
        vaultRepo: VaultRepository = vaultRepository,
        generatorRepo: GeneratorRepository = generatorRepository,
        bitwardenResourceManager: ResourceManager = resourceManager,
        clock: Clock = fixedClock,
    ): VaultAddEditViewModel =
        VaultAddEditViewModel(
            savedStateHandle = savedStateHandle,
            authRepository = authRepository,
            clipboardManager = bitwardenClipboardManager,
            policyManager = policyManager,
            vaultRepository = vaultRepo,
            fido2CredentialManager = fido2CredentialManager,
            generatorRepository = generatorRepo,
            settingsRepository = settingsRepository,
            specialCircumstanceManager = specialCircumstanceManager,
            resourceManager = bitwardenResourceManager,
            clock = clock,
            organizationEventManager = organizationEventManager,
            networkConnectionManager = networkConnectionManager,
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
                            shouldManageResetPassword = false,
                            shouldUseKeyConnector = false,
                            role = OrganizationType.ADMIN,
                            shouldUsersGetPremium = false,
                        ),
                    ),
                    isBiometricsEnabled = true,
                    vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                    needsMasterPassword = false,
                    trustedDevice = null,
                    hasMasterPassword = true,
                    isUsingKeyConnector = false,
                    onboardingStatus = OnboardingStatus.COMPLETE,
                    firstTimeState = FirstTimeState(showImportLoginsCard = true),
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
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
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

        viewModel.trySendAction(action)

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
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
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

        viewModel.trySendAction(action)
        assertEquals(expectedState, viewModel.stateFlow.value.viewState)
    }
    //endregion Helper functions
}

private const val TEST_ID = "testId"

private const val DEFAULT_EDIT_ITEM_ID: String = "mockId-1"
