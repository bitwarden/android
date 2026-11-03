package com.x8bit.bitwarden.ui.auth.feature.checkemail

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph

/**
 * Navigate to the check email screen.
 */
fun NavigationController<AuthGraph>.navigateToCheckEmail(emailAddress: String) {
    this.navigate(route = AuthGraph.CheckEmailRoute(emailAddress = emailAddress))
}

/**
 * Add the check email screen to the navigation entries.
 */
fun EntryProviderScope<AuthGraph>.checkEmailDestination(
    onNavigateBack: () -> Unit,
) {
    slideUpEntry<AuthGraph.CheckEmailRoute> { route ->
        CheckEmailScreen(
            viewModel = hiltViewModel<CheckEmailViewModel, CheckEmailViewModel.Factory>(
                creationCallback = { factory -> factory.create(route = route) },
            ),
            onNavigateBack = onNavigateBack,
        )
    }
}
