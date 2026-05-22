package com.x8bit.bitwarden.ui.platform.feature.premium.plan

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.util.toFormattedDateStyle
import com.bitwarden.data.repository.util.baseWebVaultUrlOrDefault
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.manager.intent.model.AuthTabData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.billing.manager.PremiumStateManager
import com.x8bit.bitwarden.data.billing.repository.BillingRepository
import com.x8bit.bitwarden.data.billing.repository.model.CheckoutSessionResult
import com.x8bit.bitwarden.data.billing.repository.model.CustomerPortalResult
import com.x8bit.bitwarden.data.billing.repository.model.PlanCadence
import com.x8bit.bitwarden.data.billing.repository.model.PremiumPlanPricingResult
import com.x8bit.bitwarden.data.billing.repository.model.PremiumSubscriptionStatus
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionInfo
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionResult
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionStatusState
import com.x8bit.bitwarden.data.billing.util.PremiumCheckoutCallbackResult
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Clock
import java.time.Instant
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

private const val KEY_STATE = "state"
private const val MONTHS_PER_YEAR = 12
private const val PLACEHOLDER_TEXT = "--"

/**
 * The callback URL for the premium checkout custom tab.
 */
const val PREMIUM_CHECKOUT_CALLBACK_URL = "bitwarden://premium-checkout-result"

/**
 * View model for the plan screen, driving the upgrade flow for free users and
 * the subscription management surface for premium users.
 */
@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
@HiltViewModel
class PlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val billingRepository: BillingRepository,
    private val authRepository: AuthRepository,
    private val premiumStateManager: PremiumStateManager,
    private val environmentRepository: EnvironmentRepository,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
    private val vaultRepository: VaultRepository,
    private val clock: Clock,
) : BaseViewModel<PlanState, PlanEvent, PlanAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val planMode = savedStateHandle.toPlanArgs().planMode
        val isPremium = authRepository
            .userStateFlow
            .value
            ?.activeAccount
            ?.isPremium == true
        val showsPremiumView = isPremium ||
            premiumStateManager.subscriptionStatusStateFlow.value.isPremiumViewEligible()
        val isSelfHosted = premiumStateManager.isSelfHosted
        PlanState(
            planMode = planMode,
            viewState = when {
                showsPremiumView -> PlanState.ViewState.Premium()
                isSelfHosted -> PlanState.ViewState.Free.SelfHosted
                else -> PlanState.ViewState.Free.Cloud(
                    rate = PLACEHOLDER_TEXT,
                    checkoutUrl = null,
                    isAwaitingPremiumStatus = false,
                )
            },
            dialogState = null,
        )
    },
) {

    private val currencyFormatter: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.US)

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)

        authRepository
            .userStateFlow
            .map { PlanAction.Internal.UserStateUpdateReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        specialCircumstanceManager
            .specialCircumstanceStateFlow
            .map { PlanAction.Internal.SpecialCircumstanceReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        premiumStateManager
            .subscriptionStatusStateFlow
            .map { PlanAction.Internal.SubscriptionStatusUpdateReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        onFreeCloudContent {
            viewModelScope.launch {
                sendAction(
                    PlanAction.Internal.PricingResultReceive(
                        result = billingRepository.getPremiumPlanPricing(),
                    ),
                )
            }
        }

        onPremiumContent {
            mutableStateFlow.update {
                it.copy(
                    dialogState = PlanState.DialogState.Loading(
                        message = BitwardenString.loading_subscription.asText(),
                    ),
                )
            }
            viewModelScope.launch {
                sendAction(
                    PlanAction.Internal.SubscriptionResultReceive(
                        result = billingRepository.getSubscription(),
                    ),
                )
            }
        }
    }

    override fun handleAction(action: PlanAction) {
        when (action) {
            is PlanAction.BackClick -> handleBackClick()
            is PlanAction.UpgradeNowClick -> handleUpgradeNowClick()
            is PlanAction.DismissError -> handleDismissError()
            is PlanAction.ClosePricingErrorClick -> handleClosePricingErrorClick()
            is PlanAction.RetryClick -> handleRetryClick()
            is PlanAction.RetryPricingClick -> handleRetryPricingClick()
            is PlanAction.CancelWaiting -> handleCancelWaiting()
            is PlanAction.GoBackClick -> handleGoBackClick()
            is PlanAction.SyncClick -> handleSyncClick()
            is PlanAction.ContinueClick -> handleContinueClick()
            is PlanAction.ManagePlanClick -> handleManagePlanClick()
            is PlanAction.CancelPremiumClick -> handleCancelPremiumClick()
            is PlanAction.ConfirmCancelClick -> handleConfirmCancelClick()
            is PlanAction.DismissCancelConfirmation -> handleDismissCancelConfirmation()
            is PlanAction.DismissPortalError -> handleDismissPortalError()
            is PlanAction.RetryPortalClick -> handleRetryPortalClick()
            is PlanAction.RetrySubscriptionClick -> handleRetrySubscriptionClick()
            is PlanAction.Internal.CheckoutUrlReceive -> handleCheckoutUrlReceive(action)
            is PlanAction.Internal.UserStateUpdateReceive -> handleUserStateUpdateReceive(action)
            is PlanAction.Internal.SpecialCircumstanceReceive -> {
                handleSpecialCircumstanceReceive(action)
            }

            is PlanAction.Internal.SyncCompleteReceive -> handleSyncCompleteReceive()
            is PlanAction.Internal.PricingResultReceive -> handlePricingResultReceive(action)
            is PlanAction.Internal.PortalUrlReceive -> handlePortalUrlReceive(action)
            is PlanAction.Internal.SubscriptionResultReceive -> {
                handleSubscriptionResultReceive(action)
            }

            is PlanAction.Internal.SubscriptionStatusUpdateReceive -> {
                handleSubscriptionStatusUpdateReceive(action)
            }
        }
    }

    private fun handleBackClick() {
        sendEvent(PlanEvent.NavigateBack)
    }

    // region Free user handlers

    private fun handleUpgradeNowClick() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.Loading(
                    message = BitwardenString.opening_checkout.asText(),
                ),
            )
        }
        viewModelScope.launch {
            sendAction(
                PlanAction.Internal.CheckoutUrlReceive(
                    result = billingRepository.getCheckoutSessionUrl(),
                ),
            )
        }
    }

    private fun handleRetryClick() {
        handleUpgradeNowClick()
    }

    private fun handleDismissError() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleClosePricingErrorClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
        sendEvent(PlanEvent.NavigateBack)
    }

    private fun handleCancelWaiting() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleSyncClick() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.Loading(
                    message = BitwardenString.confirming_your_upgrade.asText(),
                ),
            )
        }
        triggerSync()
    }

    private fun handleContinueClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
        sendEvent(PlanEvent.NavigateBack)
    }

    private fun handleGoBackClick() {
        onFreeCloudContent { freeState ->
            freeState.checkoutUrl?.let { url ->
                sendEvent(
                    PlanEvent.LaunchBrowser(
                        url = url,
                        authTabData = AuthTabData.CustomScheme(
                            callbackUrl = PREMIUM_CHECKOUT_CALLBACK_URL,
                        ),
                    ),
                )
            }
        }
    }

    private fun handleCheckoutUrlReceive(
        action: PlanAction.Internal.CheckoutUrlReceive,
    ) {
        when (val result = action.result) {
            is CheckoutSessionResult.Success -> {
                sendEvent(
                    PlanEvent.LaunchBrowser(
                        url = result.url,
                        authTabData = AuthTabData.CustomScheme(
                            callbackUrl = PREMIUM_CHECKOUT_CALLBACK_URL,
                        ),
                    ),
                )
                onFreeCloudContent { freeState ->
                    mutableStateFlow.update {
                        it.copy(
                            viewState = freeState.copy(
                                checkoutUrl = result.url,
                            ),
                            dialogState = null,
                        )
                    }
                }
            }

            is CheckoutSessionResult.Error -> {
                mutableStateFlow.update {
                    it.copy(dialogState = PlanState.DialogState.CheckoutError)
                }
            }
        }
    }

    // endregion Free user handlers

    // region Premium user handlers

    private fun handleManagePlanClick() {
        val webVaultBaseUrl = environmentRepository
            .environment
            .environmentUrlData
            .baseWebVaultUrlOrDefault
        sendEvent(PlanEvent.LaunchUri(url = "$webVaultBaseUrl/#/settings/subscription/premium"))
    }

    private fun handleCancelPremiumClick() {
        onPremiumContent { premiumState ->
            mutableStateFlow.update {
                it.copy(
                    dialogState = PlanState.DialogState.CancelConfirmation(
                        nextRenewalDate = premiumState.nextChargeDateText
                            ?: PLACEHOLDER_TEXT,
                    ),
                )
            }
        }
    }

    private fun handleConfirmCancelClick() {
        launchPortalFetch()
    }

    private fun handleDismissCancelConfirmation() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleRetryPortalClick() {
        launchPortalFetch()
    }

    private fun handleDismissPortalError() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun launchPortalFetch() {
        mutableStateFlow.update {
            it.copy(dialogState = PlanState.DialogState.LoadingPortal)
        }
        viewModelScope.launch {
            sendAction(
                PlanAction.Internal.PortalUrlReceive(
                    result = billingRepository.getPortalUrl(),
                ),
            )
        }
    }

    private fun handlePortalUrlReceive(
        action: PlanAction.Internal.PortalUrlReceive,
    ) {
        when (val result = action.result) {
            is CustomerPortalResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(PlanEvent.LaunchPortal(url = result.url))
            }

            is CustomerPortalResult.Error -> {
                mutableStateFlow.update {
                    it.copy(dialogState = PlanState.DialogState.PortalError)
                }
            }
        }
    }

    private fun handleRetrySubscriptionClick() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.Loading(
                    message = BitwardenString.loading_subscription.asText(),
                ),
            )
        }
        viewModelScope.launch {
            sendAction(
                PlanAction.Internal.SubscriptionResultReceive(
                    result = billingRepository.getSubscription(),
                ),
            )
        }
    }

    private fun handleSubscriptionResultReceive(
        action: PlanAction.Internal.SubscriptionResultReceive,
    ) {
        when (val result = action.result) {
            is SubscriptionResult.Success -> {
                val info = result.subscription
                mutableStateFlow.update {
                    it.copy(
                        viewState = info.toPremiumViewState(),
                        dialogState = null,
                    )
                }
            }

            SubscriptionResult.NotFound -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = PlanState.ViewState.Free.Cloud(
                            rate = PLACEHOLDER_TEXT,
                            checkoutUrl = null,
                            isAwaitingPremiumStatus = false,
                        ),
                        dialogState = PlanState.DialogState.Loading(
                            message = BitwardenString.loading.asText(),
                        ),
                    )
                }
                viewModelScope.launch {
                    sendAction(
                        PlanAction.Internal.PricingResultReceive(
                            result = billingRepository.getPremiumPlanPricing(),
                        ),
                    )
                }
            }

            is SubscriptionResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = PlanState.DialogState.SubscriptionError(
                            title = BitwardenString.subscription_error.asText(),
                            message = BitwardenString
                                .trouble_loading_subscription
                                .asText(),
                        ),
                    )
                }
            }
        }
    }

    private fun handleSubscriptionStatusUpdateReceive(
        action: PlanAction.Internal.SubscriptionStatusUpdateReceive,
    ) {
        val status = (action.state as? SubscriptionStatusState.Available)?.status
            ?: return
        if (!status.isPremiumViewEligible()) return
        onFreeCloudContent { freeState ->
            if (freeState.isAwaitingPremiumStatus) return@onFreeCloudContent
            mutableStateFlow.update {
                it.copy(
                    viewState = PlanState.ViewState.Premium(),
                    dialogState = PlanState.DialogState.Loading(
                        message = BitwardenString.loading_subscription.asText(),
                    ),
                )
            }
            viewModelScope.launch {
                sendAction(
                    PlanAction.Internal.SubscriptionResultReceive(
                        result = billingRepository.getSubscription(),
                    ),
                )
            }
        }
    }

    // endregion Premium user handlers

    // region Shared handlers

    private fun handleUserStateUpdateReceive(
        action: PlanAction.Internal.UserStateUpdateReceive,
    ) {
        onFreeCloudContent { freeState ->
            if (!freeState.isAwaitingPremiumStatus) return@onFreeCloudContent

            val isPremium = action.userState?.activeAccount?.isPremium == true
            if (isPremium) {
                onPremiumUpgradeSuccess()
            }
        }
    }

    private fun handleSpecialCircumstanceReceive(
        action: PlanAction.Internal.SpecialCircumstanceReceive,
    ) {
        when (val circumstance = action.specialCircumstance) {
            is SpecialCircumstance.PremiumCheckout -> {
                handlePremiumCheckoutCircumstance(circumstance)
            }

            SpecialCircumstance.StripePortal -> handleStripePortalCircumstance()
            else -> Unit
        }
    }

    private fun handleStripePortalCircumstance() {
        specialCircumstanceManager.specialCircumstance = null
        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.Loading(
                    message = BitwardenString.loading_subscription.asText(),
                ),
            )
        }
        viewModelScope.launch {
            sendAction(
                PlanAction.Internal.SubscriptionResultReceive(
                    result = billingRepository.getSubscription(),
                ),
            )
        }
    }

    private fun handlePremiumCheckoutCircumstance(
        checkoutResult: SpecialCircumstance.PremiumCheckout,
    ) {
        specialCircumstanceManager.specialCircumstance = null

        if (checkoutResult.callbackResult is PremiumCheckoutCallbackResult.Canceled) {
            onFreeCloudContent { freeState ->
                mutableStateFlow.update {
                    it.copy(
                        viewState = freeState.copy(
                            isAwaitingPremiumStatus = true,
                        ),
                        dialogState = PlanState.DialogState.WaitingForPayment,
                    )
                }
            }
            return
        }

        val isPremium = authRepository
            .userStateFlow
            .value
            ?.activeAccount
            ?.isPremium == true
        if (isPremium) {
            onPremiumUpgradeSuccess()
        } else {
            onFreeCloudContent { freeState ->
                mutableStateFlow.update {
                    it.copy(
                        viewState = freeState.copy(
                            isAwaitingPremiumStatus = true,
                        ),
                        dialogState = PlanState.DialogState.Loading(
                            message = BitwardenString.confirming_your_upgrade.asText(),
                        ),
                    )
                }
            }
            triggerSync()
        }
    }

    private fun triggerSync() {
        viewModelScope.launch {
            val result = vaultRepository.syncForResult(forced = true)
            sendAction(PlanAction.Internal.SyncCompleteReceive(result))
        }
    }

    private fun handleSyncCompleteReceive() {
        onFreeCloudContent { freeState ->
            if (!freeState.isAwaitingPremiumStatus) return@onFreeCloudContent

            val isPremium = authRepository
                .userStateFlow
                .value
                ?.activeAccount
                ?.isPremium == true
            if (isPremium) {
                onPremiumUpgradeSuccess()
            } else {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = PlanState.DialogState.PendingUpgrade,
                    )
                }
            }
        }
    }

    private fun onPremiumUpgradeSuccess() {
        onFreeCloudContent {
            mutableStateFlow.update {
                it.copy(
                    viewState = PlanState.ViewState.Premium(),
                    dialogState = PlanState.DialogState.Loading(
                        message = BitwardenString.loading_subscription.asText(),
                    ),
                )
            }
            viewModelScope.launch {
                sendAction(
                    PlanAction.Internal.SubscriptionResultReceive(
                        result = billingRepository.getSubscription(),
                    ),
                )
            }
        }
        // The Upgraded to Premium route uses `launchSingleTop = true` so a duplicate event is a
        // no-op for the user. The event itself is harmless to re-emit; the state mutation above
        // is what's guarded by `onFreeCloudContent`.
        sendEvent(PlanEvent.NavigateToUpgradedToPremium)
    }

    private fun handlePricingResultReceive(
        action: PlanAction.Internal.PricingResultReceive,
    ) {
        when (val result = action.result) {
            is PremiumPlanPricingResult.Success -> {
                val formattedRate = currencyFormatter
                    .format(result.annualPrice / MONTHS_PER_YEAR)
                mutableStateFlow.update { currentState ->
                    val updatedViewState = when (val vs = currentState.viewState) {
                        is PlanState.ViewState.Free.Cloud -> vs.copy(rate = formattedRate)
                        is PlanState.ViewState.Free.SelfHosted,
                        is PlanState.ViewState.Premium,
                            -> vs
                    }
                    currentState.copy(
                        viewState = updatedViewState,
                        dialogState = null,
                    )
                }
            }

            is PremiumPlanPricingResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = PlanState.DialogState.GetPricingError(
                            title = BitwardenString.pricing_unavailable.asText(),
                            message = result.errorMessage?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }
        }
    }

    private fun handleRetryPricingClick() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.Loading(
                    message = BitwardenString.loading.asText(),
                ),
            )
        }
        viewModelScope.launch {
            sendAction(
                PlanAction.Internal.PricingResultReceive(
                    result = billingRepository.getPremiumPlanPricing(),
                ),
            )
        }
    }

    private inline fun onFreeCloudContent(
        block: (PlanState.ViewState.Free.Cloud) -> Unit,
    ) {
        (state.viewState as? PlanState.ViewState.Free.Cloud)?.let(block)
    }

    private inline fun onPremiumContent(
        block: (PlanState.ViewState.Premium) -> Unit,
    ) {
        (state.viewState as? PlanState.ViewState.Premium)?.let(block)
    }

    private fun SubscriptionInfo.toPremiumViewState(): PlanState.ViewState.Premium {
        val formattedTotal = currencyFormatter.format(nextChargeTotal)
        val formattedDate = nextCharge?.toLocalizedDate()
        val formattedCancelAt = cancelAt?.toLocalizedDate()
        val formattedCanceled = canceledDate?.toLocalizedDate()
        val formattedSuspension = suspensionDate?.toLocalizedDate()

        return PlanState.ViewState.Premium(
            status = status,
            billingAmountText = seatsCost.toBillingAmountText(cadence),
            storageCostText = storageCost.toOptionalMoneyText(),
            discountAmountText = discountAmount.toOptionalMoneyText(negative = true),
            estimatedTaxText = estimatedTax.toRequiredMoneyText(),
            totalText = nextChargeTotal.toBillingAmountText(cadence),
            nextChargeTotalText = formattedTotal,
            nextChargeDateText = formattedDate,
            cancelAtDateText = formattedCancelAt,
            canceledDateText = formattedCanceled,
            suspensionDateText = formattedSuspension,
            gracePeriodDays = gracePeriodDays,
            showCancelButton = status.canBeCanceled(),
        )
    }

    private fun BigDecimal.toBillingAmountText(cadence: PlanCadence): Text {
        val formatted = currencyFormatter.format(this)
        val cadenceRes = when (cadence) {
            PlanCadence.ANNUALLY -> BitwardenString.billing_rate_per_year
            PlanCadence.MONTHLY -> BitwardenString.billing_rate_per_month
        }
        return cadenceRes.asText(formatted)
    }

    /**
     * Formats this amount for an always-rendered line item. Null is coerced to zero so the row
     * still shows the locale-formatted `$0.00`, matching the Web convention of always rendering
     * the Estimated Tax and Total rows.
     */
    private fun BigDecimal?.toRequiredMoneyText(): String =
        currencyFormatter.format(this ?: BigDecimal.ZERO)

    /**
     * Formats this amount for a hide-when-absent line item. Returns `null` when the amount is
     * `null` or non-positive so the caller can omit the row entirely (Discount, Storage).
     * When [negative] is true, the formatted value is prefixed with `-` to match the canonical
     * Web discount styling.
     */
    private fun BigDecimal?.toOptionalMoneyText(negative: Boolean = false): String? =
        when {
            this == null || this.signum() <= 0 -> null
            negative -> "-${currencyFormatter.format(this)}"
            else -> currencyFormatter.format(this)
        }

    private fun Instant.toLocalizedDate(): String =
        toFormattedDateStyle(
            dateStyle = FormatStyle.LONG,
            locale = Locale.US,
            clock = clock,
        )

    // endregion Shared handlers
}

/**
 * Determines how the Plan screen was reached.
 */
@Serializable
enum class PlanMode {
    /** Back arrow, bottom nav visible (push sub-screen from Settings). */
    Standard,

    /** Close icon, bottom nav hidden (modal overlay from Vault). */
    Modal,
}

/**
 * Models state for the plan screen.
 */
@Parcelize
data class PlanState(
    val planMode: PlanMode,
    val viewState: ViewState,
    val dialogState: DialogState?,
) : Parcelable {

    /**
     * The navigation icon drawable resource for the top app bar.
     */
    @get:DrawableRes
    val navigationIcon: Int
        get() = when (planMode) {
            PlanMode.Standard -> BitwardenDrawable.ic_back
            PlanMode.Modal -> BitwardenDrawable.ic_close
        }

    /**
     * The navigation icon content description string resource.
     */
    @get:StringRes
    val navigationIconContentDescription: Int
        get() = when (planMode) {
            PlanMode.Standard -> BitwardenString.back
            PlanMode.Modal -> BitwardenString.close
        }

    /**
     * The title string resource for the top app bar.
     */
    @get:StringRes
    val title: Int
        get() = when (viewState) {
            is ViewState.Free -> BitwardenString.upgrade_to_premium
            is ViewState.Premium -> BitwardenString.plan
        }

    /**
     * Models the content state of the plan screen.
     */
    sealed class ViewState : Parcelable {

        /**
         * Free user view — shows the upgrade flow for cloud accounts or a
         * "manage on web vault" info card for self-hosted accounts.
         */
        sealed class Free : ViewState() {

            /**
             * Free user on a cloud-hosted environment — shows upgrade pricing
             * and feature list.
             */
            @Parcelize
            data class Cloud(
                val rate: String,
                val checkoutUrl: String?,
                val isAwaitingPremiumStatus: Boolean,
            ) : Free()

            /**
             * Free user on a self-hosted environment — Stripe checkout is
             * unavailable, so the screen redirects the user to manage their
             * subscription on the web vault.
             */
            @Parcelize
            data object SelfHosted : Free()
        }

        /**
         * Premium user view — shows subscription details and management options.
         *
         * Line-item text fields follow two visibility contracts that mirror the
         * canonical Web subscription card:
         *
         * - **Required** ([billingAmountText], [estimatedTaxText], [totalText]):
         *   the row is always rendered. A zero amount is formatted as `$0.00`
         *   rather than hidden. Defaults are sensible empty values used only
         *   during the initial load — the `DialogState.Loading` overlay covers
         *   the screen during the fetch, so these defaults are never surfaced
         *   to the user.
         * - **Optional** ([storageCostText], [discountAmountText]): a `null`
         *   value signals the screen to omit the row entirely (along with its
         *   leading divider). When non-null, the value is fully formatted by
         *   the view model — the screen renders it verbatim.
         */
        @Parcelize
        data class Premium(
            val status: PremiumSubscriptionStatus? = null,
            val billingAmountText: Text = "".asText(),
            val storageCostText: String? = null,
            val discountAmountText: String? = null,
            val estimatedTaxText: String = "$0.00",
            val totalText: Text = "".asText(),
            val nextChargeTotalText: String? = null,
            val nextChargeDateText: String? = null,
            val cancelAtDateText: String? = null,
            val canceledDateText: String? = null,
            val suspensionDateText: String? = null,
            val gracePeriodDays: Int? = null,
            val showCancelButton: Boolean = false,
        ) : ViewState()
    }

    /**
     * Represents the dialog/overlay state for the plan screen.
     */
    sealed class DialogState : Parcelable {

        /**
         * Loading overlay with a configurable message.
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()

        /**
         * Error dialog shown when the checkout session could not be loaded.
         */
        @Parcelize
        data object CheckoutError : DialogState()

        /**
         * Error dialog shown when pricing information cannot be retrieved.
         */
        @Parcelize
        data class GetPricingError(
            val title: Text,
            val message: Text,
        ) : DialogState()

        /**
         * Waiting dialog shown when the user returns from checkout without
         * completing payment.
         */
        @Parcelize
        data object WaitingForPayment : DialogState()

        /**
         * Dialog shown after a successful checkout when premium status has not
         * yet been provisioned by the server.
         */
        @Parcelize
        data object PendingUpgrade : DialogState()

        /**
         * Confirmation dialog shown before cancelling premium.
         */
        @Parcelize
        data class CancelConfirmation(
            val nextRenewalDate: String,
        ) : DialogState()

        /**
         * Loading overlay while fetching the portal URL.
         */
        @Parcelize
        data object LoadingPortal : DialogState()

        /**
         * Error dialog shown when the portal URL could not be loaded.
         */
        @Parcelize
        data object PortalError : DialogState()

        /**
         * Error dialog shown when subscription details cannot be loaded.
         */
        @Parcelize
        data class SubscriptionError(
            val title: Text,
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events for the plan screen.
 */
sealed class PlanEvent {

    /**
     * Launch the user's browser with the given checkout [url] via AuthTab.
     */
    data class LaunchBrowser(
        val url: String,
        val authTabData: AuthTabData,
    ) : PlanEvent()

    /**
     * Launch the user's browser with the given portal [url].
     */
    data class LaunchPortal(
        val url: String,
    ) : PlanEvent()

    /**
     * Launch the user's browser with the given web vault [url].
     */
    data class LaunchUri(
        val url: String,
    ) : PlanEvent()

    /**
     * Navigate back to the previous screen.
     */
    data object NavigateBack : PlanEvent()

    /**
     * Navigate to the full-screen "Upgraded to Premium" screen. The destination registrant
     * encodes the originating [PlanMode] when issuing the navigation; the screen's dismiss
     * uses that mode to choose pop semantics.
     */
    data object NavigateToUpgradedToPremium : PlanEvent()
}

/**
 * Models actions for the plan screen.
 */
sealed class PlanAction {

    /**
     * The user clicked the back/close button.
     */
    data object BackClick : PlanAction()

    // region Free user actions

    /**
     * The user clicked the upgrade now button.
     */
    data object UpgradeNowClick : PlanAction()

    /**
     * The user dismissed the checkout error dialog.
     */
    data object DismissError : PlanAction()

    /**
     * The user clicked retry on the checkout error dialog.
     */
    data object RetryClick : PlanAction()

    /**
     * The user clicked retry on the pricing error screen.
     */
    data object RetryPricingClick : PlanAction()

    /**
     * The user clicked the close button on the pricing error dialog.
     */
    data object ClosePricingErrorClick : PlanAction()

    /**
     * The user dismissed the waiting for payment dialog.
     */
    data object CancelWaiting : PlanAction()

    /**
     * The user clicked go back on the waiting for payment dialog.
     */
    data object GoBackClick : PlanAction()

    /**
     * The user clicked sync on the pending upgrade dialog.
     */
    data object SyncClick : PlanAction()

    /**
     * The user chose to continue without waiting for upgrade.
     */
    data object ContinueClick : PlanAction()

    // endregion Free user actions

    // region Premium user actions

    /**
     * The user clicked manage plan.
     */
    data object ManagePlanClick : PlanAction()

    /**
     * The user clicked cancel premium.
     */
    data object CancelPremiumClick : PlanAction()

    /**
     * The user confirmed the cancel premium action.
     */
    data object ConfirmCancelClick : PlanAction()

    /**
     * The user dismissed the cancel confirmation dialog.
     */
    data object DismissCancelConfirmation : PlanAction()

    /**
     * The user dismissed the portal error dialog.
     */
    data object DismissPortalError : PlanAction()

    /**
     * The user clicked retry on the portal error dialog.
     */
    data object RetryPortalClick : PlanAction()

    /**
     * The user clicked retry on the subscription error dialog.
     */
    data object RetrySubscriptionClick : PlanAction()

    // endregion Premium user actions

    /**
     * Models actions the view model sends itself.
     */
    sealed class Internal : PlanAction() {

        /**
         * A checkout URL result has been received from the repository.
         */
        data class CheckoutUrlReceive(
            val result: CheckoutSessionResult,
        ) : Internal()

        /**
         * A user state update has been received.
         */
        data class UserStateUpdateReceive(
            val userState: UserState?,
        ) : Internal()

        /**
         * A special circumstance update has been received.
         */
        data class SpecialCircumstanceReceive(
            val specialCircumstance: SpecialCircumstance?,
        ) : Internal()

        /**
         * A vault sync has completed.
         */
        data class SyncCompleteReceive(
            val result: SyncVaultDataResult,
        ) : Internal()

        /**
         * A pricing result has been received from the repository.
         */
        data class PricingResultReceive(
            val result: PremiumPlanPricingResult,
        ) : Internal()

        /**
         * A portal URL result has been received from the repository.
         */
        data class PortalUrlReceive(
            val result: CustomerPortalResult,
        ) : Internal()

        /**
         * A subscription result has been received from the repository.
         */
        data class SubscriptionResultReceive(
            val result: SubscriptionResult,
        ) : Internal()

        /**
         * The shared subscription status state for the active user has updated.
         */
        data class SubscriptionStatusUpdateReceive(
            val state: SubscriptionStatusState,
        ) : Internal()
    }
}

/**
 * Returns `true` when this status corresponds to a subscription that the user can still
 * cancel through the Stripe portal — i.e., a live or recoverable subscription. Terminal
 * states (canceled) do not present a cancel action.
 */
private fun PremiumSubscriptionStatus.canBeCanceled(): Boolean = when (this) {
    PremiumSubscriptionStatus.CANCELED,
    PremiumSubscriptionStatus.PENDING_CANCELLATION,
        -> false

    PremiumSubscriptionStatus.ACTIVE,
    PremiumSubscriptionStatus.PAST_DUE,
    PremiumSubscriptionStatus.PAUSED,
    PremiumSubscriptionStatus.UPDATE_PAYMENT,
        -> true
}

/**
 * Returns `true` when this status should route the Plan screen to the Premium view even
 * if `Account.isPremium=false`. Trouble states (canceled, past due, paused, update payment)
 * carry enough context to render a status badge and Manage/Resubscribe affordances, which
 * the Free view does not surface.
 */
private fun PremiumSubscriptionStatus.isPremiumViewEligible(): Boolean = when (this) {
    PremiumSubscriptionStatus.CANCELED,
    PremiumSubscriptionStatus.PAST_DUE,
    PremiumSubscriptionStatus.PAUSED,
    PremiumSubscriptionStatus.PENDING_CANCELLATION,
    PremiumSubscriptionStatus.UPDATE_PAYMENT,
        -> true

    PremiumSubscriptionStatus.ACTIVE -> false
}

/**
 * Returns `true` when the current [SubscriptionStatusState] indicates that the Plan screen
 * should render the Premium view, even if the user account's `isPremium` flag is `false`.
 */
private fun SubscriptionStatusState.isPremiumViewEligible(): Boolean =
    this is SubscriptionStatusState.Available && this.status.isPremiumViewEligible()
