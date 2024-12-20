package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.components.collectionItemsSelector
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCommonHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditIdentityTypeHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultIdentityTitle
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The UI for adding and editing an identity cipher.
 */
@Suppress("LongMethod")
fun LazyListScope.vaultAddEditIdentityItems(
    commonState: VaultAddEditState.ViewState.Content.Common,
    identityState: VaultAddEditState.ViewState.Content.ItemType.Identity,
    isAddItemMode: Boolean,
    commonTypeHandlers: VaultAddEditCommonHandlers,
    identityItemTypeHandlers: VaultAddEditIdentityTypeHandlers,
) {
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.name),
            value = commonState.name,
            onValueChange = commonTypeHandlers.onNameTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "ItemNameEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        TitleMultiSelectButton(
            selectedTitle = identityState.selectedTitle,
            onTitleSelected = identityItemTypeHandlers.onTitleSelected,
            modifier = Modifier
                .testTag("IdentityTitlePicker")
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.first_name),
            value = identityState.firstName,
            onValueChange = identityItemTypeHandlers.onFirstNameTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityFirstNameEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.middle_name),
            value = identityState.middleName,
            onValueChange = identityItemTypeHandlers.onMiddleNameTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityMiddleNameEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.last_name),
            value = identityState.lastName,
            onValueChange = identityItemTypeHandlers.onLastNameTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityLastNameEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.username),
            value = identityState.username,
            onValueChange = identityItemTypeHandlers.onUsernameTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityUsernameEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.company),
            value = identityState.company,
            onValueChange = identityItemTypeHandlers.onCompanyTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityCompanyEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.ssn),
            value = identityState.ssn,
            onValueChange = identityItemTypeHandlers.onSsnTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentitySsnEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.passport_number),
            value = identityState.passportNumber,
            onValueChange = identityItemTypeHandlers.onPassportNumberTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityPassportNumberEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.license_number),
            value = identityState.licenseNumber,
            onValueChange = identityItemTypeHandlers.onLicenseNumberTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityLicenseNumberEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.email),
            value = identityState.email,
            onValueChange = identityItemTypeHandlers.onEmailTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityEmailEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.phone),
            value = identityState.phone,
            onValueChange = identityItemTypeHandlers.onPhoneTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityPhoneEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.address1),
            value = identityState.address1,
            onValueChange = identityItemTypeHandlers.onAddress1TextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityAddressOneEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.address2),
            value = identityState.address2,
            onValueChange = identityItemTypeHandlers.onAddress2TextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityAddressTwoEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.address3),
            value = identityState.address3,
            onValueChange = identityItemTypeHandlers.onAddress3TextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityAddressThreeEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.city_town),
            value = identityState.city,
            onValueChange = identityItemTypeHandlers.onCityTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityCityEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.state_province),
            value = identityState.state,
            onValueChange = identityItemTypeHandlers.onStateTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityStateEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.zip_postal_code),
            value = identityState.zip,
            onValueChange = identityItemTypeHandlers.onZipTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityPostalCodeEntry",
        )
    }
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.country),
            value = identityState.country,
            onValueChange = identityItemTypeHandlers.onCountryTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "IdentityCountryEntry",
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
            label = stringResource(
                id = R.string.favorite,
            ),
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
            BitwardenSwitch(
                label = stringResource(id = R.string.password_prompt),
                isChecked = commonState.masterPasswordReprompt,
                onCheckedChange = commonTypeHandlers.onToggleMasterPasswordReprompt,
                modifier = Modifier
                    .testTag("MasterPasswordRepromptToggle")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "ItemNotesEntry",
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
            supportedLinkedTypes = persistentListOf(
                VaultLinkedFieldType.TITLE,
                VaultLinkedFieldType.MIDDLE_NAME,
                VaultLinkedFieldType.ADDRESS_1,
                VaultLinkedFieldType.ADDRESS_2,
                VaultLinkedFieldType.ADDRESS_3,
                VaultLinkedFieldType.CITY,
                VaultLinkedFieldType.STATE,
                VaultLinkedFieldType.POSTAL_CODE,
                VaultLinkedFieldType.COUNTRY,
                VaultLinkedFieldType.COMPANY,
                VaultLinkedFieldType.EMAIL,
                VaultLinkedFieldType.PHONE,
                VaultLinkedFieldType.SSN,
                VaultLinkedFieldType.IDENTITY_USERNAME,
                VaultLinkedFieldType.PASSPORT_NUMBER,
                VaultLinkedFieldType.LICENSE_NUMBER,
                VaultLinkedFieldType.FIRST_NAME,
                VaultLinkedFieldType.LAST_NAME,
                VaultLinkedFieldType.FULL_NAME,
            ),
            onHiddenVisibilityChanged = commonTypeHandlers.onHiddenFieldVisibilityChange,
        )
    }

    item {
        Spacer(modifier = Modifier.height(16.dp))
        VaultAddEditCustomFieldsButton(
            onFinishNamingClick = commonTypeHandlers.onAddNewCustomFieldClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    if (isAddItemMode && commonState.hasOrganizations) {
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

@Composable
private fun TitleMultiSelectButton(
    selectedTitle: VaultIdentityTitle,
    onTitleSelected: (VaultIdentityTitle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    BitwardenMultiSelectButton(
        label = stringResource(id = R.string.title),
        options = VaultIdentityTitle
            .entries
            .map { it.value() }
            .toImmutableList(),
        selectedOption = selectedTitle.value(),
        onOptionSelected = { selectedString ->
            onTitleSelected(
                VaultIdentityTitle
                    .entries
                    .first { it.value.toString(resources) == selectedString },
            )
        },
        modifier = modifier,
    )
}
