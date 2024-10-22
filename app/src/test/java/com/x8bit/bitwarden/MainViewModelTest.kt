package com.x8bit.bitwarden

import android.content.Intent
import android.content.pm.SigningInfo
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.EmailTokenResult
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.util.getCompleteRegistrationDataIntentOrNull
import com.x8bit.bitwarden.data.auth.util.getPasswordlessRequestDataIntentOrNull
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilitySelectionManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilitySelectionManagerImpl
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2CredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2GetCredentialsRequest
import com.x8bit.bitwarden.data.autofill.fido2.util.getFido2AssertionRequestOrNull
import com.x8bit.bitwarden.data.autofill.fido2.util.getFido2CredentialRequestOrNull
import com.x8bit.bitwarden.data.autofill.fido2.util.getFido2GetCredentialsRequestOrNull
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManagerImpl
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.autofill.util.getAutofillSaveItemOrNull
import com.x8bit.bitwarden.data.autofill.util.getAutofillSelectionDataOrNull
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.garbage.GarbageCollectionManager
import com.x8bit.bitwarden.data.platform.manager.model.CompleteRegistrationData
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.PasswordlessRequestData
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.manager.model.VaultStateEvent
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.util.isAccountSecurityShortcut
import com.x8bit.bitwarden.ui.platform.util.isMyVaultShortcut
import com.x8bit.bitwarden.ui.platform.util.isPasswordGeneratorShortcut
import com.x8bit.bitwarden.ui.vault.model.TotpData
import com.x8bit.bitwarden.ui.vault.util.getTotpDataOrNull
import io.mockk.coEvery
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Suppress("LargeClass")
class MainViewModelTest : BaseViewModelTest() {

    private val autofillSelectionManager: AutofillSelectionManager = AutofillSelectionManagerImpl()
    private val accessibilitySelectionManager: AccessibilitySelectionManager =
        AccessibilitySelectionManagerImpl()
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(null)
    private val mutableAppThemeFlow = MutableStateFlow(AppTheme.DEFAULT)
    private val mutableScreenCaptureAllowedFlow = MutableStateFlow(true)
    private val settingsRepository = mockk<SettingsRepository> {
        every { appTheme } returns AppTheme.DEFAULT
        every { appThemeStateFlow } returns mutableAppThemeFlow
        every { isScreenCaptureAllowed } returns true
        every { isScreenCaptureAllowedStateFlow } returns mutableScreenCaptureAllowedFlow
        every { storeUserHasLoggedInValue(any()) } just runs
    }
    private val authRepository = mockk<AuthRepository> {
        every { activeUserId } returns DEFAULT_USER_STATE.activeUserId
        every { userStateFlow } returns mutableUserStateFlow
        every { switchAccount(any()) } returns SwitchAccountResult.NoChange
        coEvery { validateEmailToken(any(), any()) } returns EmailTokenResult.Success
    }
    private val mutableVaultStateEventFlow = bufferedMutableSharedFlow<VaultStateEvent>()
    private val vaultRepository = mockk<VaultRepository> {
        every { vaultStateEventFlow } returns mutableVaultStateEventFlow
    }
    private val garbageCollectionManager = mockk<GarbageCollectionManager> {
        every { tryCollect() } just runs
    }
    private val mockAuthRepository = mockk<AuthRepository>(relaxed = true)
    private val specialCircumstanceManager: SpecialCircumstanceManager =
        SpecialCircumstanceManagerImpl(
            authRepository = mockAuthRepository,
            dispatcherManager = FakeDispatcherManager(),
        )
    private val environmentRepository = mockk<EnvironmentRepository>(relaxed = true) {
        every { loadEnvironmentForEmail(any()) } returns true
    }
    private val intentManager: IntentManager = mockk {
        every { getShareDataFromIntent(any()) } returns null
    }
    private val fido2CredentialManager = mockk<Fido2CredentialManager> {
        every { isUserVerified } returns true
        every { isUserVerified = any() } just runs
    }
    private val savedStateHandle = SavedStateHandle()

    @BeforeEach
    fun setup() {
        mockkStatic(
            Intent::getTotpDataOrNull,
            Intent::getPasswordlessRequestDataIntentOrNull,
            Intent::getAutofillSaveItemOrNull,
            Intent::getAutofillSelectionDataOrNull,
            Intent::getCompleteRegistrationDataIntentOrNull,
            Intent::getFido2AssertionRequestOrNull,
            Intent::getFido2CredentialRequestOrNull,
            Intent::getFido2GetCredentialsRequestOrNull,
        )
        mockkStatic(
            Intent::isMyVaultShortcut,
            Intent::isPasswordGeneratorShortcut,
            Intent::isAccountSecurityShortcut,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            Intent::getTotpDataOrNull,
            Intent::getPasswordlessRequestDataIntentOrNull,
            Intent::getAutofillSaveItemOrNull,
            Intent::getAutofillSelectionDataOrNull,
            Intent::getCompleteRegistrationDataIntentOrNull,
            Intent::getFido2AssertionRequestOrNull,
            Intent::getFido2CredentialRequestOrNull,
            Intent::getFido2GetCredentialsRequestOrNull,
        )
        unmockkStatic(
            Intent::isMyVaultShortcut,
            Intent::isPasswordGeneratorShortcut,
            Intent::isAccountSecurityShortcut,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initialization should set a saved SpecialCircumstance to the SpecialCircumstanceManager if present`() {
        assertNull(specialCircumstanceManager.specialCircumstance)

        val specialCircumstance = mockk<SpecialCircumstance>()
        createViewModel(
            initialSpecialCircumstance = specialCircumstance,
        )

        assertEquals(
            specialCircumstance,
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Test
    fun `user state updates should emit Recreate event and trigger garbage collection`() = runTest {
        val userId1 = "userId1"
        val userId2 = "userId12"
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            mutableUserStateFlow.value = UserState(
                activeUserId = userId1,
                accounts = listOf(
                    mockk<UserState.Account> {
                        every { userId } returns userId1
                    },
                ),
                hasPendingAccountAddition = false,
            )
            assertEquals(MainEvent.Recreate, awaitItem())

            mutableUserStateFlow.value = UserState(
                activeUserId = userId1,
                accounts = listOf(
                    mockk<UserState.Account> {
                        every { userId } returns userId1
                    },
                ),
                hasPendingAccountAddition = true,
            )
            assertEquals(MainEvent.Recreate, awaitItem())

            mutableUserStateFlow.value = UserState(
                activeUserId = userId2,
                accounts = listOf(
                    mockk<UserState.Account> {
                        every { userId } returns userId1
                    },
                    mockk<UserState.Account> {
                        every { userId } returns userId2
                    },
                ),
                hasPendingAccountAddition = true,
            )
            assertEquals(MainEvent.Recreate, awaitItem())
        }
        verify(exactly = 3) {
            garbageCollectionManager.tryCollect()
        }
    }

    @Test
    fun `vault state lock events should emit Recreate event and trigger garbage collection`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                mutableVaultStateEventFlow.tryEmit(VaultStateEvent.Unlocked(userId = "userId"))
                expectNoEvents()

                mutableVaultStateEventFlow.tryEmit(VaultStateEvent.Locked(userId = "userId"))
                assertEquals(MainEvent.Recreate, awaitItem())
            }
            verify(exactly = 1) {
                garbageCollectionManager.tryCollect()
            }
        }

    @Test
    fun `accessibility selection updates should emit CompleteAccessibilityAutofill events`() =
        runTest {
            val viewModel = createViewModel()
            val cipherView = mockk<CipherView>()
            viewModel.eventFlow.test {
                accessibilitySelectionManager.emitAccessibilitySelection(cipherView = cipherView)
                assertEquals(
                    MainEvent.CompleteAccessibilityAutofill(cipherView = cipherView),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `autofill selection updates should emit CompleteAutofill events`() = runTest {
        val viewModel = createViewModel()
        val cipherView = mockk<CipherView>()
        viewModel.eventFlow.test {
            autofillSelectionManager.emitAutofillSelection(cipherView = cipherView)
            assertEquals(
                MainEvent.CompleteAutofill(cipherView = cipherView),
                awaitItem(),
            )
        }
    }

    @Test
    fun `SpecialCircumstance updates should update the SavedStateHandle`() {
        createViewModel()

        assertNull(savedStateHandle[SPECIAL_CIRCUMSTANCE_KEY])

        val specialCircumstance = mockk<SpecialCircumstance>()
        specialCircumstanceManager.specialCircumstance = specialCircumstance

        assertEquals(
            specialCircumstance,
            savedStateHandle[SPECIAL_CIRCUMSTANCE_KEY],
        )
    }

    @Test
    fun `on AppThemeChanged should update state`() {
        val viewModel = createViewModel()

        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
        viewModel.trySendAction(
            MainAction.Internal.ThemeUpdate(
                theme = AppTheme.DARK,
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                theme = AppTheme.DARK,
            ),
            viewModel.stateFlow.value,
        )

        verify {
            settingsRepository.appTheme
            settingsRepository.appThemeStateFlow
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with TOTP data should set the special circumstance to AddTotpLoginItem`() {
        val viewModel = createViewModel()
        val totpData = mockk<TotpData>()
        val mockIntent = createMockIntent(mockTotpData = totpData)

        viewModel.trySendAction(MainAction.ReceiveFirstIntent(intent = mockIntent))
        assertEquals(
            SpecialCircumstance.AddTotpLoginItem(data = totpData),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with share data should set the special circumstance to ShareNewSend`() {
        val viewModel = createViewModel()
        val mockIntent = createMockIntent()
        val shareData = mockk<IntentManager.ShareData>()
        every { intentManager.getShareDataFromIntent(mockIntent) } returns shareData

        viewModel.trySendAction(
            MainAction.ReceiveFirstIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.ShareNewSend(
                data = shareData,
                shouldFinishWhenComplete = true,
            ),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with autofill data should set the special circumstance to AutofillSelection`() {
        val viewModel = createViewModel()
        val autofillSelectionData = mockk<AutofillSelectionData>()
        val mockIntent = createMockIntent(mockAutofillSelectionData = autofillSelectionData)

        viewModel.trySendAction(
            MainAction.ReceiveFirstIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.AutofillSelection(
                autofillSelectionData = autofillSelectionData,
                shouldFinishWhenComplete = true,
            ),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with complete registration data should set the special circumstance to CompleteRegistration if token is valid`() {
        val viewModel = createViewModel()
        val completeRegistrationData = mockk<CompleteRegistrationData> {
            every { email } returns "email"
            every { verificationToken } returns "token"
        }
        val mockIntent = createMockIntent(mockCompleteRegistrationData = completeRegistrationData)
        every { authRepository.activeUserId } returns null

        viewModel.trySendAction(
            MainAction.ReceiveFirstIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.RegistrationEvent.CompleteRegistration(
                completeRegistrationData = completeRegistrationData,
                timestamp = FIXED_CLOCK.millis(),
            ),
            specialCircumstanceManager.specialCircumstance,
        )

        verify(exactly = 0) { authRepository.hasPendingAccountAddition = true }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with complete registration data should set pending account addition to true if there is an active user`() {
        val viewModel = createViewModel()
        val completeRegistrationData = mockk<CompleteRegistrationData> {
            every { email } returns "email"
            every { verificationToken } returns "token"
        }
        val mockIntent = createMockIntent(mockCompleteRegistrationData = completeRegistrationData)
        every { authRepository.activeUserId } returns "activeId"
        every { authRepository.hasPendingAccountAddition = true } just runs

        viewModel.trySendAction(
            MainAction.ReceiveFirstIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.RegistrationEvent.CompleteRegistration(
                completeRegistrationData = completeRegistrationData,
                timestamp = FIXED_CLOCK.millis(),
            ),
            specialCircumstanceManager.specialCircumstance,
        )
        verify { authRepository.hasPendingAccountAddition = true }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with complete registration data should set the special circumstance to ExpiredRegistration if token is not valid`() {
        val viewModel = createViewModel()
        val intentEmail = "email"
        val token = "token"
        val completeRegistrationData = mockk<CompleteRegistrationData> {
            every { email } returns intentEmail
            every { verificationToken } returns token
        }
        val mockIntent = createMockIntent(mockCompleteRegistrationData = completeRegistrationData)
        every { authRepository.activeUserId } returns null
        coEvery {
            authRepository.validateEmailToken(
                email = intentEmail,
                token = token,
            )
        } returns EmailTokenResult.Expired

        viewModel.trySendAction(
            MainAction.ReceiveFirstIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.RegistrationEvent.ExpiredRegistrationLink,
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with complete registration data should show toast if token is not valid but unable to determine reason`() =
        runTest {
            val viewModel = createViewModel()
            val intentEmail = "email"
            val token = "token"
            val completeRegistrationData = mockk<CompleteRegistrationData> {
                every { email } returns intentEmail
                every { verificationToken } returns token
            }
            val mockIntent = createMockIntent(
                mockCompleteRegistrationData = completeRegistrationData,
            )
            every { authRepository.activeUserId } returns null
            coEvery {
                authRepository.validateEmailToken(
                    intentEmail,
                    token,
                )
            } returns EmailTokenResult.Error(message = null)

            viewModel.trySendAction(
                MainAction.ReceiveFirstIntent(
                    intent = mockIntent,
                ),
            )
            viewModel.eventFlow.test {
                assertEquals(
                    MainEvent.ShowToast(R.string.there_was_an_issue_validating_the_registration_token.asText()),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with complete registration data should show toast with custom message if token is not valid but unable to determine reason`() =
        runTest {
            val viewModel = createViewModel()
            val intentEmail = "email"
            val token = "token"
            val completeRegistrationData = mockk<CompleteRegistrationData> {
                every { email } returns intentEmail
                every { verificationToken } returns token
            }
            val mockIntent = createMockIntent(
                mockCompleteRegistrationData = completeRegistrationData,
            )
            every { authRepository.activeUserId } returns null

            val expectedMessage = "expectedMessage"
            coEvery {
                authRepository.validateEmailToken(
                    intentEmail,
                    token,
                )
            } returns EmailTokenResult.Error(message = expectedMessage)

            viewModel.trySendAction(
                MainAction.ReceiveFirstIntent(
                    intent = mockIntent,
                ),
            )
            viewModel.eventFlow.test {
                assertEquals(
                    MainEvent.ShowToast(expectedMessage.asText()),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with an autofill save item should set the special circumstance to AutofillSave`() {
        val viewModel = createViewModel()
        val autofillSaveItem = mockk<AutofillSaveItem>()
        val mockIntent = createMockIntent(mockAutofillSaveItem = autofillSaveItem)

        viewModel.trySendAction(
            MainAction.ReceiveFirstIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.AutofillSave(
                autofillSaveItem = autofillSaveItem,
            ),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with a passwordless request data should set the special circumstance to PasswordlessRequest`() {
        val viewModel = createViewModel()
        val passwordlessRequestData = DEFAULT_PASSWORDLESS_REQUEST_DATA
        val mockIntent = createMockIntent(mockPasswordlessRequestData = passwordlessRequestData)

        viewModel.trySendAction(
            MainAction.ReceiveFirstIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.PasswordlessRequest(
                passwordlessRequestData = passwordlessRequestData,
                shouldFinishWhenComplete = true,
            ),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with fido2 request data should set the special circumstance to Fido2Save`() {
        val viewModel = createViewModel()
        val fido2CredentialRequest = Fido2CredentialRequest(
            userId = DEFAULT_USER_STATE.activeUserId,
            requestJson = """{"mockRequestJson":1}""",
            packageName = "com.x8bit.bitwarden",
            signingInfo = SigningInfo(),
            origin = "mockOrigin",
        )
        val fido2Intent = createMockIntent(mockFido2CredentialRequest = fido2CredentialRequest)

        coEvery {
            fido2CredentialManager.validateOrigin(
                fido2CredentialRequest.callingAppInfo,
                fido2CredentialRequest.requestJson,
            )
        } returns Fido2ValidateOriginResult.Success

        viewModel.trySendAction(
            MainAction.ReceiveFirstIntent(
                intent = fido2Intent,
            ),
        )

        assertEquals(
            SpecialCircumstance.Fido2Save(
                fido2CredentialRequest = fido2CredentialRequest,
            ),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Test
    fun `on ReceiveFirstIntent with fido2 request data should set the user to unverified`() {
        val viewModel = createViewModel()
        val fido2Intent = createMockIntent(
            mockFido2CredentialRequest = createMockFido2CredentialRequest(number = 1),
        )

        viewModel.trySendAction(
            MainAction.ReceiveFirstIntent(
                intent = fido2Intent,
            ),
        )

        verify {
            fido2CredentialManager.isUserVerified = false
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with fido2 request data should switch users if active user is not selected`() {
        mutableUserStateFlow.value = DEFAULT_USER_STATE
        val viewModel = createViewModel()
        val fido2CredentialRequest = Fido2CredentialRequest(
            userId = "selectedUserId",
            requestJson = """{"mockRequestJson":1}""",
            packageName = "com.x8bit.bitwarden",
            signingInfo = SigningInfo(),
            origin = "mockOrigin",
        )
        val mockIntent = createMockIntent(mockFido2CredentialRequest = fido2CredentialRequest)
        coEvery {
            fido2CredentialManager.validateOrigin(
                fido2CredentialRequest.callingAppInfo,
                fido2CredentialRequest.requestJson,
            )
        } returns Fido2ValidateOriginResult.Success

        viewModel.trySendAction(
            MainAction.ReceiveFirstIntent(
                intent = mockIntent,
            ),
        )

        verify(exactly = 1) { authRepository.switchAccount(fido2CredentialRequest.userId) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with fido2 request data should not switch users if active user is selected`() {
        val viewModel = createViewModel()
        val fido2CredentialRequest = Fido2CredentialRequest(
            userId = DEFAULT_USER_STATE.activeUserId,
            requestJson = """{"mockRequestJson":1}""",
            packageName = "com.x8bit.bitwarden",
            signingInfo = SigningInfo(),
            origin = "mockOrigin",
        )
        val mockIntent = createMockIntent(mockFido2CredentialRequest = fido2CredentialRequest)
        coEvery {
            fido2CredentialManager.validateOrigin(
                fido2CredentialRequest.callingAppInfo,
                fido2CredentialRequest.requestJson,
            )
        } returns Fido2ValidateOriginResult.Success

        viewModel.trySendAction(
            MainAction.ReceiveFirstIntent(
                intent = mockIntent,
            ),
        )

        verify(exactly = 0) { authRepository.switchAccount(fido2CredentialRequest.userId) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with FIDO 2 assertion request data should set the special circumstance to Fido2Assertion`() {
        val viewModel = createViewModel()
        val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
        val fido2AssertionIntent = createMockIntent(
            mockFido2CredentialAssertionRequest = mockAssertionRequest,
        )

        viewModel.trySendAction(
            MainAction.ReceiveFirstIntent(
                intent = fido2AssertionIntent,
            ),
        )

        assertEquals(
            SpecialCircumstance.Fido2Assertion(mockAssertionRequest),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with fido2 get credentials request data should set the special circumstance to Fido2GetCredentials`() {
        val viewModel = createViewModel()
        val mockGetCredentialsRequest = createMockFido2GetCredentialsRequest(number = 1)
        val mockIntent = createMockIntent(
            mockFido2GetCredentialsRequest = mockGetCredentialsRequest,
        )

        viewModel.trySendAction(
            MainAction.ReceiveFirstIntent(
                intent = mockIntent,
            ),
        )

        assertEquals(
            SpecialCircumstance.Fido2GetCredentials(mockGetCredentialsRequest),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveNewIntent with share data should set the special circumstance to ShareNewSend`() {
        val viewModel = createViewModel()
        val mockIntent = createMockIntent()
        val shareData = mockk<IntentManager.ShareData>()
        every { intentManager.getShareDataFromIntent(mockIntent) } returns shareData

        viewModel.trySendAction(
            MainAction.ReceiveNewIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.ShareNewSend(
                data = shareData,
                shouldFinishWhenComplete = false,
            ),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveNewIntent with TOTP data should set the special circumstance to AddTotpLoginItem`() {
        val viewModel = createViewModel()
        val totpData = mockk<TotpData>()
        val mockIntent = createMockIntent(mockTotpData = totpData)

        viewModel.trySendAction(MainAction.ReceiveNewIntent(intent = mockIntent))
        assertEquals(
            SpecialCircumstance.AddTotpLoginItem(data = totpData),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveNewIntent with autofill data should set the special circumstance to AutofillSelection`() {
        val viewModel = createViewModel()
        val autofillSelectionData = mockk<AutofillSelectionData>()
        val mockIntent = createMockIntent(mockAutofillSelectionData = autofillSelectionData)

        viewModel.trySendAction(
            MainAction.ReceiveNewIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.AutofillSelection(
                autofillSelectionData = autofillSelectionData,
                shouldFinishWhenComplete = false,
            ),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveNewIntent with an autofill save item should set the special circumstance to AutofillSave`() {
        val viewModel = createViewModel()
        val autofillSaveItem = mockk<AutofillSaveItem>()
        val mockIntent = createMockIntent(mockAutofillSaveItem = autofillSaveItem)

        viewModel.trySendAction(
            MainAction.ReceiveNewIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.AutofillSave(
                autofillSaveItem = autofillSaveItem,
            ),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveNewIntent with a passwordless auth request data should set the special circumstance to PasswordlessRequest`() {
        val viewModel = createViewModel()
        val passwordlessRequestData = DEFAULT_PASSWORDLESS_REQUEST_DATA
        val mockIntent = createMockIntent(mockPasswordlessRequestData = passwordlessRequestData)

        viewModel.trySendAction(
            MainAction.ReceiveNewIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.PasswordlessRequest(
                passwordlessRequestData = passwordlessRequestData,
                shouldFinishWhenComplete = false,
            ),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveNewIntent with a Vault deeplink data should set the special circumstance to VaultShortcut`() {
        val viewModel = createViewModel()
        val mockIntent = createMockIntent(mockIsMyVaultShortcut = true)

        viewModel.trySendAction(
            MainAction.ReceiveNewIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.VaultShortcut,
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveNewIntent with account security deeplink data should set the special circumstance to AccountSecurityShortcut `() {
        val viewModel = createViewModel()
        val mockIntent = createMockIntent(mockIsAccountSecurityShortcut = true)

        viewModel.trySendAction(
            MainAction.ReceiveNewIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.AccountSecurityShortcut,
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveNewIntent with a password generator deeplink data should set the special circumstance to GeneratorShortcut`() {
        val viewModel = createViewModel()
        val mockIntent = createMockIntent(mockIsPasswordGeneratorShortcut = true)

        viewModel.trySendAction(
            MainAction.ReceiveNewIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.GeneratorShortcut,
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Test
    fun `changes in the allowed screen capture value should update the state`() {
        val viewModel = createViewModel()

        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)

        mutableScreenCaptureAllowedFlow.value = false

        assertEquals(
            DEFAULT_STATE.copy(isScreenCaptureAllowed = false),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `send NavigateToDebugMenu action when OpenDebugMenu action is sent`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(MainAction.OpenDebugMenu)

        viewModel.eventFlow.test {
            assertEquals(MainEvent.NavigateToDebugMenu, awaitItem())
        }
    }

    @Test
    fun `store logged in user status of the any active users on startup if they exist`() = runTest {
        mutableUserStateFlow.value = DEFAULT_USER_STATE
        createViewModel()
        verify(exactly = 1) {
            settingsRepository.storeUserHasLoggedInValue(userId = DEFAULT_USER_STATE.activeUserId)
        }
    }

    @Test
    fun `store logged in user should recorded each active user`() = runTest {
        val userId2 = "activeUserId2"
        val multipleUserState = DEFAULT_USER_STATE.copy(
            accounts = listOf(
                DEFAULT_ACCOUNT,
                DEFAULT_ACCOUNT.copy(userId = userId2),
            ),
        )
        mutableUserStateFlow.value = multipleUserState
        createViewModel()
        verify(exactly = 1) {
            settingsRepository.storeUserHasLoggedInValue(userId = DEFAULT_USER_STATE.activeUserId)
            settingsRepository.storeUserHasLoggedInValue(userId = userId2)
        }
    }

    @Test
    fun `store logged in should not be called when there are no active users`() = runTest {
        mutableUserStateFlow.value = null
        createViewModel()
        verify(exactly = 0) {
            settingsRepository.storeUserHasLoggedInValue(userId = DEFAULT_USER_STATE.activeUserId)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveNewIntent with a passwordless auth request data userId that doesn't match activeUserId and the vault is not locked should switchAccount`() {
        val userId = "userId"
        val viewModel = createViewModel()
        val passwordlessRequestData = mockk<PasswordlessRequestData>()
        val mockIntent = createMockIntent(mockPasswordlessRequestData = passwordlessRequestData)
        every { vaultRepository.isVaultUnlocked(ACTIVE_USER_ID) } returns false
        every { passwordlessRequestData.userId } returns userId

        viewModel.trySendAction(
            MainAction.ReceiveNewIntent(
                intent = mockIntent,
            ),
        )

        verify { authRepository.switchAccount(userId) }
    }

    private fun createViewModel(
        initialSpecialCircumstance: SpecialCircumstance? = null,
    ) = MainViewModel(
        accessibilitySelectionManager = accessibilitySelectionManager,
        autofillSelectionManager = autofillSelectionManager,
        specialCircumstanceManager = specialCircumstanceManager,
        garbageCollectionManager = garbageCollectionManager,
        fido2CredentialManager = fido2CredentialManager,
        intentManager = intentManager,
        settingsRepository = settingsRepository,
        vaultRepository = vaultRepository,
        authRepository = authRepository,
        clock = FIXED_CLOCK,
        environmentRepository = environmentRepository,
        savedStateHandle = savedStateHandle.apply {
            set(SPECIAL_CIRCUMSTANCE_KEY, initialSpecialCircumstance)
        },
    )
}

private val DEFAULT_STATE: MainState = MainState(
    theme = AppTheme.DEFAULT,
    isScreenCaptureAllowed = true,
)

private val DEFAULT_FIRST_TIME_STATE = FirstTimeState(
    showImportLoginsCard = true,
)

private const val SPECIAL_CIRCUMSTANCE_KEY: String = "special-circumstance"
private const val ACTIVE_USER_ID: String = "activeUserId"
private val DEFAULT_ACCOUNT = UserState.Account(
    userId = ACTIVE_USER_ID,
    name = "Active User",
    email = "active@bitwarden.com",
    environment = Environment.Us,
    avatarColorHex = "#aa00aa",
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
    firstTimeState = DEFAULT_FIRST_TIME_STATE,
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = "activeUserId",
    accounts = listOf(DEFAULT_ACCOUNT),
)

private val DEFAULT_PASSWORDLESS_REQUEST_DATA = PasswordlessRequestData(
    userId = "activeUserId",
    loginRequestId = "",
)

@Suppress("LongParameterList")
private fun createMockIntent(
    mockTotpData: TotpData? = null,
    mockPasswordlessRequestData: PasswordlessRequestData? = null,
    mockAutofillSaveItem: AutofillSaveItem? = null,
    mockAutofillSelectionData: AutofillSelectionData? = null,
    mockCompleteRegistrationData: CompleteRegistrationData? = null,
    mockFido2CredentialAssertionRequest: Fido2CredentialAssertionRequest? = null,
    mockFido2CredentialRequest: Fido2CredentialRequest? = null,
    mockFido2GetCredentialsRequest: Fido2GetCredentialsRequest? = null,
    mockIsMyVaultShortcut: Boolean = false,
    mockIsPasswordGeneratorShortcut: Boolean = false,
    mockIsAccountSecurityShortcut: Boolean = false,
): Intent = mockk<Intent> {
    every { getTotpDataOrNull() } returns mockTotpData
    every { getPasswordlessRequestDataIntentOrNull() } returns mockPasswordlessRequestData
    every { getAutofillSaveItemOrNull() } returns mockAutofillSaveItem
    every { getAutofillSelectionDataOrNull() } returns mockAutofillSelectionData
    every { getCompleteRegistrationDataIntentOrNull() } returns mockCompleteRegistrationData
    every { getFido2AssertionRequestOrNull() } returns mockFido2CredentialAssertionRequest
    every { getFido2CredentialRequestOrNull() } returns mockFido2CredentialRequest
    every { getFido2GetCredentialsRequestOrNull() } returns mockFido2GetCredentialsRequest
    every { isMyVaultShortcut } returns mockIsMyVaultShortcut
    every { isPasswordGeneratorShortcut } returns mockIsPasswordGeneratorShortcut
    every { isAccountSecurityShortcut } returns mockIsAccountSecurityShortcut
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
