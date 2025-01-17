package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.navigation.navOptions
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.FakeNavHostController
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RootNavScreenTest : BaseComposeTest() {
    private val fakeNavHostController = FakeNavHostController()

    private val expectedNavOptions = navOptions {
        // When changing root navigation state, pop everything else off the back stack:
        popUpTo(fakeNavHostController.graphId) {
            inclusive = false
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }

    @Test
    fun `initial route should be splash`() {
        val viewModel = mockk<RootNavViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(RootNavState.Splash)
        }
        composeTestRule.setContent {
            RootNavScreen(
                viewModel = viewModel,
                navController = fakeNavHostController,
            )
        }
        composeTestRule.runOnIdle {
            fakeNavHostController.assertCurrentRoute("splash")
        }
    }

    @Test
    fun `when root nav destination changes, navigation should follow`() = runTest {
        val rootNavStateFlow = MutableStateFlow<RootNavState>(RootNavState.Splash)
        val viewModel = mockk<RootNavViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns rootNavStateFlow
        }
        var isSplashScreenRemoved = false
        composeTestRule.setContent {
            RootNavScreen(
                viewModel = viewModel,
                navController = fakeNavHostController,
                onSplashScreenRemoved = { isSplashScreenRemoved = true },
            )
        }
        composeTestRule.runOnIdle {
            fakeNavHostController.assertCurrentRoute("splash")
        }
        assertFalse(isSplashScreenRemoved)

        // Make sure navigating to Auth works as expected:
        rootNavStateFlow.value = RootNavState.Auth
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "auth_graph",
                navOptions = expectedNavOptions,
            )
        }
        assertTrue(isSplashScreenRemoved)

        // Make sure navigating to Auth with the welcome route works as expected:
        rootNavStateFlow.value = RootNavState.AuthWithWelcome
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "welcome",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to complete registration route works as expected:
        rootNavStateFlow.value = RootNavState.CompleteOngoingRegistration(
            email = "example@email.com",
            verificationToken = "verificationToken",
            fromEmail = true,
            timestamp = FIXED_CLOCK.millis(),
        )
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "complete_registration/example@email.com/verificationToken/true",
            )
        }

        // Make sure navigating to expired registration link route works as expected:
        rootNavStateFlow.value = RootNavState.ExpiredRegistrationLink
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "expired_registration_link",
            )
        }

        // Make sure navigating to vault locked works as expected:
        rootNavStateFlow.value = RootNavState.VaultLocked
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "vault_unlock/STANDARD",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to reset password works as expected:
        rootNavStateFlow.value = RootNavState.ResetPassword
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "reset_password",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to set password works as expected:
        rootNavStateFlow.value = RootNavState.SetPassword
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "set_password",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to set password works as expected:
        rootNavStateFlow.value = RootNavState.TrustedDevice
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "trusted_device_graph",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to vault unlocked works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlocked(activeUserId = "userId")
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "vault_unlocked_graph",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to vault unlocked for new totp works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlockedForNewTotp(activeUserId = "userId")
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "vault_item_listing_as_root/login",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to vault unlocked for new sends works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlockedForNewSend
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "add_send_item/add",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to vault unlocked for autofill save works as expected:
        rootNavStateFlow.value =
            RootNavState.VaultUnlockedForAutofillSave(
                autofillSaveItem = mockk(),
            )
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "vault_add_edit_item/add",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to vault unlocked for autofill works as expected:
        rootNavStateFlow.value =
            RootNavState.VaultUnlockedForAutofillSelection(
                activeUserId = "userId",
                type = AutofillSelectionData.Type.LOGIN,
            )
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "vault_item_listing_as_root/login",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to vault unlocked for Fido2Save works as expected:
        rootNavStateFlow.value =
            RootNavState.VaultUnlockedForFido2Save(
                activeUserId = "activeUserId",
                fido2CreateCredentialRequest = mockk(),
            )
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "vault_item_listing_as_root/login",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to vault unlocked for Fido2Assertion works as expected:
        rootNavStateFlow.value =
            RootNavState.VaultUnlockedForFido2Assertion(
                activeUserId = "activeUserId",
                fido2CredentialAssertionRequest = mockk(),
            )
        composeTestRule
            .runOnIdle {
                fakeNavHostController.assertLastNavigation(
                    route = "vault_item_listing_as_root/login",
                    navOptions = expectedNavOptions,
                )
            }

        // Make sure navigating to vault unlocked for Fido2GetCredentials works as expected:
        rootNavStateFlow.value =
            RootNavState.VaultUnlockedForFido2GetCredentials(
                activeUserId = "activeUserId",
                fido2GetCredentialsRequest = mockk(),
            )
        composeTestRule
            .runOnIdle {
                fakeNavHostController.assertLastNavigation(
                    route = "vault_item_listing_as_root/login",
                    navOptions = expectedNavOptions,
                )
            }

        // Make sure navigating to account lock setup works as expected:
        rootNavStateFlow.value =
            RootNavState.OnboardingAccountLockSetup
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "setup_unlock_as_root/true",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to account autofill setup works as expected:
        rootNavStateFlow.value =
            RootNavState.OnboardingAutoFillSetup
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "setup_auto_fill_as_root/true",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to account setup complete works as expected:
        rootNavStateFlow.value =
            RootNavState.OnboardingStepsComplete
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "setup_complete",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to new device two factor works as expected:
        rootNavStateFlow.value =
            RootNavState.NewDeviceTwoFactorNotice(email = "example@bitwarden.com")
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "new_device_notice/example@bitwarden.com",
                navOptions = expectedNavOptions,
            )
        }
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
