package com.x8bit.bitwarden.ui.platform.components.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled filled button with an icon.
 *
 * @param label The label for the button.
 * @param icon The icon for the button.
 * @param onClick The callback when the button is clicked.
 * @param modifier The [Modifier] to be applied to the button.
 * @param isEnabled Whether or not the button is enabled.
 */
@Composable
fun BitwardenFilledButtonWithIcon(
    label: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .semantics(mergeDescendants = true) { },
        enabled = isEnabled,
        contentPadding = PaddingValues(
            vertical = 10.dp,
            horizontal = 24.dp,
        ),
        colors = ButtonDefaults.buttonColors(),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 8.dp),
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenFilledButtonWithIcon_preview() {
    BitwardenTheme {
        BitwardenFilledButtonWithIcon(
            label = "Test Button",
            icon = painterResource(id = R.drawable.ic_tooltip),
            onClick = {},
            isEnabled = true,
        )
    }
}
