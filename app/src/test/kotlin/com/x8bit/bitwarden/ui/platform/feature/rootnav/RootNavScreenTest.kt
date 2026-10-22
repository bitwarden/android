package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.compose.material3.Text
import androidx.navigation3.runtime.NavEntry
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.navigationControllerOf
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.AccountSetupGraph
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.model.UnlockType
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.ModeType
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import com.x8bit.bitwarden.ui.vault.feature.exportitems.ExportItemsGraph
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RootNavScreenTest : BitwardenComposeTest() {
    private val rootNavStateFlow = MutableStateFlow<RootNavState>(RootNavState.Splash)
    private val viewModel = mockk<RootNavViewModel> {
        every { eventFlow } returns emptyFlow()
        every { stateFlow } returns rootNavStateFlow
    }

    private var isSplashScreenRemoved: Boolean = false

    private fun setRootNavScreenContent(
        navigationController: NavigationController<RootNavGraph> = navigationControllerOf(
            routes = arrayOf(RootNavGraph.SplashRoute),
        ),
    ): NavigationController<RootNavGraph> {
        setContent {
            RootNavScreen(
                viewModel = viewModel,
                navigationController = navigationController,
                entryProvider = { key ->
                    NavEntry(key = key) { Text(text = key.toString()) }
                },
                onSplashScreenRemoved = { isSplashScreenRemoved = true },
            )
        }
        return navigationController
    }

    @Test
    fun `initial back stack should be splash`() {
        val navigationController = setRootNavScreenContent()
        composeTestRule.runOnIdle {
            assertEquals(listOf(RootNavGraph.SplashRoute), navigationController.backStack.toList())
        }
        assertFalse(isSplashScreenRemoved)
    }

    @Test
    fun `when root nav state changes away from splash, the splash screen should be removed`() {
        setRootNavScreenContent()
        assertFalse(isSplashScreenRemoved)

        rootNavStateFlow.value = RootNavState.Auth

        composeTestRule.runOnIdle {
            assertTrue(isSplashScreenRemoved)
        }
    }

    @Test
    fun `when root nav state changes, the back stack should be replaced with the new route`() {
        val navigationController = setRootNavScreenContent()

        rootNavStateFlow.value = RootNavState.Auth
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(RootNavGraph.AuthRoute(backStack = listOf(AuthGraph.LandingRoute))),
                navigationController.backStack.toList(),
            )
        }

        rootNavStateFlow.value = RootNavState.VaultUnlocked(activeUserId = ACTIVE_USER_ID)
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(
                    RootNavGraph.VaultUnlockedRoute(
                        activeUserId = ACTIVE_USER_ID,
                        backStack = listOf(VaultUnlockedGraph.NavBarRoute),
                    ),
                ),
                navigationController.backStack.toList(),
            )
        }
    }

    @Suppress("LongMethod")
    @Test
    fun `toRoute should return the route of the section for each state`() {
        fun assertRouteForState(state: RootNavState, expectedRoute: RootNavGraph) {
            assertEquals(expectedRoute, state.toRoute())
        }

        assertRouteForState(
            state = RootNavState.Auth,
            expectedRoute = RootNavGraph.AuthRoute(backStack = listOf(AuthGraph.LandingRoute)),
        )
        assertRouteForState(
            state = RootNavState.AuthWithWelcome,
            expectedRoute = RootNavGraph.AuthRoute(backStack = listOf(AuthGraph.WelcomeRoute)),
        )
        assertRouteForState(
            state = RootNavState.CompleteOngoingRegistration(
                email = "example@email.com",
                verificationToken = "verificationToken",
                fromEmail = true,
                timestamp = FIXED_CLOCK.millis(),
            ),
            expectedRoute = RootNavGraph.AuthRoute(
                backStack = listOf(
                    AuthGraph.LandingRoute,
                    AuthGraph.CompleteRegistrationRoute(
                        emailAddress = "example@email.com",
                        verificationToken = "verificationToken",
                        fromEmail = true,
                    ),
                ),
            ),
        )
        assertRouteForState(
            state = RootNavState.ExpiredRegistrationLink,
            expectedRoute = RootNavGraph.AuthRoute(
                backStack = listOf(AuthGraph.LandingRoute, AuthGraph.ExpiredRegistrationLinkRoute),
            ),
        )
        assertRouteForState(
            state = RootNavState.VaultLocked,
            expectedRoute = RootNavGraph.AuthRoute(
                backStack = listOf(AuthGraph.VaultUnlockRoute(unlockType = UnlockType.STANDARD)),
            ),
        )
        assertRouteForState(
            state = RootNavState.SetPassword,
            expectedRoute = RootNavGraph.AuthRoute(backStack = listOf(AuthGraph.SetPasswordRoute)),
        )
        assertRouteForState(
            state = RootNavState.TrustedDevice,
            expectedRoute = RootNavGraph.AuthRoute(
                backStack = listOf(AuthGraph.TrustedDeviceRoute),
            ),
        )
        assertRouteForState(
            state = RootNavState.ResetPassword,
            expectedRoute = RootNavGraph.ResetPasswordRoute,
        )
        assertRouteForState(
            state = RootNavState.RemovePassword,
            expectedRoute = RootNavGraph.RemovePasswordRoute,
        )
        assertRouteForState(
            state = RootNavState.MigrateToMyItems,
            expectedRoute = RootNavGraph.MigrateToMyItemsRoute,
        )
        assertRouteForState(
            state = RootNavState.VaultUnlocked(activeUserId = ACTIVE_USER_ID),
            expectedRoute = vaultUnlockedRoute(route = VaultUnlockedGraph.NavBarRoute),
        )
        assertRouteForState(
            state = RootNavState.VaultUnlockedForNewTotp(activeUserId = ACTIVE_USER_ID),
            expectedRoute = vaultUnlockedRoute(route = LOGIN_ITEM_LISTING_ROUTE),
        )
        listOf(SendItemType.FILE, SendItemType.TEXT).forEach { sendType ->
            assertRouteForState(
                state = RootNavState.VaultUnlockedForNewSend(
                    activeUserId = ACTIVE_USER_ID,
                    sendType = sendType,
                ),
                expectedRoute = vaultUnlockedRoute(
                    route = VaultUnlockedGraph.AddEditSendRoute(
                        sendType = sendType,
                        modeType = ModeType.ADD,
                    ),
                ),
            )
        }
        assertRouteForState(
            state = RootNavState.VaultUnlockedForAutofillSave(
                activeUserId = ACTIVE_USER_ID,
                autofillSaveItem = mockk<AutofillSaveItem.Login>(),
            ),
            expectedRoute = vaultUnlockedRoute(
                route = VaultUnlockedGraph.VaultAddEditRoute(
                    vaultAddEditType = VaultAddEditType.AddItem(),
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            ),
        )
        assertRouteForState(
            state = RootNavState.VaultUnlockedForAutofillSave(
                activeUserId = ACTIVE_USER_ID,
                autofillSaveItem = mockk<AutofillSaveItem.Card>(),
            ),
            expectedRoute = vaultUnlockedRoute(
                route = VaultUnlockedGraph.VaultAddEditRoute(
                    vaultAddEditType = VaultAddEditType.AddItem(),
                    vaultItemCipherType = VaultItemCipherType.CARD,
                ),
            ),
        )
        assertRouteForState(
            state = RootNavState.VaultUnlockedForAutofillSelection(
                activeUserId = ACTIVE_USER_ID,
                type = AutofillSelectionData.Type.LOGIN,
            ),
            expectedRoute = vaultUnlockedRoute(route = LOGIN_ITEM_LISTING_ROUTE),
        )
        assertRouteForState(
            state = RootNavState.VaultUnlockedForCreatePasswordRequest(
                activeUserId = ACTIVE_USER_ID,
                username = "mockUsername",
                password = "mockPassword",
                uri = "mockUri",
            ),
            expectedRoute = vaultUnlockedRoute(
                route = VaultUnlockedGraph.VaultAddEditRoute(
                    vaultAddEditType = VaultAddEditType.AddItem(),
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                ),
            ),
        )
        assertRouteForState(
            state = RootNavState.VaultUnlockedForAuthRequest(activeUserId = ACTIVE_USER_ID),
            expectedRoute = vaultUnlockedRoute(
                route = VaultUnlockedGraph.LoginApprovalRoute(fingerprint = null),
            ),
        )
        listOf(
            RootNavState.VaultUnlockedForFido2Save(
                activeUserId = ACTIVE_USER_ID,
                createCredentialRequest = mockk(),
            ),
            RootNavState.VaultUnlockedForFido2Assertion(
                activeUserId = ACTIVE_USER_ID,
                fido2CredentialAssertionRequest = mockk(),
            ),
            RootNavState.VaultUnlockedForPasswordGet(
                activeUserId = ACTIVE_USER_ID,
                providerGetPasswordCredentialRequest = mockk(),
            ),
            RootNavState.VaultUnlockedForProviderGetCredentials(
                activeUserId = ACTIVE_USER_ID,
                getCredentialsRequest = mockk(),
            ),
        )
            .forEach { state ->
                assertRouteForState(
                    state = state,
                    expectedRoute = vaultUnlockedRoute(route = LOGIN_ITEM_LISTING_ROUTE),
                )
            }
        assertRouteForState(
            state = RootNavState.OnboardingAccountLockSetup,
            expectedRoute = accountSetupRoute(AccountSetupGraph.SetupUnlockRoute),
        )
        assertRouteForState(
            state = RootNavState.OnboardingAutoFillSetup,
            expectedRoute = accountSetupRoute(AccountSetupGraph.SetupAutofillRoute),
        )
        assertRouteForState(
            state = RootNavState.OnboardingBrowserAutofillSetup,
            expectedRoute = accountSetupRoute(AccountSetupGraph.SetupBrowserAutofillRoute),
        )
        assertRouteForState(
            state = RootNavState.OnboardingStepsComplete,
            expectedRoute = accountSetupRoute(AccountSetupGraph.SetupCompleteRoute),
        )
        assertRouteForState(
            state = RootNavState.CredentialExchangeExport,
            expectedRoute = RootNavGraph.ExportItemsRoute(
                backStack = listOf(ExportItemsGraph.SelectAccountRoute),
            ),
        )
        assertRouteForState(
            state = RootNavState.CredentialExchangeExportSkipAccountSelection(
                userId = ACTIVE_USER_ID,
            ),
            expectedRoute = RootNavGraph.ExportItemsRoute(
                backStack = listOf(
                    ExportItemsGraph.VerifyPasswordRoute(
                        userId = ACTIVE_USER_ID,
                        hasOtherAccounts = false,
                    ),
                ),
            ),
        )
        assertRouteForState(
            state = RootNavState.Splash,
            expectedRoute = RootNavGraph.SplashRoute,
        )
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)

private const val ACTIVE_USER_ID: String = "active_user_id"

private val LOGIN_ITEM_LISTING_ROUTE: VaultUnlockedGraph = VaultUnlockedGraph.VaultItemListingRoute(
    vaultItemListingType = VaultItemListingType.Login,
)

private fun accountSetupRoute(
    route: AccountSetupGraph,
): RootNavGraph = RootNavGraph.AccountSetupRoute(
    backStack = listOf(route),
)

private fun vaultUnlockedRoute(
    activeUserId: String = ACTIVE_USER_ID,
    route: VaultUnlockedGraph,
): RootNavGraph = RootNavGraph.VaultUnlockedRoute(
    activeUserId = activeUserId,
    backStack = listOf(route),
)
