package com.x8bit.bitwarden.ui.vault.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenSelectionRow
import com.x8bit.bitwarden.ui.vault.components.model.CreateVaultItemType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Reusable dialog for making selections between supported [VaultItemCipherType]s
 *
 * @param onOptionSelected Lambda to be called when an item is selected.
 * @param onDismissRequest Lambda to call when the dialog is dismissed by user.
 * @param selectedOption The currently selected item, if null will assign the selected item to
 * the default type.
 * @param shouldDismissOnSelection Whether or not to call [onDismissRequest] when a selection is
 * made. Default value is set to true.
 * @param excludedOptions List of [VaultItemCipherType] to exclude from the presented list.
 */
@Composable
fun VaultItemSelectionDialog(
    onOptionSelected: (option: CreateVaultItemType) -> Unit,
    onDismissRequest: () -> Unit,
    selectedOption: CreateVaultItemType? = null,
    shouldDismissOnSelection: Boolean = true,
    // TODO: PM-TBD possibly remove SSH_KEY as default exclusion once added SSH_KEY is enabled.
    excludedOptions: ImmutableList<CreateVaultItemType> = persistentListOf(
        CreateVaultItemType.SSH_KEY,
    ),
) {
    val selectedOptionOrDefault =
        selectedOption ?: VaultItemCipherType.LOGIN
    val supportedEntries = CreateVaultItemType
        .entries
        .filterNot { excludedOptions.contains(it) }
    BitwardenSelectionDialog(
        title = stringResource(R.string.type),
        onDismissRequest = onDismissRequest,
    ) {
        supportedEntries.forEach {
            BitwardenSelectionRow(
                text = it.selectionText.asText(),
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
