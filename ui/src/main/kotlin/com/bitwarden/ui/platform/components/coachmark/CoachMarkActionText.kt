package com.bitwarden.ui.platform.components.coachmark

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Clickable text used for the standard action UI for a Coach Mark which applies
 * correct text style by default.
 */
@Composable
fun CoachMarkActionText(
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenClickableText(
        label = actionLabel,
        onClick = onActionClick,
        style = BitwardenTheme.typography.labelLarge,
        modifier = modifier,
    )
}
