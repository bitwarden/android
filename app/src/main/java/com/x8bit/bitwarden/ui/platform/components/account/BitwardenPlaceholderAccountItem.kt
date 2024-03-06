package com.x8bit.bitwarden.ui.platform.components.account

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
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
        modifier = Modifier
            .semantics(mergeDescendants = true) {},
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_account_initials_container),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondaryContainer,
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_dots),
            contentDescription = stringResource(id = R.string.account),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
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
