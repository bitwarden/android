package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.model.IconResource
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A composable function that displays a customizable item
 * with an outlined text field and two icons.
 *
 * @param label Label for the outlined text field.
 * @param value Current value of the outlined text field.
 * @param firstIconResource The resource data for the first icon.
 * @param onFirstIconClick Callback for when the first icon is clicked.
 * @param secondIconResource The resource data for the second icon.
 * @param onSecondIconClick Callback for when the second icon is clicked.
 * @param modifier Modifier for the outermost Box. Default is [Modifier].
 */
@Composable
fun BitwardenTextFieldWithTwoIcons(
    label: String,
    value: String,
    firstIconResource: IconResource,
    onFirstIconClick: () -> Unit,
    secondIconResource: IconResource,
    onSecondIconClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                modifier = modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        contentDescription = "$label, $value"
                    },
                readOnly = true,
                label = {
                    Text(
                        text = label,
                    )
                },
                value = value,
                onValueChange = {
                    // no-op
                },
            )
            RowOfIconButtons(
                modifier = modifier,
                firstIconResource = firstIconResource,
                onFirstIconClick = onFirstIconClick,
                secondIconResource = secondIconResource,
                onSecondIconClick = onSecondIconClick,
            )
        }
    }
}

/**
 * A row of two customizable icon buttons.
 *
 * @param modifier Modifier for the Row.
 * @param firstIconResource The resource data for the first icon button.
 * @param onFirstIconClick Callback for when the first icon button is clicked.
 * @param secondIconResource The resource data for the second icon button.
 * @param onSecondIconClick Callback for when the second icon button is clicked.
 */
@Composable
private fun RowOfIconButtons(
    modifier: Modifier,
    firstIconResource: IconResource,
    onFirstIconClick: () -> Unit,
    secondIconResource: IconResource,
    onSecondIconClick: () -> Unit,
) {
    Row(
        modifier = modifier.padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButtonWithResource(firstIconResource, onClick = onFirstIconClick)
        IconButtonWithResource(secondIconResource, onClick = onSecondIconClick)
    }
}

/**
 * An icon button that displays an icon from the provided [IconResource].
 *
 * @param onClick Callback for when the icon button is clicked.
 */
@Composable
private fun IconButtonWithResource(iconRes: IconResource, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Icon(
            painter = iconRes.iconPainter,
            contentDescription = iconRes.contentDescription,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Preview
@Composable
private fun BitwardenTextFieldWithTwoIcons_preview() {
    BitwardenTheme {
        BitwardenTextFieldWithTwoIcons(
            label = "Label",
            value = "",
            firstIconResource = IconResource(
                iconPainter = painterResource(R.drawable.ic_copy),
                contentDescription = stringResource(id = R.string.copy),
            ),
            onFirstIconClick = {},
            secondIconResource = IconResource(
                iconPainter = painterResource(R.drawable.ic_generator),
                contentDescription = stringResource(id = R.string.generate_password),
            ),
            onSecondIconClick = {},
        )
    }
}
