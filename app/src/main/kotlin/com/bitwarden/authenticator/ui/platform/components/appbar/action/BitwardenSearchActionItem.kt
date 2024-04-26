package com.bitwarden.authenticator.ui.platform.components.appbar.action

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.components.util.rememberVectorPainter

/**
 * Represents the Bitwarden search action item.
 *
 * This is an [Icon] composable tailored specifically for the search functionality
 * in the Bitwarden app.
 * It presents the search icon and offers an `onClick` callback for when the icon is tapped.
 *
 * @param contentDescription A description of the UI element, used for accessibility purposes.
 * @param onClick A callback to be invoked when this action item is clicked.
 */
@Composable
fun BitwardenSearchActionItem(
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.testTag("SearchButton"),
    ) {
        Icon(
            painter = rememberVectorPainter(id = R.drawable.ic_search_24px),
            contentDescription = contentDescription,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenSearchActionItem_preview() {
    BitwardenSearchActionItem(
        contentDescription = "Search",
        onClick = {},
    )
}
