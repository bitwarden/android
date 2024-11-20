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
private const val REQUEST_ID: String = "requestId"
private const val LOGIN_APPROVAL_PREFIX = "login_approval"
private const val LOGIN_APPROVAL_ROUTE =
    "$LOGIN_APPROVAL_PREFIX?$FINGERPRINT={$FINGERPRINT}&$REQUEST_ID={$REQUEST_ID}"

/**
 * Class to retrieve login approval arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class LoginApprovalArgs(val fingerprint: String?, val requestId: String?) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        fingerprint = savedStateHandle.get<String>(FINGERPRINT),
        requestId = savedStateHandle.get<String>(REQUEST_ID),
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
            navArgument(REQUEST_ID) {
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
    requestId: String? = null,
    navOptions: NavOptions? = null,
) {
    navigate("$LOGIN_APPROVAL_PREFIX?$FINGERPRINT=$fingerprint&$REQUEST_ID=$requestId", navOptions)
}
