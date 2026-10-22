package com.x8bit.bitwarden.ui.auth.feature.completeregistration

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.popUpTo
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph

/**
 * Navigate to the complete registration screen.
 */
fun NavigationController<AuthGraph>.navigateToCompleteRegistration(
    emailAddress: String,
    verificationToken: String,
    fromEmail: Boolean,
) {
    this.navigate(
        route = AuthGraph.CompleteRegistrationRoute(
            emailAddress = emailAddress,
            verificationToken = verificationToken,
            fromEmail = fromEmail,
        ),
    )
}

/**
 * Add the complete registration screen to the navigation entries.
 */
fun EntryProviderScope<AuthGraph>.completeRegistrationDestination(
    onNavigateBack: () -> Unit,
    onNavigateToPasswordGuidance: () -> Unit,
    onNavigateToPreventAccountLockout: () -> Unit,
    onNavigateToLogin: (email: String) -> Unit,
) {
    slideUpEntry<AuthGraph.CompleteRegistrationRoute> { route ->
        CompleteRegistrationScreen(
            viewModel = completeRegistrationViewModel(route = route),
            onNavigateBack = onNavigateBack,
            onNavigateToPasswordGuidance = onNavigateToPasswordGuidance,
            onNavigateToPreventAccountLockout = onNavigateToPreventAccountLockout,
            onNavigateToLogin = onNavigateToLogin,
        )
    }
}

/**
 * Pop up to the complete registration screen.
 */
fun NavigationController<AuthGraph>.popUpToCompleteRegistration() {
    this.popUpTo<AuthGraph.CompleteRegistrationRoute>()
}

@Composable
private fun completeRegistrationViewModel(
    route: AuthGraph.CompleteRegistrationRoute,
): CompleteRegistrationViewModel =
    hiltViewModel<CompleteRegistrationViewModel, CompleteRegistrationViewModel.Factory>(
        creationCallback = { factory -> factory.create(route = route) },
    )
