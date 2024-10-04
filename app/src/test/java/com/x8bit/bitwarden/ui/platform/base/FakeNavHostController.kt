package com.x8bit.bitwarden.ui.platform.base

import android.content.Context
import android.net.Uri
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavigatorProvider
import androidx.navigation.NavigatorState
import androidx.navigation.compose.ComposeNavigator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals

/**
 * A "fake" implementation of a [NavHostController] that serves as an alternative to the direct
 * use of a [TestNavHostController](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:navigation/navigation-testing/src/main/java/androidx/navigation/testing/TestNavHostController.kt?q=TestNavHostController).
 *
 * The primary features of this implementations are:
 *
 * - Access to a [Context] is not required, so the class can be instantiated immediately in a test.
 * - Requested navigation is never actually performed. Instead, a record of the [lastNavigation]
 *   params is provided, as well as what would be the [currentRoute] if that navigation was
 *   performed.
 * - Helper functions like [assertCurrentRoute] are provided to make clear what kind of
 *   functionality is probably mocked/faked and suitable for testing. These are the recommended
 *   way to interact with this class.
 *
 * Note that this should only be used in cases where testing that a navigation is requested but
 * not performed is sufficient (i.e. in focused tests of a single Composable screen, rather than
 * an actual navigation flow). Because the initial setting of the graph is also stubbed out, tests
 * of Composable screens that represent the host for a nested navigation graph should not try to
 * test any of that graph's visible contents.
 */
@Suppress("MaxLineLength")
class FakeNavHostController : NavHostController(context = mockk()) {
    init {
        navigatorProvider = TestNavigatorProvider()
        navigatorProvider.addNavigator(ComposeNavigator())

        val state = mockk<NavigatorState>(relaxed = true) {
            every { backStack } returns MutableStateFlow(emptyList())
        }
        navigatorProvider.navigators.forEach { (_, navigator) ->
            navigator.onAttach(state)
        }
    }

    /**
     * A fake ID that may be used when testing "popping" to a particular graph ID.
     */
    val graphId: Int = -1

    /**
     * The current route (or `null` if no initial graph has been set and no navigation has been
     * performed).
     *
     * Note that this represents what the route **would be** if actual navigation were allowed to
     * be performed.
     */
    private var currentRoute: String? = null

    /**
     * Represents the parameters of the last known navigation attempt via a call to [navigate].
     */
    private var lastNavigation: Navigation? = null

    /**
     * A mocked-out internal graph. This exists purely to allow for some internal Compose logic
     * to complete without incident when rending a nav graph in a Composable.
     */
    private val internalGraph =
        mockk<NavGraph>().apply {
            every { id } returns graphId
            every { startDestinationId } returns graphId
            every {
                findNode(graphId)
            } returns mockk { every { id } returns graphId }
        }

    override var graph: NavGraph
        get() = internalGraph
        set(value) {
            currentRoute = value.startDestinationRoute
        }

    override fun navigate(
        request: NavDeepLinkRequest,
        navOptions: NavOptions?,
    ) {
        navigate(
            request = request,
            navOptions = navOptions,
            navigatorExtras = null,
        )
    }

    override fun navigate(
        request: NavDeepLinkRequest,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
    ) {
        lastNavigation = Navigation(
            request = request,
            navOptions = navOptions,
            navigatorExtras = navigatorExtras,
        )
        currentRoute = request.uri?.route
    }

    /**
     * Asserts the [currentRoute] matches the given [route].
     */
    fun assertCurrentRoute(route: String) {
        assertEquals(route, currentRoute)
    }

    /**
     * Asserts multiple aspects of the last navigation to have occurred.
     */
    fun assertLastNavigation(
        route: String,
        navOptions: NavOptions? = null,
        navigatorExtras: Navigator.Extras? = null,
    ) {
        assertEquals(route, currentRoute)
        assertEquals(navOptions, lastNavigation?.navOptions)
        assertEquals(navigatorExtras, lastNavigation?.navigatorExtras)
    }

    /**
     * Asserts the [lastNavigation] includes the given [navOptions].
     */
    fun assertLastNavOptions(navOptions: NavOptions?) {
        assertEquals(navOptions, lastNavigation?.navOptions)
    }

    data class Navigation(
        val request: NavDeepLinkRequest,
        val navOptions: NavOptions?,
        val navigatorExtras: Navigator.Extras?,
    )
}

/**
 * Helper function for converting a [Uri] to a "route" that we'd expect to see when calling
 * `NavHostController.currentDestination?.route`.
 */
private val Uri.route: String get() = "${this.path}".removePrefix("/")

/**
 * The following is borrowed directly from the TestNavigatorProvider of the compose testing
 * library.
 *
 * See https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:navigation/navigation-testing/src/main/java/androidx/navigation/testing/TestNavigatorProvider.kt?q=TestNavigatorProvider
 */
@Suppress("MaxLineLength")
private class TestNavigatorProvider : NavigatorProvider() {

    /**
     * A [Navigator] that only supports creating destinations.
     */
    private val navigator = object : Navigator<NavDestination>() {
        override fun createDestination() = NavDestination("test")
    }

    init {
        addNavigator(NavGraphNavigator(this))
        addNavigator("test", navigator)
    }

    override fun <T : Navigator<out NavDestination>> getNavigator(name: String): T {
        return try {
            super.getNavigator(name)
        } catch (e: IllegalStateException) {
            @Suppress("UNCHECKED_CAST")
            navigator as T
        }
    }
}
