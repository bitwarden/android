package com.x8bit.bitwarden.ui.platform.feature.settings.vault.myaccess

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import app.cash.turbine.test
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.DecryptCipherListResult
import com.x8bit.bitwarden.data.pam.manager.PamApprovalManager
import com.x8bit.bitwarden.data.pam.repository.PamRepository
import com.x8bit.bitwarden.data.pam.repository.model.PamExtensionPolicy
import com.x8bit.bitwarden.data.pam.repository.model.PamLease
import com.x8bit.bitwarden.data.pam.repository.model.PamLeaseStatus
import com.x8bit.bitwarden.data.pam.repository.model.PamMyAccessRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.model.PamAlertData
import com.x8bit.bitwarden.data.platform.manager.model.PamAlertType
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MyAccessViewModelTest : BaseViewModelTest() {

    private val specialCircumstanceManager: SpecialCircumstanceManager =
        SpecialCircumstanceManagerImpl(
            authRepository = mockk(relaxed = true),
            dispatcherManager = FakeDispatcherManager(),
        )
    private val pamRepository = mockk<PamRepository> {
        coEvery { getMyAccessRequests() } returns emptyList<PamMyAccessRequest>().asSuccess()
    }
    private val mutableCipherListFlow = MutableStateFlow<DataState<DecryptCipherListResult>>(
        DataState.Loaded(
            data = DecryptCipherListResult(
                successes = listOf(
                    mockk<CipherListView> {
                        every { id } returns CIPHER_ID
                        every { name } returns "Prod DB"
                        every { type } returns CipherListViewType.SecureNote
                    },
                ),
                failures = emptyList(),
            ),
        ),
    )
    private val pamApprovalManager = mockk<PamApprovalManager> {
        every { refreshMyRequestCount() } just runs
    }
    private val vaultRepository = mockk<VaultRepository> {
        every { decryptCipherListResultStateFlow } returns mutableCipherListFlow
    }
    private val featureFlagManager = mockk<FeatureFlagManager> {
        coEvery { getFeatureFlag(FlagKey.PamMobilePrivilegedControls) } returns true
    }

    private var hasShownPrompt = false
    private val settingsRepository = mockk<SettingsRepository> {
        every { hasShownPamNotificationPrompt } answers { hasShownPrompt }
        every { hasShownPamNotificationPrompt = any() } answers { hasShownPrompt = firstArg() }
        every { isIconLoadingDisabled } returns false
    }
    private val environmentRepository = FakeEnvironmentRepository()
    private val buildInfoManager = mockk<BuildInfoManager> {
        every { isFdroid } returns false
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(::isBuildVersionAtLeast)
        every { isBuildVersionAtLeast(any()) } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::isBuildVersionAtLeast)
    }

    @Test
    fun `first open on Android 13 plus asks for notifications and dismissing remembers it`() {
        val viewModel = createViewModel()
        assertTrue(viewModel.stateFlow.value.showNotificationPrompt)

        viewModel.trySendAction(MyAccessAction.NotificationPromptDismiss)

        assertFalse(viewModel.stateFlow.value.showNotificationPrompt)
        assertTrue(hasShownPrompt)
        assertFalse(createViewModel().stateFlow.value.showNotificationPrompt)
    }

    @Test
    fun `no notification prompt after the approver inbox already asked`() {
        hasShownPrompt = true
        assertFalse(createViewModel().stateFlow.value.showNotificationPrompt)
    }

    @Test
    fun `no notification prompt on F-Droid`() {
        every { buildInfoManager.isFdroid } returns true
        assertFalse(createViewModel().stateFlow.value.showNotificationPrompt)
    }

    @Test
    fun `no notification prompt before Android 13`() {
        every { isBuildVersionAtLeast(any()) } returns false
        assertFalse(createViewModel().stateFlow.value.showNotificationPrompt)
    }

    @Test
    fun `initial load should show an empty current list`() = runTest {
        val viewModel = createViewModel()

        assertEquals(
            MyAccessState.ViewState.Content(requests = emptyList()),
            viewModel.stateFlow.value.viewState,
        )
        assertTrue(viewModel.stateFlow.value.visibleRequests.isEmpty())
    }

    @Test
    fun `load should navigate back when the feature flag is off`() = runTest {
        coEvery {
            featureFlagManager.getFeatureFlag(FlagKey.PamMobilePrivilegedControls)
        } returns false

        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            assertEquals(MyAccessEvent.NavigateBack, awaitItem())
        }
        coVerify(exactly = 0) { pamRepository.getMyAccessRequests() }
    }

    @Test
    fun `load failure should show the error state`() = runTest {
        coEvery { pamRepository.getMyAccessRequests() } returns IllegalStateException().asFailure()

        val viewModel = createViewModel()

        assertEquals(MyAccessState.ViewState.Error, viewModel.stateFlow.value.viewState)
    }

    @Test
    fun `refresh failure should keep the content that was already loaded`() = runTest {
        val viewModel = createViewModel()
        coEvery { pamRepository.getMyAccessRequests() } returns IllegalStateException().asFailure()

        viewModel.trySendAction(MyAccessAction.RefreshPull)

        assertTrue(viewModel.stateFlow.value.viewState is MyAccessState.ViewState.Content)
        assertEquals(false, viewModel.stateFlow.value.isRefreshing)
    }

    @Test
    fun `requests should be split between current and history with item names`() = runTest {
        coEvery { pamRepository.getMyAccessRequests() } returns listOf(
            createRequest(id = "pending", status = PamRequestStatus.PENDING),
            createRequest(id = "ready", status = PamRequestStatus.APPROVED, awaiting = true),
            createRequest(id = "active", status = PamRequestStatus.APPROVED, lease = activeLease()),
            createRequest(id = "denied", status = PamRequestStatus.DENIED),
            createRequest(id = "cancelled", status = PamRequestStatus.CANCELLED),
            createRequest(id = "expired", status = PamRequestStatus.EXPIRED),
            createRequest(
                id = "revoked",
                status = PamRequestStatus.APPROVED,
                lease = activeLease(status = PamLeaseStatus.REVOKED),
            ),
            createRequest(
                id = "lapsed",
                status = PamRequestStatus.APPROVED,
                lease = activeLease(notAfter = NOW.minusSeconds(1)),
            ),
        ).asSuccess()

        val viewModel = createViewModel()

        val current = viewModel.stateFlow.value.visibleRequests
        assertEquals(listOf("pending", "ready", "active"), current.map { it.id })
        assertEquals(
            listOf(
                MyAccessState.Stage.PENDING,
                MyAccessState.Stage.READY,
                MyAccessState.Stage.ACTIVE,
            ),
            current.map { it.stage },
        )
        assertEquals("Prod DB", current.first().itemName)

        viewModel.trySendAction(MyAccessAction.TabClick(MyAccessState.Tab.HISTORY))

        val history = viewModel.stateFlow.value.visibleRequests
        assertEquals(
            listOf(
                MyAccessState.Stage.DENIED,
                MyAccessState.Stage.CANCELLED,
                MyAccessState.Stage.EXPIRED,
                MyAccessState.Stage.REVOKED,
                MyAccessState.Stage.EXPIRED,
            ),
            history.map { it.stage },
        )
    }

    @Test
    fun `an approved extension request should not offer to start access`() = runTest {
        coEvery { pamRepository.getMyAccessRequests() } returns listOf(
            createRequest(
                id = "ext",
                status = PamRequestStatus.APPROVED,
                awaiting = true,
                isExtension = true,
            ),
        ).asSuccess()

        val viewModel = createViewModel()

        viewModel.trySendAction(MyAccessAction.TabClick(MyAccessState.Tab.HISTORY))
        assertEquals(
            MyAccessState.Stage.APPROVED,
            viewModel.stateFlow.value.visibleRequests.single().stage,
        )
    }

    @Test
    fun `active lease should be extendable only when the server allows it`() = runTest {
        coEvery { pamRepository.getMyAccessRequests() } returns listOf(
            createRequest(
                id = "can",
                status = PamRequestStatus.APPROVED,
                lease = activeLease(),
                extension = PamExtensionPolicy(maxDurationSeconds = 900L),
            ),
            createRequest(
                id = "cannot",
                status = PamRequestStatus.APPROVED,
                lease = activeLease(id = "lease2"),
            ),
        ).asSuccess()

        val viewModel = createViewModel()

        val requests = viewModel.stateFlow.value.visibleRequests
        assertTrue(requests[0].canExtend)
        assertEquals(15L, requests[0].maxExtensionMinutes)
        assertEquals(false, requests[1].canExtend)
    }

    @Test
    fun `RequestClick should navigate to the vault item when it is in the local vault`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                viewModel.trySendAction(MyAccessAction.RequestClick(cipherId = CIPHER_ID))
                assertEquals(
                    MyAccessEvent.NavigateToVaultItem(
                        args = VaultItemArgs(
                            vaultItemId = CIPHER_ID,
                            cipherType = VaultItemCipherType.SECURE_NOTE,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `RequestClick should show a message when the item is not in the local vault`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MyAccessAction.RequestClick(cipherId = "unknown"))
            assertEquals(
                MyAccessEvent.ShowSnackbar(
                    BitwardenSnackbarData(message = BitwardenString.pam_item_unavailable.asText()),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `RequestClick without a cipher id should show a message`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MyAccessAction.RequestClick(cipherId = null))
            assertEquals(
                MyAccessEvent.ShowSnackbar(
                    BitwardenSnackbarData(message = BitwardenString.pam_item_unavailable.asText()),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `StartAccessClick should start access and show a snackbar`() = runTest {
        coEvery { pamRepository.startAccess(REQUEST_ID) } returns Unit.asSuccess()
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MyAccessAction.StartAccessClick(REQUEST_ID))
            assertEquals(
                MyAccessEvent.ShowSnackbar(
                    BitwardenSnackbarData(message = BitwardenString.pam_access_started.asText()),
                ),
                awaitItem(),
            )
        }
        assertNull(viewModel.stateFlow.value.dialogState)
        coVerify(exactly = 1) { pamRepository.startAccess(REQUEST_ID) }
        coVerify(exactly = 2) { pamRepository.getMyAccessRequests() }
    }

    @Test
    fun `StartAccessClick failure should show an error dialog and reload`() = runTest {
        coEvery {
            pamRepository.startAccess(REQUEST_ID)
        } returns IllegalStateException().asFailure()
        val viewModel = createViewModel()

        viewModel.trySendAction(MyAccessAction.StartAccessClick(REQUEST_ID))

        assertEquals(
            MyAccessState.DialogState.Error(BitwardenString.pam_action_failed.asText()),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify(exactly = 2) { pamRepository.getMyAccessRequests() }
    }

    @Test
    fun `CancelRequestClick should confirm before cancelling`() = runTest {
        coEvery { pamRepository.cancelAccessRequest(REQUEST_ID) } returns Unit.asSuccess()
        val viewModel = createViewModel()

        viewModel.trySendAction(MyAccessAction.CancelRequestClick(REQUEST_ID))
        assertEquals(
            MyAccessState.DialogState.ConfirmCancel(REQUEST_ID),
            viewModel.stateFlow.value.dialogState,
        )
        coVerify(exactly = 0) { pamRepository.cancelAccessRequest(any()) }

        viewModel.trySendAction(MyAccessAction.ConfirmCancelRequestClick)

        coVerify(exactly = 1) { pamRepository.cancelAccessRequest(REQUEST_ID) }
        assertNull(viewModel.stateFlow.value.dialogState)
    }

    @Test
    fun `DismissDialog should clear the dialog without acting`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(MyAccessAction.EndAccessClick(LEASE_ID))
        viewModel.trySendAction(MyAccessAction.DismissDialog)

        assertNull(viewModel.stateFlow.value.dialogState)
        coVerify(exactly = 0) { pamRepository.endAccess(any()) }
    }

    @Test
    fun `EndAccessClick should confirm before ending access`() = runTest {
        coEvery { pamRepository.endAccess(LEASE_ID) } returns Unit.asSuccess()
        val viewModel = createViewModel()

        viewModel.trySendAction(MyAccessAction.EndAccessClick(LEASE_ID))
        assertEquals(
            MyAccessState.DialogState.ConfirmEnd(LEASE_ID),
            viewModel.stateFlow.value.dialogState,
        )

        viewModel.trySendAction(MyAccessAction.ConfirmEndAccessClick)

        coVerify(exactly = 1) { pamRepository.endAccess(LEASE_ID) }
        assertNull(viewModel.stateFlow.value.dialogState)
    }

    @Test
    fun `ExtendAccessClick should open the form with the server maximum`() = runTest {
        coEvery { pamRepository.getMyAccessRequests() } returns listOf(
            createRequest(
                id = REQUEST_ID,
                status = PamRequestStatus.APPROVED,
                lease = activeLease(),
                extension = PamExtensionPolicy(maxDurationSeconds = 900L),
            ),
        ).asSuccess()
        val viewModel = createViewModel()

        viewModel.trySendAction(MyAccessAction.ExtendAccessClick(LEASE_ID))

        assertEquals(
            MyAccessState.DialogState.Extend(
                leaseId = LEASE_ID,
                durationMinutes = "",
                reason = "",
                maxDurationMinutes = 15L,
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `ExtendAccessClick should be ignored when the lease cannot be extended`() = runTest {
        coEvery { pamRepository.getMyAccessRequests() } returns listOf(
            createRequest(
                id = REQUEST_ID,
                status = PamRequestStatus.APPROVED,
                lease = activeLease(),
            ),
        ).asSuccess()
        val viewModel = createViewModel()

        viewModel.trySendAction(MyAccessAction.ExtendAccessClick(LEASE_ID))

        assertNull(viewModel.stateFlow.value.dialogState)
    }

    @Test
    fun `SubmitExtendClick should require a reason and a valid duration`() = runTest {
        val viewModel = extendableViewModel(maxSeconds = 900L)

        viewModel.trySendAction(MyAccessAction.ExtendAccessClick(LEASE_ID))
        viewModel.trySendAction(MyAccessAction.ExtendDurationChange("10"))
        viewModel.trySendAction(MyAccessAction.SubmitExtendClick)
        coVerify(exactly = 0) { pamRepository.extendAccess(any(), any(), any()) }

        viewModel.trySendAction(MyAccessAction.ExtendReasonChange("   "))
        viewModel.trySendAction(MyAccessAction.SubmitExtendClick)
        coVerify(exactly = 0) { pamRepository.extendAccess(any(), any(), any()) }

        viewModel.trySendAction(MyAccessAction.ExtendReasonChange("Need it"))
        viewModel.trySendAction(MyAccessAction.ExtendDurationChange("16"))
        viewModel.trySendAction(MyAccessAction.SubmitExtendClick)
        coVerify(exactly = 0) { pamRepository.extendAccess(any(), any(), any()) }
    }

    @Test
    fun `SubmitExtendClick should extend the lease in seconds with the trimmed reason`() =
        runTest {
            coEvery {
                pamRepository.extendAccess(LEASE_ID, 600L, "Need it")
            } returns Unit.asSuccess()
            val viewModel = extendableViewModel(maxSeconds = 900L)

            viewModel.eventFlow.test {
                viewModel.trySendAction(MyAccessAction.ExtendAccessClick(LEASE_ID))
                viewModel.trySendAction(MyAccessAction.ExtendDurationChange("10"))
                viewModel.trySendAction(MyAccessAction.ExtendReasonChange("  Need it "))
                viewModel.trySendAction(MyAccessAction.SubmitExtendClick)
                assertEquals(
                    MyAccessEvent.ShowSnackbar(
                        BitwardenSnackbarData(
                            message = BitwardenString.pam_extension_requested.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
            assertNull(viewModel.stateFlow.value.dialogState)
            coVerify(exactly = 1) { pamRepository.extendAccess(LEASE_ID, 600L, "Need it") }
        }

    @Test
    fun `LeaseCountdownFinished should reload the requests`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(MyAccessAction.LeaseCountdownFinished)

        coVerify(exactly = 2) { pamRepository.getMyAccessRequests() }
    }

    @Test
    fun `BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MyAccessAction.BackClick)
            assertEquals(MyAccessEvent.NavigateBack, awaitItem())
        }
    }

    private fun extendableViewModel(maxSeconds: Long?): MyAccessViewModel {
        coEvery { pamRepository.getMyAccessRequests() } returns listOf(
            createRequest(
                id = REQUEST_ID,
                status = PamRequestStatus.APPROVED,
                lease = activeLease(),
                extension = PamExtensionPolicy(maxDurationSeconds = maxSeconds),
            ),
        ).asSuccess()
        return createViewModel()
    }

    @Test
    fun `clearing the view model should drop a decision PamAlert`() {
        val data = PamAlertData("userId", REQUEST_ID, PamAlertType.ACCESS_APPROVED)
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.PamAlert(data)
        clearViewModel(createViewModel())
        assertNull(specialCircumstanceManager.specialCircumstance)
    }

    @Test
    fun `clearing the view model should keep an approver PamAlert`() {
        val data = PamAlertData("userId", REQUEST_ID, PamAlertType.ACCESS_REQUESTED)
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.PamAlert(data)
        clearViewModel(createViewModel())
        assertEquals(
            SpecialCircumstance.PamAlert(data),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    private fun clearViewModel(viewModel: ViewModel) {
        val store = ViewModelStore()
        val provider = ViewModelProvider.create(
            store = store,
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T = viewModel as T
            },
        )
        provider[viewModel::class.java]
        store.clear()
    }

    @Test
    fun `a successful load should refresh the vault my requests count`() = runTest {
        createViewModel()

        verify { pamApprovalManager.refreshMyRequestCount() }
    }

    private fun createViewModel(): MyAccessViewModel = MyAccessViewModel(
        pamRepository = pamRepository,
        pamApprovalManager = pamApprovalManager,
        vaultRepository = vaultRepository,
        featureFlagManager = featureFlagManager,
        clock = FIXED_CLOCK,
        specialCircumstanceManager = specialCircumstanceManager,
        settingsRepository = settingsRepository,
        environmentRepository = environmentRepository,
        buildInfoManager = buildInfoManager,
        savedStateHandle = SavedStateHandle(),
    )
}

private const val CIPHER_ID = "cipherId"
private const val REQUEST_ID = "requestId"
private const val LEASE_ID = "leaseId"
private val NOW: Instant = Instant.parse("2026-01-01T12:00:00Z")
private val FIXED_CLOCK: Clock = Clock.fixed(NOW, ZoneOffset.UTC)

private fun activeLease(
    id: String = LEASE_ID,
    status: PamLeaseStatus = PamLeaseStatus.ACTIVE,
    notAfter: Instant = NOW.plusSeconds(1_800),
): PamLease = PamLease(
    id = id,
    status = status,
    notBefore = NOW.minusSeconds(600),
    notAfter = notAfter,
)

@Suppress("LongParameterList")
private fun createRequest(
    id: String,
    status: PamRequestStatus,
    awaiting: Boolean = false,
    isExtension: Boolean = false,
    lease: PamLease? = null,
    extension: PamExtensionPolicy? = null,
): PamMyAccessRequest = PamMyAccessRequest(
    id = id,
    cipherId = CIPHER_ID,
    status = status,
    reason = "Because",
    submittedAt = NOW.minusSeconds(60),
    resolvedAt = null,
    windowStart = NOW.minusSeconds(600),
    windowEnd = NOW.plusSeconds(1_800),
    isExtension = isExtension,
    awaitingActivation = awaiting,
    lease = lease,
    extensionPolicy = extension,
)
