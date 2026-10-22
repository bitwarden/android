package com.bitwarden.ui.platform.navigation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.rememberBitwardenNavEntryDecorators
import com.bitwarden.ui.platform.theme.ContentTransformProvider
import com.bitwarden.ui.platform.theme.NavDisplayTransitions
import com.bitwarden.ui.platform.theme.PredictiveContentTransformProvider

/**
 * A [NavDisplay] that displays the back stack of the given [navigationController], which is
 * popped whenever the user navigates back.
 */
@Composable
fun <T : NavKey> BitwardenNavDisplay(
    navigationController: NavigationController<T>,
    entryProvider: (key: T) -> NavEntry<T>,
    modifier: Modifier = Modifier,
    transitionSpec: ContentTransformProvider<T> = NavDisplayTransitions.root(),
    popTransitionSpec: ContentTransformProvider<T> = transitionSpec,
    predictivePopTransitionSpec: PredictiveContentTransformProvider<T> =
        NavDisplayTransitions.predictive(provider = popTransitionSpec),
) {
    NavDisplay(
        backStack = navigationController.backStack,
        modifier = modifier,
        onBack = navigationController::navigateBack,
        entryDecorators = rememberBitwardenNavEntryDecorators(),
        transitionSpec = transitionSpec,
        popTransitionSpec = popTransitionSpec,
        predictivePopTransitionSpec = predictivePopTransitionSpec,
        entryProvider = entryProvider,
    )
}
