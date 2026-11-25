package com.x8bit.bitwarden.ui.auth.feature.masterpasswordhint

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph

/**
 * Navigate to the master password hint screen.
 */
fun NavigationController<AuthGraph>.navigateToMasterPasswordHint(
    emailAddress: String,
) {
    this.navigate(route = AuthGraph.MasterPasswordHintRoute(emailAddress = emailAddress))
}

/**
 * Add the master password hint screen to the navigation entries.
 */
fun EntryProviderScope<AuthGraph>.masterPasswordHintDestination(
    onNavigateBack: () -> Unit,
) {
    slideUpEntry<AuthGraph.MasterPasswordHintRoute> { route ->
        MasterPasswordHintScreen(
            viewModel = masterPasswordHintViewModel(route = route),
            onNavigateBack = onNavigateBack,
        )
    }
}

@Composable
private fun masterPasswordHintViewModel(
    route: AuthGraph.MasterPasswordHintRoute,
): MasterPasswordHintViewModel =
    hiltViewModel<MasterPasswordHintViewModel, MasterPasswordHintViewModel.Factory>(
        creationCallback = { factory -> factory.create(route = route) },
    )
