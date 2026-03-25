package com.x8bit.bitwarden.ui.platform.feature.premium.plan.handlers

import com.x8bit.bitwarden.ui.platform.feature.premium.plan.PlanAction
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.PlanViewModel

/**
 * A collection of handler functions for managing actions within the context of
 * the plan screen.
 */
data class PlanHandlers(
    val onBackClick: () -> Unit,
    val onUpgradeNowClick: () -> Unit,
    val onDismissError: () -> Unit,
    val onRetryClick: () -> Unit,
    val onCancelWaiting: () -> Unit,
    val onGoBackClick: () -> Unit,
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
            onCancelWaiting = { viewModel.trySendAction(PlanAction.CancelWaiting) },
            onGoBackClick = { viewModel.trySendAction(PlanAction.GoBackClick) },
        )
    }
}
