package com.x8bit.bitwarden.ui.auth.feature.welcome

import app.cash.turbine.test
import com.bitwarden.data.datasource.disk.model.ServerConfig
import com.bitwarden.data.repository.ServerConfigRepository
import com.bitwarden.network.model.ConfigResponseJson
import com.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WelcomeViewModelTest : BaseViewModelTest() {

    private val mutableServerConfigFlow = MutableStateFlow<ServerConfig?>(null)
    private val serverConfigRepository: ServerConfigRepository = mockk {
        every { serverConfigStateFlow } returns mutableServerConfigFlow
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
        }
    }

    @Test
    fun `initial state should be correct when user registration is disabled`() = runTest {
        mutableServerConfigFlow.value = createServerConfig(disableUserRegistration = true)
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(disableCreateAccount = true),
                awaitItem(),
            )
        }
    }

    @Test
    fun `server config changes should update state`() = runTest {
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableServerConfigFlow.value = createServerConfig(disableUserRegistration = true)
            assertEquals(DEFAULT_STATE.copy(disableCreateAccount = true), awaitItem())
            mutableServerConfigFlow.value = createServerConfig(disableUserRegistration = false)
            assertEquals(DEFAULT_STATE.copy(disableCreateAccount = false), awaitItem())
            mutableServerConfigFlow.value = createServerConfig(disableUserRegistration = true)
            assertEquals(DEFAULT_STATE.copy(disableCreateAccount = true), awaitItem())
            mutableServerConfigFlow.value = null
            assertEquals(DEFAULT_STATE.copy(disableCreateAccount = false), awaitItem())
        }
    }

    @Test
    fun `PagerSwipe should update state`() = runTest {
        val viewModel = createViewModel()
        val newIndex = 2

        viewModel.trySendAction(WelcomeAction.PagerSwipe(index = newIndex))

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(index = newIndex),
                awaitItem(),
            )
        }
    }

    @Test
    fun `DotClick should update state and emit UpdatePager`() = runTest {
        val viewModel = createViewModel()
        val newIndex = 2

        viewModel.trySendAction(WelcomeAction.DotClick(index = newIndex))

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(index = newIndex),
                awaitItem(),
            )
        }
        viewModel.eventFlow.test {
            assertEquals(
                WelcomeEvent.UpdatePager(index = newIndex),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CreateAccountClick should emit NavigateToStartRegistration`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(WelcomeAction.CreateAccountClick)

        viewModel.eventFlow.test {
            assertEquals(
                WelcomeEvent.NavigateToStartRegistration,
                awaitItem(),
            )
        }
    }

    @Test
    fun `LoginClick should emit NavigateToLogin`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(WelcomeAction.LoginClick)

        viewModel.eventFlow.test {
            assertEquals(
                WelcomeEvent.NavigateToLogin,
                awaitItem(),
            )
        }
    }

    private fun createViewModel(): WelcomeViewModel = WelcomeViewModel(
        serverConfigRepository = serverConfigRepository,
    )
}

private val DEFAULT_STATE = WelcomeState(
    index = 0,
    pages = listOf(
        WelcomeState.WelcomeCard.CardOne,
        WelcomeState.WelcomeCard.CardTwo,
        WelcomeState.WelcomeCard.CardThree,
        WelcomeState.WelcomeCard.CardFour,
    ),
    disableCreateAccount = false,
)

private fun createServerConfig(
    disableUserRegistration: Boolean,
): ServerConfig = ServerConfig(
    lastSync = 0L,
    serverData = ConfigResponseJson(
        type = null,
        version = null,
        gitHash = null,
        server = null,
        environment = null,
        featureStates = null,
        communication = null,
        settings = ConfigResponseJson.SettingJson(
            disableUserRegistration = disableUserRegistration,
        ),
    ),
)
