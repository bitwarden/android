package com.bitwarden.ui.platform.components.tooltip

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

private val MIN_TOOLTIP_WIDTH = 312.dp

/**
 * Bitwarden themed rich tool-tip to show within a [TooltipScope].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipScope.BitwardenToolTip(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    leftAction: (@Composable RowScope.() -> Unit)? = null,
    rightAction: (@Composable RowScope.() -> Unit)? = null,
) {
    PlainTooltip(
        modifier = modifier.requiredSizeIn(minWidth = MIN_TOOLTIP_WIDTH),
        caretShape = TooltipDefaults.caretShape(caretSize = DpSize(width = 24.dp, height = 12.dp)),
        shape = BitwardenTheme.shapes.coachmark,
        contentColor = BitwardenTheme.colorScheme.text.primary,
        containerColor = BitwardenTheme.colorScheme.background.secondary,
    ) {
        // PlainTooltip already applies 8.dp of horizontal padding and 4.dp of vertical to the
        // content.
        Column(
            modifier = Modifier.padding(
                bottom = 4.dp,
                start = 8.dp,
                end = 8.dp,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = BitwardenTheme.typography.eyebrowMedium,
                    color = BitwardenTheme.colorScheme.text.secondary,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                Spacer(modifier = Modifier.weight(1f))
                onDismiss?.let {
                    BitwardenStandardIconButton(
                        painter = rememberVectorPainter(BitwardenDrawable.ic_close_small),
                        contentDescription = stringResource(BitwardenString.close),
                        onClick = it,
                        modifier = Modifier.offset(x = 16.dp),
                    )
                }
            }
            Text(
                text = description,
                style = BitwardenTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                leftAction?.invoke(this)
                Spacer(modifier = Modifier.weight(1f))
                rightAction?.invoke(this)
            }
        }
    }
}
