package com.bitwarden.ui.platform.components.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bitwarden.ui.platform.components.button.model.BitwardenHelpButtonData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A filled icon button that displays an icon.
 *
 * @param helpData All the relevant data for displaying a help icon button.
 * @param modifier A [Modifier] for the composable.
 */
@Composable
fun BitwardenHelpIconButton(
    helpData: BitwardenHelpButtonData,
    modifier: Modifier = Modifier,
) {
    BitwardenStandardIconButton(
        vectorIconRes = BitwardenDrawable.ic_question_circle_small,
        contentDescription = if (helpData.isExternalLink) {
            stringResource(
                id = BitwardenString.external_link_format,
                formatArgs = arrayOf(helpData.contentDescription),
            )
        } else {
            helpData.contentDescription
        },
        onClick = helpData.onClick,
        contentColor = BitwardenTheme.colorScheme.icon.secondary,
        modifier = modifier,
    )
}
