package com.x8bit.bitwarden.ui.auth.feature.masterpasswordgenerator

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val MASTER_PASSWORD_GENERATOR = "master_password_generator"

/**
 * Navigate to master password generator  screen.
 */
fun NavController.navigateToMasterPasswordGenerator(navOptions: NavOptions? = null) {
    this.navigate(MASTER_PASSWORD_GENERATOR, navOptions)
}

/**
 * Add the master password generator screen to the nav graph.
 */
fun NavGraphBuilder.masterPasswordGeneratorDestination(
    onNavigateBack: () -> Unit,
    onNavigateToPreventLockout: () -> Unit,
    onNavigateBackWithPassword: () -> Unit,
) {
    composableWithSlideTransitions(
        route = MASTER_PASSWORD_GENERATOR,
    ) {
        MasterPasswordGeneratorScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToPreventLockout = onNavigateToPreventLockout,
            onNavigateBackWithPassword = onNavigateBackWithPassword,
        )
    }
}
