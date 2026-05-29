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
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditPassportTypeHandlers

/**
 * The UI for adding and editing a passport cipher.
 */
@Suppress("LongMethod")
fun LazyListScope.vaultAddEditPassportItems(
    passportState: VaultAddEditState.ViewState.Content.ItemType.Passport,
    passportHandlers: VaultAddEditPassportTypeHandlers,
) {
    item {
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = BitwardenString.passport_details),
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
            value = passportState.givenName,
            onValueChange = passportHandlers.onGivenNameTextChange,
            textFieldTestTag = "PassportGivenNameEntry",
            cardStyle = CardStyle.Top(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.last_name),
            value = passportState.surname,
            onValueChange = passportHandlers.onSurnameTextChange,
            textFieldTestTag = "PassportSurnameEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenDatePickerButton(
            label = stringResource(id = BitwardenString.date_of_birth),
            currentDate = passportState.dateOfBirth,
            onDateSelect = passportHandlers.onDateOfBirthChange,
            textFieldTestTag = "PassportDateOfBirthEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.sex),
            value = passportState.sex,
            onValueChange = passportHandlers.onSexTextChange,
            textFieldTestTag = "PassportSexEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.birth_place),
            value = passportState.birthPlace,
            onValueChange = passportHandlers.onBirthPlaceTextChange,
            textFieldTestTag = "PassportBirthPlaceEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.nationality),
            value = passportState.nationality,
            onValueChange = passportHandlers.onNationalityTextChange,
            textFieldTestTag = "PassportNationalityEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenPasswordField(
            label = stringResource(id = BitwardenString.passport_number),
            value = passportState.passportNumber,
            onValueChange = passportHandlers.onPassportNumberTextChange,
            passwordFieldTestTag = "PassportPassportNumberEntry",
            showPasswordTestTag = "PassportShowPassportNumberButton",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.passport_type),
            value = passportState.passportType,
            onValueChange = passportHandlers.onPassportTypeTextChange,
            textFieldTestTag = "PassportPassportTypeEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenPasswordField(
            label = stringResource(id = BitwardenString.national_identification_number),
            value = passportState.nationalIdentificationNumber,
            onValueChange = passportHandlers.onNationalIdentificationNumberTextChange,
            passwordFieldTestTag = "PassportNationalIdentificationNumberEntry",
            showPasswordTestTag = "PassportShowNationalIdentificationNumberButton",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.issuing_country),
            value = passportState.issuingCountry,
            onValueChange = passportHandlers.onIssuingCountryTextChange,
            textFieldTestTag = "PassportIssuingCountryEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.issuing_authority),
            value = passportState.issuingAuthority,
            onValueChange = passportHandlers.onIssuingAuthorityTextChange,
            textFieldTestTag = "PassportIssuingAuthorityEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenDatePickerButton(
            label = stringResource(id = BitwardenString.issue_date),
            currentDate = passportState.issueDate,
            onDateSelect = passportHandlers.onIssueDateChange,
            textFieldTestTag = "PassportIssueDateEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenDatePickerButton(
            label = stringResource(id = BitwardenString.expiration_date),
            currentDate = passportState.expirationDate,
            onDateSelect = passportHandlers.onExpirationDateChange,
            textFieldTestTag = "PassportExpirationDateEntry",
            cardStyle = CardStyle.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}
