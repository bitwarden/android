package com.bitwarden.ui.platform.navigation

import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavKey
import app.cash.turbine.test
import com.bitwarden.ui.platform.navigation.util.navigationControllerOf
import com.bitwarden.ui.platform.navigation.util.popUpTo
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NavigationControllerTest {

    @Test
    fun `navigate should add the route to the top of the back stack`() {
        val navigationController = navigationControllerOf<TestGraph>(TestGraph.RouteA)

        navigationController.navigate(route = TestGraph.RouteB)

        assertEquals(listOf(TestGraph.RouteA, TestGraph.RouteB), navigationController.backStack)
    }

    @Test
    fun `navigateBack should remove the top entry when there are multiple entries`() {
        val navigationController = navigationControllerOf(TestGraph.RouteA, TestGraph.RouteB)

        val result = navigationController.navigateBack()

        assertTrue(result)
        assertEquals(listOf(TestGraph.RouteA), navigationController.backStack)
    }

    @Test
    fun `navigateBack should not remove the last remaining entry`() {
        val navigationController = navigationControllerOf(TestGraph.RouteA)

        val result = navigationController.navigateBack()

        assertFalse(result)
        assertEquals(listOf(TestGraph.RouteA), navigationController.backStack)
    }

    @Test
    fun `addSingleTop should add the route when the top entry is a different type`() {
        val navigationController = navigationControllerOf<TestGraph>(TestGraph.RouteA)

        navigationController.addSingleTop(route = TestGraph.RouteB)

        assertEquals(listOf(TestGraph.RouteA, TestGraph.RouteB), navigationController.backStack)
    }

    @Test
    fun `addSingleTop should not add the route when the top entry is the same type`() {
        val navigationController = navigationControllerOf(
            TestGraph.RouteA,
            TestGraph.RouteC(id = "1"),
        )

        navigationController.addSingleTop(TestGraph.RouteC(id = "2"))

        assertEquals(
            listOf(TestGraph.RouteA, TestGraph.RouteC(id = "1")),
            navigationController.backStack,
        )
    }

    @Test
    fun `popUpTo should pop every entry above the most recent matching entry`() {
        val navigationController = navigationControllerOf(
            TestGraph.RouteA,
            TestGraph.RouteB,
            TestGraph.RouteC(id = "1"),
            TestGraph.RouteB,
            TestGraph.RouteC(id = "2"),
        )

        val result = navigationController.popUpTo<TestGraph.RouteB>()

        assertTrue(result)
        assertEquals(
            listOf(
                TestGraph.RouteA,
                TestGraph.RouteB,
                TestGraph.RouteC(id = "1"),
                TestGraph.RouteB,
            ),
            navigationController.backStack,
        )
    }

    @Test
    fun `popUpTo with inclusive should also pop the matching entry`() {
        val navigationController = navigationControllerOf(
            TestGraph.RouteA,
            TestGraph.RouteB,
            TestGraph.RouteC(id = "1"),
        )

        val result = navigationController.popUpTo<TestGraph.RouteB>(inclusive = true)

        assertTrue(result)
        assertEquals(listOf(TestGraph.RouteA), navigationController.backStack)
    }

    @Test
    fun `popUpTo with inclusive should not pop the last remaining entry`() {
        val navigationController = navigationControllerOf(TestGraph.RouteA, TestGraph.RouteB)

        val result = navigationController.popUpTo<TestGraph.RouteA>(inclusive = true)

        assertTrue(result)
        assertEquals(listOf(TestGraph.RouteA), navigationController.backStack)
    }

    @Test
    fun `popUpTo should do nothing when there is no matching entry`() {
        val navigationController = navigationControllerOf(TestGraph.RouteA, TestGraph.RouteB)

        val result = navigationController.popUpTo<TestGraph.RouteC>()

        assertFalse(result)
        assertEquals(listOf(TestGraph.RouteA, TestGraph.RouteB), navigationController.backStack)
    }

    @Test
    fun `popUpTo with a predicate should pop every entry above the most recent match`() {
        val navigationController = navigationControllerOf(
            TestGraph.RouteA,
            TestGraph.RouteC(id = "1"),
            TestGraph.RouteC(id = "2"),
            TestGraph.RouteB,
        )

        val result = navigationController.popUpTo { it == TestGraph.RouteC(id = "1") }

        assertTrue(result)
        assertEquals(
            listOf(TestGraph.RouteA, TestGraph.RouteC(id = "1")),
            navigationController.backStack,
        )
    }

    @Test
    fun `popToRoot should pop every entry except the root entry`() {
        val navigationController = navigationControllerOf(
            TestGraph.RouteA,
            TestGraph.RouteB,
            TestGraph.RouteC(id = "1"),
        )

        navigationController.popToRoot()

        assertEquals(listOf(TestGraph.RouteA), navigationController.backStack)
    }

    @Test
    fun `popToRoot should do nothing when only the root entry exists`() {
        val navigationController = navigationControllerOf(TestGraph.RouteA)

        navigationController.popToRoot()

        assertEquals(listOf(TestGraph.RouteA), navigationController.backStack)
    }

    @Test
    fun `navigatePoppingUpTo should pop above the matching entry before adding the route`() {
        val navigationController = navigationControllerOf(
            TestGraph.RouteA,
            TestGraph.RouteB,
            TestGraph.RouteC(id = "1"),
        )

        navigationController.navigatePoppingUpTo(route = TestGraph.RouteC(id = "2")) {
            it is TestGraph.RouteB
        }

        assertEquals(
            listOf(TestGraph.RouteA, TestGraph.RouteB, TestGraph.RouteC(id = "2")),
            navigationController.backStack,
        )
    }

    @Test
    fun `navigatePoppingUpTo with inclusive should pop the matching entry before adding`() {
        val navigationController = navigationControllerOf(
            TestGraph.RouteA,
            TestGraph.RouteB,
            TestGraph.RouteC(id = "1"),
        )

        navigationController.navigatePoppingUpTo(
            route = TestGraph.RouteC(id = "2"),
            inclusive = true,
        ) { it is TestGraph.RouteB }

        assertEquals(
            listOf(TestGraph.RouteA, TestGraph.RouteC(id = "2")),
            navigationController.backStack,
        )
    }

    @Test
    fun `navigatePoppingUpTo with inclusive should allow replacing the only entry`() {
        val navigationController = navigationControllerOf<TestGraph>(TestGraph.RouteB)

        navigationController.navigatePoppingUpTo(route = TestGraph.RouteA, inclusive = true) {
            it is TestGraph.RouteB
        }

        assertEquals(listOf(TestGraph.RouteA), navigationController.backStack)
    }

    @Test
    fun `navigatePoppingUpTo should add the route when there is no matching entry`() {
        val navigationController = navigationControllerOf<TestGraph>(TestGraph.RouteA)

        navigationController.navigatePoppingUpTo(route = TestGraph.RouteC(id = "1")) {
            it is TestGraph.RouteB
        }

        assertEquals(
            listOf(TestGraph.RouteA, TestGraph.RouteC(id = "1")),
            navigationController.backStack,
        )
    }

    @Test
    fun `replaceAll should replace the entire back stack`() {
        val navigationController = navigationControllerOf(TestGraph.RouteA, TestGraph.RouteB)

        navigationController.replaceAll(TestGraph.RouteC(id = "1"), TestGraph.RouteB)

        assertEquals(
            listOf(TestGraph.RouteC(id = "1"), TestGraph.RouteB),
            navigationController.backStack,
        )
    }

    @Test
    fun `replaceAll with a list should replace the entire back stack`() {
        val navigationController = navigationControllerOf(TestGraph.RouteA, TestGraph.RouteB)

        navigationController.replaceAll(routes = listOf(TestGraph.RouteC(id = "1")))

        assertEquals(listOf(TestGraph.RouteC(id = "1")), navigationController.backStack)
    }

    @Test
    fun `replaceAll should throw when the given routes are empty`() {
        val navigationController = navigationControllerOf(TestGraph.RouteA)

        assertThrows<IllegalArgumentException> {
            navigationController.replaceAll(routes = emptyList())
        }
        assertEquals(listOf(TestGraph.RouteA), navigationController.backStack)
    }

    @Test
    fun `navigateToTopLevelRoute should retain the start route beneath the given route`() {
        val navigationController = navigationControllerOf(TestGraph.RouteA, TestGraph.RouteB)

        navigationController.navigateToTopLevelRoute(route = TestGraph.RouteC(id = "1"))

        assertEquals(
            listOf(TestGraph.RouteA, TestGraph.RouteC(id = "1")),
            navigationController.backStack,
        )
    }

    @Test
    fun `navigateToTopLevelRoute should only contain the start route when navigating to it`() {
        val navigationController = navigationControllerOf(TestGraph.RouteA, TestGraph.RouteB)

        navigationController.navigateToTopLevelRoute(route = TestGraph.RouteA)

        assertEquals(listOf(TestGraph.RouteA), navigationController.backStack)
    }

    @Test
    fun `replaceLast should replace the top entry of the back stack`() {
        val navigationController = navigationControllerOf(TestGraph.RouteA, TestGraph.RouteB)

        navigationController.replaceLast(route = TestGraph.RouteC(id = "1"))

        assertEquals(
            listOf(TestGraph.RouteA, TestGraph.RouteC(id = "1")),
            navigationController.backStack,
        )
    }

    @Test
    fun `replaceLast should replace the only entry of the back stack`() {
        val navigationController = navigationControllerOf<TestGraph>(TestGraph.RouteA)

        navigationController.replaceLast(route = TestGraph.RouteB)

        assertEquals(listOf(TestGraph.RouteB), navigationController.backStack)
    }

    @Test
    fun `backStackSnapshot should emit the current back stack and any changes to it`() =
        runTest {
            val navigationController = navigationControllerOf<TestGraph>(TestGraph.RouteA)

            navigationController.backStackSnapshot.test {
                assertEquals(listOf(TestGraph.RouteA), awaitItem())

                navigationController.navigate(route = TestGraph.RouteB)
                Snapshot.sendApplyNotifications()
                assertEquals(listOf(TestGraph.RouteA, TestGraph.RouteB), awaitItem())

                navigationController.replaceAll(TestGraph.RouteC(id = "1"))
                assertEquals(listOf(TestGraph.RouteC(id = "1")), awaitItem())
            }
        }
}

private sealed class TestGraph : NavKey {
    data object RouteA : TestGraph()

    data object RouteB : TestGraph()

    data class RouteC(val id: String) : TestGraph()
}
