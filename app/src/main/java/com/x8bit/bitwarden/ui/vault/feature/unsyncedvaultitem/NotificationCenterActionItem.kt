package com.x8bit.bitwarden.ui.vault.feature.unsyncedvaultitem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Displays an icon representing the Notification Center
 *
 * @param onClick An action to be invoked when the icon is clicked.
 */
@Composable
fun NotificationCenterActionItem(
    onClick: () -> Unit,
) {
    val contentDescription = stringResource(id = R.string.account)

    BitwardenStandardIconButton(
        vectorIconRes = R.drawable.ic,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = Modifier.testTag(tag = "NotificationCenter"),
    )
}

@Preview(showBackground = true)
@Composable
private fun NotificationCenterActionItem_preview() {
    BitwardenTheme {
        NotificationCenterActionItem (
            onClick = {},
        )
    }
}
