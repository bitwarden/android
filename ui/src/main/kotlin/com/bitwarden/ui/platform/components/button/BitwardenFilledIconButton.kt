package com.bitwarden.ui.platform.components.button

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.bitwarden.ui.platform.base.util.interactiveBorder
import com.bitwarden.ui.platform.components.button.color.bitwardenFilledIconButtonColors
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.theme.ripple.NoFocusRippleConfig

/**
 * A filled icon button that displays an icon.
 *
 * @param vectorIconRes Icon to display on the button.
 * @param contentDescription The content description for this icon button.
 * @param onClick Callback for when the icon button is clicked.
 * @param modifier A [Modifier] for the composable.
 * @param interactionSource A [MutableInteractionSource] for observing and emitting interactions
 * for this component.
 * @param isEnabled Whether the button should be enabled.
 */
@Composable
fun BitwardenFilledIconButton(
    @DrawableRes vectorIconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    isEnabled: Boolean = true,
) {
    val colors = bitwardenFilledIconButtonColors()
    CompositionLocalProvider(LocalRippleConfiguration provides NoFocusRippleConfig) {
        FilledIconButton(
            onClick = onClick,
            colors = colors,
            enabled = isEnabled,
            shape = BitwardenTheme.shapes.button,
            interactionSource = interactionSource,
            modifier = modifier
                .interactiveBorder(
                    interactionSource = interactionSource,
                    shape = BitwardenTheme.shapes.button,
                    color = colors.containerColor,
                )
                .semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                },
        ) {
            Icon(
                painter = rememberVectorPainter(id = vectorIconRes),
                contentDescription = null,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenFilledIconButton_preview() {
    BitwardenTheme {
        BitwardenFilledIconButton(
            vectorIconRes = BitwardenDrawable.ic_question_circle,
            contentDescription = "Sample Icon",
            onClick = {},
        )
    }
}
