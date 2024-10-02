package com.x8bit.bitwarden.ui.platform.components.account

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.color.bitwardenStandardIconButtonColors
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A placeholder item to be used to represent an account.
 *
 * @param onClick An action to be invoked when the icon is clicked.
 */
@Composable
fun BitwardenPlaceholderAccountActionItem(
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        colors = bitwardenStandardIconButtonColors(),
        modifier = Modifier
            .semantics(mergeDescendants = true) { testTag = "CurrentActiveAccount" },
    ) {
        Icon(
            painter = rememberVectorPainter(id = R.drawable.ic_account_initials_container),
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.background.tertiary,
        )
        Icon(
            painter = rememberVectorPainter(id = R.drawable.ic_dots),
            contentDescription = stringResource(id = R.string.account),
            tint = BitwardenTheme.colorScheme.text.interaction,
        )
    }
}

@Preview
@Composable
private fun BitwardenPlaceholderAccountActionItem_preview_light() {
    BitwardenTheme(theme = AppTheme.LIGHT) {
        BitwardenPlaceholderAccountActionItem(
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun BitwardenPlaceholderAccountActionItem_preview_dark() {
    BitwardenTheme(theme = AppTheme.DARK) {
        BitwardenPlaceholderAccountActionItem(
            onClick = {},
        )
    }
}
