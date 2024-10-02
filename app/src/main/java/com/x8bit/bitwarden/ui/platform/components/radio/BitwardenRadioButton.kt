package com.x8bit.bitwarden.ui.platform.components.radio

import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.x8bit.bitwarden.ui.platform.components.radio.color.bitwardenRadioButtonColors

/**
 * A custom Bitwarden-themed radio button.
 *
 * @param isSelected Whether this radio button is selected or not.
 * @param onClick The lambda to be invoked when the item is clicked.
 * @param modifier The [Modifier] to be applied to this radio button.
 */
@Composable
fun BitwardenRadioButton(
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    RadioButton(
        modifier = modifier,
        selected = isSelected,
        onClick = onClick,
        colors = bitwardenRadioButtonColors(),
    )
}
