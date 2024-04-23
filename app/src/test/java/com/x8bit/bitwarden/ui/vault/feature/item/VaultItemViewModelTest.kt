package com.x8bit.bitwarden.ui.vault.feature.item

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
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

    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val authRepo: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val vaultRepo: VaultRepository = mockk {
        every { getAuthCodeFlow(VAULT_ITEM_ID) } returns mutableAuthCodeItemFlow
        every { getVaultItemStateFlow(VAULT_ITEM_ID) } returns mutableVaultItemFlow
    }

    private val mockFileManager: FileManager = mockk()

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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
                        isPremiumUser = true,
                        totpCodeItemData = null,
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

                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns loginState
                }

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

                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns loginState
                }

                val expected = DEFAULT_STATE.copy(
                    viewState = loginState,
                    dialog = VaultItemState.DialogState.DeleteConfirmationPrompt(
                        R.string.do_you_really_want_to_permanently_delete_cipher.asText(),
                    ),
                )

                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = createTotpCodeData(),
                        )
                    } returns loginViewState
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())

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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns loginViewState
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = createTotpCodeData(),
                        )
                    } returns loginViewState
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())

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
        @Suppress("MaxLineLength")
        fun `ConfirmRestoreClick with RestoreCipherResult Success should should ShowToast and NavigateBack`() =
            runTest {
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = createTotpCodeData(),
                        )
                    } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())

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
        fun `ConfirmRestoreClick with RestoreCipherResult Failure should should Show generic error`() =
            runTest {
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = createTotpCodeData(),
                        )
                    } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())
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
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(
                        isPremiumUser = true,
                        totpCodeItemData = createTotpCodeData(),
                    )
                } returns DEFAULT_VIEW_STATE
            }
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = createVerificationCodeItem())
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
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(
                        isPremiumUser = true,
                        totpCodeItemData = null,
                    )
                } returns loginViewState
            }
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
            val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
            val viewModel = createViewModel(state = loginState)

            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemAction.Common.EditClick)
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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns loginViewState
                }
                val password = "password"
                coEvery {
                    authRepo.validatePassword(password)
                } returns ValidatePasswordResult.Success(isValid = true)
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

                val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
                val viewModel = createViewModel(state = loginState)

                turbineScope {
                    val stateFlow = viewModel.stateFlow.testIn(backgroundScope)
                    val eventFlow = viewModel.eventFlow.testIn(backgroundScope)
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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns loginViewState
                }
                val password = "password"
                coEvery {
                    authRepo.validatePassword(password)
                } returns ValidatePasswordResult.Success(isValid = false)
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns loginViewState
                }
                val password = "password"
                coEvery {
                    authRepo.validatePassword(password)
                } returns ValidatePasswordResult.Error
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
            every { vaultRepo.sync() } just runs
            val viewModel = createViewModel(state = DEFAULT_STATE)

            viewModel.trySendAction(VaultItemAction.Common.RefreshClick)

            verify(exactly = 1) {
                vaultRepo.sync()
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `on CopyCustomHiddenFieldClick should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
                        isPremiumUser = true,
                        totpCodeItemData = null,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CopyCustomHiddenFieldClick should call setText on ClipboardManager when re-prompt is not required`() {
            val field = "field"
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(
                        isPremiumUser = true,
                        totpCodeItemData = null,
                    )
                } returns createViewState(common = DEFAULT_COMMON.copy(requiresReprompt = false))
            }
            every { clipboardManager.setText(text = field) } just runs

            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

            viewModel.trySendAction(VaultItemAction.Common.CopyCustomHiddenFieldClick(field))

            verify(exactly = 1) {
                clipboardManager.setText(text = field)
                mockCipherView.toViewState(
                    isPremiumUser = true,
                    totpCodeItemData = null,
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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
                        isPremiumUser = true,
                        totpCodeItemData = null,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on HiddenFieldVisibilityClicked should update hidden field visibility when re-prompt is not required`() =
            runTest {
                val hiddenField = VaultItemState.ViewState.Content.Common.Custom.HiddenField(
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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns loginViewState
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
                        isPremiumUser = true,
                        totpCodeItemData = null,
                    )
                }
            }

        @Test
        fun `on AttachmentsClick should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
                        isPremiumUser = true,
                        totpCodeItemData = null,
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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns loginViewState
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(
                        isPremiumUser = true,
                        totpCodeItemData = null,
                    )
                } returns loginViewState
            }
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
                    isPremiumUser = true,
                    totpCodeItemData = null,
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
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(
                        isPremiumUser = true,
                        totpCodeItemData = null,
                    )
                } returns loginViewState
            }
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(
                        isPremiumUser = true,
                        totpCodeItemData = null,
                    )
                } returns DEFAULT_VIEW_STATE
            }
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
                    isPremiumUser = true,
                    totpCodeItemData = null,
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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns loginViewState
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
                        isPremiumUser = true,
                        totpCodeItemData = null,
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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns loginViewState
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns loginViewState
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns loginViewState
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns loginViewState
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)
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
                    mockFileManager.deleteFile(any())
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
                    mockFileManager.deleteFile(file)
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
                    mockFileManager.deleteFile(any())
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
                    mockFileManager.deleteFile(file)
                    mockFileManager.fileToUri(uri, file)
                }
            }

        @Test
        fun `on NoAttachmentFileLocationReceive failure should show dialog`() {
            val file = mockk<File>()
            val viewModel = createViewModel(state = DEFAULT_STATE, tempAttachmentFile = file)

            coEvery {
                mockFileManager.deleteFile(any())
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

            coVerify { mockFileManager.deleteFile(file) }
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
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(
                        isPremiumUser = true,
                        totpCodeItemData = createTotpCodeData(),
                    )
                } returns DEFAULT_VIEW_STATE
            }
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value =
                DataState.Loaded(data = createVerificationCodeItem())

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
                    isPremiumUser = true,
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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = createTotpCodeData(),
                        )
                    } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())

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
                        isPremiumUser = true,
                        totpCodeItemData = createTotpCodeData(),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CopyPasswordClick should call setText on the ClipboardManager when re-prompt is not required`() {
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(
                        isPremiumUser = true,
                        totpCodeItemData = createTotpCodeData(),
                    )
                } returns createViewState(common = DEFAULT_COMMON.copy(requiresReprompt = false))
            }
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value =
                DataState.Loaded(data = createVerificationCodeItem())

            every { clipboardManager.setText(text = DEFAULT_LOGIN_PASSWORD) } just runs

            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyPasswordClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = DEFAULT_LOGIN_PASSWORD)
                mockCipherView.toViewState(
                    isPremiumUser = true,
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
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(
                        isPremiumUser = true,
                        totpCodeItemData = createTotpCodeData(),
                    )
                } returns createViewState(common = DEFAULT_COMMON.copy(requiresReprompt = false))
            }
            every { clipboardManager.setText(text = DEFAULT_LOGIN_USERNAME) } just runs
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = createVerificationCodeItem())

            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyUsernameClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = DEFAULT_LOGIN_USERNAME)
                mockCipherView.toViewState(
                    isPremiumUser = true,
                    totpCodeItemData = createTotpCodeData(),
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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = createTotpCodeData(),
                        )
                    } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())

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
                        isPremiumUser = true,
                        totpCodeItemData = createTotpCodeData(),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on PasswordHistoryClick should emit NavigateToPasswordHistory when re-prompt is not required`() =
            runTest {
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = createTotpCodeData(),
                        )
                    }
                        .returns(
                            createViewState(
                                common = DEFAULT_COMMON.copy(requiresReprompt = false),
                            ),
                        )
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())

                viewModel.eventFlow.test {
                    viewModel.trySendAction(VaultItemAction.ItemType.Login.PasswordHistoryClick)
                    assertEquals(
                        VaultItemEvent.NavigateToPasswordHistory(VAULT_ITEM_ID),
                        awaitItem(),
                    )
                }

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        isPremiumUser = true,
                        totpCodeItemData = createTotpCodeData(),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on PasswordVisibilityClicked should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = createTotpCodeData(),
                        )
                    } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())

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
                        isPremiumUser = true,
                        totpCodeItemData = createTotpCodeData(),
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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = createTotpCodeData(),
                        )
                    } returns loginViewState
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value =
                    DataState.Loaded(data = createVerificationCodeItem())

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
                        isPremiumUser = true,
                        totpCodeItemData = createTotpCodeData(),
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
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns CARD_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

                assertEquals(cardState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.ItemType.Card.CopyNumberClick)
                assertEquals(
                    cardState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            action = PasswordRepromptAction.CopyClick(
                                value = requireNotNull(DEFAULT_CARD_TYPE.number),
                            ),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        isPremiumUser = true,
                        totpCodeItemData = null,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CopyNumberClick should call setText on the ClipboardManager when re-prompt is not required`() {
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(
                        isPremiumUser = true,
                        totpCodeItemData = null,
                    )
                } returns createViewState(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                    type = DEFAULT_CARD_TYPE,
                )
            }
            every { clipboardManager.setText(text = "12345436") } just runs
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

            viewModel.trySendAction(VaultItemAction.ItemType.Card.CopyNumberClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = "12345436")
                mockCipherView.toViewState(
                    isPremiumUser = true,
                    totpCodeItemData = null,
                )
            }
        }

        @Test
        fun `on CopySecurityCodeClick should show password dialog when re-prompt is required`() =
            runTest {
                val cardState = DEFAULT_STATE.copy(viewState = CARD_VIEW_STATE)
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(
                            isPremiumUser = true,
                            totpCodeItemData = null,
                        )
                    } returns CARD_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
                mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

                assertEquals(cardState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.ItemType.Card.CopySecurityCodeClick)
                assertEquals(
                    cardState.copy(
                        dialog = VaultItemState.DialogState.MasterPasswordDialog(
                            action = PasswordRepromptAction.CopyClick(
                                value = requireNotNull(DEFAULT_CARD_TYPE.securityCode),
                            ),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(
                        isPremiumUser = true,
                        totpCodeItemData = null,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CopySecurityCodeClick should call setText on the ClipboardManager when re-prompt is not required`() {
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(
                        isPremiumUser = true,
                        totpCodeItemData = null,
                    )
                } returns createViewState(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                    type = DEFAULT_CARD_TYPE,
                )
            }
            every { clipboardManager.setText(text = "987") } just runs
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            mutableAuthCodeItemFlow.value = DataState.Loaded(data = null)

            viewModel.trySendAction(VaultItemAction.ItemType.Card.CopySecurityCodeClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = "987")
                mockCipherView.toViewState(
                    isPremiumUser = true,
                    totpCodeItemData = null,
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
            val cipherView = mockk<CipherView> {
                every {
                    toViewState(isPremiumUser = true, totpCodeItemData = null)
                } returns viewState
            }
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Loaded(data = cipherView)

            assertEquals(DEFAULT_STATE.copy(viewState = viewState), viewModel.stateFlow.value)
        }

        @Test
        fun `on VaultDataReceive with Loaded and null data should update the ViewState to Error`() {
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Loaded(data = null)

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
            val cipherView = mockk<CipherView> {
                every {
                    toViewState(isPremiumUser = true, totpCodeItemData = null)
                } returns viewState
            }
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Pending(data = cipherView)

            assertEquals(DEFAULT_STATE.copy(viewState = viewState), viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `on VaultDataReceive with Pending and null data should update the ViewState to Error`() {
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Pending(data = null)

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
            val cipherView = mockk<CipherView> {
                every {
                    toViewState(isPremiumUser = true, totpCodeItemData = null)
                } returns viewState
            }
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.Error(error = Throwable(), data = cipherView)

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
            val cipherView = mockk<CipherView> {
                every {
                    toViewState(isPremiumUser = true, totpCodeItemData = null)
                } returns viewState
            }
            val viewModel = createViewModel(state = null)

            mutableVaultItemFlow.value = DataState.NoNetwork(data = cipherView)

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

    @Suppress("LongParameterList")
    private fun createViewModel(
        state: VaultItemState?,
        vaultItemId: String = VAULT_ITEM_ID,
        bitwardenClipboardManager: BitwardenClipboardManager = clipboardManager,
        authRepository: AuthRepository = authRepo,
        vaultRepository: VaultRepository = vaultRepo,
        fileManager: FileManager = mockFileManager,
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
            )

        private val DEFAULT_CARD_TYPE: VaultItemState.ViewState.Content.ItemType.Card =
            VaultItemState.ViewState.Content.ItemType.Card(
                cardholderName = "mockName",
                number = "12345436",
                brand = VaultCardBrand.VISA,
                expiration = "03/2027",
                securityCode = "987",
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
    }
}
