package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a row of text that can be clicked on and contains an external link.
 *
 * @param text The label for the row as a [String].
 * @param onClick The callback when the row is clicked.
 * @param modifier The modifier to be applied to the layout.
 * @param withDivider Indicates if a divider should be drawn on the bottom of the row, defaults
 * to `true`.
 */
@Composable
fun BitwardenExternalLinkRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    withDivider: Boolean = true,
) {
    BitwardenTextRow(
        text = text,
        onClick = onClick,
        modifier = modifier,
        withDivider = withDivider,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_external_link),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview
@Composable
private fun BitwardenExternalLinkRow_preview() {
    BitwardenTheme {
        BitwardenExternalLinkRow(
            text = "Linked Text",
            onClick = { },
        )
    }
}
