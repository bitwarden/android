package com.x8bit.bitwarden.ui.auth.feature.masterpasswordhint

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val EMAIL_ADDRESS: String = "email_address"
private const val MASTER_PASSWORD_HINT_ROUTE: String = "master_password_hint/{$EMAIL_ADDRESS}"

/**
 * Class to retrieve login arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class MasterPasswordHintArgs(val emailAddress: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[EMAIL_ADDRESS]) as String,
    )
}

/**
 * Navigate to the master password hint screen.
 */
fun NavController.navigateToMasterPasswordHint(
    emailAddress: String,
    navOptions: NavOptions? = null,
) {
    this.navigate("master_password_hint/$emailAddress", navOptions)
}

/**
 * Add the master password hint screen to the nav graph.
 */
fun NavGraphBuilder.masterPasswordHintDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = MASTER_PASSWORD_HINT_ROUTE,
        arguments = listOf(
            navArgument(EMAIL_ADDRESS) { type = NavType.StringType },
        ),
    ) {
        MasterPasswordHintScreen(onNavigateBack = onNavigateBack)
    }
}
