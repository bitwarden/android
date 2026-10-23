package com.x8bit.bitwarden.ui.vault.feature.pamapprovals.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import app.cash.turbine.test
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.DecryptCipherListResult
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.createMockOrganization
import com.x8bit.bitwarden.data.pam.repository.PamRepository
import com.x8bit.bitwarden.data.pam.repository.model.DecidePamAccessResult
import com.x8bit.bitwarden.data.pam.repository.model.PamApprovalDecision
import com.x8bit.bitwarden.data.pam.repository.model.PamApprovalRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamDecisionVerdict
import com.x8bit.bitwarden.data.pam.repository.model.PamLease
import com.x8bit.bitwarden.data.pam.repository.model.PamLeaseStatus
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import com.x8bit.bitwarden.data.pam.repository.model.RevokePamAccessResult
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.model.PamAlertData
import com.x8bit.bitwarden.data.platform.manager.model.PamAlertType
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.PAM_TEST_NOW
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.createMockPamApprovalRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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

class PamApprovalDetailViewModelTest : BaseViewModelTest() {

    private val specialCircumstanceManager: SpecialCircumstanceManager =
        SpecialCircumstanceManagerImpl(
            authRepository = mockk(relaxed = true),
            dispatcherManager = FakeDispatcherManager(),
        )
    private val pamRepository = mockk<PamRepository> {
        coEvery { getApprovalInbox() } returns listOf(createMockPamApprovalRequest()).asSuccess()
        coEvery { getApprovalHistory() } returns emptyList<PamApprovalRequest>().asSuccess()
    }
    private val vaultRepository = mockk<VaultRepository> {
        every { decryptCipherListResultStateFlow } returns MutableStateFlow(
            DataState.Loaded(
                DecryptCipherListResult(
                    successes = listOf(
                        createMockCipherListView(
                            number = 1,
                            id = "cipherId",
                            name = "Prod DB",
                        ),
                    ),
                    failures = emptyList(),
                ),
            ),
        )
        every { collectionsStateFlow } returns MutableStateFlow<DataState<List<CollectionView>>>(
            DataState.Loaded(
                listOf(createMockCollectionView(number = 1, name = "Production")),
            ),
        )
    }
    private val authRepository = mockk<AuthRepository> {
        every { userStateFlow } returns MutableStateFlow(
            mockk<UserState> {
                every { activeAccount } returns mockk<UserState.Account> {
                    every { organizations } returns listOf(
                        createMockOrganization(
                            number = 1,
                            id = "organizationId",
                            name = "Meridian Group",
                        ),
                    )
                }
            },
        )
    }
    private val environmentRepository = FakeEnvironmentRepository()
    private val settingsRepository = mockk<SettingsRepository> {
        every { isIconLoadingDisabled } returns false
    }
    private val featureFlagManager = mockk<FeatureFlagManager> {
        coEvery { getFeatureFlag(FlagKey.PamMobilePrivilegedControls) } returns true
    }
    private val clock: Clock = Clock.fixed(PAM_TEST_NOW, ZoneOffset.UTC)

    @BeforeEach
    fun setup() {
        mockkStatic(SavedStateHandle::toPamApprovalDetailArgs)
        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.mockuri1.com"
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SavedStateHandle::toPamApprovalDetailArgs)
        unmockkStatic(Uri::class)
    }

    @Test
    fun `load should show the request with names resolved from the local vault`() = runTest {
        val viewModel = createViewModel()

        val detail = viewModel.detail()
        assertEquals(
            PamApprovalDetailState.Detail(
                itemName = "Prod DB",
                itemIcon = IconData.Network(
                    uri = "${Environment.Prod.Us.baseIconUrl}/www.mockuri1.com/icon.png",
                    fallbackIconRes = BitwardenDrawable.ic_globe,
                ),
                organizationName = "Meridian Group",
                collectionName = "Production",
                requesterName = "Grace",
                requesterEmail = "grace@example.com",
                durationMinutes = 90L,
                windowStart = PAM_TEST_NOW,
                windowEnd = PAM_TEST_NOW.plusSeconds(5_400),
                reason = "Prod incident",
                status = PamRequestStatus.PENDING,
                isOwnRequest = false,
                canDecide = true,
                decidedBy = null,
                decisionComment = null,
            ),
            detail,
        )
    }

    @Test
    fun `load should use the generic icon when website icons are disabled`() = runTest {
        every { settingsRepository.isIconLoadingDisabled } returns true

        val viewModel = createViewModel()

        assertEquals(
            IconData.Local(iconRes = BitwardenDrawable.ic_globe),
            viewModel.detail().itemIcon,
        )
    }

    @Test
    fun `load should use the item type icon for items that are not logins`() = runTest {
        every { vaultRepository.decryptCipherListResultStateFlow } returns MutableStateFlow(
            DataState.Loaded(
                DecryptCipherListResult(
                    successes = listOf(
                        createMockCipherListView(
                            number = 1,
                            id = "cipherId",
                            type = CipherListViewType.SecureNote,
                        ),
                    ),
                    failures = emptyList(),
                ),
            ),
        )

        val viewModel = createViewModel()

        assertEquals(
            IconData.Local(iconRes = BitwardenDrawable.ic_note),
            viewModel.detail().itemIcon,
        )
    }

    @Test
    fun `load should use the generic icon when the item is not in the local vault`() = runTest {
        every { vaultRepository.decryptCipherListResultStateFlow } returns MutableStateFlow(
            DataState.Loaded(
                DecryptCipherListResult(successes = emptyList(), failures = emptyList()),
            ),
        )

        val viewModel = createViewModel()

        assertEquals(
            IconData.Local(iconRes = BitwardenDrawable.ic_globe),
            viewModel.detail().itemIcon,
        )
    }

    @Test
    fun `load should navigate back when the feature flag is off`() = runTest {
        coEvery {
            featureFlagManager.getFeatureFlag(FlagKey.PamMobilePrivilegedControls)
        } returns false

        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            assertEquals(PamApprovalDetailEvent.NavigateBack, awaitItem())
        }
        coVerify(exactly = 0) { pamRepository.getApprovalInbox() }
    }

    @Test
    fun `a request in the history should be shown with its decision and cannot be decided`() =
        runTest {
            coEvery { pamRepository.getApprovalInbox() } returns emptyList<PamApprovalRequest>()
                .asSuccess()
            coEvery { pamRepository.getApprovalHistory() } returns listOf(
                createMockPamApprovalRequest(status = PamRequestStatus.APPROVED).copy(
                    decision = PamApprovalDecision(
                        verdict = PamDecisionVerdict.APPROVE,
                        comment = "Looks fine",
                        deciderName = "Ada",
                        decidedAt = PAM_TEST_NOW,
                    ),
                ),
            ).asSuccess()

            val viewModel = createViewModel()

            val detail = viewModel.detail()
            assertEquals(PamRequestStatus.APPROVED, detail.status)
            assertEquals("Ada", detail.decidedBy)
            assertEquals("Looks fine", detail.decisionComment)
            assertFalse(detail.canDecide)
        }

    @Test
    fun `a request that is in neither list should show the not found state`() = runTest {
        coEvery { pamRepository.getApprovalInbox() } returns emptyList<PamApprovalRequest>()
            .asSuccess()

        val viewModel = createViewModel()

        assertEquals(PamApprovalDetailState.ViewState.NotFound, viewModel.stateFlow.value.viewState)
    }

    @Test
    fun `load failure of both lists should show the error state`() = runTest {
        coEvery { pamRepository.getApprovalInbox() } returns IllegalStateException().asFailure()
        coEvery { pamRepository.getApprovalHistory() } returns IllegalStateException().asFailure()

        val viewModel = createViewModel()

        assertEquals(PamApprovalDetailState.ViewState.Error, viewModel.stateFlow.value.viewState)
    }

    @Test
    fun `the user's own request cannot be decided and no dialog opens`() = runTest {
        coEvery { pamRepository.getApprovalInbox() } returns listOf(
            createMockPamApprovalRequest(isOwnRequest = true),
        ).asSuccess()

        val viewModel = createViewModel()
        val detail = viewModel.detail()
        assertTrue(detail.isOwnRequest)
        assertFalse(detail.canDecide)

        viewModel.trySendAction(PamApprovalDetailAction.ApproveClick)
        assertEquals(null, viewModel.stateFlow.value.dialogState)
        viewModel.trySendAction(PamApprovalDetailAction.DenyClick)
        assertEquals(null, viewModel.stateFlow.value.dialogState)
    }

    @Test
    fun `a request whose window has lapsed cannot be decided`() = runTest {
        coEvery { pamRepository.getApprovalInbox() } returns listOf(
            createMockPamApprovalRequest(windowEnd = PAM_TEST_NOW.minusSeconds(1)),
        ).asSuccess()

        val viewModel = createViewModel()

        val detail = viewModel.detail()
        assertFalse(detail.canDecide)
    }

    @Test
    fun `approve click should open the decision dialog with an empty comment`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(PamApprovalDetailAction.ApproveClick)

        assertEquals(
            PamApprovalDetailState.DialogState.Decide(
                verdict = PamDecisionVerdict.APPROVE,
                comment = "",
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `approving should be allowed without a comment`() = runTest {
        val viewModel = createViewModel()
        coEvery {
            pamRepository.decideAccessRequest(any(), any(), any())
        } returns DecidePamAccessResult.Success

        viewModel.trySendAction(PamApprovalDetailAction.ApproveClick)
        assertTrue(
            (viewModel.stateFlow.value.dialogState as PamApprovalDetailState.DialogState.Decide)
                .isSubmittable,
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(PamApprovalDetailAction.ConfirmDecisionClick)

            assertEquals(
                PamApprovalDetailEvent.ShowSnackbar(
                    BitwardenSnackbarData(message = BitwardenString.pam_request_approved.asText()),
                ),
                awaitItem(),
            )
        }
        coVerify {
            pamRepository.decideAccessRequest(
                request = createMockPamApprovalRequest(),
                verdict = PamDecisionVerdict.APPROVE,
                comment = "",
            )
        }
        assertEquals(null, viewModel.stateFlow.value.dialogState)
    }

    @Test
    fun `approving should send the comment`() = runTest {
        val viewModel = createViewModel()
        coEvery {
            pamRepository.decideAccessRequest(any(), any(), any())
        } returns DecidePamAccessResult.Success

        viewModel.trySendAction(PamApprovalDetailAction.ApproveClick)
        viewModel.trySendAction(PamApprovalDetailAction.CommentChange("Go ahead"))
        viewModel.trySendAction(PamApprovalDetailAction.ConfirmDecisionClick)

        coVerify {
            pamRepository.decideAccessRequest(
                request = createMockPamApprovalRequest(),
                verdict = PamDecisionVerdict.APPROVE,
                comment = "Go ahead",
            )
        }
    }

    @Test
    fun `denying without a reason should not be submittable and send nothing`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(PamApprovalDetailAction.DenyClick)
        val dialog = viewModel.stateFlow.value.dialogState
            as PamApprovalDetailState.DialogState.Decide
        assertFalse(dialog.isSubmittable)

        viewModel.trySendAction(PamApprovalDetailAction.ConfirmDecisionClick)
        viewModel.trySendAction(PamApprovalDetailAction.CommentChange("   "))
        viewModel.trySendAction(PamApprovalDetailAction.ConfirmDecisionClick)

        coVerify(exactly = 0) { pamRepository.decideAccessRequest(any(), any(), any()) }
        assertTrue(
            viewModel.stateFlow.value.dialogState is PamApprovalDetailState.DialogState.Decide,
        )
    }

    @Test
    fun `denying with a reason should send the decision and show a snackbar`() = runTest {
        val viewModel = createViewModel()
        coEvery {
            pamRepository.decideAccessRequest(any(), any(), any())
        } returns DecidePamAccessResult.Success

        viewModel.trySendAction(PamApprovalDetailAction.DenyClick)
        viewModel.trySendAction(PamApprovalDetailAction.CommentChange("Not needed"))
        assertTrue(
            (viewModel.stateFlow.value.dialogState as PamApprovalDetailState.DialogState.Decide)
                .isSubmittable,
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(PamApprovalDetailAction.ConfirmDecisionClick)

            assertEquals(
                PamApprovalDetailEvent.ShowSnackbar(
                    BitwardenSnackbarData(message = BitwardenString.pam_request_denied.asText()),
                ),
                awaitItem(),
            )
        }
        coVerify {
            pamRepository.decideAccessRequest(
                request = createMockPamApprovalRequest(),
                verdict = PamDecisionVerdict.DENY,
                comment = "Not needed",
            )
        }
    }

    @Test
    fun `a successful decision should reload the request`() = runTest {
        val viewModel = createViewModel()
        coEvery {
            pamRepository.decideAccessRequest(any(), any(), any())
        } returns DecidePamAccessResult.Success
        coEvery { pamRepository.getApprovalInbox() } returns emptyList<PamApprovalRequest>()
            .asSuccess()
        coEvery { pamRepository.getApprovalHistory() } returns listOf(
            createMockPamApprovalRequest(status = PamRequestStatus.APPROVED),
        ).asSuccess()

        viewModel.trySendAction(PamApprovalDetailAction.ApproveClick)
        viewModel.trySendAction(PamApprovalDetailAction.ConfirmDecisionClick)

        val detail = viewModel.detail()
        assertEquals(PamRequestStatus.APPROVED, detail.status)
        assertFalse(detail.canDecide)
    }

    @Test
    fun `a request already decided by another approver should say so and refresh`() = runTest {
        val viewModel = createViewModel()
        coEvery {
            pamRepository.decideAccessRequest(any(), any(), any())
        } returns DecidePamAccessResult.AlreadyDecided
        coEvery { pamRepository.getApprovalInbox() } returns emptyList<PamApprovalRequest>()
            .asSuccess()
        coEvery { pamRepository.getApprovalHistory() } returns listOf(
            createMockPamApprovalRequest(status = PamRequestStatus.DENIED),
        ).asSuccess()

        viewModel.trySendAction(PamApprovalDetailAction.ApproveClick)
        viewModel.trySendAction(PamApprovalDetailAction.ConfirmDecisionClick)

        assertEquals(
            PamApprovalDetailState.DialogState.Error(
                message = BitwardenString.pam_already_decided.asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        val detail = viewModel.detail()
        assertEquals(PamRequestStatus.DENIED, detail.status)
        coVerify(exactly = 2) { pamRepository.getApprovalInbox() }
    }

    @Test
    fun `a failed decision should show an error`() = runTest {
        val viewModel = createViewModel()
        coEvery {
            pamRepository.decideAccessRequest(any(), any(), any())
        } returns DecidePamAccessResult.Error(IllegalStateException())

        viewModel.trySendAction(PamApprovalDetailAction.ApproveClick)
        viewModel.trySendAction(PamApprovalDetailAction.ConfirmDecisionClick)

        assertEquals(
            PamApprovalDetailState.DialogState.Error(
                message = BitwardenString.pam_decision_failed.asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `a decision that is not allowed should show the own request message`() = runTest {
        val viewModel = createViewModel()
        coEvery {
            pamRepository.decideAccessRequest(any(), any(), any())
        } returns DecidePamAccessResult.NotAllowed

        viewModel.trySendAction(PamApprovalDetailAction.ApproveClick)
        viewModel.trySendAction(PamApprovalDetailAction.ConfirmDecisionClick)

        assertEquals(
            PamApprovalDetailState.DialogState.Error(
                message = BitwardenString.pam_cannot_decide_own.asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `a request with an active lease of another member can be revoked`() = runTest {
        stubActiveLease()
        val viewModel = createViewModel()

        val detail = viewModel.detail()

        assertTrue(detail.canRevoke)
        assertEquals(LEASE_END, detail.accessEndsAt)
        assertFalse(detail.canDecide)
    }

    @Test
    fun `a request without an active lease cannot be revoked`() = runTest {
        stubActiveLease(lease = createLease(status = PamLeaseStatus.REVOKED))
        val viewModel = createViewModel()

        val detail = viewModel.detail()

        assertFalse(detail.canRevoke)
        assertNull(detail.accessEndsAt)
    }

    @Test
    fun `a lease whose end has passed cannot be revoked`() = runTest {
        stubActiveLease(lease = createLease(notAfter = PAM_TEST_NOW.minusSeconds(1)))
        val viewModel = createViewModel()

        assertFalse(viewModel.detail().canRevoke)
    }

    @Test
    fun `the user's own lease cannot be revoked and no dialog opens`() = runTest {
        stubActiveLease(isOwnRequest = true)
        val viewModel = createViewModel()

        viewModel.trySendAction(PamApprovalDetailAction.RevokeClick)

        assertFalse(viewModel.detail().canRevoke)
        assertNull(viewModel.stateFlow.value.dialogState)
    }

    @Test
    fun `revoke click should ask for confirmation and send nothing`() = runTest {
        stubActiveLease()
        val viewModel = createViewModel()

        viewModel.trySendAction(PamApprovalDetailAction.RevokeClick)

        assertEquals(
            PamApprovalDetailState.DialogState.ConfirmRevoke,
            viewModel.stateFlow.value.dialogState,
        )
        coVerify(exactly = 0) { pamRepository.revokeAccess(any()) }
    }

    @Test
    fun `confirming without asking first should send nothing`() = runTest {
        stubActiveLease()
        val viewModel = createViewModel()

        viewModel.trySendAction(PamApprovalDetailAction.ConfirmRevokeClick)

        coVerify(exactly = 0) { pamRepository.revokeAccess(any()) }
    }

    @Test
    fun `confirming should revoke, show a snackbar and reload the request`() = runTest {
        stubActiveLease()
        val viewModel = createViewModel()
        coEvery { pamRepository.revokeAccess(any()) } returns RevokePamAccessResult.Success
        coEvery { pamRepository.getApprovalHistory() } returns listOf(
            createMockPamApprovalRequest(
                status = PamRequestStatus.APPROVED,
                lease = createLease(status = PamLeaseStatus.REVOKED),
            ),
        ).asSuccess()

        viewModel.eventFlow.test {
            viewModel.trySendAction(PamApprovalDetailAction.RevokeClick)
            viewModel.trySendAction(PamApprovalDetailAction.ConfirmRevokeClick)

            assertEquals(
                PamApprovalDetailEvent.ShowSnackbar(
                    data = BitwardenSnackbarData(
                        message = BitwardenString.pam_access_revoked.asText(),
                    ),
                ),
                awaitItem(),
            )
        }

        assertNull(viewModel.stateFlow.value.dialogState)
        assertFalse(viewModel.detail().canRevoke)
        coVerify { pamRepository.revokeAccess(match { it.lease?.id == LEASE_ID }) }
        coVerify(exactly = 2) { pamRepository.getApprovalInbox() }
    }

    @Test
    fun `a lease that already ended should say so and refresh`() = runTest {
        stubActiveLease()
        val viewModel = createViewModel()
        coEvery { pamRepository.revokeAccess(any()) } returns RevokePamAccessResult.AlreadyEnded
        coEvery { pamRepository.getApprovalHistory() } returns listOf(
            createMockPamApprovalRequest(
                status = PamRequestStatus.APPROVED,
                lease = createLease(status = PamLeaseStatus.EXPIRED),
            ),
        ).asSuccess()

        viewModel.trySendAction(PamApprovalDetailAction.RevokeClick)
        viewModel.trySendAction(PamApprovalDetailAction.ConfirmRevokeClick)

        assertEquals(
            PamApprovalDetailState.DialogState.Error(
                message = BitwardenString
                    .pam_this_access_already_ended_the_latest_status_is_shown
                    .asText(),
            ),
            viewModel.stateFlow.value.dialogState,
        )
        assertFalse(viewModel.detail().canRevoke)
    }

    @Test
    fun `a failed or refused revocation should show an error`() = runTest {
        listOf(
            RevokePamAccessResult.Error(IllegalStateException()),
            RevokePamAccessResult.NotAllowed,
        ).forEach { result ->
            stubActiveLease()
            val viewModel = createViewModel()
            coEvery { pamRepository.revokeAccess(any()) } returns result

            viewModel.trySendAction(PamApprovalDetailAction.RevokeClick)
            viewModel.trySendAction(PamApprovalDetailAction.ConfirmRevokeClick)

            assertEquals(
                PamApprovalDetailState.DialogState.Error(
                    message = BitwardenString.pam_could_not_revoke_access.asText(),
                ),
                viewModel.stateFlow.value.dialogState,
            )
        }
    }

    @Test
    fun `dismissing the dialog should clear it`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(PamApprovalDetailAction.DenyClick)

        viewModel.trySendAction(PamApprovalDetailAction.DismissDialog)

        assertEquals(null, viewModel.stateFlow.value.dialogState)
    }

    @Test
    fun `back click should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(PamApprovalDetailAction.BackClick)
            assertEquals(PamApprovalDetailEvent.NavigateBack, awaitItem())
        }
    }

    private fun PamApprovalDetailViewModel.detail(): PamApprovalDetailState.Detail =
        (stateFlow.value.viewState as PamApprovalDetailState.ViewState.Content).detail

    @Test
    fun `clearing the view model should drop the PamAlert for this request`() {
        val data = PamAlertData("userId", "requestId", PamAlertType.ACCESS_REQUESTED)
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.PamAlert(data)
        clearViewModel(createViewModel())
        assertNull(specialCircumstanceManager.specialCircumstance)
    }

    @Test
    fun `clearing the view model should keep the PamAlert for another request`() {
        val data = PamAlertData("userId", "otherRequestId", PamAlertType.ACCESS_REQUESTED)
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

    private fun stubActiveLease(
        isOwnRequest: Boolean = false,
        lease: PamLease = createLease(),
    ) {
        coEvery { pamRepository.getApprovalInbox() } returns emptyList<PamApprovalRequest>()
            .asSuccess()
        coEvery { pamRepository.getApprovalHistory() } returns listOf(
            createMockPamApprovalRequest(
                status = PamRequestStatus.APPROVED,
                isOwnRequest = isOwnRequest,
                lease = lease,
            ),
        ).asSuccess()
    }

    private fun createViewModel(): PamApprovalDetailViewModel = PamApprovalDetailViewModel(
        pamRepository = pamRepository,
        vaultRepository = vaultRepository,
        authRepository = authRepository,
        environmentRepository = environmentRepository,
        settingsRepository = settingsRepository,
        featureFlagManager = featureFlagManager,
        clock = clock,
        specialCircumstanceManager = specialCircumstanceManager,
        savedStateHandle = SavedStateHandle().apply {
            every { toPamApprovalDetailArgs() } returns PamApprovalDetailArgs(
                requestId = "requestId",
            )
        },
    )
}

private const val LEASE_ID = "leaseId"
private val LEASE_END = PAM_TEST_NOW.plusSeconds(1_800)

private fun createLease(
    status: PamLeaseStatus = PamLeaseStatus.ACTIVE,
    notAfter: Instant = LEASE_END,
): PamLease = PamLease(
    id = LEASE_ID,
    status = status,
    notBefore = PAM_TEST_NOW.minusSeconds(600),
    notAfter = notAfter,
)
