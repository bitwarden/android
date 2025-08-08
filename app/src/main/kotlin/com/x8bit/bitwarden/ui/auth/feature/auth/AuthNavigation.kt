package com.x8bit.bitwarden.ui.auth.feature.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import androidx.navigation.navigation
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
import com.x8bit.bitwarden.ui.auth.feature.landing.LandingRoute
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
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.navigateToTwoFactorLogin
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.twoFactorLoginDestination
import com.x8bit.bitwarden.ui.auth.feature.welcome.welcomeDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.navigateToPreAuthSettings
import com.x8bit.bitwarden.ui.platform.feature.settings.preAuthSettingsDestinations
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the auth graph.
 */
@Serializable
data object AuthGraphRoute

/**
 * Add auth destinations to the nav graph.
 */
@Suppress("LongMethod")
fun NavGraphBuilder.authGraph(
    navController: NavHostController,
) {
    navigation<AuthGraphRoute>(
        startDestination = LandingRoute,
    ) {
        startRegistrationDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToCompleteRegistration = { emailAddress, verificationToken ->
                navController.navigateToCompleteRegistration(
                    emailAddress = emailAddress,
                    verificationToken = verificationToken,
                    fromEmail = false,
                )
            },
            onNavigateToCheckEmail = { emailAddress ->
                navController.navigateToCheckEmail(emailAddress = emailAddress)
            },
            onNavigateToEnvironment = { navController.navigateToEnvironment() },
        )
        checkEmailDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        completeRegistrationDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToPasswordGuidance = {
                navController.navigateToMasterPasswordGuidance()
            },
            onNavigateToPreventAccountLockout = {
                navController.navigateToPreventAccountLockout()
            },
            onNavigateToLogin = { emailAddress ->
                navController.navigateToLogin(
                    emailAddress = emailAddress,
                    navOptions = navOptions {
                        popUpTo(route = LandingRoute)
                    },
                )
            },
        )
        enterpriseSignOnDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSetPassword = { navController.navigateToSetPassword() },
            onNavigateToTwoFactorLogin = { emailAddress, orgIdentifier ->
                navController.navigateToTwoFactorLogin(
                    emailAddress = emailAddress,
                    password = null,
                    orgIdentifier = orgIdentifier,
                )
            },
        )
        setPasswordDestination()
        landingDestination(
            onNavigateToLogin = { emailAddress ->
                navController.navigateToLogin(
                    emailAddress = emailAddress,
                )
            },
            onNavigateToEnvironment = {
                navController.navigateToEnvironment()
            },
            onNavigateToStartRegistration = { navController.navigateToStartRegistration() },
            onNavigateToPreAuthSettings = { navController.navigateToPreAuthSettings() },
        )
        welcomeDestination(
            onNavigateToLogin = { navController.navigateToLanding() },
            onNavigateToStartRegistration = { navController.navigateToStartRegistration() },
        )
        loginDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToMasterPasswordHint = { emailAddress ->
                navController.navigateToMasterPasswordHint(
                    emailAddress = emailAddress,
                )
            },
            onNavigateToEnterpriseSignOn = { emailAddress ->
                navController.navigateToEnterpriseSignOn(
                    emailAddress = emailAddress,
                )
            },
            onNavigateToLoginWithDevice = { emailAddress ->
                navController.navigateToLoginWithDevice(
                    emailAddress = emailAddress,
                    loginType = LoginWithDeviceType.OTHER_DEVICE,
                )
            },
            onNavigateToTwoFactorLogin = { emailAddress, password, isNewDeviceVerification ->
                navController.navigateToTwoFactorLogin(
                    emailAddress = emailAddress,
                    password = password,
                    orgIdentifier = null,
                    isNewDeviceVerification = isNewDeviceVerification,
                )
            },
        )
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
        environmentDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        masterPasswordHintDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        twoFactorLoginDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        masterPasswordGuidanceDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToGeneratePassword = { navController.navigateToMasterPasswordGenerator() },
        )
        preventAccountLockoutDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        masterPasswordGeneratorDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToPreventLockout = { navController.navigateToPreventAccountLockout() },
            onNavigateBackWithPassword = {
                navController.popUpToCompleteRegistration()
            },
        )
        expiredRegistrationLinkDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToStartRegistration = {
                navController.navigateToStartRegistration(
                    navOptions = navOptions {
                        popUpTo(route = LandingRoute)
                    },
                )
            },
            onNavigateToLogin = {
                navController.navigateToLanding(
                    navOptions = navOptions {
                        popUpTo(route = LandingRoute)
                    },
                )
            },
        )
        preAuthSettingsDestinations(navController = navController)
    }
}

/**
 * Navigate to the auth screen. Note this will only work if auth destination was added
 * via [authGraph].
 */
fun NavController.navigateToAuthGraph(
    navOptions: NavOptions? = null,
) {
    navigate(route = AuthGraphRoute, navOptions = navOptions)
}
