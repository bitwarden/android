package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph
import com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.model.LoginWithDeviceType

/**
 * Navigate to the Login with Device screen.
 */
fun NavigationController<AuthGraph>.navigateToLoginWithDevice(
    emailAddress: String,
    loginType: LoginWithDeviceType,
) {
    this.navigate(
        route = AuthGraph.LoginWithDeviceRoute(
            emailAddress = emailAddress,
            loginType = loginType,
        ),
    )
}

/**
 * Add the Login with Device screen to the navigation entries.
 */
fun EntryProviderScope<AuthGraph>.loginWithDeviceDestination(
    onNavigateBack: () -> Unit,
    onNavigateToTwoFactorLogin: (emailAddress: String) -> Unit,
) {
    slideUpEntry<AuthGraph.LoginWithDeviceRoute> { route ->
        LoginWithDeviceScreen(
            viewModel = hiltViewModel<LoginWithDeviceViewModel, LoginWithDeviceViewModel.Factory>(
                creationCallback = { factory -> factory.create(route = route) },
            ),
            onNavigateBack = onNavigateBack,
            onNavigateToTwoFactorLogin = onNavigateToTwoFactorLogin,
        )
    }
}
