package com.x8bit.bitwarden.ui.platform.components.text

import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.bitwarden.ui.platform.base.util.annotatedStringResource
import com.bitwarden.ui.platform.base.util.spanStyleOf
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Uses an annotated string resource to create a string with clickable text.
 *
 * Note: This only supports one clickable text section.
 */
@Composable
fun BitwardenHyperTextLink(
    @StringRes annotatedResId: Int,
    vararg args: String,
    annotationKey: String,
    accessibilityString: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = BitwardenTheme.typography.labelMedium,
    color: Color = BitwardenTheme.colorScheme.text.secondary,
) {
    Text(
        text = annotatedStringResource(
            id = annotatedResId,
            args = args,
            style = spanStyleOf(color = color, textStyle = style),
            onAnnotationClick = { key ->
                when (key) {
                    annotationKey -> onClick()
                }
            },
        ),
        style = style,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(
                        label = accessibilityString,
                        action = {
                            onClick()
                            true
                        },
                    ),
                )
            },
    )
}
