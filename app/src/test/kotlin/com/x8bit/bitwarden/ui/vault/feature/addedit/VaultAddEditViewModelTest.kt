package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.core.os.bundleOf
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.toast.ToastManager
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.send.SendView
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.model.TotpData
import com.bitwarden.ui.platform.resource.BitwardenPlurals
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asPluralsText
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.FolderView
import com.bitwarden.vault.UriMatchType
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePinResult
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.credentials.manager.BitwardenCredentialManager
import com.x8bit.bitwarden.data.credentials.model.CreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.credentials.model.UserVerificationRequirement
import com.x8bit.bitwarden.data.credentials.model.createMockCreateCredentialRequest
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.CoachMarkTourType
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.util.FakeGeneratorRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createEditCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createEditExceptPasswordsCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createManageCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockDecryptCipherListResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCipherPermissions
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFido2CredentialList
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createViewCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createViewExceptPasswordsCollectionView
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.CreateFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.credentials.manager.model.CreateCredentialResult
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldType
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.createMockPasskeyAttestationOptions
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toDefaultAddTypeContent
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toViewState
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
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType as UriMatchTypeModel

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
        every { defaultUriMatchType } returns UriMatchTypeModel.EXACT
    }
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(createUserState())
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }

    private val loginInitialState = createVaultAddItemState(
        typeContentViewState = createLoginTypeContentViewState(),
    )
    private val loginInitialSavedStateHandle
        get() = createSavedStateHandleWithState(
            state = loginInitialState,
            vaultAddEditType = VaultAddEditType.AddItem,
            vaultItemCipherType = VaultItemCipherType.LOGIN,
        )

    private val totpTestCodeFlow: MutableSharedFlow<TotpCodeResult> = bufferedMutableSharedFlow()

    private val mutableVaultDataFlow = MutableStateFlow<DataState<VaultData>>(DataState.Loading)
    private val mutableFolderStateFlow = MutableStateFlow<DataState<List<FolderView>>>(
        DataState.Loading,
    )
    private val resourceManager: ResourceManager = mockk {
        every { getString(BitwardenString.folder_none) } returns "No Folder"
    }
    private val clipboardManager: BitwardenClipboardManager = mockk {
        every { setText(text = any<String>(), toastDescriptorOverride = any<Text>()) } just runs
    }
    private val policyManager: PolicyManager = mockk {
        every {
            getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns emptyList()
    }
    private val bitwardenCredentialManager = mockk<BitwardenCredentialManager> {
        every { isUserVerified } returns false
        every { isUserVerified = any() } just runs
        every { authenticationAttempts } returns 0
        every { authenticationAttempts = any() } just runs
        every { hasAuthenticationAttemptsRemaining() } returns true
    }
    private val vaultRepository: VaultRepository = mockk {
        every { vaultDataStateFlow } returns mutableVaultDataFlow
        every { totpCodeFlow } returns totpTestCodeFlow
        every { foldersStateFlow } returns mutableFolderStateFlow
        coEvery {
            getCipher(any())
        } returns GetCipherResult.Success(
            cipherView = createMockCipherView(number = 1),
        )
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

    private val mutableShouldShowAddLoginCoachMarkFlow = MutableStateFlow(false)
    private val firstTimeActionManager = mockk<FirstTimeActionManager> {
        every { markCoachMarkTourCompleted(CoachMarkTourType.ADD_LOGIN) } just runs
        every { shouldShowAddLoginCoachMarkFlow } returns mutableShouldShowAddLoginCoachMarkFlow
    }
    private val mutableSnackbarDataFlow: MutableSharedFlow<BitwardenSnackbarData> =
        bufferedMutableSharedFlow()
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay> = mockk {
        every {
            getSnackbarDataFlow(relay = any(), relays = anyVararg())
        } returns mutableSnackbarDataFlow
        every { sendSnackbarData(data = any(), relay = any()) } just runs
    }
    private val toastManager: ToastManager = mockk {
        every { show(messageId = any(), duration = any()) } just runs
        every { show(message = any(), duration = any()) } just runs
    }

    @BeforeEach
    fun setup() {
        mockkStatic(
            SavedStateHandle::toVaultAddEditArgs,
            CipherView::toViewState,
            UUID::randomUUID,
        )
        mockkObject(ProviderCreateCredentialRequest.Companion)
        every { UUID.randomUUID().toString() } returns TEST_ID
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            SavedStateHandle::toVaultAddEditArgs,
            CipherView::toViewState,
            UUID::randomUUID,
        )
        unmockkObject(ProviderCreateCredentialRequest.Companion)
    }

    @Test
    fun `initial state should be correct when state is null`() = runTest {
        val expectedState = VaultAddEditState(
            vaultAddEditType = VaultAddEditType.AddItem,
            cipherType = VaultItemCipherType.LOGIN,
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                isIndividualVaultDisabled = false,
                type = VaultAddEditState.ViewState.Content.ItemType.Login(),
            ),
            dialog = null,
            bottomSheetState = null,
            totpData = null,
            shouldShowCloseButton = true,
            shouldExitOnSave = false,
            shouldShowCoachMarkTour = false,
            defaultUriMatchType = UriMatchTypeModel.EXACT,
        )
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = null,
                vaultAddEditType = VaultAddEditType.AddItem,
                vaultItemCipherType = VaultItemCipherType.LOGIN,
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
        val initState = createVaultAddItemState()
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = VaultAddEditType.AddItem,
                vaultItemCipherType = VaultItemCipherType.LOGIN,
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
            createMockPolicy(
                organizationId = "Test Org",
                id = "testId",
                type = PolicyTypeJson.PERSONAL_OWNERSHIP,
                isEnabled = true,
                data = null,
            ),
        )
        val vaultAddEditType = VaultAddEditType.AddItem
        val vaultItemCipherType = VaultItemCipherType.LOGIN
        mutableVaultDataFlow.value = DataState.Loaded(
            data = createVaultData(),
        )
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = null,
                vaultAddEditType = vaultAddEditType,
                vaultItemCipherType = vaultItemCipherType,
            ),
        )
        assertEquals(
            VaultAddEditState(
                vaultAddEditType = vaultAddEditType,
                cipherType = vaultItemCipherType,
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
                bottomSheetState = null,
                shouldShowCoachMarkTour = false,
                defaultUriMatchType = UriMatchTypeModel.EXACT,
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
        val vaultAddEditType = VaultAddEditType.AddItem
        val vaultItemCipherType = VaultItemCipherType.LOGIN
        val initState = createVaultAddItemState(
            vaultAddEditType = vaultAddEditType,
            commonContentViewState = autofillContentState.common,
            typeContentViewState = autofillContentState.type,
        )
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = vaultAddEditType,
                vaultItemCipherType = vaultItemCipherType,
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
        val vaultItemCipherType = VaultItemCipherType.LOGIN
        val initState = createVaultAddItemState(
            vaultAddEditType = vaultAddEditType,
            commonContentViewState = autofillContentState.common,
            typeContentViewState = autofillContentState.type,
        )
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = vaultAddEditType,
                vaultItemCipherType = vaultItemCipherType,
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
        every {
            ProviderCreateCredentialRequest.fromBundle(any())
        } returns mockk(relaxed = true)
        val createCredentialRequest = CreateCredentialRequest(
            userId = "mockUserId-1",
            isUserPreVerified = false,
            requestData = bundleOf(),
        )
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.ProviderCreateCredential(
                createCredentialRequest = createCredentialRequest,
            )
        val fido2ContentState = createCredentialRequest.toDefaultAddTypeContent(
            attestationOptions = createMockPasskeyAttestationOptions(number = 1),
            isIndividualVaultDisabled = false,
        )
        val vaultAddEditType = VaultAddEditType.AddItem
        val vaultItemCipherType = VaultItemCipherType.LOGIN
        val initState = createVaultAddItemState(
            vaultAddEditType = vaultAddEditType,
            commonContentViewState = fido2ContentState.common,
            typeContentViewState = fido2ContentState.type,
            createCredentialRequest = createCredentialRequest,
        )

        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = vaultAddEditType,
                vaultItemCipherType = vaultItemCipherType,
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
                vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                vaultItemCipherType = VaultItemCipherType.LOGIN,
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
    fun `snackbar relay emission should send ShowSnackbar`() = runTest {
        val viewModel = createAddVaultItemViewModel()
        val snackbarData = mockk<BitwardenSnackbarData>()
        viewModel.eventFlow.test {
            mutableSnackbarDataFlow.emit(snackbarData)
            assertEquals(VaultAddEditEvent.ShowSnackbar(snackbarData), awaitItem())
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
                vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                vaultItemCipherType = VaultItemCipherType.LOGIN,
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
    fun `ConfirmDeleteClick with DeleteCipherResult Success should emit send snackbar event and NavigateBack`() =
        runTest {
            val cipherListView = createMockCipherListView(number = 1)
            val cipherView = createMockCipherView(number = 1)
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val initState = createVaultAddItemState(vaultAddEditType = vaultAddEditType)
            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(cipherListView = cipherListView),
            )
            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = initState,
                    vaultAddEditType = vaultAddEditType,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                    VaultAddEditEvent.NavigateBack,
                    awaitItem(),
                )
            }
            verify(exactly = 1) {
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(BitwardenString.item_soft_deleted.asText()),
                    relay = SnackbarRelay.CIPHER_DELETED_SOFT,
                )
            }
        }

    @Test
    fun `ConfirmDeleteClick with DeleteCipherResult Failure should show generic error`() =
        runTest {
            val cipherListView = createMockCipherListView(number = 1)
            val cipherView = createMockCipherView(number = 1)
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val initState = createVaultAddItemState(vaultAddEditType = vaultAddEditType)
            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(cipherListView = cipherListView),
            )

            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = initState,
                    vaultAddEditType = vaultAddEditType,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            )

            val error = Throwable("Oh dang.")
            coEvery {
                vaultRepository.softDeleteCipher(
                    cipherId = "mockId-1",
                    cipherView = cipherView,
                )
            } returns DeleteCipherResult.Error(error = error)

            viewModel.trySendAction(VaultAddEditAction.Common.ConfirmDeleteClick)

            assertEquals(
                createVaultAddItemState(
                    vaultAddEditType = vaultAddEditType,
                    dialogState = VaultAddEditState.DialogState.Generic(
                        message = BitwardenString.generic_error_message.asText(),
                        error = error,
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
                dialogState = VaultAddEditState.DialogState.Loading(
                    BitwardenString.saving.asText(),
                ),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
            val stateWithName = createVaultAddItemState(
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
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                    VaultAddEditEvent.NavigateBack,
                    eventFlow.awaitItem(),
                )
            }
            verify(exactly = 1) {
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(BitwardenString.new_item_created.asText()),
                    relay = SnackbarRelay.CIPHER_CREATED,
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
                dialogState = VaultAddEditState.DialogState.Loading(
                    BitwardenString.saving.asText(),
                ),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
                .copy(shouldExitOnSave = true)
            val stateWithName = createVaultAddItemState(
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
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                dialogState = VaultAddEditState.DialogState.Loading(BitwardenString.saving.asText()),
                commonContentViewState = createCommonContentViewState(name = "issuer"),
                totpData = totpData,
                shouldExitOnSave = true,
            )
            val stateWithName = createVaultAddItemState(
                commonContentViewState = createCommonContentViewState(name = "issuer"),
                totpData = totpData,
                shouldExitOnSave = true,
            )
            mutableVaultDataFlow.value = DataState.Loaded(createVaultData())
            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
    fun `in add mode during autofill selection, SaveClick should show dialog, remove it once an item is saved, show a toast and navigate back not clearing special circumstances`() =
        runTest {
            val autofillData = AutofillSelectionData(
                type = AutofillSelectionData.Type.LOGIN,
                framework = AutofillSelectionData.Framework.AUTOFILL,
                uri = "mockUri",
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AutofillSelection(
                    autofillSelectionData = autofillData,
                    shouldFinishWhenComplete = true,
                )
            val stateWithDialog = createVaultAddItemState(
                dialogState = VaultAddEditState.DialogState.Loading(BitwardenString.saving.asText()),
                commonContentViewState = createCommonContentViewState(name = "issuer"),
                shouldExitOnSave = false,
                shouldClearSpecialCircumstance = false,
            )
            val stateWithName = createVaultAddItemState(
                commonContentViewState = createCommonContentViewState(name = "issuer"),
                shouldExitOnSave = false,
                shouldClearSpecialCircumstance = false,
            )
            mutableVaultDataFlow.value = DataState.Loaded(createVaultData())
            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                assertEquals(VaultAddEditEvent.NavigateBack, eventTurbine.awaitItem())
            }
            assertNotNull(specialCircumstanceManager.specialCircumstance)
            verify(exactly = 1) {
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(BitwardenString.new_item_created.asText()),
                    relay = SnackbarRelay.CIPHER_CREATED,
                )
            }
            coVerify(exactly = 1) {
                vaultRepository.createCipherInOrganization(any(), any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode during fido2 registration, SaveClick should show saving dialog, and request user verification when required`() =
        runTest {
            val createCredentialRequest = CreateCredentialRequest(
                userId = "mockUserId",
                isUserPreVerified = false,
                requestData = bundleOf(),
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = createCredentialRequest,
                )
            val stateWithSavingDialog = createVaultAddItemState(
                dialogState = VaultAddEditState.DialogState.Loading(
                    BitwardenString.saving.asText(),
                ),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
                createCredentialRequest = createCredentialRequest,
            )
                .copy(shouldExitOnSave = true)

            val stateWithNewLogin = createVaultAddItemState(
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
                createCredentialRequest = createCredentialRequest,
            )
                .copy(shouldExitOnSave = true)

            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(),
            )

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithNewLogin,
                    vaultAddEditType = VaultAddEditType.AddItem,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            )

            val mockCreatePublicKeyCredentialRequest =
                mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
            setupFido2CreateRequest(
                mockCreatePublicKeyCredentialRequest = mockCreatePublicKeyCredentialRequest,
            )
            coEvery {
                bitwardenCredentialManager.registerFido2Credential(
                    userId = "mockUserId",
                    selectedCipherView = any(),
                    createPublicKeyCredentialRequest = mockCreatePublicKeyCredentialRequest,
                    callingAppInfo = any(),
                )
            } returns Fido2RegisterCredentialResult.Success("mockRegistrationResponse")
            every {
                bitwardenCredentialManager.getUserVerificationRequirement(
                    request = mockCreatePublicKeyCredentialRequest,
                )
            } returns UserVerificationRequirement.REQUIRED
            every { authRepository.activeUserId } returns "mockUserId"
            coEvery {
                vaultRepository.createCipherInOrganization(any(), any())
            } returns CreateCipherResult.Success

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
            val createCredentialRequest = CreateCredentialRequest(
                userId = mockUserId,
                isUserPreVerified = false,
                requestData = bundleOf(),
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = createCredentialRequest,
                )
            val stateWithSavingDialog = createVaultAddItemState(
                dialogState = VaultAddEditState.DialogState.Loading(
                    BitwardenString.saving.asText(),
                ),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
                createCredentialRequest = createCredentialRequest,
            )
                .copy(shouldExitOnSave = true)

            val stateWithName = createVaultAddItemState(
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
                createCredentialRequest = createCredentialRequest,
            )
                .copy(shouldExitOnSave = true)

            val mockCreatePublicKeyCredentialRequest =
                mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
            setupFido2CreateRequest(
                mockCreatePublicKeyCredentialRequest = mockCreatePublicKeyCredentialRequest,
            )

            every {
                bitwardenCredentialManager.getUserVerificationRequirement(
                    mockCreatePublicKeyCredentialRequest,
                )
            } returns UserVerificationRequirement.DISCOURAGED
            coEvery {
                bitwardenCredentialManager.registerFido2Credential(
                    userId = "mockUserId",
                    selectedCipherView = any(),
                    createPublicKeyCredentialRequest = mockCreatePublicKeyCredentialRequest,
                    callingAppInfo = any(),
                )
            } returns Fido2RegisterCredentialResult.Success(responseJson = "mockResponse")
            every { authRepository.activeUserId } returns mockUserId
            coEvery {
                vaultRepository.createCipherInOrganization(any(), any())
            } returns CreateCipherResult.Success

            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(),
            )
            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            )

            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

                assertEquals(stateWithName, stateFlow.awaitItem())
                assertEquals(stateWithSavingDialog, stateFlow.awaitItem())
                assertEquals(stateWithName, stateFlow.awaitItem())
                assertEquals(
                    VaultAddEditEvent.CompleteCredentialRegistration(
                        CreateCredentialResult.Success.Fido2CredentialRegistered(
                            responseJson = "mockResponse",
                        ),
                    ),
                    eventFlow.awaitItem(),
                )
                verify(exactly = 1) {
                    toastManager.show(messageId = BitwardenString.item_updated)
                }
                coVerify(exactly = 1) {
                    bitwardenCredentialManager.registerFido2Credential(
                        userId = mockUserId,
                        selectedCipherView = any(),
                        createPublicKeyCredentialRequest = mockCreatePublicKeyCredentialRequest,
                        callingAppInfo = any(),
                    )
                }
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode during fido2, SaveClick should skip user verification when user is verified`() =
        runTest {
            val fido2CredentialRequest = createMockCreateCredentialRequest(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = fido2CredentialRequest,
                )
            val stateWithName = createVaultAddItemState(
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
                createCredentialRequest = fido2CredentialRequest,
            )
                .copy(shouldExitOnSave = true)

            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(),
            )
            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            )

            val mockCreatePublicKeyCredentialRequest =
                mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
            setupFido2CreateRequest(
                mockCreatePublicKeyCredentialRequest = mockCreatePublicKeyCredentialRequest,
            )

            every {
                bitwardenCredentialManager.getUserVerificationRequirement(
                    mockCreatePublicKeyCredentialRequest,
                )
            } returns UserVerificationRequirement.DISCOURAGED
            coEvery {
                bitwardenCredentialManager.registerFido2Credential(
                    userId = "mockUserId",
                    selectedCipherView = any(),
                    createPublicKeyCredentialRequest = mockCreatePublicKeyCredentialRequest,
                    callingAppInfo = any(),
                )
            } returns Fido2RegisterCredentialResult.Success(responseJson = "mockResponse")
            every { authRepository.activeUserId } returns fido2CredentialRequest.userId
            every { bitwardenCredentialManager.isUserVerified } returns true
            coEvery {
                bitwardenCredentialManager.registerFido2Credential(
                    userId = fido2CredentialRequest.userId,
                    createPublicKeyCredentialRequest = mockCreatePublicKeyCredentialRequest,
                    selectedCipherView = any(),
                    callingAppInfo = any(),
                )
            } returns Fido2RegisterCredentialResult.Success(responseJson = "mockResponse")
            coEvery {
                vaultRepository.createCipherInOrganization(any(), any())
            } returns CreateCipherResult.Success

            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

            coVerify {
                bitwardenCredentialManager.registerFido2Credential(
                    userId = fido2CredentialRequest.userId,
                    createPublicKeyCredentialRequest = mockCreatePublicKeyCredentialRequest,
                    selectedCipherView = any(),
                    callingAppInfo = any(),
                )
            }

            verify(exactly = 0) {
                bitwardenCredentialManager.getPasskeyAttestationOptionsOrNull(any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode during fido2, SaveClick should emit fido user verification as optional when verification is PREFERRED`() =
        runTest {
            val fido2CredentialRequest = createMockCreateCredentialRequest(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = fido2CredentialRequest,
                )
            val stateWithName = createVaultAddItemState(
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
                createCredentialRequest = fido2CredentialRequest,
            )
                .copy(shouldExitOnSave = true)

            setupFido2CreateRequest()
            every {
                bitwardenCredentialManager.getUserVerificationRequirement(
                    request = any<CreatePublicKeyCredentialRequest>(),
                )
            } returns UserVerificationRequirement.PREFERRED

            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(),
            )

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
            val fido2CredentialRequest = createMockCreateCredentialRequest(number = 1)
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = fido2CredentialRequest,
                )
            val stateWithName = createVaultAddItemState(
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
                createCredentialRequest = fido2CredentialRequest,
            )
                .copy(shouldExitOnSave = true)

            setupFido2CreateRequest()

            every {
                bitwardenCredentialManager.getUserVerificationRequirement(
                    request = any<CreatePublicKeyCredentialRequest>(),
                )
            } returns UserVerificationRequirement.REQUIRED

            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(),
            )

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
    @Suppress("MaxLineLength")
    fun `in add mode, createCipherInOrganization success should send snackbar event and NavigateBack`() =
        runTest {
            val stateWithName = createVaultAddItemState(
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )

            mutableVaultDataFlow.value = DataState.Loaded(createVaultData())

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            )

            coEvery {
                vaultRepository.createCipherInOrganization(any(), any())
            } returns CreateCipherResult.Success
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)
                assertEquals(VaultAddEditEvent.NavigateBack, awaitItem())
            }
            verify(exactly = 1) {
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(BitwardenString.new_item_created.asText()),
                    relay = SnackbarRelay.CIPHER_CREATED,
                )
            }
        }

    @Test
    fun `in edit mode, canDelete should be true when cipher permission is true`() =
        runTest {
            val cipherListView = createMockCipherListView(number = 1)
                .copy(
                    permissions = createMockSdkCipherPermissions(delete = true, restore = false),
                )
            val cipherView = createMockCipherView(1)
                .copy(
                    permissions = createMockSdkCipherPermissions(
                        delete = true,
                        restore = false,
                    ),
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
                    canDelete = true,
                ),
            )

            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.Success(cipherView)
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
                    cipherListView = cipherListView,
                    collectionViewList = listOf(
                        createEditCollectionView(number = 1),
                    ),
                ),
            )

            createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
    fun `in edit mode, canDelete should be true when cipher is in a collection the user can manage`() =
        runTest {
            val cipherListView =
                createMockCipherListView(number = 1, collectionIds = listOf("mockId-1", "mockId-2"))
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

            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.Success(cipherView)
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
                    cipherListView = cipherListView,
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
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
    fun `in edit mode, canAssociateToCollections should be false when cipher is in a collection with manage permission and a collection with edit, except password permission`() =
        runTest {
            val cipherListView = createMockCipherListView(1)
                .copy(collectionIds = listOf("mockId-1", "mockId-2"))
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

            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.Success(cipherView)
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
                    cipherListView = cipherListView,
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
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
    fun `in edit mode, canAssociateToCollections should be false when cipher is in a collection with manage permission and a collection with view, except password permission`() =
        runTest {
            val cipherListView = createMockCipherListView(1)
                .copy(collectionIds = listOf("mockId-1", "mockId-2"))
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

            coEvery {
                vaultRepository.getCipher("mockId-1")
            } returns GetCipherResult.Success(cipherView)
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
                    cipherListView = cipherListView,
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
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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

    @Test
    fun `in edit mode, updateCipher success should send snackbar event and NavigateBack`() =
        runTest {
            val cipherView = createMockCipherListView(1)
            val stateWithName = createVaultAddItemState(
                vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID),
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )

            mutableVaultDataFlow.value =
                DataState.Loaded(createVaultData(cipherListView = cipherView))

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            )

            coEvery {
                vaultRepository.updateCipher(any(), any())
            } returns UpdateCipherResult.Success
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)
                assertEquals(VaultAddEditEvent.NavigateBack, awaitItem())
            }
            verify(exactly = 1) {
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(BitwardenString.item_updated.asText()),
                    relay = SnackbarRelay.CIPHER_UPDATED,
                )
            }
        }

    @Test
    fun `in add mode, SaveClick with no network connection error should show error dialog`() =
        runTest {
            val stateWithName = createVaultAddItemState(
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
            mutableVaultDataFlow.value = DataState.Loaded(createVaultData())
            every { networkConnectionManager.isNetworkConnected } returns false

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            )

            coEvery {
                vaultRepository.createCipherInOrganization(any(), any())
            } returns CreateCipherResult.Error(errorMessage = null, error = Throwable("Oh dang"))
            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

            assertEquals(
                stateWithName.copy(
                    dialog = VaultAddEditState.DialogState.Generic(
                        title = BitwardenString.internet_connection_required_title.asText(),
                        message = BitwardenString.internet_connection_required_message.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `in edit mode, SaveClick should show network error message in dialog when not null`() =
        runTest {
            val stateWithName = createVaultAddItemState(
                commonContentViewState = createCommonContentViewState(
                    name = "mockName-1",
                ),
            )
            mutableVaultDataFlow.value = DataState.Loaded(createVaultData())

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            )

            coEvery {
                vaultRepository.createCipherInOrganization(any(), any())
            } returns CreateCipherResult.Error(
                errorMessage = "Network error message",
                error = null,
            )
            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

            assertEquals(
                stateWithName.copy(
                    dialog = VaultAddEditState.DialogState.Generic(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = "Network error message".asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )

            // Verify default error message is shown when errorMessage is null
            coEvery {
                vaultRepository.createCipherInOrganization(any(), any())
            } returns CreateCipherResult.Error(
                errorMessage = null,
                error = null,
            )
            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)
            assertEquals(
                stateWithName.copy(
                    dialog = VaultAddEditState.DialogState.Generic(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `in edit mode, SaveClick should show dialog, and remove it once an item is saved`() =
        runTest {
            val cipherListView = createMockCipherListView(1)
            val cipherView = createMockCipherView(1)
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val stateWithDialog = createVaultAddItemState(
                vaultAddEditType = vaultAddEditType,
                dialogState = VaultAddEditState.DialogState.Loading(
                    BitwardenString.saving.asText(),
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
                data = createVaultData(cipherListView = cipherListView),
            )

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
            val cipherListView = createMockCipherListView(1)
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
            val error = Throwable("Oh dang.")
            coEvery {
                vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
            } returns UpdateCipherResult.Error(errorMessage = null, error = error)
            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(cipherListView = cipherListView),
            )

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            )

            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

            assertEquals(
                stateWithName.copy(
                    dialog = VaultAddEditState.DialogState.Generic(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.generic_error_message.asText(),
                        error = error,
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
            val cipherListView = createMockCipherListView(1)
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
            } returns UpdateCipherResult.Error(errorMessage = errorMessage, error = null)
            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(cipherListView = cipherListView),
            )

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            )

            viewModel.trySendAction(VaultAddEditAction.Common.SaveClick)

            assertEquals(
                stateWithName.copy(
                    dialog = VaultAddEditState.DialogState.Generic(
                        title = BitwardenString.an_error_has_occurred.asText(),
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
            val cipherListView = createMockCipherListView(1)
            val cipherView = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            val mockFido2CredentialRequest = createMockCreateCredentialRequest(number = 1)
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val stateWithName = createVaultAddItemState(
                commonContentViewState = createCommonContentViewState(
                    name = cipherView.name,
                    originalCipher = cipherView,
                ),
                typeContentViewState = createLoginTypeContentViewState(
                    fido2CredentialCreationDateTime = BitwardenString.created_x.asText(
                        "May 08, 2024, 4:30 PM",
                    ),
                ),
                createCredentialRequest = mockFido2CredentialRequest,
            )

            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = mockFido2CredentialRequest,
                )

            setupFido2CreateRequest()

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
                createVaultData(cipherListView = cipherListView),
            )

            val viewModel = createAddVaultItemViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
        val cipherListView = createMockCipherListView(1)
        val cipherView = createMockCipherView(
            number = 1,
            fido2Credentials = createMockSdkFido2CredentialList(number = 1),
        )
        val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
        val mockFidoRequest = createMockCreateCredentialRequest(number = 1)
        val stateWithName = createVaultAddItemState(
            vaultAddEditType = vaultAddEditType,
            commonContentViewState = createCommonContentViewState(
                name = "mockName-1",
                originalCipher = cipherView,
                notes = "mockNotes-1",
            ),
            createCredentialRequest = mockFidoRequest,
        )
        val mockCallingAppInfo = mockk<CallingAppInfo>(relaxed = true)
        val mockCreatePublicKeyCredentialRequest =
            mockk<CreatePublicKeyCredentialRequest>(relaxed = true)
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.ProviderCreateCredential(
                createCredentialRequest = mockFidoRequest,
            )
        setupFido2CreateRequest(
            mockCallingAppInfo = mockCallingAppInfo,
            mockCreatePublicKeyCredentialRequest = mockCreatePublicKeyCredentialRequest,
        )
        coEvery {
            bitwardenCredentialManager.registerFido2Credential(
                userId = mockFidoRequest.userId,
                createPublicKeyCredentialRequest = mockCreatePublicKeyCredentialRequest,
                callingAppInfo = mockCallingAppInfo,
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
        every { bitwardenCredentialManager.isUserVerified } returns true

        mutableVaultDataFlow.value = DataState.Loaded(
            createVaultData(cipherListView = cipherListView),
        )

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = stateWithName,
                vaultAddEditType = vaultAddEditType,
                vaultItemCipherType = VaultItemCipherType.LOGIN,
            ),
        )
        viewModel.trySendAction(VaultAddEditAction.Common.ConfirmOverwriteExistingPasskeyClick)

        coVerify {
            bitwardenCredentialManager.isUserVerified
            bitwardenCredentialManager.registerFido2Credential(
                userId = mockFidoRequest.userId,
                createPublicKeyCredentialRequest = mockCreatePublicKeyCredentialRequest,
                callingAppInfo = mockCallingAppInfo,
                selectedCipherView = any(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmOverwriteExistingPasskeyClick should check if user verification is required`() =
        runTest {
            val cipherListView = createMockCipherListView(1)
            val cipherView = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            val mockFidoRequest = createMockCreateCredentialRequest(number = 1)
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
                createCredentialRequest = mockFidoRequest,
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createCredentialRequest = mockFidoRequest,
                )

            setupFido2CreateRequest()

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
            every { bitwardenCredentialManager.isUserVerified } returns false
            every {
                bitwardenCredentialManager.getUserVerificationRequirement(
                    any<CreatePublicKeyCredentialRequest>(),
                )
            } returns UserVerificationRequirement.REQUIRED

            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(cipherListView = cipherListView),
            )

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            )
            viewModel.trySendAction(VaultAddEditAction.Common.ConfirmOverwriteExistingPasskeyClick)

            verify {
                bitwardenCredentialManager.isUserVerified
            }
        }

    @Test
    fun `Saving item with an empty name field will cause a dialog to show up`() = runTest {
        mutableVaultDataFlow.value = DataState.Loaded(
            createVaultData(cipherListView = createMockCipherListView(1)),
        )
        val stateWithNoName = createVaultAddItemState(
            commonContentViewState = createCommonContentViewState(name = ""),
        )

        val stateWithNoNameAndDialog = createVaultAddItemState(
            commonContentViewState = createCommonContentViewState(name = ""),
            dialogState = VaultAddEditState.DialogState.Generic(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString.validation_field_required
                    .asText(BitwardenString.name.asText()),
            ),
        )

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = stateWithNoName,
                vaultAddEditType = VaultAddEditType.AddItem,
                vaultItemCipherType = VaultItemCipherType.LOGIN,
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
            createVaultData(cipherListView = createMockCipherListView(1)),
        )
        val errorState = createVaultAddItemState(
            dialogState = VaultAddEditState.DialogState.Generic(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString.validation_field_required
                    .asText(BitwardenString.name.asText()),
            ),
        )

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = errorState,
                vaultAddEditType = VaultAddEditType.AddItem,
                vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                dialogState = VaultAddEditState.DialogState.CredentialError(
                    message = BitwardenString.passkey_operation_failed_because_user_could_not_be_verified.asText(),
                ),
            )
            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = errorState,
                    vaultAddEditType = VaultAddEditType.AddItem,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            )
            viewModel.trySendAction(
                VaultAddEditAction.Common.CredentialErrorDialogDismissed(
                    BitwardenString.passkey_operation_failed_because_user_could_not_be_verified.asText(),
                ),
            )
            viewModel.eventFlow.test {
                assertNull(viewModel.stateFlow.value.dialog)
                assertEquals(
                    VaultAddEditEvent.CompleteCredentialRegistration(
                        result = CreateCredentialResult.Error(
                            BitwardenString.passkey_operation_failed_because_user_could_not_be_verified
                                .asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Nested
    inner class VaultAddEditLoginTypeItemActions {
        private lateinit var viewModel: VaultAddEditViewModel

        @BeforeEach
        fun setup() {
            mutableVaultDataFlow.value = DataState.Loaded(
                createVaultData(cipherListView = createMockCipherListView(1)),
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
            val cipherView = createMockCipherListView(1)
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
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            )

            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(
                    cipherListView = cipherView,
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
                            label = BitwardenString.loading.asText(),
                        ),
                    ),
                    awaitItem(),
                )

                assertEquals(
                    loginState.copy(
                        dialog = VaultAddEditState.DialogState.Generic(
                            message = BitwardenPlurals.password_exposed.asPluralsText(
                                quantity = breachCount,
                                args = arrayOf(breachCount),
                            ),
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
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.CopyTotpKeyClick(
                    testKey,
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = testKey,
                    toastDescriptorOverride = BitwardenString.authenticator_key.asText(),
                )
            }
        }

        @Test
        fun `Authenticator TooltipCLick emits NavigateToAuthenticatorKeyTooltipUri`() = runTest {
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    VaultAddEditAction.ItemType.LoginType.AuthenticatorHelpToolTipClick,
                )
                assertEquals(
                    VaultAddEditEvent.NavigateToAuthenticatorKeyTooltipUri,
                    awaitItem(),
                )
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
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                    VaultAddEditEvent.ShowSnackbar(
                        message = BitwardenString.authenticator_key_added.asText(),
                    ),
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
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                            fido2CredentialCreationDateTime = BitwardenString.created_x.asText(
                                "May 08, 2024, 4:30 PM",
                            ),
                        ),
                    ),
                    vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID),
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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

    @Suppress("MaxLineLength")
    @Test
    fun `when first time action manager should show logins tour value updates to false shouldShowLearnAboutNewLogins should update to false`() {
        mutableShouldShowAddLoginCoachMarkFlow.update { true }
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = createVaultAddItemState(
                    typeContentViewState = createLoginTypeContentViewState(),
                ),
                vaultAddEditType = VaultAddEditType.AddItem,
                vaultItemCipherType = VaultItemCipherType.LOGIN,
            ),
        )
        assertTrue(viewModel.stateFlow.value.shouldShowLearnAboutNewLogins)
        mutableShouldShowAddLoginCoachMarkFlow.update { false }
        assertFalse(viewModel.stateFlow.value.shouldShowLearnAboutNewLogins)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when first time action manager value is false, but edit type is EditItem shouldShowLearnAboutNewLogins should be false`() {
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = createVaultAddItemState(
                    vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = "1234"),
                    typeContentViewState = createLoginTypeContentViewState(),
                ),
                vaultAddEditType = VaultAddEditType.EditItem(
                    vaultItemId = "1234",
                ),
                vaultItemCipherType = VaultItemCipherType.LOGIN,
            ),
        )
        assertFalse(viewModel.stateFlow.value.shouldShowLearnAboutNewLogins)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `LearnAboutLoginsDismissed action calls first time action manager hasSeenAddLoginCoachMarkTour called`() {
        val viewModel = createAddVaultItemViewModel()

        viewModel.trySendAction(VaultAddEditAction.ItemType.LoginType.LearnAboutLoginsDismissed)
        verify(exactly = 1) {
            firstTimeActionManager.markCoachMarkTourCompleted(CoachMarkTourType.ADD_LOGIN)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `StartLearnAboutLogins action calls first time action manager hasSeenAddLoginCoachMarkTour called and show coach mark event sent`() =
        runTest {
            val viewModel = createAddVaultItemViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAddEditAction.ItemType.LoginType.StartLearnAboutLogins)
                assertEquals(VaultAddEditEvent.StartAddLoginItemCoachMarkTour, awaitItem())
            }
            verify(exactly = 1) {
                firstTimeActionManager.markCoachMarkTourCompleted(CoachMarkTourType.ADD_LOGIN)
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
                createVaultData(cipherListView = createMockCipherListView(1)),
            )
            vaultAddItemInitialState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Identity(),
            )
            identityInitialSavedStateHandle = createSavedStateHandleWithState(
                state = vaultAddItemInitialState,
                vaultAddEditType = VaultAddEditType.AddItem,
                vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                createVaultData(cipherListView = createMockCipherListView(1)),
            )
            vaultAddItemInitialState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.Card(),
            )
            identityInitialSavedStateHandle = createSavedStateHandleWithState(
                state = vaultAddItemInitialState,
                vaultAddEditType = VaultAddEditType.AddItem,
                vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                createVaultData(cipherListView = createMockCipherListView(1)),
            )
            vaultAddItemInitialState = createVaultAddItemState(
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.SshKey(),
            )
            sshKeyInitialSavedStateHandle = createSavedStateHandleWithState(
                state = vaultAddItemInitialState,
                vaultAddEditType = VaultAddEditType.AddItem,
                vaultItemCipherType = VaultItemCipherType.SSH_KEY,
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
                createVaultData(cipherListView = createMockCipherListView(1)),
            )
            vaultAddItemInitialState = createVaultAddItemState()
            secureNotesInitialSavedStateHandle = createSavedStateHandleWithState(
                state = vaultAddItemInitialState,
                vaultAddEditType = VaultAddEditType.AddItem,
                vaultItemCipherType = VaultItemCipherType.LOGIN,
            )
            viewModel = VaultAddEditViewModel(
                savedStateHandle = secureNotesInitialSavedStateHandle,
                authRepository = authRepository,
                clipboardManager = clipboardManager,
                policyManager = policyManager,
                vaultRepository = vaultRepository,
                bitwardenCredentialManager = bitwardenCredentialManager,
                generatorRepository = generatorRepository,
                settingsRepository = settingsRepository,
                snackbarRelayManager = snackbarRelayManager,
                toastManager = toastManager,
                specialCircumstanceManager = specialCircumstanceManager,
                resourceManager = resourceManager,
                clock = fixedClock,
                organizationEventManager = organizationEventManager,
                networkConnectionManager = networkConnectionManager,
                firstTimeActionManager = firstTimeActionManager,
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
                folderId = "mockId-1",
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
        fun `SelectOrAddFolderFoItem should update state to show bottom sheet`() = runTest {
            val action = VaultAddEditAction.Common.SelectOrAddFolderForItem

            viewModel.trySendAction(action)

            val expectedState = vaultAddItemInitialState.copy(
                bottomSheetState = VaultAddEditState.BottomSheetState.FolderSelection,
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `DismissFolderSelectionBottomSheet should update state to hide bottom sheet`() =
            runTest {
                val action = VaultAddEditAction.Common.DismissBottomSheet

                viewModel.trySendAction(VaultAddEditAction.Common.SelectOrAddFolderForItem)

                assertEquals(
                    vaultAddItemInitialState.copy(
                        bottomSheetState = VaultAddEditState.BottomSheetState.FolderSelection,
                    ),
                    viewModel.stateFlow.value,
                )
                val expectedState = vaultAddItemInitialState.copy(
                    bottomSheetState = null,
                )
                viewModel.trySendAction(action)
                assertEquals(
                    expectedState,
                    viewModel.stateFlow.value,
                )
            }

        @Test
        fun `AddNewFolder action calls create folder from vault repository`() = runTest {
            val folderName = "folderName"
            val expectedFolderResult = FolderView(
                id = "123",
                name = folderName,
                revisionDate = fixedClock.instant(),
            )
            coEvery {
                vaultRepository.createFolder(any())
            } returns CreateFolderResult.Success(expectedFolderResult)
            viewModel.trySendAction(VaultAddEditAction.Common.AddNewFolder(folderName))
            coVerify {
                vaultRepository.createFolder(
                    FolderView(
                        name = folderName,
                        id = null,
                        revisionDate = fixedClock.instant(),
                    ),
                )
            }
        }

        @Test
        fun `AddNewFolder updates dialog states and selected folder id on success`() = runTest {
            val folderId = "123"
            val folderName = "folderName"
            val expectedFolderResult = FolderView(
                id = folderId,
                name = folderName,
                revisionDate = fixedClock.instant(),
            )
            coEvery {
                vaultRepository.createFolder(any())
            } returns CreateFolderResult.Success(expectedFolderResult)

            viewModel.stateFlow.test {
                awaitItem() // initial state.
                viewModel.trySendAction(VaultAddEditAction.Common.AddNewFolder(folderName))
                assertEquals(
                    vaultAddItemInitialState.copy(
                        dialog = VaultAddEditState.DialogState.Loading(
                            BitwardenString.saving.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    createVaultAddItemState(
                        dialogState = null,
                        commonContentViewState = createCommonContentViewState(
                            selectedFolderId = folderId,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

        @Test
        fun `State updates when available folders state is updated`() {
            mutableFolderStateFlow.update {
                DataState.Loaded(
                    data = listOf(
                        FolderView(
                            name = "folder",
                            revisionDate = fixedClock.instant(),
                            id = null,
                        ),
                    ),
                )
            }
            val folderList = listOf(
                VaultAddEditState.Folder(
                    id = null,
                    name = "No Folder",
                ),
                VaultAddEditState.Folder(
                    id = null,
                    name = "folder",
                ),
            )

            assertEquals(
                createVaultAddItemState(
                    commonContentViewState = createCommonContentViewState(
                        availableFolders = folderList.toList(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
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
            val action = VaultAddEditAction.Common.OwnershipChange("mockId-1")

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

        @Test
        fun `SelectOwnerForItem should update state to show bottom sheet`() = runTest {
            val action = VaultAddEditAction.Common.SelectOwnerForItem

            viewModel.trySendAction(action)

            val expectedState = vaultAddItemInitialState.copy(
                bottomSheetState = VaultAddEditState.BottomSheetState.OwnerSelection,
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `DismissOwnerSelectionBottomSheet should update state to hide bottom sheet`() =
            runTest {
                val action = VaultAddEditAction.Common.DismissBottomSheet

                viewModel.trySendAction(VaultAddEditAction.Common.SelectOwnerForItem)

                assertEquals(
                    vaultAddItemInitialState.copy(
                        bottomSheetState = VaultAddEditState.BottomSheetState.OwnerSelection,
                    ),
                    viewModel.stateFlow.value,
                )
                val expectedState = vaultAddItemInitialState.copy(
                    bottomSheetState = null,
                )
                viewModel.trySendAction(action)
                assertEquals(
                    expectedState,
                    viewModel.stateFlow.value,
                )
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
                typeContentViewState = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                commonContentViewState = VaultAddEditState.ViewState.Content.Common(
                    customFieldData = listOf(
                        VaultAddEditState.Custom.TextField(
                            itemId = "TestId 1",
                            name = "Test Text",
                            value = "Test Text",
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
                    vaultAddEditType = VaultAddEditType.AddItem,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                    vaultAddEditType = VaultAddEditType.AddItem,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                    vaultAddEditType = VaultAddEditType.AddItem,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
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
        fun `CollectionSelect should update selectedOwnerId when isIndividualVaultDisabled is true`() =
            runTest {
                every {
                    policyManager.getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
                } returns listOf(
                    createMockPolicy(
                        organizationId = "Test Org",
                        id = "testId",
                        type = PolicyTypeJson.PERSONAL_OWNERSHIP,
                        isEnabled = true,
                        data = null,
                    ),
                )

                val vaultAddEditType = VaultAddEditType.AddItem
                val vaultItemCipherType = VaultItemCipherType.LOGIN
                mutableVaultDataFlow.value = DataState.Loaded(
                    data = createVaultData(),
                )

                val viewModel = createAddVaultItemViewModel(
                    savedStateHandle = createSavedStateHandleWithState(
                        state = null,
                        vaultAddEditType = vaultAddEditType,
                        vaultItemCipherType = vaultItemCipherType,
                    ),
                )

                val action = collectionSelectAction()
                viewModel.trySendAction(action)

                val expectedState = vaultAddItemInitialState.copy(
                    viewState = VaultAddEditState.ViewState.Content(
                        common = createCommonContentViewState(
                            availableOwners = listOf(
                                VaultAddEditState.Owner(
                                    id = "organizationId",
                                    name = "organizationName",
                                    collections = emptyList(),
                                ),
                            ),
                            selectedOwnerId = "organizationId",
                        ),
                        isIndividualVaultDisabled = true,
                        type = createLoginTypeContentViewState(),
                    ),
                )
                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationLockout should set isUserVerified to false and display CredentialErrorDialog`() {
            viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationLockOut)

            verify { bitwardenCredentialManager.isUserVerified = false }
            assertEquals(
                VaultAddEditState.DialogState.CredentialError(
                    message = BitwardenString.passkey_operation_failed_because_user_could_not_be_verified.asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationCancelled should clear dialog state, set isUserVerified to false, and emit CompleteCredentialRegistration with cancelled result`() =
            runTest {
                viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationCancelled)

                verify { bitwardenCredentialManager.isUserVerified = false }
                assertNull(viewModel.stateFlow.value.dialog)
                viewModel.eventFlow.test {
                    assertEquals(
                        VaultAddEditEvent.CompleteCredentialRegistration(
                            result = CreateCredentialResult.Cancelled,
                        ),
                        awaitItem(),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationFail should set isUserVerified to false, and display CredentialErrorDialog`() {
            viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationFail)

            verify { bitwardenCredentialManager.isUserVerified = false }
            assertEquals(
                VaultAddEditState.DialogState.CredentialError(
                    message = BitwardenString.passkey_operation_failed_because_user_could_not_be_verified.asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationNotSupported should display CredentialErrorDialog when active account not found`() {
            mutableUserStateFlow.value = null
            viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationNotSupported)
            verify { bitwardenCredentialManager.isUserVerified = false }
            assertEquals(
                VaultAddEditState.DialogState.CredentialError(
                    message = BitwardenString.passkey_operation_failed_because_user_could_not_be_verified.asText(),
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
            verify { bitwardenCredentialManager.isUserVerified = false }
            assertEquals(
                VaultAddEditState.DialogState.Fido2PinPrompt,
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationNotSupported should display Fido2MasterPasswordPrompt when user has password but no pin`() {
            viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationNotSupported)
            verify { bitwardenCredentialManager.isUserVerified = false }
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

            verify { bitwardenCredentialManager.isUserVerified = false }
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

            verify { bitwardenCredentialManager.isUserVerified = false }
            assertEquals(
                VaultAddEditState.DialogState.Fido2PinSetUpPrompt,
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `MasterPasswordFido2VerificationSubmit should display CredentialError when password verification fails`() {
            val password = "password"
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Error(error = Throwable("Fail!"))

            viewModel.trySendAction(
                VaultAddEditAction.Common.MasterPasswordFido2VerificationSubmit(
                    password = password,
                ),
            )

            assertEquals(
                VaultAddEditState.DialogState.CredentialError(
                    message = BitwardenString.passkey_operation_failed_because_user_could_not_be_verified
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
        fun `MasterPasswordFido2VerificationSubmit should display CredentialError when user has no retries remaining`() {
            val password = "password"
            every { bitwardenCredentialManager.hasAuthenticationAttemptsRemaining() } returns false
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = false)

            viewModel.trySendAction(
                VaultAddEditAction.Common.MasterPasswordFido2VerificationSubmit(
                    password = password,
                ),
            )

            assertEquals(
                VaultAddEditState.DialogState.CredentialError(
                    message = BitwardenString.passkey_operation_failed_because_user_could_not_be_verified
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
        fun `PinFido2VerificationSubmit should display CredentialError when Pin verification fails`() {
            val pin = "PIN"
            coEvery {
                authRepository.validatePinUserKey(pin = pin)
            } returns ValidatePinResult.Error(error = Throwable("Fail!"))

            viewModel.trySendAction(
                VaultAddEditAction.Common.PinFido2VerificationSubmit(
                    pin = pin,
                ),
            )

            assertEquals(
                VaultAddEditState.DialogState.CredentialError(
                    message = BitwardenString.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
            coVerify {
                authRepository.validatePinUserKey(pin = pin)
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `PinFido2VerificationSubmit should display Fido2PinError when user has retries remaining`() {
            val pin = "PIN"
            coEvery {
                authRepository.validatePinUserKey(pin = pin)
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
                authRepository.validatePinUserKey(pin = pin)
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `PinFido2VerificationSubmit should display CredentialError when user has no retries remaining`() {
            val pin = "PIN"
            every { bitwardenCredentialManager.hasAuthenticationAttemptsRemaining() } returns false
            coEvery {
                authRepository.validatePinUserKey(pin = pin)
            } returns ValidatePinResult.Success(isValid = false)

            viewModel.trySendAction(
                VaultAddEditAction.Common.PinFido2VerificationSubmit(
                    pin = pin,
                ),
            )

            assertEquals(
                VaultAddEditState.DialogState.CredentialError(
                    message = BitwardenString.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
            coVerify {
                authRepository.validatePinUserKey(pin = pin)
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `PinFido2VerificationSubmit should register credential when pin authenticated successfully`() {
            val pin = "PIN"
            coEvery {
                authRepository.validatePinUserKey(pin = pin)
            } returns ValidatePinResult.Success(isValid = true)

            viewModel.trySendAction(
                VaultAddEditAction.Common.PinFido2VerificationSubmit(
                    pin = pin,
                ),
            )
            coVerify {
                authRepository.validatePinUserKey(pin = pin)
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
        fun `DismissFido2VerificationDialogClick should display CredentialErrorDialog`() {
            viewModel.trySendAction(
                VaultAddEditAction.Common.DismissFido2VerificationDialogClick,
            )

            assertEquals(
                VaultAddEditState.DialogState.CredentialError(
                    message = BitwardenString
                        .passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationSuccess should display CredentialErrorDialog when request is invalid`() {
            every { authRepository.activeUserId } returns null
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.ProviderCreateCredential(
                    createMockCreateCredentialRequest(
                        number = 1,
                    ),
                )

            viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationSuccess)

            assertEquals(
                VaultAddEditState.DialogState.CredentialError(
                    message = BitwardenString.passkey_operation_failed_because_the_request_is_unsupported
                        .asText(),
                ),
                viewModel.stateFlow.value.dialog,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UserVerificationSuccess should set isUserVerified to true`() =
            runTest {
                val mockRequest = createMockCreateCredentialRequest(number = 1)
                val mockResult = Fido2RegisterCredentialResult.Success(
                    responseJson = "mockResponse",
                )
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.ProviderCreateCredential(
                        createCredentialRequest = mockRequest,
                    )
                setupFido2CreateRequest()
                every { authRepository.activeUserId } returns "activeUserId"
                coEvery {
                    bitwardenCredentialManager.registerFido2Credential(
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns mockResult
                every { bitwardenCredentialManager.isUserVerified } returns false
                viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationSuccess)

                verify {
                    bitwardenCredentialManager.isUserVerified = true
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `Fido2RegisterCredentialResult Error should show toast and emit CompleteCredentialRegistration result`() =
            runTest {
                val mockRequest = createMockCreateCredentialRequest(number = 1)
                val mockResult = Fido2RegisterCredentialResult.Error.InternalError
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.ProviderCreateCredential(
                        createCredentialRequest = mockRequest,
                    )
                every { authRepository.activeUserId } returns "activeUserId"
                coEvery {
                    bitwardenCredentialManager.registerFido2Credential(
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns mockResult

                setupFido2CreateRequest()

                viewModel.trySendAction(
                    VaultAddEditAction.Internal.Fido2RegisterCredentialResultReceive(
                        mockResult,
                    ),
                )

                viewModel.eventFlow.test {
                    assertEquals(
                        VaultAddEditEvent.CompleteCredentialRegistration(
                            CreateCredentialResult.Error(
                                BitwardenString.passkey_registration_failed_due_to_an_internal_error
                                    .asText(),
                            ),
                        ),
                        awaitItem(),
                    )
                }
                verify(exactly = 1) {
                    toastManager.show(messageId = BitwardenString.an_error_has_occurred)
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `Fido2RegisterCredentialResult Success should show toast and emit CompleteCredentialRegistration result`() =
            runTest {
                val mockRequest = createMockCreateCredentialRequest(number = 1)
                val mockResult = Fido2RegisterCredentialResult.Success(
                    responseJson = "mockResponse",
                )
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.ProviderCreateCredential(
                        createCredentialRequest = mockRequest,
                    )
                setupFido2CreateRequest()
                every { authRepository.activeUserId } returns "activeUserId"
                coEvery {
                    bitwardenCredentialManager.registerFido2Credential(
                        any(),
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
                        VaultAddEditEvent.CompleteCredentialRegistration(
                            CreateCredentialResult.Success.Fido2CredentialRegistered(
                                responseJson = "mockResponse",
                            ),
                        ),
                        awaitItem(),
                    )
                }
                verify(exactly = 1) {
                    toastManager.show(messageId = BitwardenString.item_updated)
                }
            }
    }

    @Test
    fun `when LearnMoreClick action is handled NavigateToLearnMore event is sent`() =
        runTest {
            val viewModel = createAddVaultItemViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultAddEditAction.ItemType.LoginType.LearnMoreClick)
                assertEquals(
                    VaultAddEditEvent.NavigateToLearnMore,
                    awaitItem(),
                )
            }
        }

    //region Helper functions

    @Suppress("LongParameterList")
    private fun createVaultAddItemState(
        vaultAddEditType: VaultAddEditType = VaultAddEditType.AddItem,
        vaultItemCipherType: VaultItemCipherType = VaultItemCipherType.LOGIN,
        commonContentViewState: VaultAddEditState.ViewState.Content.Common =
            createCommonContentViewState(),
        isIndividualVaultDisabled: Boolean = false,
        shouldExitOnSave: Boolean = false,
        typeContentViewState: VaultAddEditState.ViewState.Content.ItemType =
            createLoginTypeContentViewState(),
        dialogState: VaultAddEditState.DialogState? = null,
        bottomSheetState: VaultAddEditState.BottomSheetState? = null,
        totpData: TotpData? = null,
        shouldClearSpecialCircumstance: Boolean = true,
        createCredentialRequest: CreateCredentialRequest? = null,
    ): VaultAddEditState =
        VaultAddEditState(
            vaultAddEditType = vaultAddEditType,
            cipherType = vaultItemCipherType,
            viewState = VaultAddEditState.ViewState.Content(
                common = commonContentViewState,
                isIndividualVaultDisabled = isIndividualVaultDisabled,
                type = typeContentViewState,
            ),
            dialog = dialogState,
            bottomSheetState = bottomSheetState,
            shouldExitOnSave = shouldExitOnSave,
            totpData = totpData,
            shouldShowCoachMarkTour = false,
            shouldClearSpecialCircumstance = shouldClearSpecialCircumstance,
            createCredentialRequest = createCredentialRequest,
            defaultUriMatchType = UriMatchTypeModel.EXACT,
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
        selectedFolderId: String? = null,
    ): VaultAddEditState.ViewState.Content.Common =
        VaultAddEditState.ViewState.Content.Common(
            name = name,
            selectedFolderId = selectedFolderId,
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
        vaultItemCipherType: VaultItemCipherType,
    ): SavedStateHandle = SavedStateHandle().apply {
        set("state", state)
        every { toVaultAddEditArgs() } returns VaultAddEditArgs(
            vaultAddEditType = vaultAddEditType,
            vaultItemCipherType = vaultItemCipherType,
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
            bitwardenCredentialManager = bitwardenCredentialManager,
            generatorRepository = generatorRepo,
            settingsRepository = settingsRepository,
            snackbarRelayManager = snackbarRelayManager,
            toastManager = toastManager,
            specialCircumstanceManager = specialCircumstanceManager,
            resourceManager = bitwardenResourceManager,
            clock = clock,
            organizationEventManager = organizationEventManager,
            networkConnectionManager = networkConnectionManager,
            firstTimeActionManager = firstTimeActionManager,
        )

    private fun createVaultData(
        cipherListView: CipherListView? = null,
        collectionViewList: List<CollectionView> = emptyList(),
        folderViewList: List<FolderView> = emptyList(),
        sendViewList: List<SendView> = emptyList(),
    ): VaultData =
        VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = cipherListView?.let { listOf(it) } ?: emptyList(),
            ),
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
                            keyConnectorUrl = null,
                            userIsClaimedByOrganization = false,
                            limitItemDeletion = false,
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
                    isExportable = true,
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
                            isDefaultUserCollection = false,
                        ),
                    )
                } else {
                    emptyList()
                },
            ),
        )

    private fun ownershipChangeAction(): VaultAddEditAction.Common.OwnershipChange =
        VaultAddEditAction.Common.OwnershipChange("organizationId")

    private fun collectionSelectAction(): VaultAddEditAction.Common.CollectionSelect =
        VaultAddEditAction.Common.CollectionSelect(
            VaultCollection(
                id = "mockId-1",
                name = "mockName-1",
                isSelected = false,
                isDefaultUserCollection = false,
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
                vaultItemCipherType = VaultItemCipherType.LOGIN,
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
                vaultAddEditType = VaultAddEditType.AddItem,
                vaultItemCipherType = VaultItemCipherType.LOGIN,
            ),
        )

        var name: String
        lateinit var expectedCustomField: VaultAddEditState.Custom
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
        val action = VaultAddEditAction.Common.AddNewCustomFieldClick(type, name)
        expectedState = currentContentState
            .copy(
                common = currentContentState.common.copy(
                    customFieldData = listOf(expectedCustomField),
                ),
            )

        viewModel.trySendAction(action)
        assertEquals(expectedState, viewModel.stateFlow.value.viewState)
    }

    private fun setupFido2CreateRequest(
        mockCallingAppInfo: CallingAppInfo = mockk(relaxed = true),
        mockCreatePublicKeyCredentialRequest: CreatePublicKeyCredentialRequest =
            mockk<CreatePublicKeyCredentialRequest>(relaxed = true),
        mockProviderCreateCredentialRequest: ProviderCreateCredentialRequest =
            mockk<ProviderCreateCredentialRequest>(relaxed = true) {
                every { callingAppInfo } returns mockCallingAppInfo
                every { callingRequest } returns mockCreatePublicKeyCredentialRequest
            },
    ) {
        every {
            ProviderCreateCredentialRequest.fromBundle(any())
        } returns mockProviderCreateCredentialRequest
    }
    //endregion Helper functions
}

private const val TEST_ID = "testId"

private const val DEFAULT_EDIT_ITEM_ID: String = "mockId-1"
