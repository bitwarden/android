@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.bitwarden.core.annotation.OmitFromCoverage

const val GENERATOR_GRAPH_ROUTE: String = "generator_graph"

/**
 * Add generator destination to the root nav graph.
 */
fun NavGraphBuilder.generatorGraph(
    onNavigateToPasswordHistory: () -> Unit,
    onDimNavBarRequest: (Boolean) -> Unit,
) {
    navigation(
        route = GENERATOR_GRAPH_ROUTE,
        startDestination = GENERATOR_ROUTE,
    ) {
        generatorDestination(
            onNavigateToPasswordHistory = onNavigateToPasswordHistory,
            onDimNavBarRequest = onDimNavBarRequest,
        )
    }
}

/**
 * Navigate to the generator graph.
 */
fun NavController.navigateToGeneratorGraph(navOptions: NavOptions? = null) {
    navigate(GENERATOR_GRAPH_ROUTE, navOptions)
}
