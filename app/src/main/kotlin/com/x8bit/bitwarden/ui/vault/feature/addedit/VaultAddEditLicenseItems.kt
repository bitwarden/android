package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.dropdown.BitwardenDatePickerButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditLicenseTypeHandlers

/**
 * The UI for adding and editing a license cipher.
 */
@Suppress("LongMethod")
fun LazyListScope.vaultAddEditLicenseItems(
    licenseState: VaultAddEditState.ViewState.Content.ItemType.License,
    licenseHandlers: VaultAddEditLicenseTypeHandlers,
) {
    item {
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = BitwardenString.license_details),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = BitwardenString.first_name),
            value = licenseState.firstName,
            onValueChange = licenseHandlers.onFirstNameTextChange,
            textFieldTestTag = "LicenseFirstNameEntry",
            cardStyle = CardStyle.Top(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.middle_name),
            value = licenseState.middleName,
            onValueChange = licenseHandlers.onMiddleNameTextChange,
            textFieldTestTag = "LicenseMiddleNameEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.last_name),
            value = licenseState.lastName,
            onValueChange = licenseHandlers.onLastNameTextChange,
            textFieldTestTag = "LicenseLastNameEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenPasswordField(
            label = stringResource(id = BitwardenString.license_number),
            value = licenseState.licenseNumber,
            onValueChange = licenseHandlers.onLicenseNumberTextChange,
            passwordFieldTestTag = "LicenseLicenseNumberEntry",
            showPasswordTestTag = "LicenseShowLicenseNumberButton",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenDatePickerButton(
            label = stringResource(id = BitwardenString.date_of_birth),
            currentDate = licenseState.dateOfBirth,
            onDateSelect = licenseHandlers.onDateOfBirthChange,
            textFieldTestTag = "LicenseDateOfBirthEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.issuing_country),
            value = licenseState.issuingCountry,
            onValueChange = licenseHandlers.onIssuingCountryTextChange,
            textFieldTestTag = "LicenseIssuingCountryEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.issuing_state),
            value = licenseState.issuingState,
            onValueChange = licenseHandlers.onIssuingStateTextChange,
            textFieldTestTag = "LicenseIssuingStateEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.issuing_authority),
            value = licenseState.issuingAuthority,
            onValueChange = licenseHandlers.onIssuingAuthorityTextChange,
            textFieldTestTag = "LicenseIssuingAuthorityEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenDatePickerButton(
            label = stringResource(id = BitwardenString.issue_date),
            currentDate = licenseState.issueDate,
            onDateSelect = licenseHandlers.onIssueDateChange,
            textFieldTestTag = "LicenseIssueDateEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenDatePickerButton(
            label = stringResource(id = BitwardenString.expiration_date),
            currentDate = licenseState.expirationDate,
            onDateSelect = licenseHandlers.onExpirationDateChange,
            textFieldTestTag = "LicenseExpirationDateEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.license_class),
            value = licenseState.licenseClass,
            onValueChange = licenseHandlers.onLicenseClassTextChange,
            textFieldTestTag = "LicenseLicenseClassEntry",
            cardStyle = CardStyle.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}
