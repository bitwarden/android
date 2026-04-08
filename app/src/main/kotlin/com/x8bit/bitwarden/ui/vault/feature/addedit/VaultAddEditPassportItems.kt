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
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditPassportTypeHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import kotlinx.collections.immutable.toImmutableList

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
            label = stringResource(id = BitwardenString.surname),
            value = passportState.surname,
            onValueChange = passportHandlers.onSurnameTextChange,
            textFieldTestTag = "PassportSurnameEntry",
            cardStyle = CardStyle.Top(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.given_name),
            value = passportState.givenName,
            onValueChange = passportHandlers.onGivenNameTextChange,
            textFieldTestTag = "PassportGivenNameEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        val resources = LocalResources.current
        val selectedDobMonth = VaultCardExpirationMonth.entries
            .find { it.number == passportState.dobMonth }
            ?: VaultCardExpirationMonth.SELECT
        BitwardenMultiSelectButton(
            label = stringResource(id = BitwardenString.date_of_birth) + " - " +
                stringResource(id = BitwardenString.month),
            options = VaultCardExpirationMonth
                .entries
                .map { it.value() }
                .toImmutableList(),
            selectedOption = selectedDobMonth.value(),
            onOptionSelected = { selectedString ->
                passportHandlers.onDobMonthSelect(
                    VaultCardExpirationMonth
                        .entries
                        .first {
                            it.value.toString(resources) == selectedString
                        },
                )
            },
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .testTag("PassportDobMonthPicker")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.date_of_birth) + " - " +
                stringResource(id = BitwardenString.year),
            value = passportState.dobYear,
            onValueChange = passportHandlers.onDobYearTextChange,
            keyboardType = KeyboardType.Number,
            textFieldTestTag = "PassportDobYearEntry",
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
        BitwardenTextField(
            label = stringResource(id = BitwardenString.passport_number),
            value = passportState.passportNumber,
            onValueChange = passportHandlers.onPassportNumberTextChange,
            textFieldTestTag = "PassportNumberEntry",
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
            textFieldTestTag = "PassportTypeEntry",
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
        val resources = LocalResources.current
        val selectedIssueMonth = VaultCardExpirationMonth.entries
            .find { it.number == passportState.issueMonth }
            ?: VaultCardExpirationMonth.SELECT
        BitwardenMultiSelectButton(
            label = stringResource(id = BitwardenString.issue_date) + " - " +
                stringResource(id = BitwardenString.month),
            options = VaultCardExpirationMonth
                .entries
                .map { it.value() }
                .toImmutableList(),
            selectedOption = selectedIssueMonth.value(),
            onOptionSelected = { selectedString ->
                passportHandlers.onIssueMonthSelect(
                    VaultCardExpirationMonth
                        .entries
                        .first {
                            it.value.toString(resources) == selectedString
                        },
                )
            },
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .testTag("PassportIssueMonthPicker")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.issue_date) + " - " +
                stringResource(id = BitwardenString.year),
            value = passportState.issueYear,
            onValueChange = passportHandlers.onIssueYearTextChange,
            keyboardType = KeyboardType.Number,
            textFieldTestTag = "PassportIssueYearEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        val resources = LocalResources.current
        val selectedExpMonth = VaultCardExpirationMonth.entries
            .find { it.number == passportState.expirationMonth }
            ?: VaultCardExpirationMonth.SELECT
        BitwardenMultiSelectButton(
            label = stringResource(id = BitwardenString.expiration_date) + " - " +
                stringResource(id = BitwardenString.month),
            options = VaultCardExpirationMonth
                .entries
                .map { it.value() }
                .toImmutableList(),
            selectedOption = selectedExpMonth.value(),
            onOptionSelected = { selectedString ->
                passportHandlers.onExpirationMonthSelect(
                    VaultCardExpirationMonth
                        .entries
                        .first {
                            it.value.toString(resources) == selectedString
                        },
                )
            },
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .testTag("PassportExpirationMonthPicker")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.expiration_date) + " - " +
                stringResource(id = BitwardenString.year),
            value = passportState.expirationYear,
            onValueChange = passportHandlers.onExpirationYearTextChange,
            keyboardType = KeyboardType.Number,
            textFieldTestTag = "PassportExpirationYearEntry",
            cardStyle = CardStyle.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}

@Preview
@Composable
private fun VaultAddEditPassportItems_preview() {
    BitwardenTheme {
        LazyColumn {
            vaultAddEditPassportItems(
                passportState =
                    VaultAddEditState.ViewState.Content.ItemType.Passport(
                        surname = "Doe",
                        givenName = "John",
                    ),
                passportHandlers = VaultAddEditPassportTypeHandlers(
                    onSurnameTextChange = {},
                    onGivenNameTextChange = {},
                    onDobMonthSelect = {},
                    onDobYearTextChange = {},
                    onNationalityTextChange = {},
                    onPassportNumberTextChange = {},
                    onPassportTypeTextChange = {},
                    onIssuingCountryTextChange = {},
                    onIssuingAuthorityTextChange = {},
                    onIssueMonthSelect = {},
                    onIssueYearTextChange = {},
                    onExpirationMonthSelect = {},
                    onExpirationYearTextChange = {},
                ),
            )
        }
    }
}
