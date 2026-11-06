package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the login approval screen to the navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.loginApprovalDestination(
    onNavigateBack: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.LoginApprovalRoute> { route ->
        LoginApprovalScreen(
            viewModel = hiltViewModel<LoginApprovalViewModel, LoginApprovalViewModel.Factory>(
                creationCallback = { factory -> factory.create(route = route) },
            ),
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Login Approval screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToLoginApproval(fingerprint: String?) {
    this.navigate(route = VaultUnlockedGraph.LoginApprovalRoute(fingerprint = fingerprint))
}
