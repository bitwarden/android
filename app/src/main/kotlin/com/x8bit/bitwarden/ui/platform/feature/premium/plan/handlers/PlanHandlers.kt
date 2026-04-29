package com.x8bit.bitwarden.ui.platform.feature.premium.plan.handlers

import com.x8bit.bitwarden.ui.platform.feature.premium.plan.PlanAction
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.PlanViewModel

/**
 * A collection of handler functions for managing actions within the context of
 * the plan screen.
 */
@Suppress("LongParameterList")
data class PlanHandlers(
    val onBackClick: () -> Unit,
    val onUpgradeNowClick: () -> Unit,
    val onDismissError: () -> Unit,
    val onRetryClick: () -> Unit,
    val onRetryPricingClick: () -> Unit,
    val onClosePricingErrorClick: () -> Unit,
    val onCancelWaiting: () -> Unit,
    val onGoBackClick: () -> Unit,
    val onSyncClick: () -> Unit,
    val onContinueClick: () -> Unit,
    val onManagePlanClick: () -> Unit,
    val onCancelPremiumClick: () -> Unit,
    val onConfirmCancelClick: () -> Unit,
    val onDismissCancelConfirmation: () -> Unit,
    val onDismissPortalError: () -> Unit,
    val onRetrySubscriptionClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates the [PlanHandlers] using the [PlanViewModel] to send desired
         * actions.
         */
        fun create(viewModel: PlanViewModel): PlanHandlers = PlanHandlers(
            onBackClick = { viewModel.trySendAction(PlanAction.BackClick) },
            onUpgradeNowClick = { viewModel.trySendAction(PlanAction.UpgradeNowClick) },
            onDismissError = { viewModel.trySendAction(PlanAction.DismissError) },
            onRetryClick = { viewModel.trySendAction(PlanAction.RetryClick) },
            onRetryPricingClick = { viewModel.trySendAction(PlanAction.RetryPricingClick) },
            onClosePricingErrorClick = {
                viewModel.trySendAction(PlanAction.ClosePricingErrorClick)
            },
            onCancelWaiting = { viewModel.trySendAction(PlanAction.CancelWaiting) },
            onGoBackClick = { viewModel.trySendAction(PlanAction.GoBackClick) },
            onSyncClick = { viewModel.trySendAction(PlanAction.SyncClick) },
            onContinueClick = { viewModel.trySendAction(PlanAction.ContinueClick) },
            onManagePlanClick = { viewModel.trySendAction(PlanAction.ManagePlanClick) },
            onCancelPremiumClick = {
                viewModel.trySendAction(PlanAction.CancelPremiumClick)
            },
            onConfirmCancelClick = {
                viewModel.trySendAction(PlanAction.ConfirmCancelClick)
            },
            onDismissCancelConfirmation = {
                viewModel.trySendAction(PlanAction.DismissCancelConfirmation)
            },
            onDismissPortalError = {
                viewModel.trySendAction(PlanAction.DismissPortalError)
            },
            onRetrySubscriptionClick = {
                viewModel.trySendAction(PlanAction.RetrySubscriptionClick)
            },
        )
    }
}
