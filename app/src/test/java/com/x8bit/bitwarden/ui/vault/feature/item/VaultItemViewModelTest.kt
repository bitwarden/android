package com.x8bit.bitwarden.ui.vault.feature.item

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.CollectionView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.manager.FileManager
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.DownloadAttachmentResult
import com.x8bit.bitwarden.data.vault.repository.model.RestoreCipherResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.vault.feature.item.model.TotpCodeItemData
import com.x8bit.bitwarden.ui.vault.feature.item.util.createCommonContent
import com.x8bit.bitwarden.ui.vault.feature.item.util.createLoginContent
import com.x8bit.bitwarden.ui.vault.feature.item.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.util.createVerificationCodeItem
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
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

    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val authRepo: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val vaultRepo: VaultRepository = mockk {
        every { getAuthCodeFlow(VAULT_ITEM_ID) } returns mutableAuthCodeItemFlow
        every { getVaultItemStateFlow(VAULT_ITEM_ID) } returns mutableVaultItemFlow
        every { collectionsStateFlow } returns mutableCollectionsStateFlow
    }

    private val mockFileManager: FileManager = mockk()

    private val organizationEventManager = mockk<OrganizationEventManager> {
        every { trackEvent(event = any()) } just runs
    }
    private val mockCipherView = mockk<CipherView> {
        every { collectionIds } returns emptyList()
    }

    @BeforeEach
    fun setup() {
        mockkStatic(CipherView::toViewState)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(CipherView::toViewState)
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
                    message = R.string.loading.asText(),
                ),
            )
            val viewModel = createViewModel(state = initialState)
            assertEquals(initialState, viewModel.stateFlow.value)

            viewModel.trySendAction(VaultItemAction.Common.DismissDialogClick)
            assertEquals(initialState.copy(dialog = null), viewModel.stateFlow.value)
        }

        @Test
        fun `DeleteClick should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns DEFAULT_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.Common.DeleteClick)
                assertEquals(
                    loginState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            action = PasswordRepromptAction.DeleteClick,
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                }
            }

        @Test
        fun `DeleteClick should update state when re-prompt is not required`() =
            runTest {
                val loginState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON
                        .copy(requiresReprompt = false),
                )
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns loginState

                val expected = DEFAULT_STATE.copy(
                    viewState = DEFAULT_VIEW_STATE.copy(
                        common = DEFAULT_COMMON.copy(
                            requiresReprompt = false,
                        ),
                    ),
                    dialog = VaultItemState.DialogState.DeleteConfirmationPrompt(
                        R.string.do_you_really_want_to_soft_delete_cipher.asText(),
                    ),
                )

                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                viewModel.trySendAction(VaultItemAction.Common.DeleteClick)
                assertEquals(expected, viewModel.stateFlow.value)
            }

        @Suppress("MaxLineLength")
        @Test
        fun `DeleteClick should update state when re-prompt is not required and it is a hard delete`() =
            runTest {
                val loginState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON
                        .copy(
                            requiresReprompt = false,
                            currentCipher = DEFAULT_COMMON
                                .currentCipher
                                ?.copy(deletedDate = Instant.MIN),
                        ),
                )

                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns loginState

                val expected = DEFAULT_STATE.copy(
                    viewState = loginState,
                    dialog = VaultItemState.DialogState.DeleteConfirmationPrompt(
                        R.string.do_you_really_want_to_permanently_delete_cipher.asText(),
                    ),
                )

                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                viewModel.trySendAction(VaultItemAction.Common.DeleteClick)
                assertEquals(expected, viewModel.stateFlow.value)
            }

        @Test
        @Suppress("MaxLineLength")
        fun `ConfirmDeleteClick with DeleteCipherResult Success should should ShowToast and NavigateBack`() =
            runTest {
                val loginViewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                )
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns loginViewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

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
                        VaultItemEvent.ShowToast(R.string.item_soft_deleted.asText()),
                        awaitItem(),
                    )
                    assertEquals(
                        VaultItemEvent.NavigateBack,
                        awaitItem(),
                    )
                }
            }

        @Test
        @Suppress("MaxLineLength")
        fun `ConfirmDeleteClick with DeleteCipherResult Failure should should Show generic error`() =
            runTest {
                val loginViewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                )
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns loginViewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                val viewModel = createViewModel(state = DEFAULT_STATE)
                coEvery {
                    vaultRepo.softDeleteCipher(
                        cipherId = VAULT_ITEM_ID,
                        cipherView = createMockCipherView(number = 1),
                    )
                } returns DeleteCipherResult.Error

                viewModel.trySendAction(VaultItemAction.Common.ConfirmDeleteClick)

                assertEquals(
                    DEFAULT_STATE.copy(
                        viewState = loginViewState,
                        dialog = VaultItemState.DialogState.Generic(
                            message = R.string.generic_error_message.asText(),
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
                            requiresReprompt = false,
                            currentCipher = DEFAULT_COMMON
                                .currentCipher
                                ?.copy(deletedDate = Instant.MIN),
                        ),
                )
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns loginViewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                val viewModel = createViewModel(state = DEFAULT_STATE)
                coEvery {
                    vaultRepo.hardDeleteCipher(
                        cipherId = VAULT_ITEM_ID,
                    )
                } returns DeleteCipherResult.Success

                viewModel.trySendAction(VaultItemAction.Common.ConfirmDeleteClick)

                viewModel.eventFlow.test {
                    assertEquals(
                        VaultItemEvent.ShowToast(R.string.item_deleted.asText()),
                        awaitItem(),
                    )
                    assertEquals(
                        VaultItemEvent.NavigateBack,
                        awaitItem(),
                    )
                }
                coVerify { vaultRepo.hardDeleteCipher(cipherId = VAULT_ITEM_ID) }
            }

        @Test
        fun `on RestoreItemClick should prompt for master password when required`() = runTest {
            every {
                mockCipherView.toViewState(
                    previousState = any(),
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    totpCodeItemData = createTotpCodeData(),
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns DEFAULT_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = createVerificationCodeItem())
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
            val viewModel = createViewModel(state = loginState)
            assertEquals(loginState, viewModel.stateFlow.value)

            viewModel.trySendAction(VaultItemAction.Common.RestoreVaultItemClick)
            assertEquals(
                loginState.copy(
                    dialog = VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.RestoreItemClick,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `on RestoreItemClick when no need to prompt for master password updates pendingCipher state correctly`() =
            runTest {
                val viewState =
                    DEFAULT_VIEW_STATE.copy(common = DEFAULT_COMMON.copy(requiresReprompt = false))
                every {
                    mockCipherView.toViewState(
                        previousState = any(),
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns viewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
                val loginState = DEFAULT_STATE.copy(viewState = viewState)
                val viewModel = createViewModel(state = loginState)
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
        fun `ConfirmRestoreClick with RestoreCipherResult Success should should ShowToast and NavigateBack`() =
            runTest {
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns DEFAULT_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(
                    data = createVerificationCodeItem(),
                )
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

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
                        VaultItemEvent.ShowToast(R.string.item_restored.asText()),
                        awaitItem(),
                    )
                    assertEquals(
                        VaultItemEvent.NavigateBack,
                        awaitItem(),
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
                    hasMasterPassword = true,
                    totpCodeItemData = createTotpCodeData(),
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns DEFAULT_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = createVerificationCodeItem())
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

            val viewModel = createViewModel(state = DEFAULT_STATE)
            coEvery {
                vaultRepo.restoreCipher(
                    cipherId = VAULT_ITEM_ID,
                    cipherView = createMockCipherView(number = 1),
                )
            } returns RestoreCipherResult.Error

            viewModel.trySendAction(VaultItemAction.Common.ConfirmRestoreClick)

            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = DEFAULT_VIEW_STATE,
                    dialog = VaultItemState.DialogState.Generic(
                        message = R.string.generic_error_message.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

        @Test
        fun `on EditClick should do nothing when ViewState is not Content`() = runTest {
            val initialState = DEFAULT_STATE
            val viewModel = createViewModel(state = initialState)

            assertEquals(initialState, viewModel.stateFlow.value)
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemAction.Common.EditClick)
                expectNoEvents()
            }
            assertEquals(initialState, viewModel.stateFlow.value)
        }

        @Test
        fun `on EditClick should prompt for master password when required`() = runTest {
            every {
                mockCipherView.toViewState(
                    previousState = any(),
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    totpCodeItemData = createTotpCodeData(),
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns DEFAULT_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = createVerificationCodeItem())
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
            val viewModel = createViewModel(state = loginState)
            assertEquals(loginState, viewModel.stateFlow.value)

            viewModel.trySendAction(VaultItemAction.Common.EditClick)
            assertEquals(
                loginState.copy(
                    dialog = VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.EditClick,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

        @Test
        fun `on EditClick should navigate password is not required`() = runTest {
            val loginViewState = createViewState(
                common = DEFAULT_COMMON.copy(requiresReprompt = false),
            )
            every {
                mockCipherView.toViewState(
                    previousState = any(),
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns loginViewState
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
            val viewModel = createViewModel(state = loginState)

            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemAction.Common.EditClick)
                verify {
                    mockCipherView.toViewState(
                        previousState = loginViewState,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                }
                assertEquals(
                    VaultItemEvent.NavigateToAddEdit(
                        itemId = VAULT_ITEM_ID,
                        isClone = false,
                    ),
                    awaitItem(),
                )
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `on MasterPasswordSubmit should disabled required prompt when validatePassword success with valid password`() =
            runTest {
                val loginViewState = createViewState(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                )
                every {
                    mockCipherView.toViewState(
                        previousState = any(),
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true, totpCodeItemData = null,
                    )
                } returns loginViewState

                val password = "password"
                coEvery {
                    authRepo.validatePassword(password)
                } returns ValidatePasswordResult.Success(isValid = true)
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
                val viewModel = createViewModel(state = loginState)

                viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                    assertEquals(loginState, stateFlow.awaitItem())
                    viewModel.trySendAction(
                        VaultItemAction.Common.MasterPasswordSubmit(
                            masterPassword = password,
                            action = PasswordRepromptAction.EditClick,
                        ),
                    )
                    assertEquals(
                        loginState.copy(
                            dialog = VaultItemState.DialogState.Loading(
                                message = R.string.loading.asText(),
                            ),
                        ),
                        stateFlow.awaitItem(),
                    )
                    assertEquals(
                        loginState.copy(
                            viewState = loginViewState.copy(
                                common = DEFAULT_COMMON.copy(requiresReprompt = false),
                            ),
                        ),
                        stateFlow.awaitItem(),
                    )
                    assertEquals(
                        VaultItemEvent.NavigateToAddEdit(
                            itemId = DEFAULT_STATE.vaultItemId,
                            isClone = false,
                        ),
                        eventFlow.awaitItem(),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on MasterPasswordSubmit should show incorrect password dialog when validatePassword success with invalid password`() =
            runTest {
                val loginViewState = createViewState(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                )
                every {
                    mockCipherView.toViewState(
                        previousState = any(),
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true, totpCodeItemData = null,
                    )
                } returns loginViewState

                val password = "password"
                coEvery {
                    authRepo.validatePassword(password)
                } returns ValidatePasswordResult.Success(isValid = false)
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
                val viewModel = createViewModel(state = loginState)

                viewModel.stateFlow.test {
                    assertEquals(loginState, awaitItem())
                    viewModel.trySendAction(
                        VaultItemAction.Common.MasterPasswordSubmit(
                            masterPassword = password,
                            action = PasswordRepromptAction.DeleteClick,
                        ),
                    )
                    assertEquals(
                        loginState.copy(
                            dialog = VaultItemState.DialogState.Loading(
                                message = R.string.loading.asText(),
                            ),
                        ),
                        awaitItem(),
                    )
                    assertEquals(
                        loginState.copy(
                            dialog = VaultItemState.DialogState.Generic(
                                message = R.string.invalid_master_password.asText(),
                            ),
                        ),
                        awaitItem(),
                    )
                }
            }

        @Test
        fun `on MasterPasswordSubmit should show error dialog when validatePassword Error`() =
            runTest {
                val loginViewState = createViewState(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                )
                every {
                    mockCipherView.toViewState(
                        previousState = any(),
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true, totpCodeItemData = null,
                    )
                } returns loginViewState

                val password = "password"
                coEvery {
                    authRepo.validatePassword(password)
                } returns ValidatePasswordResult.Error
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
                val viewModel = createViewModel(state = loginState)

                viewModel.stateFlow.test {
                    assertEquals(loginState, awaitItem())
                    viewModel.trySendAction(
                        VaultItemAction.Common.MasterPasswordSubmit(
                            masterPassword = password,
                            action = PasswordRepromptAction.DeleteClick,
                        ),
                    )
                    assertEquals(
                        loginState.copy(
                            dialog = VaultItemState.DialogState.Loading(
                                message = R.string.loading.asText(),
                            ),
                        ),
                        awaitItem(),
                    )
                    assertEquals(
                        loginState.copy(
                            dialog = VaultItemState.DialogState.Generic(
                                message = R.string.generic_error_message.asText(),
                            ),
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

        @Suppress("MaxLineLength")
        @Test
        fun `on CopyCustomHiddenFieldClick should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true, totpCodeItemData = null,
                    )
                } returns DEFAULT_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.Common.CopyCustomHiddenFieldClick("field"))
                assertEquals(
                    loginState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            action = PasswordRepromptAction.CopyClick(value = "field"),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true, totpCodeItemData = null,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CopyCustomHiddenFieldClick should call setText on ClipboardManager when re-prompt is not required`() {
            val field = "field"
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    canDelete = true,
                    canAssignToCollections = true, totpCodeItemData = null,
                )
            } returns createViewState(common = DEFAULT_COMMON.copy(requiresReprompt = false))
            every { clipboardManager.setText(text = field) } just runs

            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.Common.CopyCustomHiddenFieldClick(field))

            verify(exactly = 1) {
                clipboardManager.setText(text = field)
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    canDelete = true,
                    canAssignToCollections = true, totpCodeItemData = null,
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

        @Suppress("MaxLineLength")
        @Test
        fun `on HiddenFieldVisibilityClicked should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
                val field = VaultItemState.ViewState.Content.Common.Custom.HiddenField(
                    name = "hidden",
                    value = "value",
                    isCopyable = true,
                    isVisible = false,
                )
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true, totpCodeItemData = null,
                    )
                } returns DEFAULT_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(
                    VaultItemAction.Common.HiddenFieldVisibilityClicked(
                        field = field,
                        isVisible = true,
                    ),
                )
                assertEquals(
                    loginState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            action = PasswordRepromptAction.ViewHiddenFieldClicked(
                                field = field,
                                isVisible = true,
                            ),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true, totpCodeItemData = null,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on HiddenFieldVisibilityClicked should update hidden field visibility when re-prompt is not required`() =
            runTest {
                val hiddenField =
                    VaultItemState.ViewState.Content.Common.Custom.HiddenField(
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
                        requiresReprompt = false,
                        customFields = listOf(hiddenField),
                    ),
                    type = createLoginContent(isEmpty = true),
                )
                val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns loginViewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

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
                                requiresReprompt = false,
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
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true, totpCodeItemData = null,
                    )
                    organizationEventManager.trackEvent(
                        event = OrganizationEvent.CipherClientToggledHiddenFieldVisible(
                            cipherId = VAULT_ITEM_ID,
                        ),
                    )
                }
            }

        @Test
        fun `on AttachmentsClick should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true, totpCodeItemData = null,
                    )
                } returns DEFAULT_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.Common.AttachmentsClick)
                assertEquals(
                    loginState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            action = PasswordRepromptAction.AttachmentsClick,
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true, totpCodeItemData = null,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on AttachmentsClick should emit NavigateToAttachments when re-prompt is not required`() =
            runTest {
                val loginViewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                )
                val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true, totpCodeItemData = null,
                    )
                } returns loginViewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(loginState, viewModel.stateFlow.value)

                viewModel.eventFlow.test {
                    viewModel.trySendAction(VaultItemAction.Common.AttachmentsClick)
                    assertEquals(
                        VaultItemEvent.NavigateToAttachments(itemId = VAULT_ITEM_ID),
                        awaitItem(),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CloneClick should show confirmation when cipher contains a passkey`() = runTest {
            val loginViewState = DEFAULT_VIEW_STATE.copy(
                common = DEFAULT_COMMON.copy(
                    requiresReprompt = false,
                    requiresCloneConfirmation = true,
                ),
            )
            val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    canDelete = true,
                    canAssignToCollections = true, totpCodeItemData = null,
                )
            } returns loginViewState
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

            assertEquals(loginState, viewModel.stateFlow.value)

            viewModel.trySendAction(VaultItemAction.Common.CloneClick)

            assertEquals(
                loginState.copy(
                    dialog = VaultItemState.DialogState.Fido2CredentialCannotBeCopiedConfirmationPrompt(
                        R.string.the_passkey_will_not_be_copied_to_the_cloned_item_do_you_want_to_continue_cloning_this_item.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )

            verify(exactly = 1) {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    canDelete = true,
                    canAssignToCollections = true, totpCodeItemData = null,
                )
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `on CloneClick should show confirmation before re-prompt when both are required`() {
            val loginViewState = DEFAULT_VIEW_STATE.copy(
                common = DEFAULT_COMMON.copy(
                    requiresReprompt = true,
                    requiresCloneConfirmation = true,
                ),
            )
            val loginState = DEFAULT_STATE.copy(
                viewState = loginViewState,
            )
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    canDelete = true,
                    canAssignToCollections = true, totpCodeItemData = null,
                )
            } returns loginViewState
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

            assertEquals(loginState, viewModel.stateFlow.value)
            viewModel.trySendAction(VaultItemAction.Common.CloneClick)

            // Assert clone confirmation dialog is triggered
            assertEquals(
                loginState.copy(
                    dialog = VaultItemState.DialogState.Fido2CredentialCannotBeCopiedConfirmationPrompt(
                        R.string.the_passkey_will_not_be_copied_to_the_cloned_item_do_you_want_to_continue_cloning_this_item.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )

            // Simulate confirmation click.
            viewModel.trySendAction(VaultItemAction.Common.ConfirmCloneWithoutFido2CredentialClick)

            // Assert MP dialog is triggered.
            assertEquals(
                loginState.copy(
                    dialog = VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.CloneClick,
                    ),
                    viewState = loginViewState.copy(
                        common = loginViewState.common.copy(
                            requiresCloneConfirmation = false,
                        ),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

        @Test
        fun `on CloneClick should show password dialog when re-prompt is required`() = runTest {
            val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    canDelete = true,
                    canAssignToCollections = true, totpCodeItemData = null,
                )
            } returns DEFAULT_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

            assertEquals(loginState, viewModel.stateFlow.value)
            viewModel.trySendAction(VaultItemAction.Common.CloneClick)
            assertEquals(
                loginState.copy(
                    dialog = VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.CloneClick,
                    ),
                ),
                viewModel.stateFlow.value,
            )

            verify(exactly = 1) {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    canDelete = true,
                    canAssignToCollections = true, totpCodeItemData = null,
                )
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `on CloneClick should emit NavigateToAddEdit when re-prompt is not required`() =
            runTest {
                val loginViewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                )
                val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true, totpCodeItemData = null,
                    )
                } returns loginViewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(loginState, viewModel.stateFlow.value)

                viewModel.eventFlow.test {
                    viewModel.trySendAction(VaultItemAction.Common.CloneClick)
                    assertEquals(
                        VaultItemEvent.NavigateToAddEdit(
                            itemId = VAULT_ITEM_ID,
                            isClone = true,
                        ),
                        awaitItem(),
                    )
                }
            }

        @Test
        fun `on MoveToOrganizationClick should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true, totpCodeItemData = null,
                    )
                } returns DEFAULT_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.Common.MoveToOrganizationClick)
                assertEquals(
                    loginState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            action = PasswordRepromptAction.MoveToOrganizationClick,
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true, totpCodeItemData = null,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on MoveToOrganizationClick should emit NavigateToMoveToOrganization when re-prompt is not required`() =
            runTest {
                val loginViewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                )
                val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true,
                        totpCodeItemData = null,
                    )
                } returns loginViewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

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

        @Test
        fun `on AttachmentDownloadClick should prompt for password if required`() =
            runTest {
                val loginViewState = createViewState(
                    common = DEFAULT_COMMON.copy(requiresReprompt = true),
                )
                every {
                    mockCipherView.toViewState(
                        previousState = any(),
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true,
                        totpCodeItemData = null,
                    )
                } returns loginViewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
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
                            dialog = VaultItemState.DialogState.MasterPasswordDialog(
                                action = PasswordRepromptAction.AttachmentDownloadClick(attachment),
                            ),
                        ),
                        awaitItem(),
                    )
                }

                coVerify(exactly = 0) {
                    vaultRepo.downloadAttachment(any(), any())
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on AttachmentDownloadClick should show loading dialog, attempt to download an attachment, and display an error dialog on failure`() =
            runTest {
                val loginViewState = createViewState(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                )
                every {
                    mockCipherView.toViewState(
                        previousState = any(),
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true,
                        totpCodeItemData = null,
                    )
                } returns loginViewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
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

                coEvery {
                    vaultRepo.downloadAttachment(any(), any())
                } returns DownloadAttachmentResult.Failure

                viewModel.stateFlow.test {
                    assertEquals(
                        loginState,
                        awaitItem(),
                    )

                    viewModel.trySendAction(
                        VaultItemAction.Common.AttachmentDownloadClick(attachment),
                    )

                    assertEquals(
                        loginState.copy(dialog = VaultItemState.DialogState.Loading(R.string.downloading.asText())),
                        awaitItem(),
                    )

                    assertEquals(
                        loginState.copy(
                            dialog = VaultItemState.DialogState.Generic(
                                R.string.unable_to_download_file.asText(),
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
                val loginViewState = createViewState(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                )
                every {
                    mockCipherView.toViewState(
                        previousState = any(),
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true,
                        totpCodeItemData = null,
                    )
                } returns loginViewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
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
                                R.string.downloading.asText(),
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
        fun `on AttachmentFileLocationReceive success should hide loading dialog, copy file, delete file, and show toast`() =
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
                        VaultItemEvent.ShowToast(R.string.save_attachment_success.asText()),
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
                                R.string.unable_to_save_attachment.asText(),
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
                        R.string.unable_to_save_attachment.asText(),
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
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns DEFAULT_VIEW_STATE

            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

            val notes = "Lots of notes"
            every { clipboardManager.setText(text = notes) } just runs

            viewModel.trySendAction(VaultItemAction.Common.CopyNotesClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = notes)
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
                    hasMasterPassword = true,
                    canDelete = true,
                    canAssignToCollections = true,
                    totpCodeItemData = createTotpCodeData(),
                )
            } returns DEFAULT_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = createVerificationCodeItem())
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

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
                            message = R.string.loading.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    loginState.copy(
                        dialog = VaultItemState.DialogState.Generic(
                            message = R.string.password_exposed.asText(breachCount),
                        ),
                    ),
                    awaitItem(),
                )
            }

            verify(exactly = 1) {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    canDelete = true,
                    canAssignToCollections = true,
                    totpCodeItemData = createTotpCodeData(),
                )
            }
            coVerify(exactly = 1) {
                authRepo.getPasswordBreachCount(password = DEFAULT_LOGIN_PASSWORD)
            }
        }

        @Test
        fun `on CopyPasswordClick should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true,
                        totpCodeItemData = createTotpCodeData(),
                    )
                } returns DEFAULT_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyPasswordClick)
                assertEquals(
                    loginState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            action = PasswordRepromptAction.CopyClick(
                                value = DEFAULT_LOGIN_PASSWORD,
                            ),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        canDelete = true,
                        canAssignToCollections = true,
                        totpCodeItemData = createTotpCodeData(),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CopyPasswordClick should call setText on the ClipboardManager when re-prompt is not required`() {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    canDelete = true,
                    canAssignToCollections = true,
                    totpCodeItemData = createTotpCodeData(),
                )
            } returns createViewState(common = DEFAULT_COMMON.copy(requiresReprompt = false))
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value =
                DataState.Loaded(data = createVerificationCodeItem())
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

            every { clipboardManager.setText(text = DEFAULT_LOGIN_PASSWORD) } just runs

            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyPasswordClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = DEFAULT_LOGIN_PASSWORD)
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    canDelete = true,
                    canAssignToCollections = true,
                    totpCodeItemData = createTotpCodeData(),
                )
            }
        }

        @Test
        fun `on CopyTotpClick should call setText on the ClipboardManager`() {
            every { clipboardManager.setText(text = "123456") } just runs
            mutableVaultItemFlow.value = DataState.Loaded(
                data = createMockCipherView(1),
            )
            mutableAuthCodeItemFlow.value = DataState.Loaded(
                data = createVerificationCodeItem(),
            )
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyTotpClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = "123456")
            }
        }

        @Test
        fun `on CopyUriClick should call setText on ClipboardManager`() {
            val uri = "uri"
            every { clipboardManager.setText(text = uri) } just runs

            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyUriClick(uri))

            verify(exactly = 1) { clipboardManager.setText(text = uri) }
        }

        @Test
        fun `on CopyUsernameClick should call setText on ClipboardManager`() {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    canDelete = true,
                    canAssignToCollections = true,
                    totpCodeItemData = createTotpCodeData(),
                )
            } returns createViewState(common = DEFAULT_COMMON.copy(requiresReprompt = false))
            every { clipboardManager.setText(text = DEFAULT_LOGIN_USERNAME) } just runs
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value =
                DataState.Loaded(data = createVerificationCodeItem())
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyUsernameClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = DEFAULT_LOGIN_USERNAME)
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    totpCodeItemData = createTotpCodeData(),
                    canDelete = true,
                    canAssignToCollections = true,
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
        fun `on PasswordHistoryClick should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns DEFAULT_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.ItemType.Login.PasswordHistoryClick)
                assertEquals(
                    loginState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            action = PasswordRepromptAction.PasswordHistoryClick,
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on PasswordHistoryClick should emit NavigateToPasswordHistory when re-prompt is not required`() =
            runTest {
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                }
                    .returns(
                        createViewState(
                            common = DEFAULT_COMMON.copy(requiresReprompt = false),
                        ),
                    )
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                viewModel.eventFlow.test {
                    viewModel.trySendAction(VaultItemAction.ItemType.Login.PasswordHistoryClick)
                    assertEquals(
                        VaultItemEvent.NavigateToPasswordHistory(VAULT_ITEM_ID),
                        awaitItem(),
                    )
                }

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on PasswordVisibilityClicked should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns DEFAULT_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(
                    VaultItemAction.ItemType.Login.PasswordVisibilityClicked(
                        isVisible = true,
                    ),
                )
                assertEquals(
                    loginState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            action = PasswordRepromptAction.ViewPasswordClick(isVisible = true),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on PasswordVisibilityClicked should update password visibility when re-prompt is not required`() =
            runTest {
                val loginViewState = createViewState(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                )
                val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns loginViewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(
                    VaultItemAction.ItemType.Login.PasswordVisibilityClicked(
                        true,
                    ),
                )
                assertEquals(
                    loginState.copy(
                        viewState = loginViewState.copy(
                            common = DEFAULT_COMMON.copy(requiresReprompt = false),
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
                        hasMasterPassword = true,
                        totpCodeItemData = createTotpCodeData(),
                        canDelete = true,
                        canAssignToCollections = true,
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
        fun `on CopyNumberClick should show password dialog when re-prompt is required`() =
            runTest {
                val cardState = DEFAULT_STATE.copy(viewState = CARD_VIEW_STATE)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns CARD_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(cardState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.ItemType.Card.CopyNumberClick)
                assertEquals(
                    cardState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            action = PasswordRepromptAction.CopyClick(
                                value = requireNotNull(DEFAULT_CARD_TYPE.number).number,
                            ),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CopyNumberClick should call setText on the ClipboardManager when re-prompt is not required`() {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns createViewState(
                common = DEFAULT_COMMON.copy(requiresReprompt = false),
                type = DEFAULT_CARD_TYPE,
            )
            every { clipboardManager.setText(text = "12345436") } just runs
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.ItemType.Card.CopyNumberClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = "12345436")
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            }
        }

        @Test
        fun `on NumberVisibilityClick should show password dialog when re-prompt is required`() =
            runTest {
                val cardState = DEFAULT_STATE.copy(viewState = CARD_VIEW_STATE)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns CARD_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(cardState, viewModel.stateFlow.value)
                viewModel.trySendAction(
                    VaultItemAction.ItemType.Card.NumberVisibilityClick(isVisible = true),
                )
                assertEquals(
                    cardState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            action = PasswordRepromptAction.ViewNumberClick(isVisible = true),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on NumberVisibilityClick should call trackEvent on the OrganizationEventManager and update the ViewState when re-prompt is not required`() {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns createViewState(
                common = DEFAULT_COMMON.copy(requiresReprompt = false),
                type = DEFAULT_CARD_TYPE,
            )
            every { clipboardManager.setText(text = "12345436") } just runs
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

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
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            }
        }

        @Test
        fun `on CopySecurityCodeClick should show password dialog when re-prompt is required`() =
            runTest {
                val cardState = DEFAULT_STATE.copy(viewState = CARD_VIEW_STATE)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns CARD_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(cardState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.ItemType.Card.CopySecurityCodeClick)
                assertEquals(
                    cardState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            action = PasswordRepromptAction.CopyClick(
                                value = requireNotNull(DEFAULT_CARD_TYPE.securityCode).code,
                            ),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CopySecurityCodeClick should call setText on the ClipboardManager when re-prompt is not required`() {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns createViewState(
                common = DEFAULT_COMMON.copy(requiresReprompt = false),
                type = DEFAULT_CARD_TYPE,
            )
            every { clipboardManager.setText(text = "987") } just runs
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.ItemType.Card.CopySecurityCodeClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = "987")
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            }
        }

        @Test
        fun `on CodeVisibilityClick should show password dialog when re-prompt is required`() =
            runTest {
                val cardState = DEFAULT_STATE.copy(viewState = CARD_VIEW_STATE)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns CARD_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(cardState, viewModel.stateFlow.value)
                viewModel.trySendAction(
                    VaultItemAction.ItemType.Card.CodeVisibilityClick(isVisible = true),
                )
                assertEquals(
                    cardState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            action = PasswordRepromptAction.ViewCodeClick(isVisible = true),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CodeVisibilityClick should call trackEvent on the OrganizationEventManager and update the ViewState when re-prompt is not required`() {
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns createViewState(
                common = DEFAULT_COMMON.copy(requiresReprompt = false),
                type = DEFAULT_CARD_TYPE,
            )
            every { clipboardManager.setText(text = "987") } just runs
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
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
            every { clipboardManager.setText("mockPublicKey") } just runs
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns SSH_KEY_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.ItemType.SshKey.CopyPublicKeyClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = DEFAULT_SSH_KEY_TYPE.publicKey)
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `on PrivateKeyVisibilityClick should show private key when re-prompt is not required`() =
            runTest {
                val sshKeyViewState = createViewState(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                    type = DEFAULT_SSH_KEY_TYPE,
                )
                val sshKeyState = DEFAULT_STATE.copy(viewState = sshKeyViewState)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns sshKeyViewState
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(sshKeyState, viewModel.stateFlow.value)
                viewModel.trySendAction(
                    VaultItemAction.ItemType.SshKey.PrivateKeyVisibilityClicked(
                        isVisible = true,
                    ),
                )
                assertEquals(
                    sshKeyState.copy(
                        viewState = sshKeyViewState.copy(
                            common = DEFAULT_COMMON.copy(requiresReprompt = false),
                            type = DEFAULT_SSH_KEY_TYPE.copy(showPrivateKey = true),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )
                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on PrivateKeyVisibilityClick should show password dialog when re-prompt is required`() =
            runTest {
                val sshKeyState = DEFAULT_STATE.copy(viewState = SSH_KEY_VIEW_STATE)
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns SSH_KEY_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                assertEquals(sshKeyState, viewModel.stateFlow.value)
                viewModel.trySendAction(
                    VaultItemAction.ItemType.SshKey.PrivateKeyVisibilityClicked(
                        isVisible = true,
                    ),
                )
                assertEquals(
                    sshKeyState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            PasswordRepromptAction.ViewPrivateKeyClicked(isVisible = true),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )
                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `onPrivateKeyCopyClick should copy private key to clipboard when re-prompt is not required`() =
            runTest {
                every { clipboardManager.setText("mockPrivateKey") } just runs
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns createViewState(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                    type = DEFAULT_SSH_KEY_TYPE,
                )
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                viewModel.trySendAction(VaultItemAction.ItemType.SshKey.CopyPrivateKeyClick)

                verify(exactly = 1) {
                    clipboardManager.setText(text = DEFAULT_SSH_KEY_TYPE.privateKey)
                }
            }

        @Test
        fun `onPrivateKeyCopyClick should show password dialog when re-prompt is required`() =
            runTest {
                val sshKeyState = DEFAULT_STATE.copy(viewState = SSH_KEY_VIEW_STATE)
                every { clipboardManager.setText("mockPrivateKey") } just runs
                every {
                    mockCipherView.toViewState(
                        previousState = null,
                        isPremiumUser = true,
                        hasMasterPassword = true,
                        totpCodeItemData = null,
                        canDelete = true,
                        canAssignToCollections = true,
                    )
                } returns SSH_KEY_VIEW_STATE
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
                mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

                viewModel.trySendAction(VaultItemAction.ItemType.SshKey.CopyPrivateKeyClick)

                assertEquals(
                    sshKeyState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            action = PasswordRepromptAction.CopyClick(
                                value = DEFAULT_SSH_KEY_TYPE.privateKey,
                            ),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )
                verify(exactly = 0) {
                    clipboardManager.setText(text = DEFAULT_SSH_KEY_TYPE.privateKey)
                }
            }

        @Test
        fun `on CopyFingerprintClick should copy fingerprint to clipboard`() = runTest {
            every { clipboardManager.setText("mockFingerprint") } just runs
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns SSH_KEY_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

            viewModel.trySendAction(VaultItemAction.ItemType.SshKey.CopyFingerprintClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = DEFAULT_SSH_KEY_TYPE.fingerprint)
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
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns IDENTITY_VIEW_STATE
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())
        }

        @Test
        fun `on CopyIdentityNameClick should copy fingerprint to clipboard`() =
            runTest {
                val username = "the username"
                every { clipboardManager.setText(text = username) } just runs

                viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyUsernameClick)

                verify(exactly = 1) {
                    clipboardManager.setText(text = username)
                }
            }

        @Test
        fun `on CopyUsernameClick should copy fingerprint to clipboard`() =
            runTest {
                val identityName = "the identity name"
                every { clipboardManager.setText(text = identityName) } just runs

                viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyIdentityNameClick)

                verify(exactly = 1) {
                    clipboardManager.setText(text = identityName)
                }
            }

        @Test
        fun `on CopyCompanyClick should copy company to clipboard`() = runTest {
            val company = "the company name"
            every { clipboardManager.setText(text = company) } just runs

            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyCompanyClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = company)
            }
        }

        @Test
        fun `on CopySsnClick should copy SSN to clipboard`() = runTest {
            val ssn = "the SSN"
            every { clipboardManager.setText(text = ssn) } just runs

            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopySsnClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = ssn)
            }
        }

        @Test
        fun `on CopyPassportNumberClick should copy passport number to clipboard`() = runTest {
            val passportNumber = "the passport number"
            every { clipboardManager.setText(text = passportNumber) } just runs

            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyPassportNumberClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = passportNumber)
            }
        }

        @Test
        fun `on CopyLicenseNumberClick should copy license number to clipboard`() = runTest {
            val licenseNumber = "the license number"
            every { clipboardManager.setText(text = licenseNumber) } just runs

            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyLicenseNumberClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = licenseNumber)
            }
        }

        @Test
        fun `on CopyEmailClick should copy email to clipboard`() = runTest {
            val email = "the email address"
            every { clipboardManager.setText(text = email) } just runs

            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyEmailClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = email)
            }
        }

        @Test
        fun `on CopyPhoneClick should copy phone to clipboard`() = runTest {
            val phone = "the phone number"
            every { clipboardManager.setText(text = phone) } just runs

            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyPhoneClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = phone)
            }
        }

        @Test
        fun `on CopyAddressClick should copy address to clipboard`() = runTest {
            val address = "the address"
            every { clipboardManager.setText(text = address) } just runs

            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyAddressClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = address)
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
            every {
                mockCipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns viewState
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

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

            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = VaultItemState.ViewState.Error(
                        message = R.string.generic_error_message.asText(),
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
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
                )
            } returns viewState
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Pending(data = mockCipherView)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

            assertEquals(DEFAULT_STATE.copy(viewState = viewState), viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `on VaultDataReceive with Pending and null data should update the ViewState to Error`() {
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Pending(data = null)
            mutableCollectionsStateFlow.value = DataState.Loaded(emptyList())

            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = VaultItemState.ViewState.Error(
                        message = R.string.generic_error_message.asText(),
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
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
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
                        message = R.string.generic_error_message.asText(),
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
                    hasMasterPassword = true,
                    totpCodeItemData = null,
                    canDelete = true,
                    canAssignToCollections = true,
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
                        message = R.string.internet_connection_required_title
                            .asText()
                            .concat(
                                " ".asText(),
                                R.string.internet_connection_required_message.asText(),
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
        bitwardenClipboardManager: BitwardenClipboardManager = clipboardManager,
        authRepository: AuthRepository = authRepo,
        vaultRepository: VaultRepository = vaultRepo,
        fileManager: FileManager = mockFileManager,
        eventManager: OrganizationEventManager = organizationEventManager,
        tempAttachmentFile: File? = null,
    ): VaultItemViewModel = VaultItemViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
            set("vault_item_id", vaultItemId)
            set("tempAttachmentFile", tempAttachmentFile)
        },
        clipboardManager = bitwardenClipboardManager,
        authRepository = authRepository,
        vaultRepository = vaultRepository,
        fileManager = fileManager,
        organizationEventManager = eventManager,
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
            totpCode = "mockTotp-1",
        )

    companion object {
        private const val VAULT_ITEM_ID = "vault_item_id"
        private const val DEFAULT_LOGIN_PASSWORD = "password"
        private const val DEFAULT_LOGIN_USERNAME = "username"

        private val DEFAULT_STATE: VaultItemState = VaultItemState(
            vaultItemId = VAULT_ITEM_ID,
            viewState = VaultItemState.ViewState.Loading,
            dialog = null,
        )

        private val DEFAULT_USER_STATE: UserState = UserState(
            activeUserId = "user_id_1",
            accounts = listOf(
                UserState.Account(
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
                    organizations = emptyList(),
                    needsMasterPassword = false,
                    trustedDevice = null,
                    hasMasterPassword = true,
                    isUsingKeyConnector = false,
                    onboardingStatus = OnboardingStatus.COMPLETE,
                    firstTimeState = FirstTimeState(showImportLoginsCard = true),
                ),
            ),
        )

        private val DEFAULT_LOGIN_TYPE: VaultItemState.ViewState.Content.ItemType.Login =
            VaultItemState.ViewState.Content.ItemType.Login(
                passwordHistoryCount = 1,
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
                passwordRevisionDate = "12/31/69 06:16 PM",
                isPremiumUser = true,
                totpCodeItemData = TotpCodeItemData(
                    totpCode = "otpauth://totp/Example:alice@google.com" +
                        "?secret=JBSWY3DPEHPK3PXP&issuer=Example",
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
                lastUpdated = "12/31/69 06:16 PM",
                notes = "Lots of notes",
                customFields = listOf(
                    VaultItemState.ViewState.Content.Common.Custom.TextField(
                        name = "text",
                        value = "value",
                        isCopyable = true,
                    ),
                    VaultItemState.ViewState.Content.Common.Custom.HiddenField(
                        name = "hidden",
                        value = "value",
                        isCopyable = true,
                        isVisible = false,
                    ),
                    VaultItemState.ViewState.Content.Common.Custom.BooleanField(
                        name = "boolean",
                        value = true,
                    ),
                    VaultItemState.ViewState.Content.Common.Custom.LinkedField(
                        name = "linked username",
                        vaultLinkedFieldType = VaultLinkedFieldType.USERNAME,
                    ),
                    VaultItemState.ViewState.Content.Common.Custom.LinkedField(
                        name = "linked password",
                        vaultLinkedFieldType = VaultLinkedFieldType.PASSWORD,
                    ),
                ),
                requiresReprompt = true,
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
                canAssignToCollections = true,
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
