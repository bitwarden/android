package com.bitwarden.ui.platform.components.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.components.button.color.bitwardenOutlinedButtonColors
import com.bitwarden.ui.platform.components.button.model.BitwardenOutlinedButtonColors
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.components.util.throttledClick
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled filled [OutlinedButton].
 *
 * @param label The label for the button.
 * @param onClick The callback when the button is clicked.
 * @param modifier The [Modifier] to be applied to the button.
 * @param icon The icon for the button.
 * @param isEnabled Whether or not the button is enabled.
 * @param cardStyle The optional card style to surround the button.
 * `null`.
 * @param cardInsets The internal insets for the card, only applied when the [cardStyle] is not
 * `null`.
 */
@Composable
fun BitwardenOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    isEnabled: Boolean = true,
    colors: BitwardenOutlinedButtonColors = bitwardenOutlinedButtonColors(),
    cardStyle: CardStyle? = null,
    cardInsets: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
) {
    OutlinedButton(
        modifier = modifier
            .semantics(mergeDescendants = true) { }
            .cardStyle(cardStyle = cardStyle, padding = cardInsets),
        onClick = throttledClick(onClick = onClick),
        enabled = isEnabled,
        contentPadding = PaddingValues(
            top = 10.dp,
            bottom = 10.dp,
            start = if (icon == null) 24.dp else 16.dp,
            end = 24.dp,
        ),
        colors = colors.materialButtonColors,
        border = BorderStroke(
            width = 1.dp,
            color = if (isEnabled) {
                colors.outlineBorderColor
            } else {
                colors.outlinedDisabledBorderColor
            },
        ),
    ) {
        icon?.let {
            Icon(
                painter = icon,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = BitwardenTheme.typography.labelLarge,
        )
    }
}

@Preview
@Composable
private fun BitwardenOutlinedButton_preview() {
    Column {
        BitwardenOutlinedButton(
            label = "Label",
            onClick = {},
            icon = null,
            isEnabled = true,
        )
        BitwardenOutlinedButton(
            label = "Label",
            onClick = {},
            icon = rememberVectorPainter(id = BitwardenDrawable.ic_question_circle),
            isEnabled = true,
        )
        BitwardenOutlinedButton(
            label = "Label",
            onClick = {},
            icon = null,
            isEnabled = false,
        )
        BitwardenOutlinedButton(
            label = "Label",
            onClick = {},
            icon = rememberVectorPainter(id = BitwardenDrawable.ic_question_circle),
            isEnabled = false,
        )
    }
}
