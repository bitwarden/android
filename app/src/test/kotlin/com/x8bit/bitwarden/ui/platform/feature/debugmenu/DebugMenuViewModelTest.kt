package com.x8bit.bitwarden.ui.platform.feature.debugmenu

import app.cash.turbine.test
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.util.assertCoroutineThrows
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.CookieAcquisitionRequestManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.log.SettingsLogManager
import com.x8bit.bitwarden.data.platform.manager.model.CookieAcquisitionRequest
import com.x8bit.bitwarden.data.platform.manager.model.NotificationPayload
import com.x8bit.bitwarden.data.platform.manager.model.PamAlertType
import com.x8bit.bitwarden.data.platform.repository.DebugMenuRepository
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DebugMenuViewModelTest : BaseViewModelTest() {

    private val mockFeatureFlagManager = mockk<FeatureFlagManager>(relaxed = true) {
        every { getFeatureFlagFlow<Boolean>(any()) } returns flowOf(true)
    }

    private val mockAuthRepository = mockk<AuthRepository>(relaxed = true) {
        every { hasPendingAccountAddition = true } just runs
    }

    private val mockDebugMenuRepository = mockk<DebugMenuRepository>(relaxed = true) {
        coEvery { resetFeatureFlagOverrides() } just runs
        every { updateFeatureFlag<Boolean>(any(), any()) } just runs
        every { resetOnboardingStatusForCurrentUser() } just runs
        every { resetAccessibilityDisclaimer() } just runs
        every {
            modifyStateToShowOnboardingCarousel(userStateUpdateTrigger = any())
        } answers {
            // invokes the passed in lambda, allowing verification in tests.
            firstArg<() -> Unit>().invoke()
        }
    }

    private val logsManager = mockk<LogsManager> {
        every { trackNonFatalException(throwable = any()) } just runs
    }
    private val settingsLogManager = mockk<SettingsLogManager> {
        every { data } returns "settingsLogData"
    }

    private val mockCookieAcquisitionRequestManager =
        mockk<CookieAcquisitionRequestManager> {
            every { setPendingCookieAcquisition(data = any()) } just runs
        }

    private val fakeEnvironmentRepository = FakeEnvironmentRepository()

    private val pushManager = mockk<PushManager> {
        every { onMessageReceived(data = any()) } just runs
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(viewModel.stateFlow.value, DEFAULT_STATE)
    }

    @Test
    fun `handleMainTypeOptionClick should update the main option state`() {
        val viewModel = createViewModel()
        assertEquals(viewModel.stateFlow.value, DEFAULT_STATE)
        viewModel.trySendAction(
            DebugMenuAction.MainTypeOptionClick(DebugMenuState.MainTypeOption.OPTIONS),
        )
        assertEquals(
            viewModel.stateFlow.value,
            DEFAULT_STATE.copy(mainTypeOption = DebugMenuState.MainTypeOption.OPTIONS),
        )
    }

    @Test
    fun `handleUpdateFeatureFlag should update the feature flag`() {
        val viewModel = createViewModel()
        assertEquals(viewModel.stateFlow.value, DEFAULT_STATE)
        viewModel.trySendAction(
            DebugMenuAction.Internal.UpdateFeatureFlagMap(UPDATED_MAP_VALUE),
        )
        assertEquals(
            viewModel.stateFlow.value,
            DEFAULT_STATE.copy(featureFlags = UPDATED_MAP_VALUE),
        )
    }

    @Test
    fun `handleResetFeatureFlagValues should reset the feature flag values`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.ResetFeatureFlagValues)
        coVerify(exactly = 1) { mockDebugMenuRepository.resetFeatureFlagOverrides() }
    }

    @Test
    fun `handleNavigateBack should send NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.NavigateBack)
        viewModel.eventFlow.test {
            assertEquals(DebugMenuEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `GenerateCrashClick should throw an IllegalStateException`() {
        val viewModel = createViewModel()
        assertCoroutineThrows<IllegalStateException> {
            viewModel.trySendAction(DebugMenuAction.GenerateCrashClick)
        }
    }

    @Test
    fun `GenerateErrorReportClick should log an IllegalStateException`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.GenerateErrorReportClick)
        verify(exactly = 1) {
            logsManager.trackNonFatalException(throwable = any())
        }
    }

    @Test
    fun `SendPamAlertClick for a request should feed the push manager a type 30 payload`() {
        every { mockAuthRepository.userStateFlow } returns MutableStateFlow(USER_STATE)
        createViewModel().trySendAction(
            DebugMenuAction.SendPamAlertClick(type = PamAlertType.ACCESS_REQUESTED),
        )

        val data = slot<Map<String, String>>()
        verify(exactly = 1) { pushManager.onMessageReceived(data = capture(data)) }
        assertEquals("30", data.captured["type"])
        val payload = Json.decodeFromString<NotificationPayload.PamAccessNotification>(
            requireNotNull(data.captured["payload"]),
        )
        assertEquals("activeUserId", payload.userId)
        assertFalse(payload.requestId.isNullOrBlank())
        assertNull(payload.outcome)
    }

    @Test
    fun `SendPamAlertClick for a decision should feed the push manager a type 31 payload`() {
        every { mockAuthRepository.userStateFlow } returns MutableStateFlow(USER_STATE)
        val viewModel = createViewModel()
        val outcomes = listOf(
            PamAlertType.ACCESS_APPROVED to 1,
            PamAlertType.ACCESS_DENIED to 2,
            PamAlertType.ACCESS_REVOKED to 3,
        )
        outcomes.forEach { (type, _) ->
            viewModel.trySendAction(DebugMenuAction.SendPamAlertClick(type = type))
        }

        val data = mutableListOf<Map<String, String>>()
        verify(exactly = 3) { pushManager.onMessageReceived(data = capture(data)) }
        data.zip(outcomes).forEach { (sent, expected) ->
            assertEquals("31", sent["type"])
            val payload = Json.decodeFromString<NotificationPayload.PamAccessNotification>(
                requireNotNull(sent["payload"]),
            )
            assertEquals("activeUserId", payload.userId)
            assertEquals(expected.second, payload.outcome)
        }
    }

    @Test
    fun `SendPamAlertClick without an active user should not send anything`() {
        every { mockAuthRepository.userStateFlow } returns MutableStateFlow(null)
        createViewModel().trySendAction(
            DebugMenuAction.SendPamAlertClick(type = PamAlertType.ACCESS_APPROVED),
        )
        verify(exactly = 0) { pushManager.onMessageReceived(data = any()) }
    }

    @Test
    fun `handleUpdateFeatureFlag should update the feature flag via the repository`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.UpdateFeatureFlag(FlagKey.DummyBoolean, false))
        verify(exactly = 1) {
            mockDebugMenuRepository.updateFeatureFlag(FlagKey.DummyBoolean, false)
        }
    }

    @Test
    fun `handleResetOnboardingStatus should reset the onboarding status`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.RestartOnboarding)
        verify(exactly = 1) { mockDebugMenuRepository.resetOnboardingStatusForCurrentUser() }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `handleResetOnboardingCarousel should reset the onboarding carousel and update user state pending account action`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.RestartOnboardingCarousel)
        verify(exactly = 1) {
            mockDebugMenuRepository.modifyStateToShowOnboardingCarousel(any())
            mockAuthRepository.hasPendingAccountAddition = true
        }
    }

    @Test
    fun `ResetAccessibilityDisclaimer should call repository to reset values`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.ResetAccessibilityDisclaimer)
        verify(exactly = 1) {
            mockDebugMenuRepository.resetAccessibilityDisclaimer()
        }
    }

    @Test
    fun `handleResetCoachMarkTourStatuses should call repository to reset values`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.ResetCoachMarkTourStatuses)
        verify(exactly = 1) {
            mockDebugMenuRepository.resetCoachMarkTourStatuses()
        }
    }

    @Test
    fun `ClearSsoCookies should call clearSsoCookies on DebugMenuRepository`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.ClearSsoCookies)
        verify(exactly = 1) {
            mockDebugMenuRepository.clearSsoCookies()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ResetPremiumUpgradeBanner should call resetPremiumUpgradeBannerDismiss on DebugMenuRepository`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.ResetPremiumUpgradeBanner)
        verify(exactly = 1) {
            mockDebugMenuRepository.resetPremiumUpgradeBannerDismiss()
        }
    }

    @Test
    fun `ShowUpgradedToPremiumCard should call showUpgradedToPremiumCard on DebugMenuRepository`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.ShowUpgradedToPremiumCard)
        verify(exactly = 1) {
            mockDebugMenuRepository.showUpgradedToPremiumCard()
        }
    }

    @Test
    fun `TriggerCookieAcquisition should set pending cookie acquisition`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(DebugMenuAction.TriggerCookieAcquisition)
            verify(exactly = 1) {
                mockCookieAcquisitionRequestManager.setPendingCookieAcquisition(
                    data = CookieAcquisitionRequest(
                        hostname = "https://vault.bitwarden.com",
                    ),
                )
            }
            viewModel.eventFlow.test { expectNoEvents() }
        }

    @Test
    fun `ShareSettingsClick should emit ShareText with the settings log data`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(DebugMenuAction.ShareSettingsClick)
            assertEquals(
                DebugMenuEvent.ShareText(text = "settingsLogData"),
                awaitItem(),
            )
        }
    }

    private fun createViewModel(): DebugMenuViewModel = DebugMenuViewModel(
        featureFlagManager = mockFeatureFlagManager,
        debugMenuRepository = mockDebugMenuRepository,
        authRepository = mockAuthRepository,
        logsManager = logsManager,
        settingsLogManager = settingsLogManager,
        cookieAcquisitionRequestManager = mockCookieAcquisitionRequestManager,
        environmentRepository = fakeEnvironmentRepository,
        pushManager = pushManager,
    )
}

private val USER_STATE = mockk<UserState> {
    every { activeUserId } returns "activeUserId"
}

private val DEFAULT_MAP_VALUE: ImmutableMap<FlagKey<Any>, Any> = FlagKey
    .activePasswordManagerFlags
    .associateWith { true }
    .toImmutableMap()

private val UPDATED_MAP_VALUE: ImmutableMap<FlagKey<Any>, Any> = FlagKey
    .activePasswordManagerFlags
    .associateWith { false }
    .toImmutableMap()

private val DEFAULT_STATE = DebugMenuState(
    featureFlags = DEFAULT_MAP_VALUE,
    mainTypeOption = DebugMenuState.MainTypeOption.FLAGS,
)
