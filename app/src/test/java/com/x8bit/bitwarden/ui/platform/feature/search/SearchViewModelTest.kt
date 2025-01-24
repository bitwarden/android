package com.x8bit.bitwarden.ui.platform.feature.search

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.LoginUriView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilitySelectionManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilitySelectionManagerImpl
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManagerImpl
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFido2CredentialList
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockUriView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.feature.search.util.createMockDisplayItemForCipher
import com.x8bit.bitwarden.ui.platform.feature.search.util.filterAndOrganize
import com.x8bit.bitwarden.ui.platform.feature.search.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toFilteredList
import io.mockk.awaits
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Suppress("LargeClass")
class SearchViewModelTest : BaseViewModelTest() {

    private val accessibilitySelectionManager: AccessibilitySelectionManager =
        AccessibilitySelectionManagerImpl()
    private val autofillSelectionManager: AutofillSelectionManager =
        AutofillSelectionManagerImpl()

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val clipboardManager: BitwardenClipboardManager = mockk {
        every { setText(any<String>()) } just runs
    }
    private val policyManager: PolicyManager = mockk<PolicyManager> {
        every {
            getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns emptyList()
    }
    private val mutableVaultDataStateFlow =
        MutableStateFlow<DataState<VaultData>>(DataState.Loading)
    private val vaultRepository: VaultRepository = mockk {
        every { vaultFilterType } returns VaultFilterType.AllVaults
        every { vaultDataStateFlow } returns mutableVaultDataStateFlow
    }
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val environmentRepository: EnvironmentRepository = mockk {
        every { environment } returns Environment.Us
    }
    private val mutableIsIconLoadingDisabledFlow = MutableStateFlow(false)
    private val settingsRepository: SettingsRepository = mockk {
        every { isIconLoadingDisabled } returns false
        every { isIconLoadingDisabledFlow } returns mutableIsIconLoadingDisabledFlow
    }

    private val mockAuthRepository = mockk<AuthRepository>(relaxed = true)
    private val specialCircumstanceManager: SpecialCircumstanceManager =
        SpecialCircumstanceManagerImpl(
            authRepository = mockAuthRepository,
            dispatcherManager = FakeDispatcherManager(),
        )
    private val organizationEventManager = mockk<OrganizationEventManager> {
        every { trackEvent(event = any()) } just runs
    }

    @BeforeEach
    fun setup() {
        mockkStatic(
            List<CipherView>::toViewState,
            List<CipherView>::filterAndOrganize,
            List<CipherView>::toFilteredList,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            List<CipherView>::toViewState,
            List<CipherView>::filterAndOrganize,
            List<CipherView>::toFilteredList,
        )
    }

    @Test
    fun `initial state should be correct when set`() {
        val state = DEFAULT_STATE.copy(searchType = SearchTypeData.Sends.All)
        val viewModel = createViewModel(initialState = state)
        assertEquals(state, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when user has PERSONAL_OWNERSHIP policy`() {
        every {
            policyManager.getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(
            SyncResponseJson.Policy(
                organizationId = "Test Org",
                id = "testId",
                type = PolicyTypeJson.PERSONAL_OWNERSHIP,
                isEnabled = true,
                data = null,
            ),
        )
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        verify {
            policyManager.getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        }
    }

    @Test
    fun `BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SearchAction.BackClick)
            assertEquals(SearchEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `DismissDialogClick should clear the dialog state`() {
        val initialState = DEFAULT_STATE.copy(
            dialogState = SearchState.DialogState.Error(
                title = null,
                message = "message".asText(),
            ),
        )
        val viewModel = createViewModel(initialState)
        viewModel.trySendAction(SearchAction.DismissDialogClick)
        assertEquals(initialState.copy(dialogState = null), viewModel.stateFlow.value)
    }

    @Test
    fun `ItemClick for vault item without totp should emit NavigateToViewCipher`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SearchAction.ItemClick(itemId = "mock"))
            assertEquals(SearchEvent.NavigateToViewCipher(cipherId = "mock"), awaitItem())
        }
    }

    @Test
    fun `ItemClick for vault item with totp should emit NavigateToEditCipher`() = runTest {
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.AddTotpLoginItem(mockk())
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SearchAction.ItemClick(itemId = "mock"))
            assertEquals(SearchEvent.NavigateToEditCipher(cipherId = "mock"), awaitItem())
        }
    }

    @Test
    fun `ItemClick for send item should emit NavigateToEditSend`() = runTest {
        val viewModel = createViewModel(DEFAULT_STATE.copy(searchType = SearchTypeData.Sends.All))
        viewModel.eventFlow.test {
            viewModel.trySendAction(SearchAction.ItemClick(itemId = "mock"))
            assertEquals(SearchEvent.NavigateToEditSend(sendId = "mock"), awaitItem())
        }
    }

    @Test
    fun `AutofillItemClick should call emitAccessibilitySelection`() = runTest {
        val cipherView = setupForAutofill(
            autofillSelectionData = AUTOFILL_SELECTION_DATA.copy(
                framework = AutofillSelectionData.Framework.ACCESSIBILITY,
            ),
        )
        val cipherId = CIPHER_ID
        val viewModel = createViewModel()

        accessibilitySelectionManager.accessibilitySelectionFlow.test {
            viewModel.trySendAction(SearchAction.AutofillItemClick(itemId = cipherId))
            assertEquals(cipherView, awaitItem())
        }
    }

    @Test
    fun `AutofillItemClick should call emitAutofillSelection`() = runTest {
        val cipherView = setupForAutofill()
        val cipherId = CIPHER_ID
        val viewModel = createViewModel()

        autofillSelectionManager.autofillSelectionFlow.test {
            viewModel.trySendAction(SearchAction.AutofillItemClick(itemId = cipherId))
            assertEquals(cipherView, awaitItem())
        }
    }

    @Test
    fun `AutofillAndSaveItemClick with request error should show error dialog`() = runTest {
        val cipherView = setupForAutofill()
        val cipherId = CIPHER_ID
        val errorMessage = "Server error"
        val updatedCipherView = cipherView.copy(
            login = createMockLoginView(number = 1, clock = clock).copy(
                uris = listOf(createMockUriView(number = 1)) +
                    LoginUriView(
                        uri = AUTOFILL_URI,
                        match = null,
                        uriChecksum = null,
                    ),
            ),
        )
        val viewModel = createViewModel()
        coEvery {
            vaultRepository.updateCipher(
                cipherId = cipherId,
                cipherView = updatedCipherView,
            )
        } returns UpdateCipherResult.Error(errorMessage)

        viewModel.stateFlow.test {
            assertEquals(INITIAL_STATE_FOR_AUTOFILL, awaitItem())

            viewModel.trySendAction(SearchAction.AutofillAndSaveItemClick(itemId = cipherId))

            assertEquals(
                INITIAL_STATE_FOR_AUTOFILL
                    .copy(
                        dialogState = SearchState.DialogState.Loading(
                            message = R.string.loading.asText(),
                        ),
                    ),
                awaitItem(),
            )

            assertEquals(
                INITIAL_STATE_FOR_AUTOFILL
                    .copy(
                        dialogState = SearchState.DialogState.Error(
                            title = null,
                            message = errorMessage.asText(),
                        ),
                    ),
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `AutofillAndSaveItemClick with request success should post to the AccessibilitySelectionManager`() =
        runTest {
            val autofillSelectionData = AUTOFILL_SELECTION_DATA.copy(
                framework = AutofillSelectionData.Framework.ACCESSIBILITY,
            )
            val cipherView = setupForAutofill(autofillSelectionData = autofillSelectionData)
            val cipherId = CIPHER_ID
            val updatedCipherView = cipherView.copy(
                login = createMockLoginView(number = 1, clock = clock).copy(
                    uris = listOf(createMockUriView(number = 1)) +
                        LoginUriView(
                            uri = AUTOFILL_URI,
                            match = null,
                            uriChecksum = null,
                        ),
                ),
            )
            val initialState = INITIAL_STATE_FOR_AUTOFILL.copy(
                autofillSelectionData = autofillSelectionData,
            )
            val viewModel = createViewModel()
            coEvery {
                vaultRepository.updateCipher(
                    cipherId = cipherId,
                    cipherView = updatedCipherView,
                )
            } returns UpdateCipherResult.Success

            turbineScope {
                val stateTurbine = viewModel
                    .stateFlow
                    .testIn(backgroundScope)
                val selectionTurbine = accessibilitySelectionManager
                    .accessibilitySelectionFlow
                    .testIn(backgroundScope)

                assertEquals(initialState, stateTurbine.awaitItem())

                viewModel.trySendAction(SearchAction.AutofillAndSaveItemClick(itemId = cipherId))

                assertEquals(
                    initialState.copy(
                        dialogState = SearchState.DialogState.Loading(
                            message = R.string.loading.asText(),
                        ),
                    ),
                    stateTurbine.awaitItem(),
                )

                assertEquals(initialState, stateTurbine.awaitItem())

                // Autofill flow is completed
                assertEquals(cipherView, selectionTurbine.awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `AutofillAndSaveItemClick with request success should post to the AutofillSelectionManager`() =
        runTest {
            val cipherView = setupForAutofill()
            val cipherId = CIPHER_ID
            val updatedCipherView = cipherView.copy(
                login = createMockLoginView(number = 1, clock = clock).copy(
                    uris = listOf(createMockUriView(number = 1)) +
                        LoginUriView(
                            uri = AUTOFILL_URI,
                            match = null,
                            uriChecksum = null,
                        ),
                ),
            )
            val viewModel = createViewModel()
            coEvery {
                vaultRepository.updateCipher(
                    cipherId = cipherId,
                    cipherView = updatedCipherView,
                )
            } returns UpdateCipherResult.Success

            turbineScope {
                val stateTurbine = viewModel
                    .stateFlow
                    .testIn(backgroundScope)
                val selectionTurbine = autofillSelectionManager
                    .autofillSelectionFlow
                    .testIn(backgroundScope)

                assertEquals(INITIAL_STATE_FOR_AUTOFILL, stateTurbine.awaitItem())

                viewModel.trySendAction(SearchAction.AutofillAndSaveItemClick(itemId = cipherId))

                assertEquals(
                    INITIAL_STATE_FOR_AUTOFILL
                        .copy(
                            dialogState = SearchState.DialogState.Loading(
                                message = R.string.loading.asText(),
                            ),
                        ),
                    stateTurbine.awaitItem(),
                )

                assertEquals(
                    INITIAL_STATE_FOR_AUTOFILL,
                    stateTurbine.awaitItem(),
                )

                // Autofill flow is completed
                assertEquals(cipherView, selectionTurbine.awaitItem())
            }
        }

    @Test
    fun `MasterPasswordRepromptSubmit for a request Error should show a generic error dialog`() =
        runTest {
            setupMockUri()
            setupForAutofill()
            val cipherId = CIPHER_ID
            val password = "password"
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Error
            val viewModel = createViewModel()
            assertEquals(
                INITIAL_STATE_FOR_AUTOFILL,
                viewModel.stateFlow.value,
            )

            viewModel.trySendAction(
                SearchAction.MasterPasswordRepromptSubmit(
                    password = password,
                    masterPasswordRepromptData = MasterPasswordRepromptData.Autofill(
                        cipherId = cipherId,
                    ),
                ),
            )

            assertEquals(
                INITIAL_STATE_FOR_AUTOFILL.copy(
                    dialogState = SearchState.DialogState.Error(
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
            setupForAutofill()
            val cipherId = CIPHER_ID
            val password = "password"
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = false)
            val viewModel = createViewModel()
            assertEquals(
                INITIAL_STATE_FOR_AUTOFILL,
                viewModel.stateFlow.value,
            )

            viewModel.trySendAction(
                SearchAction.MasterPasswordRepromptSubmit(
                    password = password,
                    masterPasswordRepromptData = MasterPasswordRepromptData.Autofill(
                        cipherId = cipherId,
                    ),
                ),
            )

            assertEquals(
                INITIAL_STATE_FOR_AUTOFILL.copy(
                    dialogState = SearchState.DialogState.Error(
                        title = null,
                        message = R.string.invalid_master_password.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordRepromptSubmit for a request Success with a valid password for autofill should post to the AccessibilitySelectionManager`() =
        runTest {
            setupMockUri()
            val cipherView = setupForAutofill(
                autofillSelectionData = AUTOFILL_SELECTION_DATA.copy(
                    framework = AutofillSelectionData.Framework.ACCESSIBILITY,
                ),
            )
            val cipherId = CIPHER_ID
            val password = "password"
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)
            val viewModel = createViewModel()

            accessibilitySelectionManager.accessibilitySelectionFlow.test {
                viewModel.trySendAction(
                    SearchAction.MasterPasswordRepromptSubmit(
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
    fun `MasterPasswordRepromptSubmit for a request Success with a valid password for autofill should post to the AutofillSelectionManager`() =
        runTest {
            setupMockUri()
            val cipherView = setupForAutofill()
            val cipherId = CIPHER_ID
            val password = "password"
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)
            val viewModel = createViewModel()

            autofillSelectionManager.autofillSelectionFlow.test {
                viewModel.trySendAction(
                    SearchAction.MasterPasswordRepromptSubmit(
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
    fun `MasterPasswordRepromptSubmit for a request Success with a valid password for autofill-and-save should make a the cipher update request`() =
        runTest {
            setupMockUri()
            val cipherView = setupForAutofill()
            val cipherId = CIPHER_ID
            val password = "password"
            val updatedCipherView = cipherView.copy(
                login = createMockLoginView(number = 1, clock = clock).copy(
                    uris = listOf(createMockUriView(number = 1)) +
                        LoginUriView(
                            uri = AUTOFILL_URI,
                            match = null,
                            uriChecksum = null,
                        ),
                ),
            )
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)
            coEvery {
                vaultRepository.updateCipher(
                    cipherId = cipherId,
                    cipherView = updatedCipherView,
                )
            } just awaits
            val viewModel = createViewModel()

            viewModel.trySendAction(
                SearchAction.MasterPasswordRepromptSubmit(
                    password = password,
                    masterPasswordRepromptData = MasterPasswordRepromptData.AutofillAndSave(
                        cipherId = cipherId,
                    ),
                ),
            )

            coVerify {
                vaultRepository.updateCipher(
                    cipherId = cipherId,
                    cipherView = updatedCipherView,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordRepromptSubmit for a request Success with a valid password for totp should emit NavigateToEditCipher`() =
        runTest {
            setupMockUri()
            val cipherId = CIPHER_ID
            val password = "password"

            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)
            val viewModel = createViewModel(initialState = DEFAULT_STATE.copy(totpData = mockk()))

            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    SearchAction.MasterPasswordRepromptSubmit(
                        password = password,
                        masterPasswordRepromptData = MasterPasswordRepromptData.Totp(
                            cipherId = cipherId,
                        ),
                    ),
                )
                assertEquals(SearchEvent.NavigateToEditCipher(cipherId), awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `MasterPasswordRepromptSubmit for a request Success with a valid password for an overflow action should perform the action`() =
        runTest {
            val cipherId = "cipherId-1234"
            val password = "password"
            val viewModel = createViewModel()
            coEvery {
                authRepository.validatePassword(password = password)
            } returns ValidatePasswordResult.Success(isValid = true)

            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    SearchAction.MasterPasswordRepromptSubmit(
                        password = password,
                        masterPasswordRepromptData = MasterPasswordRepromptData.OverflowItem(
                            action = ListingItemOverflowAction.VaultAction.EditClick(
                                cipherId = cipherId,
                                requiresPasswordReprompt = true,
                            ),
                        ),
                    ),
                )
                // Edit actions should navigate to the Edit screen
                assertEquals(SearchEvent.NavigateToEditCipher(cipherId), awaitItem())
            }
        }

    @Test
    fun `OverflowOptionClick Send EditClick should emit NavigateToEditSend`() = runTest {
        val sendId = "sendId"
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    ListingItemOverflowAction.SendAction.EditClick(sendId = sendId),
                ),
            )
            assertEquals(SearchEvent.NavigateToEditSend(sendId), awaitItem())
        }
    }

    @Test
    fun `OverflowOptionClick Send CopyUrlClick should call setText on clipboardManager`() {
        val sendUrl = "www.test.com"
        every { clipboardManager.setText(sendUrl) } just runs
        val viewModel = createViewModel()
        viewModel.trySendAction(
            SearchAction.OverflowOptionClick(
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
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    SearchAction.OverflowOptionClick(
                        ListingItemOverflowAction.SendAction.DeleteClick(sendId = sendId),
                    ),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = SearchState.DialogState.Loading(
                            message = R.string.deleting.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = SearchState.DialogState.Error(
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

            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    SearchAction.OverflowOptionClick(
                        ListingItemOverflowAction.SendAction.DeleteClick(sendId = sendId),
                    ),
                )
                assertEquals(
                    SearchEvent.ShowToast(R.string.send_deleted.asText()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `OverflowOptionClick Send ShareUrlClick should emit ShowShareSheet`() = runTest {
        val sendUrl = "www.test.com"
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    ListingItemOverflowAction.SendAction.ShareUrlClick(sendUrl = sendUrl),
                ),
            )
            assertEquals(SearchEvent.ShowShareSheet(sendUrl), awaitItem())
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

            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    SearchAction.OverflowOptionClick(
                        ListingItemOverflowAction.SendAction.RemovePasswordClick(sendId = sendId),
                    ),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = SearchState.DialogState.Loading(
                            message = R.string.removing_send_password.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = SearchState.DialogState.Error(
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

            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    SearchAction.OverflowOptionClick(
                        ListingItemOverflowAction.SendAction.RemovePasswordClick(sendId = sendId),
                    ),
                )
                assertEquals(
                    SearchEvent.ShowToast(R.string.send_password_removed.asText()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `OverflowOptionClick Vault CopyNoteClick should call setText on the ClipboardManager`() =
        runTest {
            val notes = "notes"
            val viewModel = createViewModel()
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
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
            val viewModel = createViewModel()
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
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
    fun `OverflowOptionClick Vault CopyTotpClick with GenerateTotpCode success should call setText on the ClipboardManager`() =
        runTest {
            val totpCode = "totpCode"
            val code = "Code"

            coEvery {
                vaultRepository.generateTotp(totpCode, clock.instant())
            } returns GenerateTotpResult.Success(code, 30)

            val viewModel = createViewModel()
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
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

            val viewModel = createViewModel()
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.CopyTotpClick(totpCode),
                ),
            )

            verify(exactly = 0) {
                clipboardManager.setText(text = any<String>())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OverflowOptionClick Vault CopyPasswordClick should call setText on the ClipboardManager`() =
        runTest {
            val password = "passTheWord"
            val cipherId = "mockId-1"
            val viewModel = createViewModel()
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
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
            val viewModel = createViewModel()
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
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
    fun `OverflowOptionClick Vault CopyUsernameClick should call setText on the ClipboardManager`() =
        runTest {
            val username = "bitwarden"
            val viewModel = createViewModel()
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
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
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = cipherId,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
            assertEquals(SearchEvent.NavigateToEditCipher(cipherId), awaitItem())
        }
    }

    @Test
    fun `OverflowOptionClick Vault LaunchClick should emit NavigateToUrl`() = runTest {
        val url = "www.test.com"
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.LaunchClick(url = url),
                ),
            )
            assertEquals(SearchEvent.NavigateToUrl(url), awaitItem())
        }
    }

    @Test
    fun `OverflowOptionClick Vault ViewClick should emit NavigateToUrl`() = runTest {
        val cipherId = "cipherId-9876"
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = cipherId),
                ),
            )
            assertEquals(SearchEvent.NavigateToViewCipher(cipherId), awaitItem())
        }
    }

    @Test
    fun `vaultDataStateFlow Loaded with items should update ViewState to Content`() = runTest {
        setupMockUri()
        val ciphers = listOf(createMockCipherView(number = 1))
        val expectedViewState = SearchState.ViewState.Content(
            displayItems = listOf(createMockDisplayItemForCipher(number = 1)),
        )
        every {
            ciphers.filterAndOrganize(
                searchTypeData = SearchTypeData.Vault.All,
                searchTerm = "",
            )
        } returns ciphers
        every {
            ciphers.toFilteredList(vaultFilterType = VaultFilterType.AllVaults)
        } returns ciphers
        every {
            ciphers.toViewState(
                searchTerm = "",
                baseIconUrl = "https://vault.bitwarden.com/icons",
                isIconLoadingDisabled = false,
                isAutofill = false,
                hasMasterPassword = true,
                isPremiumUser = true,
                isTotp = false,
                organizationPremiumStatusMap = emptyMap(),
            )
        } returns expectedViewState
        val dataState = DataState.Loaded(
            data = VaultData(
                cipherViewList = ciphers,
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )

        val viewModel = createViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = SearchState.ViewState.Content(
                    displayItems = listOf(
                        createMockDisplayItemForCipher(number = 1),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Loaded with empty items should update ViewState to Empty`() = runTest {
        val dataState = DataState.Loaded(
            data = VaultData(
                cipherViewList = emptyList(),
                folderViewList = emptyList(),
                collectionViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )
        val viewModel = createViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)
        assertEquals(
            DEFAULT_STATE.copy(viewState = SearchState.ViewState.Empty(null)),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Loaded with trash items should update ViewState to Empty`() = runTest {
        val dataState = DataState.Loaded(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )
        val viewModel = createViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = SearchState.ViewState.Empty(null),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Loading should update state to Loading`() = runTest {
        mutableVaultDataStateFlow.tryEmit(value = DataState.Loading)

        val viewModel = createViewModel()

        assertEquals(
            DEFAULT_STATE.copy(viewState = SearchState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Pending with data should update state to Content`() = runTest {
        setupMockUri()
        val ciphers = listOf(createMockCipherView(number = 1))
        val expectedViewState = SearchState.ViewState.Content(
            displayItems = listOf(createMockDisplayItemForCipher(number = 1)),
        )
        every {
            ciphers.filterAndOrganize(
                searchTypeData = SearchTypeData.Vault.All,
                searchTerm = "",
            )
        } returns ciphers
        every {
            ciphers.toFilteredList(vaultFilterType = VaultFilterType.AllVaults)
        } returns ciphers
        every {
            ciphers.toViewState(
                searchTerm = "",
                baseIconUrl = "https://vault.bitwarden.com/icons",
                isIconLoadingDisabled = false,
                isAutofill = false,
                hasMasterPassword = true,
                isPremiumUser = true,
                isTotp = false,
                organizationPremiumStatusMap = emptyMap(),
            )
        } returns expectedViewState
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Pending(
                data = VaultData(
                    cipherViewList = ciphers,
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            ),
        )

        val viewModel = createViewModel()

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = SearchState.ViewState.Content(
                    displayItems = listOf(
                        createMockDisplayItemForCipher(number = 1),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Pending with empty data should update state to Empty`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Pending(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            ),
        )

        val viewModel = createViewModel()

        assertEquals(
            DEFAULT_STATE.copy(viewState = SearchState.ViewState.Empty(message = null)),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Pending with trash data should update state to Empty`() = runTest {
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

        val viewModel = createViewModel()

        assertEquals(
            DEFAULT_STATE.copy(viewState = SearchState.ViewState.Empty(message = null)),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error without data should update state to Error`() = runTest {
        val dataState = DataState.Error<VaultData>(
            error = IllegalStateException(),
        )

        val viewModel = createViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = SearchState.ViewState.Error(
                    message = R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error with data should update state to Content`() = runTest {
        setupMockUri()
        val ciphers = listOf(createMockCipherView(number = 1))
        val expectedViewState = SearchState.ViewState.Content(
            displayItems = listOf(createMockDisplayItemForCipher(number = 1)),
        )
        every {
            ciphers.filterAndOrganize(
                searchTypeData = SearchTypeData.Vault.All,
                searchTerm = "",
            )
        } returns ciphers
        every {
            ciphers.toFilteredList(vaultFilterType = VaultFilterType.AllVaults)
        } returns ciphers
        every {
            ciphers.toViewState(
                searchTerm = "",
                baseIconUrl = "https://vault.bitwarden.com/icons",
                isIconLoadingDisabled = false,
                isAutofill = false,
                hasMasterPassword = true,
                isPremiumUser = true,
                isTotp = false,
                organizationPremiumStatusMap = emptyMap(),
            )
        } returns expectedViewState
        val dataState = DataState.Error(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
            error = IllegalStateException(),
        )

        val viewModel = createViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)
        assertEquals(
            DEFAULT_STATE.copy(viewState = expectedViewState),
            viewModel.stateFlow.value,
        )

        unmockkStatic(Uri::class)
    }

    @Test
    fun `vaultDataStateFlow Error with empty data should update state to Empty`() = runTest {
        val dataState = DataState.Error(
            data = VaultData(
                cipherViewList = emptyList(),
                folderViewList = emptyList(),
                collectionViewList = emptyList(),
                sendViewList = emptyList(),
            ),
            error = IllegalStateException(),
        )

        val viewModel = createViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = SearchState.ViewState.Empty(message = null),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error with trash data should update state to Empty`() = runTest {
        val dataState = DataState.Error(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
            error = IllegalStateException(),
        )

        val viewModel = createViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = SearchState.ViewState.Empty(message = null),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow NoNetwork without data should update state to Error`() = runTest {
        val dataState = DataState.NoNetwork<VaultData>()

        val viewModel = createViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = SearchState.ViewState.Error(
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
        val ciphers = listOf(createMockCipherView(number = 1))
        val expectedViewState = SearchState.ViewState.Content(
            displayItems = listOf(createMockDisplayItemForCipher(number = 1)),
        )
        every {
            ciphers.filterAndOrganize(
                searchTypeData = SearchTypeData.Vault.All,
                searchTerm = "",
            )
        } returns ciphers
        every {
            ciphers.toFilteredList(vaultFilterType = VaultFilterType.AllVaults)
        } returns ciphers
        every {
            ciphers.toViewState(
                searchTerm = "",
                baseIconUrl = "https://vault.bitwarden.com/icons",
                isIconLoadingDisabled = false,
                isAutofill = false,
                hasMasterPassword = true,
                isPremiumUser = true,
                isTotp = false,
                organizationPremiumStatusMap = emptyMap(),
            )
        } returns expectedViewState
        val dataState = DataState.NoNetwork(
            data = VaultData(
                cipherViewList = ciphers,
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )

        val viewModel = createViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = SearchState.ViewState.Content(
                    displayItems = listOf(
                        createMockDisplayItemForCipher(number = 1),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )

        unmockkStatic(Uri::class)
    }

    @Test
    fun `vaultDataStateFlow NoNetwork with empty data should update state to Empty`() = runTest {
        val dataState = DataState.NoNetwork(
            data = VaultData(
                cipherViewList = emptyList(),
                folderViewList = emptyList(),
                collectionViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )

        val viewModel = createViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = SearchState.ViewState.Empty(message = null),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow NoNetwork with trash data should update state to Empty`() = runTest {
        val dataState = DataState.NoNetwork(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )

        val viewModel = createViewModel()

        mutableVaultDataStateFlow.tryEmit(value = dataState)
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = SearchState.ViewState.Empty(message = null),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `icon loading state updates should update isIconLoadingDisabled`() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.stateFlow.value.isIconLoadingDisabled)

        mutableIsIconLoadingDisabledFlow.value = true
        assertTrue(viewModel.stateFlow.value.isIconLoadingDisabled)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun createViewModel(
        initialState: SearchState? = null,
    ): SearchViewModel = SearchViewModel(
        SavedStateHandle().apply {
            set("state", initialState)
            set(
                "search_type",
                when (initialState?.searchType) {
                    SearchTypeData.Sends.All -> "search_type_sends_all"
                    SearchTypeData.Sends.Files -> "search_type_sends_file"
                    SearchTypeData.Sends.Texts -> "search_type_sends_text"
                    SearchTypeData.Vault.All -> "search_type_vault_all"
                    SearchTypeData.Vault.Cards -> "search_type_vault_cards"
                    SearchTypeData.Vault.SshKeys -> "search_type_vault_ssh_keys"
                    is SearchTypeData.Vault.Collection -> "search_type_vault_collection"
                    is SearchTypeData.Vault.Folder -> "search_type_vault_folder"
                    SearchTypeData.Vault.Identities -> "search_type_vault_identities"
                    SearchTypeData.Vault.Logins -> "search_type_vault_logins"
                    SearchTypeData.Vault.NoFolder -> "search_type_vault_no_folder"
                    SearchTypeData.Vault.SecureNotes -> "search_type_vault_secure_notes"
                    SearchTypeData.Vault.VerificationCodes -> "search_type_vault_verification_codes"
                    SearchTypeData.Vault.Trash -> "search_type_vault_trash"
                    null -> "search_type_vault_all"
                },
            )
            set(
                "search_type_id",
                when (val searchType = initialState?.searchType) {
                    SearchTypeData.Sends.All -> null
                    SearchTypeData.Sends.Files -> null
                    SearchTypeData.Sends.Texts -> null
                    SearchTypeData.Vault.All -> null
                    SearchTypeData.Vault.Cards -> null
                    SearchTypeData.Vault.SshKeys -> null
                    is SearchTypeData.Vault.Collection -> searchType.collectionId
                    is SearchTypeData.Vault.Folder -> searchType.folderId
                    SearchTypeData.Vault.Identities -> null
                    SearchTypeData.Vault.Logins -> null
                    SearchTypeData.Vault.NoFolder -> null
                    SearchTypeData.Vault.SecureNotes -> null
                    SearchTypeData.Vault.VerificationCodes -> null
                    SearchTypeData.Vault.Trash -> null
                    null -> null
                },
            )
        },
        clock = clock,
        vaultRepo = vaultRepository,
        authRepo = authRepository,
        environmentRepo = environmentRepository,
        settingsRepo = settingsRepository,
        clipboardManager = clipboardManager,
        policyManager = policyManager,
        specialCircumstanceManager = specialCircumstanceManager,
        accessibilitySelectionManager = accessibilitySelectionManager,
        autofillSelectionManager = autofillSelectionManager,
        organizationEventManager = organizationEventManager,
    )

    /**
     * Generates and returns [CipherView] to be populated for autofill testing and sets up the
     * state to return that item.
     */
    private fun setupForAutofill(
        autofillSelectionData: AutofillSelectionData = AUTOFILL_SELECTION_DATA,
    ): CipherView {
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.AutofillSelection(
            autofillSelectionData = autofillSelectionData,
            shouldFinishWhenComplete = true,
        )
        val cipherView = createMockCipherView(
            number = 1,
            fido2Credentials = createMockSdkFido2CredentialList(number = 1),
        )
        val ciphers = listOf(cipherView)
        val expectedViewState = SearchState.ViewState.Content(
            displayItems = listOf(createMockDisplayItemForCipher(number = 1)),
        )
        every {
            ciphers.filterAndOrganize(
                searchTypeData = SearchTypeData.Vault.All,
                searchTerm = "",
            )
        } returns ciphers
        every {
            ciphers.toFilteredList(vaultFilterType = VaultFilterType.AllVaults)
        } returns ciphers
        every {
            ciphers.toViewState(
                searchTerm = "",
                baseIconUrl = "https://vault.bitwarden.com/icons",
                isIconLoadingDisabled = false,
                isAutofill = true,
                hasMasterPassword = true,
                isPremiumUser = true,
                isTotp = false,
                organizationPremiumStatusMap = emptyMap(),
            )
        } returns expectedViewState
        val dataState = DataState.Loaded(
            data = VaultData(
                cipherViewList = ciphers,
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )
        mutableVaultDataStateFlow.value = dataState
        return cipherView
    }

    private fun setupMockUri() {
        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.mockuri.com"
    }
}

private val DEFAULT_STATE: SearchState = SearchState(
    searchTerm = "",
    searchType = SearchTypeData.Vault.All,
    viewState = SearchState.ViewState.Loading,
    dialogState = null,
    vaultFilterData = null,
    baseWebSendUrl = "https://vault.bitwarden.com/#/send/",
    baseIconUrl = "https://vault.bitwarden.com/icons",
    isIconLoadingDisabled = false,
    hasMasterPassword = true,
    totpData = null,
    autofillSelectionData = null,
    isPremium = true,
    organizationPremiumStatusMap = emptyMap(),
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = "activeUserId",
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

private const val AUTOFILL_URI = "autofill-uri"

private const val CIPHER_ID = "mockId-1"

private val AUTOFILL_SELECTION_DATA =
    AutofillSelectionData(
        type = AutofillSelectionData.Type.LOGIN,
        framework = AutofillSelectionData.Framework.AUTOFILL,
        uri = AUTOFILL_URI,
    )

private val INITIAL_STATE_FOR_AUTOFILL =
    DEFAULT_STATE.copy(
        viewState = SearchState.ViewState.Content(
            displayItems = listOf(createMockDisplayItemForCipher(number = 1)),
        ),
        autofillSelectionData = AUTOFILL_SELECTION_DATA,
    )
