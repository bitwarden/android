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
import com.x8bit.bitwarden.data.billing.repository.model.CustomerPortalResult
import com.x8bit.bitwarden.data.billing.repository.model.PlanCadence
import com.x8bit.bitwarden.data.billing.repository.model.PremiumPlanPricingResult
import com.x8bit.bitwarden.data.billing.repository.model.PremiumSubscriptionStatus
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionInfo
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionResult
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
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

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
    fun `initial state before pricing fetch resolves should show placeholder rate`() =
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
                        dialogState = null,
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

    @Test
    fun `init should fetch pricing for Free viewstate`() = runTest {
        createViewModel()

        coVerify(exactly = 1) {
            mockBillingRepository.getPremiumPlanPricing()
        }
    }

    @Test
    fun `init should not fetch pricing for Premium viewstate`() = runTest {
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(DEFAULT_ACCOUNT.copy(isPremium = true)),
        )

        createViewModel()

        coVerify(exactly = 0) {
            mockBillingRepository.getPremiumPlanPricing()
        }
    }

    // endregion Pricing fetch

    // region Premium user path

    @Test
    fun `initial state should be Premium ViewState with loading dialog for premium user`() =
        runTest {
            markUserPremium()

            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_PREMIUM_LOADING_STATE, awaitItem())
            }
        }

    @Test
    fun `init should fetch subscription for Premium viewstate`() = runTest {
        markUserPremium()

        createViewModel(subscriptionResult = SUBSCRIPTION_SUCCESS_ACTIVE)

        coVerify(exactly = 1) {
            mockBillingRepository.getSubscription()
        }
    }

    @Test
    fun `init should not fetch subscription for Free viewstate`() = runTest {
        createViewModel()

        coVerify(exactly = 0) {
            mockBillingRepository.getSubscription()
        }
    }

    @Test
    fun `SubscriptionResultReceive Success should populate Premium state from SubscriptionInfo`() =
        runTest {
            markUserPremium()

            val viewModel = createViewModel(
                subscriptionResult = SUBSCRIPTION_SUCCESS_ACTIVE,
            )

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_PREMIUM_LOADED_STATE, awaitItem())
            }
        }

    @Test
    fun `SubscriptionResultReceive Success with Canceled status should hide cancel button`() =
        runTest {
            markUserPremium()

            val viewModel = createViewModel(
                subscriptionResult = SubscriptionResult.Success(
                    subscription = SUBSCRIPTION_INFO_ACTIVE.copy(
                        status = PremiumSubscriptionStatus.CANCELED,
                        canceledDate = Instant.parse("2026-04-21T00:00:00Z"),
                    ),
                ),
            )

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_PREMIUM_LOADED_STATE.copy(
                        viewState = DEFAULT_PREMIUM_ACTIVE_VIEW_STATE.copy(
                            status = PremiumSubscriptionStatus.CANCELED,
                            descriptionText = BitwardenString
                                .subscription_canceled_description
                                .asText("April 21, 2026"),
                            showCancelButton = false,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `SubscriptionResultReceive Success with OverduePayment status should describe overdue`() =
        runTest {
            markUserPremium()

            val viewModel = createViewModel(
                subscriptionResult = SubscriptionResult.Success(
                    subscription = SUBSCRIPTION_INFO_ACTIVE.copy(
                        status = PremiumSubscriptionStatus.OVERDUE_PAYMENT,
                        suspensionDate = Instant.parse("2026-04-21T00:00:00Z"),
                    ),
                ),
            )

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_PREMIUM_LOADED_STATE.copy(
                        viewState = DEFAULT_PREMIUM_ACTIVE_VIEW_STATE.copy(
                            status = PremiumSubscriptionStatus.OVERDUE_PAYMENT,
                            descriptionText = BitwardenString
                                .subscription_overdue_description
                                .asText("April 21, 2026"),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `SubscriptionResultReceive Success with PastDue status should describe grace period`() =
        runTest {
            markUserPremium()

            val viewModel = createViewModel(
                subscriptionResult = SubscriptionResult.Success(
                    subscription = SUBSCRIPTION_INFO_ACTIVE.copy(
                        status = PremiumSubscriptionStatus.PAST_DUE,
                        suspensionDate = Instant.parse("2026-04-21T00:00:00Z"),
                        gracePeriodDays = 7,
                    ),
                ),
            )

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_PREMIUM_LOADED_STATE.copy(
                        viewState = DEFAULT_PREMIUM_ACTIVE_VIEW_STATE.copy(
                            status = PremiumSubscriptionStatus.PAST_DUE,
                            descriptionText = BitwardenString
                                .subscription_past_due_description
                                .asText(7, "April 21, 2026"),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `SubscriptionResultReceive Success with PastDue and null gracePeriodDays uses fallback`() =
        runTest {
            markUserPremium()

            val viewModel = createViewModel(
                subscriptionResult = SubscriptionResult.Success(
                    subscription = SUBSCRIPTION_INFO_ACTIVE.copy(
                        status = PremiumSubscriptionStatus.PAST_DUE,
                        suspensionDate = Instant.parse("2026-04-21T00:00:00Z"),
                        gracePeriodDays = null,
                    ),
                ),
            )

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_PREMIUM_LOADED_STATE.copy(
                        viewState = DEFAULT_PREMIUM_ACTIVE_VIEW_STATE.copy(
                            status = PremiumSubscriptionStatus.PAST_DUE,
                            descriptionText = BitwardenString
                                .subscription_past_due_description
                                .asText(0, "April 21, 2026"),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `SubscriptionResultReceive Success with Paused status should describe paused`() =
        runTest {
            markUserPremium()

            val viewModel = createViewModel(
                subscriptionResult = SubscriptionResult.Success(
                    subscription = SUBSCRIPTION_INFO_ACTIVE.copy(
                        status = PremiumSubscriptionStatus.PAUSED,
                    ),
                ),
            )

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_PREMIUM_LOADED_STATE.copy(
                        viewState = DEFAULT_PREMIUM_ACTIVE_VIEW_STATE.copy(
                            status = PremiumSubscriptionStatus.PAUSED,
                            descriptionText = BitwardenString
                                .subscription_paused_description
                                .asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `SubscriptionResultReceive Success with Monthly cadence formats per-month rate`() =
        runTest {
            markUserPremium()

            val viewModel = createViewModel(
                subscriptionResult = SubscriptionResult.Success(
                    subscription = SUBSCRIPTION_INFO_ACTIVE.copy(
                        cadence = PlanCadence.MONTHLY,
                    ),
                ),
            )

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_PREMIUM_LOADED_STATE.copy(
                        viewState = DEFAULT_PREMIUM_ACTIVE_VIEW_STATE.copy(
                            billingAmountText = BitwardenString
                                .billing_rate_per_month
                                .asText("$19.80"),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `SubscriptionResultReceive Success with zero seatsCost shows placeholder rate`() =
        runTest {
            markUserPremium()

            val viewModel = createViewModel(
                subscriptionResult = SubscriptionResult.Success(
                    subscription = SUBSCRIPTION_INFO_ACTIVE.copy(
                        seatsCost = BigDecimal.ZERO,
                    ),
                ),
            )

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_PREMIUM_LOADED_STATE.copy(
                        viewState = DEFAULT_PREMIUM_ACTIVE_VIEW_STATE.copy(
                            billingAmountText = PLACEHOLDER.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `SubscriptionResultReceive Success with null line items shows placeholder text`() =
        runTest {
            markUserPremium()

            val viewModel = createViewModel(
                subscriptionResult = SubscriptionResult.Success(
                    subscription = SUBSCRIPTION_INFO_ACTIVE.copy(
                        storageCost = null,
                        discountAmount = null,
                    ),
                ),
            )

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_PREMIUM_LOADED_STATE.copy(
                        viewState = DEFAULT_PREMIUM_ACTIVE_VIEW_STATE.copy(
                            storageCostText = PLACEHOLDER,
                            discountAmountText = PLACEHOLDER,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `SubscriptionResultReceive Success with zero line items shows placeholder text`() =
        runTest {
            markUserPremium()

            val viewModel = createViewModel(
                subscriptionResult = SubscriptionResult.Success(
                    subscription = SUBSCRIPTION_INFO_ACTIVE.copy(
                        storageCost = BigDecimal.ZERO,
                        discountAmount = BigDecimal.ZERO,
                        estimatedTax = BigDecimal.ZERO,
                    ),
                ),
            )

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_PREMIUM_LOADED_STATE.copy(
                        viewState = DEFAULT_PREMIUM_ACTIVE_VIEW_STATE.copy(
                            storageCostText = PLACEHOLDER,
                            discountAmountText = PLACEHOLDER,
                            estimatedTaxText = PLACEHOLDER,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `SubscriptionResultReceive Success with null nextCharge shows placeholder date`() =
        runTest {
            markUserPremium()

            val viewModel = createViewModel(
                subscriptionResult = SubscriptionResult.Success(
                    subscription = SUBSCRIPTION_INFO_ACTIVE.copy(
                        nextCharge = null,
                    ),
                ),
            )

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_PREMIUM_LOADED_STATE.copy(
                        viewState = DEFAULT_PREMIUM_ACTIVE_VIEW_STATE.copy(
                            descriptionText = BitwardenString
                                .premium_next_charge_summary
                                .asText("$45.55", PLACEHOLDER),
                            nextChargeDateText = null,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `SubscriptionResultReceive Error should show SubscriptionError dialog`() = runTest {
        markUserPremium()

        val viewModel = createViewModel(
            subscriptionResult = SubscriptionResult.Error(error = RuntimeException("boom")),
        )

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_PREMIUM_LOADING_STATE.copy(
                    dialogState = PlanState.DialogState.SubscriptionError(
                        title = BitwardenString.subscription_error.asText(),
                        message = BitwardenString
                            .trouble_loading_subscription
                            .asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `RetrySubscriptionClick should transition to Loading then refetch subscription`() =
        runTest {
            markUserPremium()

            val viewModel = createViewModel(
                subscriptionResult = SUBSCRIPTION_SUCCESS_ACTIVE,
            )

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_PREMIUM_LOADED_STATE, awaitItem())

                viewModel.trySendAction(PlanAction.RetrySubscriptionClick)

                assertEquals(
                    DEFAULT_PREMIUM_LOADED_STATE.copy(
                        dialogState = PlanState.DialogState.Loading(
                            message = BitwardenString.loading_subscription.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(DEFAULT_PREMIUM_LOADED_STATE, awaitItem())
            }
            coVerify(exactly = 2) { mockBillingRepository.getSubscription() }
        }

    @Test
    fun `ManagePlanClick should show LoadingPortal then emit LaunchPortal on success`() =
        runTest {
            markUserPremium()

            val viewModel = createViewModel(
                subscriptionResult = SUBSCRIPTION_SUCCESS_ACTIVE,
                portalResult = CustomerPortalResult.Success(url = "https://portal"),
            )

            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                assertEquals(DEFAULT_PREMIUM_LOADED_STATE, stateFlow.awaitItem())

                viewModel.trySendAction(PlanAction.ManagePlanClick)

                assertEquals(
                    DEFAULT_PREMIUM_LOADED_STATE.copy(
                        dialogState = PlanState.DialogState.LoadingPortal,
                    ),
                    stateFlow.awaitItem(),
                )
                assertEquals(
                    PlanEvent.LaunchPortal(url = "https://portal"),
                    eventFlow.awaitItem(),
                )
                assertEquals(DEFAULT_PREMIUM_LOADED_STATE, stateFlow.awaitItem())
            }
        }

    @Test
    fun `ManagePlanClick should show PortalError on failure`() = runTest {
        markUserPremium()

        val viewModel = createViewModel(
            subscriptionResult = SUBSCRIPTION_SUCCESS_ACTIVE,
            portalResult = CustomerPortalResult.Error(error = RuntimeException("boom")),
        )

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_PREMIUM_LOADED_STATE, awaitItem())

            viewModel.trySendAction(PlanAction.ManagePlanClick)

            assertEquals(
                DEFAULT_PREMIUM_LOADED_STATE.copy(
                    dialogState = PlanState.DialogState.LoadingPortal,
                ),
                awaitItem(),
            )
            assertEquals(
                DEFAULT_PREMIUM_LOADED_STATE.copy(
                    dialogState = PlanState.DialogState.PortalError,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CancelPremiumClick should show CancelConfirmation with next renewal date`() = runTest {
        markUserPremium()

        val viewModel = createViewModel(
            subscriptionResult = SUBSCRIPTION_SUCCESS_ACTIVE,
        )

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_PREMIUM_LOADED_STATE, awaitItem())

            viewModel.trySendAction(PlanAction.CancelPremiumClick)

            assertEquals(
                DEFAULT_PREMIUM_LOADED_STATE.copy(
                    dialogState = PlanState.DialogState.CancelConfirmation(
                        nextRenewalDate = "April 2, 2026",
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `DismissCancelConfirmation should clear dialog`() = runTest {
        markUserPremium()

        val viewModel = createViewModel(
            subscriptionResult = SUBSCRIPTION_SUCCESS_ACTIVE,
        )

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_PREMIUM_LOADED_STATE, awaitItem())

            viewModel.trySendAction(PlanAction.CancelPremiumClick)
            assertEquals(
                DEFAULT_PREMIUM_LOADED_STATE.copy(
                    dialogState = PlanState.DialogState.CancelConfirmation(
                        nextRenewalDate = "April 2, 2026",
                    ),
                ),
                awaitItem(),
            )

            viewModel.trySendAction(PlanAction.DismissCancelConfirmation)
            assertEquals(DEFAULT_PREMIUM_LOADED_STATE, awaitItem())
        }
    }

    private fun markUserPremium() {
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(DEFAULT_ACCOUNT.copy(isPremium = true)),
        )
    }

    // endregion Premium user path

    @Suppress("LongParameterList")
    private fun createViewModel(
        initialState: PlanState? = null,
        planMode: PlanMode = PlanMode.Modal,
        pricingResult: PremiumPlanPricingResult? = DEFAULT_PRICING_SUCCESS,
        subscriptionResult: SubscriptionResult? = null,
        portalResult: CustomerPortalResult? = null,
        clock: Clock = FIXED_CLOCK,
    ): PlanViewModel {
        coEvery {
            mockBillingRepository.getPremiumPlanPricing()
        } coAnswers {
            pricingResult ?: awaitCancellation()
        }
        coEvery {
            mockBillingRepository.getSubscription()
        } coAnswers {
            subscriptionResult ?: awaitCancellation()
        }
        coEvery {
            mockBillingRepository.getPortalUrl()
        } coAnswers {
            portalResult ?: awaitCancellation()
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
            clock = clock,
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

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2026-04-21T00:00:00Z"),
    ZoneOffset.UTC,
)

private val SUBSCRIPTION_INFO_ACTIVE = SubscriptionInfo(
    status = PremiumSubscriptionStatus.ACTIVE,
    cadence = PlanCadence.ANNUALLY,
    seatsCost = BigDecimal("19.80"),
    storageCost = BigDecimal("24.00"),
    discountAmount = BigDecimal("2.10"),
    estimatedTax = BigDecimal("3.85"),
    nextChargeTotal = BigDecimal("45.55"),
    nextCharge = Instant.parse("2026-04-02T00:00:00Z"),
    canceledDate = null,
    suspensionDate = null,
    gracePeriodDays = null,
)

private val SUBSCRIPTION_SUCCESS_ACTIVE =
    SubscriptionResult.Success(subscription = SUBSCRIPTION_INFO_ACTIVE)

private val DEFAULT_PRICING_SUCCESS = PremiumPlanPricingResult.Success(
    annualPrice = ANNUAL_PRICE,
)

private const val PLACEHOLDER = "--"

private val DEFAULT_PREMIUM_ACTIVE_VIEW_STATE = PlanState.ViewState.Premium(
    status = PremiumSubscriptionStatus.ACTIVE,
    descriptionText = BitwardenString.premium_next_charge_summary.asText(
        "$45.55",
        "April 2, 2026",
    ),
    billingAmountText = BitwardenString.billing_rate_per_year.asText("$19.80"),
    storageCostText = "$24.00",
    discountAmountText = "-$2.10",
    estimatedTaxText = "$3.85",
    nextChargeDateText = "April 2, 2026",
    showCancelButton = true,
)

private val DEFAULT_PREMIUM_LOADED_STATE = PlanState(
    planMode = PlanMode.Modal,
    viewState = DEFAULT_PREMIUM_ACTIVE_VIEW_STATE,
    dialogState = null,
)

private val DEFAULT_PREMIUM_LOADING_STATE = PlanState(
    planMode = PlanMode.Modal,
    viewState = PlanState.ViewState.Premium(),
    dialogState = PlanState.DialogState.Loading(
        message = BitwardenString.loading_subscription.asText(),
    ),
)
