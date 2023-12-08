package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

/**
 * The functions below pertain to entry into the [GeneratorScreen].
 */
const val GENERATOR_ROUTE: String = "generator"

/**
 * Navigate to the [GeneratorScreen].
 */
fun NavController.navigateToGenerator(navOptions: NavOptions? = null) {
    navigate(GENERATOR_ROUTE, navOptions)
}

/**
 * Add generator destination to the root nav graph.
 */
fun NavGraphBuilder.generatorDestination(
    onNavigateToPasswordHistory: () -> Unit,
) {
    composable(GENERATOR_ROUTE) {
        GeneratorScreen(
            onNavigateToPasswordHistory = onNavigateToPasswordHistory,
        )
    }
}
