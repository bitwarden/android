package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditDriversLicenseTypeHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import kotlinx.collections.immutable.toImmutableList

/**
 * The UI for adding and editing a driver's license cipher.
 */
@Suppress("LongMethod")
fun LazyListScope.vaultAddEditDriversLicenseItems(
    driversLicenseState: VaultAddEditState.ViewState.Content.ItemType.DriversLicense,
    driversLicenseHandlers: VaultAddEditDriversLicenseTypeHandlers,
) {
    item {
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = BitwardenString.drivers_license_details),
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
            value = driversLicenseState.firstName,
            onValueChange = driversLicenseHandlers.onFirstNameTextChange,
            textFieldTestTag = "DriversLicenseFirstNameEntry",
            cardStyle = CardStyle.Top(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.middle_name),
            value = driversLicenseState.middleName,
            onValueChange = driversLicenseHandlers.onMiddleNameTextChange,
            textFieldTestTag = "DriversLicenseMiddleNameEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.last_name),
            value = driversLicenseState.lastName,
            onValueChange = driversLicenseHandlers.onLastNameTextChange,
            textFieldTestTag = "DriversLicenseLastNameEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.license_number),
            value = driversLicenseState.licenseNumber,
            onValueChange = driversLicenseHandlers.onLicenseNumberTextChange,
            textFieldTestTag = "DriversLicenseLicenseNumberEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.issuing_country),
            value = driversLicenseState.issuingCountry,
            onValueChange = driversLicenseHandlers.onIssuingCountryTextChange,
            textFieldTestTag = "DriversLicenseIssuingCountryEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.issuing_state),
            value = driversLicenseState.issuingState,
            onValueChange = driversLicenseHandlers.onIssuingStateTextChange,
            textFieldTestTag = "DriversLicenseIssuingStateEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        val resources = LocalResources.current
        val selectedMonth = VaultCardExpirationMonth.entries
            .find { it.number == driversLicenseState.expirationMonth }
            ?: VaultCardExpirationMonth.SELECT
        BitwardenMultiSelectButton(
            label = stringResource(id = BitwardenString.expiration_month),
            options = VaultCardExpirationMonth
                .entries
                .map { it.value() }
                .toImmutableList(),
            selectedOption = selectedMonth.value(),
            onOptionSelected = { selectedString ->
                driversLicenseHandlers.onExpirationMonthSelect(
                    VaultCardExpirationMonth
                        .entries
                        .first {
                            it.value.toString(resources) == selectedString
                        },
                )
            },
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .testTag("DriversLicenseExpirationMonthPicker")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.expiration_year),
            value = driversLicenseState.expirationYear,
            onValueChange = driversLicenseHandlers.onExpirationYearTextChange,
            keyboardType = KeyboardType.Number,
            textFieldTestTag = "DriversLicenseExpirationYearEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.license_class),
            value = driversLicenseState.licenseClass,
            onValueChange = driversLicenseHandlers.onLicenseClassTextChange,
            textFieldTestTag = "DriversLicenseLicenseClassEntry",
            cardStyle = CardStyle.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}

@Preview
@Composable
private fun VaultAddEditDriversLicenseItems_preview() {
    BitwardenTheme {
        LazyColumn {
            vaultAddEditDriversLicenseItems(
                driversLicenseState =
                    VaultAddEditState.ViewState.Content.ItemType.DriversLicense(
                        firstName = "John",
                        lastName = "Doe",
                    ),
                driversLicenseHandlers = VaultAddEditDriversLicenseTypeHandlers(
                    onFirstNameTextChange = {},
                    onMiddleNameTextChange = {},
                    onLastNameTextChange = {},
                    onLicenseNumberTextChange = {},
                    onIssuingCountryTextChange = {},
                    onIssuingStateTextChange = {},
                    onExpirationMonthSelect = {},
                    onExpirationYearTextChange = {},
                    onLicenseClassTextChange = {},
                ),
            )
        }
    }
}
