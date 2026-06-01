package com.x8bit.bitwarden.ui.auth.feature.masterpasswordhint

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the master password hint screen.
 */
@Serializable
data class MasterPasswordHintRoute(
    val emailAddress: String,
)

/**
 * Class to retrieve login arguments from the [SavedStateHandle].
 */
data class MasterPasswordHintArgs(val emailAddress: String)

/**
 * Constructs a [MasterPasswordHintArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toMasterPasswordHintArgs(): MasterPasswordHintArgs {
    val route = this.toRoute<MasterPasswordHintRoute>()
    return MasterPasswordHintArgs(emailAddress = route.emailAddress)
}

/**
 * Navigate to the master password hint screen.
 */
fun NavController.navigateToMasterPasswordHint(
    emailAddress: String,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = MasterPasswordHintRoute(emailAddress = emailAddress),
        navOptions = navOptions,
    )
}

/**
 * Add the master password hint screen to the nav graph.
 */
fun NavGraphBuilder.masterPasswordHintDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<MasterPasswordHintRoute> {
        MasterPasswordHintScreen(onNavigateBack = onNavigateBack)
    }
}
