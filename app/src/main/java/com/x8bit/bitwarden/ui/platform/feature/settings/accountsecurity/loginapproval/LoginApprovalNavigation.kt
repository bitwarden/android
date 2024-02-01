package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val FINGERPRINT: String = "fingerprint"
private const val LOGIN_APPROVAL_PREFIX = "login_approval"
private const val LOGIN_APPROVAL_ROUTE = "$LOGIN_APPROVAL_PREFIX?$FINGERPRINT={$FINGERPRINT}"

/**
 * Class to retrieve login approval arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class LoginApprovalArgs(val fingerprint: String?) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        fingerprint = savedStateHandle.get<String>(FINGERPRINT),
    )
}

/**
 * Add login approval destinations to the nav graph.
 */
fun NavGraphBuilder.loginApprovalDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = LOGIN_APPROVAL_ROUTE,
        arguments = listOf(
            navArgument(FINGERPRINT) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) {
        LoginApprovalScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Login Approval screen.
 */
fun NavController.navigateToLoginApproval(
    fingerprint: String?,
    navOptions: NavOptions? = null,
) {
    navigate("$LOGIN_APPROVAL_PREFIX?$FINGERPRINT=$fingerprint", navOptions)
}
