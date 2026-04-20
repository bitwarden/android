package com.x8bit.bitwarden.ui.platform.feature.premium.plan

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.intent.model.AuthTabData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.billing.repository.BillingRepository
import com.x8bit.bitwarden.data.billing.repository.model.CheckoutSessionResult
import com.x8bit.bitwarden.data.billing.repository.model.PremiumPlanPricingResult
import com.x8bit.bitwarden.data.billing.util.PremiumCheckoutCallbackResult
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class PlanViewModelTest : BaseViewModelTest() {

    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val mockAuthRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val mockBillingRepository: BillingRepository = mockk()
    private val mutableSpecialCircumstanceStateFlow =
        MutableStateFlow<SpecialCircumstance?>(null)
    private val mockSpecialCircumstanceManager: SpecialCircumstanceManager =
        mockk(relaxed = true) {
            every { specialCircumstance } returns mutableSpecialCircumstanceStateFlow.value
            every { specialCircumstanceStateFlow } returns mutableSpecialCircumstanceStateFlow
        }
    private val mockVaultRepository: VaultRepository = mockk {
        coEvery {
            syncForResult(any())
        } returns SyncVaultDataResult.Success(itemsAvailable = true)
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
    fun `PremiumCheckoutResult with isSuccess false should show WaitingForPayment when not premium`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_FREE_STATE, awaitItem())

                mutableSpecialCircumstanceStateFlow.value =
                    SpecialCircumstance.PremiumCheckout(
                        callbackResult = PremiumCheckoutCallbackResult.Canceled,
                    )

                assertEquals(
                    DEFAULT_FREE_STATE.copy(
                        viewState = PlanState.ViewState.Free(
                            rate = "$1.67",
                            checkoutUrl = null,
                            isAwaitingPremiumStatus = true,
                        ),
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
    fun `PremiumCheckoutResult with isSuccess true should show snackbar when premium`() =
        runTest {
            mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                accounts = listOf(
                    DEFAULT_ACCOUNT.copy(isPremium = true),
                ),
            )

            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                mutableSpecialCircumstanceStateFlow.value =
                    SpecialCircumstance.PremiumCheckout(
                        callbackResult = PremiumCheckoutCallbackResult.Success,
                    )

                assertEquals(
                    PlanEvent.ShowSnackbar(
                        data = BitwardenSnackbarData(
                            message = BitwardenString.upgraded_to_premium.asText(),
                        ),
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
    fun `UpgradeNowClick should transition to Loading then dismiss dialog and emit LaunchBrowser on success`() =
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
                            rate = "$1.67",
                            checkoutUrl = checkoutUrl,
                            isAwaitingPremiumStatus = false,
                        ),
                        dialogState = null,
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
    fun `RetryClick should transition to Loading then dismiss dialog and emit LaunchBrowser on success`() =
        runTest {
            val checkoutUrl = "https://checkout.stripe.com/session123"
            coEvery {
                mockBillingRepository.getCheckoutSessionUrl()
            } returns CheckoutSessionResult.Success(url = checkoutUrl)

            val viewModel = createViewModel(
                initialState = DEFAULT_FREE_STATE.copy(
                    dialogState = PlanState.DialogState.CheckoutError,
                ),
                pricingResult = null,
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
                            rate = "$1.67",
                            checkoutUrl = checkoutUrl,
                            isAwaitingPremiumStatus = false,
                        ),
                        dialogState = null,
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
            pricingResult = null,
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
                pricingResult = null,
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
                rate = "$1.67",
                checkoutUrl = checkoutUrl,
                isAwaitingPremiumStatus = false,
            )
            val viewModel = createViewModel(
                initialState = DEFAULT_FREE_STATE.copy(
                    viewState = freeState,
                    dialogState = PlanState.DialogState.WaitingForPayment,
                ),
                pricingResult = null,
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
                    dialogState = PlanState.DialogState.WaitingForPayment,
                ),
                pricingResult = null,
            )

            viewModel.eventFlow.test {
                viewModel.trySendAction(PlanAction.GoBackClick)
                expectNoEvents()
            }
        }

    @Test
    fun `premium status flip should show snackbar when in WaitingForPayment`() =
        runTest {
            val viewModel = createViewModel(
                initialState = DEFAULT_FREE_STATE.copy(
                    viewState = PlanState.ViewState.Free(
                        rate = "$1.67",
                        checkoutUrl = null,
                        isAwaitingPremiumStatus = true,
                    ),
                    dialogState = PlanState.DialogState.WaitingForPayment,
                ),
                pricingResult = null,
            )

            viewModel.eventFlow.test {
                // Simulate premium status flip while WaitingForPayment.
                mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                    accounts = listOf(
                        DEFAULT_ACCOUNT.copy(isPremium = true),
                    ),
                )

                assertEquals(
                    PlanEvent.ShowSnackbar(
                        data = BitwardenSnackbarData(
                            message = BitwardenString.upgraded_to_premium.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `premium status flip via canceled special circumstance should show snackbar`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                assertEquals(DEFAULT_FREE_STATE, stateFlow.awaitItem())

                // Simulate returning from checkout canceled (isSuccess = false),
                // then premium status updates.
                mutableSpecialCircumstanceStateFlow.value =
                    SpecialCircumstance.PremiumCheckout(
                        callbackResult = PremiumCheckoutCallbackResult.Canceled,
                    )

                assertEquals(
                    DEFAULT_FREE_STATE.copy(
                        viewState = PlanState.ViewState.Free(
                            rate = "$1.67",
                            checkoutUrl = null,
                            isAwaitingPremiumStatus = true,
                        ),
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

                // State clears dialog and isAwaitingPremiumStatus.
                assertEquals(
                    DEFAULT_FREE_STATE,
                    stateFlow.awaitItem(),
                )

                assertEquals(
                    PlanEvent.ShowSnackbar(
                        data = BitwardenSnackbarData(
                            message = BitwardenString.upgraded_to_premium.asText(),
                        ),
                    ),
                    eventFlow.awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `PremiumCheckoutResult with isSuccess true should trigger sync and show PendingUpgrade when not premium`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_FREE_STATE, awaitItem())

                mutableSpecialCircumstanceStateFlow.value =
                    SpecialCircumstance.PremiumCheckout(
                        callbackResult = PremiumCheckoutCallbackResult.Success,
                    )

                // Loading while sync is in progress.
                awaitItem()

                // Sync completes without premium — PendingUpgrade shown.
                assertEquals(
                    DEFAULT_FREE_STATE.copy(
                        viewState = PlanState.ViewState.Free(
                            rate = "$1.67",
                            checkoutUrl = null,
                            isAwaitingPremiumStatus = true,
                        ),
                        dialogState = PlanState.DialogState.PendingUpgrade,
                    ),
                    awaitItem(),
                )
            }

            verify {
                mockSpecialCircumstanceManager.specialCircumstance = null
            }
            coVerify {
                mockVaultRepository.syncForResult(forced = true)
            }
        }

    @Test
    fun `UserStateUpdateReceive with premium during Loading should show snackbar`() =
        runTest {
            val viewModel = createViewModel(
                initialState = DEFAULT_FREE_STATE.copy(
                    viewState = PlanState.ViewState.Free(
                        rate = "$1.67",
                        checkoutUrl = null,
                        isAwaitingPremiumStatus = true,
                    ),
                    dialogState = PlanState.DialogState.Loading(
                        message = BitwardenString.confirming_your_upgrade.asText(),
                    ),
                ),
                pricingResult = null,
            )

            viewModel.eventFlow.test {
                mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                    accounts = listOf(
                        DEFAULT_ACCOUNT.copy(isPremium = true),
                    ),
                )

                assertEquals(
                    PlanEvent.ShowSnackbar(
                        data = BitwardenSnackbarData(
                            message = BitwardenString.upgraded_to_premium.asText(),
                        ),
                    ),
                    awaitItem(),
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
        val viewModel = createViewModel(
            initialState = savedState,
            pricingResult = null,
        )

        viewModel.stateFlow.test {
            assertEquals(savedState, awaitItem())
        }
    }

    // endregion Free user path

    // region Pricing fetch

    @Test
    fun `initial state before pricing fetch resolves should show Loading dialog`() =
        runTest {
            val viewModel = createViewModel(pricingResult = null)

            viewModel.stateFlow.test {
                assertEquals(
                    PlanState(
                        planMode = PlanMode.Modal,
                        viewState = PlanState.ViewState.Free(
                            rate = "--",
                            checkoutUrl = null,
                            isAwaitingPremiumStatus = false,
                        ),
                        dialogState = PlanState.DialogState.Loading(
                            message = BitwardenString.loading.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `pricing fetch failure should show GetPricingError dialog`() =
        runTest {
            val viewModel = createViewModel(
                pricingResult = PremiumPlanPricingResult.Error(
                    error = RuntimeException("Network error"),
                ),
            )

            viewModel.stateFlow.test {
                assertEquals(
                    PlanState(
                        planMode = PlanMode.Modal,
                        viewState = PlanState.ViewState.Free(
                            rate = "--",
                            checkoutUrl = null,
                            isAwaitingPremiumStatus = false,
                        ),
                        dialogState = PlanState.DialogState.GetPricingError(
                            title = BitwardenString.pricing_unavailable.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `RetryPricingClick should transition to Loading then Free on success`() =
        runTest {
            val viewModel = createViewModel(
                pricingResult = PremiumPlanPricingResult.Error(
                    error = RuntimeException("Network error"),
                ),
            )

            viewModel.stateFlow.test {
                assertEquals(
                    PlanState(
                        planMode = PlanMode.Modal,
                        viewState = PlanState.ViewState.Free(
                            rate = "--",
                            checkoutUrl = null,
                            isAwaitingPremiumStatus = false,
                        ),
                        dialogState = PlanState.DialogState.GetPricingError(
                            title = BitwardenString.pricing_unavailable.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    ),
                    awaitItem(),
                )

                // Override mock for retry to return success.
                coEvery {
                    mockBillingRepository.getPremiumPlanPricing()
                } returns DEFAULT_PRICING_SUCCESS

                viewModel.trySendAction(PlanAction.RetryPricingClick)

                assertEquals(
                    PlanState(
                        planMode = PlanMode.Modal,
                        viewState = PlanState.ViewState.Free(
                            rate = "--",
                            checkoutUrl = null,
                            isAwaitingPremiumStatus = false,
                        ),
                        dialogState = PlanState.DialogState.Loading(
                            message = BitwardenString.loading.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_FREE_STATE,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `ClosePricingErrorClick should clear dialog and emit NavigateBack`() =
        runTest {
            val viewModel = createViewModel(
                pricingResult = PremiumPlanPricingResult.Error(
                    error = RuntimeException("Network error"),
                ),
            )

            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                assertEquals(
                    PlanState(
                        planMode = PlanMode.Modal,
                        viewState = PlanState.ViewState.Free(
                            rate = "--",
                            checkoutUrl = null,
                            isAwaitingPremiumStatus = false,
                        ),
                        dialogState = PlanState.DialogState.GetPricingError(
                            title = BitwardenString.pricing_unavailable.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    ),
                    stateFlow.awaitItem(),
                )

                viewModel.trySendAction(PlanAction.ClosePricingErrorClick)

                assertEquals(
                    PlanState(
                        planMode = PlanMode.Modal,
                        viewState = PlanState.ViewState.Free(
                            rate = "--",
                            checkoutUrl = null,
                            isAwaitingPremiumStatus = false,
                        ),
                        dialogState = null,
                    ),
                    stateFlow.awaitItem(),
                )
                assertEquals(
                    PlanEvent.NavigateBack,
                    eventFlow.awaitItem(),
                )
            }
        }

    // endregion Pricing fetch

    private fun createViewModel(
        initialState: PlanState? = null,
        planMode: PlanMode = PlanMode.Modal,
        pricingResult: PremiumPlanPricingResult? = DEFAULT_PRICING_SUCCESS,
    ): PlanViewModel {
        coEvery {
            mockBillingRepository.getPremiumPlanPricing()
        } coAnswers {
            pricingResult ?: awaitCancellation()
        }
        val savedStateHandle = SavedStateHandle().apply {
            set("state", initialState)
            every { toPlanArgs() } returns PlanArgs(planMode = planMode)
        }
        return PlanViewModel(
            savedStateHandle = savedStateHandle,
            authRepository = mockAuthRepository,
            billingRepository = mockBillingRepository,
            specialCircumstanceManager = mockSpecialCircumstanceManager,
            vaultRepository = mockVaultRepository,
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
        rate = "$1.67",
        checkoutUrl = null,
        isAwaitingPremiumStatus = false,
    ),
    dialogState = null,
)

private const val ANNUAL_PRICE = 19.99
private val DEFAULT_PRICING_SUCCESS = PremiumPlanPricingResult.Success(
    annualPrice = ANNUAL_PRICE,
)
