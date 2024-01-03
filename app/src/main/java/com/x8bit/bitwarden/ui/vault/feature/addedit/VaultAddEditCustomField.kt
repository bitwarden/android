package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.showNotYetImplementedToast
import com.x8bit.bitwarden.ui.platform.components.BitwardenIconButtonWithResource
import com.x8bit.bitwarden.ui.platform.components.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenPasswordFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenRowOfActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenWideSwitch
import com.x8bit.bitwarden.ui.platform.components.model.IconResource
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The UI element used to display custom field items.
 *
 * @param customField The field that is to be displayed.
 * @param onCustomFieldValueChange Invoked when the user changes the value.
 * @param modifier Modifier for the UI elements.
 * @param supportedLinkedTypes The supported linked types for the vault item.
 */
@Composable
@Suppress("LongMethod")
fun VaultAddEditCustomField(
    customField: VaultAddEditState.Custom,
    onCustomFieldValueChange: (VaultAddEditState.Custom) -> Unit,
    modifier: Modifier = Modifier,
    supportedLinkedTypes: ImmutableList<VaultLinkedFieldType> = persistentListOf(),
) {
    when (customField) {
        is VaultAddEditState.Custom.BooleanField -> {
            CustomFieldBoolean(
                label = customField.name,
                value = customField.value,
                onValueChanged = { onCustomFieldValueChange(customField.copy(value = it)) },
                modifier = modifier,
            )
        }

        is VaultAddEditState.Custom.HiddenField -> {
            CustomFieldHiddenField(
                customField.name,
                customField.value,
                onValueChanged = {
                    onCustomFieldValueChange(customField.copy(value = it))
                },
                modifier = modifier,
            )
        }

        is VaultAddEditState.Custom.LinkedField -> {
            CustomFieldLinkedField(
                selectedOption = customField.vaultLinkedFieldType,
                supportedLinkedTypes = supportedLinkedTypes,
                onValueChanged = {
                    onCustomFieldValueChange(customField.copy(vaultLinkedFieldType = it))
                },
                modifier = modifier,
            )
        }

        is VaultAddEditState.Custom.TextField -> {
            CustomFieldTextField(
                label = customField.name,
                value = customField.value,
                onValueChanged = { onCustomFieldValueChange(customField.copy(value = it)) },
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BitwardenWideSwitch(
            label = label,
            isChecked = value,
            onCheckedChange = onValueChanged,
            modifier = Modifier.weight(1f),
        )

        BitwardenRowOfActions(
            actions = {
                BitwardenIconButtonWithResource(
                    iconRes = IconResource(
                        iconPainter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = stringResource(id = R.string.edit),
                    ),
                    onClick = {
                        // TODO add support for custom field actions (BIT-540)
                        showNotYetImplementedToast(context = context)
                    },
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    BitwardenPasswordFieldWithActions(
        label = label,
        value = value,
        onValueChange = onValueChanged,
        singleLine = true,
        modifier = modifier,
        actions = {
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = stringResource(id = R.string.edit),
                ),
                onClick = {
                    // TODO Add support for custom field actions (BIT-540)
                    showNotYetImplementedToast(context = context)
                },
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    BitwardenTextFieldWithActions(
        label = label,
        value = value,
        onValueChange = onValueChanged,
        singleLine = true,
        modifier = modifier,
        actions = {
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = stringResource(id = R.string.edit),
                ),
                onClick = {
                    // TODO add support for custom field actions (BIT-540)
                    showNotYetImplementedToast(context = context)
                },
            )
        },
    )
}

/**
 * A UI element that is used to display custom field linked fields.
 */
@Composable
private fun CustomFieldLinkedField(
    selectedOption: VaultLinkedFieldType,
    onValueChanged: (VaultLinkedFieldType) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    supportedLinkedTypes: ImmutableList<VaultLinkedFieldType> = persistentListOf(),
) {
    val context = LocalContext.current
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
                BitwardenIconButtonWithResource(
                    iconRes = IconResource(
                        iconPainter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = stringResource(id = R.string.edit),
                    ),
                    onClick = {
                        // TODO add support for custom field actions (BIT-540)
                        showNotYetImplementedToast(context = context)
                    },
                )
            },
        )
    }
}
