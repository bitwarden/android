package com.x8bit.bitwarden.ui.autofill.fido2

import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePinResult
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsResult
import com.x8bit.bitwarden.data.autofill.fido2.model.UserVerificationRequirement
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2GetCredentialsRequest
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.datasource.network.model.OrganizationType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFido2CredentialAutofillView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFido2Credential
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DecryptFido2CredentialAutofillViewResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.autofill.fido2.Fido2Event.CompleteFido2GetCredentialsRequest
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.createMockPasskeyAssertionOptions
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class Fido2ViewModelTest : BaseViewModelTest() {

    private val mutableVaultDataStateFlow =
        MutableStateFlow<DataState<VaultData>>(DataState.Loading)
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(createUserState())
    private val mockAuthRepository = mockk<AuthRepository> {
        every { userStateFlow } returns mutableUserStateFlow
        every { activeUserId } returns mutableUserStateFlow.value?.activeUserId
        every { switchAccount(any()) } returns SwitchAccountResult.AccountSwitched
        coEvery { validatePassword(any()) } returns ValidatePasswordResult.Success(true)
        coEvery { validatePin(any()) } returns ValidatePinResult.Success(true)
    }
    private val mockVaultRepository = mockk<VaultRepository> {
        every { vaultDataStateFlow } returns mutableVaultDataStateFlow
    }
    private val mockSettingsRepository = mockk<SettingsRepository> {
        every { isUnlockWithPinEnabled } returns false
    }
    private val mockFido2CredentialManager = mockk<Fido2CredentialManager> {
        every { isUserVerified } returns true
        every { isUserVerified = any() } just runs
        every { authenticationAttempts = any() } just runs
        every { authenticationAttempts } returns 0
        every { hasAuthenticationAttemptsRemaining() } returns true
        coEvery {
            authenticateFido2Credential(
                any(),
                any(),
                any(),
            )
        } returns Fido2CredentialAssertionResult.Success("mockResponseJson")
    }
    private val mockSpecialCircumstanceManager = mockk<SpecialCircumstanceManager>()

    @Test
    fun `initialState should be correct when specialCircumstance is Fido2GetCredentials`() {
        val mockFido2GetCredentialsRequest = createMockFido2GetCredentialsRequest(number = 1)
        val viewModel = createViewModelForGetCredentials(mockFido2GetCredentialsRequest)

        assertEquals(
            Fido2State(
                requestUserId = mockFido2GetCredentialsRequest.userId,
                fido2GetCredentialsRequest = mockFido2GetCredentialsRequest,
                fido2AssertCredentialRequest = null,
                dialog = Fido2State.DialogState.Loading,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissErrorDialogClick should send CompleteFido2GetCredentialsRequest with Error result when specialCircumstance is Fido2GetCredentials`() =
        runTest {
            val viewModel = createViewModelForGetCredentials()
                .also { it.trySendAction(Fido2Action.DismissErrorDialogClick) }

            viewModel.eventFlow.test {
                assertEquals(
                    Fido2Event.CompleteFido2GetCredentialsRequest(
                        result = Fido2GetCredentialsResult.Error,
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissBitwardenUserVerification should send CompleteFido2GetCredentialsRequest with Cancelled result when specialCircumstance is Fido2GetCredentials`() =
        runTest {
            val mockFido2CredentialAssertionRequest =
                createMockFido2CredentialAssertionRequest(number = 1)
            val viewModel = createViewModelForAssertion(mockFido2CredentialAssertionRequest)
                .also { it.trySendAction(Fido2Action.DeviceUserVerificationCancelled) }

            viewModel.eventFlow.test {
                assertEquals(
                    Fido2Event.CompleteFido2GetCredentialsRequest(
                        result = Fido2GetCredentialsResult.Cancelled,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `initialState should be correct when specialCircumstance is Fido2Assertion`() {
        val mockFido2CredentialAssertionRequest =
            createMockFido2CredentialAssertionRequest(number = 1)
        val viewModel = createViewModelForAssertion(mockFido2CredentialAssertionRequest)

        assertEquals(
            Fido2State(
                requestUserId = mockFido2CredentialAssertionRequest.userId,
                fido2GetCredentialsRequest = null,
                fido2AssertCredentialRequest = mockFido2CredentialAssertionRequest,
                dialog = Fido2State.DialogState.Loading,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissErrorDialogClick should send CompleteFido2Assertion with Error result when specialCircumstance is Fido2Assertion`() =
        runTest {
            val mockCipherView = createMockCipherView(number = 1)
            val mockAutofillView =
                createMockFido2CredentialAutofillView(number = 1, cipherId = mockCipherView.id!!)
            coEvery {
                mockVaultRepository.getDecryptedFido2CredentialAutofillViews(listOf(mockCipherView))
            } returns DecryptFido2CredentialAutofillViewResult.Success(
                fido2CredentialAutofillViews = listOf(mockAutofillView),
            )
            createViewModelForAssertion()
                .also { it.trySendAction(Fido2Action.DismissErrorDialogClick) }
                .eventFlow
                .test {
                    assertEquals(
                        Fido2Event.CompleteFido2GetCredentialsRequest(
                            result = Fido2GetCredentialsResult.Error,
                        ),
                        awaitItem(),
                    )
                }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissBitwardenUserVerification should send CompleteFido2Assertion with Cancelled result when specialCircumstance is Fido2Assertion`() =
        runTest {
            val mockFido2CredentialAssertionRequest =
                createMockFido2CredentialAssertionRequest(number = 1)
            createViewModelForAssertion(mockFido2CredentialAssertionRequest)
                .also { it.trySendAction(Fido2Action.DeviceUserVerificationCancelled) }
                .eventFlow.test {
                    assertEquals(
                        Fido2Event.CompleteFido2GetCredentialsRequest(
                            result = Fido2GetCredentialsResult.Cancelled,
                        ),
                        awaitItem(),
                    )
                }
        }

    @Test
    fun `DeviceUserVerificationFail should show error dialog and mark user as not verified`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also {
                    it.trySendAction(Fido2Action.DeviceUserVerificationFail)
                    verify { mockFido2CredentialManager.isUserVerified = false }
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State.DialogState.Error(
                                R.string.an_error_has_occurred.asText(),
                                R.string.passkey_operation_failed_because_user_could_not_be_verified
                                    .asText(),
                            ),
                        ),
                        it.stateFlow.value,
                    )
                }
        }
    }

    @Test
    fun `DeviceUserVerificationLockOut should show error dialog and mark user as not verified`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also {
                    it.trySendAction(Fido2Action.DeviceUserVerificationLockOut)
                    verify { mockFido2CredentialManager.isUserVerified = false }
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State.DialogState.Error(
                                R.string.an_error_has_occurred.asText(),
                                R.string.passkey_operation_failed_because_user_could_not_be_verified
                                    .asText(),
                            ),
                        ),
                        it.stateFlow.value,
                    )
                }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `DeviceUserVerificationSuccess should authenticate credential, mark user as not verified, clear dialog, and emit CompleteFido2Assertion with result`() =
        runTest {
            val mockCipherView = createMockCipherView(number = 1)
            val mockFido2CredentialAssertionRequest =
                createMockFido2CredentialAssertionRequest(number = 1)
            val mockAuthResult = Fido2CredentialAssertionResult.Success("mockResponseJson")
            coEvery {
                mockFido2CredentialManager.authenticateFido2Credential(
                    userId = mockFido2CredentialAssertionRequest.userId,
                    request = mockFido2CredentialAssertionRequest,
                    selectedCipherView = mockCipherView,
                )
            } returns mockAuthResult

            createViewModelForAssertion(mockFido2CredentialAssertionRequest)
                .also {
                    it.trySendAction(Fido2Action.DeviceUserVerificationSuccess(mockCipherView))
                    coVerify {
                        mockFido2CredentialManager.isUserVerified = true
                        mockFido2CredentialManager.authenticateFido2Credential(
                            userId = mockFido2CredentialAssertionRequest.userId,
                            request = mockFido2CredentialAssertionRequest,
                            selectedCipherView = mockCipherView,
                        )
                        mockFido2CredentialManager.isUserVerified = false
                    }
                    it.eventFlow.test {
                        assertEquals(
                            Fido2Event.CompleteFido2Assertion(result = mockAuthResult),
                            awaitItem(),
                        )
                    }
                }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DeviceUserVerificationNotSupported should set user as not verified and show error dialog when selectedCipherId is null`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this).also {
                it.trySendAction(Fido2Action.DeviceUserVerificationNotSupported(null))
                verify { mockFido2CredentialManager.isUserVerified = false }
                assertEquals(
                    Fido2State(
                        requestUserId = userId,
                        fido2GetCredentialsRequest = null,
                        fido2AssertCredentialRequest = this,
                        dialog = Fido2State.DialogState.Error(
                            R.string.an_error_has_occurred.asText(),
                            R.string.passkey_operation_failed_because_passkey_does_not_exist
                                .asText(),
                        ),
                    ),
                    it.stateFlow.value,
                )
            }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `DeviceUserVerificationNotSupported should show user verification error dialog when UserState is null`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    mutableUserStateFlow.value = null
                    viewModel.trySendAction(
                        Fido2Action.DeviceUserVerificationNotSupported(
                            selectedCipherId = cipherId,
                        ),
                    )
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State.DialogState.Error(
                                title = R.string.an_error_has_occurred.asText(),
                                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                                    .asText(),
                            ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `DeviceUserVerificationNotSupported should show Fido2PinPrompt dialog when unlockWithPin is enabled`() {
        every { mockSettingsRepository.isUnlockWithPinEnabled } returns true
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.DeviceUserVerificationNotSupported(cipherId),
                    )
                    verify { mockSettingsRepository.isUnlockWithPinEnabled }
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State.DialogState.Fido2PinPrompt(cipherId!!),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `DeviceUserVerificationNotSupported should show Fido2MasterPasswordPrompt dialog when unlockWithPin is not enabled and activeAccount has a master password`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.DeviceUserVerificationNotSupported(cipherId),
                    )
                    verify { mockSettingsRepository.isUnlockWithPinEnabled }
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State.DialogState.Fido2MasterPasswordPrompt(cipherId!!),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `DeviceUserVerificationNotSupported should show Fido2PinSetUpPrompt dialog when unlockWithPin is not enabled and activeAccount has no master password`() {
        mutableUserStateFlow.value =
            createUserState(account = DEFAULT_ACCOUNT.copy(hasMasterPassword = false))
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.DeviceUserVerificationNotSupported(cipherId),
                    )
                    verify { mockSettingsRepository.isUnlockWithPinEnabled }
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State.DialogState.Fido2PinSetUpPrompt(cipherId!!),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `DeviceUserVerificationCancelled should set user as not verified, clear dialog, and emit CompleteFido2GetCredentialsRequest with Cancelled`() =
        runTest {
            with(createMockFido2CredentialAssertionRequest(number = 1)) {
                createViewModelForAssertion(this)
                    .also { viewModel ->
                        viewModel.trySendAction(Fido2Action.DeviceUserVerificationCancelled)
                        verify { mockFido2CredentialManager.isUserVerified = false }
                        assertEquals(
                            Fido2State(
                                requestUserId = userId,
                                fido2GetCredentialsRequest = null,
                                fido2AssertCredentialRequest = this,
                                dialog = null,
                            ),
                            viewModel.stateFlow.value,
                        )
                        viewModel.eventFlow.test {
                            assertEquals(
                                Fido2Event.CompleteFido2GetCredentialsRequest(
                                    result = Fido2GetCredentialsResult.Cancelled,
                                ),
                                awaitItem(),
                            )
                        }
                    }
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissBitwardenUserVerification should set user as not verified, clear dialog, and emit CompleteFido2GetCredentialsRequest with Cancelled`() =
        runTest {
            with(createMockFido2CredentialAssertionRequest(number = 1)) {
                createViewModelForAssertion(this)
                    .also { viewModel ->
                        viewModel.trySendAction(Fido2Action.DismissBitwardenUserVerification)
                        verify { mockFido2CredentialManager.isUserVerified = false }
                        assertEquals(
                            Fido2State(
                                requestUserId = userId,
                                fido2GetCredentialsRequest = null,
                                fido2AssertCredentialRequest = this,
                                dialog = null,
                            ),
                            viewModel.stateFlow.value,
                        )
                        viewModel.eventFlow.test {
                            assertEquals(
                                Fido2Event.CompleteFido2GetCredentialsRequest(
                                    result = Fido2GetCredentialsResult.Cancelled,
                                ),
                                awaitItem(),
                            )
                        }
                    }
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordFido2VerificationSubmit should should reset authentication attempts and show error dialog when password validation is Error`() =
        runTest {
            coEvery { mockAuthRepository.validatePassword("incorrectPassword") } returns ValidatePasswordResult.Error
            with(createMockFido2CredentialAssertionRequest(number = 1)) {
                createViewModelForAssertion(this)
                    .also { viewModel ->
                        viewModel.trySendAction(
                            Fido2Action.MasterPasswordFido2VerificationSubmit(
                                password = "incorrectPassword",
                                selectedCipherId = cipherId!!,
                            ),
                        )
                        coVerify {
                            mockAuthRepository.validatePassword(password = "incorrectPassword")
                        }
                        assertEquals(
                            Fido2State(
                                requestUserId = userId,
                                fido2GetCredentialsRequest = null,
                                fido2AssertCredentialRequest = this,
                                dialog = Fido2State.DialogState.Error(
                                    R.string.an_error_has_occurred.asText(),
                                    R.string.passkey_operation_failed_because_user_could_not_be_verified
                                        .asText(),
                                ),
                            ),
                            viewModel.stateFlow.value,
                        )
                    }
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordFido2VerificationSubmit should increment authentication attempts and show error dialog when password validation is invalid and no attempts remain`() =
        runTest {
            every { mockFido2CredentialManager.hasAuthenticationAttemptsRemaining() } returns false

            coEvery {
                mockAuthRepository.validatePassword("incorrectPassword")
            } returns ValidatePasswordResult.Success(isValid = false)

            with(createMockFido2CredentialAssertionRequest(number = 1)) {
                createViewModelForAssertion(this)
                    .also { viewModel ->
                        viewModel.trySendAction(
                            Fido2Action.MasterPasswordFido2VerificationSubmit(
                                password = "incorrectPassword",
                                selectedCipherId = cipherId!!,
                            ),
                        )
                        verify(ordering = Ordering.ORDERED) {
                            mockFido2CredentialManager.authenticationAttempts += 1
                            mockFido2CredentialManager.hasAuthenticationAttemptsRemaining()
                        }
                        assertEquals(
                            Fido2State(
                                requestUserId = userId,
                                fido2GetCredentialsRequest = null,
                                fido2AssertCredentialRequest = this,
                                dialog = Fido2State.DialogState.Error(
                                    R.string.an_error_has_occurred.asText(),
                                    R.string.passkey_operation_failed_because_user_could_not_be_verified
                                        .asText(),
                                ),
                            ),
                            viewModel.stateFlow.value,
                        )
                    }
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordFido2VerificationSubmit should show Fido2MasterPasswordError dialog when password validation is invalid and attempts remain`() {
        coEvery {
            mockAuthRepository.validatePassword("incorrectPassword")
        } returns ValidatePasswordResult.Success(isValid = false)

        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(request = this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.MasterPasswordFido2VerificationSubmit(
                            password = "incorrectPassword",
                            selectedCipherId = cipherId!!,
                        ),
                    )
                    verify(ordering = Ordering.ORDERED) {
                        mockFido2CredentialManager.authenticationAttempts += 1
                        mockFido2CredentialManager.hasAuthenticationAttemptsRemaining()
                    }
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State
                                .DialogState
                                .Fido2MasterPasswordError(
                                    title = null,
                                    message = R.string.invalid_master_password.asText(),
                                    selectedCipherId = cipherId,
                                ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordFido2VerificationSubmit should set user as verified, reset authentication attempts, and authenticate credential when password validation is valid`() =
        runTest {
            with(createMockFido2CredentialAssertionRequest(number = 1)) {
                val mockCipherView = createMockCipherView(number = 1)
                    .copy(id = cipherId)
                mutableVaultDataStateFlow.value = DataState.Loaded(
                    data = VaultData(
                        cipherViewList = listOf(mockCipherView),
                        folderViewList = emptyList(),
                        collectionViewList = emptyList(),
                        sendViewList = emptyList(),
                    ),
                )
                coEvery {
                    mockFido2CredentialManager.authenticateFido2Credential(
                        userId = userId,
                        request = this@with,
                        selectedCipherView = mockCipherView,
                    )
                } returns Fido2CredentialAssertionResult.Success("mockResponseJson")

                createViewModelForAssertion(request = this)
                    .also { viewModel ->
                        viewModel.trySendAction(
                            Fido2Action.MasterPasswordFido2VerificationSubmit(
                                password = "correctPassword",
                                selectedCipherId = cipherId!!,
                            ),
                        )
                        coVerify(ordering = Ordering.ORDERED) {
                            mockFido2CredentialManager.isUserVerified = true
                            mockFido2CredentialManager.authenticationAttempts = 0
                            mockFido2CredentialManager.authenticateFido2Credential(
                                userId = userId,
                                request = this@with,
                                selectedCipherView = mockCipherView,
                            )
                        }
                    }
            }
        }

    @Test
    fun `PinFido2SetUpSubmit should show Fido2PinSetUpError dialog when PIN is blank`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.PinFido2SetUpSubmit(
                            pin = "",
                            selectedCipherId = cipherId!!,
                        ),
                    )

                    assertEquals(
                        Fido2State.DialogState.Fido2PinSetUpError(
                            title = null,
                            message = R.string.validation_field_required.asText(
                                R.string.pin.asText(),
                            ),
                            selectedCipherId = cipherId,
                        ),
                        viewModel.stateFlow.value.dialog,
                    )
                }
        }
    }

    @Test
    fun `PinFido2SetUpSubmit should store unlock PIN when PIN is not blank`() = runTest {
        every {
            mockSettingsRepository.storeUnlockPin(
                pin = "1234",
                shouldRequireMasterPasswordOnRestart = false,
            )
        } just runs

        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.PinFido2SetUpSubmit(
                            pin = "1234",
                            selectedCipherId = cipherId!!,
                        ),
                    )

                    verify {
                        mockSettingsRepository.storeUnlockPin(
                            "1234",
                            shouldRequireMasterPasswordOnRestart = false,
                        )
                    }
                }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PinFido2SetUpSubmit should set user as verified, reset authentication attempts, and authenticate credential when password validation is valid`() =
        runTest {

            every {
                mockSettingsRepository.storeUnlockPin(
                    pin = "1234",
                    shouldRequireMasterPasswordOnRestart = false,
                )
            } just runs

            with(createMockFido2CredentialAssertionRequest(number = 1)) {
                val mockCipherView = createMockCipherView(number = 1)
                    .copy(id = cipherId)

                mutableVaultDataStateFlow.value = DataState.Loaded(
                    data = VaultData(
                        cipherViewList = listOf(mockCipherView),
                        folderViewList = emptyList(),
                        collectionViewList = emptyList(),
                        sendViewList = emptyList(),
                    ),
                )

                createViewModelForAssertion(request = this)
                    .also { viewModel ->
                        viewModel.trySendAction(
                            Fido2Action.PinFido2SetUpSubmit(
                                pin = "1234",
                                selectedCipherId = cipherId!!,
                            ),
                        )

                        coVerify(ordering = Ordering.ORDERED) {
                            mockFido2CredentialManager.isUserVerified = true
                            mockFido2CredentialManager.authenticationAttempts = 0
                            mockFido2CredentialManager.authenticateFido2Credential(
                                userId = userId,
                                request = this@with,
                                selectedCipherView = mockCipherView,
                            )
                        }
                    }
            }
        }

    @Test
    fun `PinFido2VerificationSubmit should validatePin`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.PinFido2VerificationSubmit(
                            pin = "1234",
                            selectedCipherId = cipherId!!,
                        ),
                    )

                    coVerify {
                        mockAuthRepository.validatePin("1234")
                    }
                }
        }
    }

    @Test
    fun `ValidateFido2PinResultReceive should show user verification error when result is Error`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.Internal.ValidateFido2PinResultReceive(
                            result = ValidatePinResult.Error,
                            selectedCipherId = cipherId!!,
                        ),
                    )
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State.DialogState.Error(
                                R.string.an_error_has_occurred.asText(),
                                R.string.passkey_operation_failed_because_user_could_not_be_verified
                                    .asText(),
                            ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ValidateFido2PinResultReceive should increment authentication attempts and show Fido2PinError dialog when result is Invalid and attempts remain`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.Internal.ValidateFido2PinResultReceive(
                            result = ValidatePinResult.Success(isValid = false),
                            selectedCipherId = cipherId!!,
                        ),
                    )

                    verify { mockFido2CredentialManager.authenticationAttempts += 1 }
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State.DialogState.Fido2PinError(
                                title = null,
                                message = R.string.invalid_pin.asText(),
                                selectedCipherId = cipherId,
                            ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ValidateFido2PinResultReceive should increment authentication attempts and show Error dialog when result is Invalid and no attempts remain`() =
        runTest {
            every { mockFido2CredentialManager.hasAuthenticationAttemptsRemaining() } returns false
            with(createMockFido2CredentialAssertionRequest(number = 1)) {
                createViewModelForAssertion(this)
                    .also { viewModel ->
                        viewModel.trySendAction(
                            Fido2Action.Internal.ValidateFido2PinResultReceive(
                                result = ValidatePinResult.Success(isValid = false),
                                selectedCipherId = cipherId!!,
                            ),
                        )
                        verify(ordering = Ordering.ORDERED) {
                            mockFido2CredentialManager.authenticationAttempts += 1
                            mockFido2CredentialManager.hasAuthenticationAttemptsRemaining()
                        }

                        assertEquals(
                            Fido2State(
                                requestUserId = userId,
                                fido2GetCredentialsRequest = null,
                                fido2AssertCredentialRequest = this,
                                dialog = Fido2State.DialogState.Error(
                                    R.string.an_error_has_occurred.asText(),
                                    R.string.passkey_operation_failed_because_user_could_not_be_verified
                                        .asText(),
                                ),
                            ),
                            viewModel.stateFlow.value,
                        )
                    }
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ValidateFido2PinResultReceive should set user as verified, reset authentication attempts, and authenticate credential when result is valid`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            val mockCipherView = createMockCipherView(number = 1)
                .copy(id = cipherId)
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(mockCipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.Internal.ValidateFido2PinResultReceive(
                            result = ValidatePinResult.Success(isValid = true),
                            selectedCipherId = cipherId!!,
                        ),
                    )
                    coVerify(ordering = Ordering.ORDERED) {
                        mockFido2CredentialManager.isUserVerified = true
                        mockFido2CredentialManager.authenticationAttempts = 0
                        mockFido2CredentialManager.authenticateFido2Credential(
                            userId = userId,
                            request = this@with,
                            selectedCipherView = mockCipherView,
                        )
                    }
                }
        }
    }

    @Test
    fun `RetryFido2PinSetUpClick should show Fido2PinSetUpPrompt dialog`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.RetryFido2PinSetUpClick(
                            selectedCipherId = cipherId!!,
                        ),
                    )
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State.DialogState.Fido2PinSetUpPrompt(
                                selectedCipherId =
                                cipherId,
                            ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Test
    fun `RetryFido2PasswordVerificationClick should show Fido2MasterPasswordPrompt dialog`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.RetryFido2PasswordVerificationClick(
                            selectedCipherId = cipherId!!,
                        ),
                    )
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State.DialogState.Fido2MasterPasswordPrompt(
                                selectedCipherId = cipherId,
                            ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Test
    fun `RetryFido2PinVerificationClick should show Fido2PinPrompt dialog`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.RetryFido2PinVerificationClick(
                            selectedCipherId = cipherId!!,
                        ),
                    )
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State.DialogState.Fido2PinPrompt(
                                selectedCipherId = cipherId,
                            ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Test
    fun `VaultDataStateChangeReceive should show error dialog when vault data is error`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.Internal.VaultDataStateChangeReceive(
                            vaultData = DataState.Error(
                                error = IllegalStateException(),
                                data = null,
                            ),
                        ),
                    )
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State.DialogState.Error(
                                title = R.string.an_error_has_occurred.asText(),
                                message = R.string.generic_error_message.asText(),
                            ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Test
    fun `VaultDataStateChangeReceive should show Loading dialog when vault data is loading`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.Internal.VaultDataStateChangeReceive(
                            vaultData = DataState.Loading,
                        ),
                    )
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State.DialogState.Loading,
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Test
    fun `VaultDataStateChangeReceive should show NoNetwork dialog when vault data is no network`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.Internal.VaultDataStateChangeReceive(
                            vaultData = DataState.NoNetwork(data = null),
                        ),
                    )
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State.DialogState.Error(
                                title = R.string.internet_connection_required_title.asText(),
                                message = R.string.internet_connection_required_message.asText(),
                            ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Test
    fun `VaultDataStateChangeReceive should clear dialog state when vault data is pending`() {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.Internal.VaultDataStateChangeReceive(
                            vaultData = DataState.Pending(data = EMPTY_VAULT_DATA),
                        ),
                    )
                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = null,
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `VaultDataStateChangeReceive should clear dialog state and wait when activeUserId does not equal requestUserId`() {
        every { mockAuthRepository.activeUserId } returns "differentUserId"
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            val mockCipherView = createMockCipherView(number = 1)
                .copy(id = cipherId)
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.Internal.VaultDataStateChangeReceive(
                            vaultData = DataState.Loaded(
                                data = VaultData(
                                    cipherViewList = listOf(mockCipherView),
                                    folderViewList = emptyList(),
                                    collectionViewList = emptyList(),
                                    sendViewList = emptyList(),
                                ),
                            ),
                        ),
                    )

                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = null,
                        ),
                        viewModel.stateFlow.value,
                    )

                    verify(exactly = 0) {
                        mockFido2CredentialManager.getPasskeyAssertionOptionsOrNull(any())
                        mockFido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
                    }
                }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `VaultDataStateChangeReceive with Fido2GetCredentialsRequest should show error dialog with message when passkeyAssertionOptions is null`() {
        with(createMockFido2GetCredentialsRequest(number = 1)) {
            every {
                mockFido2CredentialManager.getPasskeyAssertionOptionsOrNull(option.requestJson)
            } returns null
            every { mockAuthRepository.activeUserId } returns userId

            val mockCipherView = createMockCipherView(number = 1)
            createViewModelForGetCredentials(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.Internal.VaultDataStateChangeReceive(
                            vaultData = DataState.Loaded(
                                data = VaultData(
                                    cipherViewList = listOf(mockCipherView),
                                    folderViewList = emptyList(),
                                    collectionViewList = emptyList(),
                                    sendViewList = emptyList(),
                                ),
                            ),
                        ),
                    )

                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = this,
                            fido2AssertCredentialRequest = null,
                            dialog = Fido2State.DialogState.Error(
                                R.string.an_error_has_occurred.asText(),
                                R.string.passkey_operation_failed_because_app_could_not_be_verified
                                    .asText(),
                            ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `VaultDataStateChangeReceive with Fido2GetCredentialsRequest should show error dialog with message when passkeyAssertionOptions relyingPartyId is null`() =
        runTest {
            val mockOptions = createMockPasskeyAssertionOptions(
                number = 1,
                relyingPartyId = null,
            )
            with(createMockFido2GetCredentialsRequest(number = 1)) {
                every {
                    mockFido2CredentialManager.getPasskeyAssertionOptionsOrNull(option.requestJson)
                } returns mockOptions
                every { mockAuthRepository.activeUserId } returns userId

                val mockCipherView = createMockCipherView(number = 1)
                createViewModelForGetCredentials(this)
                    .also { viewModel ->
                        viewModel.trySendAction(
                            Fido2Action.Internal.VaultDataStateChangeReceive(
                                vaultData = DataState.Loaded(
                                    data = VaultData(
                                        cipherViewList = listOf(mockCipherView),
                                        folderViewList = emptyList(),
                                        collectionViewList = emptyList(),
                                        sendViewList = emptyList(),
                                    ),
                                ),
                            ),
                        )

                        assertEquals(
                            Fido2State(
                                requestUserId = userId,
                                fido2GetCredentialsRequest = this,
                                fido2AssertCredentialRequest = null,
                                dialog = Fido2State.DialogState.Error(
                                    R.string.an_error_has_occurred.asText(),
                                    R.string.passkey_operation_failed_because_app_could_not_be_verified
                                        .asText(),
                                ),
                            ),
                            viewModel.stateFlow.value,
                        )
                    }
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `VaultDataStateChangeReceive with Fido2GetCredentialsRequest should decrypt FIDO 2 autofill views`() =
        runTest {
            val mockOptions = createMockPasskeyAssertionOptions(number = 1)
            with(createMockFido2GetCredentialsRequest(number = 1)) {
                val mockCipherView = createMockCipherView(
                    number = 1,
                    fido2Credentials = listOf(createMockSdkFido2Credential(number = 1)),
                )
                val mockCipherViewList = listOf(mockCipherView)
                val mockAutofillView =
                    createMockFido2CredentialAutofillView(
                        number = 1,
                        cipherId = mockCipherView.id!!,
                    )
                every { mockAuthRepository.activeUserId } returns userId
                every {
                    mockFido2CredentialManager.getPasskeyAssertionOptionsOrNull(option.requestJson)
                } returns mockOptions
                coEvery {
                    mockVaultRepository.getDecryptedFido2CredentialAutofillViews(mockCipherViewList)
                } returns DecryptFido2CredentialAutofillViewResult.Success(
                    fido2CredentialAutofillViews = listOf(mockAutofillView),
                )

                createViewModelForGetCredentials(this)
                    .also { viewModel ->
                        viewModel.trySendAction(
                            Fido2Action.Internal.VaultDataStateChangeReceive(
                                vaultData = DataState.Loaded(
                                    data = VaultData(
                                        cipherViewList = mockCipherViewList,
                                        folderViewList = emptyList(),
                                        collectionViewList = emptyList(),
                                        sendViewList = emptyList(),
                                    ),
                                ),
                            ),
                        )

                        coVerify {
                            mockVaultRepository.getDecryptedFido2CredentialAutofillViews(
                                cipherViewList = mockCipherViewList,
                            )
                        }

                        assertEquals(
                            Fido2State(
                                requestUserId = userId,
                                fido2GetCredentialsRequest = this,
                                fido2AssertCredentialRequest = null,
                                dialog = null,
                            ),
                            viewModel.stateFlow.value,
                        )
                    }
            }
        }

    @Test
    fun `GetCredentialsResultReceive should show error dialog when result is Error`() = runTest {
        with(createMockFido2CredentialAssertionRequest(number = 1)) {
            createViewModelForAssertion(this)
                .also { viewModel ->
                    viewModel.trySendAction(
                        Fido2Action.Internal.GetCredentialsResultReceive.Error,
                    )

                    assertEquals(
                        Fido2State(
                            requestUserId = userId,
                            fido2GetCredentialsRequest = null,
                            fido2AssertCredentialRequest = this,
                            dialog = Fido2State.DialogState.Error(
                                R.string.an_error_has_occurred.asText(),
                                R.string.passkey_operation_failed_because_app_could_not_be_verified
                                    .asText(),
                            ),
                        ),
                        viewModel.stateFlow.value,
                    )
                }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `GetCredentialsResultReceive should emit CompleteFido2GetCredentialsRequest when result is Success`() =
        runTest {
            val mockAutofillView = createMockFido2CredentialAutofillView(number = 1)
            with(createMockFido2GetCredentialsRequest(number = 1)) {
                createViewModelForGetCredentials(this)
                    .also { viewModel ->
                        viewModel.trySendAction(
                            Fido2Action.Internal.GetCredentialsResultReceive.Success(
                                request = this,
                                credentials = listOf(mockAutofillView),
                            ),
                        )

                        viewModel.eventFlow.test {
                            val event = awaitItem()
                            assertTrue(event is CompleteFido2GetCredentialsRequest)

                            val result = (event as CompleteFido2GetCredentialsRequest).result
                            assertTrue(result is Fido2GetCredentialsResult.Success)

                            val success = result as Fido2GetCredentialsResult.Success
                            assertEquals(
                                listOf(mockAutofillView),
                                success.credentials,
                            )
                        }
                    }
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `VaultDataStateChangeReceive with Fido2AssertCredentialRequest should show error dialog when selectedCipherView is null`() =
        runTest {
            with(createMockFido2CredentialAssertionRequest(number = 1)) {
                every { mockAuthRepository.activeUserId } returns userId
                createViewModelForAssertion(this)
                    .also { viewModel ->
                        viewModel.trySendAction(
                            Fido2Action.Internal.VaultDataStateChangeReceive(
                                vaultData = DataState.Loaded(EMPTY_VAULT_DATA),
                            ),
                        )

                        assertEquals(
                            Fido2State(
                                requestUserId = userId,
                                fido2GetCredentialsRequest = null,
                                fido2AssertCredentialRequest = this,
                                dialog = Fido2State.DialogState.Error(
                                    R.string.an_error_has_occurred.asText(),
                                    R.string.passkey_operation_failed_because_passkey_does_not_exist
                                        .asText(),
                                ),
                            ),
                            viewModel.stateFlow.value,
                        )
                    }
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `VaultDataStateChangeReceive with Fido2GetCredentialsRequest should show error dialog when data does not contain matching credential`() =
        runTest {
            with(createMockFido2CredentialAssertionRequest(number = 1)) {
                val mockCipherView = createMockCipherView(
                    number = 1,
                    fido2Credentials = listOf(createMockSdkFido2Credential(number = 2)),
                )
                every { mockAuthRepository.activeUserId } returns userId
                createViewModelForAssertion(this)
                    .also { viewModel ->
                        viewModel.trySendAction(
                            Fido2Action.Internal.VaultDataStateChangeReceive(
                                vaultData = DataState.Loaded(
                                    data = VaultData(
                                        cipherViewList = listOf(mockCipherView),
                                        folderViewList = emptyList(),
                                        collectionViewList = emptyList(),
                                        sendViewList = emptyList(),
                                    ),
                                ),
                            ),
                        )

                        assertEquals(
                            Fido2State(
                                requestUserId = userId,
                                fido2GetCredentialsRequest = null,
                                fido2AssertCredentialRequest = this,
                                dialog = Fido2State.DialogState.Error(
                                    R.string.an_error_has_occurred.asText(),
                                    R.string.passkey_operation_failed_because_passkey_does_not_exist
                                        .asText(),
                                ),
                            ),
                            viewModel.stateFlow.value,
                        )
                    }
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `VaultDataStateChangeReceive with Fido2AssertCredentialRequest should show error dialog when passkeyAssertionOptions is null`() =
        runTest {
            with(createMockFido2CredentialAssertionRequest(number = 1)) {
                val mockCipherView = createMockCipherView(
                    number = 1,
                    fido2Credentials = listOf(createMockSdkFido2Credential(number = 1)),
                ).copy(id = cipherId)
                every { mockAuthRepository.activeUserId } returns userId
                every {
                    mockFido2CredentialManager.getPasskeyAssertionOptionsOrNull(requestJson)
                } returns null
                createViewModelForAssertion(this)
                    .also { viewModel ->
                        viewModel.trySendAction(
                            Fido2Action.Internal.VaultDataStateChangeReceive(
                                vaultData = DataState.Loaded(
                                    data = VaultData(
                                        cipherViewList = listOf(mockCipherView),
                                        folderViewList = emptyList(),
                                        collectionViewList = emptyList(),
                                        sendViewList = emptyList(),
                                    ),
                                ),
                            ),
                        )

                        assertEquals(
                            Fido2State(
                                requestUserId = userId,
                                fido2GetCredentialsRequest = null,
                                fido2AssertCredentialRequest = this,
                                dialog = Fido2State.DialogState.Error(
                                    R.string.an_error_has_occurred.asText(),
                                    R.string.passkey_operation_failed_because_app_could_not_be_verified
                                        .asText(),
                                ),
                            ),
                            viewModel.stateFlow.value,
                        )
                    }
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `VaultDataStateChangeReceive with Fido2AssertCredentialRequest should authenticateCredential when user is verified`() =
        runTest {
            every { mockFido2CredentialManager.isUserVerified } returns true
            with(createMockFido2CredentialAssertionRequest(number = 1)) {
                val mockOptions = createMockPasskeyAssertionOptions(number = 1)
                val mockCipherView = createMockCipherView(
                    number = 1,
                    fido2Credentials = listOf(createMockSdkFido2Credential(number = 1)),
                ).copy(id = cipherId)
                every { mockAuthRepository.activeUserId } returns userId
                every {
                    mockFido2CredentialManager.getPasskeyAssertionOptionsOrNull(requestJson)
                } returns mockOptions
                createViewModelForAssertion(this)
                    .also { viewModel ->
                        viewModel.trySendAction(
                            Fido2Action.Internal.VaultDataStateChangeReceive(
                                vaultData = DataState.Loaded(
                                    data = VaultData(
                                        cipherViewList = listOf(mockCipherView),
                                        folderViewList = emptyList(),
                                        collectionViewList = emptyList(),
                                        sendViewList = emptyList(),
                                    ),
                                ),
                            ),
                        )

                        coVerify {
                            mockFido2CredentialManager.authenticateFido2Credential(
                                userId = userId,
                                request = this@with,
                                selectedCipherView = mockCipherView,
                            )
                        }
                    }
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `VaultDataStateChangeReceive with Fido2AssertCredentialRequest should authenticateFido2Credential when user verification is DISCOURAGED`() =
        runTest {
            every { mockFido2CredentialManager.isUserVerified } returns false
            val mockOptions = createMockPasskeyAssertionOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.DISCOURAGED,
            )
            with(createMockFido2CredentialAssertionRequest(number = 1)) {
                val mockCipherView = createMockCipherView(
                    number = 1,
                    fido2Credentials = listOf(createMockSdkFido2Credential(number = 1)),
                ).copy(id = cipherId)
                every { mockAuthRepository.activeUserId } returns userId
                every {
                    mockFido2CredentialManager.getPasskeyAssertionOptionsOrNull(requestJson)
                } returns mockOptions

                createViewModelForAssertion(this)
                    .also { viewModel ->
                        viewModel.trySendAction(
                            Fido2Action.Internal.VaultDataStateChangeReceive(
                                vaultData = DataState.Loaded(
                                    data = VaultData(
                                        cipherViewList = listOf(mockCipherView),
                                        folderViewList = emptyList(),
                                        collectionViewList = emptyList(),
                                        sendViewList = emptyList(),
                                    ),
                                ),
                            ),
                        )

                        coVerify {
                            mockFido2CredentialManager.authenticateFido2Credential(
                                userId = userId,
                                request = this@with,
                                selectedCipherView = mockCipherView,
                            )
                        }
                    }
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `VaultDataStateChangeReceive with Fido2AssertCredentialRequest should emit user verification event when user is not verified and user verification is PREFERRED`() =
        runTest {
            every { mockFido2CredentialManager.isUserVerified } returns false
            val mockOptions = createMockPasskeyAssertionOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.PREFERRED,
            )
            with(createMockFido2CredentialAssertionRequest(number = 1)) {
                val mockCipherView = createMockCipherView(
                    number = 1,
                    fido2Credentials = listOf(createMockSdkFido2Credential(number = 1)),
                ).copy(id = cipherId)
                every { mockAuthRepository.activeUserId } returns userId
                every {
                    mockFido2CredentialManager.getPasskeyAssertionOptionsOrNull(requestJson)
                } returns mockOptions

                createViewModelForAssertion(this)
                    .also { viewModel ->
                        viewModel.trySendAction(
                            Fido2Action.Internal.VaultDataStateChangeReceive(
                                vaultData = DataState.Loaded(
                                    data = VaultData(
                                        cipherViewList = listOf(mockCipherView),
                                        folderViewList = emptyList(),
                                        collectionViewList = emptyList(),
                                        sendViewList = emptyList(),
                                    ),
                                ),
                            ),
                        )

                        viewModel.eventFlow.test {
                            assertEquals(
                                Fido2Event.Fido2UserVerification(
                                    required = false,
                                    selectedCipher = mockCipherView,
                                ),
                                awaitItem(),
                            )
                        }
                    }
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `VaultDataStateChangeReceive with Fido2AssertCredentialRequest should emit user verification event when user is not verified and user verification is REQUIRED`() =
        runTest {
            every { mockFido2CredentialManager.isUserVerified } returns false
            val mockOptions = createMockPasskeyAssertionOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.REQUIRED,
            )
            with(createMockFido2CredentialAssertionRequest(number = 1)) {
                val mockCipherView = createMockCipherView(
                    number = 1,
                    fido2Credentials = listOf(createMockSdkFido2Credential(number = 1)),
                ).copy(id = cipherId)
                every { mockAuthRepository.activeUserId } returns userId
                every {
                    mockFido2CredentialManager.getPasskeyAssertionOptionsOrNull(requestJson)
                } returns mockOptions

                createViewModelForAssertion(this)
                    .also { viewModel ->
                        viewModel.trySendAction(
                            Fido2Action.Internal.VaultDataStateChangeReceive(
                                vaultData = DataState.Loaded(
                                    data = VaultData(
                                        cipherViewList = listOf(mockCipherView),
                                        folderViewList = emptyList(),
                                        collectionViewList = emptyList(),
                                        sendViewList = emptyList(),
                                    ),
                                ),
                            ),
                        )

                        viewModel.eventFlow.test {
                            assertEquals(
                                Fido2Event.Fido2UserVerification(
                                    required = true,
                                    selectedCipher = mockCipherView,
                                ),
                                awaitItem(),
                            )
                        }
                    }
            }
        }

    private fun createViewModelForAssertion(
        request: Fido2CredentialAssertionRequest =
            createMockFido2CredentialAssertionRequest(number = 1),
    ): Fido2ViewModel {
        every {
            mockSpecialCircumstanceManager.specialCircumstance
        } returns SpecialCircumstance.Fido2Assertion(request)
        return createViewModel()
    }

    private fun createViewModelForGetCredentials(
        request: Fido2GetCredentialsRequest = createMockFido2GetCredentialsRequest(
            number = 1,
        ),
    ): Fido2ViewModel {
        every {
            mockSpecialCircumstanceManager.specialCircumstance
        } returns SpecialCircumstance.Fido2GetCredentials(request)
        return createViewModel()
    }

    private fun createViewModel() = Fido2ViewModel(
        authRepository = mockAuthRepository,
        vaultRepository = mockVaultRepository,
        settingsRepository = mockSettingsRepository,
        fido2CredentialManager = mockFido2CredentialManager,
        specialCircumstanceManager = mockSpecialCircumstanceManager,
    )
}

private val DEFAULT_ACCOUNT = UserState.Account(
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
        ),
    ),
    isBiometricsEnabled = true,
    vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
    needsMasterPassword = false,
    trustedDevice = null,
    hasMasterPassword = true,
    isUsingKeyConnector = false,
)

private fun createUserState(account: UserState.Account = DEFAULT_ACCOUNT): UserState {
    return UserState(
        activeUserId = "activeUserId",
        accounts = listOf(account),
        hasPendingAccountAddition = false,
    )
}

private val EMPTY_VAULT_DATA = VaultData(
    cipherViewList = emptyList(),
    folderViewList = emptyList(),
    collectionViewList = emptyList(),
    sendViewList = emptyList(),
)
