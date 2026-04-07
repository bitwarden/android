package com.bitwarden.ui.platform.components.tooltip.model

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState

/**
 * A custom [TooltipState] to be used for the tool tips which should not be
 * dismissed automatically by clicking outside of the pop-up area.
 */
@OptIn(ExperimentalMaterial3Api::class)
interface BitwardenToolTipState : TooltipState {
    /**
     * Call to dismiss the tool tip from the screen, should be used in
     * place of [TooltipState.dismiss]
     */
    fun dismissBitwardenToolTip()
}
