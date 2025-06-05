package com.bitwarden.ui.platform.components.button.model

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton

/**
 * Colors for a [BitwardenOutlinedButton].
 */
@Immutable
data class BitwardenOutlinedButtonColors(
    val materialButtonColors: ButtonColors,
    val outlineBorderColor: Color,
    val outlinedDisabledBorderColor: Color,
)
