package com.x8bit.bitwarden.ui.vault.feature.additem

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
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
import kotlinx.collections.immutable.toImmutableList

/**
 * The UI for adding and editing a login cipher.
 */
@Suppress("LongMethod")
fun LazyListScope.addEditLoginItems(
    state: VaultAddItemState.ViewState.Content.Login,
    loginItemTypeHandlers: VaultAddLoginItemTypeHandlers,
) {
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.name),
            value = state.name,
            onValueChange = loginItemTypeHandlers.onNameTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextFieldWithActions(
            label = stringResource(id = R.string.username),
            value = state.username,
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
            value = state.password,
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
            onClick = loginItemTypeHandlers.onSetupTotpClick,
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
            value = state.uri,
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
            options = state.availableFolders.map { it.invoke() }.toImmutableList(),
            selectedOption = state.folderName.invoke(),
            onOptionSelected = loginItemTypeHandlers.onFolderTextChange,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenSwitch(
            label = stringResource(
                id = R.string.favorite,
            ),
            isChecked = state.favorite,
            onCheckedChange = loginItemTypeHandlers.onToggleFavorite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenSwitchWithActions(
            label = stringResource(id = R.string.password_prompt),
            isChecked = state.masterPasswordReprompt,
            onCheckedChange = loginItemTypeHandlers.onToggleMasterPasswordReprompt,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            actions = {
                IconButton(onClick = loginItemTypeHandlers.onTooltipClick) {
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
            value = state.notes,
            onValueChange = loginItemTypeHandlers.onNotesTextChange,
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

    item {
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenFilledTonalButton(
            label = stringResource(id = R.string.new_custom_field),
            onClick = loginItemTypeHandlers.onAddNewCustomFieldClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

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
            options = state.availableOwners.toImmutableList(),
            selectedOption = state.ownership,
            onOptionSelected = loginItemTypeHandlers.onOwnershipTextChange,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(24.dp))
    }
}
