@file:OmitFromCoverage

package com.bitwarden.testharness.ui.platform.feature.autofill

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.bitwarden.annotation.OmitFromCoverage
import kotlinx.serialization.Serializable

/**
 * Autofill graph route - serves as the parent for all Autofill test flows.
 */
@Serializable
data object AutofillGraphRoute

/**
 * Autofill test placeholder screen - the start destination of the graph.
 */
@Serializable
data object AutofillPlaceholderRoute

/**
 * Add Autofill nav graph to the root nav graph.
 *
 * This graph contains the placeholder screen and can be expanded with
 * additional autofill test screens as needed.
 */
fun NavGraphBuilder.autofillGraph(
    onNavigateBack: () -> Unit,
) {
    navigation<AutofillGraphRoute>(
        startDestination = AutofillPlaceholderRoute,
    ) {
        composable<AutofillPlaceholderRoute> {
            AutofillPlaceholderScreen(
                onNavigateBack = onNavigateBack,
            )
        }
    }
}

/**
 * Navigate to the Autofill graph.
 */
fun NavController.navigateToAutofillGraph(navOptions: NavOptions? = null) {
    navigate(route = AutofillGraphRoute, navOptions = navOptions)
}
