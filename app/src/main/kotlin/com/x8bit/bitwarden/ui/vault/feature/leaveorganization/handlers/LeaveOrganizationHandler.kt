package com.x8bit.bitwarden.ui.vault.feature.leaveorganization.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.vault.feature.leaveorganization.LeaveOrganizationAction
import com.x8bit.bitwarden.ui.vault.feature.leaveorganization.LeaveOrganizationViewModel

/**
 * A class to handle user interactions for the Leave Organization screen.
 */
data class LeaveOrganizationHandler(
    val onBackClick: () -> Unit,
    val onLeaveClick: () -> Unit,
    val onHelpClick: () -> Unit,
    val onDismissDialog: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [LeaveOrganizationHandler] using the provided
         * [LeaveOrganizationViewModel].
         */
        fun create(viewModel: LeaveOrganizationViewModel): LeaveOrganizationHandler =
            LeaveOrganizationHandler(
                onBackClick = {
                    viewModel.trySendAction(LeaveOrganizationAction.BackClick)
                },
                onLeaveClick = {
                    viewModel.trySendAction(LeaveOrganizationAction.LeaveOrganizationClick)
                },
                onHelpClick = {
                    viewModel.trySendAction(LeaveOrganizationAction.HelpLinkClick)
                },
                onDismissDialog = {
                    viewModel.trySendAction(LeaveOrganizationAction.DismissDialog)
                },
            )
    }
}

/**
 * Helper function to create and remember a [LeaveOrganizationHandler] instance.
 */
@Composable
fun rememberLeaveOrganizationHandler(
    viewModel: LeaveOrganizationViewModel,
): LeaveOrganizationHandler = remember(viewModel) {
    LeaveOrganizationHandler.create(viewModel)
}
