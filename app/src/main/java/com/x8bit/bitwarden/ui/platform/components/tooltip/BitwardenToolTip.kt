package com.x8bit.bitwarden.ui.platform.components.tooltip

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.tooltip.color.bitwardenTooltipColors
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

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
    RichTooltip(
        modifier = modifier,
        caretSize = DpSize(width = 24.dp, height = 16.dp),
        shape = BitwardenTheme.shapes.coachmark,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = BitwardenTheme.typography.eyebrowMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                onDismiss?.let {
                    BitwardenStandardIconButton(
                        painter = rememberVectorPainter(R.drawable.ic_close_small),
                        contentDescription = stringResource(R.string.close),
                        onClick = it,
                        modifier = Modifier.offset(x = 16.dp, y = (-16).dp),
                    )
                }
            }
        },
        action = {
            Row(
                Modifier.fillMaxWidth(),
            ) {
                leftAction?.invoke(this)
                Spacer(modifier = Modifier.weight(1f))
                rightAction?.invoke(this)
            }
        },
        colors = bitwardenTooltipColors(),
    ) {
        Text(
            text = description,
            style = BitwardenTheme.typography.bodyMedium,
        )
    }
}
