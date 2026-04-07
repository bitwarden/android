package com.bitwarden.authenticator.ui.platform.feature.debugmenu

import app.cash.turbine.test
import com.bitwarden.authenticator.data.platform.manager.FeatureFlagManager
import com.bitwarden.authenticator.data.platform.repository.DebugMenuRepository
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DebugMenuViewModelTest : BaseViewModelTest() {

    private val mockFeatureFlagManager = mockk<FeatureFlagManager>(relaxed = true) {
        every { getFeatureFlagFlow<Boolean>(any()) } returns flowOf(true)
    }

    private val mockDebugMenuRepository = mockk<DebugMenuRepository>(relaxed = true) {
        coEvery { resetFeatureFlagOverrides() } just runs
        every { updateFeatureFlag<Boolean>(any(), any()) } just runs
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(viewModel.stateFlow.value, DEFAULT_STATE)
    }

    @Test
    fun `handleUpdateFeatureFlag should update the feature flag`() {
        val viewModel = createViewModel()
        assertEquals(viewModel.stateFlow.value, DEFAULT_STATE)
        viewModel.trySendAction(
            DebugMenuAction.Internal.UpdateFeatureFlagMap(UPDATED_MAP_VALUE),
        )
        assertEquals(viewModel.stateFlow.value, DebugMenuState(UPDATED_MAP_VALUE))
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
    fun `handleUpdateFeatureFlag should update the feature flag via the repository`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(
            DebugMenuAction.UpdateFeatureFlag(FlagKey.BitwardenAuthenticationEnabled, false),
        )
        verify {
            mockDebugMenuRepository.updateFeatureFlag(FlagKey.BitwardenAuthenticationEnabled, false)
        }
    }

    private fun createViewModel(): DebugMenuViewModel = DebugMenuViewModel(
        featureFlagManager = mockFeatureFlagManager,
        debugMenuRepository = mockDebugMenuRepository,
    )
}

private val DEFAULT_MAP_VALUE: ImmutableMap<FlagKey<Any>, Any> = FlagKey
    .activeAuthenticatorFlags
    .associateWith { true }
    .toImmutableMap()

private val UPDATED_MAP_VALUE: ImmutableMap<FlagKey<Any>, Any> = FlagKey
    .activeAuthenticatorFlags
    .associateWith { false }
    .toImmutableMap()

private val DEFAULT_STATE = DebugMenuState(
    featureFlags = DEFAULT_MAP_VALUE,
)
