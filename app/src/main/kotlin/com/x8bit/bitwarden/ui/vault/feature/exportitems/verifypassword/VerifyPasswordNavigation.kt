@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry
import com.x8bit.bitwarden.ui.vault.feature.exportitems.ExportItemsGraph

/**
 * Add the [VerifyPasswordScreen] to the navigation entries.
 */
fun EntryProviderScope<ExportItemsGraph>.verifyPasswordDestination(
    onNavigateBack: () -> Unit,
    onPasswordVerified: (userId: String) -> Unit,
) {
    pushEntry<ExportItemsGraph.VerifyPasswordRoute> { route ->
        VerifyPasswordScreen(
            viewModel = hiltViewModel<VerifyPasswordViewModel, VerifyPasswordViewModel.Factory>(
                creationCallback = { factory -> factory.create(route = route) },
            ),
            onNavigateBack = onNavigateBack,
            onPasswordVerified = onPasswordVerified,
        )
    }
}

/**
 * Navigate to the [VerifyPasswordScreen].
 */
fun NavigationController<ExportItemsGraph>.navigateToVerifyPassword(
    userId: String,
    hasOtherAccounts: Boolean,
) {
    this.navigate(
        route = ExportItemsGraph.VerifyPasswordRoute(
            userId = userId,
            hasOtherAccounts = hasOtherAccounts,
        ),
    )
}
