package com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
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
import org.junit.jupiter.api.Test

class VerifyPasswordViewModelTest : BaseViewModelTest() {

    private val mutableUserStateFlow = MutableStateFlow(DEFAULT_USER_STATE)
    private val authRepository = mockk<AuthRepository> {
        every { userStateFlow } returns mutableUserStateFlow
        every { activeUserId } returns DEFAULT_USER_ID
    }
    private val vaultRepository = mockk<VaultRepository> {
        every { isVaultUnlocked(any()) } returns true
        coEvery {
            unlockVaultWithMasterPassword(masterPassword = any())
        } returns VaultUnlockResult.Success
    }
    private val policyManager = mockk<PolicyManager> {
        every { getActivePolicies(PolicyTypeJson.RESTRICT_ITEM_TYPES) } returns listOf(
            createMockPolicy(
                number = 1,
                organizationId = DEFAULT_ORGANIZATION_ID,
                isEnabled = false,
            ),
        )
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

    @Test
    fun `initial state should be correct when account is not restricted`() = runTest {
        createViewModel()
            .also {
                assertEquals(
                    VerifyPasswordState(
                        AccountSelectionListItem(
                            userId = DEFAULT_USER_ID,
                            email = DEFAULT_USER_STATE.activeAccount.email,
                            avatarColorHex = DEFAULT_USER_STATE.activeAccount.avatarColorHex,
                            isItemRestricted = false,
                            initials = DEFAULT_USER_STATE.activeAccount.initials,
                        ),
                    ),
                    it.stateFlow.value,
                )
            }
    }

    @Test
    fun `initial state should be correct when account has item restrictions`() = runTest {
        every {
            policyManager.getActivePolicies(PolicyTypeJson.RESTRICT_ITEM_TYPES)
        } returns listOf(
            createMockPolicy(
                number = 1,
                organizationId = DEFAULT_ORGANIZATION_ID,
                isEnabled = true,
            ),
        )

        createViewModel()
            .also {
                assertEquals(
                    VerifyPasswordState(
                        accountSummaryListItem = DEFAULT_ACCOUNT_SELECTION_LIST_ITEM
                            .copy(isItemRestricted = true),
                    ),
                    it.stateFlow.value,
                )
            }
    }

    @Test
    fun `NavigateBackClick should send NavigateBack event`() = runTest {
        createViewModel().also {
            it.trySendAction(VerifyPasswordAction.NavigateBackClick)
            it.eventFlow.test {
                assertEquals(
                    VerifyPasswordEvent.NavigateBack,
                    awaitItem(),
                )
            }
        }
    }

    @Test
    fun `UnlockClick with empty input should show error dialog`() = runTest {
        createViewModel().also {
            it.trySendAction(VerifyPasswordAction.UnlockClick)
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

    @Suppress("MaxLineLength")
    @Test
    fun `UnlockClick with non-empty input should show loading dialog, validate password and send validates password`() =
        runTest {
            val initialState = DEFAULT_STATE.copy(input = "mockInput")
            coEvery { authRepository.validatePassword(password = "mockInput") } just awaits

            createViewModel(state = initialState).also { viewModel ->
                viewModel.trySendAction(VerifyPasswordAction.UnlockClick)

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
    fun `UnlockClick with non-empty input should show loading dialog, switch accounts, then validate password when selected account is not active and switch is successful`() =
        runTest {
            val initialState = DEFAULT_STATE.copy(
                accountSummaryListItem = DEFAULT_ACCOUNT_SELECTION_LIST_ITEM
                    .copy(userId = "otherUserId"),
                input = "mockInput",
            )
            every {
                authRepository.switchAccount("otherUserId")
            } returns SwitchAccountResult.AccountSwitched
            coEvery { authRepository.validatePassword(password = "mockInput") } just awaits
            createViewModel(state = initialState).also { viewModel ->
                viewModel.trySendAction(VerifyPasswordAction.UnlockClick)
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
    fun `UnlockClick with non-empty input should show error dialog when switch account is unsuccessful`() =
        runTest {
            val initialState = DEFAULT_STATE.copy(
                accountSummaryListItem = DEFAULT_ACCOUNT_SELECTION_LIST_ITEM
                    .copy(userId = "otherUserId"),
                input = "mockInput",
            )
            every {
                authRepository.switchAccount("otherUserId")
            } returns SwitchAccountResult.NoChange
            coEvery { authRepository.validatePassword(password = "mockInput") } just awaits

            createViewModel(state = initialState).also { viewModel ->
                viewModel.stateFlow.test {
                    // Await initial state update
                    awaitItem()
                    viewModel.trySendAction(VerifyPasswordAction.UnlockClick)
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
    fun `UnlockClick with non-empty input should show loading dialog, then unlock vault when vault is locked`() =
        runTest {
            val initialState = DEFAULT_STATE.copy(input = "mockInput")
            every { vaultRepository.isVaultUnlocked(any()) } returns false
            coEvery {
                vaultRepository.unlockVaultWithMasterPassword(masterPassword = "mockInput")
            } just awaits
            createViewModel(state = initialState).also { viewModel ->
                viewModel.trySendAction(VerifyPasswordAction.UnlockClick)
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
                        vaultRepository.unlockVaultWithMasterPassword(masterPassword = "mockInput")
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
                DEFAULT_STATE.copy(input = "mockInput"),
                viewModel.stateFlow.value,
            )
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

    @Suppress("MaxLineLength")
    @Test
    fun `ValidatePasswordResultReceive should send PasswordVerified event and clear input when result is Success and isValid is true`() =
        runTest {
            createViewModel(state = DEFAULT_STATE.copy(input = "mockInput"))
                .also { viewModel ->
                    viewModel.trySendAction(
                        VerifyPasswordAction.Internal.ValidatePasswordResultReceive(
                            ValidatePasswordResult.Success(isValid = true),
                        ),
                    )
                    assertEquals(
                        DEFAULT_STATE.copy(input = ""),
                        viewModel.stateFlow.value,
                    )
                    viewModel.eventFlow.test {
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
            createViewModel().also { viewModel ->
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
    fun `ValidatePasswordResultReceive should show error dialog when result is Error`() = runTest {
        val throwable = Throwable()
        createViewModel().also { viewModel ->
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
    fun `UnlockVaultResultReceive should send PasswordVerified event and clear inputs when vault unlock result is Success`() =
        runTest {
            createViewModel(state = DEFAULT_STATE.copy(input = "mockInput"))
                .also { viewModel ->
                    viewModel.trySendAction(
                        VerifyPasswordAction.Internal.UnlockVaultResultReceive(
                            VaultUnlockResult.Success,
                        ),
                    )
                    assertEquals(
                        DEFAULT_STATE.copy(input = ""),
                        viewModel.stateFlow.value,
                    )
                    viewModel.eventFlow.test {
                        assertEquals(
                            VerifyPasswordEvent.PasswordVerified(DEFAULT_USER_ID),
                            awaitItem(),
                        )
                    }
                }
        }

    @Test
    fun `UnlockVaultResultReceive should show error dialog when vault unlock result is Error`() =
        runTest {
            val throwable = Throwable()
            createViewModel().also { viewModel ->
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

    @Suppress("MaxLineLength")
    @Test
    fun `UnlockVaultResultReceive should show error dialog when vault unlock result is AuthenticationError`() =
        runTest {
            val throwable = Throwable()
            createViewModel().also { viewModel ->
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
            createViewModel().also { viewModel ->
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
            createViewModel().also { viewModel ->
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
            createViewModel().also { viewModel ->
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

    private fun createViewModel(
        state: VerifyPasswordState? = null,
        userId: String = DEFAULT_USER_ID,
    ): VerifyPasswordViewModel = VerifyPasswordViewModel(
        authRepository = authRepository,
        vaultRepository = vaultRepository,
        policyManager = policyManager,
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
            set("userId", userId)
            every {
                toVerifyPasswordArgs()
            } returns VerifyPasswordArgs(
                userId = DEFAULT_USER_ID,
            )
        },
    )
}

private const val DEFAULT_USER_ID: String = "activeUserId"
private const val DEFAULT_ORGANIZATION_ID: String = "activeOrganizationId"
private val DEFAULT_USER_STATE = UserState(
    activeUserId = DEFAULT_USER_ID,
    accounts = listOf(
        UserState.Account(
            userId = "activeUserId",
            name = "Active User",
            email = "active@bitwarden.com",
            avatarColorHex = "#aa00aa",
            environment = Environment.Us,
            isPremium = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = listOf(
                Organization(
                    id = DEFAULT_ORGANIZATION_ID,
                    name = "Organization User",
                    shouldUseKeyConnector = false,
                    shouldManageResetPassword = false,
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
private val DEFAULT_STATE = VerifyPasswordState(
    accountSummaryListItem = DEFAULT_ACCOUNT_SELECTION_LIST_ITEM,
    input = "",
    dialog = null,
)
