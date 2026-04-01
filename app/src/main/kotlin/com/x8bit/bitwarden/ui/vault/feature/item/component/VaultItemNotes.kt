package com.x8bit.bitwarden.ui.vault.feature.item.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCommonItemTypeHandlers

/**
 * Displays the common notes field for the vault item screen.
 *
 * @param notes The notes.
 * @param vaultCommonItemTypeHandlers Provides the handlers required for the notes.
 * @param showHeader Indicates whether to show the header.
 */
fun LazyListScope.vaultItemNotes(
    notes: String?,
    vaultCommonItemTypeHandlers: VaultCommonItemTypeHandlers,
    showHeader: Boolean = true,
) {
    notes ?: return
    item(key = "notes") {
        if (showHeader) {
            Spacer(modifier = Modifier.height(height = 16.dp))
            BitwardenListHeaderText(
                label = stringResource(id = BitwardenString.additional_options),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp)
                    .animateItem(),
            )
        }
        Spacer(modifier = Modifier.height(height = 8.dp))
        BitwardenTextField(
            label = stringResource(id = BitwardenString.notes),
            value = notes,
            onValueChange = { },
            readOnly = true,
            singleLine = false,
            actions = {
                BitwardenStandardIconButton(
                    vectorIconRes = BitwardenDrawable.ic_copy,
                    contentDescription = stringResource(id = BitwardenString.copy_notes),
                    onClick = vaultCommonItemTypeHandlers.onCopyNotesClick,
                    modifier = Modifier.testTag(tag = "CipherNotesCopyButton"),
                )
            },
            textFieldTestTag = "CipherNotesLabel",
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .animateItem(),
        )
    }
}
