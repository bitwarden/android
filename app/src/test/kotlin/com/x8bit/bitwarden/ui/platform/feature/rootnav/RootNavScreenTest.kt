package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.navigation.navOptions
import com.bitwarden.ui.platform.base.createMockNavHostController
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.SetupAutofillRoute
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.SetupBrowserAutofillRoute
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.SetupCompleteRoute
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.SetupUnlockRoute
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraphRoute
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationRoute
import com.x8bit.bitwarden.ui.auth.feature.expiredregistrationlink.ExpiredRegistrationLinkRoute
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.ResetPasswordRoute
import com.x8bit.bitwarden.ui.auth.feature.setpassword.SetPasswordRoute
import com.x8bit.bitwarden.ui.auth.feature.trusteddevice.TrustedDeviceGraphRoute
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.VaultUnlockRoute
import com.x8bit.bitwarden.ui.auth.feature.welcome.WelcomeRoute
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.feature.splash.SplashRoute
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraphRoute
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendRoute
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.ModeType
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditMode
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditRoute
import com.x8bit.bitwarden.ui.vault.feature.exportitems.ExportItemsGraphRoute
import com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword.VerifyPasswordRoute
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.ItemListingType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingRoute
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RootNavScreenTest : BitwardenComposeTest() {
    private val mockNavHostController = createMockNavHostController()
    private val rootNavStateFlow = MutableStateFlow<RootNavState>(RootNavState.Splash)
    private val viewModel = mockk<RootNavViewModel> {
        every { eventFlow } returns emptyFlow()
        every { stateFlow } returns rootNavStateFlow
    }

    private val expectedNavOptions = navOptions {
        // When changing root navigation state, pop everything else off the back stack:
        popUpTo(id = mockNavHostController.graph.id) {
            inclusive = false
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }

    private var isSplashScreenRemoved: Boolean = false

    @Before
    fun setup() {
        setContent {
            RootNavScreen(
                viewModel = viewModel,
                navController = mockNavHostController,
                onSplashScreenRemoved = { isSplashScreenRemoved = true },
            )
        }
    }

    @Test
    fun `initial route should be splash`() {
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = SplashRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }
    }

    @Test
    fun `when root nav destination changes, navigation should follow`() = runTest {
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = SplashRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }
        assertFalse(isSplashScreenRemoved)

        // Make sure navigating to Auth works as expected:
        rootNavStateFlow.value = RootNavState.Auth
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = AuthGraphRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }
        assertTrue(isSplashScreenRemoved)

        // Make sure navigating to Auth with the welcome route works as expected:
        rootNavStateFlow.value = RootNavState.AuthWithWelcome
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = WelcomeRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to complete registration route works as expected:
        rootNavStateFlow.value = RootNavState.CompleteOngoingRegistration(
            email = "example@email.com",
            verificationToken = "verificationToken",
            fromEmail = true,
            timestamp = FIXED_CLOCK.millis(),
        )
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = CompleteRegistrationRoute(
                        emailAddress = "example@email.com",
                        verificationToken = "verificationToken",
                        fromEmail = true,
                    ),
                    navOptions = null,
                )
            }
        }

        // Make sure navigating to expired registration link route works as expected:
        rootNavStateFlow.value = RootNavState.ExpiredRegistrationLink
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(route = ExpiredRegistrationLinkRoute)
            }
        }

        // Make sure navigating to vault locked works as expected:
        rootNavStateFlow.value = RootNavState.VaultLocked
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = VaultUnlockRoute.Standard,
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to reset password works as expected:
        rootNavStateFlow.value = RootNavState.ResetPassword
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = ResetPasswordRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to set password works as expected:
        rootNavStateFlow.value = RootNavState.SetPassword
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = SetPasswordRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to set password works as expected:
        rootNavStateFlow.value = RootNavState.TrustedDevice
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = TrustedDeviceGraphRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to vault unlocked works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlocked(activeUserId = "userId")
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = VaultUnlockedGraphRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to vault unlocked for new totp works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlockedForNewTotp(activeUserId = "userId")
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = VaultItemListingRoute.AsRoot(
                        type = ItemListingType.LOGIN,
                        itemId = null,
                    ),
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to vault unlocked for new sends works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlockedForNewSend(sendType = SendItemType.FILE)
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = AddEditSendRoute(
                        modeType = ModeType.ADD,
                        sendType = SendItemType.FILE,
                        sendId = null,
                    ),
                    navOptions = expectedNavOptions,
                )
            }
        }
        rootNavStateFlow.value = RootNavState.VaultUnlockedForNewSend(sendType = SendItemType.TEXT)
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = AddEditSendRoute(
                        modeType = ModeType.ADD,
                        sendType = SendItemType.TEXT,
                        sendId = null,
                    ),
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to vault unlocked for autofill save for login works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlockedForAutofillSave(
            autofillSaveItem = mockk<AutofillSaveItem.Login>(),
        )
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = VaultUnlockedGraphRoute,
                    navOptions = expectedNavOptions,
                )
                mockNavHostController.navigate(
                    route = VaultAddEditRoute(
                        vaultAddEditMode = VaultAddEditMode.ADD,
                        vaultItemId = null,
                        vaultItemCipherType = VaultItemCipherType.LOGIN,
                        selectedFolderId = null,
                        selectedCollectionId = null,
                    ),
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to vault unlocked for autofill save for card works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlockedForAutofillSave(
            autofillSaveItem = mockk<AutofillSaveItem.Card>(),
        )
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = VaultUnlockedGraphRoute,
                    navOptions = expectedNavOptions,
                )
                mockNavHostController.navigate(
                    route = VaultAddEditRoute(
                        vaultAddEditMode = VaultAddEditMode.ADD,
                        vaultItemId = null,
                        vaultItemCipherType = VaultItemCipherType.CARD,
                        selectedFolderId = null,
                        selectedCollectionId = null,
                    ),
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to vault unlocked for autofill works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlockedForAutofillSelection(
            activeUserId = "userId",
            type = AutofillSelectionData.Type.LOGIN,
        )
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = VaultUnlockedGraphRoute,
                    navOptions = expectedNavOptions,
                )
                mockNavHostController.navigate(
                    route = VaultItemListingRoute.AsRoot(
                        type = ItemListingType.LOGIN,
                        itemId = null,
                    ),
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to vault unlocked for create password request works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlockedForCreatePasswordRequest(
            username = "activeUserId",
            password = "mockPassword",
            uri = "mockUri",
        )
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = VaultUnlockedGraphRoute,
                    navOptions = expectedNavOptions,
                )
                mockNavHostController.navigate(
                    route = VaultAddEditRoute(
                        vaultAddEditMode = VaultAddEditMode.ADD,
                        vaultItemId = null,
                        vaultItemCipherType = VaultItemCipherType.LOGIN,
                        selectedFolderId = null,
                        selectedCollectionId = null,
                    ),
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to vault unlocked for CreateCredentialRequest works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlockedForFido2Save(
            activeUserId = "activeUserId",
            createCredentialRequest = mockk(),
        )
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = VaultUnlockedGraphRoute,
                    navOptions = expectedNavOptions,
                )
                mockNavHostController.navigate(
                    route = VaultItemListingRoute.AsRoot(
                        type = ItemListingType.LOGIN,
                        itemId = null,
                    ),
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to vault unlocked for Fido2Assertion works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlockedForFido2Assertion(
            activeUserId = "activeUserId",
            fido2CredentialAssertionRequest = mockk(),
        )
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = VaultUnlockedGraphRoute,
                    navOptions = expectedNavOptions,
                )
                mockNavHostController.navigate(
                    route = VaultItemListingRoute.AsRoot(
                        type = ItemListingType.LOGIN,
                        itemId = null,
                    ),
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to vault unlocked for PasswordGet works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlockedForPasswordGet(
            activeUserId = "activeUserId",
            providerGetPasswordCredentialRequest = mockk(),
        )
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = VaultUnlockedGraphRoute,
                    navOptions = expectedNavOptions,
                )
                mockNavHostController.navigate(
                    route = VaultItemListingRoute.AsRoot(
                        type = ItemListingType.LOGIN,
                        itemId = null,
                    ),
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to vault unlocked for GetCredentialsRequest works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlockedForProviderGetCredentials(
            activeUserId = "activeUserId",
            getCredentialsRequest = mockk(),
        )
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = VaultUnlockedGraphRoute,
                    navOptions = expectedNavOptions,
                )
                mockNavHostController.navigate(
                    route = VaultItemListingRoute.AsRoot(
                        type = ItemListingType.LOGIN,
                        itemId = null,
                    ),
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to account lock setup works as expected:
        rootNavStateFlow.value = RootNavState.OnboardingAccountLockSetup
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = SetupUnlockRoute.AsRoot,
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to account autofill setup works as expected:
        rootNavStateFlow.value = RootNavState.OnboardingAutoFillSetup
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = SetupAutofillRoute.AsRoot,
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to browser autofill setup works as expected:
        rootNavStateFlow.value = RootNavState.OnboardingBrowserAutofillSetup
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = SetupBrowserAutofillRoute.AsRoot,
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to account setup complete works as expected:
        rootNavStateFlow.value = RootNavState.OnboardingStepsComplete
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = SetupCompleteRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to export items graph works as expected:
        rootNavStateFlow.value = RootNavState.CredentialExchangeExport
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = ExportItemsGraphRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }

        // Make sure navigating to export items graph works as expected:
        rootNavStateFlow.value = RootNavState.CredentialExchangeExportSkipAccountSelection(
            userId = "activeUserId",
        )
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = ExportItemsGraphRoute,
                    navOptions = expectedNavOptions,
                )

                mockNavHostController.navigate(
                    route = VerifyPasswordRoute(
                        userId = "activeUserId",
                        hasOtherAccounts = false,
                    ),
                    navOptions = expectedNavOptions,
                )
            }
        }
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
