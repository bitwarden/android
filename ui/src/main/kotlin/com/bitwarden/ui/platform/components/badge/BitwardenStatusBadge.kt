package com.bitwarden.ui.platform.components.badge

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.theme.color.BitwardenColorScheme

/**
 * A reusable status badge composable that displays a colored pill with a label.
 *
 * @param label The text to display in the badge.
 * @param colors The border, background, and text colors for the badge.
 * @param modifier The [Modifier] to apply to this badge.
 */
@Composable
fun BitwardenStatusBadge(
    label: String,
    colors: BitwardenColorScheme.StatusBadgeVariantColors,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(size = 12.dp)
    Surface(
        shape = shape,
        color = colors.background,
        modifier = modifier
            .height(24.dp)
            .border(
                width = 1.dp,
                color = colors.border,
                shape = shape,
            ),
    ) {
        Text(
            text = label,
            style = BitwardenTheme.typography.labelSmall,
            color = colors.text,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 4.dp,
            ),
        )
    }
}

@OmitFromCoverage
@Preview
@Composable
private fun BitwardenStatusBadge_Preview() {
    BitwardenTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BitwardenStatusBadge(
                label = "Active",
                colors = BitwardenTheme.colorScheme.statusBadge.success,
            )
            BitwardenStatusBadge(
                label = "Canceled",
                colors = BitwardenTheme.colorScheme.statusBadge.error,
            )
            BitwardenStatusBadge(
                label = "Update payment",
                colors = BitwardenTheme.colorScheme.statusBadge.warning,
            )
        }
    }
}

@OmitFromCoverage
@Preview
@Composable
private fun BitwardenStatusBadge_PreviewDark() {
    BitwardenTheme(theme = AppTheme.DARK) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BitwardenStatusBadge(
                label = "Active",
                colors = BitwardenTheme.colorScheme.statusBadge.success,
            )
            BitwardenStatusBadge(
                label = "Canceled",
                colors = BitwardenTheme.colorScheme.statusBadge.error,
            )
            BitwardenStatusBadge(
                label = "Update payment",
                colors = BitwardenTheme.colorScheme.statusBadge.warning,
            )
        }
    }
}
