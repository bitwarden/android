package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val ENTERPRISE_SIGN_ON_PREFIX = "enterprise_sign_on "
private const val EMAIL_ADDRESS: String = "email_address"
private const val ENTERPRISE_SIGN_ON_ROUTE = "$ENTERPRISE_SIGN_ON_PREFIX/{$EMAIL_ADDRESS}"

/**
 * Class to retrieve login arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class EnterpriseSignOnArgs(val emailAddress: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[EMAIL_ADDRESS]) as String,
    )
}

/**
 * Navigate to the enterprise single sign on screen.
 */
fun NavController.navigateToEnterpriseSignOn(
    emailAddress: String,
    navOptions: NavOptions? = null,
) {
    this.navigate("$ENTERPRISE_SIGN_ON_PREFIX/$emailAddress", navOptions)
}

/**
 * Add the enterprise sign on screen to the nav graph.
 */
fun NavGraphBuilder.enterpriseSignOnDestination(
    onNavigateBack: () -> Unit,
    onNavigateToSetPassword: () -> Unit,
    onNavigateToTwoFactorLogin: (emailAddress: String, orgIdentifier: String) -> Unit,
) {
    composableWithSlideTransitions(
        route = ENTERPRISE_SIGN_ON_ROUTE,
        arguments = listOf(
            navArgument(EMAIL_ADDRESS) { type = NavType.StringType },
        ),
    ) {
        EnterpriseSignOnScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToSetPassword = onNavigateToSetPassword,
            onNavigateToTwoFactorLogin = onNavigateToTwoFactorLogin,
        )
    }
}
