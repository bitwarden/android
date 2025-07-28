package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenBasicDialogRow
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenSelectionRow
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriMatchDisplayType
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toDisplayMatchType
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toUriMatchType

/**
 * The URI item displayed to the user.
 */
@Suppress("LongMethod")
@Composable
fun VaultAddEditUriItem(
    uriItem: UriItem,
    onUriItemRemoved: (UriItem) -> Unit,
    onUriValueChange: (UriItem) -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    var shouldShowOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var shouldShowMatchDialog by rememberSaveable { mutableStateOf(false) }

    BitwardenTextField(
        label = stringResource(id = BitwardenString.website_uri),
        value = uriItem.uri.orEmpty(),
        onValueChange = { onUriValueChange(uriItem.copy(uri = it)) },
        actions = {
            BitwardenStandardIconButton(
                vectorIconRes = BitwardenDrawable.ic_cog,
                contentDescription = stringResource(id = BitwardenString.options),
                onClick = { shouldShowOptionsDialog = true },
                modifier = Modifier.testTag(tag = "LoginUriOptionsButton"),
            )
        },
        textFieldTestTag = "LoginUriEntry",
        cardStyle = cardStyle,
        modifier = modifier,
    )

    if (shouldShowOptionsDialog) {
        BitwardenSelectionDialog(
            title = stringResource(id = BitwardenString.options),
            onDismissRequest = { shouldShowOptionsDialog = false },
        ) {
            BitwardenBasicDialogRow(
                text = stringResource(id = BitwardenString.match_detection),
                onClick = {
                    shouldShowOptionsDialog = false
                    shouldShowMatchDialog = true
                },
            )
            BitwardenBasicDialogRow(
                text = stringResource(id = BitwardenString.remove),
                onClick = {
                    shouldShowOptionsDialog = false
                    onUriItemRemoved(uriItem)
                },
            )
        }
    }

    if (shouldShowMatchDialog) {
        val selectedString = uriItem.match.toDisplayMatchType().text.invoke()

        BitwardenSelectionDialog(
            title = stringResource(id = BitwardenString.uri_match_detection),
            onDismissRequest = { shouldShowMatchDialog = false },
        ) {
            UriMatchDisplayType
                .entries
                .forEach { matchType ->
                    BitwardenSelectionRow(
                        text = matchType.text,
                        isSelected = matchType.text.invoke() == selectedString,
                        onClick = {
                            shouldShowMatchDialog = false
                            onUriValueChange(
                                uriItem.copy(match = matchType.toUriMatchType()),
                            )
                        },
                    )
                }
        }
    }
}
