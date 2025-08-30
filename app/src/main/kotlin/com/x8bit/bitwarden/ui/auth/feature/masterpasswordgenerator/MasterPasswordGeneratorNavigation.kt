package com.x8bit.bitwarden.ui.auth.feature.masterpasswordgenerator

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the master password generator screen.
 */
@Serializable
data object MasterPasswordGeneratorRoute

/**
 * Navigate to master password generator  screen.
 */
fun NavController.navigateToMasterPasswordGenerator(navOptions: NavOptions? = null) {
    this.navigate(route = MasterPasswordGeneratorRoute, navOptions = navOptions)
}

/**
 * Add the master password generator screen to the nav graph.
 */
fun NavGraphBuilder.masterPasswordGeneratorDestination(
    onNavigateBack: () -> Unit,
    onNavigateToPreventLockout: () -> Unit,
    onNavigateBackWithPassword: () -> Unit,
) {
    composableWithSlideTransitions<MasterPasswordGeneratorRoute> {
        MasterPasswordGeneratorScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToPreventLockout = onNavigateToPreventLockout,
            onNavigateBackWithPassword = onNavigateBackWithPassword,
        )
    }
}
