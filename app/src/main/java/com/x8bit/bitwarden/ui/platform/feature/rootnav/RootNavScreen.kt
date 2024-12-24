package com.x8bit.bitwarden.ui.platform.feature.rootnav

import android.app.Activity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.SETUP_AUTO_FILL_AS_ROOT_ROUTE
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.SETUP_COMPLETE_ROUTE
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.SETUP_UNLOCK_AS_ROOT_ROUTE
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupAutoFillAsRootScreen
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupCompleteScreen
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupUnlockScreenAsRoot
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.setupAutoFillDestinationAsRoot
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.setupCompleteDestination
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.setupUnlockDestinationAsRoot
import com.x8bit.bitwarden.ui.auth.feature.auth.AUTH_GRAPH_ROUTE
import com.x8bit.bitwarden.ui.auth.feature.auth.authGraph
import com.x8bit.bitwarden.ui.auth.feature.auth.navigateToAuthGraph
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.navigateToCompleteRegistration
import com.x8bit.bitwarden.ui.auth.feature.expiredregistrationlink.navigateToExpiredRegistrationLinkScreen
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.navigateToNewDeviceNoticeEmailAccess
import com.x8bit.bitwarden.ui.auth.feature.removepassword.REMOVE_PASSWORD_ROUTE
import com.x8bit.bitwarden.ui.auth.feature.removepassword.navigateToRemovePassword
import com.x8bit.bitwarden.ui.auth.feature.removepassword.removePasswordDestination
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.RESET_PASSWORD_ROUTE
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.navigateToResetPasswordGraph
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.resetPasswordDestination
import com.x8bit.bitwarden.ui.auth.feature.setpassword.SET_PASSWORD_ROUTE
import com.x8bit.bitwarden.ui.auth.feature.setpassword.navigateToSetPassword
import com.x8bit.bitwarden.ui.auth.feature.trusteddevice.TRUSTED_DEVICE_GRAPH_ROUTE
import com.x8bit.bitwarden.ui.auth.feature.trusteddevice.navigateToTrustedDeviceGraph
import com.x8bit.bitwarden.ui.auth.feature.trusteddevice.trustedDeviceGraph
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.VAULT_UNLOCK_ROUTE
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.navigateToVaultUnlock
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.vaultUnlockDestination
import com.x8bit.bitwarden.ui.auth.feature.welcome.navigateToWelcome
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.setupDebugMenuDestination
import com.x8bit.bitwarden.ui.platform.feature.rootnav.util.toVaultItemListingType
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval.navigateToLoginApproval
import com.x8bit.bitwarden.ui.platform.feature.splash.SPLASH_ROUTE
import com.x8bit.bitwarden.ui.platform.feature.splash.navigateToSplash
import com.x8bit.bitwarden.ui.platform.feature.splash.splashDestination
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VAULT_UNLOCKED_GRAPH_ROUTE
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.navigateToVaultUnlockedGraph
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.vaultUnlockedGraph
import com.x8bit.bitwarden.ui.platform.theme.NonNullEnterTransitionProvider
import com.x8bit.bitwarden.ui.platform.theme.NonNullExitTransitionProvider
import com.x8bit.bitwarden.ui.platform.theme.RootTransitionProviders
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.model.AddSendType
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.navigateToAddSend
import com.x8bit.bitwarden.ui.vault.feature.addedit.navigateToVaultAddEdit
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.navigateToVaultItemListingAsRoot
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import java.util.concurrent.atomic.AtomicReference

/**
 * Controls root level [NavHost] for the app.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun RootNavScreen(
    viewModel: RootNavViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    onSplashScreenRemoved: () -> Unit = {},
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val previousStateReference = remember { AtomicReference(state) }

    val isNotSplashScreen = state != RootNavState.Splash
    LaunchedEffect(isNotSplashScreen) {
        if (isNotSplashScreen) onSplashScreenRemoved()
    }

    NavHost(
        navController = navController,
        startDestination = SPLASH_ROUTE,
        enterTransition = { toEnterTransition()(this) },
        exitTransition = { toExitTransition()(this) },
        popEnterTransition = { toEnterTransition()(this) },
        popExitTransition = { toExitTransition()(this) },
    ) {
        splashDestination()
        authGraph(navController)
        removePasswordDestination()
        resetPasswordDestination()
        trustedDeviceGraph(navController)
        vaultUnlockDestination()
        vaultUnlockedGraph(navController)
        setupDebugMenuDestination(onNavigateBack = { navController.popBackStack() })
        setupUnlockDestinationAsRoot()
        setupAutoFillDestinationAsRoot()
        setupCompleteDestination()
    }

    val targetRoute = when (state) {
        RootNavState.Auth,
        is RootNavState.CompleteOngoingRegistration,
        RootNavState.AuthWithWelcome,
        RootNavState.ExpiredRegistrationLink,
            -> AUTH_GRAPH_ROUTE

        RootNavState.ResetPassword -> RESET_PASSWORD_ROUTE
        RootNavState.SetPassword -> SET_PASSWORD_ROUTE
        RootNavState.RemovePassword -> REMOVE_PASSWORD_ROUTE
        RootNavState.Splash -> SPLASH_ROUTE
        RootNavState.TrustedDevice -> TRUSTED_DEVICE_GRAPH_ROUTE
        RootNavState.VaultLocked -> VAULT_UNLOCK_ROUTE
        is RootNavState.VaultUnlocked,
        is RootNavState.VaultUnlockedForAutofillSave,
        is RootNavState.VaultUnlockedForAutofillSelection,
        is RootNavState.VaultUnlockedForNewSend,
        is RootNavState.VaultUnlockedForNewTotp,
        is RootNavState.VaultUnlockedForAuthRequest,
        is RootNavState.VaultUnlockedForFido2Save,
        is RootNavState.VaultUnlockedForFido2Assertion,
        is RootNavState.VaultUnlockedForFido2GetCredentials,
        is RootNavState.NewDeviceTwoFactorNotice,
            -> VAULT_UNLOCKED_GRAPH_ROUTE

        RootNavState.OnboardingAccountLockSetup -> SETUP_UNLOCK_AS_ROOT_ROUTE
        RootNavState.OnboardingAutoFillSetup -> SETUP_AUTO_FILL_AS_ROOT_ROUTE
        RootNavState.OnboardingStepsComplete -> SETUP_COMPLETE_ROUTE
    }
    val currentRoute = navController.currentDestination?.rootLevelRoute()

    // Don't navigate if we are already at the correct root. This notably happens during process
    // death. In this case, the NavHost already restores state, so we don't have to navigate.
    // However, if the route is correct but the underlying state is different, we should still
    // proceed in order to get a fresh version of that route.
    if (currentRoute == targetRoute && previousStateReference.get() == state) {
        previousStateReference.set(state)
        return
    }
    previousStateReference.set(state)

    // In some scenarios on an emulator the Activity can leak when recreated
    // if we don't first clear focus anytime we change the root destination.
    (LocalContext.current as? Activity)?.currentFocus?.clearFocus()

    // When state changes, navigate to different root navigation state
    val rootNavOptions = navOptions {
        // When changing root navigation state, pop everything else off the back stack:
        popUpTo(navController.graph.id) {
            inclusive = false
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }

    // Use a LaunchedEffect to ensure we don't navigate too soon when the app first opens. This
    // avoids a bug that first appeared in Compose Material3 1.2.0-rc01 that causes the initial
    // transition to appear corrupted.
    LaunchedEffect(state) {
        when (val currentState = state) {
            RootNavState.Auth -> navController.navigateToAuthGraph(rootNavOptions)
            RootNavState.AuthWithWelcome -> navController.navigateToWelcome(rootNavOptions)
            is RootNavState.CompleteOngoingRegistration -> {
                navController.navigateToAuthGraph(rootNavOptions)
                navController.navigateToCompleteRegistration(
                    emailAddress = currentState.email,
                    verificationToken = currentState.verificationToken,
                    fromEmail = currentState.fromEmail,
                )
            }

            RootNavState.ExpiredRegistrationLink -> {
                navController.navigateToAuthGraph(rootNavOptions)
                navController.navigateToExpiredRegistrationLinkScreen()
            }

            RootNavState.RemovePassword -> navController.navigateToRemovePassword(rootNavOptions)
            RootNavState.ResetPassword -> navController.navigateToResetPasswordGraph(rootNavOptions)
            RootNavState.SetPassword -> navController.navigateToSetPassword(rootNavOptions)
            RootNavState.Splash -> navController.navigateToSplash(rootNavOptions)
            RootNavState.TrustedDevice -> navController.navigateToTrustedDeviceGraph(rootNavOptions)
            RootNavState.VaultLocked -> navController.navigateToVaultUnlock(rootNavOptions)
            is RootNavState.VaultUnlocked -> navController.navigateToVaultUnlockedGraph(
                navOptions = rootNavOptions,
            )

            RootNavState.VaultUnlockedForNewSend -> {
                navController.navigateToVaultUnlock(rootNavOptions)
                navController.navigateToAddSend(
                    sendAddType = AddSendType.AddItem,
                    navOptions = rootNavOptions,
                )
            }

            is RootNavState.VaultUnlockedForNewTotp -> {
                navController.navigateToVaultUnlock(rootNavOptions)
                navController.navigateToVaultItemListingAsRoot(
                    vaultItemListingType = VaultItemListingType.Login,
                    navOptions = rootNavOptions,
                )
            }

            is RootNavState.VaultUnlockedForAutofillSave -> {
                navController.navigateToVaultUnlockedGraph(rootNavOptions)
                navController.navigateToVaultAddEdit(
                    vaultAddEditType = VaultAddEditType.AddItem(
                        vaultItemCipherType = VaultItemCipherType.LOGIN,
                    ),
                    navOptions = rootNavOptions,
                )
            }

            is RootNavState.VaultUnlockedForAutofillSelection -> {
                navController.navigateToVaultUnlockedGraph(rootNavOptions)
                navController.navigateToVaultItemListingAsRoot(
                    vaultItemListingType = currentState.type.toVaultItemListingType(),
                    navOptions = rootNavOptions,
                )
            }

            RootNavState.VaultUnlockedForAuthRequest -> {
                navController.navigateToVaultUnlockedGraph(rootNavOptions)
                navController.navigateToLoginApproval(
                    fingerprint = null,
                    navOptions = rootNavOptions,
                )
            }

            is RootNavState.VaultUnlockedForFido2Save,
            is RootNavState.VaultUnlockedForFido2Assertion,
            is RootNavState.VaultUnlockedForFido2GetCredentials,
                -> {
                navController.navigateToVaultUnlockedGraph(rootNavOptions)
                navController.navigateToVaultItemListingAsRoot(
                    vaultItemListingType = VaultItemListingType.Login,
                    navOptions = rootNavOptions,
                )
            }

            RootNavState.OnboardingAccountLockSetup -> {
                navController.navigateToSetupUnlockScreenAsRoot(rootNavOptions)
            }

            RootNavState.OnboardingAutoFillSetup -> {
                navController.navigateToSetupAutoFillAsRootScreen(rootNavOptions)
            }

            RootNavState.OnboardingStepsComplete -> {
                navController.navigateToSetupCompleteScreen(rootNavOptions)
            }

            is RootNavState.NewDeviceTwoFactorNotice -> {
                navController.navigateToNewDeviceNoticeEmailAccess(
                    emailAddress = currentState.email,
                    navOptions = rootNavOptions,
                )
            }
        }
    }
}

/**
 * Helper method that returns the highest level route for the given [NavDestination].
 *
 * As noted above, this can be removed after upgrading to latest compose navigation, since
 * the nav args can prevent us from having to do this check.
 */
private fun NavDestination?.rootLevelRoute(): String? {
    if (this == null) {
        return null
    }
    if (parent?.route == null) {
        return route
    }
    return parent.rootLevelRoute()
}

/**
 * Define the enter transition for each route.
 */
@Suppress("MaxLineLength")
private fun AnimatedContentTransitionScope<NavBackStackEntry>.toEnterTransition(): NonNullEnterTransitionProvider =
    when (targetState.destination.rootLevelRoute()) {
        RESET_PASSWORD_ROUTE -> RootTransitionProviders.Enter.slideUp
        else -> when (initialState.destination.rootLevelRoute()) {
            // Disable transitions when coming from the splash screen
            SPLASH_ROUTE -> RootTransitionProviders.Enter.none
            // The RESET_PASSWORD_ROUTE animation should be stay but due to an issue when combining
            // certain animations, we are just using a fadeIn instead.
            RESET_PASSWORD_ROUTE -> RootTransitionProviders.Enter.fadeIn
            else -> RootTransitionProviders.Enter.fadeIn
        }
    }

/**
 * Define the exit transition for each route.
 */
@Suppress("MaxLineLength")
private fun AnimatedContentTransitionScope<NavBackStackEntry>.toExitTransition(): NonNullExitTransitionProvider =
    when (initialState.destination.rootLevelRoute()) {
        // Disable transitions when coming from the splash screen
        SPLASH_ROUTE -> RootTransitionProviders.Exit.none
        RESET_PASSWORD_ROUTE -> RootTransitionProviders.Exit.slideDown
        else -> when (targetState.destination.rootLevelRoute()) {
            RESET_PASSWORD_ROUTE -> RootTransitionProviders.Exit.stay
            else -> RootTransitionProviders.Exit.fadeOut
        }
    }
