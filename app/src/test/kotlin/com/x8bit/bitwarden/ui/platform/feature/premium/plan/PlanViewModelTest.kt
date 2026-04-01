package com.x8bit.bitwarden.ui.platform.feature.premium.plan

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.intent.model.AuthTabData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.billing.repository.BillingRepository
import com.x8bit.bitwarden.data.billing.repository.model.CheckoutSessionResult
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlanViewModelTest : BaseViewModelTest() {

    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val mockAuthRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val mockBillingRepository: BillingRepository = mockk()
    private val mockSnackbarRelayManager: SnackbarRelayManager<SnackbarRelay> =
        mockk {
            every { sendSnackbarData(any(), any()) } just runs
        }
    private val mockSpecialCircumstanceManager: SpecialCircumstanceManager =
        mockk(relaxed = true) {
            every { specialCircumstance } returns null
        }

    @BeforeEach
    fun setup() {
        mockkStatic(SavedStateHandle::toPlanArgs)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SavedStateHandle::toPlanArgs)
    }

    // region PlanMode propagation

    @Test
    fun `initial state should use Standard PlanMode for Standard route`() =
        runTest {
            val viewModel = createViewModel(planMode = PlanMode.Standard)

            viewModel.stateFlow.test {
                assertEquals(PlanMode.Standard, awaitItem().planMode)
            }
        }

    @Test
    fun `initial state should use Modal PlanMode for Modal route`() =
        runTest {
            val viewModel = createViewModel(planMode = PlanMode.Modal)

            viewModel.stateFlow.test {
                assertEquals(PlanMode.Modal, awaitItem().planMode)
            }
        }

    // endregion PlanMode propagation

    // region Free user path

    @Test
    fun `initial state should be Free ViewState after pricing fetch`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_FREE_STATE, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `initial state should be WaitingForPayment when PremiumCheckoutResult special circumstance present`() =
        runTest {
            every {
                mockSpecialCircumstanceManager.specialCircumstance
            } returns SpecialCircumstance.PremiumCheckoutResult

            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_FREE_STATE.copy(
                        dialogState = PlanState.DialogState.WaitingForPayment,
                    ),
                    awaitItem(),
                )
            }

            verify {
                mockSpecialCircumstanceManager.specialCircumstance = null
            }
        }

    @Test
    fun `BackClick should emit NavigateBack event`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(PlanAction.BackClick)
            assertEquals(PlanEvent.NavigateBack, awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UpgradeNowClick should transition to Loading then emit LaunchBrowser and WaitingForPayment on success`() =
        runTest {
            val checkoutUrl = "https://checkout.stripe.com/session123"
            coEvery {
                mockBillingRepository.getCheckoutSessionUrl()
            } returns CheckoutSessionResult.Success(url = checkoutUrl)

            val viewModel = createViewModel()

            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                assertEquals(DEFAULT_FREE_STATE, stateFlow.awaitItem())

                viewModel.trySendAction(PlanAction.UpgradeNowClick)

                assertEquals(
                    DEFAULT_FREE_STATE.copy(
                        dialogState = PlanState.DialogState.Loading(
                            message = BitwardenString.opening_checkout.asText(),
                        ),
                    ),
                    stateFlow.awaitItem(),
                )
                assertEquals(
                    DEFAULT_FREE_STATE.copy(
                        viewState = PlanState.ViewState.Free(
                            rate = "$1.65",
                            checkoutUrl = checkoutUrl,
                        ),
                        dialogState = PlanState.DialogState.WaitingForPayment,
                    ),
                    stateFlow.awaitItem(),
                )
                assertEquals(
                    PlanEvent.LaunchBrowser(
                        url = checkoutUrl,
                        authTabData = AuthTabData.CustomScheme(
                            callbackUrl = PREMIUM_CHECKOUT_CALLBACK_URL,
                        ),
                    ),
                    eventFlow.awaitItem(),
                )
            }
        }

    @Test
    fun `UpgradeNowClick should transition to Loading then Error on failure`() =
        runTest {
            coEvery {
                mockBillingRepository.getCheckoutSessionUrl()
            } returns CheckoutSessionResult.Error(
                error = RuntimeException("Network error"),
            )

            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_FREE_STATE, awaitItem())

                viewModel.trySendAction(PlanAction.UpgradeNowClick)

                assertEquals(
                    DEFAULT_FREE_STATE.copy(
                        dialogState = PlanState.DialogState.Loading(
                            message = BitwardenString.opening_checkout.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_FREE_STATE.copy(
                        dialogState = PlanState.DialogState.CheckoutError,
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `RetryClick should transition to Loading then emit LaunchBrowser and WaitingForPayment on success`() =
        runTest {
            val checkoutUrl = "https://checkout.stripe.com/session123"
            coEvery {
                mockBillingRepository.getCheckoutSessionUrl()
            } returns CheckoutSessionResult.Success(url = checkoutUrl)

            val viewModel = createViewModel(
                initialState = DEFAULT_FREE_STATE.copy(
                    dialogState = PlanState.DialogState.CheckoutError,
                ),
            )

            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                assertEquals(
                    DEFAULT_FREE_STATE.copy(
                        dialogState = PlanState.DialogState.CheckoutError,
                    ),
                    stateFlow.awaitItem(),
                )

                viewModel.trySendAction(PlanAction.RetryClick)

                assertEquals(
                    DEFAULT_FREE_STATE.copy(
                        dialogState = PlanState.DialogState.Loading(
                            message = BitwardenString.opening_checkout.asText(),
                        ),
                    ),
                    stateFlow.awaitItem(),
                )
                assertEquals(
                    DEFAULT_FREE_STATE.copy(
                        viewState = PlanState.ViewState.Free(
                            rate = "$1.65",
                            checkoutUrl = checkoutUrl,
                        ),
                        dialogState = PlanState.DialogState.WaitingForPayment,
                    ),
                    stateFlow.awaitItem(),
                )
                assertEquals(
                    PlanEvent.LaunchBrowser(
                        url = checkoutUrl,
                        authTabData = AuthTabData.CustomScheme(
                            callbackUrl = PREMIUM_CHECKOUT_CALLBACK_URL,
                        ),
                    ),
                    eventFlow.awaitItem(),
                )
            }
        }

    @Test
    fun `DismissError should transition from Error to Content`() = runTest {
        val viewModel = createViewModel(
            initialState = DEFAULT_FREE_STATE.copy(
                dialogState = PlanState.DialogState.CheckoutError,
            ),
        )

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_FREE_STATE.copy(
                    dialogState = PlanState.DialogState.CheckoutError,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(PlanAction.DismissError)

            assertEquals(DEFAULT_FREE_STATE, awaitItem())
        }
    }

    @Test
    fun `CancelWaiting should transition from WaitingForPayment to Content`() =
        runTest {
            val viewModel = createViewModel(
                initialState = DEFAULT_FREE_STATE.copy(
                    dialogState = PlanState.DialogState.WaitingForPayment,
                ),
            )

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_FREE_STATE.copy(
                        dialogState = PlanState.DialogState.WaitingForPayment,
                    ),
                    awaitItem(),
                )

                viewModel.trySendAction(PlanAction.CancelWaiting)

                assertEquals(DEFAULT_FREE_STATE, awaitItem())
            }
        }

    @Test
    fun `GoBackClick should emit LaunchBrowser with checkout URL when URL is available`() =
        runTest {
            val checkoutUrl = "https://checkout.stripe.com/session123"
            val freeState = PlanState.ViewState.Free(
                rate = "$1.65",
                checkoutUrl = checkoutUrl,
            )
            val viewModel = createViewModel(
                initialState = DEFAULT_FREE_STATE.copy(
                    viewState = freeState,
                    dialogState = PlanState.DialogState.WaitingForPayment,
                ),
            )

            viewModel.eventFlow.test {
                viewModel.trySendAction(PlanAction.GoBackClick)
                assertEquals(
                    PlanEvent.LaunchBrowser(
                        url = checkoutUrl,
                        authTabData = AuthTabData.CustomScheme(
                            callbackUrl = PREMIUM_CHECKOUT_CALLBACK_URL,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `GoBackClick should not emit event when checkout URL is null`() =
        runTest {
            val viewModel = createViewModel(
                initialState = DEFAULT_FREE_STATE.copy(
                    dialogState = PlanState.DialogState
                        .WaitingForPayment,
                ),
            )

            viewModel.eventFlow.test {
                viewModel.trySendAction(PlanAction.GoBackClick)
                expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `premium status flip should send snackbar and emit NavigateBack when in WaitingForPayment`() =
        runTest {
            val checkoutUrl = "https://checkout.stripe.com/session123"
            coEvery {
                mockBillingRepository.getCheckoutSessionUrl()
            } returns CheckoutSessionResult.Success(url = checkoutUrl)

            val viewModel = createViewModel()

            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                assertEquals(DEFAULT_FREE_STATE, stateFlow.awaitItem())

                viewModel.trySendAction(PlanAction.UpgradeNowClick)

                // Consume Loading and WaitingForPayment states.
                stateFlow.awaitItem()
                stateFlow.awaitItem()
                assertEquals(
                    PlanEvent.LaunchBrowser(
                        url = checkoutUrl,
                        authTabData = AuthTabData.CustomScheme(
                            callbackUrl = PREMIUM_CHECKOUT_CALLBACK_URL,
                        ),
                    ),
                    eventFlow.awaitItem(),
                )

                // Simulate premium status flip.
                mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                    accounts = listOf(
                        DEFAULT_ACCOUNT.copy(isPremium = true),
                    ),
                )

                assertEquals(
                    PlanEvent.NavigateBack,
                    eventFlow.awaitItem(),
                )
            }

            verify {
                mockSnackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(
                        message = BitwardenString.upgraded_to_premium.asText(),
                    ),
                    relay = SnackbarRelay.PREMIUM_UPGRADED,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `premium status flip should send snackbar and emit NavigateBack via special circumstance`() =
        runTest {
            every {
                mockSpecialCircumstanceManager.specialCircumstance
            } returns SpecialCircumstance.PremiumCheckoutResult

            val viewModel = createViewModel()

            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                assertEquals(
                    DEFAULT_FREE_STATE.copy(
                        dialogState = PlanState.DialogState.WaitingForPayment,
                    ),
                    stateFlow.awaitItem(),
                )

                // Simulate premium status flip.
                mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                    accounts = listOf(
                        DEFAULT_ACCOUNT.copy(isPremium = true),
                    ),
                )

                assertEquals(
                    PlanEvent.NavigateBack,
                    eventFlow.awaitItem(),
                )
            }

            verify {
                mockSnackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(
                        message = BitwardenString.upgraded_to_premium.asText(),
                    ),
                    relay = SnackbarRelay.PREMIUM_UPGRADED,
                )
            }
        }

    @Test
    fun `premium status flip should not emit event when not in WaitingForPayment`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                // Flip premium while in Content state.
                mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                    accounts = listOf(
                        DEFAULT_ACCOUNT.copy(isPremium = true),
                    ),
                )
                expectNoEvents()
            }
        }

    @Test
    fun `saved state should be restored`() = runTest {
        val savedState = DEFAULT_FREE_STATE.copy(
            dialogState = PlanState.DialogState.CheckoutError,
        )
        val viewModel = createViewModel(initialState = savedState)

        viewModel.stateFlow.test {
            assertEquals(savedState, awaitItem())
        }
    }

    // endregion Free user path

    private fun createViewModel(
        initialState: PlanState? = null,
        planMode: PlanMode = PlanMode.Modal,
    ): PlanViewModel {
        val savedStateHandle = SavedStateHandle().apply {
            set("state", initialState)
            every { toPlanArgs() } returns PlanArgs(planMode = planMode)
        }
        return PlanViewModel(
            savedStateHandle = savedStateHandle,
            authRepository = mockAuthRepository,
            billingRepository = mockBillingRepository,
            snackbarRelayManager = mockSnackbarRelayManager,
            specialCircumstanceManager = mockSpecialCircumstanceManager,
        )
    }
}

private val DEFAULT_ACCOUNT = UserState.Account(
    userId = "user-id-1",
    name = "Test User",
    email = "test@bitwarden.com",
    avatarColorHex = "#000000",
    environment = mockk(),
    isPremium = false,
    isLoggedIn = true,
    isVaultUnlocked = true,
    needsPasswordReset = false,
    isBiometricsEnabled = false,
    organizations = emptyList(),
    needsMasterPassword = false,
    trustedDevice = null,
    hasMasterPassword = true,
    isUsingKeyConnector = false,
    onboardingStatus = mockk(),
    firstTimeState = mockk(),
    isExportable = false,
    creationDate = null,
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = "user-id-1",
    accounts = listOf(DEFAULT_ACCOUNT),
    hasPendingAccountAddition = false,
)

private val DEFAULT_FREE_STATE = PlanState(
    planMode = PlanMode.Modal,
    viewState = PlanState.ViewState.Free(
        rate = "$1.65",
    ),
    dialogState = null,
)
