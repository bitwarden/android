@file:OmitFromCoverage

package com.bitwarden.testharness.ui.platform.feature.createpassword

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * Create password test screen.
 */
@Serializable
data object CreatePasswordRoute

/**
 * Add Create Password destination to the nav graph.
 */
fun NavGraphBuilder.createPasswordDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions<CreatePasswordRoute> {
        CreatePasswordScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Create Password test screen.
 */
fun NavController.navigateToCreatePassword(navOptions: NavOptions? = null) {
    navigate(route = CreatePasswordRoute, navOptions = navOptions)
}
