package com.x8bit.bitwarden.ui.platform.feature.premium.plan

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.intent.model.AuthTabData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.billing.repository.BillingRepository
import com.x8bit.bitwarden.data.billing.repository.model.CheckoutSessionResult
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * The callback URL for the premium checkout custom tab.
 */
const val PREMIUM_CHECKOUT_CALLBACK_URL = "bitwarden://premium-upgrade-callback"

/**
 * Placeholder rate until dynamic pricing is available.
 * TODO: [PM-33946] Replace with dynamic pricing from GET /api/plans/premium.
 */
private const val PLACEHOLDER_RATE = "$1.65"

/**
 * View model for the plan screen, handling the free-user upgrade flow.
 */
@HiltViewModel
class PlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val billingRepository: BillingRepository,
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
    authRepository: AuthRepository,
    specialCircumstanceManager: SpecialCircumstanceManager,
) : BaseViewModel<PlanState, PlanEvent, PlanAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val planMode = savedStateHandle.toPlanArgs().planMode
        val dialogState = when (specialCircumstanceManager.specialCircumstance) {
            is SpecialCircumstance.PremiumCheckoutResult -> {
                specialCircumstanceManager.specialCircumstance = null
                PlanState.DialogState.WaitingForPayment
            }

            else -> null
        }
        PlanState(
            planMode = planMode,
            viewState = PlanState.ViewState.Free(rate = PLACEHOLDER_RATE),
            dialogState = dialogState,
        )
    },
) {
    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)

        authRepository
            .userStateFlow
            .map { PlanAction.Internal.UserStateUpdateReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: PlanAction) {
        when (action) {
            is PlanAction.BackClick -> handleBackClick()
            is PlanAction.UpgradeNowClick -> {
                handleUpgradeNowClick()
            }

            is PlanAction.DismissError -> handleDismissError()
            is PlanAction.RetryClick -> handleRetryClick()
            is PlanAction.CancelWaiting -> handleCancelWaiting()
            is PlanAction.GoBackClick -> handleGoBackClick()
            is PlanAction.Internal.CheckoutUrlReceive -> {
                handleCheckoutUrlReceive(action)
            }

            is PlanAction.Internal.UserStateUpdateReceive -> {
                handleUserStateUpdateReceive(action)
            }
        }
    }

    private fun handleBackClick() {
        sendEvent(PlanEvent.NavigateBack)
    }

    private fun handleUpgradeNowClick() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.Loading(
                    message = BitwardenString.opening_checkout.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = billingRepository.getCheckoutSessionUrl()
            sendAction(
                PlanAction.Internal.CheckoutUrlReceive(result),
            )
        }
    }

    private fun handleRetryClick() {
        handleUpgradeNowClick()
    }

    private fun handleDismissError() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleCancelWaiting() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleGoBackClick() {
        onFreeContent { freeState ->
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
                onFreeContent { freeState ->
                    mutableStateFlow.update {
                        it.copy(
                            viewState = freeState.copy(
                                checkoutUrl = result.url,
                            ),
                            dialogState = PlanState.DialogState.WaitingForPayment,
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

    private fun handleUserStateUpdateReceive(
        action: PlanAction.Internal.UserStateUpdateReceive,
    ) {
        if (state.dialogState !is PlanState.DialogState.WaitingForPayment) return

        val isPremium = action.userState?.activeAccount?.isPremium == true
        if (isPremium) {
            snackbarRelayManager.sendSnackbarData(
                data = BitwardenSnackbarData(
                    message = BitwardenString.upgraded_to_premium.asText(),
                ),
                relay = SnackbarRelay.PREMIUM_UPGRADED,
            )
            sendEvent(PlanEvent.NavigateBack)
        }
    }

    private inline fun onFreeContent(
        block: (PlanState.ViewState.Free) -> Unit,
    ) {
        (state.viewState as? PlanState.ViewState.Free)
            ?.let(block)
    }
}

/**
 * Determines how the Plan screen was reached.
 */
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
     * Models the content state of the plan screen.
     */
    sealed class ViewState : Parcelable {

        /**
         * The monthly billing rate for the plan.
         */
        abstract val rate: String

        /**
         * Free user view — shows upgrade pricing and feature list.
         */
        @Parcelize
        data class Free(
            override val rate: String,
            val checkoutUrl: String? = null,
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
         * Error dialog shown when the checkout session could not
         * be loaded.
         */
        @Parcelize
        data object CheckoutError : DialogState()

        /**
         * Waiting dialog shown after the browser has been launched
         * for checkout.
         */
        @Parcelize
        data object WaitingForPayment : DialogState()
    }
}

/**
 * Models events for the plan screen.
 */
sealed class PlanEvent {

    /**
     * Launch the user's browser with the given checkout [url]
     * via AuthTab.
     */
    data class LaunchBrowser(
        val url: String,
        val authTabData: AuthTabData,
    ) : PlanEvent()

    /**
     * Navigate back to the previous screen.
     */
    data object NavigateBack : PlanEvent()
}

/**
 * Models actions for the plan screen.
 */
sealed class PlanAction {

    /**
     * The user clicked the back/close button.
     */
    data object BackClick : PlanAction()

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
     * The user dismissed the waiting for payment dialog.
     */
    data object CancelWaiting : PlanAction()

    /**
     * The user clicked go back on the waiting for payment dialog.
     */
    data object GoBackClick : PlanAction()

    /**
     * Models actions the view model sends itself.
     */
    sealed class Internal : PlanAction() {

        /**
         * A checkout URL result has been received from the
         * repository.
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
    }
}
