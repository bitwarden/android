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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitchWithActions
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.vault.components.collectionItemsSelector
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCommonHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The UI for adding and editing a secure notes cipher.
 */
@Suppress("LongMethod")
fun LazyListScope.vaultAddEditSecureNotesItems(
    commonState: VaultAddEditState.ViewState.Content.Common,
    isAddItemMode: Boolean,
    commonTypeHandlers: VaultAddEditCommonHandlers,
) {
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.name),
            value = commonState.name,
            onValueChange = commonTypeHandlers.onNameTextChange,
            modifier = Modifier
                .testTag("ItemNameEntry")
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
                commonTypeHandlers.onFolderSelected(
                    commonState
                        .availableFolders
                        .first { it.name == selectedFolderName },
                )
            },
            modifier = Modifier
                .testTag("FolderPicker")
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenSwitch(
            label = stringResource(id = R.string.favorite),
            isChecked = commonState.favorite,
            onCheckedChange = commonTypeHandlers.onToggleFavorite,
            modifier = Modifier
                .testTag("ItemFavoriteToggle")
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    if (commonState.isUnlockWithPasswordEnabled) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenSwitchWithActions(
                label = stringResource(id = R.string.password_prompt),
                isChecked = commonState.masterPasswordReprompt,
                onCheckedChange = commonTypeHandlers.onToggleMasterPasswordReprompt,
                modifier = Modifier
                    .testTag("MasterPasswordRepromptToggle")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                actions = {
                    IconButton(onClick = commonTypeHandlers.onTooltipClick) {
                        Icon(
                            painter = rememberVectorPainter(id = R.drawable.ic_tooltip),
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = stringResource(
                                id = R.string.master_password_re_prompt_help,
                            ),
                        )
                    }
                },
            )
        }
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
            onValueChange = commonTypeHandlers.onNotesTextChange,
            modifier = Modifier
                .testTag("ItemNotesEntry")
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
        Spacer(modifier = Modifier.height(8.dp))
        VaultAddEditCustomField(
            customField = customItem,
            onCustomFieldValueChange = commonTypeHandlers.onCustomFieldValueChange,
            onCustomFieldAction = commonTypeHandlers.onCustomFieldActionSelect,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onHiddenVisibilityChanged = commonTypeHandlers.onHiddenFieldVisibilityChange,
        )
    }

    item {
        Spacer(modifier = Modifier.height(16.dp))
        VaultAddEditCustomFieldsButton(
            onFinishNamingClick = commonTypeHandlers.onAddNewCustomFieldClick,
            options = persistentListOf(
                CustomFieldType.TEXT,
                CustomFieldType.HIDDEN,
                CustomFieldType.BOOLEAN,
            ),
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
                    commonTypeHandlers.onOwnerSelected(
                        commonState
                            .availableOwners
                            .first { it.name == selectedOwnerName },
                    )
                },
                modifier = Modifier
                    .testTag("ItemOwnershipPicker")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        if (commonState.selectedOwnerId != null) {
            collectionItemsSelector(
                collectionList = commonState.selectedOwner?.collections,
                onCollectionSelect = commonTypeHandlers.onCollectionSelect,
            )
        }
    }

    item {
        Spacer(modifier = Modifier.height(24.dp))
    }
}
