package com.bitwarden.ui.platform.navigation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.bitwarden.ui.platform.navigation.BottomNavigationController
import com.bitwarden.ui.platform.theme.ContentTransformProvider
import com.bitwarden.ui.platform.theme.NavDisplayTransitions
import com.bitwarden.ui.platform.theme.PredictiveContentTransformProvider

/**
 * A [NavDisplay] that displays the back stacks of the tabs managed by the given
 * [bottomNavigationController], following the "multiple back stacks" pattern.
 *
 * The [tabEntries] of every tab currently in the back stack of the
 * [BottomNavigationController.root] are displayed together, so the back stack of the start tab is
 * always displayed beneath the back stack of the selected tab. The
 * [BottomNavigationController.navigateBack] is invoked whenever the user navigates back.
 */
@Composable
fun <T : NavKey> BitwardenTabNavDisplay(
    bottomNavigationController: BottomNavigationController<T>,
    tabEntries: Map<T, List<NavEntry<NavKey>>>,
    modifier: Modifier = Modifier,
    transitionSpec: ContentTransformProvider<NavKey> = NavDisplayTransitions.fade(),
    popTransitionSpec: ContentTransformProvider<NavKey> = transitionSpec,
    predictivePopTransitionSpec: PredictiveContentTransformProvider<NavKey> =
        NavDisplayTransitions.predictive(provider = popTransitionSpec),
) {
    NavDisplay(
        entries = bottomNavigationController.root.backStack.flatMap { tab ->
            tabEntries.getValue(key = tab)
        },
        modifier = modifier,
        onBack = bottomNavigationController::navigateBack,
        transitionSpec = transitionSpec,
        popTransitionSpec = popTransitionSpec,
        predictivePopTransitionSpec = predictivePopTransitionSpec,
    )
}
