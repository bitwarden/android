package com.bitwarden.authenticator.ui.platform.components.appbar.action

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable

/**
 * Represents the Authenticator search action item.
 *
 * This is an [Icon] composable tailored specifically for the search functionality
 * in the Authenticator app.
 * It presents the search icon and offers an `onClick` callback for when the icon is tapped.
 *
 * @param contentDescription A description of the UI element, used for accessibility purposes.
 * @param onClick A callback to be invoked when this action item is clicked.
 */
@Composable
fun AuthenticatorSearchActionItem(
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.testTag("SearchButton"),
    ) {
        Icon(
            painter = rememberVectorPainter(id = BitwardenDrawable.ic_search_wide),
            contentDescription = contentDescription,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthenticatorSearchActionItem_preview() {
    AuthenticatorSearchActionItem(
        contentDescription = "Search",
        onClick = {},
    )
}
