package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph

/**
 * Navigate to the enterprise single sign on screen.
 */
fun NavigationController<AuthGraph>.navigateToEnterpriseSignOn(
    emailAddress: String,
) {
    this.navigate(route = AuthGraph.EnterpriseSignOnRoute(emailAddress = emailAddress))
}

/**
 * Add the enterprise sign on screen to the navigation entries.
 */
fun EntryProviderScope<AuthGraph>.enterpriseSignOnDestination(
    onNavigateBack: () -> Unit,
    onNavigateToSetPassword: () -> Unit,
    onNavigateToTwoFactorLogin: (emailAddress: String, orgIdentifier: String) -> Unit,
) {
    slideUpEntry<AuthGraph.EnterpriseSignOnRoute> { route ->
        EnterpriseSignOnScreen(
            viewModel = hiltViewModel<EnterpriseSignOnViewModel, EnterpriseSignOnViewModel.Factory>(
                creationCallback = { factory -> factory.create(route = route) },
            ),
            onNavigateBack = onNavigateBack,
            onNavigateToSetPassword = onNavigateToSetPassword,
            onNavigateToTwoFactorLogin = onNavigateToTwoFactorLogin,
        )
    }
}
