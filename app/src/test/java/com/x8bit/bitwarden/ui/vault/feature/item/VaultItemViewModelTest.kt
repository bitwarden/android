package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.item.util.DEFAULT_EMPTY_LOGIN_VIEW_STATE
import com.x8bit.bitwarden.ui.vault.feature.item.util.toViewState
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

    @Test
    fun `on CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel(state = DEFAULT_STATE)
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemAction.CloseClick)
            assertEquals(VaultItemEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on DismissDialogClick should clear the dialog state`() = runTest {
        val initialState = DEFAULT_STATE.copy(dialog = VaultItemState.DialogState.Loading)
        val viewModel = createViewModel(state = initialState)
        assertEquals(initialState, viewModel.stateFlow.value)

        viewModel.trySendAction(VaultItemAction.DismissDialogClick)
        assertEquals(initialState.copy(dialog = null), viewModel.stateFlow.value)
    }

    @Test
    fun `on EditClick should do nothing when ViewState is not Content`() = runTest {
        val initialState = DEFAULT_STATE
        val viewModel = createViewModel(state = initialState)

        assertEquals(initialState, viewModel.stateFlow.value)
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemAction.EditClick)
            expectNoEvents()
        }
        assertEquals(initialState, viewModel.stateFlow.value)
    }

    @Test
    fun `on EditClick should prompt for master password when required`() = runTest {
        val mockCipherView = mockk<CipherView> {
            every { toViewState(isPremiumUser = true) } returns DEFAULT_LOGIN_VIEW_STATE
        }
        mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
        val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_LOGIN_VIEW_STATE)
        val viewModel = createViewModel(state = loginState)
        assertEquals(loginState, viewModel.stateFlow.value)

        viewModel.trySendAction(VaultItemAction.EditClick)
        assertEquals(
            loginState.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog),
            viewModel.stateFlow.value,
        )

        verify(exactly = 1) {
            mockCipherView.toViewState(isPremiumUser = true)
        }
    }

    @Test
    fun `on EditClick should navigate password is not required`() = runTest {
        val loginViewState = DEFAULT_LOGIN_VIEW_STATE.copy(requiresReprompt = false)
        val mockCipherView = mockk<CipherView> {
            every { toViewState(isPremiumUser = true) } returns loginViewState
        }
        mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
        val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
        val viewModel = createViewModel(state = loginState)

        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemAction.EditClick)
            assertEquals(VaultItemEvent.NavigateToEdit(VAULT_ITEM_ID), awaitItem())
        }

        verify(exactly = 1) {
            mockCipherView.toViewState(isPremiumUser = true)
        }
    }

    @Test
    fun `on MasterPasswordSubmit should verify the password`() = runTest {
        val loginViewState = DEFAULT_LOGIN_VIEW_STATE.copy(requiresReprompt = false)
        val mockCipherView = mockk<CipherView> {
            every { toViewState(isPremiumUser = true) } returns loginViewState
        }
        mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
        val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
        val viewModel = createViewModel(state = loginState)

        viewModel.stateFlow.test {
            assertEquals(loginState, awaitItem())
            viewModel.trySendAction(VaultItemAction.MasterPasswordSubmit("password"))
            assertEquals(loginState.copy(dialog = VaultItemState.DialogState.Loading), awaitItem())
            assertEquals(
                loginState.copy(viewState = loginViewState.copy(requiresReprompt = false)),
                awaitItem(),
            )
        }

        verify(exactly = 1) {
            mockCipherView.toViewState(isPremiumUser = true)
        }
    }

    @Test
    fun `on RefreshClick should sync`() = runTest {
        every { vaultRepo.sync() } just runs
        val viewModel = createViewModel(state = DEFAULT_STATE)

        viewModel.trySendAction(VaultItemAction.RefreshClick)

        verify(exactly = 1) {
            vaultRepo.sync()
        }
    }

    @Nested
    inner class LoginActions {
        private lateinit var viewModel: VaultItemViewModel

        @BeforeEach
        fun setup() {
            viewModel = createViewModel(
                state = DEFAULT_STATE.copy(viewState = DEFAULT_LOGIN_VIEW_STATE),
            )
        }

        @Test
        fun `on CheckForBreachClick should process a password`() = runTest {
            val mockCipherView = mockk<CipherView> {
                every { toViewState(isPremiumUser = true) } returns DEFAULT_LOGIN_VIEW_STATE
            }
            mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)
            val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_LOGIN_VIEW_STATE)
            val breachCount = 5
            coEvery {
                authRepo.getPasswordBreachCount(password = DEFAULT_LOGIN_PASSWORD)
            } returns BreachCountResult.Success(breachCount = breachCount)

            viewModel.stateFlow.test {
                assertEquals(loginState, awaitItem())
                viewModel.trySendAction(VaultItemAction.Login.CheckForBreachClick)
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
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_LOGIN_VIEW_STATE)
                val mockCipherView = mockk<CipherView> {
                    every { toViewState(isPremiumUser = true) } returns DEFAULT_LOGIN_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.Login.CopyPasswordClick)
                assertEquals(
                    loginState.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
                }
            }

        @Test
        fun `on CopyPasswordClick should emit CopyToClipboard when re-prompt is not required`() =
            runTest {
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(isPremiumUser = true)
                    } returns DEFAULT_LOGIN_VIEW_STATE.copy(requiresReprompt = false)
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                viewModel.eventFlow.test {
                    viewModel.trySendAction(VaultItemAction.Login.CopyPasswordClick)
                    assertEquals(
                        VaultItemEvent.CopyToClipboard(DEFAULT_LOGIN_PASSWORD.asText()),
                        awaitItem(),
                    )
                }

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on CopyCustomHiddenFieldClick should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_LOGIN_VIEW_STATE)
                val mockCipherView = mockk<CipherView> {
                    every { toViewState(isPremiumUser = true) } returns DEFAULT_LOGIN_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.Login.CopyCustomHiddenFieldClick("field"))
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
        fun `on CopyCustomHiddenFieldClick should emit CopyToClipboard when re-prompt is not required`() =
            runTest {
                val field = "field"
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(isPremiumUser = true)
                    } returns DEFAULT_LOGIN_VIEW_STATE.copy(requiresReprompt = false)
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                viewModel.eventFlow.test {
                    viewModel.trySendAction(VaultItemAction.Login.CopyCustomHiddenFieldClick(field))
                    assertEquals(
                        VaultItemEvent.CopyToClipboard(field.asText()),
                        awaitItem(),
                    )
                }

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
                }
            }

        @Test
        fun `on CopyCustomTextFieldClick should emit CopyToClipboard`() = runTest {
            val field = "field"
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemAction.Login.CopyCustomTextFieldClick(field))
                assertEquals(VaultItemEvent.CopyToClipboard(field.asText()), awaitItem())
            }
        }

        @Test
        fun `on CopyUriClick should emit CopyToClipboard`() = runTest {
            val uri = "uri"
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemAction.Login.CopyUriClick(uri))
                assertEquals(VaultItemEvent.CopyToClipboard(uri.asText()), awaitItem())
            }
        }

        @Test
        fun `on CopyUsernameClick should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_LOGIN_VIEW_STATE)
                val mockCipherView = mockk<CipherView> {
                    every { toViewState(isPremiumUser = true) } returns DEFAULT_LOGIN_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.Login.CopyUsernameClick)
                assertEquals(
                    loginState.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
                }
            }

        @Test
        fun `on CopyUsernameClick should emit CopyToClipboard when re-prompt is not required`() =
            runTest {
                val mockCipherView = mockk<CipherView> {
                    every {
                        toViewState(isPremiumUser = true)
                    } returns DEFAULT_LOGIN_VIEW_STATE.copy(requiresReprompt = false)
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                viewModel.eventFlow.test {
                    viewModel.trySendAction(VaultItemAction.Login.CopyUsernameClick)
                    assertEquals(
                        VaultItemEvent.CopyToClipboard(DEFAULT_LOGIN_USERNAME.asText()),
                        awaitItem(),
                    )
                }

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
                }
            }

        @Test
        fun `on LaunchClick should emit NavigateToUri`() = runTest {
            val uri = "uri"
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemAction.Login.LaunchClick(uri))
                assertEquals(VaultItemEvent.NavigateToUri(uri), awaitItem())
            }
        }

        @Test
        fun `on PasswordHistoryClick should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_LOGIN_VIEW_STATE)
                val mockCipherView = mockk<CipherView> {
                    every { toViewState(isPremiumUser = true) } returns DEFAULT_LOGIN_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.Login.PasswordHistoryClick)
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
                    } returns DEFAULT_LOGIN_VIEW_STATE.copy(requiresReprompt = false)
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                viewModel.eventFlow.test {
                    viewModel.trySendAction(VaultItemAction.Login.PasswordHistoryClick)
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
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_LOGIN_VIEW_STATE)
                val mockCipherView = mockk<CipherView> {
                    every { toViewState(isPremiumUser = true) } returns DEFAULT_LOGIN_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.Login.PasswordVisibilityClicked(true))
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
                val loginViewState = DEFAULT_LOGIN_VIEW_STATE.copy(requiresReprompt = false)
                val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
                val mockCipherView = mockk<CipherView> {
                    every { toViewState(isPremiumUser = true) } returns loginViewState
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(VaultItemAction.Login.PasswordVisibilityClicked(true))
                assertEquals(
                    loginState.copy(
                        viewState = loginViewState.copy(
                            passwordData = loginViewState.passwordData!!.copy(isVisible = true),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `on HiddenFieldVisibilityClicked should show password dialog when re-prompt is required`() =
            runTest {
                val loginState = DEFAULT_STATE.copy(viewState = DEFAULT_LOGIN_VIEW_STATE)
                val mockCipherView = mockk<CipherView> {
                    every { toViewState(isPremiumUser = true) } returns DEFAULT_LOGIN_VIEW_STATE
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(
                    VaultItemAction.Login.HiddenFieldVisibilityClicked(
                        field = VaultItemState.ViewState.Content.Custom.HiddenField(
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
                val hiddenField = VaultItemState.ViewState.Content.Custom.HiddenField(
                    name = "hidden",
                    value = "value",
                    isCopyable = true,
                    isVisible = false,
                )
                val loginViewState = DEFAULT_EMPTY_LOGIN_VIEW_STATE.copy(
                    requiresReprompt = false,
                    customFields = listOf(hiddenField),
                )
                val loginState = DEFAULT_STATE.copy(viewState = loginViewState)
                val mockCipherView = mockk<CipherView> {
                    every { toViewState(isPremiumUser = true) } returns loginViewState
                }
                mutableVaultItemFlow.value = DataState.Loaded(data = mockCipherView)

                assertEquals(loginState, viewModel.stateFlow.value)
                viewModel.trySendAction(
                    VaultItemAction.Login.HiddenFieldVisibilityClicked(
                        field = hiddenField,
                        isVisible = true,
                    ),
                )
                assertEquals(
                    loginState.copy(
                        viewState = loginViewState.copy(
                            customFields = listOf(hiddenField.copy(isVisible = true)),
                        ),
                    ),
                    viewModel.stateFlow.value,
                )

                verify(exactly = 1) {
                    mockCipherView.toViewState(isPremiumUser = true)
                }
            }
    }

    private fun createViewModel(
        state: VaultItemState?,
        vaultItemId: String = VAULT_ITEM_ID,
        authRepository: AuthRepository = authRepo,
        vaultRepository: VaultRepository = vaultRepo,
    ): VaultItemViewModel = VaultItemViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
            set("vault_item_id", vaultItemId)
        },
        authRepository = authRepository,
        vaultRepository = vaultRepository,
    )
}

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
        ),
    ),
)

private val DEFAULT_LOGIN_VIEW_STATE: VaultItemState.ViewState.Content.Login =
    VaultItemState.ViewState.Content.Login(
        name = "login cipher",
        lastUpdated = "12/31/69 06:16 PM",
        passwordHistoryCount = 1,
        notes = "Lots of notes",
        isPremiumUser = true,
        customFields = listOf(
            VaultItemState.ViewState.Content.Custom.TextField(
                name = "text",
                value = "value",
                isCopyable = true,
            ),
            VaultItemState.ViewState.Content.Custom.HiddenField(
                name = "hidden",
                value = "value",
                isCopyable = true,
                isVisible = false,
            ),
            VaultItemState.ViewState.Content.Custom.BooleanField(
                name = "boolean",
                value = true,
            ),
            VaultItemState.ViewState.Content.Custom.LinkedField(
                name = "linked username",
                id = 100U,
            ),
            VaultItemState.ViewState.Content.Custom.LinkedField(
                name = "linked password",
                id = 101U,
            ),
        ),
        requiresReprompt = true,
        username = DEFAULT_LOGIN_USERNAME,
        passwordData = VaultItemState.ViewState.Content.PasswordData(
            password = DEFAULT_LOGIN_PASSWORD,
            isVisible = false,
        ),
        uris = listOf(
            VaultItemState.ViewState.Content.UriData(
                uri = "www.example.com",
                isCopyable = true,
                isLaunchable = true,
            ),
        ),
        passwordRevisionDate = "12/31/69 06:16 PM",
        totp = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
    )
