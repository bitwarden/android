package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.navOptions
import com.bitwarden.ui.platform.theme.NonNullEnterTransitionProvider
import com.bitwarden.ui.platform.theme.NonNullExitTransitionProvider
import com.bitwarden.ui.platform.theme.RootTransitionProviders
import com.bitwarden.ui.platform.util.toObjectNavigationRoute
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.SetupAutofillRoute
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.SetupBrowserAutofillRoute
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.SetupCompleteRoute
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.SetupUnlockRoute
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupAutoFillAsRootScreen
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupBrowserAutoFillAsRootScreen
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupCompleteScreen
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupUnlockScreenAsRoot
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.setupAutoFillDestinationAsRoot
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.setupBrowserAutofillDestinationAsRoot
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.setupCompleteDestination
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.setupUnlockDestinationAsRoot
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraphRoute
import com.x8bit.bitwarden.ui.auth.feature.auth.authGraph
import com.x8bit.bitwarden.ui.auth.feature.auth.navigateToAuthGraph
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.navigateToCompleteRegistration
import com.x8bit.bitwarden.ui.auth.feature.expiredregistrationlink.navigateToExpiredRegistrationLinkScreen
import com.x8bit.bitwarden.ui.auth.feature.preventaccountlockout.navigateToPreventAccountLockout
import com.x8bit.bitwarden.ui.auth.feature.removepassword.RemovePasswordRoute
import com.x8bit.bitwarden.ui.auth.feature.removepassword.navigateToRemovePassword
import com.x8bit.bitwarden.ui.auth.feature.removepassword.removePasswordDestination
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.ResetPasswordRoute
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.navigateToResetPasswordScreen
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.resetPasswordDestination
import com.x8bit.bitwarden.ui.auth.feature.setpassword.SetPasswordRoute
import com.x8bit.bitwarden.ui.auth.feature.setpassword.navigateToSetPassword
import com.x8bit.bitwarden.ui.auth.feature.trusteddevice.TrustedDeviceGraphRoute
import com.x8bit.bitwarden.ui.auth.feature.trusteddevice.navigateToTrustedDeviceGraph
import com.x8bit.bitwarden.ui.auth.feature.trusteddevice.trustedDeviceGraph
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.VaultUnlockRoute
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.navigateToVaultUnlock
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.vaultUnlockDestination
import com.x8bit.bitwarden.ui.auth.feature.welcome.navigateToWelcome
import com.x8bit.bitwarden.ui.platform.components.util.rememberBitwardenNavController
import com.x8bit.bitwarden.ui.platform.feature.rootnav.util.toVaultItemListingType
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval.navigateToLoginApproval
import com.x8bit.bitwarden.ui.platform.feature.splash.SplashRoute
import com.x8bit.bitwarden.ui.platform.feature.splash.navigateToSplash
import com.x8bit.bitwarden.ui.platform.feature.splash.splashDestination
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraphRoute
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.navigateToVaultUnlockedGraph
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.vaultUnlockedGraph
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendRoute
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.ModeType
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.navigateToAddEditSend
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.addedit.navigateToVaultAddEdit
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toVaultItemCipherType
import com.x8bit.bitwarden.ui.vault.feature.exportitems.ExportItemsGraphRoute
import com.x8bit.bitwarden.ui.vault.feature.exportitems.exportItemsGraph
import com.x8bit.bitwarden.ui.vault.feature.exportitems.navigateToExportItemsGraph
import com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword.navigateToVerifyPassword
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.navigateToVaultItemListingAsRoot
import com.x8bit.bitwarden.ui.vault.feature.migratetomyitems.MigrateToMyItemsRoute
import com.x8bit.bitwarden.ui.vault.feature.migratetomyitems.navigateToMigrateToMyItems
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
    navController: NavHostController = rememberBitwardenNavController(name = "RootNavScreen"),
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
        startDestination = SplashRoute,
        enterTransition = { toEnterTransition()(this) },
        exitTransition = { toExitTransition()(this) },
        popEnterTransition = { toEnterTransition()(this) },
        popExitTransition = { toExitTransition()(this) },
    ) {
        splashDestination()
        authGraph(navController)
        removePasswordDestination()
        resetPasswordDestination(
            onNavigateToPreventAccountLockOut = {
                navController.navigateToPreventAccountLockout()
            },
        )
        trustedDeviceGraph(navController)
        vaultUnlockDestination()
        vaultUnlockedGraph(navController)
        setupUnlockDestinationAsRoot()
        setupBrowserAutofillDestinationAsRoot()
        setupAutoFillDestinationAsRoot()
        setupCompleteDestination()
        exportItemsGraph(navController)
    }

    val targetRoute = when (state) {
        RootNavState.Auth,
        is RootNavState.CompleteOngoingRegistration,
        RootNavState.AuthWithWelcome,
        RootNavState.ExpiredRegistrationLink,
            -> AuthGraphRoute

        RootNavState.ResetPassword -> ResetPasswordRoute
        RootNavState.SetPassword -> SetPasswordRoute
        RootNavState.RemovePassword -> RemovePasswordRoute
        RootNavState.Splash -> SplashRoute
        RootNavState.TrustedDevice -> TrustedDeviceGraphRoute
        RootNavState.VaultLocked -> VaultUnlockRoute.Standard
        is RootNavState.VaultUnlocked,
        is RootNavState.VaultUnlockedForAutofillSave,
        is RootNavState.VaultUnlockedForAutofillSelection,
        is RootNavState.VaultUnlockedForNewSend,
        is RootNavState.VaultUnlockedForNewTotp,
        is RootNavState.VaultUnlockedForAuthRequest,
        is RootNavState.VaultUnlockedForFido2Save,
        is RootNavState.VaultUnlockedForFido2Assertion,
        is RootNavState.VaultUnlockedForPasswordGet,
        is RootNavState.VaultUnlockedForProviderGetCredentials,
        is RootNavState.VaultUnlockedForCreatePasswordRequest,
            -> VaultUnlockedGraphRoute

        is RootNavState.CredentialExchangeExport,
        is RootNavState.CredentialExchangeExportSkipAccountSelection,
            -> ExportItemsGraphRoute

        RootNavState.OnboardingAccountLockSetup -> SetupUnlockRoute.AsRoot
        RootNavState.OnboardingAutoFillSetup -> SetupAutofillRoute.AsRoot
        RootNavState.OnboardingBrowserAutofillSetup -> SetupBrowserAutofillRoute.AsRoot
        RootNavState.OnboardingStepsComplete -> SetupCompleteRoute
        is RootNavState.MigrateToMyItems -> {
            val migrateState = state as RootNavState.MigrateToMyItems
            MigrateToMyItemsRoute(
                organizationId = migrateState.organizationId,
                organizationName = migrateState.organizationName,
            )
        }
    }
    val currentRoute = navController.currentDestination?.rootLevelRoute()

    // Don't navigate if we are already at the correct root. This notably happens during process
    // death. In this case, the NavHost already restores state, so we don't have to navigate.
    // However, if the route is correct but the underlying state is different, we should still
    // proceed in order to get a fresh version of that route.
    if (currentRoute == targetRoute.toObjectNavigationRoute() &&
        previousStateReference.get() == state
    ) {
        previousStateReference.set(state)
        return
    }
    previousStateReference.set(state)

    // In some scenarios on an emulator the Activity can leak when recreated
    // if we don't first clear focus anytime we change the root destination.
    LocalActivity.current?.currentFocus?.clearFocus()

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

            is RootNavState.MigrateToMyItems -> {
                navController.navigateToMigrateToMyItems(
                    organizationName = currentState.organizationName,
                    organizationId = currentState.organizationId,
                    navOptions = rootNavOptions,
                )
            }

            RootNavState.RemovePassword -> navController.navigateToRemovePassword(rootNavOptions)
            RootNavState.ResetPassword -> {
                navController.navigateToResetPasswordScreen(rootNavOptions)
            }

            RootNavState.SetPassword -> navController.navigateToSetPassword(rootNavOptions)
            RootNavState.Splash -> navController.navigateToSplash(rootNavOptions)
            RootNavState.TrustedDevice -> navController.navigateToTrustedDeviceGraph(rootNavOptions)
            RootNavState.VaultLocked -> navController.navigateToVaultUnlock(rootNavOptions)
            is RootNavState.VaultUnlocked -> navController.navigateToVaultUnlockedGraph(
                navOptions = rootNavOptions,
            )

            is RootNavState.VaultUnlockedForNewSend -> {
                navController.navigateToVaultUnlock(rootNavOptions)
                navController.navigateToAddEditSend(
                    route = AddEditSendRoute(
                        sendType = currentState.sendType,
                        modeType = ModeType.ADD,
                    ),
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
                    args = VaultAddEditArgs(
                        vaultAddEditType = VaultAddEditType.AddItem,
                        vaultItemCipherType = currentState.autofillSaveItem.toVaultItemCipherType(),
                    ),
                    navOptions = rootNavOptions,
                )
            }

            is RootNavState.VaultUnlockedForCreatePasswordRequest -> {
                navController.navigateToVaultUnlockedGraph(rootNavOptions)
                navController.navigateToVaultAddEdit(
                    args = VaultAddEditArgs(
                        vaultAddEditType = VaultAddEditType.AddItem,
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
            is RootNavState.VaultUnlockedForPasswordGet,
            is RootNavState.VaultUnlockedForProviderGetCredentials,
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

            RootNavState.OnboardingBrowserAutofillSetup -> {
                navController.navigateToSetupBrowserAutoFillAsRootScreen(rootNavOptions)
            }

            RootNavState.OnboardingStepsComplete -> {
                navController.navigateToSetupCompleteScreen(rootNavOptions)
            }

            is RootNavState.CredentialExchangeExport -> {
                navController.navigateToExportItemsGraph(rootNavOptions)
            }

            is RootNavState.CredentialExchangeExportSkipAccountSelection -> {
                navController.navigateToVerifyPassword(
                    userId = currentState.userId,
                    navOptions = rootNavOptions,
                    hasOtherAccounts = false,
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
        ResetPasswordRoute.toObjectNavigationRoute() -> RootTransitionProviders.Enter.slideUp
        else -> when (initialState.destination.rootLevelRoute()) {
            // Disable transitions when coming from the splash screen
            SplashRoute.toObjectNavigationRoute() -> RootTransitionProviders.Enter.none
            // The RESET_PASSWORD_ROUTE animation should be stay but due to an issue when combining
            // certain animations, we are just using a fadeIn instead.
            ResetPasswordRoute.toObjectNavigationRoute() -> RootTransitionProviders.Enter.fadeIn
            else -> RootTransitionProviders.Enter.fadeIn
        }
    }

/**
 * Define the exit transition for each route.
 */
@Suppress("MaxLineLength")
private fun AnimatedContentTransitionScope<NavBackStackEntry>.toExitTransition(): NonNullExitTransitionProvider {
    return when (initialState.destination.rootLevelRoute()) {
        // Disable transitions when coming from the splash screen
        SplashRoute.toObjectNavigationRoute() -> RootTransitionProviders.Exit.none
        ResetPasswordRoute.toObjectNavigationRoute() -> RootTransitionProviders.Exit.slideDown
        else -> when (targetState.destination.rootLevelRoute()) {
            ResetPasswordRoute.toObjectNavigationRoute() -> RootTransitionProviders.Exit.stay
            else -> RootTransitionProviders.Exit.fadeOut
        }
    }
}
