package com.bitwarden.authenticator.ui.platform.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bitwarden.authenticator.ui.platform.base.util.Text

/**
 * A clickable item that displays a radio button and text.
 *
 * @param text The text to display.
 * @param onClick Invoked when either the radio button or text is clicked.
 * @param isSelected Whether or not the radio button should be checked.
 */
@Composable
fun BitwardenSelectionRow(
    text: Text,
    onClick: () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                selected = isSelected
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            modifier = Modifier.padding(16.dp),
            selected = isSelected,
            onClick = null,
        )
        Text(
            text = text(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
