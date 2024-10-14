package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.loginWithDeviceDestination
import com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.model.LoginWithDeviceType
import com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.navigateToLoginWithDevice
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.navigateToTwoFactorLogin
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.twoFactorLoginDestination
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.navigateToTdeVaultUnlock
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.tdeVaultUnlockDestination

const val TRUSTED_DEVICE_GRAPH_ROUTE: String = "trusted_device_graph"

/**
 * Add trusted device destinations to the nav graph.
 */
fun NavGraphBuilder.trustedDeviceGraph(navController: NavHostController) {
    navigation(
        startDestination = TRUSTED_DEVICE_ROUTE,
        route = TRUSTED_DEVICE_GRAPH_ROUTE,
    ) {
        loginWithDeviceDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToTwoFactorLogin = {
                navController.navigateToTwoFactorLogin(
                    emailAddress = it,
                    password = null,
                    orgIdentifier = null,
                )
            },
        )
        trustedDeviceDestination(
            onNavigateToAdminApproval = {
                navController.navigateToLoginWithDevice(
                    emailAddress = it,
                    loginType = LoginWithDeviceType.SSO_ADMIN_APPROVAL,
                )
            },
            onNavigateToLoginWithOtherDevice = {
                navController.navigateToLoginWithDevice(
                    emailAddress = it,
                    loginType = LoginWithDeviceType.SSO_OTHER_DEVICE,
                )
            },
            onNavigateToLock = {
                navController.navigateToTdeVaultUnlock()
            },
        )
        tdeVaultUnlockDestination()
        twoFactorLoginDestination(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

/**
 * Navigate to the trusted device graph.
 */
fun NavController.navigateToTrustedDeviceGraph(
    navOptions: NavOptions? = null,
) {
    navigate(TRUSTED_DEVICE_GRAPH_ROUTE, navOptions)
}
