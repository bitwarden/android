package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.item.util.createCommonContent
import com.x8bit.bitwarden.ui.vault.feature.item.util.createLoginContent
import com.x8bit.bitwarden.ui.vault.feature.item.util.toViewState
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

class VaultItemViewModelTest : BaseViewModelTest() {

    private val mutableVaultItemFlow = MutableStateFlow<DataState<CipherView?>>(DataState.Loading)
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)

    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val authRepo: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val vaultRepo: VaultRepository = mockk {
        every { getVaultItemStateFlow(VAULT_ITEM_ID) } returns mutableVaultItemFlow
    }

    @BeforeEach
    fun setup() {
        mockkStatic(CIPHER_VIEW_EXTENSIONS_PATH)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(CIPHER_VIEW_EXTENSIONS_PATH)
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
            val initialState = DEFAULT_STATE.copy(dialog = VaultItemState.DialogState.Loading)
            val viewModel = createViewModel(state = initialState)
            assertEquals(initialState, viewModel.stateFlow.value)

            viewModel.trySendAction(VaultItemAction.Common.DismissDialogClick)
            assertEquals(initialState.copy(dialog = null), viewModel.stateFlow.value)
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
                every { toViewState(isPremiumUser = true) } returns DEFAULT_VIEW_STATE
            }
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
            val viewModel = createViewModel(state = loginState)
            assertEquals(loginState, viewModel.stateFlow.value)

            viewModel.trySendAction(VaultItemAction.Common.EditClick)
            assertEquals(
                loginState.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog),
                viewModel.stateFlow.value,
            )
        }

        @Test
        fun `on EditClick should navigate password is not required`() = runTest {
            val loginViewState = createViewState(
                common = DEFAULT_COMMON.copy(requiresReprompt = false),
            )
            val mockCipherView = mockk<CipherView> {
                every { toViewState(isPremiumUser = true) } returns loginViewState
            }
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
            val viewModel = createViewModel(state = loginState)

            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemAction.Common.EditClick)
                assertEquals(VaultItemEvent.NavigateToEdit(VAULT_ITEM_ID), awaitItem())
            }
        }

        @Test
        fun `on MasterPasswordSubmit should verify the password`() = runTest {
            val loginViewState = createViewState(
                common = DEFAULT_COMMON.copy(requiresReprompt = false),
            )
            val mockCipherView = mockk<CipherView> {
                every { toViewState(isPremiumUser = true) } returns loginViewState
            }
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
            val viewModel = createViewModel(state = loginState)

            viewModel.stateFlow.test {
                assertEquals(loginState, awaitItem())
                viewModel.trySendAction(VaultItemAction.Common.MasterPasswordSubmit("password"))
                assertEquals(
                    loginState.copy(dialog = VaultItemState.DialogState.Loading),
                    awaitItem(),
                )
                assertEquals(
                    loginState.copy(
                        viewState = loginViewState.copy(
                            common = DEFAULT_COMMON.copy(requiresReprompt = false),
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
                    every { toViewState(isPremiumUser = true) } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.Common.CopyCustomHiddenFieldClick("field"))
                assertEquals(
                    loginState.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CopyCustomHiddenFieldClick should call setText on ClipboardManager when re-prompt is not required`() {
            val field = "field"
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(isPremiumUser = true)
                } returns createViewState(common = DEFAULT_COMMON.copy(requiresReprompt = false))
            }
            every { clipboardManager.setText(text = field) } just runs
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

            viewModel.trySendAction(VaultItemAction.Common.CopyCustomHiddenFieldClick(field))

            verify(exactly = 1) {
                clipboardManager.setText(text = field)
                mockCipherView.toViewState(isPremiumUser = true)
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
                val mockCipherView = mockk<CipherView> {
                    every { toViewState(isPremiumUser = true) } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(
                    VaultItemAction.Common.HiddenFieldVisibilityClicked(
                        field = VaultItemState.ViewState.Content.Common.Custom.HiddenField(
                            name = "hidden",
                            value = "value",
                            isCopyable = true,
                            isVisible = false,
                        ),
                        isVisible = true,
                    ),
                )
                assertEquals(
                    loginState.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
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
                    common = createCommonContent(isEmpty = true).copy(
                        requiresReprompt = false,
                        customFields = listOf(hiddenField),
                    ),
                    type = createLoginContent(isEmpty = true),
                )
                val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
                val mockCipherView = mockk<CipherView> {
                    every { toViewState(isPremiumUser = true) } returns loginViewState
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

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
                            common = createCommonContent(isEmpty = true).copy(
                                requiresReprompt = false,
                                customFields = listOf(hiddenField.copy(isVisible = true)),
                            ),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
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
            val mockCipherView = mockk<CipherView> {
                every { toViewState(isPremiumUser = true) } returns DEFAULT_VIEW_STATE
            }
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
            val breachCount = 5
            coEvery {
                authRepo.getPasswordBreachCount(password = DEFAULT_LOGIN_PASSWORD)
            } returns BreachCountResult.Success(breachCount = breachCount)

            viewModel.stateFlow.test {
                assertEquals(loginState, awaitItem())
                viewModel.trySendAction(VaultItemAction.ItemType.Login.CheckForBreachClick)
                assertEquals(
                    loginState.copy(dialog = VaultItemState.DialogState.Loading),
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
                mockCipherView.toViewState(isPremiumUser = true)
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
                    every { toViewState(isPremiumUser = true) } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyPasswordClick)
                assertEquals(
                    loginState.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CopyPasswordClick should call setText on the CLipboardManager when re-prompt is not required`() {
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(isPremiumUser = true)
                } returns createViewState(common = DEFAULT_COMMON.copy(requiresReprompt = false))
            }
            every { clipboardManager.setText(text = DEFAULT_LOGIN_PASSWORD) } just runs
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyPasswordClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = DEFAULT_LOGIN_PASSWORD)
                mockCipherView.toViewState(isPremiumUser = true)
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
        fun `on CopyUsernameClick should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
                val mockCipherView = mockk<CipherView> {
                    every { toViewState(isPremiumUser = true) } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyUsernameClick)
                assertEquals(
                    loginState.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CopyUsernameClick should call setText on ClipboardManager when re-prompt is not required`() {
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(isPremiumUser = true)
                } returns createViewState(common = DEFAULT_COMMON.copy(requiresReprompt = false))
            }
            every { clipboardManager.setText(text = DEFAULT_LOGIN_USERNAME) } just runs
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyUsernameClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = DEFAULT_LOGIN_USERNAME)
                mockCipherView.toViewState(isPremiumUser = true)
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
                    every { toViewState(isPremiumUser = true) } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.ItemType.Login.PasswordHistoryClick)
                assertEquals(
                    loginState.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on PasswordHistoryClick should emit NavigateToPasswordHistory when re-prompt is not required`() =
            runTest {
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(isPremiumUser = true)
                    }
                        .returns(
                            createViewState(
                                common = DEFAULT_COMMON.copy(requiresReprompt = false),
                            ),
                        )
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                viewModel.eventFlow.test {
                    viewModel.trySendAction(VaultItemAction.ItemType.Login.PasswordHistoryClick)
                    assertEquals(
                        VaultItemEvent.NavigateToPasswordHistory(VAULT_ITEM_ID),
                        awaitItem(),
                    )
                }

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on PasswordVisibilityClicked should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_VIEW_STATE)
                val mockCipherView = mockk<CipherView> {
                    every { toViewState(isPremiumUser = true) } returns DEFAULT_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(
                    VaultItemAction.ItemType.Login.PasswordVisibilityClicked(
                        true,
                    ),
                )
                assertEquals(
                    loginState.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
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
                    every { toViewState(isPremiumUser = true) } returns loginViewState
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

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
                    mockCipherView.toViewState(isPremiumUser = true)
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
                    every { toViewState(isPremiumUser = true) } returns CARD_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(cardState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.ItemType.Card.CopyNumberClick)
                assertEquals(
                    cardState.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CopyNumberClick should call setText on the ClipboardManager when re-prompt is not required`() {
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(isPremiumUser = true)
                } returns createViewState(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                    type = DEFAULT_CARD_TYPE,
                )
            }
            every { clipboardManager.setText(text = "12345436") } just runs
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

            viewModel.trySendAction(VaultItemAction.ItemType.Card.CopyNumberClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = "12345436")
                mockCipherView.toViewState(isPremiumUser = true)
            }
        }

        @Test
        fun `on CopySecurityCodeClick should show password dialog when re-prompt is required`() =
            runTest {
                val cardState = DEFAULT_STATE.copy(viewState = CARD_VIEW_STATE)
                val mockCipherView = mockk<CipherView> {
                    every { toViewState(isPremiumUser = true) } returns CARD_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(cardState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.ItemType.Card.CopySecurityCodeClick)
                assertEquals(
                    cardState.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CopySecurityCodeClick should call setText on the ClipboardManager when re-prompt is not required`() {
            val mockCipherView = mockk<CipherView> {
                every {
                    toViewState(isPremiumUser = true)
                } returns createViewState(
                    common = DEFAULT_COMMON.copy(requiresReprompt = false),
                    type = DEFAULT_CARD_TYPE,
                )
            }
            every { clipboardManager.setText(text = "987") } just runs
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

            viewModel.trySendAction(VaultItemAction.ItemType.Card.CopySecurityCodeClick)

            verify(exactly = 1) {
                clipboardManager.setText(text = "987")
                mockCipherView.toViewState(isPremiumUser = true)
            }
        }
    }

    private fun createViewModel(
        state: VaultItemState?,
        vaultItemId: String = VAULT_ITEM_ID,
        bitwardenClipboardManager: BitwardenClipboardManager = clipboardManager,
        authRepository: AuthRepository = authRepo,
        vaultRepository: VaultRepository = vaultRepo,
    ): VaultItemViewModel = VaultItemViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
            set("vault_item_id", vaultItemId)
        },
        clipboardManager = bitwardenClipboardManager,
        authRepository = authRepository,
        vaultRepository = vaultRepository,
    )

    private fun createViewState(
        common: VaultItemState.ViewState.Content.Common = DEFAULT_COMMON,
        type: VaultItemState.ViewState.Content.ItemType = DEFAULT_LOGIN_TYPE,
    ): VaultItemState.ViewState.Content =
        VaultItemState.ViewState.Content(
            common = common,
            type = type,
        )

    companion object {
        private const val CIPHER_VIEW_EXTENSIONS_PATH: String =
            "com.x8bit.bitwarden.ui.vault.feature.item.util.CipherViewExtensionsKt"

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
                    isVaultUnlocked = true,
                    organizations = emptyList(),
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
                totp = "otpauth://totp/Example:alice@google.com" +
                    "?secret=JBSWY3DPEHPK3PXP&issuer=Example",
                isPremiumUser = true,
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
