@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.verificationcode

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.core.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs

private const val VERIFICATION_CODE_ROUTE: String = "verification_code"

/**
 * Add the verification code screen to the nav graph.
 */
fun NavGraphBuilder.vaultVerificationCodeDestination(
    onNavigateBack: () -> Unit,
    onNavigateToSearchVault: () -> Unit,
    onNavigateToVaultItemScreen: (args: VaultItemArgs) -> Unit,
) {
    composableWithPushTransitions(
        route = VERIFICATION_CODE_ROUTE,
    ) {
        VerificationCodeScreen(
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
            onNavigateToSearch = onNavigateToSearchVault,
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the verification code screen.
 */
fun NavController.navigateToVerificationCodeScreen(
    navOptions: NavOptions? = null,
) {
    this.navigate(VERIFICATION_CODE_ROUTE, navOptions)
}
