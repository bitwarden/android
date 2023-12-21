package com.x8bit.bitwarden.ui.vault.feature.additem

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledTonalButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledTonalButtonWithIcon
import com.x8bit.bitwarden.ui.platform.components.BitwardenIconButtonWithResource
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenPasswordFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.BitwardenSwitchWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.model.IconResource
import com.x8bit.bitwarden.ui.vault.feature.additem.handlers.VaultAddItemCommonHandlers
import com.x8bit.bitwarden.ui.vault.feature.additem.handlers.VaultAddLoginItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The UI for adding and editing a login cipher.
 */
@Suppress("LongMethod", "LongParameterList")
fun LazyListScope.addEditLoginItems(
    commonState: VaultAddItemState.ViewState.Content.Common,
    loginState: VaultAddItemState.ViewState.Content.ItemType.Login,
    isAddItemMode: Boolean,
    commonActionHandler: VaultAddItemCommonHandlers,
    loginItemTypeHandlers: VaultAddLoginItemTypeHandlers,
    onTotpSetupClick: () -> Unit,
) {
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.name),
            value = commonState.name,
            onValueChange = commonActionHandler.onNameTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextFieldWithActions(
            label = stringResource(id = R.string.username),
            value = loginState.username,
            onValueChange = loginItemTypeHandlers.onUsernameTextChange,
            actions = {
                BitwardenIconButtonWithResource(
                    iconRes = IconResource(
                        iconPainter = painterResource(id = R.drawable.ic_generator),
                        contentDescription = stringResource(id = R.string.generate_username),
                    ),
                    onClick = loginItemTypeHandlers.onOpenUsernameGeneratorClick,
                )
            },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenPasswordFieldWithActions(
            label = stringResource(id = R.string.password),
            value = loginState.password,
            onValueChange = loginItemTypeHandlers.onPasswordTextChange,
            modifier = Modifier
                .padding(horizontal = 16.dp),
        ) {
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_check_mark),
                    contentDescription = stringResource(id = R.string.check_password),
                ),
                onClick = loginItemTypeHandlers.onPasswordCheckerClick,
            )
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_generator),
                    contentDescription = stringResource(id = R.string.generate_password),
                ),
                onClick = loginItemTypeHandlers.onOpenPasswordGeneratorClick,
            )
        }
    }

    item {
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenListHeaderText(
            label = stringResource(id = R.string.authenticator_key),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenFilledTonalButtonWithIcon(
            label = stringResource(id = R.string.setup_totp),
            icon = painterResource(id = R.drawable.ic_light_bulb),
            onClick = onTotpSetupClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenListHeaderText(
            label = stringResource(id = R.string.ur_is),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextFieldWithActions(
            label = stringResource(id = R.string.uri),
            value = loginState.uri,
            onValueChange = loginItemTypeHandlers.onUriTextChange,
            actions = {
                BitwardenIconButtonWithResource(
                    iconRes = IconResource(
                        iconPainter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = stringResource(id = R.string.options),
                    ),
                    onClick = loginItemTypeHandlers.onUriSettingsClick,
                )
            },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenFilledTonalButton(
            label = stringResource(id = R.string.new_uri),
            onClick = loginItemTypeHandlers.onAddNewUriClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenListHeaderText(
            label = stringResource(id = R.string.miscellaneous),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenMultiSelectButton(
            label = stringResource(id = R.string.folder),
            options = commonState.availableFolders.map { it.invoke() }.toImmutableList(),
            selectedOption = commonState.folderName.invoke(),
            onOptionSelected = commonActionHandler.onFolderTextChange,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenSwitch(
            label = stringResource(
                id = R.string.favorite,
            ),
            isChecked = commonState.favorite,
            onCheckedChange = commonActionHandler.onToggleFavorite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenSwitchWithActions(
            label = stringResource(id = R.string.password_prompt),
            isChecked = commonState.masterPasswordReprompt,
            onCheckedChange = commonActionHandler.onToggleMasterPasswordReprompt,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            actions = {
                IconButton(onClick = commonActionHandler.onTooltipClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tooltip),
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = stringResource(
                            id = R.string.master_password_re_prompt_help,
                        ),
                    )
                }
            },
        )
    }

    item {
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenListHeaderText(
            label = stringResource(id = R.string.notes),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            singleLine = false,
            label = stringResource(id = R.string.notes),
            value = commonState.notes,
            onValueChange = commonActionHandler.onNotesTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenListHeaderText(
            label = stringResource(id = R.string.custom_fields),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    items(commonState.customFieldData) { customItem ->
        AddEditCustomField(
            customItem,
            onCustomFieldValueChange = commonActionHandler.onCustomFieldValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            supportedLinkedTypes = persistentListOf(
                VaultLinkedFieldType.PASSWORD,
                VaultLinkedFieldType.USERNAME,
            ),
        )
    }

    item {
        Spacer(modifier = Modifier.height(16.dp))
        AddEditCustomFieldsButton(
            onFinishNamingClick = commonActionHandler.onAddNewCustomFieldClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    if (isAddItemMode) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            BitwardenListHeaderText(
                label = stringResource(id = R.string.ownership),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenMultiSelectButton(
                label = stringResource(id = R.string.who_owns_this_item),
                options = commonState.availableOwners.toImmutableList(),
                selectedOption = commonState.ownership,
                onOptionSelected = commonActionHandler.onOwnershipTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
    }

    item {
        Spacer(modifier = Modifier.height(24.dp))
    }
}
