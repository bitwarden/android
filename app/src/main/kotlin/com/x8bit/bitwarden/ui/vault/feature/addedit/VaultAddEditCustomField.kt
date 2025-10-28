package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTextEntryDialog
import com.bitwarden.ui.platform.components.dialog.row.BitwardenBasicDialogRow
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
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
 * @param onHiddenVisibilityChanged Emits when the visibility of a hidden custom field changes.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier Modifier for the UI elements.
 * @param supportedLinkedTypes The supported linked types for the vault item.
 */
@Composable
@Suppress("LongMethod")
fun VaultAddEditCustomField(
    customField: VaultAddEditState.Custom,
    onCustomFieldValueChange: (VaultAddEditState.Custom) -> Unit,
    onCustomFieldAction: (CustomFieldAction, VaultAddEditState.Custom) -> Unit,
    onHiddenVisibilityChanged: (Boolean) -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
    supportedLinkedTypes: ImmutableList<VaultLinkedFieldType> = persistentListOf(),
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
            title = stringResource(id = BitwardenString.custom_field_name),
            textFieldLabel = stringResource(id = BitwardenString.name),
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
                cardStyle = cardStyle,
                modifier = modifier
                    .fillMaxWidth()
                    .testTag(tag = "AddEditCustomBooleanField"),
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
                cardStyle = cardStyle,
                modifier = modifier.testTag("AddEditCustomHiddenField"),
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
                    cardStyle = cardStyle,
                    modifier = modifier.testTag("AddEditCustomLinkedField"),
                )
            }
        }

        is VaultAddEditState.Custom.TextField -> {
            CustomFieldTextField(
                label = customField.name,
                value = customField.value,
                onValueChanged = { onCustomFieldValueChange(customField.copy(value = it)) },
                onEditValue = { shouldShowChooserDialog = true },
                cardStyle = cardStyle,
                modifier = modifier.testTag("AddEditCustomTextField"),
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
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    BitwardenSwitch(
        modifier = modifier,
        label = label,
        isChecked = value,
        onCheckedChange = onValueChanged,
        cardStyle = cardStyle,
        actions = {
            BitwardenStandardIconButton(
                vectorIconRes = BitwardenDrawable.ic_cog,
                contentDescription = stringResource(id = BitwardenString.edit),
                onClick = onEditValue,
                modifier = Modifier.testTag(tag = "CustomFieldSettingsButton"),
            )
        },
    )
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
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    var shouldShowPassword by remember { mutableStateOf(value = false) }
    BitwardenPasswordField(
        label = label,
        value = value,
        onValueChange = onValueChanged,
        showPassword = shouldShowPassword,
        showPasswordChange = {
            shouldShowPassword = !shouldShowPassword
            onVisibilityChanged(shouldShowPassword)
        },
        singleLine = true,
        showPasswordTestTag = "CustomFieldShowPasswordButton",
        passwordFieldTestTag = "CustomFieldValue",
        actions = {
            BitwardenStandardIconButton(
                vectorIconRes = BitwardenDrawable.ic_cog,
                contentDescription = stringResource(id = BitwardenString.edit),
                onClick = onEditValue,
                modifier = Modifier.testTag("CustomFieldSettingsButton"),
            )
        },
        cardStyle = cardStyle,
        modifier = modifier,
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
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = label,
        value = value,
        onValueChange = onValueChanged,
        singleLine = false,
        textFieldTestTag = "CustomFieldValue",
        actions = {
            BitwardenStandardIconButton(
                vectorIconRes = BitwardenDrawable.ic_cog,
                contentDescription = stringResource(id = BitwardenString.edit),
                onClick = onEditValue,
                modifier = Modifier.testTag("CustomFieldSettingsButton"),
            )
        },
        cardStyle = cardStyle,
        modifier = modifier,
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
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
    supportedLinkedTypes: ImmutableList<VaultLinkedFieldType> = persistentListOf(),
) {
    val possibleTypesWithStrings = supportedLinkedTypes.associateWith { it.label.invoke() }
    BitwardenMultiSelectButton(
        modifier = modifier.testTag("CustomFieldDropdown"),
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
        actions = {
            BitwardenStandardIconButton(
                vectorIconRes = BitwardenDrawable.ic_cog,
                contentDescription = stringResource(id = BitwardenString.edit),
                onClick = onEditValue,
                modifier = Modifier.testTag("CustomFieldSettingsButton"),
            )
        },
        actionsPadding = PaddingValues(end = 4.dp),
        cardStyle = cardStyle,
    )
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
        title = stringResource(id = BitwardenString.options),
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
