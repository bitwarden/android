package com.x8bit.bitwarden.ui.auth.feature.login

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph

/**
 * Navigate to the login screen with the given email address and region label.
 */
fun NavigationController<AuthGraph>.navigateToLogin(
    emailAddress: String,
) {
    this.navigate(route = AuthGraph.LoginRoute(emailAddress = emailAddress))
}

/**
 * Add the Login screen to the navigation entries.
 */
fun EntryProviderScope<AuthGraph>.loginDestination(
    onNavigateBack: () -> Unit,
    onNavigateToMasterPasswordHint: (emailAddress: String) -> Unit,
    onNavigateToEnterpriseSignOn: (emailAddress: String) -> Unit,
    onNavigateToLoginWithDevice: (emailAddress: String) -> Unit,
    onNavigateToTwoFactorLogin: (
        emailAddress: String,
        password: String?,
        isNewDeviceVerification: Boolean,
    ) -> Unit,
) {
    slideUpEntry<AuthGraph.LoginRoute> { route ->
        LoginScreen(
            viewModel = hiltViewModel<LoginViewModel, LoginViewModel.Factory>(
                creationCallback = { factory -> factory.create(route = route) },
            ),
            onNavigateBack = onNavigateBack,
            onNavigateToMasterPasswordHint = onNavigateToMasterPasswordHint,
            onNavigateToEnterpriseSignOn = onNavigateToEnterpriseSignOn,
            onNavigateToLoginWithDevice = onNavigateToLoginWithDevice,
            onNavigateToTwoFactorLogin = onNavigateToTwoFactorLogin,
        )
    }
}
