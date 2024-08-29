package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val EMAIL_ADDRESS = "email_address"
private const val PASSWORD = "password"
private const val TWO_FACTOR_LOGIN_PREFIX = "two_factor_login"
private const val TWO_FACTOR_LOGIN_ROUTE =
    "$TWO_FACTOR_LOGIN_PREFIX/{${EMAIL_ADDRESS}}?$PASSWORD={$PASSWORD}"

/**
 * Class to retrieve Two-Factor Login arguments from the [SavedStateHandle].
 *
 * @property base64EncodedPassword Base64 URL encoded password input.
 */
@OmitFromCoverage
data class TwoFactorLoginArgs(val emailAddress: String, val base64EncodedPassword: String?) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        emailAddress = checkNotNull(savedStateHandle[EMAIL_ADDRESS]) as String,
        base64EncodedPassword = savedStateHandle[PASSWORD],
    )
}

/**
 * Navigate to the Two-Factor Login screen.
 */
fun NavController.navigateToTwoFactorLogin(
    emailAddress: String,
    base64EncodedPassword: String?,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = "$TWO_FACTOR_LOGIN_PREFIX/$emailAddress?$PASSWORD=$base64EncodedPassword",
        navOptions = navOptions,
    )
}

/**
 * Add the Two-Factor Login screen to the nav graph.
 */
fun NavGraphBuilder.twoFactorLoginDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = TWO_FACTOR_LOGIN_ROUTE,
    ) {
        TwoFactorLoginScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}
