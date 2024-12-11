package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import android.content.pm.SigningInfo
import android.net.Uri
import androidx.credentials.provider.CallingAppInfo
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePinResult
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilitySelectionManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilitySelectionManagerImpl
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2OriginManager
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.data.autofill.fido2.model.UserVerificationRequirement
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2CredentialRequest
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManagerImpl
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.platform.repository.util.baseWebSendUrl
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFido2CredentialList
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DecryptFido2CredentialAutofillViewResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.createMockPasskeyAssertionOptions
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.createMockPasskeyAttestationOptions
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.createMockDisplayItemForCipher
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toActiveAccountSummary
import com.x8bit.bitwarden.ui.vault.model.TotpData
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import io.mockk.Ordering
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Suppress("LargeClass")
class VaultItemListingViewModelTest : BaseViewModelTest() {

    private val accessibilitySelectionManager: AccessibilitySelectionManager =
        AccessibilitySelectionManagerImpl()
    private val autofillSelectionManager: AutofillSelectionManager = AutofillSelectionManagerImpl()

    private var mockFilteredCiphers: List<CipherView>? = null
    private val cipherMatchingManager: CipherMatchingManager = object : CipherMatchingManager {
        // Just do no-op filtering unless we have mock filtered data
        override suspend fun filterCiphersForMatches(
            ciphers: List<CipherView>,
            matchUri: String,
        ): List<CipherView> = mockFilteredCiphers ?: ciphers
    }

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    private val clipboardManager: BitwardenClipboardManager = mockk {
        every { setText(any<String>()) } just runs
    }

    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository = mockk<AuthRepository> {
        every { activeUserId } answers { mutableUserStateFlow.value?.activeUserId }
        every { userStateFlow } returns mutableUserStateFlow
        every { logout() } just runs
        every { logout(any()) } just runs
        every { switchAccount(any()) } returns SwitchAccountResult.AccountSwitched
    }
    private val mutableVaultDataStateFlow =
        MutableStateFlow<DataState<VaultData>>(DataState.Loading)
    private val vaultRepository: VaultRepository = mockk {
        every { vaultFilterType } returns VaultFilterType.AllVaults
        every { vaultDataStateFlow } returns mutableVaultDataStateFlow
        every { lockVault(any()) } just runs
        every { sync(forced = any()) } just runs
        coEvery {
            getDecryptedFido2CredentialAutofillViews(any())
        } returns DecryptFido2CredentialAutofillViewResult.Error
    }
    private val environmentRepository: EnvironmentRepository = mockk {
        every { environment } returns Environment.Us
        every { environmentStateFlow } returns mockk()
    }

    private val mutablePullToRefreshEnabledFlow = MutableStateFlow(false)
    private val mutableIsIconLoadingDisabledFlow = MutableStateFlow(false)
    private val settingsRepository: SettingsRepository = mockk {
        every { isIconLoadingDisabled } returns false
        every { isIconLoadingDisabledFlow } returns mutableIsIconLoadingDisabledFlow
        every { getPullToRefreshEnabledFlow() } returns mutablePullToRefreshEnabledFlow
        every { isUnlockWithPinEnabled } returns false
    }

    private val mockAuthRepository = mockk<AuthRepository>(relaxed = true)
    private val specialCircumstanceManager: SpecialCircumstanceManager =
        SpecialCircumstanceManagerImpl(
            authRepository = mockAuthRepository,
            dispatcherManager = FakeDispatcherManager(),
        )
    private val policyManager: PolicyManager = mockk {
        every { getActivePolicies(type = PolicyTypeJson.DISABLE_SEND) } returns emptyList()
        every { getActivePoliciesFlow(type = PolicyTypeJson.DISABLE_SEND) } returns emptyFlow()
    }
    private val fido2CredentialManager: Fido2CredentialManager = mockk {
        every { isUserVerified } returns false
        every { isUserVerified = any() } just runs
        every { authenticationAttempts } returns 0
        every { authenticationAttempts = any() } just runs
        every { hasAuthenticationAttemptsRemaining() } returns true
        every { getPasskeyAttestationOptionsOrNull(any()) } returns mockk(relaxed = true)
    }
    private val fido2OriginManager: Fido2OriginManager = mockk {
        coEvery { validateOrigin(any(), any()) } returns Fido2ValidateOriginResult.Success(null)
    }

    private val organizationEventManager = mockk<OrganizationEventManager> {
        every { trackEvent(event = any()) } just runs
    }

    private val initialState = createVaultItemListingState()
    private val initialSavedStateHandle = createSavedStateHandleWithVaultItemListingType(
        vaultItemListingType = VaultItemListingType.Login,
    )

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                initialState, awaitItem(),
            )
        }
    }

    @Test
    fun `initial dialog state should be correct when fido2Request is present`() = runTest {
        val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
            "mockUserId",
            "{}",
            "com.x8bit.bitwarden",
            SigningInfo(),
            origin = null,
        )
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
            fido2CreateCredentialRequest = fido2CreateCredentialRequest,
        )

        val viewModel = createVaultItemListingViewModel()

        viewModel.stateFlow.test {
            assertEquals(
                initialState.copy(
                    fido2CreateCredentialRequest = fido2CreateCredentialRequest,
                    dialogState = VaultItemListingState.DialogState.Loading(
                        message = R.string.loading.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on LockAccountClick should call lockVault for the given account`() {
        val accountUserId = "userId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns accountUserId
        }
        val viewModel = createVaultItemListingViewModel()

        viewModel.trySendAction(VaultItemListingsAction.LockAccountClick(accountSummary))

        verify { vaultRepository.lockVault(userId = accountUserId) }
    }

    @Test
    fun `on LogoutAccountClick should call logout for the given account`() {
        val accountUserId = "userId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns accountUserId
        }
        val viewModel = createVaultItemListingViewModel()

        viewModel.trySendAction(VaultItemListingsAction.LogoutAccountClick(accountSummary))

        verify { authRepository.logout(userId = accountUserId) }
    }

    @Test
    fun `on SwitchAccountClick should switch to the given account`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        val updatedUserId = "updatedUserId"
        viewModel.trySendAction(
            VaultItemListingsAction.SwitchAccountClick(
                accountSummary = mockk {
                    every { userId } returns updatedUserId
                },
            ),
        )
        verify { authRepository.switchAccount(userId = updatedUserId) }
    }

    @Test
    fun `BackClick with TotpData should emit ExitApp`() = runTest {
        val totpData = TotpData(
            uri = "otpauth://totp/issuer:accountName?secret=secret",
            issuer = "issuer",
            accountName = "accountName",
            secret = "secret",
            digits = 6,
            period = 30,
            algorithm = TotpData.CryptoHashAlgorithm.SHA_1,
        )
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.AddTotpLoginItem(data = totpData)
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.BackClick)
            assertEquals(VaultItemListingEvent.ExitApp, awaitItem())
        }
    }

    @Test
    fun `BackClick with AutofillSelectionData should emit ExitApp`() = runTest {
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.AutofillSelection(
            autofillSelectionData = AutofillSelectionData(
                framework = AutofillSelectionData.Framework.ACCESSIBILITY,
                type = AutofillSelectionData.Type.LOGIN,
                uri = null,
            ),
            shouldFinishWhenComplete = false,
        )
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.BackClick)
            assertEquals(VaultItemListingEvent.ExitApp, awaitItem())
        }
    }

    @Test
    fun `BackClick should emit NavigateBack`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.BackClick)
            assertEquals(VaultItemListingEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `DismissDialogClick should clear the dialog state`() {
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(VaultItemListingsAction.DismissDialogClick)
        assertEquals(initialState.copy(dialogState = null), viewModel.stateFlow.value)
    }

    @Test
    fun `SearchIconClick should emit NavigateToVaultSearchScreen`() = runTest {
        val searchType = SearchType.Vault.Logins
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.SearchIconClick)
            assertEquals(VaultItemListingEvent.NavigateToSearchScreen(searchType), awaitItem())
        }
    }

    @Test
    fun `LockClick should call lockVaultForCurrentUser`() {
        every { vaultRepository.lockVaultForCurrentUser() } just runs
        val viewModel = createVaultItemListingViewModel()

        viewModel.trySendAction(VaultItemListingsAction.LockClick)

        verify(exactly = 1) {
            vaultRepository.lockVaultForCurrentUser()
        }
    }

    @Test
    fun `SyncClick should display the loading dialog and call sync`() {
        val viewModel = createVaultItemListingViewModel()

        viewModel.trySendAction(VaultItemListingsAction.SyncClick)

        assertEquals(
            initialState.copy(
                dialogState = VaultItemListingState.DialogState.Loading(
                    message = R.string.syncing.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            vaultRepository.sync(forced = true)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ItemClick for vault item when accessibility autofill should post to the accessibilitySelectionManager`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            coEvery {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView),
                )
            } returns DecryptFido2CredentialAutofillViewResult.Error
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.AutofillSelection(
                autofillSelectionData = AutofillSelectionData(
                    type = AutofillSelectionData.Type.LOGIN,
                    framework = AutofillSelectionData.Framework.ACCESSIBILITY,
                    uri = "https://www.test.com",
                ),
                shouldFinishWhenComplete = true,
            )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel()

            accessibilitySelectionManager.accessibilitySelectionFlow.test {
                viewModel.trySendAction(VaultItemListingsAction.ItemClick(id = "mockId-1"))
                assertEquals(cipherView, awaitItem())
            }
            coVerify(exactly = 1) {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView),
                )
            }
        }

    @Test
    fun `ItemClick for vault item when autofill should post to the AutofillSelectionManager`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            coEvery {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView),
                )
            } returns DecryptFido2CredentialAutofillViewResult.Error
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AutofillSelection(
                    autofillSelectionData = AutofillSelectionData(
                        type = AutofillSelectionData.Type.LOGIN,
                        framework = AutofillSelectionData.Framework.AUTOFILL,
                        uri = "https://www.test.com",
                    ),
                    shouldFinishWhenComplete = true,
                )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel()

            autofillSelectionManager.autofillSelectionFlow.test {
                viewModel.trySendAction(VaultItemListingsAction.ItemClick(id = "mockId-1"))
                assertEquals(
                    cipherView,
                    awaitItem(),
                )
            }
            coVerify {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ItemClick for vault item during FIDO 2 registration should show FIDO 2 error dialog when cipherView is null`() {
        val cipherView = createMockCipherView(number = 1)
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
            fido2CreateCredentialRequest = createMockFido2CredentialRequest(number = 1),
        )
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(),
                folderViewList = emptyList(),
                collectionViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(VaultItemListingsAction.ItemClick(cipherView.id.orEmpty()))

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ItemClick for vault item during FIDO 2 registration should show FIDO 2 error dialog when PasskeyCreateOptions is null`() {
        setupMockUri()
        val cipherView = createMockCipherView(number = 1)
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
            fido2CreateCredentialRequest = createMockFido2CredentialRequest(number = 1),
        )
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(cipherView),
                folderViewList = emptyList(),
                collectionViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )
        every { fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any()) } returns null

        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(VaultItemListingsAction.ItemClick(cipherView.id.orEmpty()))

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ItemClick for vault item during FIDO 2 registration should show overwrite passkey confirmation when selected cipher has existing passkey`() {
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = createMockFido2CredentialRequest(number = 1),
            )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns createMockPasskeyAttestationOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.REQUIRED,
            )
            coEvery {
                fido2CredentialManager.registerFido2Credential(
                    any(),
                    any(),
                    any(),
                )
            } returns Fido2RegisterCredentialResult.Success("mockResponse")

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(VaultItemListingsAction.ItemClick(cipherView.id.orEmpty()))

            assertEquals(
                VaultItemListingState.DialogState.OverwritePasskeyConfirmationPrompt(
                    cipherViewId = cipherView.id!!,
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ItemClick for vault item during FIDO 2 registration should show loading dialog, then request user verification when required`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1, fido2Credentials = null)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = createMockFido2CredentialRequest(number = 1),
            )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns createMockPasskeyAttestationOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.REQUIRED,
            )
            coEvery {
                fido2CredentialManager.registerFido2Credential(
                    any(),
                    any(),
                    any(),
                )
            } returns Fido2RegisterCredentialResult.Success("mockResponse")

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(VaultItemListingsAction.ItemClick(cipherView.id.orEmpty()))

            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingState.DialogState.Loading(R.string.saving.asText()),
                    viewModel.stateFlow.value.dialogState,
                )
                assertEquals(
                    VaultItemListingEvent.Fido2UserVerification(
                        isRequired = true,
                        selectedCipherView = cipherView,
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ItemClick for vault item during FIDO 2 registration should skip user verification and perform registration when discouraged`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1)
            val mockFido2CredentialRequest = createMockFido2CredentialRequest(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = mockFido2CredentialRequest,
            )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns createMockPasskeyAttestationOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.DISCOURAGED,
            )
            coEvery {
                fido2CredentialManager.registerFido2Credential(
                    any(),
                    any(),
                    any(),
                )
            } returns Fido2RegisterCredentialResult.Success("mockResponse")

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(VaultItemListingsAction.ItemClick(cipherView.id.orEmpty()))

            coVerify {
                fido2CredentialManager.registerFido2Credential(
                    userId = DEFAULT_USER_STATE.activeUserId,
                    fido2CreateCredentialRequest = mockFido2CredentialRequest,
                    selectedCipherView = cipherView,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ItemClick for vault item during FIDO 2 registration should skip user verification when user is verified`() {
        setupMockUri()
        val cipherView = createMockCipherView(number = 1)
        val mockFido2CredentialRequest = createMockFido2CredentialRequest(number = 1)
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
            fido2CreateCredentialRequest = mockFido2CredentialRequest,
        )
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(cipherView),
                folderViewList = emptyList(),
                collectionViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )
        every {
            fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
        } returns createMockPasskeyAttestationOptions(
            number = 1,
            userVerificationRequirement = UserVerificationRequirement.REQUIRED,
        )
        coEvery {
            fido2CredentialManager.registerFido2Credential(
                any(),
                any(),
                any(),
            )
        } returns Fido2RegisterCredentialResult.Success("mockResponse")
        every { fido2CredentialManager.isUserVerified } returns true

        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(VaultItemListingsAction.ItemClick(cipherView.id.orEmpty()))

        coVerify { fido2CredentialManager.isUserVerified }
        coVerify(exactly = 1) {
            fido2CredentialManager.registerFido2Credential(
                userId = DEFAULT_USER_STATE.activeUserId,
                fido2CreateCredentialRequest = mockFido2CredentialRequest,
                selectedCipherView = cipherView,
            )
        }
    }

    @Test
    fun `ItemClick for vault item should emit NavigateToVaultItem`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.ItemClick(id = "mock"))
            assertEquals(VaultItemListingEvent.NavigateToVaultItem(id = "mock"), awaitItem())
        }
    }

    @Test
    fun `ItemClick for send item should emit NavigateToSendItem`() = runTest {
        val viewModel = createVaultItemListingViewModel(
            createSavedStateHandleWithVaultItemListingType(VaultItemListingType.SendFile),
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.ItemClick(id = "mock"))
            assertEquals(VaultItemListingEvent.NavigateToSendItem(id = "mock"), awaitItem())
        }
    }

    @Test
    fun `MasterPasswordRepromptSubmit for a request Error should show a generic error dialog`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1)
            val cipherId = "mockId-1"
            val password = "password"
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Error
            val initialState = createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createMockDisplayItemForCipher(number = 1)
                            .copy(secondSubtitleTestTag = "PasskeySite"),
                    ),
                    displayFolderList = emptyList(),
                ),
            )
            assertEquals(
                initialState,
                viewModel.stateFlow.value,
            )

            viewModel.trySendAction(
                VaultItemListingsAction.MasterPasswordRepromptSubmit(
                    password = password,
                    masterPasswordRepromptData = MasterPasswordRepromptData.Autofill(
                        cipherId = cipherId,
                    ),
                ),
            )

            assertEquals(
                initialState.copy(
                    dialogState = VaultItemListingState.DialogState.Error(
                        title = null,
                        message = R.string.generic_error_message.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordRepromptSubmit for a request Success with an invalid password should show an invalid password dialog`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1)
            val cipherId = "mockId-1"
            val password = "password"
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val initialState = createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createMockDisplayItemForCipher(number = 1)
                            .copy(secondSubtitleTestTag = "PasskeySite"),
                    ),
                    displayFolderList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = false)

            assertEquals(
                initialState,
                viewModel.stateFlow.value,
            )

            viewModel.trySendAction(
                VaultItemListingsAction.MasterPasswordRepromptSubmit(
                    password = password,
                    masterPasswordRepromptData = MasterPasswordRepromptData.Autofill(
                        cipherId = cipherId,
                    ),
                ),
            )

            assertEquals(
                initialState.copy(
                    dialogState = VaultItemListingState.DialogState.Error(
                        title = null,
                        message = R.string.invalid_master_password.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordRepromptSubmit for a request Success with a valid password for autofill should post to the AutofillSelectionManager`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1)
            val cipherId = "mockId-1"
            val password = "password"
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AutofillSelection(
                    autofillSelectionData = AutofillSelectionData(
                        type = AutofillSelectionData.Type.LOGIN,
                        framework = AutofillSelectionData.Framework.AUTOFILL,
                        uri = "https://www.test.com",
                    ),
                    shouldFinishWhenComplete = true,
                )
            val viewModel = createVaultItemListingViewModel()
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)

            autofillSelectionManager.autofillSelectionFlow.test {
                viewModel.trySendAction(
                    VaultItemListingsAction.MasterPasswordRepromptSubmit(
                        password = password,
                        masterPasswordRepromptData = MasterPasswordRepromptData.Autofill(
                            cipherId = cipherId,
                        ),
                    ),
                )
                assertEquals(
                    cipherView,
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordRepromptSubmit for a request Success with a valid password for accessibility autofill should post to the AccessibilitySelectionManager`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1)
            val cipherId = "mockId-1"
            val password = "password"
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AutofillSelection(
                    autofillSelectionData = AutofillSelectionData(
                        type = AutofillSelectionData.Type.LOGIN,
                        framework = AutofillSelectionData.Framework.ACCESSIBILITY,
                        uri = "https://www.test.com",
                    ),
                    shouldFinishWhenComplete = true,
                )
            val viewModel = createVaultItemListingViewModel()
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)

            accessibilitySelectionManager.accessibilitySelectionFlow.test {
                viewModel.trySendAction(
                    VaultItemListingsAction.MasterPasswordRepromptSubmit(
                        password = password,
                        masterPasswordRepromptData = MasterPasswordRepromptData.Autofill(
                            cipherId = cipherId,
                        ),
                    ),
                )
                assertEquals(cipherView, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordRepromptSubmit for a request Success with a valid password for overflow actions should process the action`() =
        runTest {
            val cipherId = "cipherId-1234"
            val password = "password"
            val viewModel = createVaultItemListingViewModel()
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)

            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    VaultItemListingsAction.MasterPasswordRepromptSubmit(
                        password = password,
                        masterPasswordRepromptData = MasterPasswordRepromptData.OverflowItem(
                            action = ListingItemOverflowAction.VaultAction.EditClick(
                                cipherId = cipherId,
                                requiresPasswordReprompt = true,
                            ),
                        ),
                    ),
                )
                // An Edit action navigates to the Edit screen
                assertEquals(VaultItemListingEvent.NavigateToEditCipher(cipherId), awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordRepromptSubmit with a valid password for totp flow should emit NavigateToEditCipher`() =
        runTest {
            val cipherId = "cipherId-1234"
            val password = "password"
            val viewModel = createVaultItemListingViewModel()
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)

            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    VaultItemListingsAction.MasterPasswordRepromptSubmit(
                        password = password,
                        masterPasswordRepromptData = MasterPasswordRepromptData.Totp(
                            cipherId = cipherId,
                        ),
                    ),
                )
                // An Edit action navigates to the Edit screen
                assertEquals(VaultItemListingEvent.NavigateToEditCipher(cipherId), awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `AddVaultItemClick inside a folder should emit NavigateToAddVaultItem with a selected folder id`() =
        runTest {
            val viewModel = createVaultItemListingViewModel(
                savedStateHandle = createSavedStateHandleWithVaultItemListingType(
                    vaultItemListingType = VaultItemListingType.Folder(folderId = "id"),
                ),
            )
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick)
                assertEquals(
                    VaultItemListingEvent.NavigateToAddVaultItem(
                        VaultItemCipherType.LOGIN,
                        selectedFolderId = "id",
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `AddVaultItemClick for vault item should emit NavigateToAddVaultItem`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick)
            assertEquals(
                VaultItemListingEvent.NavigateToAddVaultItem(VaultItemCipherType.LOGIN),
                awaitItem(),
            )
        }
    }

    @Test
    fun `AddVaultItemClick for send item should emit NavigateToAddVaultItem`() = runTest {
        val viewModel = createVaultItemListingViewModel(
            createSavedStateHandleWithVaultItemListingType(VaultItemListingType.SendText),
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick)
            assertEquals(VaultItemListingEvent.NavigateToAddSendItem, awaitItem())
        }
    }

    @Test
    fun `FolderClick for vault item should emit NavigateToFolderItem`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        val testId = "1"

        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.FolderClick(testId))
            assertEquals(VaultItemListingEvent.NavigateToFolderItem(testId), awaitItem())
        }
    }

    @Test
    fun `CollectionClick for vault item should emit NavigateToCollectionItem`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        val testId = "1"

        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemListingsAction.CollectionClick(testId))
            assertEquals(VaultItemListingEvent.NavigateToCollectionItem(testId), awaitItem())
        }
    }

    @Test
    fun `RefreshClick should sync`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(VaultItemListingsAction.RefreshClick)
        verify { vaultRepository.sync(forced = true) }
    }

    @Test
    fun `OverflowOptionClick Send EditClick should emit NavigateToSendItem`() = runTest {
        val sendId = "sendId"
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.SendAction.EditClick(sendId = sendId),
                ),
            )
            assertEquals(VaultItemListingEvent.NavigateToSendItem(sendId), awaitItem())
        }
    }

    @Test
    fun `OverflowOptionClick Send CopyUrlClick should call setText on clipboardManager`() {
        val sendUrl = "www.test.com"
        every { clipboardManager.setText(sendUrl) } just runs
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(
            VaultItemListingsAction.OverflowOptionClick(
                ListingItemOverflowAction.SendAction.CopyUrlClick(sendUrl = sendUrl),
            ),
        )
        verify(exactly = 1) {
            clipboardManager.setText(text = sendUrl)
        }
    }

    @Test
    fun `OverflowOptionClick Send DeleteClick with deleteSend error should display error dialog`() =
        runTest {
            val sendId = "sendId1234"
            coEvery { vaultRepository.deleteSend(sendId) } returns DeleteSendResult.Error

            val viewModel = createVaultItemListingViewModel()
            viewModel.stateFlow.test {
                assertEquals(initialState, awaitItem())
                viewModel.trySendAction(
                    VaultItemListingsAction.OverflowOptionClick(
                        ListingItemOverflowAction.SendAction.DeleteClick(sendId = sendId),
                    ),
                )
                assertEquals(
                    initialState.copy(
                        dialogState = VaultItemListingState.DialogState.Loading(
                            message = R.string.deleting.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    initialState.copy(
                        dialogState = VaultItemListingState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `OverflowOptionClick Send DeleteClick with deleteSend success should emit ShowToast`() =
        runTest {
            val sendId = "sendId1234"
            coEvery { vaultRepository.deleteSend(sendId) } returns DeleteSendResult.Success

            val viewModel = createVaultItemListingViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    VaultItemListingsAction.OverflowOptionClick(
                        ListingItemOverflowAction.SendAction.DeleteClick(sendId = sendId),
                    ),
                )
                assertEquals(
                    VaultItemListingEvent.ShowToast(R.string.send_deleted.asText()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `OverflowOptionClick Send ShareUrlClick should emit ShowShareSheet`() = runTest {
        val sendUrl = "www.test.com"
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.SendAction.ShareUrlClick(sendUrl = sendUrl),
                ),
            )
            assertEquals(VaultItemListingEvent.ShowShareSheet(sendUrl), awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Send RemovePasswordClick with removePasswordSend error should display error dialog`() =
        runTest {
            val sendId = "sendId1234"
            coEvery {
                vaultRepository.removePasswordSend(sendId)
            } returns RemovePasswordSendResult.Error(errorMessage = null)

            val viewModel = createVaultItemListingViewModel()
            viewModel.stateFlow.test {
                assertEquals(initialState, awaitItem())
                viewModel.trySendAction(
                    VaultItemListingsAction.OverflowOptionClick(
                        ListingItemOverflowAction.SendAction.RemovePasswordClick(sendId = sendId),
                    ),
                )
                assertEquals(
                    initialState.copy(
                        dialogState = VaultItemListingState.DialogState.Loading(
                            message = R.string.removing_send_password.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    initialState.copy(
                        dialogState = VaultItemListingState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Send RemovePasswordClick with removePasswordSend success should emit ShowToast`() =
        runTest {
            val sendId = "sendId1234"
            coEvery {
                vaultRepository.removePasswordSend(sendId)
            } returns RemovePasswordSendResult.Success(mockk())

            val viewModel = createVaultItemListingViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    VaultItemListingsAction.OverflowOptionClick(
                        ListingItemOverflowAction.SendAction.RemovePasswordClick(sendId = sendId),
                    ),
                )
                assertEquals(
                    VaultItemListingEvent.ShowToast(R.string.send_password_removed.asText()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `OverflowOptionClick Vault CopyNoteClick should call setText on the ClipboardManager`() =
        runTest {
            val notes = "notes"
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyNoteClick(notes = notes),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(notes)
            }
        }

    @Test
    fun `OverflowOptionClick Vault CopyNumberClick should call setText on the ClipboardManager`() =
        runTest {
            val number = "12345-4321-9876-6789"
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyNumberClick(
                        number = number,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(number)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyPasswordClick should call setText on the ClipboardManager`() =
        runTest {
            val password = "passTheWord"
            val cipherId = "cipherId"
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                        password = password,
                        requiresPasswordReprompt = true,
                        cipherId = cipherId,
                    ),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(password)
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientCopiedPassword(cipherId = cipherId),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopySecurityCodeClick should call setText on the ClipboardManager`() =
        runTest {
            val securityCode = "234"
            val cipherId = "cipherId"
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopySecurityCodeClick(
                        securityCode = securityCode,
                        cipherId = cipherId,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(securityCode)
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientCopiedCardCode(cipherId = cipherId),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyTotpClick with GenerateTotpCode success should call setText on the ClipboardManager`() =
        runTest {
            val totpCode = "totpCode"
            val code = "Code"

            coEvery {
                vaultRepository.generateTotp(totpCode, clock.instant())
            } returns GenerateTotpResult.Success(code, 30)

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyTotpClick(totpCode),
                ),
            )

            verify(exactly = 1) {
                clipboardManager.setText(code)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyTotpClick with GenerateTotpCode failure should not call setText on the ClipboardManager`() =
        runTest {
            val totpCode = "totpCode"

            coEvery {
                vaultRepository.generateTotp(totpCode, clock.instant())
            } returns GenerateTotpResult.Error

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyTotpClick(totpCode),
                ),
            )

            verify(exactly = 0) {
                clipboardManager.setText(text = any<String>())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyUsernameClick should call setText on the ClipboardManager`() =
        runTest {
            val username = "bitwarden"
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyUsernameClick(
                        username = username,
                    ),
                ),
            )
            verify(exactly = 1) {
                clipboardManager.setText(username)
            }
        }

    @Test
    fun `OverflowOptionClick Vault EditClick should emit NavigateToEditCipher`() = runTest {
        val cipherId = "cipherId-1234"
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = cipherId,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            assertEquals(VaultItemListingEvent.NavigateToEditCipher(cipherId), awaitItem())
        }
    }

    @Test
    fun `OverflowOptionClick Vault LaunchClick should emit NavigateToUrl`() = runTest {
        val url = "www.test.com"
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.LaunchClick(url = url),
                ),
            )
            assertEquals(VaultItemListingEvent.NavigateToUrl(url), awaitItem())
        }
    }

    @Test
    fun `OverflowOptionClick Vault ViewClick should emit NavigateToUrl`() = runTest {
        val cipherId = "cipherId-9876"
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = cipherId),
                ),
            )
            assertEquals(VaultItemListingEvent.NavigateToVaultItem(cipherId), awaitItem())
        }
    }

    @Test
    fun `vaultDataStateFlow Loaded with items should update ViewState to Content`() = runTest {
        setupMockUri()

        val dataState = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = false)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createMockDisplayItemForCipher(number = 1)
                            .copy(secondSubtitleTestTag = "PasskeySite"),
                    ),
                    displayFolderList = emptyList(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with items and autofill filtering should update ViewState to Content with filtered data`() =
        runTest {
            setupMockUri()

            val cipherView1 = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            val cipherView2 = createMockCipherView(
                number = 2,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )

            coEvery {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView1, cipherView2),
                )
            } returns DecryptFido2CredentialAutofillViewResult.Success(emptyList())

            // Set up the data to be filtered
            mockFilteredCiphers = listOf(cipherView1)

            val autofillSelectionData = AutofillSelectionData(
                type = AutofillSelectionData.Type.LOGIN,
                framework = AutofillSelectionData.Framework.AUTOFILL,
                uri = "https://www.test.com",
            )
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AutofillSelection(
                    autofillSelectionData = autofillSelectionData,
                    shouldFinishWhenComplete = true,
                )
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView1, cipherView2),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()

            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                createVaultItemListingState(
                    viewState = VaultItemListingState.ViewState.Content(
                        displayCollectionList = emptyList(),
                        displayItemList = listOf(
                            createMockDisplayItemForCipher(number = 1).copy(
                                secondSubtitleTestTag = "PasskeySite",
                                subtitleTestTag = "PasskeyName",
                                iconData = IconData.Network(
                                    uri = "https://vault.bitwarden.com/icons/www.mockuri.com/icon.png",
                                    fallbackIconRes = R.drawable.ic_bw_passkey,
                                ),
                                isAutofill = true,
                            ),
                        ),
                        displayFolderList = emptyList(),
                    ),
                )
                    .copy(autofillSelectionData = autofillSelectionData),
                viewModel.stateFlow.value,
            )
            coVerify {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView1, cipherView2),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with items and totp data filtering should update ViewState to Content with filtered data`() =
        runTest {
            setupMockUri()

            val uri = "otpauth://totp/issuer:accountName?secret=secret"
            val totpData = TotpData(
                uri = uri,
                issuer = null,
                accountName = "Name-1",
                secret = "secret",
                digits = 6,
                period = 30,
                algorithm = TotpData.CryptoHashAlgorithm.SHA_1,
            )
            val cipherView1 = createMockCipherView(number = 1)
            val cipherView2 = createMockCipherView(number = 2)

            // Filtering comes later, so we return everything here
            mockFilteredCiphers = listOf(cipherView1, cipherView2)

            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AddTotpLoginItem(data = totpData)
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView1, cipherView2),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()

            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                createVaultItemListingState(
                    viewState = VaultItemListingState.ViewState.Content(
                        displayCollectionList = emptyList(),
                        displayItemList = listOf(
                            createMockDisplayItemForCipher(
                                number = 1,
                                secondSubtitleTestTag = "PasskeySite",
                                isTotp = true,
                            ),
                        ),
                        displayFolderList = emptyList(),
                    ),
                )
                    .copy(totpData = totpData),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with items and fido2 filtering should update ViewState to Content with filtered data`() =
        runTest {
            setupMockUri()

            val cipherView1 = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            val cipherView2 = createMockCipherView(
                number = 2,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )

            coEvery {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView1, cipherView2),
                )
            } returns DecryptFido2CredentialAutofillViewResult.Success(emptyList())
            coEvery {
                fido2OriginManager.validateOrigin(any(), any())
            } returns Fido2ValidateOriginResult.Success("")

            mockFilteredCiphers = listOf(cipherView1)

            val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
                userId = "activeUserId",
                requestJson = "{}",
                packageName = "com.x8bit.bitwarden",
                signingInfo = SigningInfo(),
                origin = "mockOrigin",
            )

            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2Save(
                    fido2CreateCredentialRequest = fido2CreateCredentialRequest,
                )
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView1, cipherView2),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()

            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                createVaultItemListingState(
                    viewState = VaultItemListingState.ViewState.Content(
                        displayCollectionList = emptyList(),
                        displayItemList = listOf(
                            createMockDisplayItemForCipher(number = 1)
                                .copy(
                                    secondSubtitleTestTag = "PasskeySite",
                                    subtitleTestTag = "PasskeyName",
                                    iconData = IconData.Network(
                                        uri = "https://vault.bitwarden.com/icons/www.mockuri.com/icon.png",
                                        fallbackIconRes = R.drawable.ic_bw_passkey,
                                    ),
                                    isFido2Creation = true,
                                ),
                        ),
                        displayFolderList = emptyList(),
                    ),
                )
                    .copy(fido2CreateCredentialRequest = fido2CreateCredentialRequest),
                viewModel.stateFlow.value,
            )
            coVerify {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView1, cipherView2),
                )
                fido2OriginManager.validateOrigin(any(), any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with Fido2GetCredentials special circumstance should update ViewState to Content with filtered data`() =
        runTest {
            setupMockUri()

            val cipherView1 = createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
            val cipherView2 = createMockCipherView(
                number = 2,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )

            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(any())
            } returns createMockPasskeyAssertionOptions(
                number = 1,
            )
            coEvery {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView1, cipherView2),
                )
            } returns DecryptFido2CredentialAutofillViewResult.Success(emptyList())

            mockFilteredCiphers = listOf(cipherView1)

            val fido2GetCredentialRequest = Fido2GetCredentialsRequest(
                requestJson = "{}",
                packageName = "com.x8bit.bitwarden",
                signingInfo = SigningInfo(),
                origin = "mockOrigin",
                candidateQueryData = mockk(),
                clientDataHash = byteArrayOf(0),
                id = "mockId",
                userId = "mockUserId",
            )

            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.Fido2GetCredentials(
                    fido2GetCredentialsRequest = fido2GetCredentialRequest,
                )
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView1, cipherView2),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createVaultItemListingViewModel()

            mutableVaultDataStateFlow.value = dataState

            assertEquals(
                createVaultItemListingState(
                    viewState = VaultItemListingState.ViewState.Content(
                        displayCollectionList = emptyList(),
                        displayItemList = listOf(
                            createMockDisplayItemForCipher(number = 1)
                                .copy(
                                    secondSubtitleTestTag = "PasskeySite",
                                ),
                        ),
                        displayFolderList = emptyList(),
                    ),
                )
                    .copy(fido2GetCredentialsRequest = fido2GetCredentialRequest),
                viewModel.stateFlow.value,
            )
            coVerify {
                vaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(cipherView1, cipherView2),
                )
            }
        }

    @Test
    fun `vaultDataStateFlow Loaded with empty items should update ViewState to NoItems`() =
        runTest {
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = emptyList(),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel()

            mutableVaultDataStateFlow.tryEmit(value = dataState)

            assertEquals(
                createVaultItemListingState(
                    viewState = VaultItemListingState.ViewState.NoItems(
                        header = R.string.save_and_protect_your_data.asText(),
                        message = R.string.no_items.asText(),
                        shouldShowAddButton = true,
                        buttonText = R.string.add_an_item.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `vaultDataStateFlow Loaded with trash items should update ViewState to NoItems`() =
        runTest {
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            val viewModel = createVaultItemListingViewModel()

            mutableVaultDataStateFlow.tryEmit(value = dataState)

            assertEquals(
                createVaultItemListingState(
                    viewState = VaultItemListingState.ViewState.NoItems(
                        header = R.string.save_and_protect_your_data.asText(),
                        message = R.string.no_items.asText(),
                        shouldShowAddButton = true,
                        buttonText = R.string.add_an_item.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `vaultDataStateFlow Loading should update state to Loading`() = runTest {
        mutableVaultDataStateFlow.tryEmit(value = DataState.Loading)

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(viewState = VaultItemListingState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Pending with data should update state to Content`() = runTest {
        setupMockUri()

        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Pending(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = false)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createMockDisplayItemForCipher(number = 1)
                            .copy(secondSubtitleTestTag = "PasskeySite"),
                    ),
                    displayFolderList = emptyList(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Pending with empty data should update state to NoItems`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Pending(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = R.string.save_and_protect_your_data.asText(),
                    message = R.string.no_items.asText(),
                    shouldShowAddButton = true,
                    buttonText = R.string.add_an_item.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Pending with trash data should update state to NoItems`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Pending(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = R.string.save_and_protect_your_data.asText(),
                    message = R.string.no_items.asText(),
                    shouldShowAddButton = true,
                    buttonText = R.string.add_an_item.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error without data should update state to Error`() = runTest {
        val dataState = DataState.Error<VaultData>(
            error = IllegalStateException(),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Error(
                    message = R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error with data should update state to Content`() = runTest {
        setupMockUri()

        val dataState = DataState.Error(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = false)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
            error = IllegalStateException(),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createMockDisplayItemForCipher(number = 1)
                            .copy(secondSubtitleTestTag = "PasskeySite"),
                    ),
                    displayFolderList = emptyList(),
                ),
            ),
            viewModel.stateFlow.value,
        )

        unmockkStatic(Uri::class)
    }

    @Test
    fun `vaultDataStateFlow Error with empty data should update state to NoItems`() = runTest {
        val dataState = DataState.Error(
            data = VaultData(
                cipherViewList = emptyList(),
                folderViewList = emptyList(),
                collectionViewList = emptyList(),
                sendViewList = emptyList(),
            ),
            error = IllegalStateException(),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = R.string.save_and_protect_your_data.asText(),
                    message = R.string.no_items.asText(),
                    shouldShowAddButton = true,
                    buttonText = R.string.add_an_item.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error with trash data should update state to NoItems`() = runTest {
        val dataState = DataState.Error(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
            error = IllegalStateException(),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = R.string.save_and_protect_your_data.asText(),
                    message = R.string.no_items.asText(),
                    shouldShowAddButton = true,
                    buttonText = R.string.add_an_item.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow NoNetwork without data should update state to Error`() = runTest {
        val dataState = DataState.NoNetwork<VaultData>()

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Error(
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

    @Test
    fun `vaultDataStateFlow NoNetwork with data should update state to Content`() = runTest {
        setupMockUri()

        val dataState = DataState.NoNetwork(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = false)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createMockDisplayItemForCipher(number = 1)
                            .copy(secondSubtitleTestTag = "PasskeySite"),
                    ),
                    displayFolderList = emptyList(),
                ),
            ),
            viewModel.stateFlow.value,
        )

        unmockkStatic(Uri::class)
    }

    @Test
    fun `vaultDataStateFlow NoNetwork with empty data should update state to NoItems`() = runTest {
        val dataState = DataState.NoNetwork(
            data = VaultData(
                cipherViewList = emptyList(),
                folderViewList = emptyList(),
                collectionViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = R.string.save_and_protect_your_data.asText(),
                    message = R.string.no_items.asText(),
                    shouldShowAddButton = true,
                    buttonText = R.string.add_an_item.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow NoNetwork with trash data should update state to NoItems`() = runTest {
        val dataState = DataState.NoNetwork(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = R.string.save_and_protect_your_data.asText(),
                    message = R.string.no_items.asText(),
                    shouldShowAddButton = true,
                    buttonText = R.string.add_an_item.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow updates should do nothing when switching accounts`() {
        val viewModel = createVaultItemListingViewModel()
        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        // Log out the accounts
        mutableUserStateFlow.value = null

        // Emit fresh data
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )

        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `icon loading state updates should update isIconLoadingDisabled`() = runTest {
        val viewModel = createVaultItemListingViewModel()

        assertFalse(viewModel.stateFlow.value.isIconLoadingDisabled)

        mutableIsIconLoadingDisabledFlow.value = true
        assertTrue(viewModel.stateFlow.value.isIconLoadingDisabled)
    }

    @Test
    fun `RefreshPull should call vault repository sync`() {
        val viewModel = createVaultItemListingViewModel()

        viewModel.trySendAction(VaultItemListingsAction.RefreshPull)

        verify(exactly = 1) {
            vaultRepository.sync(forced = false)
        }
    }

    @Test
    fun `PullToRefreshEnableReceive should update isPullToRefreshEnabled`() = runTest {
        val viewModel = createVaultItemListingViewModel()

        viewModel.trySendAction(
            VaultItemListingsAction.Internal.PullToRefreshEnableReceive(
                isPullToRefreshEnabled = true,
            ),
        )

        assertEquals(
            initialState.copy(isPullToRefreshSettingEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `Fido2Request should be evaluated before observing vault data`() {
        val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
            "mockUserId",
            "{}",
            "com.x8bit.bitwarden",
            SigningInfo(),
            origin = "com.x8bit.bitwarden",
        )
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
            fido2CreateCredentialRequest,
        )

        createVaultItemListingViewModel()

        coVerify(ordering = Ordering.ORDERED) {
            fido2OriginManager.validateOrigin(any(), any())
            vaultRepository.vaultDataStateFlow
        }
    }

    @Test
    fun `Fido2ValidateOriginResult should update dialog state on Unknown error`() = runTest {
        val mockCallingAppInfo = mockk<CallingAppInfo>(relaxed = true)
        val mock = mockk<Fido2CreateCredentialRequest> {
            every { callingAppInfo } returns mockCallingAppInfo
            every { requestJson } returns "{}"
        }

        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
            fido2CreateCredentialRequest = mock,
        )

        coEvery {
            fido2OriginManager.validateOrigin(any(), any())
        } returns Fido2ValidateOriginResult.Error.Unknown

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                R.string.an_error_has_occurred.asText(),
                R.string.generic_error_message.asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2ValidateOriginResult should update dialog state on PrivilegedAppNotAllowed error`() =
        runTest {
            val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
                userId = "mockUserId",
                requestJson = "{}",
                packageName = "com.x8bit.bitwarden",
                signingInfo = SigningInfo(),
                origin = null,
            )

            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = fido2CreateCredentialRequest,
            )

            coEvery {
                fido2OriginManager.validateOrigin(any(), any())
            } returns Fido2ValidateOriginResult.Error.PrivilegedAppNotAllowed

            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    R.string.an_error_has_occurred.asText(),
                    R.string.passkey_operation_failed_because_browser_is_not_privileged.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2ValidateOriginResult should update dialog state on PrivilegedAppSignatureNotFound error`() =
        runTest {
            val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
                userId = "mockUserId",
                requestJson = "{}",
                packageName = "com.x8bit.bitwarden",
                signingInfo = SigningInfo(),
                origin = null,
            )

            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = fido2CreateCredentialRequest,
            )

            coEvery {
                fido2OriginManager.validateOrigin(any(), any())
            } returns Fido2ValidateOriginResult.Error.PrivilegedAppSignatureNotFound

            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    R.string.an_error_has_occurred.asText(),
                    R.string.passkey_operation_failed_because_browser_signature_does_not_match.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2ValidateOriginResult should update dialog state on PasskeyNotSupportedForApp error`() =
        runTest {
            val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
                userId = "mockUserId",
                requestJson = "{}",
                packageName = "com.x8bit.bitwarden",
                signingInfo = SigningInfo(),
                origin = null,
            )

            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = fido2CreateCredentialRequest,
            )

            coEvery {
                fido2OriginManager.validateOrigin(any(), any())
            } returns Fido2ValidateOriginResult.Error.PasskeyNotSupportedForApp

            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    R.string.an_error_has_occurred.asText(),
                    R.string.passkeys_not_supported_for_this_app.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2ValidateOriginResult should update dialog state on ApplicationNotFound error`() =
        runTest {
            val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
                userId = "mockUserId",
                requestJson = "{}",
                packageName = "com.x8bit.bitwarden",
                signingInfo = SigningInfo(),
                origin = null,
            )

            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = fido2CreateCredentialRequest,
            )

            coEvery {
                fido2OriginManager.validateOrigin(any(), any())
            } returns Fido2ValidateOriginResult.Error.ApplicationNotFound

            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    R.string.an_error_has_occurred.asText(),
                    R.string.passkey_operation_failed_because_app_not_found_in_asset_links.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2ValidateOriginResult should update dialog state on AssetLinkNotFound error`() =
        runTest {
            val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
                userId = "mockUserId",
                requestJson = "{}",
                packageName = "com.x8bit.bitwarden",
                signingInfo = SigningInfo(),
                origin = null,
            )

            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = fido2CreateCredentialRequest,
            )

            coEvery {
                fido2OriginManager.validateOrigin(any(), any())
            } returns Fido2ValidateOriginResult.Error.AssetLinkNotFound

            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    R.string.an_error_has_occurred.asText(),
                    R.string.passkey_operation_failed_because_of_missing_asset_links.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2ValidateOriginResult should update dialog state on ApplicationNotVerified error`() =
        runTest {
            val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
                userId = "mockUserId",
                requestJson = "{}",
                packageName = "com.x8bit.bitwarden",
                signingInfo = SigningInfo(),
                origin = null,
            )

            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = fido2CreateCredentialRequest,
            )

            coEvery {
                fido2OriginManager.validateOrigin(any(), any())
            } returns Fido2ValidateOriginResult.Error.ApplicationFingerprintNotVerified

            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    R.string.an_error_has_occurred.asText(),
                    R.string.passkey_operation_failed_because_app_could_not_be_verified.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2RegisterCredentialResult Error should show toast and emit CompleteFido2Registration result`() =
        runTest {
            val mockResult = Fido2RegisterCredentialResult.Error

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.Internal.Fido2RegisterCredentialResultReceive(
                    mockResult,
                ),
            )

            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.ShowToast(R.string.an_error_has_occurred.asText()),
                    awaitItem(),
                )

                assertEquals(
                    VaultItemListingEvent.CompleteFido2Registration(mockResult),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2RegisterCredentialResult Success should show toast and emit CompleteFido2Registration result`() =
        runTest {
            val mockResult = Fido2RegisterCredentialResult.Success(
                registrationResponse = "mockResponse",
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.Internal.Fido2RegisterCredentialResultReceive(
                    mockResult,
                ),
            )

            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.ShowToast(R.string.item_updated.asText()),
                    awaitItem(),
                )

                assertEquals(
                    VaultItemListingEvent.CompleteFido2Registration(mockResult),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2RegisterCredentialResult Cancelled should emit CompleteFido2Registration result`() =
        runTest {
            val mockResult = Fido2RegisterCredentialResult.Cancelled
            val viewModel = createVaultItemListingViewModel()

            viewModel.trySendAction(
                VaultItemListingsAction.Internal.Fido2RegisterCredentialResultReceive(
                    mockResult,
                ),
            )

            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.CompleteFido2Registration(mockResult),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissFido2ErrorDialogClick should clear the dialog state then complete FIDO 2 registration based on state`() =
        runTest {
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                createMockFido2CredentialRequest(number = 1),
            )
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(VaultItemListingsAction.DismissFido2ErrorDialogClick)
            viewModel.eventFlow.test {
                assertNull(viewModel.stateFlow.value.dialogState)
                assertEquals(
                    VaultItemListingEvent.CompleteFido2Registration(
                        result = Fido2RegisterCredentialResult.Error,
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissFido2ErrorDialogClick should clear dialog state then complete FIDO 2 assertion with error when assertion request is not null`() =
        runTest {
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                createMockFido2CredentialAssertionRequest(number = 1),
            )
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(
                createMockCipherView(
                    number = 1,
                    fido2Credentials = mockFido2CredentialList,
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(VaultItemListingsAction.DismissFido2ErrorDialogClick)
            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.CompleteFido2Assertion(
                        result = Fido2CredentialAssertionResult.Error,
                    ),
                    awaitItem(),
                )
                assertNull(viewModel.stateFlow.value.dialogState)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DismissFido2ErrorDialogClick should show general error dialog when no FIDO 2 request is present`() =
        runTest {
            specialCircumstanceManager.specialCircumstance = null
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(VaultItemListingsAction.DismissFido2ErrorDialogClick)
            assertEquals(
                VaultItemListingState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.generic_error_message.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2AssertionRequest should display loading dialog then request user verification when user is not verified and verification is REQUIRED`() =
        runTest {
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = "mockId-1")
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialList,
            )
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                    mockAssertionRequest.requestJson,
                )
            } returns createMockPasskeyAssertionOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.REQUIRED,
            )
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(
                createMockCipherView(
                    number = 1,
                    fido2Credentials = mockFido2CredentialList,
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingState.DialogState.Loading(R.string.loading.asText()),
                    viewModel.stateFlow.value.dialogState,
                )
                verify { fido2CredentialManager.isUserVerified }
                assertEquals(
                    VaultItemListingEvent.Fido2UserVerification(
                        isRequired = true,
                        selectedCipherView = mockCipherView,
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2AssertionRequest should display loading dialog then request user verification when user is not verified and verification is PREFERED`() =
        runTest {
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = "mockId-1")
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialList,
            )
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                    mockAssertionRequest.requestJson,
                )
            } returns createMockPasskeyAssertionOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.PREFERRED,
            )
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(
                createMockCipherView(
                    number = 1,
                    fido2Credentials = mockFido2CredentialList,
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingState.DialogState.Loading(R.string.loading.asText()),
                    viewModel.stateFlow.value.dialogState,
                )
                verify { fido2CredentialManager.isUserVerified }
                assertEquals(
                    VaultItemListingEvent.Fido2UserVerification(
                        isRequired = false,
                        selectedCipherView = mockCipherView,
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2AssertionRequest should skip user verification when user is not verified and verification is DISCOURAGED`() =
        runTest {
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = "mockId-1")
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialList,
            )
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every { authRepository.activeUserId } returns "activeUserId"
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                    mockAssertionRequest.requestJson,
                )
            } returns createMockPasskeyAssertionOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.DISCOURAGED,
            )
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(
                createMockCipherView(
                    number = 1,
                    fido2Credentials = mockFido2CredentialList,
                ),
            )
            coEvery {
                fido2CredentialManager.authenticateFido2Credential(
                    userId = "activeUserId",
                    request = mockAssertionRequest,
                    selectedCipherView = mockCipherView,
                )
            } returns Fido2CredentialAssertionResult.Success(responseJson = "responseJson")

            createVaultItemListingViewModel()

            coVerify {
                fido2CredentialManager.isUserVerified
                fido2CredentialManager.authenticateFido2Credential(
                    userId = "activeUserId",
                    request = mockAssertionRequest,
                    selectedCipherView = mockCipherView,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2AssertionRequest should show error dialog when assertion options are null`() =
        runTest {
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = "mockId-1")
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(
                createMockCipherView(
                    number = 1,
                    fido2Credentials = mockFido2CredentialList,
                ),
            )
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                    mockAssertionRequest.requestJson,
                )
            } returns null

            val viewModel = createVaultItemListingViewModel()
            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
            verify(exactly = 0) { fido2CredentialManager.isUserVerified }
        }

    @Test
    fun `Fido2AssertionRequest should show error dialog when relyingPartyId is null`() = runTest {
        val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
            .copy(cipherId = "mockId-1")
        val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
            mockAssertionRequest,
        )
        every {
            vaultRepository
                .ciphersStateFlow
                .value
                .data
        } returns listOf(
            createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialList,
            ),
        )
        every {
            fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                mockAssertionRequest.requestJson,
            )
        } returns createMockPasskeyAssertionOptions(
            number = 1,
            userVerificationRequirement = UserVerificationRequirement.DISCOURAGED,
            relyingPartyId = null,
        )

        val viewModel = createVaultItemListingViewModel()
        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        verify(exactly = 0) { fido2CredentialManager.isUserVerified }
    }

    @Test
    fun `Fido2AssertionRequest should show error dialog when validateOrigin is not Success`() =
        runTest {
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = "mockId-1")
            val mockAssertionOptions = createMockPasskeyAssertionOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.DISCOURAGED,
            )
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(
                createMockCipherView(
                    number = 1,
                    fido2Credentials = mockFido2CredentialList,
                ),
            )
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                    requestJson = mockAssertionRequest.requestJson,
                )
            } returns mockAssertionOptions
            coEvery {
                fido2OriginManager.validateOrigin(any(), any())
            } returns Fido2ValidateOriginResult.Error.Unknown

            val viewModel = createVaultItemListingViewModel()

            viewModel.stateFlow.test {
                assertEquals(
                    VaultItemListingState.DialogState.Fido2OperationFail(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.generic_error_message.asText(),
                    ),
                    awaitItem().dialogState,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2AssertionRequest should observe vault data when request does not contain a cipherId`() =
        runTest {
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = null)
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(
                createMockCipherView(
                    number = 1,
                    fido2Credentials = mockFido2CredentialList,
                ),
            )

            createVaultItemListingViewModel()

            verify { vaultRepository.vaultDataStateFlow }
            verify(exactly = 0) {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                    requestJson = mockAssertionRequest.requestJson,
                )
            }
        }

    @Test
    fun `Fido2AssertionRequest should show error dialog when cipher state flow data is null`() =
        runTest {
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns null

            val viewModel = createVaultItemListingViewModel()
            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2AssertionRequest should show error dialog when cipher state flow data has no matching cipher`() =
        runTest {
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(
                createMockCipherView(
                    number = 1,
                    fido2Credentials = mockFido2CredentialList,
                ),
            )

            val viewModel = createVaultItemListingViewModel()
            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Test
    fun `Fido2AssertionRequest should skip user verification when user is verified`() = runTest {
        val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
            .copy(cipherId = "mockId-1")
        val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
        val mockCipherView = createMockCipherView(
            number = 1,
            fido2Credentials = mockFido2CredentialList,
        )
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
            mockAssertionRequest,
        )
        every { fido2CredentialManager.isUserVerified } returns true
        every {
            fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                mockAssertionRequest.requestJson,
            )
        } returns createMockPasskeyAssertionOptions(
            number = 1,
            userVerificationRequirement = UserVerificationRequirement.PREFERRED,
        )
        coEvery {
            fido2CredentialManager.authenticateFido2Credential(
                userId = "activeUserId",
                request = mockAssertionRequest,
                selectedCipherView = mockCipherView,
            )
        } returns Fido2CredentialAssertionResult.Success(responseJson = "responseJson")

        every {
            vaultRepository
                .ciphersStateFlow
                .value
                .data
        } returns listOf(
            createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialList,
            ),
        )
        every {
            fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                mockAssertionRequest.requestJson,
            )
        } returns createMockPasskeyAssertionOptions(number = 1)
        every { authRepository.activeUserId } returns "activeUserId"

        createVaultItemListingViewModel()

        coVerify {
            fido2CredentialManager.isUserVerified
            fido2CredentialManager.authenticateFido2Credential(
                userId = any(),
                request = any(),
                selectedCipherView = any(),
            )
        }
    }

    @Test
    fun `Fido2AssertionRequest should show error dialog when active user id is null`() = runTest {
        val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
            .copy(cipherId = "mockId-1")
        val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
            mockAssertionRequest,
        )
        every { fido2CredentialManager.isUserVerified } returns true
        every {
            fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                mockAssertionRequest.requestJson,
            )
        } returns createMockPasskeyAssertionOptions(
            number = 1,
            userVerificationRequirement = UserVerificationRequirement.PREFERRED,
        )
        every {
            vaultRepository
                .ciphersStateFlow
                .value
                .data
        } returns listOf(
            createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialList,
            ),
        )
        every {
            fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                mockAssertionRequest.requestJson,
            )
        } returns createMockPasskeyAssertionOptions(number = 1)
        every { authRepository.activeUserId } returns null

        val viewModel = createVaultItemListingViewModel()

        coVerify(exactly = 0) {
            fido2CredentialManager.authenticateFido2Credential(
                userId = any(),
                request = any(),
                selectedCipherView = any(),
            )
        }

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2AssertionRequest should prompt for master password when passkey is protected and user has master password`() {
        val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
            .copy(cipherId = "mockId-1")
        val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
            mockAssertionRequest,
        )
        every { fido2CredentialManager.isUserVerified } returns true
        every {
            fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                mockAssertionRequest.requestJson,
            )
        } returns createMockPasskeyAssertionOptions(
            number = 1,
            userVerificationRequirement = UserVerificationRequirement.PREFERRED,
        )
        every {
            vaultRepository
                .ciphersStateFlow
                .value
                .data
        } returns listOf(
            createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialList,
                repromptType = CipherRepromptType.PASSWORD,
            ),
        )
        every {
            fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                mockAssertionRequest.requestJson,
            )
        } returns createMockPasskeyAssertionOptions(number = 1)
        every { authRepository.activeUserId } returns null

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            VaultItemListingState.DialogState.Fido2MasterPasswordPrompt(
                selectedCipherId = mockAssertionRequest.cipherId!!,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Fido2AssertionRequest should not re-prompt master password when user does not have a master password`() =
        runTest {
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = "mockId-1")
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = mockFido2CredentialList,
                repromptType = CipherRepromptType.PASSWORD,
            )
            mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                accounts = listOf(
                    DEFAULT_ACCOUNT.copy(
                        vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                        trustedDevice = UserState.TrustedDevice(
                            isDeviceTrusted = true,
                            hasAdminApproval = true,
                            hasLoginApprovingDevice = true,
                            hasResetPasswordPermission = true,
                        ),
                        hasMasterPassword = false,
                    ),
                ),
            )

            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                mockAssertionRequest,
            )
            every { fido2CredentialManager.isUserVerified } returns true
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                    mockAssertionRequest.requestJson,
                )
            } returns createMockPasskeyAssertionOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.PREFERRED,
            )
            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(mockCipherView)
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                    mockAssertionRequest.requestJson,
                )
            } returns createMockPasskeyAssertionOptions(number = 1)
            coEvery {
                fido2CredentialManager.authenticateFido2Credential(
                    DEFAULT_USER_STATE.activeUserId,
                    mockAssertionRequest,
                    mockCipherView,
                )
            } returns Fido2CredentialAssertionResult.Success("responseJson")

            createVaultItemListingViewModel()

            coVerify {
                fido2CredentialManager.authenticateFido2Credential(
                    userId = any(),
                    request = any(),
                    selectedCipherView = any(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationLockout should display Fido2ErrorDialog and set isUserVerified to false`() {
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(VaultItemListingsAction.UserVerificationLockOut)

        verify { fido2CredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified.asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationCancelled should clear dialog state, set isUserVerified to false, and emit CompleteFido2Create with cancelled result`() =
        runTest {
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(VaultItemListingsAction.UserVerificationCancelled)

            verify { fido2CredentialManager.isUserVerified = false }
            assertNull(viewModel.stateFlow.value.dialogState)
            viewModel.eventFlow.test {
                assertEquals(
                    VaultItemListingEvent.CompleteFido2Registration(
                        result = Fido2RegisterCredentialResult.Cancelled,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `UserVerificationFail should display Fido2ErrorDialog and set isUserVerified to false`() {
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(VaultItemListingsAction.UserVerificationFail)

        verify { fido2CredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationSuccess should display Fido2ErrorDialog when SpecialCircumstance is null`() =
        runTest {
            specialCircumstanceManager.specialCircumstance = null
            coEvery {
                fido2CredentialManager.registerFido2Credential(
                    any(),
                    any(),
                    any(),
                )
            } returns Fido2RegisterCredentialResult.Success(
                registrationResponse = "mockResponse",
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationSuccess(
                    createMockCipherView(number = 1),
                ),
            )

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationSuccess should display Fido2ErrorDialog when SpecialCircumstance is invalid`() =
        runTest {
            specialCircumstanceManager.specialCircumstance =
                SpecialCircumstance.AutofillSave(
                    AutofillSaveItem.Login(
                        username = "mockUsername",
                        password = "mockPassword",
                        uri = "mockUri",
                    ),
                )
            coEvery {
                fido2CredentialManager.registerFido2Credential(
                    any(),
                    any(),
                    any(),
                )
            } returns Fido2RegisterCredentialResult.Success(
                registrationResponse = "mockResponse",
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationSuccess(
                    selectedCipherView = createMockCipherView(number = 1),
                ),
            )

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationSuccess should display Fido2ErrorDialog when activeUserId is null`() {
        every { authRepository.activeUserId } returns null
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.Fido2Save(createMockFido2CredentialRequest(number = 1))

        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationSuccess(
                selectedCipherView = createMockCipherView(number = 1),
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationSuccess should set isUserVerified to true, and register FIDO 2 credential when verification result is received`() =
        runTest {
            val mockRequest = createMockFido2CredentialRequest(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = mockRequest,
            )
            coEvery {
                fido2CredentialManager.registerFido2Credential(
                    any(),
                    any(),
                    any(),
                )
            } returns Fido2RegisterCredentialResult.Success(
                registrationResponse = "mockResponse",
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationSuccess(
                    selectedCipherView = createMockCipherView(number = 1),
                ),
            )

            coVerify {
                fido2CredentialManager.isUserVerified = true
                fido2CredentialManager.registerFido2Credential(
                    userId = DEFAULT_ACCOUNT.userId,
                    fido2CreateCredentialRequest = mockRequest,
                    selectedCipherView = any(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationSuccess should set isUserVerified to true, and authenticate FIDO 2 credential when verification result is received`() =
        runTest {
            val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
                .copy(cipherId = "mockId-1")
            val mockFido2CredentialList = createMockSdkFido2CredentialList(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Assertion(
                fido2AssertionRequest = mockAssertionRequest,
            )

            every {
                vaultRepository
                    .ciphersStateFlow
                    .value
                    .data
            } returns listOf(
                createMockCipherView(
                    number = 1,
                    fido2Credentials = mockFido2CredentialList,
                ),
            )
            every {
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                    mockAssertionRequest.requestJson,
                )
            } returns createMockPasskeyAssertionOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.PREFERRED,
            )
            coEvery {
                fido2CredentialManager.authenticateFido2Credential(
                    any(),
                    any(),
                    any(),
                )
            } returns Fido2CredentialAssertionResult.Success(
                responseJson = "mockResponse",
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationSuccess(
                    selectedCipherView = createMockCipherView(number = 1),
                ),
            )

            coVerify {
                fido2CredentialManager.isUserVerified = true
                fido2CredentialManager.authenticateFido2Credential(
                    userId = DEFAULT_ACCOUNT.userId,
                    request = mockAssertionRequest,
                    selectedCipherView = any(),
                )
            }
        }

    @Test
    fun `UserVerificationNotSupported should display Fido2CreationFail when no cipher id found`() {
        val viewModel = createVaultItemListingViewModel()

        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationNotSupported(
                selectedCipherId = null,
            ),
        )

        verify { fido2CredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationNotSupported should display Fido2CreationFail when no active account found`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        mutableUserStateFlow.value = null

        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationNotSupported(
                selectedCipherId = selectedCipherId,
            ),
        )

        verify { fido2CredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `UserVerificationNotSupported should display Fido2PinPrompt when user has pin`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(
                DEFAULT_ACCOUNT.copy(
                    vaultUnlockType = VaultUnlockType.PIN,
                ),
            ),
        )
        every { settingsRepository.isUnlockWithPinEnabled } returns true

        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationNotSupported(
                selectedCipherId = selectedCipherId,
            ),
        )

        verify { fido2CredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.Fido2PinPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationNotSupported should display Fido2MasterPasswordPrompt when user has password but no pin`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"

        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationNotSupported(
                selectedCipherId = selectedCipherId,
            ),
        )

        verify { fido2CredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.Fido2MasterPasswordPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserVerificationNotSupported should display Fido2PinSetUpPrompt when user has no password or pin`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(
                DEFAULT_ACCOUNT.copy(
                    vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                    trustedDevice = UserState.TrustedDevice(
                        isDeviceTrusted = true,
                        hasAdminApproval = true,
                        hasLoginApprovingDevice = true,
                        hasResetPasswordPermission = true,
                    ),
                    hasMasterPassword = false,
                ),
            ),
        )

        viewModel.trySendAction(
            VaultItemListingsAction.UserVerificationNotSupported(
                selectedCipherId = selectedCipherId,
            ),
        )

        verify { fido2CredentialManager.isUserVerified = false }
        assertEquals(
            VaultItemListingState.DialogState.Fido2PinSetUpPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordFido2VerificationSubmit should display Fido2ErrorDialog when password verification fails`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val password = "password"
        coEvery {
            authRepository.validatePassword(password = password)
        } returns ValidatePasswordResult.Error

        viewModel.trySendAction(
            VaultItemListingsAction.MasterPasswordFido2VerificationSubmit(
                password = password,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePassword(password = password)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordFido2VerificationSubmit should display Fido2MasterPasswordError when user has retries remaining`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val password = "password"
        coEvery {
            authRepository.validatePassword(password = password)
        } returns ValidatePasswordResult.Success(isValid = false)

        viewModel.trySendAction(
            VaultItemListingsAction.MasterPasswordFido2VerificationSubmit(
                password = password,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2MasterPasswordError(
                title = null,
                message = R.string.invalid_master_password.asText(),
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePassword(password = password)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordFido2VerificationSubmit should display Fido2ErrorDialog when user has no retries remaining`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val password = "password"
        every { fido2CredentialManager.hasAuthenticationAttemptsRemaining() } returns false
        coEvery {
            authRepository.validatePassword(password = password)
        } returns ValidatePasswordResult.Success(isValid = false)

        viewModel.trySendAction(
            VaultItemListingsAction.MasterPasswordFido2VerificationSubmit(
                password = password,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePassword(password = password)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordFido2VerificationSubmit should display Fido2ErrorDialog when cipher not found`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val password = "password"
        coEvery {
            authRepository.validatePassword(password = password)
        } returns ValidatePasswordResult.Success(isValid = true)

        viewModel.trySendAction(
            VaultItemListingsAction.MasterPasswordFido2VerificationSubmit(
                password = password,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePassword(password = password)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordFido2VerificationSubmit should register credential when password authenticated successfully`() =
        runTest {
            val viewModel = createVaultItemListingViewModel()
            val cipherView = createMockCipherView(number = 1)
            val selectedCipherId = cipherView.id ?: ""
            val password = "password"
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    collectionViewList = emptyList(),
                    folderViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)

            viewModel.trySendAction(
                VaultItemListingsAction.MasterPasswordFido2VerificationSubmit(
                    password = password,
                    selectedCipherId = selectedCipherId,
                ),
            )
            coVerify {
                authRepository.validatePassword(password = password)
            }
        }

    @Test
    fun `RetryFido2PasswordVerificationClick should display Fido2MasterPasswordPrompt`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"

        viewModel.trySendAction(
            VaultItemListingsAction.RetryFido2PasswordVerificationClick(
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2MasterPasswordPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PinFido2VerificationSubmit should display Fido2ErrorDialog when pin verification fails`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val pin = "PIN"
        coEvery {
            authRepository.validatePin(pin = pin)
        } returns ValidatePinResult.Error

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2VerificationSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePin(pin = pin)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PinFido2VerificationSubmit should display Fido2PinError when user has retries remaining`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val pin = "PIN"
        coEvery {
            authRepository.validatePin(pin = pin)
        } returns ValidatePinResult.Success(isValid = false)

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2VerificationSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2PinError(
                title = null,
                message = R.string.invalid_pin.asText(),
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePin(pin = pin)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PinFido2VerificationSubmit should display Fido2ErrorDialog when user has no retries remaining`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val pin = "PIN"
        every { fido2CredentialManager.hasAuthenticationAttemptsRemaining() } returns false
        coEvery {
            authRepository.validatePin(pin = pin)
        } returns ValidatePinResult.Success(isValid = false)

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2VerificationSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePin(pin = pin)
        }
    }

    @Test
    fun `PinFido2VerificationSubmit should display Fido2ErrorDialog when cipher not found`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"
        val pin = "PIN"
        coEvery {
            authRepository.validatePin(pin = pin)
        } returns ValidatePinResult.Success(isValid = true)

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2VerificationSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify {
            authRepository.validatePin(pin = pin)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PinFido2VerificationSubmit should register credential when pin authenticated successfully`() {
        setupMockUri()
        val viewModel = createVaultItemListingViewModel()
        val cipherView = createMockCipherView(number = 1)
        val selectedCipherId = cipherView.id ?: ""
        val pin = "PIN"
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(cipherView),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )
        coEvery {
            authRepository.validatePin(pin = pin)
        } returns ValidatePinResult.Success(isValid = true)

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2VerificationSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )
        coVerify {
            authRepository.validatePin(pin = pin)
        }
    }

    @Test
    fun `RetryFido2PinVerificationClick should display FidoPinPrompt`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"

        viewModel.trySendAction(
            VaultItemListingsAction.RetryFido2PinVerificationClick(
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2PinPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `PinFido2SetUpSubmit should display Fido2PinSetUpError for empty PIN`() {
        val viewModel = createVaultItemListingViewModel()
        val pin = ""
        val selectedCipherId = "selectedCipherId"

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2SetUpSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2PinSetUpError(
                title = null,
                message = R.string.validation_field_required.asText(R.string.pin.asText()),
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `PinFido2SetUpSubmit should save PIN and register credential for non-empty PIN`() {
        setupMockUri()
        val viewModel = createVaultItemListingViewModel()
        val cipherView = createMockCipherView(number = 1)
        val pin = "PIN"
        val selectedCipherId = "selectedCipherId"
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(cipherView),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )
        every {
            settingsRepository.storeUnlockPin(
                pin = pin,
                shouldRequireMasterPasswordOnRestart = false,
            )
        } just runs

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2SetUpSubmit(
                pin = pin,
                selectedCipherId = selectedCipherId,
            ),
        )

        verify(exactly = 1) {
            settingsRepository.storeUnlockPin(
                pin = pin,
                shouldRequireMasterPasswordOnRestart = false,
            )
        }
    }

    @Test
    fun `PinFido2SetUpRetryClick should display Fido2PinSetUpPrompt`() {
        val viewModel = createVaultItemListingViewModel()
        val selectedCipherId = "selectedCipherId"

        viewModel.trySendAction(
            VaultItemListingsAction.PinFido2SetUpRetryClick(
                selectedCipherId = selectedCipherId,
            ),
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2PinSetUpPrompt(
                selectedCipherId = selectedCipherId,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `DismissFido2VerificationDialogClick should display Fido2ErrorDialog`() {
        val viewModel = createVaultItemListingViewModel()
        viewModel.trySendAction(
            VaultItemListingsAction.DismissFido2VerificationDialogClick,
        )

        assertEquals(
            VaultItemListingState.DialogState.Fido2OperationFail(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `ConfirmOverwriteExistingPasskeyClick should check if user is verified`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = createMockFido2CredentialRequest(number = 1),
            )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            every {
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(any())
            } returns createMockPasskeyAttestationOptions(
                number = 1,
                userVerificationRequirement = UserVerificationRequirement.REQUIRED,
            )

            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.ConfirmOverwriteExistingPasskeyClick(
                    cipherViewId = cipherView.id!!,
                ),
            )

            verify { fido2CredentialManager.isUserVerified }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmOverwriteExistingPasskeyClick should display Fido2ErrorDialog when getSelectedCipher returns null`() =
        runTest {
            setupMockUri()
            val cipherView = createMockCipherView(number = 1)
            specialCircumstanceManager.specialCircumstance = SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = createMockFido2CredentialRequest(number = 1),
            )
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(cipherView),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            viewModel.trySendAction(
                VaultItemListingsAction.ConfirmOverwriteExistingPasskeyClick(
                    cipherViewId = "invalidId",
                ),
            )

            assertEquals(
                VaultItemListingState.DialogState.Fido2OperationFail(
                    R.string.an_error_has_occurred.asText(),
                    R.string.passkey_operation_failed_because_user_could_not_be_verified.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }

    @Suppress("CyclomaticComplexMethod")
    private fun createSavedStateHandleWithVaultItemListingType(
        vaultItemListingType: VaultItemListingType,
    ) = SavedStateHandle().apply {
        set(
            "vault_item_listing_type",
            when (vaultItemListingType) {
                is VaultItemListingType.Card -> "card"
                is VaultItemListingType.Collection -> "collection"
                is VaultItemListingType.Folder -> "folder"
                is VaultItemListingType.Identity -> "identity"
                is VaultItemListingType.Login -> "login"
                is VaultItemListingType.SecureNote -> "secure_note"
                is VaultItemListingType.Trash -> "trash"
                is VaultItemListingType.SendFile -> "send_file"
                is VaultItemListingType.SendText -> "send_text"
                is VaultItemListingType.SshKey -> "ssh_key"
            },
        )
        set(
            "id",
            when (vaultItemListingType) {
                is VaultItemListingType.Card -> null
                is VaultItemListingType.Collection -> vaultItemListingType.collectionId
                is VaultItemListingType.Folder -> vaultItemListingType.folderId
                is VaultItemListingType.Identity -> null
                is VaultItemListingType.Login -> null
                is VaultItemListingType.SecureNote -> null
                is VaultItemListingType.Trash -> null
                is VaultItemListingType.SendFile -> null
                is VaultItemListingType.SendText -> null
                is VaultItemListingType.SshKey -> null
            },
        )
    }

    private fun setupMockUri() {
        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.mockuri.com"
    }

    private fun createVaultItemListingViewModel(
        savedStateHandle: SavedStateHandle = initialSavedStateHandle,
    ): VaultItemListingViewModel =
        VaultItemListingViewModel(
            savedStateHandle = savedStateHandle,
            clock = clock,
            clipboardManager = clipboardManager,
            authRepository = authRepository,
            vaultRepository = vaultRepository,
            environmentRepository = environmentRepository,
            settingsRepository = settingsRepository,
            accessibilitySelectionManager = accessibilitySelectionManager,
            autofillSelectionManager = autofillSelectionManager,
            cipherMatchingManager = cipherMatchingManager,
            specialCircumstanceManager = specialCircumstanceManager,
            policyManager = policyManager,
            fido2CredentialManager = fido2CredentialManager,
            organizationEventManager = organizationEventManager,
            fido2OriginManager = fido2OriginManager,
        )

    @Suppress("MaxLineLength")
    private fun createVaultItemListingState(
        itemListingType: VaultItemListingState.ItemListingType = VaultItemListingState.ItemListingType.Vault.Login,
        viewState: VaultItemListingState.ViewState = VaultItemListingState.ViewState.Loading,
    ): VaultItemListingState =
        VaultItemListingState(
            itemListingType = itemListingType,
            activeAccountSummary = DEFAULT_USER_STATE.toActiveAccountSummary(),
            accountSummaries = DEFAULT_USER_STATE.toAccountSummaries(),
            viewState = viewState,
            vaultFilterType = vaultRepository.vaultFilterType,
            baseWebSendUrl = Environment.Us.environmentUrlData.baseWebSendUrl,
            baseIconUrl = environmentRepository.environment.environmentUrlData.baseIconUrl,
            isIconLoadingDisabled = settingsRepository.isIconLoadingDisabled,
            isPullToRefreshSettingEnabled = false,
            dialogState = null,
            totpData = null,
            autofillSelectionData = null,
            policyDisablesSend = false,
            hasMasterPassword = true,
            fido2CreateCredentialRequest = null,
            isPremium = true,
            isRefreshing = false,
            organizationPremiumStatusMap = emptyMap(),
        )
}

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
    hasMasterPassword = true,
    isUsingKeyConnector = false,
    onboardingStatus = OnboardingStatus.COMPLETE,
    firstTimeState = FirstTimeState(showImportLoginsCard = true),
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = "activeUserId",
    accounts = listOf(DEFAULT_ACCOUNT),
)
