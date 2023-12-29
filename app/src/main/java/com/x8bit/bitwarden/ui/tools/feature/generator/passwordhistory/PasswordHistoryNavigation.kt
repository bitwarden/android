package com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

/**
 * The functions below pertain to entry into the [PasswordHistoryScreen].
 */
private const val PASSWORD_HISTORY_ROUTE: String = "password_history"

/**
 * Add password history destination to the graph.
 */
fun NavGraphBuilder.passwordHistoryDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        // TODO: (BIT-617) Allow Password History screen to launch from VaultItemScreen
        route = PASSWORD_HISTORY_ROUTE,
    ) {
        PasswordHistoryScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Password History Screen.
 */
fun NavController.navigateToPasswordHistory(navOptions: NavOptions? = null) {
    navigate(PASSWORD_HISTORY_ROUTE, navOptions)
}
