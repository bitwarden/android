package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the two-factor login screen.
 */
@Serializable
data class TwoFactorLoginRoute(
    val emailAddress: String,
    val password: String?,
    val orgIdentifier: String?,
    val isNewDeviceVerification: Boolean,
)

/**
 * Class to retrieve Two-Factor Login arguments from the [SavedStateHandle].
 */
data class TwoFactorLoginArgs(
    val emailAddress: String,
    val password: String?,
    val orgIdentifier: String?,
    val isNewDeviceVerification: Boolean,
)

/**
 * Constructs a [TwoFactorLoginArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toTwoFactorLoginArgs(): TwoFactorLoginArgs {
    val route = this.toRoute<TwoFactorLoginRoute>()
    return TwoFactorLoginArgs(
        emailAddress = route.emailAddress,
        password = route.password,
        orgIdentifier = route.orgIdentifier,
        isNewDeviceVerification = route.isNewDeviceVerification,
    )
}

/**
 * Navigate to the Two-Factor Login screen.
 */
fun NavController.navigateToTwoFactorLogin(
    emailAddress: String,
    password: String?,
    orgIdentifier: String?,
    isNewDeviceVerification: Boolean = false,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = TwoFactorLoginRoute(
            emailAddress = emailAddress,
            password = password,
            orgIdentifier = orgIdentifier,
            isNewDeviceVerification = isNewDeviceVerification,
        ),
        navOptions = navOptions,
    )
}

/**
 * Add the Two-Factor Login screen to the nav graph.
 */
fun NavGraphBuilder.twoFactorLoginDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<TwoFactorLoginRoute> {
        TwoFactorLoginScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}
