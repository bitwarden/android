package com.x8bit.bitwarden.ui.platform.components.util

import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.Navigator
import androidx.navigation.compose.rememberNavController
import timber.log.Timber

/**
 * Creates a [NavHostController].
 */
@Composable
fun rememberBitwardenNavController(
    name: String,
    vararg navigators: Navigator<out NavDestination>,
): NavHostController =
    rememberNavController(navigators = navigators).apply {
        this.addOnDestinationChangedListener { _, destination, _ ->
            val graph = destination.parent?.route?.let { " in $it" }.orEmpty()
            Timber.d("$name destination changed: ${destination.route}$graph")
        }
    }
