package com.bitwarden.authenticator.ui.platform.components.icon

import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.components.model.IconResource
import com.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme

/**
 * An icon button that displays an icon from the provided [IconResource].
 *
 * @param iconRes Icon to display on the button.
 * @param onClick Callback for when the icon button is clicked.
 * @param isEnabled Whether or not the button should be enabled.
 * @param modifier A [Modifier] for the composable.
 */
@Composable
fun BitwardenIconButtonWithResource(
    iconRes: IconResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    FilledIconButton(
        modifier = modifier.semantics(mergeDescendants = true) {},
        onClick = onClick,
        colors = IconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f),
            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        enabled = isEnabled,
    ) {
        Icon(
            painter = iconRes.iconPainter,
            contentDescription = iconRes.contentDescription,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenIconButtonWithResource_preview() {
    AuthenticatorTheme {
        BitwardenIconButtonWithResource(
            iconRes = IconResource(
                iconPainter = painterResource(id = R.drawable.ic_tooltip),
                contentDescription = "Sample Icon",
            ),
            onClick = {},
        )
    }
}
