package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the generator graph.
 */
@Serializable
data object GeneratorGraphRoute

/**
 * Add generator destination to the root nav graph.
 */
fun NavGraphBuilder.generatorGraph(
    onNavigateToPasswordHistory: () -> Unit,
    onDimNavBarRequest: (Boolean) -> Unit,
) {
    navigation<GeneratorGraphRoute>(
        startDestination = GeneratorRoute.Standard,
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
    this.navigate(route = GeneratorGraphRoute, navOptions = navOptions)
}
