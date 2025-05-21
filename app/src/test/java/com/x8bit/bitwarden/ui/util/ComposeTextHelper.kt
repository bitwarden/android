package com.x8bit.bitwarden.ui.util

import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import com.x8bit.bitwarden.ui.platform.components.coachmark.IsCoachMarkToolTipKey

/**
 * A [SemanticsMatcher] user to find Popup nodes used specifically for CoachMarkToolTips
 */
val isCoachMarkToolTip: SemanticsMatcher
    get() = SemanticsMatcher("Node is used to show tool tip for active coach mark.") {
        it.config.getOrNull(IsCoachMarkToolTipKey) == true
    }
