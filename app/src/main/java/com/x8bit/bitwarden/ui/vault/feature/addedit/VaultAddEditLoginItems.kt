package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledTonalButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledTonalButtonWithIcon
import com.x8bit.bitwarden.ui.platform.components.BitwardenHiddenPasswordField
import com.x8bit.bitwarden.ui.platform.components.BitwardenIconButtonWithResource
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenPasswordFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.BitwardenSwitchWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.model.IconResource
import com.x8bit.bitwarden.ui.vault.components.collectionItemsSelector
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCommonHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditLoginTypeHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The UI for adding and editing a login cipher.
 */
@Suppress("LongMethod", "LongParameterList")
fun LazyListScope.vaultAddEditLoginItems(
    commonState: VaultAddEditState.ViewState.Content.Common,
    loginState: VaultAddEditState.ViewState.Content.ItemType.Login,
    isAddItemMode: Boolean,
    commonActionHandler: VaultAddEditCommonHandlers,
    loginItemTypeHandlers: VaultAddEditLoginTypeHandlers,
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
        UsernameRow(
            loginState = loginState,
            loginItemTypeHandlers = loginItemTypeHandlers,
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        PasswordRow(
            loginState = loginState,
            loginItemTypeHandlers = loginItemTypeHandlers,
        )
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

    if (loginState.totp != null) {
        item {
            BitwardenTextFieldWithActions(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                label = stringResource(id = R.string.totp),
                value = loginState.totp,
                trailingIconContent = {
                    IconButton(
                        onClick = loginItemTypeHandlers.onClearTotpKeyClick,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = stringResource(id = R.string.delete),
                        )
                    }
                },
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                actions = {
                    BitwardenIconButtonWithResource(
                        iconRes = IconResource(
                            iconPainter = painterResource(id = R.drawable.ic_copy),
                            contentDescription = stringResource(id = R.string.copy_totp),
                        ),
                        onClick = {
                            loginItemTypeHandlers.onCopyTotpKeyClick(loginState.totp)
                        },
                    )
                    BitwardenIconButtonWithResource(
                        iconRes = IconResource(
                            iconPainter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = stringResource(id = R.string.camera),
                        ),
                        onClick = onTotpSetupClick,
                    )
                },
            )
        }
    } else {
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

    items(loginState.uriList) { uriItem ->
        Spacer(modifier = Modifier.height(8.dp))
        VaultAddEditUriItem(
            uriItem = uriItem,
            onUriValueChange = loginItemTypeHandlers.onUriValueChange,
            onUriItemRemoved = loginItemTypeHandlers.onRemoveUriClick,
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
            options = commonState
                .availableFolders
                .map { it.name }
                .toImmutableList(),
            selectedOption = commonState.selectedFolder?.name,
            onOptionSelected = { selectedFolderName ->
                commonActionHandler.onFolderSelected(
                    commonState
                        .availableFolders
                        .first { it.name == selectedFolderName },
                )
            },
            modifier = Modifier
                .semantics { testTag = "FolderPicker" }
                .padding(horizontal = 16.dp),
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
                .semantics { testTag = "ItemFavoriteToggle" }
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
                .semantics { testTag = "MasterPasswordRepromptToggle" }
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
                .semantics { testTag = "ItemNotesEntry" }
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
        VaultAddEditCustomField(
            customItem,
            onCustomFieldValueChange = commonActionHandler.onCustomFieldValueChange,
            onCustomFieldAction = commonActionHandler.onCustomFieldActionSelect,
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
        VaultAddEditCustomFieldsButton(
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
                options = commonState
                    .availableOwners
                    .map { it.name }
                    .toImmutableList(),
                selectedOption = commonState.selectedOwner?.name,
                onOptionSelected = { selectedOwnerName ->
                    commonActionHandler.onOwnerSelected(
                        commonState
                            .availableOwners
                            .first { it.name == selectedOwnerName },
                    )
                },
                modifier = Modifier
                    .semantics { testTag = "ItemOwnershipPicker" }
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        if (commonState.selectedOwnerId != null) {
            collectionItemsSelector(
                collectionList = commonState.selectedOwner?.collections,
                onCollectionSelect = commonActionHandler.onCollectionSelect,
            )
        }
    }

    item {
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun UsernameRow(
    loginState: VaultAddEditState.ViewState.Content.ItemType.Login,
    loginItemTypeHandlers: VaultAddEditLoginTypeHandlers,
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(false) }

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
                onClick = {
                    if (loginState.username.isEmpty()) {
                        loginItemTypeHandlers.onOpenUsernameGeneratorClick()
                    } else {
                        shouldShowDialog = true
                    }
                },
                modifier = Modifier.semantics { testTag = "GenerateUsernameButton" },
            )
        },
        modifier = Modifier.padding(horizontal = 16.dp),
    )

    if (shouldShowDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.username),
            message = stringResource(
                id =
                R.string.are_you_sure_you_want_to_overwrite_the_current_username,
            ),
            confirmButtonText = stringResource(id = R.string.yes),
            dismissButtonText = stringResource(id = R.string.no),
            onConfirmClick = {
                shouldShowDialog = false
                loginItemTypeHandlers.onOpenUsernameGeneratorClick()
            },
            onDismissClick = {
                shouldShowDialog = false
            },
            onDismissRequest = {
                shouldShowDialog = false
            },
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun PasswordRow(
    loginState: VaultAddEditState.ViewState.Content.ItemType.Login,
    loginItemTypeHandlers: VaultAddEditLoginTypeHandlers,
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(false) }

    if (loginState.canViewPassword) {
        BitwardenPasswordFieldWithActions(
            label = stringResource(id = R.string.password),
            value = loginState.password,
            onValueChange = loginItemTypeHandlers.onPasswordTextChange,
            modifier = Modifier
                .fillMaxWidth()
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
                onClick = {
                    if (loginState.password.isEmpty()) {
                        loginItemTypeHandlers.onOpenPasswordGeneratorClick()
                    } else {
                        shouldShowDialog = true
                    }
                },
            )

            if (shouldShowDialog) {
                BitwardenTwoButtonDialog(
                    title = stringResource(id = R.string.password),
                    message = stringResource(
                        id =
                        R.string.password_override_alert,
                    ),
                    confirmButtonText = stringResource(id = R.string.yes),
                    dismissButtonText = stringResource(id = R.string.no),
                    onConfirmClick = {
                        shouldShowDialog = false
                        loginItemTypeHandlers.onOpenPasswordGeneratorClick()
                    },
                    onDismissClick = {
                        shouldShowDialog = false
                    },
                    onDismissRequest = {
                        shouldShowDialog = false
                    },
                )
            }
        }
    } else {
        BitwardenHiddenPasswordField(
            label = stringResource(id = R.string.password),
            value = loginState.password,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}
