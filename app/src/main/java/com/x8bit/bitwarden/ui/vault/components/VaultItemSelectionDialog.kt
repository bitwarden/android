package com.x8bit.bitwarden.ui.vault.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenSelectionRow
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

/**
 * Reusable dialog for making selections between supported [VaultItemCipherType]s
 *
 * @param onOptionSelected Lambda to be called when an item is selected.
 * @param onDismissRequest Lambda to call when the dialog is dismissed by user.
 * @param selectedOption The currently selected item, if null will assign the selected item to
 * the default type.
 * @param shouldDismissOnSelection Whether or not to call [onDismissRequest] when a selection is
 * made.
 * @param excludedOptions List of [VaultItemCipherType] to exclude from the presented list.
 */
@Composable
fun VaultItemSelectionDialog(
    onOptionSelected: (option: VaultItemCipherType) -> Unit,
    onDismissRequest: () -> Unit,
    selectedOption: VaultItemCipherType? = null,
    shouldDismissOnSelection: Boolean = false,
    // TODO: PM-TBD possibly remove SSH_KEY as default exclusion once added SSH_KEY is enabled.
    excludedOptions: List<VaultItemCipherType> = listOf(VaultItemCipherType.SSH_KEY),
) {
    val selectedOptionOrDefault =
        selectedOption ?: VaultItemCipherType.LOGIN
    val supportedEntries = VaultItemCipherType
        .entries
        .filterNot { excludedOptions.contains(it) }
    BitwardenSelectionDialog(
        title = stringResource(R.string.type),
        onDismissRequest = onDismissRequest,
    ) {
        supportedEntries.forEach {
            BitwardenSelectionRow(
                text = it.selectionTitle,
                onClick = {
                    if (shouldDismissOnSelection) {
                        onDismissRequest()
                    }
                    onOptionSelected(it)
                },
                isSelected = it == selectedOptionOrDefault,
            )
        }
    }
}

private val VaultItemCipherType.selectionTitle: Text
    get() = when (this) {
        VaultItemCipherType.LOGIN -> R.string.log_in_noun.asText()
        VaultItemCipherType.CARD -> R.string.type_card.asText()
        VaultItemCipherType.IDENTITY -> R.string.type_identity.asText()
        VaultItemCipherType.SECURE_NOTE -> R.string.type_secure_note.asText()
        VaultItemCipherType.SSH_KEY -> R.string.type_ssh_key.asText()
        VaultItemCipherType.FOLDER -> R.string.folder.asText()
    }
