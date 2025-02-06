package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditIdentityTypeHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultIdentityTitle
import kotlinx.collections.immutable.toImmutableList

/**
 * The UI for adding and editing an identity cipher.
 */
@Suppress("LongMethod")
fun LazyListScope.vaultAddEditIdentityItems(
    identityState: VaultAddEditState.ViewState.Content.ItemType.Identity,
    identityItemTypeHandlers: VaultAddEditIdentityTypeHandlers,
) {
    item {
        Spacer(modifier = Modifier.height(8.dp))
        TitleMultiSelectButton(
            selectedTitle = identityState.selectedTitle,
            onTitleSelected = identityItemTypeHandlers.onTitleSelected,
            modifier = Modifier
                .testTag("IdentityTitlePicker")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.first_name),
            value = identityState.firstName,
            onValueChange = identityItemTypeHandlers.onFirstNameTextChange,
            textFieldTestTag = "IdentityFirstNameEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.middle_name),
            value = identityState.middleName,
            onValueChange = identityItemTypeHandlers.onMiddleNameTextChange,
            textFieldTestTag = "IdentityMiddleNameEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.last_name),
            value = identityState.lastName,
            onValueChange = identityItemTypeHandlers.onLastNameTextChange,
            textFieldTestTag = "IdentityLastNameEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.username),
            value = identityState.username,
            onValueChange = identityItemTypeHandlers.onUsernameTextChange,
            textFieldTestTag = "IdentityUsernameEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.company),
            value = identityState.company,
            onValueChange = identityItemTypeHandlers.onCompanyTextChange,
            textFieldTestTag = "IdentityCompanyEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.ssn),
            value = identityState.ssn,
            onValueChange = identityItemTypeHandlers.onSsnTextChange,
            textFieldTestTag = "IdentitySsnEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.passport_number),
            value = identityState.passportNumber,
            onValueChange = identityItemTypeHandlers.onPassportNumberTextChange,
            textFieldTestTag = "IdentityPassportNumberEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.license_number),
            value = identityState.licenseNumber,
            onValueChange = identityItemTypeHandlers.onLicenseNumberTextChange,
            textFieldTestTag = "IdentityLicenseNumberEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.email),
            value = identityState.email,
            onValueChange = identityItemTypeHandlers.onEmailTextChange,
            textFieldTestTag = "IdentityEmailEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.phone),
            value = identityState.phone,
            onValueChange = identityItemTypeHandlers.onPhoneTextChange,
            textFieldTestTag = "IdentityPhoneEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.address1),
            value = identityState.address1,
            onValueChange = identityItemTypeHandlers.onAddress1TextChange,
            textFieldTestTag = "IdentityAddressOneEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.address2),
            value = identityState.address2,
            onValueChange = identityItemTypeHandlers.onAddress2TextChange,
            textFieldTestTag = "IdentityAddressTwoEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.address3),
            value = identityState.address3,
            onValueChange = identityItemTypeHandlers.onAddress3TextChange,
            textFieldTestTag = "IdentityAddressThreeEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.city_town),
            value = identityState.city,
            onValueChange = identityItemTypeHandlers.onCityTextChange,
            textFieldTestTag = "IdentityCityEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.state_province),
            value = identityState.state,
            onValueChange = identityItemTypeHandlers.onStateTextChange,
            textFieldTestTag = "IdentityStateEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.zip_postal_code),
            value = identityState.zip,
            onValueChange = identityItemTypeHandlers.onZipTextChange,
            textFieldTestTag = "IdentityPostalCodeEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.country),
            value = identityState.country,
            onValueChange = identityItemTypeHandlers.onCountryTextChange,
            textFieldTestTag = "IdentityCountryEntry",
            cardStyle = CardStyle.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
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
        cardStyle = CardStyle.Top(),
        modifier = modifier,
    )
}
