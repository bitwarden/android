package com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.bitwarden.cxf.model.ImportCredentialsRequestData
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.policies.PolicyType
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.RequestOtpResult
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.VerifyOtpResult
import com.x8bit.bitwarden.data.auth.repository.model.createMockOrganization
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockPolicyView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.ui.vault.feature.exportitems.model.AccountSelectionListItem
import com.x8bit.bitwarden.ui.vault.feature.vault.util.initials
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VerifyPasswordViewModelTest : BaseViewModelTest() {

    private val mutableUserStateFlow = MutableStateFlow(DEFAULT_USER_STATE)
    private val authRepository = mockk<AuthRepository> {
        every { userStateFlow } returns mutableUserStateFlow
        every { activeUserId } returns DEFAULT_USER_ID
        coEvery { requestOneTimePasscode() } returns RequestOtpResult.Success
    }
    private val vaultRepository = mockk<VaultRepository> {
        every { isVaultUnlocked(any()) } returns true
        coEvery {
            unlockVaultWithMasterPassword(masterPassword = any())
        } returns VaultUnlockResult.Success
    }
    private val policyManager = mockk<PolicyManager> {
        every { getActivePolicies(PolicyType.RESTRICTED_ITEM_TYPES) } returns listOf(
            createMockPolicyView(
                organizationId = DEFAULT_ORGANIZATION_ID,
                enabled = false,
            ),
        )
    }
    private val specialCircumstanceManager = mockk<SpecialCircumstanceManager> {
        every {
            specialCircumstance
        } returns SpecialCircumstance.CredentialExchangeExport(data = DEFAULT_IMPORT_REQUEST)
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(
            SavedStateHandle::toVerifyPasswordArgs,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            SavedStateHandle::toVerifyPasswordArgs,
        )
    }

    @Nested
    inner class State {
        @Test
        fun `initial state should be correct when account has a master password`() = runTest {
            createViewModel()
                .also {
                    assertEquals(
                        VerifyPasswordState(
                            importRequest = DEFAULT_IMPORT_REQUEST,
                            viewState = VerifyPasswordState.ViewState.Loading,
                            dialog = null,
                            accountSummaryListItem = DEFAULT_ACCOUNT_SELECTION_LIST_ITEM,
                            hasOtherAccounts = true,
                            hasMasterPassword = true,
                        ),
                        it.stateFlow.value,
                    )
                    coVerify(exactly = 0) { authRepository.requestOneTimePasscode() }
                }
        }

        @Test
        fun `initial state should be correct when account has no master password`() = runTest {
            mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                accounts = DEFAULT_USER_STATE.accounts.map {
                    it.copy(hasMasterPassword = false)
                },
            )
            coEvery { authRepository.requestOneTimePasscode() } returns RequestOtpResult.Success

            createViewModel()
                .also {
                    assertEquals(
                        VerifyPasswordState(
                            importRequest = DEFAULT_IMPORT_REQUEST,
                            viewState = VerifyPasswordState.ViewState.Loading,
                            dialog = null,
                            accountSummaryListItem = DEFAULT_ACCOUNT_SELECTION_LIST_ITEM,
                            hasOtherAccounts = true,
                            hasMasterPassword = false,
                        ),
                        it.stateFlow.value,
                    )
                    coVerify(exactly = 1) { authRepository.requestOneTimePasscode() }
                }
        }

        @Test
        fun `initial state should be correct when account has item restrictions`() = runTest {
            every {
                policyManager.getActivePolicies(PolicyType.RESTRICTED_ITEM_TYPES)
            } returns listOf(
                createMockPolicyView(
                    organizationId = DEFAULT_ORGANIZATION_ID,
                    enabled = true,
                ),
            )

            createViewModel()
                .also {
                    assertEquals(
                        DEFAULT_LOADING_STATE.copy(
                            accountSummaryListItem = DEFAULT_ACCOUNT_SELECTION_LIST_ITEM
                                .copy(isItemRestricted = true),
                        ),
                        it.stateFlow.value,
                    )
                }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `initial state should throw when special circumstance is not a credential exchange export`() {
            every { specialCircumstanceManager.specialCircumstance } returns null

            assertThrows<IllegalArgumentException> { createViewModel() }
        }

        @Test
        fun `initial state should throw when the account cannot be found`() {
            assertThrows<IllegalStateException> { createViewModel(userId = "unknownUserId") }
        }

        @Test
        fun `initial state should be restored from the saved state handle`() = runTest {
            val savedState = DEFAULT_STATE.copy(hasOtherAccounts = false)

            createViewModel(state = savedState)
                .also { assertEquals(savedState, it.stateFlow.value) }
        }
    }

    @Nested
    inner class ImportRequestValidation {
        @Test
        fun `initial load should emit ValidateImportRequest event`() = runTest {
            createViewModel().also { viewModel ->
                viewModel.eventFlow.test {
                    assertEquals(
                        VerifyPasswordEvent.ValidateImportRequest(
                            importCredentialsRequestData = DEFAULT_IMPORT_REQUEST,
                        ),
                        awaitItem(),
                    )
                }
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `ValidateImportRequestResultReceive with valid request should show master password content`() =
            runTest {
                createViewModel().also { viewModel ->
                    viewModel.trySendAction(
                        VerifyPasswordAction.ValidateImportRequestResultReceive(isValid = true),
                    )

                    assertEquals(
                        DEFAULT_LOADING_STATE.copy(viewState = DEFAULT_CONTENT_VIEW_STATE),
                        viewModel.stateFlow.value,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `ValidateImportRequestResultReceive with valid request should show otp content when account has no master password`() =
            runTest {
                mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                    accounts = DEFAULT_USER_STATE.accounts.map {
                        it.copy(hasMasterPassword = false)
                    },
                )

                createViewModel().also { viewModel ->
                    viewModel.trySendAction(
                        VerifyPasswordAction.ValidateImportRequestResultReceive(isValid = true),
                    )

                    assertEquals(
                        DEFAULT_LOADING_STATE.copy(
                            viewState = OTP_CONTENT_VIEW_STATE,
                            hasMasterPassword = false,
                        ),
                        viewModel.stateFlow.value,
                    )
                }
            }

        @Test
        fun `ValidateImportRequestResultReceive with invalid request should show error content`() =
            runTest {
                createViewModel().also { viewModel ->
                    viewModel.trySendAction(
                        VerifyPasswordAction.ValidateImportRequestResultReceive(isValid = false),
                    )

                    assertEquals(
                        DEFAULT_LOADING_STATE.copy(
                            viewState = VerifyPasswordState.ViewState.Error(
                                message = BitwardenString
                                    .the_import_request_could_not_be_processed
                                    .asText(),
                            ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
            }
    }

    @Nested
    inner class ViewActions {

        @Test
        fun `SendCodeClick should show loading dialog and request otp code`() = runTest {
            val initialState = DEFAULT_STATE.copy(
                viewState = OTP_CONTENT_VIEW_STATE,
                hasMasterPassword = false,
            )
            coEvery { authRepository.requestOneTimePasscode() } returns RequestOtpResult.Success
            createViewModel(state = initialState).also { viewModel ->
                viewModel.trySendAction(VerifyPasswordAction.ResendCodeClick)
                coVerify { authRepository.requestOneTimePasscode() }
            }
        }

        @Test
        fun `SendOtpCodeResultReceive success should show snackbar`() = runTest {
            createViewModel(state = DEFAULT_STATE).also { viewModel ->
                viewModel.eventFlow.test {
                    awaitValidateImportRequestEvent()

                    viewModel.trySendAction(
                        VerifyPasswordAction.Internal.SendOtpCodeResultReceive(
                            RequestOtpResult.Success,
                        ),
                    )

                    assertEquals(
                        VerifyPasswordEvent.ShowSnackbar(BitwardenString.code_sent.asText()),
                        awaitItem(),
                    )
                }
            }
        }

        @Test
        fun `SendOtpCodeResultReceive error should show dialog`() = runTest {
            createViewModel(state = DEFAULT_STATE).also { viewModel ->
                viewModel.trySendAction(
                    VerifyPasswordAction.Internal.SendOtpCodeResultReceive(
                        RequestOtpResult.Error(
                            message = "error",
                            error = IllegalStateException(),
                        ),
                    ),
                )

                assertEquals(
                    VerifyPasswordState.DialogState.General(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = "error".asText(),
                    ),
                    viewModel.stateFlow.value.dialog,
                )
            }
        }

        @Test
        fun `ContinueClick with otp should verify otp`() = runTest {
            val initialState = DEFAULT_STATE.copy(
                viewState = OTP_CONTENT_VIEW_STATE.copy(input = "123456"),
                hasMasterPassword = false,
            )
            coEvery {
                authRepository.verifyOneTimePasscode("123456")
            } returns VerifyOtpResult.Verified

            createViewModel(state = initialState).also { viewModel ->
                viewModel.trySendAction(VerifyPasswordAction.ContinueClick)

                coVerify { authRepository.verifyOneTimePasscode("123456") }
            }
        }

        @Test
        fun `VerifyOtpResultReceive verified should send event and clear input`() = runTest {
            val initialState = DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(input = "123"),
            )
            createViewModel(state = initialState)
                .also { viewModel ->
                    viewModel.eventFlow.test {
                        awaitValidateImportRequestEvent()

                        viewModel.trySendAction(
                            VerifyPasswordAction.Internal.VerifyOtpResultReceive(
                                VerifyOtpResult.Verified,
                            ),
                        )

                        assertEquals(
                            VerifyPasswordEvent.PasswordVerified(DEFAULT_USER_ID),
                            awaitItem(),
                        )
                    }

                    assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
                }
        }

        @Test
        fun `VerifyOtpResultReceive not verified should show dialog`() = runTest {
            createViewModel(state = DEFAULT_STATE).also { viewModel ->
                viewModel.trySendAction(
                    VerifyPasswordAction.Internal.VerifyOtpResultReceive(
                        VerifyOtpResult.NotVerified(
                            error = IllegalStateException(),
                            errorMessage = null,
                        ),
                    ),
                )

                assertEquals(
                    VerifyPasswordState.DialogState.General(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.invalid_verification_code.asText(),
                    ),
                    viewModel.stateFlow.value.dialog,
                )
            }
        }

        @Test
        fun `NavigateBackClick should send NavigateBack event`() = runTest {
            createViewModel(state = DEFAULT_STATE).also { viewModel ->
                viewModel.eventFlow.test {
                    awaitValidateImportRequestEvent()

                    viewModel.trySendAction(VerifyPasswordAction.NavigateBackClick)

                    assertEquals(
                        VerifyPasswordEvent.NavigateBack,
                        awaitItem(),
                    )
                }
            }
        }

        @Test
        fun `NavigateBackClick should send CancelExport event when hasOtherAccounts is false`() =
            runTest {
                val initialState = DEFAULT_STATE.copy(hasOtherAccounts = false)
                createViewModel(state = initialState).also { viewModel ->
                    viewModel.eventFlow.test {
                        awaitValidateImportRequestEvent()

                        viewModel.trySendAction(VerifyPasswordAction.NavigateBackClick)

                        assertEquals(
                            VerifyPasswordEvent.CancelExport,
                            awaitItem(),
                        )
                    }
                }
            }

        @Test
        fun `ContinueClick with empty input should show error dialog`() = runTest {
            createViewModel(state = DEFAULT_STATE).also {
                it.trySendAction(VerifyPasswordAction.ContinueClick)
                it.stateFlow.test {
                    assertEquals(
                        DEFAULT_STATE.copy(
                            dialog = VerifyPasswordState.DialogState.General(
                                title = BitwardenString.an_error_has_occurred.asText(),
                                message = BitwardenString.validation_field_required.asText(
                                    BitwardenString.master_password.asText(),
                                ),
                            ),
                        ),
                        awaitItem(),
                    )
                    coVerify(exactly = 0) {
                        authRepository.activeUserId
                        authRepository.validatePassword(password = any())
                        authRepository.switchAccount(userId = any())
                    }
                }
            }
        }

        @Test
        fun `ContinueClick should do nothing when the view state is not Content`() = runTest {
            createViewModel(state = DEFAULT_LOADING_STATE).also { viewModel ->
                viewModel.trySendAction(VerifyPasswordAction.ContinueClick)

                assertEquals(DEFAULT_LOADING_STATE, viewModel.stateFlow.value)
                coVerify(exactly = 0) {
                    authRepository.validatePassword(password = any())
                    authRepository.switchAccount(userId = any())
                    vaultRepository.unlockVaultWithMasterPassword(masterPassword = any())
                }
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `ContinueClick with non-empty input should show loading dialog, validate password and send validates password`() =
            runTest {
                val initialState = DEFAULT_STATE.copy(
                    viewState = DEFAULT_CONTENT_VIEW_STATE.copy(input = "mockInput"),
                )
                coEvery { authRepository.validatePassword(password = "mockInput") } just awaits

                createViewModel(state = initialState).also { viewModel ->
                    viewModel.trySendAction(VerifyPasswordAction.ContinueClick)

                    viewModel.stateFlow.test {
                        assertEquals(
                            initialState.copy(
                                dialog = VerifyPasswordState.DialogState.Loading(
                                    message = BitwardenString.loading.asText(),
                                ),
                            ),
                            awaitItem(),
                        )
                    }

                    coVerify(exactly = 1) {
                        authRepository.activeUserId
                        authRepository.validatePassword(password = "mockInput")
                    }
                    coVerify(exactly = 0) {
                        authRepository.switchAccount(userId = any())
                    }
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `ContinueClick with non-empty input should show loading dialog, switch accounts, then validate password when selected account is not active and switch is successful`() =
            runTest {
                val initialState = DEFAULT_STATE.copy(
                    viewState = DEFAULT_CONTENT_VIEW_STATE.copy(input = "mockInput"),
                    accountSummaryListItem = DEFAULT_ACCOUNT_SELECTION_LIST_ITEM
                        .copy(userId = "otherUserId"),
                )
                every {
                    authRepository.switchAccount("otherUserId")
                } returns SwitchAccountResult.AccountSwitched
                coEvery { authRepository.validatePassword(password = "mockInput") } just awaits
                createViewModel(state = initialState).also { viewModel ->
                    viewModel.trySendAction(VerifyPasswordAction.ContinueClick)
                    viewModel.stateFlow.test {
                        assertEquals(
                            initialState.copy(
                                dialog = VerifyPasswordState.DialogState.Loading(
                                    message = BitwardenString.loading.asText(),
                                ),
                            ),
                            awaitItem(),
                        )
                    }
                    coVerify {
                        authRepository.activeUserId
                        authRepository.switchAccount(userId = "otherUserId")
                        authRepository.validatePassword(password = "mockInput")
                    }
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `ContinueClick with non-empty input should show error dialog when switch account is unsuccessful`() =
            runTest {
                val initialState = DEFAULT_STATE.copy(
                    viewState = DEFAULT_CONTENT_VIEW_STATE.copy(input = "mockInput"),
                    accountSummaryListItem = DEFAULT_ACCOUNT_SELECTION_LIST_ITEM
                        .copy(userId = "otherUserId"),
                )
                every {
                    authRepository.switchAccount("otherUserId")
                } returns SwitchAccountResult.NoChange
                coEvery { authRepository.validatePassword(password = "mockInput") } just awaits

                createViewModel(state = initialState).also { viewModel ->
                    viewModel.stateFlow.test {
                        // Await initial state update
                        awaitItem()
                        viewModel.trySendAction(VerifyPasswordAction.ContinueClick)
                        coVerify {
                            authRepository.activeUserId
                            authRepository.switchAccount(userId = "otherUserId")
                        }
                        coVerify(exactly = 0) {
                            authRepository.validatePassword(password = any())
                        }
                        assertEquals(
                            initialState.copy(
                                dialog = VerifyPasswordState.DialogState.General(
                                    title = BitwardenString.an_error_has_occurred.asText(),
                                    message = BitwardenString.generic_error_message.asText(),
                                ),
                            ),
                            awaitItem(),
                        )
                    }
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `ContinueClick with non-empty input should show loading dialog, then unlock vault when vault is locked`() =
            runTest {
                val initialState = DEFAULT_STATE.copy(
                    viewState = DEFAULT_CONTENT_VIEW_STATE.copy(input = "mockInput"),
                )
                every { vaultRepository.isVaultUnlocked(any()) } returns false
                coEvery {
                    vaultRepository.unlockVaultWithMasterPassword(masterPassword = "mockInput")
                } just awaits
                createViewModel(state = initialState).also { viewModel ->
                    viewModel.trySendAction(VerifyPasswordAction.ContinueClick)
                    viewModel.stateFlow.test {
                        assertEquals(
                            initialState.copy(
                                dialog = VerifyPasswordState.DialogState.Loading(
                                    message = BitwardenString.loading.asText(),
                                ),
                            ),
                            awaitItem(),
                        )
                        coVerify {
                            vaultRepository.unlockVaultWithMasterPassword(
                                masterPassword = "mockInput",
                            )
                        }
                    }
                }
            }

        @Test
        fun `PasswordInputChangeReceive should update state`() = runTest {
            createViewModel(state = DEFAULT_STATE).also { viewModel ->
                viewModel.trySendAction(
                    VerifyPasswordAction.PasswordInputChangeReceive("mockInput"),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        viewState = DEFAULT_CONTENT_VIEW_STATE.copy(input = "mockInput"),
                    ),
                    viewModel.stateFlow.value,
                )
            }
        }

        @Test
        fun `PasswordInputChangeReceive should do nothing when the view state is not Content`() =
            runTest {
                createViewModel(state = DEFAULT_LOADING_STATE).also { viewModel ->
                    viewModel.trySendAction(
                        VerifyPasswordAction.PasswordInputChangeReceive("mockInput"),
                    )
                    assertEquals(DEFAULT_LOADING_STATE, viewModel.stateFlow.value)
                }
            }

        @Test
        fun `DismissDialog should update state`() = runTest {
            val initialState = DEFAULT_STATE.copy(
                dialog = VerifyPasswordState.DialogState.Loading(
                    message = BitwardenString.loading.asText(),
                ),
            )
            createViewModel(state = initialState).also { viewModel ->
                viewModel.trySendAction(VerifyPasswordAction.DismissDialog)
                assertEquals(null, viewModel.stateFlow.value.dialog)
            }
        }
    }

    @Nested
    inner class InternalActions {

        @Suppress("MaxLineLength")
        @Test
        fun `ValidatePasswordResultReceive should send PasswordVerified event when result is Success and isValid is true`() =
            runTest {
                createViewModel(state = DEFAULT_STATE).also { viewModel ->
                    viewModel.eventFlow.test {
                        awaitValidateImportRequestEvent()

                        viewModel.trySendAction(
                            VerifyPasswordAction.Internal.ValidatePasswordResultReceive(
                                ValidatePasswordResult.Success(isValid = true),
                            ),
                        )

                        assertEquals(
                            VerifyPasswordEvent.PasswordVerified(DEFAULT_USER_ID),
                            awaitItem(),
                        )
                    }
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `ValidatePasswordResultReceive should show error dialog when result is Success and isValid is false`() =
            runTest {
                createViewModel(state = DEFAULT_STATE).also { viewModel ->
                    viewModel.trySendAction(
                        VerifyPasswordAction.Internal.ValidatePasswordResultReceive(
                            ValidatePasswordResult.Success(isValid = false),
                        ),
                    )
                    assertEquals(
                        VerifyPasswordState.DialogState.General(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.invalid_master_password.asText(),
                            error = null,
                        ),
                        viewModel.stateFlow.value.dialog,
                    )
                }
            }

        @Test
        fun `ValidatePasswordResultReceive should show error dialog when result is Error`() =
            runTest {
                val throwable = Throwable()
                createViewModel(state = DEFAULT_STATE).also { viewModel ->
                    viewModel.trySendAction(
                        VerifyPasswordAction.Internal.ValidatePasswordResultReceive(
                            ValidatePasswordResult.Error(error = throwable),
                        ),
                    )
                    assertEquals(
                        VerifyPasswordState.DialogState.General(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            error = throwable,
                        ),
                        viewModel.stateFlow.value.dialog,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `UnlockVaultResultReceive should send PasswordVerified event when vault unlock result is Success`() =
            runTest {
                createViewModel(state = DEFAULT_STATE).also { viewModel ->
                    viewModel.eventFlow.test {
                        awaitValidateImportRequestEvent()

                        viewModel.trySendAction(
                            VerifyPasswordAction.Internal.UnlockVaultResultReceive(
                                VaultUnlockResult.Success,
                            ),
                        )

                        assertEquals(
                            VerifyPasswordEvent.PasswordVerified(DEFAULT_USER_ID),
                            awaitItem(),
                        )
                    }
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `UnlockVaultResultReceive should show error dialog when vault unlock result is AuthenticationError`() =
            runTest {
                val throwable = Throwable()
                createViewModel(state = DEFAULT_STATE).also { viewModel ->
                    viewModel.trySendAction(
                        VerifyPasswordAction.Internal.UnlockVaultResultReceive(
                            VaultUnlockResult.AuthenticationError(error = throwable),
                        ),
                    )
                    assertEquals(
                        DEFAULT_STATE.copy(
                            dialog = VerifyPasswordState.DialogState.General(
                                title = BitwardenString.an_error_has_occurred.asText(),
                                message = BitwardenString.invalid_master_password.asText(),
                                error = throwable,
                            ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `UnlockVaultResultReceive should show error dialog when vault unlock result is BiometricDecodingError`() =
            runTest {
                val throwable = Throwable()
                createViewModel(state = DEFAULT_STATE).also { viewModel ->
                    viewModel.trySendAction(
                        VerifyPasswordAction.Internal.UnlockVaultResultReceive(
                            VaultUnlockResult.BiometricDecodingError(error = throwable),
                        ),
                    )
                    assertEquals(
                        DEFAULT_STATE.copy(
                            dialog = VerifyPasswordState.DialogState.General(
                                title = BitwardenString.an_error_has_occurred.asText(),
                                message = BitwardenString.generic_error_message.asText(),
                                error = throwable,
                            ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `UnlockVaultResultReceive should show error dialog when vault unlock result is InvalidStateError`() =
            runTest {
                val throwable = Throwable()
                createViewModel(state = DEFAULT_STATE).also { viewModel ->
                    viewModel.trySendAction(
                        VerifyPasswordAction.Internal.UnlockVaultResultReceive(
                            VaultUnlockResult.InvalidStateError(error = throwable),
                        ),
                    )
                    assertEquals(
                        DEFAULT_STATE.copy(
                            dialog = VerifyPasswordState.DialogState.General(
                                title = BitwardenString.an_error_has_occurred.asText(),
                                message = BitwardenString.generic_error_message.asText(),
                                error = throwable,
                            ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `UnlockVaultResultReceive should show error dialog when vault unlock result is GenericError`() =
            runTest {
                val throwable = Throwable()
                createViewModel(state = DEFAULT_STATE).also { viewModel ->
                    viewModel.trySendAction(
                        VerifyPasswordAction.Internal.UnlockVaultResultReceive(
                            VaultUnlockResult.GenericError(error = throwable),
                        ),
                    )
                    assertEquals(
                        DEFAULT_STATE.copy(
                            dialog = VerifyPasswordState.DialogState.General(
                                title = BitwardenString.an_error_has_occurred.asText(),
                                message = BitwardenString.generic_error_message.asText(),
                                error = throwable,
                            ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
            }
    }

    /**
     * Awaits the [VerifyPasswordEvent.ValidateImportRequest] event that is always emitted when the
     * ViewModel is initialized.
     */
    private suspend fun TurbineTestContext<VerifyPasswordEvent>.awaitValidateImportRequestEvent() {
        assertEquals(
            VerifyPasswordEvent.ValidateImportRequest(
                importCredentialsRequestData = DEFAULT_IMPORT_REQUEST,
            ),
            awaitItem(),
        )
    }

    private fun createViewModel(
        state: VerifyPasswordState? = null,
        userId: String = DEFAULT_USER_ID,
    ): VerifyPasswordViewModel = VerifyPasswordViewModel(
        authRepository = authRepository,
        vaultRepository = vaultRepository,
        policyManager = policyManager,
        specialCircumstanceManager = specialCircumstanceManager,
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
            set("userId", userId)
            every {
                toVerifyPasswordArgs()
            } returns VerifyPasswordArgs(
                userId = userId,
                hasOtherAccounts = true,
            )
        },
    )
}

private const val DEFAULT_USER_ID: String = "activeUserId"
private const val DEFAULT_ORGANIZATION_ID: String = "activeOrganizationId"
private val DEFAULT_IMPORT_REQUEST = ImportCredentialsRequestData(
    uri = mockk<Uri>(),
    credentialTypes = setOf("mockCredentialType-1"),
    knownExtensions = setOf(),
)
private val DEFAULT_USER_STATE = UserState(
    activeUserId = DEFAULT_USER_ID,
    accounts = listOf(
        UserState.Account(
            userId = "activeUserId",
            name = "Active User",
            email = "active@bitwarden.com",
            avatarColorHex = "#aa00aa",
            environment = Environment.Prod.Us,
            isPremium = true,
            isPremiumFromSelf = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = listOf(
                createMockOrganization(
                    number = 1,
                    id = DEFAULT_ORGANIZATION_ID,
                    name = "Organization User",
                    role = OrganizationType.USER,
                    keyConnectorUrl = null,
                ),
            ),
            needsMasterPassword = false,
            trustedDevice = null,
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            onboardingStatus = OnboardingStatus.COMPLETE,
            firstTimeState = FirstTimeState(showImportLoginsCard = true),
            isExportable = true,
            creationDate = null,
        ),

        UserState.Account(
            userId = "activeUserId2",
            name = "Active User Two",
            email = "active+two@bitwarden.com",
            avatarColorHex = "#aa00aa",
            environment = Environment.Prod.Us,
            isPremium = true,
            isPremiumFromSelf = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = listOf(
                createMockOrganization(
                    number = 1,
                    id = DEFAULT_ORGANIZATION_ID,
                    name = "Organization User Two",
                    role = OrganizationType.USER,
                    keyConnectorUrl = null,
                ),
            ),
            needsMasterPassword = false,
            trustedDevice = null,
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            onboardingStatus = OnboardingStatus.COMPLETE,
            firstTimeState = FirstTimeState(showImportLoginsCard = true),
            isExportable = true,
            creationDate = null,
        ),
    ),
)
private val DEFAULT_ACCOUNT_SELECTION_LIST_ITEM = AccountSelectionListItem(
    userId = DEFAULT_USER_ID,
    email = DEFAULT_USER_STATE.activeAccount.email,
    avatarColorHex = DEFAULT_USER_STATE.activeAccount.avatarColorHex,
    isItemRestricted = false,
    initials = DEFAULT_USER_STATE.activeAccount.initials,
)
private val DEFAULT_CONTENT_VIEW_STATE = VerifyPasswordState.ViewState.Content(
    title = BitwardenString.verify_your_master_password.asText(),
    subtext = null,
    showResendCodeButton = false,
    input = "",
)
private val OTP_CONTENT_VIEW_STATE = VerifyPasswordState.ViewState.Content(
    title = BitwardenString.verify_your_account_email_address.asText(),
    subtext = BitwardenString
        .enter_the_6_digit_code_that_was_emailed_to_the_address_below
        .asText(),
    showResendCodeButton = true,
    input = "",
)
private val DEFAULT_LOADING_STATE = VerifyPasswordState(
    importRequest = DEFAULT_IMPORT_REQUEST,
    viewState = VerifyPasswordState.ViewState.Loading,
    dialog = null,
    accountSummaryListItem = DEFAULT_ACCOUNT_SELECTION_LIST_ITEM,
    hasOtherAccounts = true,
    hasMasterPassword = true,
)
private val DEFAULT_STATE = DEFAULT_LOADING_STATE.copy(
    viewState = DEFAULT_CONTENT_VIEW_STATE,
)
