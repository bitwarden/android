package com.x8bit.bitwarden.ui.vault.feature.item.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemState

/**
 * Custom Field UI common for all item types.
 */
@Suppress("LongMethod")
@Composable
fun CustomField(
    customField: VaultItemState.ViewState.Content.Common.Custom,
    onCopyCustomHiddenField: (String) -> Unit,
    onCopyCustomTextField: (String) -> Unit,
    onShowHiddenFieldClick: (
        VaultItemState.ViewState.Content.Common.Custom.HiddenField,
        Boolean,
    ) -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    when (customField) {
        is VaultItemState.ViewState.Content.Common.Custom.BooleanField -> {
            BitwardenSwitch(
                label = customField.name,
                isChecked = customField.value,
                readOnly = true,
                onCheckedChange = { },
                cardStyle = cardStyle,
                modifier = modifier.testTag("ViewCustomBooleanField"),
            )
        }

        is VaultItemState.ViewState.Content.Common.Custom.HiddenField -> {
            if (customField.isCopyable) {
                BitwardenPasswordField(
                    label = customField.name,
                    value = customField.value,
                    showPasswordChange = { onShowHiddenFieldClick(customField, it) },
                    showPassword = customField.isVisible,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    showPasswordTestTag = "CustomFieldShowPasswordButton",
                    passwordFieldTestTag = "CustomFieldValue",
                    actions = {
                        BitwardenStandardIconButton(
                            vectorIconRes = R.drawable.ic_copy,
                            contentDescription = stringResource(id = R.string.copy),
                            onClick = { onCopyCustomHiddenField(customField.value) },
                            modifier = Modifier.testTag("CustomFieldCopyValueButton"),
                        )
                    },
                    cardStyle = cardStyle,
                    modifier = modifier.testTag("ViewCustomHiddenField"),
                )
            } else {
                BitwardenPasswordField(
                    label = customField.name,
                    value = customField.value,
                    showPasswordChange = { onShowHiddenFieldClick(customField, it) },
                    showPassword = customField.isVisible,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = cardStyle,
                    modifier = modifier,
                )
            }
        }

        is VaultItemState.ViewState.Content.Common.Custom.LinkedField -> {
            BitwardenTextField(
                label = customField.name,
                value = customField.vaultLinkedFieldType.label.invoke(),
                leadingIconData = IconData.Local(
                    iconRes = R.drawable.ic_linked,
                    contentDescription = R.string.field_type_linked.asText(),
                ),
                onValueChange = { },
                readOnly = true,
                singleLine = false,
                cardStyle = cardStyle,
                textFieldTestTag = "CustomFieldDropdown",
                modifier = modifier.testTag("ViewCustomLinkedField"),
            )
        }

        is VaultItemState.ViewState.Content.Common.Custom.TextField -> {
            BitwardenTextField(
                label = customField.name,
                value = customField.value,
                onValueChange = { },
                readOnly = true,
                singleLine = false,
                textFieldTestTag = "CustomFieldValue",
                actions = {
                    if (customField.isCopyable) {
                        BitwardenStandardIconButton(
                            vectorIconRes = R.drawable.ic_copy,
                            contentDescription = stringResource(id = R.string.copy),
                            onClick = { onCopyCustomTextField(customField.value) },
                            modifier = Modifier.testTag("CustomFieldCopyValueButton"),
                        )
                    }
                },
                cardStyle = cardStyle,
                modifier = modifier.testTag("ViewCustomTextField"),
            )
        }
    }
}
