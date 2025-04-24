@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.bitwarden.core.annotation.OmitFromCoverage
import com.bitwarden.network.util.base64UrlDecodeOrNull
import com.bitwarden.network.util.base64UrlEncode
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val EMAIL_ADDRESS = "email_address"
private const val PASSWORD = "password"
private const val ORG_IDENTIFIER = "org_identifier"
private const val TWO_FACTOR_LOGIN_PREFIX = "two_factor_login"
private const val NEW_DEVICE_VERIFICATION = "new_device_verification"
private const val TWO_FACTOR_LOGIN_ROUTE =
    "$TWO_FACTOR_LOGIN_PREFIX/{$EMAIL_ADDRESS}?" +
        "$PASSWORD={$PASSWORD}&" +
        "$ORG_IDENTIFIER={$ORG_IDENTIFIER}&" +
        "$NEW_DEVICE_VERIFICATION={$NEW_DEVICE_VERIFICATION}"

/**
 * Class to retrieve Two-Factor Login arguments from the [SavedStateHandle].
 */
data class TwoFactorLoginArgs(
    val emailAddress: String,
    val password: String?,
    val orgIdentifier: String?,
    val isNewDeviceVerification: Boolean,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        emailAddress = checkNotNull(savedStateHandle[EMAIL_ADDRESS]) as String,
        password = savedStateHandle.get<String>(PASSWORD)?.base64UrlDecodeOrNull(),
        orgIdentifier = savedStateHandle.get<String>(ORG_IDENTIFIER)?.base64UrlDecodeOrNull(),
        isNewDeviceVerification = savedStateHandle.get<Boolean>(NEW_DEVICE_VERIFICATION) ?: false,
    )
}

/**
 * Navigate to the Two-Factor Login screen.
 */
fun NavController.navigateToTwoFactorLogin(
    emailAddress: String,
    password: String?,
    orgIdentifier: String?,
    navOptions: NavOptions? = null,
    isNewDeviceVerification: Boolean = false,
) {
    this.navigate(
        route = "$TWO_FACTOR_LOGIN_PREFIX/$emailAddress?" +
            "$PASSWORD=${password?.base64UrlEncode()}&" +
            "$ORG_IDENTIFIER=${orgIdentifier?.base64UrlEncode()}&" +
            "$NEW_DEVICE_VERIFICATION=$isNewDeviceVerification",
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
        arguments = listOf(
            navArgument(EMAIL_ADDRESS) { type = NavType.StringType },
            navArgument(PASSWORD) {
                type = NavType.StringType
                nullable = true
            },
            navArgument(ORG_IDENTIFIER) {
                type = NavType.StringType
                nullable = true
            },
            navArgument(NEW_DEVICE_VERIFICATION) {
                type = NavType.BoolType
            },
        ),
    ) {
        TwoFactorLoginScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}
