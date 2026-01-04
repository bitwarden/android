package com.x8bit.bitwarden.ui.vault.feature.item

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.data.manager.file.FileManager
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenPlurals
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asPluralsText
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCipherPermissions
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.DownloadAttachmentResult
import com.x8bit.bitwarden.data.vault.repository.model.RestoreCipherResult
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import com.x8bit.bitwarden.ui.vault.feature.item.model.TotpCodeItemData
import com.x8bit.bitwarden.ui.vault.feature.item.model.VaultItemLocation
import com.x8bit.bitwarden.ui.vault.feature.item.util.createCommonContent
import com.x8bit.bitwarden.ui.vault.feature.item.util.createLoginContent
import com.x8bit.bitwarden.ui.vault.feature.item.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.util.createVerificationCodeItem
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Instant

@Suppress("LargeClass")
class VaultItemViewModelTest : BaseViewModelTest() {

    private val mutableVaultItemFlow = MutableStateFlow<DataState<CipherView?>>(DataState.Loading)
    private val mutableAuthCodeItemFlow =
        MutableStateFlow<DataState<VerificationCodeItem?>>(DataState.Loading)
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val mutableCollectionsStateFlow =
        MutableStateFlow<DataState<List<CollectionView>>>(DataState.Loading)
    private val mutableFoldersStateFlow =
        MutableStateFlow<DataState<List<FolderView>>>(DataState.Loading)

    private val clipboardManager: BitwardenClipboardManager = mockk {
        every { setText(text = any<String>(), toastDescriptorOverride = any<Text>()) } just runs
    }
    private val authRepo: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val vaultRepo: VaultRepository = mockk {
        every { getAuthCodeFlow(VAULT_ITEM_ID) } returns mutableAuthCodeItemFlow
        every { getVaultItemStateFlow(VAULT_ITEM_ID) } returns mutableVaultItemFlow
        every { collectionsStateFlow } returns mutableCollectionsStateFlow
        every { foldersStateFlow } returns mutableFoldersStateFlow
    }

    private val mockFileManager: FileManager = mockk()

    private val organizationEventManager = mockk<OrganizationEventManager> {
        every { trackEvent(event = any()) } just runs
    }
    private val mockCipherView = mockk<CipherView> {
        every { collectionIds } returns emptyList()
        every { edit } returns true
        every { folderId } returns null
        every { organizationId } returns null
        every { permissions } returns null
        every { deletedDate } returns null
    }
    private val mockEnvironmentRepository = FakeEnvironmentRepository()
    private val mutableIsIconLoadingDisabledFlow = MutableStateFlow(false)
    private val mockSettingsRepository = mockk<SettingsRepository> {
        every { isIconLoadingDisabled } returns false
        every { isIconLoadingDisabledFlow } returns mutableIsIconLoadingDisabledFlow
    }
    private val mutableSnackbarDataFlow: MutableSharedFlow<BitwardenSnackbarData> =
        bufferedMutableSharedFlow()
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay> = mockk {
        every {
            getSnackbarDataFlow(relay = any(), relays = anyVararg())
        } returns mutableSnackbarDataFlow
        every { sendSnackbarData(data = any(), relay = any()) } just runs
    }

    @BeforeEach
    fun setup() {
        mockkStatic(
            SavedStateHandle::toVaultItemArgs,
            CipherView::toViewState,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            SavedStateHandle::toVaultItemArgs,
            CipherView::toViewState,
        )
        unmockkStatic(Uri::class)
    }

    @Test
    fun `initial state should be correct when not set`() {
        val viewModel = createViewModel(state = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        verify(exactly = 1) {
            organizationEventManager.trackEvent(
                event = OrganizationEvent.CipherClientViewed(cipherId = VAULT_ITEM_ID),
            )
        }
    }

    @Test
    fun `initial state should be correct when set`() {
        val differentVaultItemId = "something_different"
        every {
            vaultRepo.getVaultItemStateFlow(differentVaultItemId)
        } returns MutableStateFlow<DataState<CipherView?>>(DataState.Loading)

        every {
            vaultRepo.getAuthCodeFlow(differentVaultItemId)
        } returns MutableStateFlow<DataState<VerificationCodeItem?>>(DataState.Loading)

        val state = DEFAULT_STATE.copy(vaultItemId = differentVaultItemId)
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)
        verify(exactly = 1) {
            organizationEventManager.trackEvent(
                event = OrganizationEvent.CipherClientViewed(cipherId = differentVaultItemId),
            )
        }
    }

    @Test
    fun `snackbar relay emission should send ShowSnackbar`() = runTest {
        val viewModel = createViewModel(DEFAULT_STATE)
        val snackbarData = mockk<BitwardenSnackbarData>()
        viewModel.eventFlow.test {
            mutableSnackbarDataFlow.emit(snackbarData)
            assertEquals(VaultItemEvent.ShowSnackbar(snackbarData), awaitItem())
        }
    }

    @Nested
    inner class CommonActions {
        private lateinit var viewModel: VaultItemViewModel

        @BeforeEach
        fun setup() {
            viewModel = createViewModel(
                state = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE),
            )
        }

        @Test
        fun `on CloseClick should emit NavigateBack`() = runTest {
            val viewModel = createViewModel(state = DEFAULT_STATE)
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemAction.Common.CloseClick)
                assertEquals(VaultItemEvent.NavigateBack, awaitItem())
            }
        }

        @Test
        fun `on DismissDialogClick should clear the dialog state`() = runTest {
            val initialState = DEFAULT_STATE.copy(
                dialog = VaultItemState.DialogState.Loading(
                    message = BitwardenString.loading.asText(),
                ),
            )
            val viewModel = createViewModel(state = initialState)
            assertEquals(initialState, viewModel.stateFlow.value)

            viewModel.trySendAction(VaultItemAction.Common.DismissDialogClick)
            assertEquals(initialState.copy(dialog = null), viewModel.stateFlow.value)
        }

        @Test
        fun `DeleteClick should update state`() =
            runTest {
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canRestore = false,
                        canAssignToCollections = true,
                        canEdit = true,
                        baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                        isIconLoadingDisabled = false,
                        relatedLocations = persistentListOf(),
                        hasOrganizations = true,
                    )
                } returns DEFAULT_VIEW_STATE

                val expected = DEFAULT_STATE.copy(
                    viewState = DEFAULT_VIEW_STATE,
                    dialog = VaultItemState.DialogState.DeleteConfirmationPrompt(
                        BitwardenString.do_you_really_want_to_soft_delete_cipher.asText(),
                    ),
                )

                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
                mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

                viewModel.trySendAction(VaultItemAction.Common.DeleteClick)
                assertEquals(expected, viewModel.stateFlow.value)
            }

        @Test
        fun `DeleteClick should update state when it is a hard delete`() = runTest {
            val loginState = DEFAULT_VIEW_STATE.copy(
                common = DEFAULT_COMMON
                    .copy(
                        currentCipher = DEFAULT_COMMON
                            .currentCipher
                            ?.copy(deletedDate = Instant.MIN),
                    ),
            )

            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns loginState

            val expected = DEFAULT_STATE.copy(
                viewState = loginState,
                dialog = VaultItemState.DialogState.DeleteConfirmationPrompt(
                    BitwardenString.do_you_really_want_to_permanently_delete_cipher.asText(),
                ),
            )

            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.Common.DeleteClick)
            assertEquals(expected, viewModel.stateFlow.value)
        }

        @Test
        @Suppress("MaxLineLength")
        fun `ConfirmDeleteClick with DeleteCipherResult Success should should send snackbar data and NavigateBack`() =
            runTest {
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canRestore = false,
                        canAssignToCollections = true,
                        canEdit = true,
                        baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                        isIconLoadingDisabled = false,
                        relatedLocations = persistentListOf(),
                        hasOrganizations = true,
                    )
                } returns DEFAULT_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
                mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

                val viewModel = createViewModel(state = DEFAULT_STATE)
                coEvery {
                    vaultRepo.softDeleteCipher(
                        cipherId = VAULT_ITEM_ID,
                        cipherView = createMockCipherView(number = 1),
                    )
                } returns DeleteCipherResult.Success

                viewModel.trySendAction(VaultItemAction.Common.ConfirmDeleteClick)

                viewModel.eventFlow.test {
                    assertEquals(
                        VaultItemEvent.NavigateBack,
                        awaitItem(),
                    )
                }
                verify {
                    snackbarRelayManager.sendSnackbarData(
                        data = BitwardenSnackbarData(message = BitwardenString.item_soft_deleted.asText()),
                        relay = SnackbarRelay.CIPHER_DELETED,
                    )
                }
            }

        @Test
        @Suppress("MaxLineLength")
        fun `ConfirmDeleteClick with DeleteCipherResult Failure should should Show generic error`() =
            runTest {
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canRestore = false,
                        canAssignToCollections = true,
                        canEdit = true,
                        baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                        isIconLoadingDisabled = false,
                        relatedLocations = persistentListOf(),
                        hasOrganizations = true,
                    )
                } returns DEFAULT_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
                mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

                val viewModel = createViewModel(state = DEFAULT_STATE)
                val error = Throwable("Oh dang.")
                coEvery {
                    vaultRepo.softDeleteCipher(
                        cipherId = VAULT_ITEM_ID,
                        cipherView = createMockCipherView(number = 1),
                    )
                } returns DeleteCipherResult.Error(error = error)

                viewModel.trySendAction(VaultItemAction.Common.ConfirmDeleteClick)

                assertEquals(
                    DEFAULT_STATE.copy(
                        viewState = DEFAULT_VIEW_STATE,
                        dialog = VaultItemState.DialogState.Generic(
                            message = BitwardenString.generic_error_message.asText(),
                            error = error,
                        ),
                    ),
                    viewModel.stateFlow.value,
                )
            }

        @Test
        fun `ConfirmDeleteClick with deleted cipher should should invoke hardDeleteCipher`() =
            runTest {
                val loginViewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON
                        .copy(
                            currentCipher = DEFAULT_COMMON
                                .currentCipher
                                ?.copy(deletedDate = Instant.MIN),
                        ),
                )
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canRestore = false,
                        canAssignToCollections = true,
                        canEdit = true,
                        baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                        isIconLoadingDisabled = false,
                        relatedLocations = persistentListOf(),
                        hasOrganizations = true,
                    )
                } returns loginViewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
                mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

                val viewModel = createViewModel(state = DEFAULT_STATE)
                coEvery {
                    vaultRepo.hardDeleteCipher(
                        cipherId = VAULT_ITEM_ID,
                    )
                } returns DeleteCipherResult.Success

                viewModel.trySendAction(VaultItemAction.Common.ConfirmDeleteClick)

                viewModel.eventFlow.test {
                    assertEquals(
                        VaultItemEvent.NavigateBack,
                        awaitItem(),
                    )
                }
                verify {
                    snackbarRelayManager.sendSnackbarData(
                        data = BitwardenSnackbarData(
                            message = BitwardenString.item_deleted.asText(),
                        ),
                        relay = SnackbarRelay.CIPHER_DELETED,
                    )
                }
                coVerify { vaultRepo.hardDeleteCipher(cipherId = VAULT_ITEM_ID) }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on RestoreItemClick when no need to prompt for master password updates pendingCipher state correctly`() =
            runTest {
                val viewState = DEFAULT_VIEW_STATE
                every {
                    mockCipherView.toViewState(
                        previousState = any(),
                        isPremiumUser = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canRestore = false,
                        canAssignToCollections = true,
                        canEdit = true,
                        baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                        isIconLoadingDisabled = false,
                        relatedLocations = persistentListOf(),
                        hasOrganizations = true,
                    )
                } returns viewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
                mutableFoldersStateFlow.value = DataState.Loaded(emptyList())
                val loginState = DEFAULT_STATE.copy(viewState = viewState)
                val viewModel = createViewModel(state = DEFAULT_STATE)
                assertEquals(loginState, viewModel.stateFlow.value)

                // show dialog
                viewModel.trySendAction(VaultItemAction.Common.RestoreVaultItemClick)
                assertEquals(
                    loginState.copy(dialog = VaultItemState.DialogState.RestoreItemDialog),
                    viewModel.stateFlow.value,
                )

                // dismiss dialog
                viewModel.trySendAction(VaultItemAction.Common.DismissDialogClick)
                assertEquals(
                    // setting this to be explicit.
                    loginState.copy(dialog = null),
                    viewModel.stateFlow.value,
                )
            }

        @Test
        @Suppress("MaxLineLength")
        fun `ConfirmRestoreClick with RestoreCipherResult Success should should send snackbar data and NavigateBack`() =
            runTest {
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canRestore = false,
                        canAssignToCollections = true,
                        canEdit = true,
                        baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                        isIconLoadingDisabled = false,
                        relatedLocations = persistentListOf(),
                        hasOrganizations = true,
                    )
                } returns DEFAULT_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(
                    data = createVerificationCodeItem(),
                )
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
                mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

                val viewModel = createViewModel(state = DEFAULT_STATE)
                coEvery {
                    vaultRepo.restoreCipher(
                        cipherId = VAULT_ITEM_ID,
                        cipherView = createMockCipherView(number = 1),
                    )
                } returns RestoreCipherResult.Success

                viewModel.trySendAction(VaultItemAction.Common.ConfirmRestoreClick)

                viewModel.eventFlow.test {
                    assertEquals(
                        VaultItemEvent.NavigateBack,
                        awaitItem(),
                    )
                }
                verify {
                    snackbarRelayManager.sendSnackbarData(
                        data = BitwardenSnackbarData(message = BitwardenString.item_restored.asText()),
                        relay = SnackbarRelay.CIPHER_RESTORED,
                    )
                }
            }

        @Test
        @Suppress("MaxLineLength")
        fun `ConfirmRestoreClick with RestoreCipherResult Failure should should Show generic error`() {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = createTotpCodeData(),
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns DEFAULT_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = createVerificationCodeItem())
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            val viewModel = createViewModel(state = DEFAULT_STATE)
            val error = Throwable("Fail")
            coEvery {
                vaultRepo.restoreCipher(
                    cipherId = VAULT_ITEM_ID,
                    cipherView = createMockCipherView(number = 1),
                )
            } returns RestoreCipherResult.Error(error = error)

            viewModel.trySendAction(VaultItemAction.Common.ConfirmRestoreClick)

            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = DEFAULT_VIEW_STATE,
                    dialog = VaultItemState.DialogState.Generic(
                        message = BitwardenString.generic_error_message.asText(),
                        error = error,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

        @Test
        fun `on EditClick should navigate password`() = runTest {
            val loginViewState = createViewState()
            every {
                mockCipherView.toViewState(
                    previousState = any(),
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns loginViewState
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())
            val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
            val viewModel = createViewModel(state = loginState)

            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemAction.Common.EditClick)
                verify {
                    mockCipherView.toViewState(
                        previousState = loginViewState,
                        isPremiumUser = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canRestore = false,
                        canAssignToCollections = true,
                        canEdit = true,
                        baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                        isIconLoadingDisabled = false,
                        relatedLocations = persistentListOf(),
                        hasOrganizations = true,
                    )
                }
                assertEquals(
                    VaultItemEvent.NavigateToAddEdit(
                        itemId = VAULT_ITEM_ID,
                        isClone = false,
                        type = VaultItemCipherType.LOGIN,
                    ),
                    awaitItem(),
                )
            }
        }

        @Test
        fun `on RefreshClick should sync`() = runTest {
            every { vaultRepo.sync(forced = true) } just runs
            val viewModel = createViewModel(state = DEFAULT_STATE)

            viewModel.trySendAction(VaultItemAction.Common.RefreshClick)

            verify(exactly = 1) {
                vaultRepo.sync(forced = true)
            }
        }

        @Test
        fun `on CopyCustomHiddenFieldClick should call setText on ClipboardManager`() {
            val field = "field"
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    totpCodeItemData = null,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns createViewState()
            every { clipboardManager.setText(text = field) } just runs

            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.Common.CopyCustomHiddenFieldClick(field))

            verify(exactly = 1) {
                clipboardManager.setText(text = field)
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    totpCodeItemData = null,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientCopiedHiddenField(
                        cipherId = VAULT_ITEM_ID,
                    ),
                )
            }
        }

        @Test
        fun `on CopyCustomTextFieldClick should call setText on ClipboardManager`() {
            val field = "field"
            every { clipboardManager.setText(text = field) } just runs

            viewModel.trySendAction(VaultItemAction.Common.CopyCustomTextFieldClick(field))

            verify(exactly = 1) {
                clipboardManager.setText(text = field)
            }
        }

        @Test
        fun `on HiddenFieldVisibilityClicked should update hidden field visibility`() = runTest {
            val hiddenField =
                VaultItemState.ViewState.Content.Common.Custom.HiddenField(
                    id = "12345",
                    name = "hidden",
                    value = "value",
                    isCopyable = true,
                    isVisible = false,
                )
            val loginViewState = VaultItemState.ViewState.Content(
                common = createCommonContent(
                    isEmpty = true,
                    isPremiumUser = true,
                ).copy(
                    customFields = listOf(hiddenField),
                ),
                type = createLoginContent(isEmpty = true),
            )
            val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns loginViewState
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            assertEquals(loginState, viewModel.stateFlow.value)
            viewModel.trySendAction(
                VaultItemAction.Common.HiddenFieldVisibilityClicked(
                    field = hiddenField,
                    isVisible = true,
                ),
            )
            assertEquals(
                loginState.copy(
                    viewState = loginViewState.copy(
                        common = createCommonContent(isEmpty = true, isPremiumUser = true).copy(
                            customFields = listOf(hiddenField.copy(isVisible = true)),
                        ),
                    ),
                ),
                viewModel.stateFlow.value,
            )

            verify(exactly = 1) {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    totpCodeItemData = null,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientToggledHiddenFieldVisible(
                        cipherId = VAULT_ITEM_ID,
                    ),
                )
            }
        }

        @Test
        fun `on AttachmentsClick should emit NavigateToAttachments`() = runTest {
            val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    totpCodeItemData = null,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns DEFAULT_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            assertEquals(loginState, viewModel.stateFlow.value)

            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemAction.Common.AttachmentsClick)
                assertEquals(
                    VaultItemEvent.NavigateToAttachments(itemId = VAULT_ITEM_ID),
                    awaitItem(),
                )
            }
        }

        @Test
        fun `on CloneClick should show confirmation when cipher contains a passkey`() = runTest {
            val loginViewState = DEFAULT_VIEW_STATE.copy(
                common = DEFAULT_COMMON.copy(
                    requiresCloneConfirmation = true,
                ),
            )
            val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    totpCodeItemData = null,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns loginViewState
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            assertEquals(loginState, viewModel.stateFlow.value)

            viewModel.trySendAction(VaultItemAction.Common.CloneClick)

            @Suppress("MaxLineLength")
            assertEquals(
                loginState.copy(
                    dialog = VaultItemState.DialogState.Fido2CredentialCannotBeCopiedConfirmationPrompt(
                        message = BitwardenString.the_passkey_will_not_be_copied_to_the_cloned_item_do_you_want_to_continue_cloning_this_item.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )

            verify(exactly = 1) {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    totpCodeItemData = null,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            }
        }

        @Test
        fun `on CloneClick should emit NavigateToAddEdit`() = runTest {
            val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    totpCodeItemData = null,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns DEFAULT_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            assertEquals(loginState, viewModel.stateFlow.value)

            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemAction.Common.CloneClick)
                assertEquals(
                    VaultItemEvent.NavigateToAddEdit(
                        itemId = VAULT_ITEM_ID,
                        isClone = true,
                        type = VaultItemCipherType.LOGIN,
                    ),
                    awaitItem(),
                )
            }
        }

        @Test
        fun `on MoveToOrganizationClick should emit NavigateToMoveToOrganization`() = runTest {
            val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    totpCodeItemData = null,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns DEFAULT_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            assertEquals(loginState, viewModel.stateFlow.value)

            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemAction.Common.MoveToOrganizationClick)
                assertEquals(
                    VaultItemEvent.NavigateToMoveToOrganization(itemId = VAULT_ITEM_ID),
                    awaitItem(),
                )
            }
        }

        @Test
        fun `on CollectionsClick should emit NavigateToCollections`() = runTest {
            val viewModel = createViewModel(state = DEFAULT_STATE)
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemAction.Common.CollectionsClick)
                assertEquals(
                    VaultItemEvent.NavigateToCollections(itemId = VAULT_ITEM_ID),
                    awaitItem(),
                )
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `on AttachmentDownloadClick should show loading dialog, attempt to download an attachment, and display an error dialog on failure`() =
            runTest {
                val loginViewState = createViewState()
                every {
                    mockCipherView.toViewState(
                        previousState = any(),
                        isPremiumUser = true,
                        canDelete = true,
                        canRestore = false,
                        canAssignToCollections = true,
                        canEdit = true,
                        totpCodeItemData = null,
                        baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                        isIconLoadingDisabled = false,
                        relatedLocations = persistentListOf(),
                        hasOrganizations = true,
                    )
                } returns loginViewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
                mutableFoldersStateFlow.value = DataState.Loaded(emptyList())
                val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
                val viewModel = createViewModel(state = loginState)

                val attachment = VaultItemState.ViewState.Content.Common.AttachmentItem(
                    id = "attachment-id",
                    displaySize = "11 MB",
                    isLargeFile = true,
                    isDownloadAllowed = false,
                    url = "https://example.com",
                    title = "test.mp4",
                )
                val error = Throwable("Fail")
                coEvery {
                    vaultRepo.downloadAttachment(any(), any())
                } returns DownloadAttachmentResult.Failure(error = error)

                viewModel.stateFlow.test {
                    assertEquals(
                        loginState,
                        awaitItem(),
                    )

                    viewModel.trySendAction(
                        VaultItemAction.Common.AttachmentDownloadClick(attachment),
                    )

                    assertEquals(
                        loginState.copy(dialog = VaultItemState.DialogState.Loading(BitwardenString.downloading.asText())),
                        awaitItem(),
                    )

                    assertEquals(
                        loginState.copy(
                            dialog = VaultItemState.DialogState.Generic(
                                BitwardenString.unable_to_download_file.asText(),
                                error = error,
                            ),
                        ),
                        awaitItem(),
                    )
                }

                coVerify(exactly = 1) {
                    vaultRepo.downloadAttachment(any(), any())
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on AttachmentDownloadClick should show loading dialog, attempt to download an attachment, and emit NavigateToSelectAttachmentSaveLocation on success`() =
            runTest {
                val loginViewState = createViewState()
                every {
                    mockCipherView.toViewState(
                        previousState = any(),
                        isPremiumUser = true,
                        canDelete = true,
                        canRestore = false,
                        canAssignToCollections = true,
                        canEdit = true,
                        totpCodeItemData = null,
                        baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                        isIconLoadingDisabled = false,
                        relatedLocations = persistentListOf(),
                        hasOrganizations = true,
                    )
                } returns loginViewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
                mutableFoldersStateFlow.value = DataState.Loaded(emptyList())
                val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
                val viewModel = createViewModel(state = loginState)

                val attachment = VaultItemState.ViewState.Content.Common.AttachmentItem(
                    id = "attachment-id",
                    displaySize = "11 MB",
                    isLargeFile = true,
                    isDownloadAllowed = false,
                    url = "https://example.com",
                    title = "test.mp4",
                )

                val file = mockk<File>()
                coEvery {
                    vaultRepo.downloadAttachment(any(), any())
                } returns DownloadAttachmentResult.Success(file)

                viewModel.stateFlow.test {
                    assertEquals(
                        loginState,
                        awaitItem(),
                    )

                    viewModel.trySendAction(
                        VaultItemAction.Common.AttachmentDownloadClick(attachment),
                    )

                    assertEquals(
                        loginState.copy(
                            dialog = VaultItemState.DialogState.Loading(
                                BitwardenString.downloading.asText(),
                            ),
                        ),
                        awaitItem(),
                    )
                }

                viewModel.eventFlow.test {
                    assertEquals(
                        VaultItemEvent.NavigateToSelectAttachmentSaveLocation("test.mp4"),
                        awaitItem(),
                    )
                }

                coVerify(exactly = 1) {
                    vaultRepo.downloadAttachment(any(), any())
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on AttachmentFileLocationReceive success should hide loading dialog, copy file, delete file, and show snackbar`() =
            runTest {
                val file = mockk<File>()
                val viewModel = createViewModel(state = DEFAULT_STATE, tempAttachmentFile = file)

                coEvery {
                    mockFileManager.delete(any())
                } just runs

                val uri = mockk<Uri>()
                coEvery {
                    mockFileManager.fileToUri(uri, file)
                } returns true

                viewModel.trySendAction(
                    VaultItemAction.Common.AttachmentFileLocationReceive(uri),
                )

                assertEquals(
                    DEFAULT_STATE,
                    viewModel.stateFlow.value,
                )

                viewModel.eventFlow.test {
                    assertEquals(
                        VaultItemEvent.ShowSnackbar(BitwardenString.save_attachment_success.asText()),
                        awaitItem(),
                    )
                }

                coVerify(exactly = 1) {
                    mockFileManager.delete(file)
                    mockFileManager.fileToUri(uri, file)
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on AttachmentFileLocationReceive failure should hide loading dialog, copy file, delete file, and show dialog`() =
            runTest {
                val file = mockk<File>()
                val viewModel = createViewModel(state = DEFAULT_STATE, tempAttachmentFile = file)

                coEvery {
                    mockFileManager.delete(any())
                } just runs

                val uri = mockk<Uri>()
                coEvery {
                    mockFileManager.fileToUri(uri, file)
                } returns false

                viewModel.stateFlow.test {
                    viewModel.trySendAction(
                        VaultItemAction.Common.AttachmentFileLocationReceive(uri),
                    )

                    assertEquals(
                        DEFAULT_STATE,
                        awaitItem(),
                    )

                    assertEquals(
                        DEFAULT_STATE.copy(
                            dialog = VaultItemState.DialogState.Generic(
                                BitwardenString.unable_to_save_attachment.asText(),
                            ),
                        ),
                        awaitItem(),
                    )
                }

                coVerify(exactly = 1) {
                    mockFileManager.delete(file)
                    mockFileManager.fileToUri(uri, file)
                }
            }

        @Test
        fun `on NoAttachmentFileLocationReceive failure should show dialog`() {
            val file = mockk<File>()
            val viewModel = createViewModel(state = DEFAULT_STATE, tempAttachmentFile = file)

            coEvery {
                mockFileManager.delete(any())
            } just runs

            viewModel.trySendAction(VaultItemAction.Common.NoAttachmentFileLocationReceive)

            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = VaultItemState.DialogState.Generic(
                        BitwardenString.unable_to_save_attachment.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )

            coVerify { mockFileManager.delete(file) }
        }

        @Test
        fun `on CopyNotesFieldClick should call setText on ClipboardManager`() {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns DEFAULT_VIEW_STATE

            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            val notes = "Lots of notes"

            viewModel.trySendAction(VaultItemAction.Common.CopyNotesClick)

            verify(exactly = 1) {
                clipboardManager.setText(
                    text = notes,
                    toastDescriptorOverride = BitwardenString.notes.asText(),
                )
            }
        }
    }

    @Nested
    inner class LoginActions {
        private lateinit var viewModel: VaultItemViewModel

        @BeforeEach
        fun setup() {
            viewModel = createViewModel(
                state = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE),
            )
        }

        @Test
        fun `on CheckForBreachClick should process a password`() = runTest {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    totpCodeItemData = createTotpCodeData(),
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns DEFAULT_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = createVerificationCodeItem())
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
            val breachCount = 5
            coEvery {
                authRepo.getPasswordBreachCount(password = DEFAULT_LOGIN_PASSWORD)
            } returns BreachCountResult.Success(breachCount = breachCount)

            viewModel.stateFlow.test {
                assertEquals(loginState, awaitItem())
                viewModel.trySendAction(VaultItemAction.ItemType.Login.CheckForBreachClick)
                assertEquals(
                    loginState.copy(
                        dialog = VaultItemState.DialogState.Loading(
                            message = BitwardenString.loading.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    loginState.copy(
                        dialog = VaultItemState.DialogState.Generic(
                            message = BitwardenPlurals.password_exposed.asPluralsText(
                                quantity = breachCount,
                                args = arrayOf(breachCount),
                            ),
                        ),
                    ),
                    awaitItem(),
                )
            }

            verify(exactly = 1) {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    totpCodeItemData = createTotpCodeData(),
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            }
            coVerify(exactly = 1) {
                authRepo.getPasswordBreachCount(password = DEFAULT_LOGIN_PASSWORD)
            }
        }

        @Test
        fun `on CopyPasswordClick should call setText on the ClipboardManager`() = runTest {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    totpCodeItemData = createTotpCodeData(),
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns createViewState()
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value =
                DataState.Loaded(data = createVerificationCodeItem())
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyPasswordClick)

            verify(exactly = 1) {
                clipboardManager.setText(
                    text = DEFAULT_LOGIN_PASSWORD,
                    toastDescriptorOverride = BitwardenString.password.asText(),
                )
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    totpCodeItemData = createTotpCodeData(),
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            }
        }

        @Test
        fun `on CopyTotpClick should call setText on the ClipboardManager`() = runTest {
            setupMockUri()
            mutableVaultItemFlow.value = DataState.Loaded(
                data = createMockCipherView(1),
            )
            mutableAuthCodeItemFlow.value = DataState.Loaded(
                data = createVerificationCodeItem(),
            )
            mutableCollectionsStateFlow.value = DataState.Loaded(
                data = listOf(createMockCollectionView(1)),
            )
            mutableFoldersStateFlow.value = DataState.Loaded(
                data = listOf(createMockFolderView(1)),
            )

            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyTotpClick)

            verify(exactly = 1) {
                clipboardManager.setText(
                    text = "123456",
                    toastDescriptorOverride = BitwardenString.totp.asText(),
                )
            }
        }

        @Test
        fun `on CopyUriClick should call setText on ClipboardManager`() = runTest {
            val uri = "uri"
            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyUriClick(uri))
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = uri,
                    toastDescriptorOverride = BitwardenString.uri.asText(),
                )
            }
        }

        @Test
        fun `on CopyUsernameClick should call setText on ClipboardManager`() = runTest {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    totpCodeItemData = createTotpCodeData(),
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns createViewState()
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value =
                DataState.Loaded(data = createVerificationCodeItem())
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyUsernameClick)

            verify(exactly = 1) {
                clipboardManager.setText(
                    text = DEFAULT_LOGIN_USERNAME,
                    toastDescriptorOverride = BitwardenString.username.asText(),
                )
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = createTotpCodeData(),
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            }
        }

        @Test
        fun `on LaunchClick should emit NavigateToUri`() = runTest {
            val uri = "uri"
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemAction.ItemType.Login.LaunchClick(uri))
                assertEquals(VaultItemEvent.NavigateToUri(uri), awaitItem())
            }
        }

        @Test
        fun `on AuthenticatorHelpToolTipClick should emit NavigateToUri`() = runTest {
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    action = VaultItemAction.ItemType.Login.AuthenticatorHelpToolTipClick,
                )
                assertEquals(
                    VaultItemEvent.NavigateToUri(
                        "https://bitwarden.com/help/integrated-authenticator",
                    ),
                    awaitItem(),
                )
            }
        }

        @Test
        fun `on PasswordHistoryClick should emit NavigateToPasswordHistory`() = runTest {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = createTotpCodeData(),
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns createViewState()
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = createVerificationCodeItem())
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemAction.Common.PasswordHistoryClick)
                assertEquals(
                    VaultItemEvent.NavigateToPasswordHistory(VAULT_ITEM_ID),
                    awaitItem(),
                )
            }

            verify(exactly = 1) {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = createTotpCodeData(),
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            }
        }

        @Test
        fun `on PasswordVisibilityClicked should update password visibility`() = runTest {
            val loginViewState = createViewState()
            val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = createTotpCodeData(),
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns loginViewState
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = createVerificationCodeItem())
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            assertEquals(loginState, viewModel.stateFlow.value)
            viewModel.trySendAction(
                VaultItemAction.ItemType.Login.PasswordVisibilityClicked(
                    true,
                ),
            )
            assertEquals(
                loginState.copy(
                    viewState = loginViewState.copy(
                        type = DEFAULT_LOGIN_TYPE.copy(
                            passwordData = DEFAULT_LOGIN_TYPE.passwordData!!.copy(isVisible = true),
                        ),
                    ),
                ),
                viewModel.stateFlow.value,
            )

            verify(exactly = 1) {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = createTotpCodeData(),
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientToggledPasswordVisible(
                        cipherId = VAULT_ITEM_ID,
                    ),
                )
            }
        }
    }

    @Nested
    inner class CardActions {
        private lateinit var viewModel: VaultItemViewModel

        @BeforeEach
        fun setup() {
            viewModel = createViewModel(
                state = DEFAULT_STATE.copy(
                    viewState = CARD_VIEW_STATE,
                ),
            )
        }

        @Test
        fun `on CopyNumberClick should call setText on the ClipboardManager`() = runTest {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns createViewState(type = DEFAULT_CARD_TYPE)
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.ItemType.Card.CopyNumberClick)

            verify(exactly = 1) {
                clipboardManager.setText(
                    text = "12345436",
                    toastDescriptorOverride = BitwardenString.number.asText(),
                )
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `on NumberVisibilityClick should call trackEvent on the OrganizationEventManager and update the ViewState`() =
            runTest {
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canRestore = false,
                        canAssignToCollections = true,
                        canEdit = true,
                        baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                        isIconLoadingDisabled = false,
                        relatedLocations = persistentListOf(),
                        hasOrganizations = true,
                    )
                } returns createViewState(type = DEFAULT_CARD_TYPE)
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
                mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

                viewModel.trySendAction(
                    VaultItemAction.ItemType.Card.NumberVisibilityClick(isVisible = true),
                )

                verify(exactly = 1) {
                    organizationEventManager.trackEvent(
                        event = OrganizationEvent.CipherClientToggledCardNumberVisible(
                            cipherId = VAULT_ITEM_ID,
                        ),
                    )
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canRestore = false,
                        canAssignToCollections = true,
                        canEdit = true,
                        baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                        isIconLoadingDisabled = false,
                        relatedLocations = persistentListOf(),
                        hasOrganizations = true,
                    )
                }
            }

        @Test
        fun `on CopySecurityCodeClick should call setText on the ClipboardManager`() = runTest {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns createViewState(type = DEFAULT_CARD_TYPE)
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.ItemType.Card.CopySecurityCodeClick)

            verify(exactly = 1) {
                clipboardManager.setText(
                    text = "987",
                    toastDescriptorOverride = BitwardenString.security_code.asText(),
                )
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `on CodeVisibilityClick should call trackEvent on the OrganizationEventManager and update the ViewState`() {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns createViewState(type = DEFAULT_CARD_TYPE)
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableFoldersStateFlow.value = DataState.Loaded(data = emptyList())

            viewModel.trySendAction(
                VaultItemAction.ItemType.Card.CodeVisibilityClick(isVisible = true),
            )

            verify(exactly = 1) {
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientToggledCardCodeVisible(
                        cipherId = VAULT_ITEM_ID,
                    ),
                )
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            }
        }
    }

    @Nested
    inner class SshKeyActions {
        private lateinit var viewModel: VaultItemViewModel

        @BeforeEach
        fun setup() {
            viewModel = createViewModel(
                state = DEFAULT_STATE.copy(
                    viewState = SSH_KEY_VIEW_STATE,
                ),
            )
        }

        @Test
        fun `on CopyPublicKeyClick should copy public key to clipboard`() = runTest {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns SSH_KEY_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.ItemType.SshKey.CopyPublicKeyClick)

            verify(exactly = 1) {
                clipboardManager.setText(
                    text = DEFAULT_SSH_KEY_TYPE.publicKey,
                    toastDescriptorOverride = BitwardenString.public_key.asText(),
                )
            }
        }

        @Test
        fun `on PrivateKeyVisibilityClick should show private key`() = runTest {
            val sshKeyViewState = createViewState(type = DEFAULT_SSH_KEY_TYPE)
            val sshKeyState = DEFAULT_STATE.copy(viewState = sshKeyViewState)
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns sshKeyViewState
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            assertEquals(sshKeyState, viewModel.stateFlow.value)
            viewModel.trySendAction(
                VaultItemAction.ItemType.SshKey.PrivateKeyVisibilityClicked(
                    isVisible = true,
                ),
            )
            assertEquals(
                sshKeyState.copy(
                    viewState = sshKeyViewState.copy(
                        type = DEFAULT_SSH_KEY_TYPE.copy(showPrivateKey = true),
                    ),
                ),
                viewModel.stateFlow.value,
            )
            verify(exactly = 1) {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            }
        }

        @Test
        fun `onPrivateKeyCopyClick should copy private key to clipboard`() = runTest {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns createViewState(type = DEFAULT_SSH_KEY_TYPE)
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.ItemType.SshKey.CopyPrivateKeyClick)

            verify(exactly = 1) {
                clipboardManager.setText(
                    text = DEFAULT_SSH_KEY_TYPE.privateKey,
                    toastDescriptorOverride = BitwardenString.private_key.asText(),
                )
            }
        }

        @Test
        fun `on CopyFingerprintClick should copy fingerprint to clipboard`() = runTest {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns SSH_KEY_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.ItemType.SshKey.CopyFingerprintClick)

            verify(exactly = 1) {
                clipboardManager.setText(
                    text = DEFAULT_SSH_KEY_TYPE.fingerprint,
                    toastDescriptorOverride = BitwardenString.fingerprint.asText(),
                )
            }
        }
    }

    @Nested
    inner class IdentityActions {
        private lateinit var viewModel: VaultItemViewModel

        @BeforeEach
        fun setup() {
            viewModel = createViewModel(
                state = DEFAULT_STATE.copy(
                    viewState = IDENTITY_VIEW_STATE,
                ),
            )
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns IDENTITY_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())
        }

        @Test
        fun `on CopyIdentityNameClick should copy fingerprint to clipboard`() =
            runTest {
                val username = "the username"
                viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyUsernameClick)
                verify(exactly = 1) {
                    clipboardManager.setText(
                        text = username,
                        toastDescriptorOverride = BitwardenString.username.asText(),
                    )
                }
            }

        @Test
        fun `on CopyUsernameClick should copy fingerprint to clipboard`() =
            runTest {
                val identityName = "the identity name"
                viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyIdentityNameClick)
                verify(exactly = 1) {
                    clipboardManager.setText(
                        text = identityName,
                        toastDescriptorOverride = BitwardenString.identity_name.asText(),
                    )
                }
            }

        @Test
        fun `on CopyCompanyClick should copy company to clipboard`() = runTest {
            val company = "the company name"
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyCompanyClick)
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = company,
                    toastDescriptorOverride = BitwardenString.company.asText(),
                )
            }
        }

        @Test
        fun `on CopySsnClick should copy SSN to clipboard`() = runTest {
            val ssn = "the SSN"
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopySsnClick)
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = ssn,
                    toastDescriptorOverride = BitwardenString.ssn.asText(),
                )
            }
        }

        @Test
        fun `on CopyPassportNumberClick should copy passport number to clipboard`() = runTest {
            val passportNumber = "the passport number"
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyPassportNumberClick)
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = passportNumber,
                    toastDescriptorOverride = BitwardenString.passport_number.asText(),
                )
            }
        }

        @Test
        fun `on CopyLicenseNumberClick should copy license number to clipboard`() = runTest {
            val licenseNumber = "the license number"
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyLicenseNumberClick)
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = licenseNumber,
                    toastDescriptorOverride = BitwardenString.license_number.asText(),
                )
            }
        }

        @Test
        fun `on CopyEmailClick should copy email to clipboard`() = runTest {
            val email = "the email address"
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyEmailClick)
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = email, toastDescriptorOverride = BitwardenString.email.asText(),
                )
            }
        }

        @Test
        fun `on CopyPhoneClick should copy phone to clipboard`() = runTest {
            val phone = "the phone number"
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyPhoneClick)
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = phone, toastDescriptorOverride = BitwardenString.phone.asText(),
                )
            }
        }

        @Test
        fun `on CopyAddressClick should copy address to clipboard`() = runTest {
            val address = "the address"
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyAddressClick)
            verify(exactly = 1) {
                clipboardManager.setText(
                    text = address,
                    toastDescriptorOverride = BitwardenString.address.asText(),
                )
            }
        }
    }

    @Nested
    inner class VaultItemFlow {
        @BeforeEach
        fun setup() {
            mutableUserStateFlow.value = DEFAULT_USER_STATE
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
        }

        @Test
        fun `on VaultDataReceive with Loading should update the dialog state to loading`() {
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Loading

            assertEquals(
                DEFAULT_STATE.copy(viewState = VaultItemState.ViewState.Loading),
                viewModel.stateFlow.value,
            )
        }

        @Test
        fun `on VaultDataReceive with Loaded and nonnull data should update the ViewState`() {
            val viewState = mockk<VaultItemState.ViewState>()
            every { mockCipherView.organizationId } returns "mockOrganizationId"
            every { mockCipherView.collectionIds } returns listOf("mockId-1")
            every { mockCipherView.folderId } returns "mockId-1"
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(
                        VaultItemLocation.Organization("mockOrganizationName"),
                        VaultItemLocation.Collection("mockName-1"),
                        VaultItemLocation.Folder("mockName-1"),
                    ),
                    hasOrganizations = true,
                )
            } returns viewState
            mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                accounts = listOf(
                    DEFAULT_USER_ACCOUNT.copy(
                        organizations = listOf(
                            Organization(
                                id = "mockOrganizationId",
                                name = "mockOrganizationName",
                                shouldManageResetPassword = false,
                                shouldUseKeyConnector = false,
                                role = OrganizationType.OWNER,
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = true,
                            ),
                        ),
                    ),
                ),
            )

            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableCollectionsStateFlow.value = DataState.Loaded(
                listOf(createMockCollectionView(number = 1)),
            )
            mutableFoldersStateFlow.value = DataState.Loaded(
                listOf(createMockFolderView(number = 1)),
            )

            assertEquals(
                DEFAULT_STATE.copy(viewState = viewState),
                viewModel.stateFlow.value,
            )
        }

        @Test
        @Suppress("MaxLineLength")
        fun `on VaultDataReceive with Loaded and nonnull false permission data should update the ViewState with cipher permissions`() {
            val viewState = mockk<VaultItemState.ViewState>()
            every { mockCipherView.organizationId } returns "mockOrganizationId"
            every { mockCipherView.collectionIds } returns listOf("mockId-1")
            every { mockCipherView.folderId } returns "mockId-1"
            every {
                mockCipherView.permissions
            } returns createMockSdkCipherPermissions(delete = false, restore = false)
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = false,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(
                        VaultItemLocation.Organization("mockOrganizationName"),
                        VaultItemLocation.Collection("mockName-1"),
                        VaultItemLocation.Folder("mockName-1"),
                    ),
                    hasOrganizations = true,
                )
            } returns viewState
            mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                accounts = listOf(
                    DEFAULT_USER_ACCOUNT.copy(
                        organizations = listOf(
                            Organization(
                                id = "mockOrganizationId",
                                name = "mockOrganizationName",
                                shouldManageResetPassword = false,
                                shouldUseKeyConnector = false,
                                role = OrganizationType.OWNER,
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = true,
                            ),
                        ),
                    ),
                ),
            )

            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableCollectionsStateFlow.value = DataState.Loaded(
                listOf(createMockCollectionView(number = 1)),
            )
            mutableFoldersStateFlow.value = DataState.Loaded(
                listOf(createMockFolderView(number = 1)),
            )

            assertEquals(
                DEFAULT_STATE.copy(viewState = viewState),
                viewModel.stateFlow.value,
            )
        }

        @Test
        @Suppress("MaxLineLength")
        fun `on VaultDataReceive with Loaded and nonnull true permission data should update the ViewState with cipher permissions`() {
            val viewState = mockk<VaultItemState.ViewState>()
            every { mockCipherView.organizationId } returns "mockOrganizationId"
            every { mockCipherView.deletedDate } returns Instant.MIN
            every { mockCipherView.collectionIds } returns listOf("mockId-1")
            every { mockCipherView.folderId } returns "mockId-1"
            every { mockCipherView.permissions } returns createMockSdkCipherPermissions()
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = true,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(
                        VaultItemLocation.Organization("mockOrganizationName"),
                        VaultItemLocation.Collection("mockName-1"),
                        VaultItemLocation.Folder("mockName-1"),
                    ),
                    hasOrganizations = true,
                )
            } returns viewState
            mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                accounts = listOf(
                    DEFAULT_USER_ACCOUNT.copy(
                        organizations = listOf(
                            Organization(
                                id = "mockOrganizationId",
                                name = "mockOrganizationName",
                                shouldManageResetPassword = false,
                                shouldUseKeyConnector = false,
                                role = OrganizationType.OWNER,
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = false,
                            ),
                        ),
                    ),
                ),
            )

            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableCollectionsStateFlow.value = DataState.Loaded(
                listOf(createMockCollectionView(number = 1)),
            )
            mutableFoldersStateFlow.value = DataState.Loaded(
                listOf(createMockFolderView(number = 1)),
            )

            assertEquals(
                DEFAULT_STATE.copy(viewState = viewState),
                viewModel.stateFlow.value,
            )
        }

        @Test
        fun `on VaultDataReceive with Loaded and null data should update the ViewState to Error`() {
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = VaultItemState.ViewState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

        @Test
        fun `on VaultDataReceive with Pending and nonnull data should update the ViewState`() {
            val viewState = mockk<VaultItemState.ViewState>()
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns viewState
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Pending(data = mockCipherView)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            assertEquals(DEFAULT_STATE.copy(viewState = viewState), viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `on VaultDataReceive with Pending and null data should update the ViewState to Error`() {
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Pending(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableFoldersStateFlow.value = DataState.Loaded(emptyList())

            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = VaultItemState.ViewState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

        @Test
        fun `on VaultDataReceive with Error and nonnull data should update the ViewState`() {
            val viewState = mockk<VaultItemState.ViewState>()
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns viewState
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Error(error = Throwable(), data = mockCipherView)

            assertEquals(DEFAULT_STATE.copy(viewState = viewState), viewModel.stateFlow.value)
        }

        @Test
        fun `on VaultDataReceive with Error and null data should update the ViewState to Error`() {
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Error(error = Throwable(), data = null)

            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = VaultItemState.ViewState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

        @Test
        fun `on VaultDataReceive with NoNetwork and nonnull data should update the ViewState`() {
            val viewState = mockk<VaultItemState.ViewState>()
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canRestore = false,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                    isIconLoadingDisabled = false,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
            } returns viewState
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.NoNetwork(data = mockCipherView)

            assertEquals(DEFAULT_STATE.copy(viewState = viewState), viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `on VaultDataReceive with NoNetwork and null data should update the ViewState to Error`() {
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.NoNetwork(data = null)

            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = VaultItemState.ViewState.Error(
                        message = BitwardenString.internet_connection_required_title
                            .asText()
                            .concat(
                                " ".asText(),
                                BitwardenString.internet_connection_required_message.asText(),
                            ),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }
    }

    @Nested
    inner class CollectionsFlow {
        @BeforeEach
        fun setup() {
            mutableUserStateFlow.value = DEFAULT_USER_STATE
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
        }
    }

    @Suppress("LongParameterList")
    private fun createViewModel(
        state: VaultItemState?,
        vaultItemId: String = VAULT_ITEM_ID,
        vaultItemCipherType: VaultItemCipherType = VaultItemCipherType.LOGIN,
        bitwardenClipboardManager: BitwardenClipboardManager = clipboardManager,
        authRepository: AuthRepository = authRepo,
        vaultRepository: VaultRepository = vaultRepo,
        fileManager: FileManager = mockFileManager,
        eventManager: OrganizationEventManager = organizationEventManager,
        tempAttachmentFile: File? = null,
        environmentRepository: EnvironmentRepository = mockEnvironmentRepository,
        settingsRepository: SettingsRepository = mockSettingsRepository,
    ): VaultItemViewModel = VaultItemViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
            set("tempAttachmentFile", tempAttachmentFile)
            every {
                toVaultItemArgs()
            } returns VaultItemArgs(vaultItemId = vaultItemId, cipherType = vaultItemCipherType)
        },
        clipboardManager = bitwardenClipboardManager,
        authRepository = authRepository,
        vaultRepository = vaultRepository,
        fileManager = fileManager,
        organizationEventManager = eventManager,
        environmentRepository = environmentRepository,
        settingsRepository = settingsRepository,
        snackbarRelayManager = snackbarRelayManager,
    )

    private fun createViewState(
        common: VaultItemState.ViewState.Content.Common = DEFAULT_COMMON,
        type: VaultItemState.ViewState.Content.ItemType = DEFAULT_LOGIN_TYPE,
    ): VaultItemState.ViewState.Content =
        VaultItemState.ViewState.Content(
            common = common,
            type = type,
        )

    private fun createTotpCodeData() =
        TotpCodeItemData(
            periodSeconds = 30,
            timeLeftSeconds = 30,
            verificationCode = "123456",
        )

    private fun setupMockUri() {
        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.mockuri.com"
    }

    companion object {
        private const val VAULT_ITEM_ID = "vault_item_id"
        private const val DEFAULT_LOGIN_PASSWORD = "password"
        private const val DEFAULT_LOGIN_USERNAME = "username"

        private val DEFAULT_STATE: VaultItemState = VaultItemState(
            vaultItemId = VAULT_ITEM_ID,
            cipherType = VaultItemCipherType.LOGIN,
            viewState = VaultItemState.ViewState.Loading,
            dialog = null,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            isIconLoadingDisabled = false,
        )

        private val DEFAULT_USER_ACCOUNT = UserState.Account(
            userId = "user_id_1",
            name = "Bit",
            email = "bitwarden@gmail.com",
            avatarColorHex = "#ff00ff",
            environment = Environment.Us,
            isPremium = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = listOf(
                Organization(
                    id = "organiationId",
                    name = "Test Organization",
                    shouldManageResetPassword = false,
                    shouldUseKeyConnector = false,
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
            isExportable = true,
        )

        private val DEFAULT_USER_STATE: UserState = UserState(
            activeUserId = "user_id_1",
            accounts = listOf(DEFAULT_USER_ACCOUNT),
        )

        private val DEFAULT_LOGIN_TYPE: VaultItemState.ViewState.Content.ItemType.Login =
            VaultItemState.ViewState.Content.ItemType.Login(
                username = DEFAULT_LOGIN_USERNAME,
                passwordData = VaultItemState.ViewState.Content.ItemType.Login.PasswordData(
                    password = DEFAULT_LOGIN_PASSWORD,
                    isVisible = false,
                    canViewPassword = true,
                ),
                uris = listOf(
                    VaultItemState.ViewState.Content.ItemType.Login.UriData(
                        uri = "www.example.com",
                        isCopyable = true,
                        isLaunchable = true,
                    ),
                ),
                passwordRevisionDate = BitwardenString
                    .password_last_updated
                    .asText("12/31/69 06:16 PM"),
                isPremiumUser = true,
                totpCodeItemData = TotpCodeItemData(
                    verificationCode = "123456",
                    timeLeftSeconds = 15,
                    periodSeconds = 30,
                ),
                fido2CredentialCreationDateText = null,
                canViewTotpCode = true,
            )

        private val DEFAULT_CARD_TYPE: VaultItemState.ViewState.Content.ItemType.Card =
            VaultItemState.ViewState.Content.ItemType.Card(
                cardholderName = "mockName",
                number = VaultItemState.ViewState.Content.ItemType.Card.NumberData(
                    number = "12345436",
                    isVisible = false,
                ),
                brand = VaultCardBrand.VISA,
                expiration = "03/2027",
                securityCode = VaultItemState.ViewState.Content.ItemType.Card.CodeData(
                    code = "987",
                    isVisible = false,
                ),
                paymentCardBrandIconData = IconData.Local(
                    BitwardenDrawable.ic_payment_card_brand_visa,
                ),
            )

        private val DEFAULT_SSH_KEY_TYPE: VaultItemState.ViewState.Content.ItemType.SshKey =
            VaultItemState.ViewState.Content.ItemType.SshKey(
                name = "mockName",
                publicKey = "mockPublicKey",
                privateKey = "mockPrivateKey",
                fingerprint = "mockFingerprint",
                showPrivateKey = false,
            )

        private val DEFAULT_IDENTITY_TYPE: VaultItemState.ViewState.Content.ItemType.Identity =
            VaultItemState.ViewState.Content.ItemType.Identity(
                username = "the username",
                identityName = "the identity name",
                company = "the company name",
                ssn = "the SSN",
                passportNumber = "the passport number",
                licenseNumber = "the license number",
                email = "the email address",
                phone = "the phone number",
                address = "the address",
            )

        private val DEFAULT_COMMON: VaultItemState.ViewState.Content.Common =
            VaultItemState.ViewState.Content.Common(
                name = "login cipher",
                created = BitwardenString.created.asText("Dec 1, 1969, 05:20 PM"),
                lastUpdated = BitwardenString.last_edited.asText("Dec 31, 1969, 06:16 PM"),
                notes = "Lots of notes",
                customFields = listOf(
                    VaultItemState.ViewState.Content.Common.Custom.TextField(
                        id = "12345",
                        name = "text",
                        value = "value",
                        isCopyable = true,
                    ),
                    VaultItemState.ViewState.Content.Common.Custom.HiddenField(
                        id = "12345",
                        name = "hidden",
                        value = "value",
                        isCopyable = true,
                        isVisible = false,
                    ),
                    VaultItemState.ViewState.Content.Common.Custom.BooleanField(
                        id = "12345",
                        name = "boolean",
                        value = true,
                    ),
                    VaultItemState.ViewState.Content.Common.Custom.LinkedField(
                        id = "12345",
                        name = "linked username",
                        vaultLinkedFieldType = VaultLinkedFieldType.USERNAME,
                    ),
                    VaultItemState.ViewState.Content.Common.Custom.LinkedField(
                        id = "12345",
                        name = "linked password",
                        vaultLinkedFieldType = VaultLinkedFieldType.PASSWORD,
                    ),
                ),
                requiresCloneConfirmation = false,
                currentCipher = createMockCipherView(number = 1),
                attachments = listOf(
                    VaultItemState.ViewState.Content.Common.AttachmentItem(
                        id = "attachment-id",
                        displaySize = "11 MB",
                        isLargeFile = true,
                        isDownloadAllowed = true,
                        url = "https://example.com",
                        title = "test.mp4",
                    ),
                ),
                canDelete = true,
                canRestore = false,
                canAssignToCollections = true,
                canEdit = true,
                favorite = false,
                passwordHistoryCount = 1,
                iconData = IconData.Local(BitwardenDrawable.ic_globe),
                relatedLocations = persistentListOf(),
                hasOrganizations = true,
            )

        private val DEFAULT_VIEW_STATE: VaultItemState.ViewState.Content =
            VaultItemState.ViewState.Content(
                common = DEFAULT_COMMON,
                type = DEFAULT_LOGIN_TYPE,
            )

        private val CARD_VIEW_STATE: VaultItemState.ViewState.Content =
            VaultItemState.ViewState.Content(
                common = DEFAULT_COMMON,
                type = DEFAULT_CARD_TYPE,
            )

        private val SSH_KEY_VIEW_STATE: VaultItemState.ViewState.Content =
            VaultItemState.ViewState.Content(
                common = DEFAULT_COMMON,
                type = DEFAULT_SSH_KEY_TYPE,
            )

        private val IDENTITY_VIEW_STATE: VaultItemState.ViewState.Content =
            VaultItemState.ViewState.Content(
                common = DEFAULT_COMMON,
                type = DEFAULT_IDENTITY_TYPE,
            )
    }
}
