package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTextEntryDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenBasicDialogRow
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * A UI element that is used by the user to add a custom field item.
 *
 * @param options The types that are to be chosen by the user.
 * @param onFinishNamingClick Invoked when the user finishes naming the item.
 */
@Suppress("LongMethod")
@Composable
fun VaultAddEditCustomFieldsButton(
    onFinishNamingClick: (CustomFieldType, String) -> Unit,
    modifier: Modifier = Modifier,
    options: ImmutableList<CustomFieldType> = persistentListOf(
        CustomFieldType.TEXT,
        CustomFieldType.HIDDEN,
        CustomFieldType.BOOLEAN,
        CustomFieldType.LINKED,
    ),
) {
    var shouldShowChooserDialog by remember { mutableStateOf(false) }
    var shouldShowNameDialog by remember { mutableStateOf(false) }

    var customFieldType: CustomFieldType by remember { mutableStateOf(CustomFieldType.TEXT) }
    var customFieldName: String by remember { mutableStateOf("") }

    if (shouldShowChooserDialog) {
        BitwardenSelectionDialog(
            title = stringResource(id = R.string.select_type_field),
            onDismissRequest = { shouldShowChooserDialog = false },
        ) {
            options.forEach { type ->
                BitwardenBasicDialogRow(
                    text = type.typeText.invoke(),
                    onClick = {
                        shouldShowChooserDialog = false
                        shouldShowNameDialog = true
                        customFieldType = type
                    },
                )
            }
        }
    }

    if (shouldShowNameDialog) {
        BitwardenTextEntryDialog(
            title = stringResource(id = R.string.custom_field_name),
            textFieldLabel = stringResource(id = R.string.name),
            onDismissRequest = { shouldShowNameDialog = false },
            onConfirmClick = {
                shouldShowNameDialog = false
                customFieldName = it
                onFinishNamingClick(customFieldType, customFieldName)
            },
        )
    }

    BitwardenOutlinedButton(
        label = stringResource(id = R.string.new_custom_field),
        onClick = { shouldShowChooserDialog = true },
        modifier = modifier.testTag("NewCustomFieldButton"),
    )
}
