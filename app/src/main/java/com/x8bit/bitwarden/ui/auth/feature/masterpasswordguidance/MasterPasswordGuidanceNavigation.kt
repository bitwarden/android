package com.x8bit.bitwarden.ui.auth.feature.masterpasswordguidance

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val MASTER_PASSWORD_GUIDANCE = "master_password_guidance"

/**
 * Navigate to the master password guidance screen.
 */
fun NavController.navigateToMasterPasswordGuidance(navOptions: NavOptions? = null) {
    this.navigate(MASTER_PASSWORD_GUIDANCE, navOptions)
}

/**
 * Add the master password guidance screen to the nav graph.
 */
fun NavGraphBuilder.masterPasswordGuidanceDestination(
    onNavigateBack: () -> Unit,
    onNavigateToGeneratePassword: () -> Unit,
) {
    composableWithSlideTransitions(
        route = MASTER_PASSWORD_GUIDANCE,
    ) {
        MasterPasswordGuidanceScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToGeneratePassword = onNavigateToGeneratePassword,
        )
    }
}
