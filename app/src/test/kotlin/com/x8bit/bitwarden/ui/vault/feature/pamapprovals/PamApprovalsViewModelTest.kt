package com.x8bit.bitwarden.ui.vault.feature.pamapprovals

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.vault.DecryptCipherListResult
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.createMockOrganization
import com.x8bit.bitwarden.data.pam.manager.PamApprovalManager
import com.x8bit.bitwarden.data.pam.repository.PamRepository
import com.x8bit.bitwarden.data.pam.repository.model.PamApprovalRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
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

class PamApprovalsViewModelTest : BaseViewModelTest() {

    private val pamRepository = mockk<PamRepository> {
        coEvery { getApprovalInbox() } returns emptyList<PamApprovalRequest>().asSuccess()
        coEvery { getApprovalHistory() } returns emptyList<PamApprovalRequest>().asSuccess()
    }
    private val pamApprovalManager = mockk<PamApprovalManager> {
        every { refreshPendingCount() } just runs
    }
    private val vaultRepository = mockk<VaultRepository> {
        every { decryptCipherListResultStateFlow } returns MutableStateFlow(
            DataState.Loaded(
                DecryptCipherListResult(
                    successes = listOf(
                        createMockCipherListView(number = 1, id = "cipherId", name = "Prod DB"),
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
                        createMockOrganization(number = 1, id = "organizationId"),
                    )
                }
            },
        )
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
    private val featureFlagManager = mockk<FeatureFlagManager> {
        coEvery { getFeatureFlag(FlagKey.PamMobilePrivilegedControls) } returns true
    }

    @Test
    fun `initial load with no requests should show empty lists`() = runTest {
        val viewModel = createViewModel()

        assertEquals(
            PamApprovalsState.ViewState.Content(inbox = emptyList(), history = emptyList()),
            viewModel.stateFlow.value.viewState,
        )
        assertTrue(viewModel.stateFlow.value.visibleRequests.isEmpty())
    }

    @Test
    fun `load should navigate back and not call the SDK when the feature flag is off`() =
        runTest {
            coEvery {
                featureFlagManager.getFeatureFlag(FlagKey.PamMobilePrivilegedControls)
            } returns false

            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                assertEquals(PamApprovalsEvent.NavigateBack, awaitItem())
            }
            coVerify(exactly = 0) { pamRepository.getApprovalInbox() }
        }

    @Test
    fun `inbox and history should be mapped with names resolved from the local vault`() =
        runTest {
            coEvery { pamRepository.getApprovalInbox() } returns listOf(
                createMockPamApprovalRequest(id = "inbox"),
            ).asSuccess()
            coEvery { pamRepository.getApprovalHistory() } returns listOf(
                createMockPamApprovalRequest(
                    id = "history",
                    status = PamRequestStatus.APPROVED,
                    collectionId = "unknownCollection",
                    cipherId = "unknownCipher",
                    requesterName = null,
                ),
            ).asSuccess()

            val viewModel = createViewModel()

            val content = viewModel.stateFlow.value.viewState as PamApprovalsState.ViewState.Content
            val inbox = content.inbox.single()
            assertEquals("inbox", inbox.id)
            assertEquals("Prod DB", inbox.itemName)
            assertEquals(
                IconData.Network(
                    uri = "https://icons.bitwarden.net/www.mockuri1.com/icon.png",
                    fallbackIconRes = BitwardenDrawable.ic_globe,
                ),
                inbox.itemIcon,
            )
            assertEquals("Production", inbox.collectionName)
            assertEquals("Grace", inbox.requesterName)
            assertEquals(PamRequestStatus.PENDING, inbox.status)
            val history = content.history.single()
            assertEquals("history", history.id)
            assertEquals(null, history.itemName)
            assertEquals(IconData.Local(iconRes = BitwardenDrawable.ic_globe), history.itemIcon)
            assertEquals(null, history.collectionName)
            assertEquals("grace@example.com", history.requesterName)
            assertEquals(PamRequestStatus.APPROVED, history.status)
        }

    @Test
    fun `selecting a tab should show that tab's requests`() = runTest {
        coEvery { pamRepository.getApprovalInbox() } returns listOf(
            createMockPamApprovalRequest(id = "inbox"),
        ).asSuccess()
        coEvery { pamRepository.getApprovalHistory() } returns listOf(
            createMockPamApprovalRequest(id = "history", status = PamRequestStatus.DENIED),
        ).asSuccess()
        val viewModel = createViewModel()

        assertEquals(listOf("inbox"), viewModel.stateFlow.value.visibleRequests.map { it.id })

        viewModel.trySendAction(PamApprovalsAction.TabClick(PamApprovalsState.Tab.HISTORY))

        assertEquals(listOf("history"), viewModel.stateFlow.value.visibleRequests.map { it.id })
    }

    @Test
    fun `a failing history should not hide the inbox`() = runTest {
        coEvery { pamRepository.getApprovalInbox() } returns listOf(
            createMockPamApprovalRequest(id = "inbox"),
        ).asSuccess()
        coEvery { pamRepository.getApprovalHistory() } returns IllegalStateException().asFailure()

        val viewModel = createViewModel()

        val content = viewModel.stateFlow.value.viewState as PamApprovalsState.ViewState.Content
        assertEquals(listOf("inbox"), content.inbox.map { it.id })
        assertTrue(content.history.isEmpty())
    }

    @Test
    fun `inbox load failure should show the error state`() = runTest {
        coEvery { pamRepository.getApprovalInbox() } returns IllegalStateException().asFailure()

        val viewModel = createViewModel()

        assertEquals(PamApprovalsState.ViewState.Error, viewModel.stateFlow.value.viewState)
    }

    @Test
    fun `refresh failure should keep the content that was already loaded`() = runTest {
        val viewModel = createViewModel()
        coEvery { pamRepository.getApprovalInbox() } returns IllegalStateException().asFailure()

        viewModel.trySendAction(PamApprovalsAction.RefreshPull)

        assertTrue(viewModel.stateFlow.value.viewState is PamApprovalsState.ViewState.Content)
        assertEquals(false, viewModel.stateFlow.value.isRefreshing)
    }

    @Test
    fun `lifecycle resume should reload the requests`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(PamApprovalsAction.LifecycleResume)

        coVerify(exactly = 2) { pamRepository.getApprovalInbox() }
    }

    @Test
    fun `a successful load should refresh the pending count badge`() = runTest {
        createViewModel()

        verify { pamApprovalManager.refreshPendingCount() }
    }

    @Test
    fun `back click should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(PamApprovalsAction.BackClick)
            assertEquals(PamApprovalsEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `request click should emit NavigateToDetail`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(PamApprovalsAction.RequestClick(requestId = "abc"))
            assertEquals(PamApprovalsEvent.NavigateToDetail(requestId = "abc"), awaitItem())
        }
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(::isBuildVersionAtLeast)
        every { isBuildVersionAtLeast(any()) } returns true
        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.mockuri1.com"
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::isBuildVersionAtLeast)
        unmockkStatic(Uri::class)
    }

    @Test
    fun `first open on Android 13 plus asks for notifications and dismissing remembers it`() {
        val viewModel = createViewModel()
        assertTrue(viewModel.stateFlow.value.showNotificationPrompt)

        viewModel.trySendAction(PamApprovalsAction.NotificationPromptDismiss)

        assertFalse(viewModel.stateFlow.value.showNotificationPrompt)
        assertTrue(hasShownPrompt)
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

    private fun createViewModel(): PamApprovalsViewModel = PamApprovalsViewModel(
        pamRepository = pamRepository,
        pamApprovalManager = pamApprovalManager,
        vaultRepository = vaultRepository,
        authRepository = authRepository,
        featureFlagManager = featureFlagManager,
        settingsRepository = settingsRepository,
        environmentRepository = environmentRepository,
        buildInfoManager = buildInfoManager,
        savedStateHandle = SavedStateHandle(),
    )
}
