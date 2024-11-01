package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTonalIconButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenBasicDialogRow
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenSelectionRow
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextFieldWithActions
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
) {
    var shouldShowOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var shouldShowMatchDialog by rememberSaveable { mutableStateOf(false) }

    BitwardenTextFieldWithActions(
        label = stringResource(id = R.string.uri),
        value = uriItem.uri.orEmpty(),
        onValueChange = { onUriValueChange(uriItem.copy(uri = it)) },
        actions = {
            BitwardenTonalIconButton(
                vectorIconRes = R.drawable.ic_cog,
                contentDescription = stringResource(id = R.string.options),
                onClick = { shouldShowOptionsDialog = true },
                modifier = Modifier.testTag(tag = "LoginUriOptionsButton"),
            )
        },
        modifier = Modifier
            .padding(horizontal = 16.dp),
        textFieldTestTag = "LoginUriEntry",
    )

    if (shouldShowOptionsDialog) {
        BitwardenSelectionDialog(
            title = stringResource(id = R.string.options),
            onDismissRequest = { shouldShowOptionsDialog = false },
        ) {
            BitwardenBasicDialogRow(
                text = stringResource(id = R.string.match_detection),
                onClick = {
                    shouldShowOptionsDialog = false
                    shouldShowMatchDialog = true
                },
            )
            BitwardenBasicDialogRow(
                text = stringResource(id = R.string.remove),
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
            title = stringResource(id = R.string.uri_match_detection),
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
