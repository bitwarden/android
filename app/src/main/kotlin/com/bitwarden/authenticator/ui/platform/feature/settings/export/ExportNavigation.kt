package com.bitwarden.authenticator.ui.platform.feature.settings.export

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.authenticator.ui.platform.base.util.composableWithSlideTransitions

const val EXPORT_ROUTE = "export"

fun NavGraphBuilder.exportDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(EXPORT_ROUTE) {
        ExportScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

fun NavController.navigateToExport(navOptions: NavOptions? = null) {
    navigate(EXPORT_ROUTE, navOptions)
}
