package com.x8bit.bitwarden.authenticator.ui.platform.components.row

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme

/**
 * A composable function to display a row of actions.
 *
 * This function takes in a trailing lambda which provides a `RowScope` in order to
 * layout individual actions. The actions will be arranged in a horizontal
 * sequence, spaced by 8.dp, and are vertically centered.
 *
 * @param actions The composable actions to execute within the [RowScope]. Typically used to
 * layout individual icons or buttons.
 */
@Composable
fun BitwardenRowOfActions(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = actions,
    )
}

@Preview(showBackground = true)
@Composable
private fun BitwardenRowOfIconButtons_preview() {
    AuthenticatorTheme {
        BitwardenRowOfActions {
            Icon(
                painter = painterResource(id = R.drawable.ic_tooltip),
                contentDescription = "Icon 1",
                modifier = Modifier.size(24.dp),
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_tooltip),
                contentDescription = "Icon 2",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
