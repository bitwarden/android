package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph

/**
 * Navigate to the Two-Factor Login screen.
 */
fun NavigationController<AuthGraph>.navigateToTwoFactorLogin(
    emailAddress: String,
    password: String?,
    orgIdentifier: String?,
    isNewDeviceVerification: Boolean = false,
) {
    this.navigate(
        route = AuthGraph.TwoFactorLoginRoute(
            emailAddress = emailAddress,
            password = password,
            orgIdentifier = orgIdentifier,
            isNewDeviceVerification = isNewDeviceVerification,
        ),
    )
}

/**
 * Add the Two-Factor Login screen to the navigation entries.
 */
fun EntryProviderScope<AuthGraph>.twoFactorLoginDestination(
    onNavigateBack: () -> Unit,
) {
    slideUpEntry<AuthGraph.TwoFactorLoginRoute> { route ->
        TwoFactorLoginScreen(
            viewModel = hiltViewModel<TwoFactorLoginViewModel, TwoFactorLoginViewModel.Factory>(
                creationCallback = { factory -> factory.create(route = route) },
            ),
            onNavigateBack = onNavigateBack,
        )
    }
}
