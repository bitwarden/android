package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenListItem
import com.x8bit.bitwarden.ui.platform.components.SelectionItemData
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A Composable function that displays a row send item.
 *
 * @param label The primary text label to display for the item.
 * @param supportingLabel An secondary text label to display beneath the label.
 * @param startIcon The [Painter] object used to draw the icon at the start of the item.
 * @param onClick The lambda to be invoked when the item is clicked.
 * @param onEditClick The lambda to be invoked when the edit option is clicked from the menu.
 * @param onCopyClick The lambda to be invoked when the copy option is clicked from the menu.
 * @param onShareClick The lambda to be invoked when the share option is clicked from the menu.
 * @param modifier An optional [Modifier] for this Composable, defaulting to an empty Modifier.
 * This allows the caller to specify things like padding, size, etc.
 */
@Suppress("LongMethod")
@Composable
fun SendListItem(
    label: String,
    supportingLabel: String,
    startIcon: Painter,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenListItem(
        label = label,
        supportingLabel = supportingLabel,
        startIcon = startIcon,
        onClick = onClick,
        selectionDataList = listOf(
            SelectionItemData(
                text = stringResource(id = R.string.edit),
                onClick = onEditClick,
            ),
            SelectionItemData(
                text = stringResource(id = R.string.copy_link),
                onClick = onCopyClick,
            ),
            SelectionItemData(
                text = stringResource(id = R.string.share_link),
                onClick = onShareClick,
            ),
        ),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun SendListItem_preview() {
    BitwardenTheme {
        SendListItem(
            label = "Sample Label",
            supportingLabel = "Jan 3, 2024, 10:35 AM",
            startIcon = painterResource(id = R.drawable.ic_send_text),
            onClick = {},
            onCopyClick = {},
            onEditClick = {},
            onShareClick = {},
        )
    }
}
