package com.bitwarden.ui.platform.base

import android.annotation.SuppressLint
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.NavHostController
import androidx.navigation.Navigator
import androidx.navigation.NavigatorProvider
import androidx.navigation.NavigatorState
import androidx.navigation.compose.ComposeNavigator
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Creates a new instance of a stubbed-out mock [NavHostController] for testing purposes, it is
 * primarily designed to be used in the context of verifying calls to [NavHostController.navigate].
 */
fun createMockNavHostController(): NavHostController =
    mockk<NavHostController> {
        every { graph } returns mockGraph
        every { graph = any() } just runs
        every { navigatorProvider } returns TestNavigatorProvider()
        every { visibleEntries } returns MutableStateFlow(value = emptyList())
        every { currentDestination } returns null
        every { currentBackStackEntryFlow } returns emptyFlow()
        every { setViewModelStore(viewModelStore = any()) } just runs
        every { setLifecycleOwner(owner = any()) } just runs
        every { navigate(route = any<Any>(), navOptions = any()) } just runs
    }

/**
 * The mock graph ID for the mock graph.
 */
private const val MOCK_GRAPH_ID: Int = -1

/**
 * Creates a new instance of mock [NavGraph] for testing.
 */
private val mockGraph: NavGraph
    get() = mockk<NavGraph> {
        every { id } returns MOCK_GRAPH_ID
        every { startDestinationId } returns MOCK_GRAPH_ID
        every {
            findNode(resId = MOCK_GRAPH_ID)
        } returns mockk { every { id } returns MOCK_GRAPH_ID }
    }

/**
 * The following is borrowed directly from the TestNavigatorProvider of the compose testing
 * library.
 *
 * See https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:navigation/navigation-testing/src/main/java/androidx/navigation/testing/TestNavigatorProvider.kt?q=TestNavigatorProvider
 */
@SuppressLint("RestrictedApi")
@Suppress("MaxLineLength")
private class TestNavigatorProvider : NavigatorProvider() {
    /**
     * A [Navigator] that only supports creating destinations.
     */
    private val navigator = object : Navigator<NavDestination>() {
        override fun createDestination() = NavDestination(navigatorName = "test")
    }

    init {
        addNavigator(navigator = NavGraphNavigator(navigatorProvider = this))
        addNavigator(name = "test", navigator = navigator)
        addNavigator(navigator = ComposeNavigator())
        val state = mockk<NavigatorState>(relaxed = true) {
            every { backStack } returns MutableStateFlow(value = emptyList())
        }
        navigators.forEach { (_, navigator) -> navigator.onAttach(state = state) }
    }

    override fun <T : Navigator<out NavDestination>> getNavigator(name: String): T =
        try {
            super.getNavigator(name = name)
        } catch (_: IllegalStateException) {
            @Suppress("UNCHECKED_CAST")
            navigator as T
        }
}
