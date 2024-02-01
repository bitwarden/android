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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.BitwardenSwitchWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.vault.components.collectionItemsSelector
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCardTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCommonHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The UI for adding and editing a card cipher.
 */
@Suppress("LongMethod")
fun LazyListScope.vaultAddEditCardItems(
    commonState: VaultAddEditState.ViewState.Content.Common,
    cardState: VaultAddEditState.ViewState.Content.ItemType.Card,
    commonHandlers: VaultAddEditCommonHandlers,
    cardHandlers: VaultAddEditCardTypeHandlers,
    isAddItemMode: Boolean,
) {
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.name),
            value = commonState.name,
            onValueChange = commonHandlers.onNameTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.cardholder_name),
            value = cardState.cardHolderName,
            onValueChange = cardHandlers.onCardHolderNameTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenPasswordField(
            label = stringResource(id = R.string.number),
            value = cardState.number,
            onValueChange = cardHandlers.onNumberTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
    item {
        val resources = LocalContext.current.resources
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenMultiSelectButton(
            label = stringResource(id = R.string.brand),
            options = VaultCardBrand
                .entries
                .map { it.value() }
                .toImmutableList(),
            selectedOption = cardState.brand.value(),
            onOptionSelected = { selectedString ->
                cardHandlers.onBrandSelected(
                    VaultCardBrand
                        .entries
                        .first { it.value.toString(resources) == selectedString },
                )
            },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    item {
        val resources = LocalContext.current.resources
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenMultiSelectButton(
            label = stringResource(id = R.string.expiration_month),
            options = VaultCardExpirationMonth
                .entries
                .map { it.value() }
                .toImmutableList(),
            selectedOption = cardState.expirationMonth.value(),
            onOptionSelected = { selectedString ->
                cardHandlers.onExpirationMonthSelected(
                    VaultCardExpirationMonth
                        .entries
                        .first { it.value.toString(resources) == selectedString },
                )
            },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.expiration_year),
            value = cardState.expirationYear,
            onValueChange = cardHandlers.onExpirationYearTextChange,
            keyboardType = KeyboardType.Number,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenPasswordField(
            label = stringResource(id = R.string.security_code),
            value = cardState.securityCode,
            onValueChange = cardHandlers.onSecurityCodeTextChange,
            keyboardType = KeyboardType.NumberPassword,
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
                commonHandlers.onFolderSelected(
                    commonState
                        .availableFolders
                        .first { it.name == selectedFolderName },
                )
            },
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
            onCheckedChange = commonHandlers.onToggleFavorite,
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
            onCheckedChange = commonHandlers.onToggleMasterPasswordReprompt,
            modifier = Modifier
                .semantics { testTag = "MasterPasswordRepromptToggle" }
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            actions = {
                IconButton(onClick = commonHandlers.onTooltipClick) {
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
            onValueChange = commonHandlers.onNotesTextChange,
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
            onCustomFieldValueChange = commonHandlers.onCustomFieldValueChange,
            onCustomFieldAction = commonHandlers.onCustomFieldActionSelect,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            supportedLinkedTypes = persistentListOf(
                VaultLinkedFieldType.CARDHOLDER_NAME,
                VaultLinkedFieldType.EXPIRATION_MONTH,
                VaultLinkedFieldType.EXPIRATION_YEAR,
                VaultLinkedFieldType.SECURITY_CODE,
                VaultLinkedFieldType.BRAND,
                VaultLinkedFieldType.NUMBER,
            ),
        )
    }

    item {
        Spacer(modifier = Modifier.height(16.dp))
        VaultAddEditCustomFieldsButton(
            onFinishNamingClick = commonHandlers.onAddNewCustomFieldClick,
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
                    commonHandlers.onOwnerSelected(
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
                onCollectionSelect = commonHandlers.onCollectionSelect,
            )
        }
    }
    item {
        Spacer(modifier = Modifier.height(24.dp))
    }
}
