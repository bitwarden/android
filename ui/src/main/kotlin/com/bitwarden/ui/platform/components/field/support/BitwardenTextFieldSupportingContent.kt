package com.bitwarden.ui.platform.components.field.support

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.support.BitwardenSupportingContent

/**
 * Lays out the Bitwarden-styled supporting content for a text field.
 *
 * Intended to be reused for all implementations of a TextField, such as the [BitwardenTextField]
 * and [BitwardenPasswordField].
 */
@Composable
internal fun ColumnScope.BitwardenTextFieldSupportingContent(
    content: (@Composable ColumnScope.() -> Unit)?,
    supportingContentPadding: PaddingValues,
    noContentCardPadding: Dp,
) {
    if (content != null) {
        Spacer(modifier = Modifier.height(height = 6.dp))
        BitwardenHorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp),
        )
        BitwardenSupportingContent(
            cardStyle = null,
            insets = supportingContentPadding,
            content = content,
        )
    } else {
        // Pad out the card when there is no content.
        Spacer(modifier = Modifier.height(height = noContentCardPadding))
    }
}
