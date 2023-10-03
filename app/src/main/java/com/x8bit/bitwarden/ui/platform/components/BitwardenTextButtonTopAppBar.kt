package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R

/**
 * Represents a Bitwarden styled [TopAppBar] that assumes the following components:
 *
 * - a single navigation control in the upper-left defined by [navigationIcon],
 *   [navigationIconContentDescription], and [onNavigationIconClick].
 * - a [title] in the middle.
 * - a [BitwardenTextButton] on the right that will display [buttonText] and call [onButtonClick]
 * when clicked and [isButtonEnabled] is true.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitwardenTextButtonTopAppBar(
    title: String,
    navigationIcon: Painter,
    navigationIconContentDescription: String,
    onNavigationIconClick: () -> Unit,
    buttonText: String,
    onButtonClick: () -> Unit,
    isButtonEnabled: Boolean,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = { onNavigationIconClick() },
            ) {
                Icon(
                    painter = navigationIcon,
                    contentDescription = navigationIconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        actions = {
            BitwardenTextButton(
                label = buttonText,
                onClick = onButtonClick,
                isEnabled = isButtonEnabled,
            )
        },
    )
}

@Preview
@Composable
private fun BitwardenTextButtonTopAppBar_preview() {
    BitwardenTextButtonTopAppBar(
        title = "Title",
        navigationIcon = painterResource(id = R.drawable.ic_close),
        navigationIconContentDescription = stringResource(id = R.string.close),
        onNavigationIconClick = {},
        buttonText = "Button",
        onButtonClick = {},
        isButtonEnabled = true,
    )
}
