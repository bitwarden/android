package com.x8bit.bitwarden.ui.auth.feature.auth

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.components.BitwardenNavDisplay
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController
import com.x8bit.bitwarden.ui.auth.feature.checkemail.checkEmailDestination
import com.x8bit.bitwarden.ui.auth.feature.checkemail.navigateToCheckEmail
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.completeRegistrationDestination
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.navigateToCompleteRegistration
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.popUpToCompleteRegistration
import com.x8bit.bitwarden.ui.auth.feature.enterprisesignon.enterpriseSignOnDestination
import com.x8bit.bitwarden.ui.auth.feature.enterprisesignon.navigateToEnterpriseSignOn
import com.x8bit.bitwarden.ui.auth.feature.environment.environmentDestination
import com.x8bit.bitwarden.ui.auth.feature.environment.navigateToEnvironment
import com.x8bit.bitwarden.ui.auth.feature.expiredregistrationlink.expiredRegistrationLinkDestination
import com.x8bit.bitwarden.ui.auth.feature.landing.landingDestination
import com.x8bit.bitwarden.ui.auth.feature.landing.navigateToLanding
import com.x8bit.bitwarden.ui.auth.feature.login.loginDestination
import com.x8bit.bitwarden.ui.auth.feature.login.navigateToLogin
import com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.loginWithDeviceDestination
import com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.model.LoginWithDeviceType
import com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.navigateToLoginWithDevice
import com.x8bit.bitwarden.ui.auth.feature.masterpasswordgenerator.masterPasswordGeneratorDestination
import com.x8bit.bitwarden.ui.auth.feature.masterpasswordgenerator.navigateToMasterPasswordGenerator
import com.x8bit.bitwarden.ui.auth.feature.masterpasswordguidance.masterPasswordGuidanceDestination
import com.x8bit.bitwarden.ui.auth.feature.masterpasswordguidance.navigateToMasterPasswordGuidance
import com.x8bit.bitwarden.ui.auth.feature.masterpasswordhint.masterPasswordHintDestination
import com.x8bit.bitwarden.ui.auth.feature.masterpasswordhint.navigateToMasterPasswordHint
import com.x8bit.bitwarden.ui.auth.feature.preventaccountlockout.navigateToPreventAccountLockout
import com.x8bit.bitwarden.ui.auth.feature.preventaccountlockout.preventAccountLockoutDestination
import com.x8bit.bitwarden.ui.auth.feature.setpassword.navigateToSetPassword
import com.x8bit.bitwarden.ui.auth.feature.setpassword.setPasswordDestination
import com.x8bit.bitwarden.ui.auth.feature.startregistration.navigateToStartRegistration
import com.x8bit.bitwarden.ui.auth.feature.startregistration.startRegistrationDestination
import com.x8bit.bitwarden.ui.auth.feature.trusteddevice.trustedDeviceDestination
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.navigateToTwoFactorLogin
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.twoFactorLoginDestination
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.model.UnlockType
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.navigateToVaultUnlock
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.vaultUnlockDestination
import com.x8bit.bitwarden.ui.auth.feature.welcome.welcomeDestination
import com.x8bit.bitwarden.ui.platform.feature.rootnav.RootNavGraph
import com.x8bit.bitwarden.ui.platform.feature.settings.navigateToPreAuthSettings
import com.x8bit.bitwarden.ui.platform.feature.settings.preAuthSettingsDestination

/**
 * Add the auth section to the root navigation entries. The [RootNavGraph.AuthRoute] hosts the
 * [AuthGraph].
 */
fun EntryProviderScope<RootNavGraph>.authGraph() {
    entry<RootNavGraph.AuthRoute> { route ->
        val navigationController = rememberNavigationController(
            routes = route.backStack.toTypedArray(),
        )
        BitwardenNavDisplay(
            navigationController = navigationController,
            entryProvider = entryProvider {
                authDestinations(navigationController = navigationController)
            },
        )
    }
}

@Suppress("LongMethod")
private fun EntryProviderScope<AuthGraph>.authDestinations(
    navigationController: NavigationController<AuthGraph>,
) {
    startRegistrationDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToCompleteRegistration = { emailAddress, verificationToken ->
            navigationController.navigateToCompleteRegistration(
                emailAddress = emailAddress,
                verificationToken = verificationToken,
                fromEmail = false,
            )
        },
        onNavigateToCheckEmail = { emailAddress ->
            navigationController.navigateToCheckEmail(emailAddress = emailAddress)
        },
        onNavigateToEnvironment = { navigationController.navigateToEnvironment() },
    )
    checkEmailDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    completeRegistrationDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToPasswordGuidance = { navigationController.navigateToMasterPasswordGuidance() },
        onNavigateToPreventAccountLockout = {
            navigationController.navigateToPreventAccountLockout()
        },
        onNavigateToLogin = { emailAddress ->
            navigationController.navigatePoppingUpTo(
                route = AuthGraph.LoginRoute(emailAddress = emailAddress),
                predicate = { it is AuthGraph.LandingRoute },
            )
        },
    )
    enterpriseSignOnDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToSetPassword = { navigationController.navigateToSetPassword() },
        onNavigateToTwoFactorLogin = { emailAddress, orgIdentifier ->
            navigationController.navigateToTwoFactorLogin(
                emailAddress = emailAddress,
                password = null,
                orgIdentifier = orgIdentifier,
            )
        },
    )
    setPasswordDestination()
    landingDestination(
        onNavigateToLogin = { emailAddress ->
            navigationController.navigateToLogin(emailAddress = emailAddress)
        },
        onNavigateToEnvironment = { navigationController.navigateToEnvironment() },
        onNavigateToStartRegistration = { navigationController.navigateToStartRegistration() },
        onNavigateToPreAuthSettings = { navigationController.navigateToPreAuthSettings() },
    )
    welcomeDestination(
        onNavigateToLogin = { navigationController.navigateToLanding() },
        onNavigateToStartRegistration = { navigationController.navigateToStartRegistration() },
    )
    loginDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToMasterPasswordHint = { emailAddress ->
            navigationController.navigateToMasterPasswordHint(emailAddress = emailAddress)
        },
        onNavigateToEnterpriseSignOn = { emailAddress ->
            navigationController.navigateToEnterpriseSignOn(emailAddress = emailAddress)
        },
        onNavigateToLoginWithDevice = { emailAddress ->
            navigationController.navigateToLoginWithDevice(
                emailAddress = emailAddress,
                loginType = LoginWithDeviceType.OTHER_DEVICE,
            )
        },
        onNavigateToTwoFactorLogin = { emailAddress, password, isNewDeviceVerification ->
            navigationController.navigateToTwoFactorLogin(
                emailAddress = emailAddress,
                password = password,
                orgIdentifier = null,
                isNewDeviceVerification = isNewDeviceVerification,
            )
        },
    )
    loginWithDeviceDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToTwoFactorLogin = {
            navigationController.navigateToTwoFactorLogin(
                emailAddress = it,
                password = null,
                orgIdentifier = null,
            )
        },
    )
    environmentDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    masterPasswordHintDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    twoFactorLoginDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    masterPasswordGuidanceDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToGeneratePassword = { navigationController.navigateToMasterPasswordGenerator() },
    )
    preventAccountLockoutDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    masterPasswordGeneratorDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToPreventLockout = { navigationController.navigateToPreventAccountLockout() },
        onNavigateBackWithPassword = { navigationController.popUpToCompleteRegistration() },
    )
    expiredRegistrationLinkDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToStartRegistration = {
            navigationController.navigatePoppingUpTo(
                route = AuthGraph.StartRegistrationRoute,
                predicate = { it is AuthGraph.LandingRoute },
            )
        },
        onNavigateToLogin = {
            // The landing screen is replaced, rather than duplicated, since every entry in the
            // back stack must be unique.
            navigationController.navigatePoppingUpTo(
                route = AuthGraph.LandingRoute,
                inclusive = true,
                predicate = { it is AuthGraph.LandingRoute },
            )
        },
    )
    preAuthSettingsDestination(onNavigateBack = { navigationController.navigateBack() })
    trustedDeviceDestination(
        onNavigateToAdminApproval = {
            navigationController.navigateToLoginWithDevice(
                emailAddress = it,
                loginType = LoginWithDeviceType.SSO_ADMIN_APPROVAL,
            )
        },
        onNavigateToLoginWithOtherDevice = {
            navigationController.navigateToLoginWithDevice(
                emailAddress = it,
                loginType = LoginWithDeviceType.SSO_OTHER_DEVICE,
            )
        },
        onNavigateToLock = {
            navigationController.navigateToVaultUnlock(unlockType = UnlockType.TDE)
        },
    )
    vaultUnlockDestination()
}
