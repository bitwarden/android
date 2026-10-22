package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.activity.compose.LocalActivity
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.rememberNavEntries
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController
import com.bitwarden.ui.platform.theme.NavDisplayTransitions
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.AccountSetupGraph
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.accountSetupGraph
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph
import com.x8bit.bitwarden.ui.auth.feature.auth.authGraph
import com.x8bit.bitwarden.ui.auth.feature.removepassword.removePasswordDestination
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.resetPasswordGraph
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.model.UnlockType
import com.x8bit.bitwarden.ui.platform.feature.rootnav.util.toVaultItemListingType
import com.x8bit.bitwarden.ui.platform.feature.splash.splashDestination
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.vaultUnlockedGraph
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.ModeType
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toVaultItemCipherType
import com.x8bit.bitwarden.ui.vault.feature.exportitems.ExportItemsGraph
import com.x8bit.bitwarden.ui.vault.feature.exportitems.exportItemsGraph
import com.x8bit.bitwarden.ui.vault.feature.migratetomyitems.migrateToMyItemsGraph
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import java.util.concurrent.atomic.AtomicReference

/**
 * Controls the root level [NavDisplay] for the app.
 *
 * The root back stack is driven entirely by the [RootNavState]: whenever the state changes, the
 * entire back stack is replaced with the back stack for the new state. Each route in the
 * [RootNavGraph] represents a top-level section of the app, which may host its own nested
 * navigation graph.
 */
@Composable
fun RootNavScreen(
    viewModel: RootNavViewModel = hiltViewModel(),
    navigationController: NavigationController<RootNavGraph> = rememberNavigationController(
        routes = arrayOf(RootNavGraph.SplashRoute),
    ),
    entryProvider: (RootNavGraph) -> NavEntry<RootNavGraph> = entryProvider(),
    onSplashScreenRemoved: () -> Unit = {},
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val previousStateReference = remember { AtomicReference(state) }

    val isNotSplashScreen = state != RootNavState.Splash
    LaunchedEffect(isNotSplashScreen) {
        if (isNotSplashScreen) onSplashScreenRemoved()
    }

    NavDisplay(
        entries = navigationController.rememberNavEntries(entryProvider = entryProvider),
        onBack = navigationController::navigateBack,
        transitionSpec = NavDisplayTransitions.root(),
        popTransitionSpec = NavDisplayTransitions.root(),
        predictivePopTransitionSpec = NavDisplayTransitions.predictive(
            provider = NavDisplayTransitions.root(),
        ),
    )

    val targetRoute = state.toRoute()

    // Don't navigate if we are already at the correct root. This notably happens during process
    // death. In this case, the backstack has already been restored, so we don't have to navigate.
    // However, if the root is correct but the underlying state is different, we should still
    // proceed in order to get a fresh version of that root.
    if (navigationController.backStack.firstOrNull() == targetRoute &&
        previousStateReference.get() == state
    ) {
        return
    }
    previousStateReference.set(state)

    // In some scenarios on an emulator the Activity can leak when recreated
    // if we don't first clear focus anytime we change the root destination.
    LocalActivity.current?.currentFocus?.clearFocus()

    navigationController.replaceAll(routes = listOf(targetRoute))
}

/**
 * Returns the [RootNavGraph] route of the top-level section that should be displayed for the given
 * [RootNavState].
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
@VisibleForTesting
internal fun RootNavState.toRoute(): RootNavGraph = when (this) {
    RootNavState.Auth -> RootNavGraph.AuthRoute(backStack = listOf(AuthGraph.LandingRoute))
    RootNavState.AuthWithWelcome -> {
        RootNavGraph.AuthRoute(backStack = listOf(AuthGraph.WelcomeRoute))
    }

    is RootNavState.CompleteOngoingRegistration -> {
        RootNavGraph.AuthRoute(
            backStack = listOf(
                AuthGraph.LandingRoute,
                AuthGraph.CompleteRegistrationRoute(
                    emailAddress = this.email,
                    verificationToken = this.verificationToken,
                    fromEmail = this.fromEmail,
                ),
            ),
        )
    }

    RootNavState.ExpiredRegistrationLink -> {
        RootNavGraph.AuthRoute(
            backStack = listOf(
                AuthGraph.LandingRoute,
                AuthGraph.ExpiredRegistrationLinkRoute,
            ),
        )
    }

    RootNavState.SetPassword -> {
        RootNavGraph.AuthRoute(backStack = listOf(AuthGraph.SetPasswordRoute))
    }

    RootNavState.TrustedDevice -> {
        RootNavGraph.AuthRoute(backStack = listOf(AuthGraph.TrustedDeviceRoute))
    }

    RootNavState.VaultLocked -> {
        RootNavGraph.AuthRoute(
            backStack = listOf(AuthGraph.VaultUnlockRoute(unlockType = UnlockType.STANDARD)),
        )
    }

    is RootNavState.MigrateToMyItems -> RootNavGraph.MigrateToMyItemsRoute
    RootNavState.RemovePassword -> RootNavGraph.RemovePasswordRoute
    RootNavState.ResetPassword -> RootNavGraph.ResetPasswordRoute
    RootNavState.Splash -> RootNavGraph.SplashRoute
    is RootNavState.VaultUnlocked -> {
        RootNavGraph.VaultUnlockedRoute(
            activeUserId = this.activeUserId,
            backStack = listOf(VaultUnlockedGraph.NavBarRoute),
        )
    }

    is RootNavState.VaultUnlockedForNewSend -> {
        RootNavGraph.VaultUnlockedRoute(
            activeUserId = this.activeUserId,
            backStack = listOf(
                VaultUnlockedGraph.AddEditSendRoute(
                    sendType = this.sendType,
                    modeType = ModeType.ADD,
                ),
            ),
        )
    }

    is RootNavState.VaultUnlockedForAutofillSave -> {
        RootNavGraph.VaultUnlockedRoute(
            activeUserId = this.activeUserId,
            backStack = listOf(
                VaultUnlockedGraph.VaultAddEditRoute(
                    vaultAddEditType = VaultAddEditType.AddItem(),
                    vaultItemCipherType = this.autofillSaveItem.toVaultItemCipherType(),
                ),
            ),
        )
    }

    is RootNavState.VaultUnlockedForCreatePasswordRequest -> {
        RootNavGraph.VaultUnlockedRoute(
            activeUserId = this.activeUserId,
            backStack = listOf(
                VaultUnlockedGraph.VaultAddEditRoute(
                    vaultAddEditType = VaultAddEditType.AddItem(),
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            ),
        )
    }

    is RootNavState.VaultUnlockedForAutofillSelection -> {
        RootNavGraph.VaultUnlockedRoute(
            activeUserId = this.activeUserId,
            backStack = listOf(
                VaultUnlockedGraph.VaultItemListingRoute(
                    vaultItemListingType = this.type.toVaultItemListingType(),
                ),
            ),
        )
    }

    is RootNavState.VaultUnlockedForAuthRequest -> {
        RootNavGraph.VaultUnlockedRoute(
            activeUserId = this.activeUserId,
            backStack = listOf(VaultUnlockedGraph.LoginApprovalRoute(fingerprint = null)),
        )
    }

    is RootNavState.VaultUnlockedForNewTotp,
    is RootNavState.VaultUnlockedForFido2Save,
    is RootNavState.VaultUnlockedForFido2Assertion,
    is RootNavState.VaultUnlockedForPasswordGet,
    is RootNavState.VaultUnlockedForProviderGetCredentials,
        -> {
        RootNavGraph.VaultUnlockedRoute(
            activeUserId = this.activeUserId,
            backStack = listOf(
                VaultUnlockedGraph.VaultItemListingRoute(
                    vaultItemListingType = VaultItemListingType.Login,
                ),
            ),
        )
    }

    RootNavState.OnboardingAccountLockSetup -> {
        RootNavGraph.AccountSetupRoute(backStack = listOf(AccountSetupGraph.SetupUnlockRoute))
    }

    RootNavState.OnboardingAutoFillSetup -> {
        RootNavGraph.AccountSetupRoute(backStack = listOf(AccountSetupGraph.SetupAutofillRoute))
    }

    RootNavState.OnboardingBrowserAutofillSetup -> {
        RootNavGraph.AccountSetupRoute(
            backStack = listOf(AccountSetupGraph.SetupBrowserAutofillRoute),
        )
    }

    RootNavState.OnboardingStepsComplete -> {
        RootNavGraph.AccountSetupRoute(backStack = listOf(AccountSetupGraph.SetupCompleteRoute))
    }

    RootNavState.CredentialExchangeExport -> {
        RootNavGraph.ExportItemsRoute(backStack = listOf(ExportItemsGraph.SelectAccountRoute))
    }

    is RootNavState.CredentialExchangeExportSkipAccountSelection -> {
        RootNavGraph.ExportItemsRoute(
            backStack = listOf(
                ExportItemsGraph.VerifyPasswordRoute(
                    userId = this.userId,
                    hasOtherAccounts = false,
                ),
            ),
        )
    }
}

private fun entryProvider(): (RootNavGraph) -> NavEntry<RootNavGraph> =
    entryProvider {
        splashDestination()
        authGraph()
        removePasswordDestination()
        resetPasswordGraph()
        accountSetupGraph()
        vaultUnlockedGraph()
        exportItemsGraph()
        migrateToMyItemsGraph()
    }
