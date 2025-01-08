package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
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
            textFieldTestTag = "ItemNameEntry",
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        Spacer(modifier = Modifier.height(height = 16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = R.string.miscellaneous),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
    }

    item {
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
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .testTag("FolderPicker")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        Spacer(modifier = Modifier.height(height = 8.dp))
        BitwardenSwitch(
            label = stringResource(id = R.string.favorite),
            isChecked = commonState.favorite,
            onCheckedChange = commonTypeHandlers.onToggleFavorite,
            cardStyle = if (commonState.isUnlockWithPasswordEnabled) {
                CardStyle.Top()
            } else {
                CardStyle.Full
            },
            modifier = Modifier
                .testTag("ItemFavoriteToggle")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    if (commonState.isUnlockWithPasswordEnabled) {
        item {
            BitwardenSwitch(
                label = stringResource(id = R.string.password_prompt),
                isChecked = commonState.masterPasswordReprompt,
                onCheckedChange = commonTypeHandlers.onToggleMasterPasswordReprompt,
                actions = {
                    BitwardenStandardIconButton(
                        vectorIconRes = R.drawable.ic_question_circle,
                        contentDescription = stringResource(
                            id = R.string.master_password_re_prompt_help,
                        ),
                        onClick = commonTypeHandlers.onTooltipClick,
                        contentColor = BitwardenTheme.colorScheme.icon.secondary,
                    )
                },
                cardStyle = CardStyle.Bottom,
                modifier = Modifier
                    .testTag("MasterPasswordRepromptToggle")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }
    }

    item {
        Spacer(modifier = Modifier.height(height = 16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = R.string.notes),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
    }

    item {
        BitwardenTextField(
            singleLine = false,
            label = stringResource(id = R.string.notes),
            value = commonState.notes,
            onValueChange = commonTypeHandlers.onNotesTextChange,
            textFieldTestTag = "ItemNotesEntry",
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        Spacer(modifier = Modifier.height(height = 16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = R.string.custom_fields),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    items(commonState.customFieldData) { customItem ->
        Spacer(modifier = Modifier.height(height = 8.dp))
        VaultAddEditCustomField(
            customField = customItem,
            onCustomFieldValueChange = commonTypeHandlers.onCustomFieldValueChange,
            onCustomFieldAction = commonTypeHandlers.onCustomFieldActionSelect,
            onHiddenVisibilityChanged = commonTypeHandlers.onHiddenFieldVisibilityChange,
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
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
                .standardHorizontalMargin(),
        )
    }

    if (isAddItemMode && commonState.hasOrganizations) {
        item {
            Spacer(modifier = Modifier.height(height = 16.dp))
            BitwardenListHeaderText(
                label = stringResource(id = R.string.ownership),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
        }

        item {
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
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .testTag("ItemOwnershipPicker")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }

        if (commonState.selectedOwnerId != null) {
            collectionItemsSelector(
                collectionList = commonState.selectedOwner?.collections,
                onCollectionSelect = commonTypeHandlers.onCollectionSelect,
            )
        }
    }
}
