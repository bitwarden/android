package com.x8bit.bitwarden

import android.content.Intent
import android.content.pm.SigningInfo
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.util.getPasswordlessRequestDataIntentOrNull
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.data.autofill.fido2.util.getFido2CredentialRequestOrNull
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManagerImpl
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.autofill.util.getAutofillSaveItemOrNull
import com.x8bit.bitwarden.data.autofill.util.getAutofillSelectionDataOrNull
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.garbage.GarbageCollectionManager
import com.x8bit.bitwarden.data.platform.manager.model.PasswordlessRequestData
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.manager.model.VaultStateEvent
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.util.isMyVaultShortcut
import com.x8bit.bitwarden.ui.platform.util.isPasswordGeneratorShortcut
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

class MainViewModelTest : BaseViewModelTest() {

    private val autofillSelectionManager: AutofillSelectionManager = AutofillSelectionManagerImpl()
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(null)
    private val mutableAppThemeFlow = MutableStateFlow(AppTheme.DEFAULT)
    private val mutableScreenCaptureAllowedFlow = MutableStateFlow(true)
    private val fido2CredentialManager = mockk<Fido2CredentialManager>()
    private val settingsRepository = mockk<SettingsRepository> {
        every { appTheme } returns AppTheme.DEFAULT
        every { appThemeStateFlow } returns mutableAppThemeFlow
        every { isScreenCaptureAllowedStateFlow } returns mutableScreenCaptureAllowedFlow
    }
    private val authRepository = mockk<AuthRepository> {
        every { activeUserId } returns DEFAULT_USER_STATE.activeUserId
        every { userStateFlow } returns mutableUserStateFlow
        every { switchAccount(any()) } returns SwitchAccountResult.NoChange
    }
    private val mutableVaultStateEventFlow = bufferedMutableSharedFlow<VaultStateEvent>()
    private val vaultRepository = mockk<VaultRepository> {
        every { vaultStateEventFlow } returns mutableVaultStateEventFlow
    }
    private val garbageCollectionManager = mockk<GarbageCollectionManager> {
        every { tryCollect() } just runs
    }
    private val specialCircumstanceManager = SpecialCircumstanceManagerImpl()
    private val intentManager: IntentManager = mockk {
        every { getShareDataFromIntent(any()) } returns null
    }
    private val savedStateHandle = SavedStateHandle()

    @BeforeEach
    fun setup() {
        mockkStatic(
            Intent::getPasswordlessRequestDataIntentOrNull,
            Intent::getAutofillSaveItemOrNull,
            Intent::getAutofillSelectionDataOrNull,
            Intent::getFido2CredentialRequestOrNull,
        )
        mockkStatic(
            Intent::isMyVaultShortcut,
            Intent::isPasswordGeneratorShortcut,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            Intent::getPasswordlessRequestDataIntentOrNull,
            Intent::getAutofillSaveItemOrNull,
            Intent::getAutofillSelectionDataOrNull,
        )
        unmockkStatic(
            Intent::isMyVaultShortcut,
            Intent::isPasswordGeneratorShortcut,
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
            // Ignore initial screen capture event
            awaitItem()

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
                // Ignore initial screen capture event
                awaitItem()

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
    fun `autofill selection updates should emit CompleteAutofill events`() = runTest {
        val viewModel = createViewModel()
        val cipherView = mockk<CipherView>()
        viewModel.eventFlow.test {
            // Ignore initial screen capture event
            awaitItem()

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
            MainState(
                theme = AppTheme.DEFAULT,
            ),
            viewModel.stateFlow.value,
        )
        viewModel.trySendAction(
            MainAction.Internal.ThemeUpdate(
                theme = AppTheme.DARK,
            ),
        )
        assertEquals(
            MainState(
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
    fun `on ReceiveFirstIntent with share data should set the special circumstance to ShareNewSend`() {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent>()
        val shareData = mockk<IntentManager.ShareData>()
        every { mockIntent.getPasswordlessRequestDataIntentOrNull() } returns null
        every { mockIntent.getAutofillSaveItemOrNull() } returns null
        every { mockIntent.getAutofillSelectionDataOrNull() } returns null
        every { intentManager.getShareDataFromIntent(mockIntent) } returns shareData
        every { mockIntent.isMyVaultShortcut } returns false
        every { mockIntent.isPasswordGeneratorShortcut } returns false

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
        val mockIntent = mockk<Intent>()
        val autofillSelectionData = mockk<AutofillSelectionData>()
        every { mockIntent.getPasswordlessRequestDataIntentOrNull() } returns null
        every { mockIntent.getAutofillSaveItemOrNull() } returns null
        every { mockIntent.getAutofillSelectionDataOrNull() } returns autofillSelectionData
        every { intentManager.getShareDataFromIntent(mockIntent) } returns null
        every { mockIntent.isMyVaultShortcut } returns false
        every { mockIntent.isPasswordGeneratorShortcut } returns false

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
    fun `on ReceiveFirstIntent with an autofill save item should set the special circumstance to AutofillSave`() {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent>()
        val autofillSaveItem = mockk<AutofillSaveItem>()
        every { mockIntent.getPasswordlessRequestDataIntentOrNull() } returns null
        every { mockIntent.getAutofillSaveItemOrNull() } returns autofillSaveItem
        every { mockIntent.getAutofillSelectionDataOrNull() } returns null
        every { intentManager.getShareDataFromIntent(mockIntent) } returns null
        every { mockIntent.isMyVaultShortcut } returns false
        every { mockIntent.isPasswordGeneratorShortcut } returns false

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
        val mockIntent = mockk<Intent>()
        val passwordlessRequestData = mockk<PasswordlessRequestData>()
        every {
            mockIntent.getPasswordlessRequestDataIntentOrNull()
        } returns passwordlessRequestData
        every { mockIntent.getAutofillSaveItemOrNull() } returns null
        every { mockIntent.getAutofillSelectionDataOrNull() } returns null
        every { intentManager.getShareDataFromIntent(mockIntent) } returns null
        every { mockIntent.isMyVaultShortcut } returns false
        every { mockIntent.isPasswordGeneratorShortcut } returns false

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
        val mockIntent = mockk<Intent> {
            every { getFido2CredentialRequestOrNull() } returns fido2CredentialRequest
            every { getPasswordlessRequestDataIntentOrNull() } returns null
            every { getAutofillSelectionDataOrNull() } returns null
            every { getAutofillSaveItemOrNull() } returns null
            every { isMyVaultShortcut } returns false
            every { isPasswordGeneratorShortcut } returns false
        }
        every { intentManager.getShareDataFromIntent(mockIntent) } returns null
        coEvery {
            fido2CredentialManager.validateOrigin(any())
        } returns Fido2ValidateOriginResult.Success

        viewModel.trySendAction(
            MainAction.ReceiveFirstIntent(
                intent = mockIntent,
            ),
        )

        assertEquals(
            SpecialCircumstance.Fido2Save(
                fido2CredentialRequest = fido2CredentialRequest,
            ),
            specialCircumstanceManager.specialCircumstance,
        )
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
        val mockIntent = mockk<Intent> {
            every { getFido2CredentialRequestOrNull() } returns fido2CredentialRequest
            every { getPasswordlessRequestDataIntentOrNull() } returns null
            every { getAutofillSelectionDataOrNull() } returns null
            every { getAutofillSaveItemOrNull() } returns null
            every { isMyVaultShortcut } returns false
            every { isPasswordGeneratorShortcut } returns false
        }
        every { intentManager.getShareDataFromIntent(mockIntent) } returns null
        coEvery {
            fido2CredentialManager.validateOrigin(any())
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
        val mockIntent = mockk<Intent> {
            every { getFido2CredentialRequestOrNull() } returns fido2CredentialRequest
            every { getPasswordlessRequestDataIntentOrNull() } returns null
            every { getAutofillSelectionDataOrNull() } returns null
            every { getAutofillSaveItemOrNull() } returns null
            every { isMyVaultShortcut } returns false
            every { isPasswordGeneratorShortcut } returns false
        }
        every { intentManager.getShareDataFromIntent(mockIntent) } returns null
        coEvery {
            fido2CredentialManager.validateOrigin(any())
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
    fun `on ReceiveNewIntent with share data should set the special circumstance to ShareNewSend`() {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent>()
        val shareData = mockk<IntentManager.ShareData>()
        every { mockIntent.getPasswordlessRequestDataIntentOrNull() } returns null
        every { mockIntent.getAutofillSaveItemOrNull() } returns null
        every { mockIntent.getAutofillSelectionDataOrNull() } returns null
        every { intentManager.getShareDataFromIntent(mockIntent) } returns shareData
        every { mockIntent.isMyVaultShortcut } returns false
        every { mockIntent.isPasswordGeneratorShortcut } returns false

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
    fun `on ReceiveNewIntent with autofill data should set the special circumstance to AutofillSelection`() {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent>()
        val autofillSelectionData = mockk<AutofillSelectionData>()
        every { mockIntent.getPasswordlessRequestDataIntentOrNull() } returns null
        every { mockIntent.getAutofillSaveItemOrNull() } returns null
        every { mockIntent.getAutofillSelectionDataOrNull() } returns autofillSelectionData
        every { intentManager.getShareDataFromIntent(mockIntent) } returns null
        every { mockIntent.isMyVaultShortcut } returns false
        every { mockIntent.isPasswordGeneratorShortcut } returns false

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
        val mockIntent = mockk<Intent>()
        val autofillSaveItem = mockk<AutofillSaveItem>()
        every { mockIntent.getPasswordlessRequestDataIntentOrNull() } returns null
        every { mockIntent.getAutofillSaveItemOrNull() } returns autofillSaveItem
        every { mockIntent.getAutofillSelectionDataOrNull() } returns null
        every { intentManager.getShareDataFromIntent(mockIntent) } returns null
        every { mockIntent.isMyVaultShortcut } returns false
        every { mockIntent.isPasswordGeneratorShortcut } returns false

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
        val mockIntent = mockk<Intent>()
        val passwordlessRequestData = mockk<PasswordlessRequestData>()
        every {
            mockIntent.getPasswordlessRequestDataIntentOrNull()
        } returns passwordlessRequestData
        every { mockIntent.getAutofillSaveItemOrNull() } returns null
        every { mockIntent.getAutofillSelectionDataOrNull() } returns null
        every { intentManager.getShareDataFromIntent(mockIntent) } returns null
        every { mockIntent.isMyVaultShortcut } returns false
        every { mockIntent.isPasswordGeneratorShortcut } returns false

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
        val mockIntent = mockk<Intent> {
            every { getPasswordlessRequestDataIntentOrNull() } returns null
            every { getAutofillSaveItemOrNull() } returns null
            every { getAutofillSelectionDataOrNull() } returns null
            every { isMyVaultShortcut } returns true
            every { isPasswordGeneratorShortcut } returns false
        }
        every { intentManager.getShareDataFromIntent(mockIntent) } returns null

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
    fun `on ReceiveNewIntent with a password generator deeplink data should set the special circumstance to GeneratorShortcut`() {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent> {
            every { getPasswordlessRequestDataIntentOrNull() } returns null
            every { getAutofillSaveItemOrNull() } returns null
            every { getAutofillSelectionDataOrNull() } returns null
            every { isMyVaultShortcut } returns false
            every { isPasswordGeneratorShortcut } returns true
        }
        every { intentManager.getShareDataFromIntent(mockIntent) } returns null

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

    @Suppress("MaxLineLength")
    @Test
    fun `changes in the allowed screen capture value should result in emissions of ScreenCaptureSettingChange `() =
        runTest {
            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                assertEquals(
                    MainEvent.ScreenCaptureSettingChange(isAllowed = true),
                    awaitItem(),
                )

                mutableScreenCaptureAllowedFlow.value = false
                assertEquals(
                    MainEvent.ScreenCaptureSettingChange(isAllowed = false),
                    awaitItem(),
                )
            }
        }

    private fun createViewModel(
        initialSpecialCircumstance: SpecialCircumstance? = null,
    ) = MainViewModel(
        autofillSelectionManager = autofillSelectionManager,
        specialCircumstanceManager = specialCircumstanceManager,
        garbageCollectionManager = garbageCollectionManager,
        intentManager = intentManager,
        settingsRepository = settingsRepository,
        vaultRepository = vaultRepository,
        authRepository = authRepository,
        savedStateHandle = savedStateHandle.apply {
            set(SPECIAL_CIRCUMSTANCE_KEY, initialSpecialCircumstance)
        },
    )
}

private const val SPECIAL_CIRCUMSTANCE_KEY: String = "special-circumstance"
private val DEFAULT_ACCOUNT = UserState.Account(
    userId = "activeUserId",
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
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = "activeUserId",
    accounts = listOf(DEFAULT_ACCOUNT),
)
