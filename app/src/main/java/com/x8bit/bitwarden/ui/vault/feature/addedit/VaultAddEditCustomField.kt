package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTonalIconButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTextEntryDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenBasicDialogRow
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenRowOfActions
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldAction
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The UI element used to display custom field items.
 *
 * @param customField The field that is to be displayed.
 * @param onCustomFieldValueChange Invoked when the user changes the value.
 * @param onCustomFieldAction Invoked when the user chooses an action.
 * @param modifier Modifier for the UI elements.
 * @param supportedLinkedTypes The supported linked types for the vault item.
 * @param onHiddenVisibilityChanged Emits when the visibility of a hidden custom field changes.
 */
@Composable
@Suppress("LongMethod")
fun VaultAddEditCustomField(
    customField: VaultAddEditState.Custom,
    onCustomFieldValueChange: (VaultAddEditState.Custom) -> Unit,
    onCustomFieldAction: (CustomFieldAction, VaultAddEditState.Custom) -> Unit,
    modifier: Modifier = Modifier,
    supportedLinkedTypes: ImmutableList<VaultLinkedFieldType> = persistentListOf(),
    onHiddenVisibilityChanged: (Boolean) -> Unit,
) {
    var shouldShowChooserDialog by remember { mutableStateOf(false) }
    var shouldShowEditDialog by remember { mutableStateOf(false) }

    if (shouldShowChooserDialog) {
        CustomFieldActionDialog(
            onCustomFieldAction = { action ->
                shouldShowChooserDialog = false
                onCustomFieldAction(action, customField)
            },
            onDismissRequest = { shouldShowChooserDialog = false },
            onEditAction = {
                shouldShowEditDialog = true
                shouldShowChooserDialog = false
            },
        )
    }

    if (shouldShowEditDialog) {
        BitwardenTextEntryDialog(
            title = stringResource(id = R.string.custom_field_name),
            textFieldLabel = stringResource(id = R.string.name),
            onDismissRequest = { shouldShowEditDialog = false },
            autoFocus = true,
            initialText = customField.name,
            onConfirmClick = { name ->
                onCustomFieldValueChange(customField.updateName(name))
                shouldShowEditDialog = false
            },
        )
    }

    when (customField) {
        is VaultAddEditState.Custom.BooleanField -> {
            CustomFieldBoolean(
                label = customField.name,
                value = customField.value,
                onValueChanged = { onCustomFieldValueChange(customField.copy(value = it)) },
                onEditValue = { shouldShowChooserDialog = true },
                modifier = modifier,
            )
        }

        is VaultAddEditState.Custom.HiddenField -> {
            CustomFieldHiddenField(
                label = customField.name,
                value = customField.value,
                onValueChanged = {
                    onCustomFieldValueChange(customField.copy(value = it))
                },
                onVisibilityChanged = onHiddenVisibilityChanged,
                onEditValue = { shouldShowChooserDialog = true },
                modifier = modifier,
            )
        }

        is VaultAddEditState.Custom.LinkedField -> {
            customField.vaultLinkedFieldType?.let { fieldType ->
                CustomFieldLinkedField(
                    selectedOption = fieldType,
                    label = customField.name,
                    supportedLinkedTypes = supportedLinkedTypes,
                    onValueChanged = {
                        onCustomFieldValueChange(customField.copy(vaultLinkedFieldType = it))
                    },
                    onEditValue = { shouldShowChooserDialog = true },
                    modifier = modifier,
                )
            }
        }

        is VaultAddEditState.Custom.TextField -> {
            CustomFieldTextField(
                label = customField.name,
                value = customField.value,
                onValueChanged = { onCustomFieldValueChange(customField.copy(value = it)) },
                onEditValue = { shouldShowChooserDialog = true },
                modifier = modifier,
            )
        }
    }
}

/**
 * A UI element that is used to display custom field boolean fields.
 */
@Composable
private fun CustomFieldBoolean(
    label: String,
    value: Boolean,
    onValueChanged: (Boolean) -> Unit,
    onEditValue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BitwardenSwitch(
            label = label,
            isChecked = value,
            onCheckedChange = onValueChanged,
            modifier = Modifier.weight(1f),
        )

        BitwardenRowOfActions(
            actions = {
                BitwardenTonalIconButton(
                    vectorIconRes = R.drawable.ic_cog,
                    contentDescription = stringResource(id = R.string.edit),
                    onClick = onEditValue,
                )
            },
        )
    }
}

/**
 * A UI element that is used to display custom field hidden fields.
 */
@Composable
private fun CustomFieldHiddenField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    onEditValue: () -> Unit,
    onVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowPassword by remember { mutableStateOf(value = false) }
    BitwardenPasswordFieldWithActions(
        label = label,
        value = value,
        onValueChange = onValueChanged,
        showPassword = shouldShowPassword,
        showPasswordChange = {
            shouldShowPassword = !shouldShowPassword
            onVisibilityChanged(shouldShowPassword)
        },
        singleLine = true,
        modifier = modifier,
        actions = {
            BitwardenTonalIconButton(
                vectorIconRes = R.drawable.ic_cog,
                contentDescription = stringResource(id = R.string.edit),
                onClick = onEditValue,
            )
        },
    )
}

/**
 * A UI element that is used to display custom field text fields.
 */
@Composable
private fun CustomFieldTextField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    onEditValue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenTextFieldWithActions(
        label = label,
        value = value,
        onValueChange = onValueChanged,
        singleLine = true,
        modifier = modifier,
        actions = {
            BitwardenTonalIconButton(
                vectorIconRes = R.drawable.ic_cog,
                contentDescription = stringResource(id = R.string.edit),
                onClick = onEditValue,
            )
        },
    )
}

/**
 * A UI element that is used to display custom field linked fields.
 */
@Composable
private fun CustomFieldLinkedField(
    label: String,
    selectedOption: VaultLinkedFieldType,
    onValueChanged: (VaultLinkedFieldType) -> Unit,
    onEditValue: () -> Unit,
    modifier: Modifier = Modifier,
    supportedLinkedTypes: ImmutableList<VaultLinkedFieldType> = persistentListOf(),
) {
    val possibleTypesWithStrings = supportedLinkedTypes.associateWith { it.label.invoke() }

    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BitwardenMultiSelectButton(
            label = label,
            options = supportedLinkedTypes.map { it.label.invoke() }.toImmutableList(),
            selectedOption = selectedOption.label.invoke(),
            onOptionSelected = { selectedType ->
                possibleTypesWithStrings.forEach {
                    if (it.value == selectedType) {
                        onValueChanged(it.key)
                    }
                }
            },
            modifier = Modifier.weight(1f),
        )

        BitwardenRowOfActions(
            actions = {
                BitwardenTonalIconButton(
                    vectorIconRes = R.drawable.ic_cog,
                    contentDescription = stringResource(id = R.string.edit),
                    onClick = onEditValue,
                )
            },
        )
    }
}

/**
 * A dialog for editing a custom field item.
 */
@Composable
private fun CustomFieldActionDialog(
    onCustomFieldAction: (CustomFieldAction) -> Unit,
    onEditAction: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    BitwardenSelectionDialog(
        title = stringResource(id = R.string.options),
        onDismissRequest = onDismissRequest,
    ) {
        CustomFieldAction
            .entries
            .forEach { action ->
                BitwardenBasicDialogRow(
                    text = action.actionText.invoke(),
                    onClick = {
                        if (action == CustomFieldAction.EDIT) {
                            onEditAction()
                        } else {
                            onCustomFieldAction(action)
                        }
                    },
                )
            }
    }
}

/**
 * A helper method that will copy over the new name to a custom field.
 */
private fun VaultAddEditState.Custom.updateName(name: String): VaultAddEditState.Custom =
    when (this) {
        is VaultAddEditState.Custom.BooleanField -> this.copy(name = name)
        is VaultAddEditState.Custom.HiddenField -> this.copy(name = name)
        is VaultAddEditState.Custom.LinkedField -> this.copy(name = name)
        is VaultAddEditState.Custom.TextField -> this.copy(name = name)
    }
