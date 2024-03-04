package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenIconButtonWithResource
import com.x8bit.bitwarden.ui.platform.components.BitwardenPasswordFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.model.IconResource
import com.x8bit.bitwarden.ui.platform.components.switch.BitwardenWideSwitch

/**
 * Custom Field UI common for all item types.
 */
@Suppress("LongMethod", "MaxLineLength")
@Composable
fun CustomField(
    customField: VaultItemState.ViewState.Content.Common.Custom,
    onCopyCustomHiddenField: (String) -> Unit,
    onCopyCustomTextField: (String) -> Unit,
    onShowHiddenFieldClick: (VaultItemState.ViewState.Content.Common.Custom.HiddenField, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (customField) {
        is VaultItemState.ViewState.Content.Common.Custom.BooleanField -> {
            BitwardenWideSwitch(
                label = customField.name,
                isChecked = customField.value,
                readOnly = true,
                onCheckedChange = { },
                modifier = modifier,
            )
        }

        is VaultItemState.ViewState.Content.Common.Custom.HiddenField -> {
            BitwardenPasswordFieldWithActions(
                label = customField.name,
                value = customField.value,
                showPasswordChange = { onShowHiddenFieldClick(customField, it) },
                showPassword = customField.isVisible,
                onValueChange = { },
                readOnly = true,
                singleLine = false,
                modifier = modifier,
                actions = {
                    if (customField.isCopyable) {
                        BitwardenIconButtonWithResource(
                            iconRes = IconResource(
                                iconPainter = painterResource(id = R.drawable.ic_copy),
                                contentDescription = stringResource(id = R.string.copy),
                            ),
                            onClick = {
                                onCopyCustomHiddenField(customField.value)
                            },
                        )
                    }
                },
            )
        }

        is VaultItemState.ViewState.Content.Common.Custom.LinkedField -> {
            BitwardenTextField(
                label = customField.name,
                value = customField.vaultLinkedFieldType.label.invoke(),
                leadingIconResource = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_linked),
                    contentDescription = stringResource(id = R.string.field_type_linked),
                ),
                onValueChange = { },
                readOnly = true,
                singleLine = false,
                modifier = modifier,
            )
        }

        is VaultItemState.ViewState.Content.Common.Custom.TextField -> {
            BitwardenTextFieldWithActions(
                label = customField.name,
                value = customField.value,
                onValueChange = { },
                readOnly = true,
                singleLine = false,
                modifier = modifier,
                actions = {
                    if (customField.isCopyable) {
                        BitwardenIconButtonWithResource(
                            iconRes = IconResource(
                                iconPainter = painterResource(id = R.drawable.ic_copy),
                                contentDescription = stringResource(id = R.string.copy),
                            ),
                            onClick = { onCopyCustomTextField(customField.value) },
                        )
                    }
                },
            )
        }
    }
}
