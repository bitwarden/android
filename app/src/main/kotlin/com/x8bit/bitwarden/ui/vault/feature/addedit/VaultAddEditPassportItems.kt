package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditPassportTypeHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import kotlinx.collections.immutable.toImmutableList

/**
 * The UI for adding and editing a passport cipher.
 *
 * The passport number is rendered as a [BitwardenPasswordField] with reveal toggle to mirror the
 * national identification number UX from the Bitwarden web client. Visibility is local to the
 * composable since the canonical state model exposes [String] only.
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
        var showPassportNumber by rememberSaveable { mutableStateOf(value = false) }
        BitwardenPasswordField(
            label = stringResource(id = BitwardenString.passport_number),
            value = passportState.passportNumber,
            onValueChange = passportHandlers.onPassportNumberTextChange,
            showPassword = showPassportNumber,
            showPasswordChange = { showPassportNumber = !showPassportNumber },
            showPasswordTestTag = "ShowPassportNumberButton",
            passwordFieldTestTag = "PassportNumberEntry",
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

    passportDateRow(
        labelMonth = BitwardenString.date_of_birth_month,
        labelDay = BitwardenString.date_of_birth_day,
        labelYear = BitwardenString.date_of_birth_year,
        monthValue = passportState.dobMonth,
        dayValue = passportState.dobDay,
        yearValue = passportState.dobYear,
        monthTestTag = "PassportDateOfBirthMonthPicker",
        dayTestTag = "PassportDateOfBirthDayEntry",
        yearTestTag = "PassportDateOfBirthYearEntry",
        onMonthSelect = passportHandlers.onDateOfBirthMonthSelect,
        onDayTextChange = passportHandlers.onDateOfBirthDayTextChange,
        onYearTextChange = passportHandlers.onDateOfBirthYearTextChange,
    )

    passportDateRow(
        labelMonth = BitwardenString.issue_month,
        labelDay = BitwardenString.issue_day,
        labelYear = BitwardenString.issue_year,
        monthValue = passportState.issueMonth,
        dayValue = passportState.issueDay,
        yearValue = passportState.issueYear,
        monthTestTag = "PassportIssueMonthPicker",
        dayTestTag = "PassportIssueDayEntry",
        yearTestTag = "PassportIssueYearEntry",
        onMonthSelect = passportHandlers.onIssueMonthSelect,
        onDayTextChange = passportHandlers.onIssueDayTextChange,
        onYearTextChange = passportHandlers.onIssueYearTextChange,
    )

    passportDateRow(
        labelMonth = BitwardenString.expiration_month,
        labelDay = BitwardenString.expiration_day,
        labelYear = BitwardenString.expiration_year,
        monthValue = passportState.expirationMonth,
        dayValue = passportState.expirationDay,
        yearValue = passportState.expirationYear,
        monthTestTag = "PassportExpirationMonthPicker",
        dayTestTag = "PassportExpirationDayEntry",
        yearTestTag = "PassportExpirationYearEntry",
        onMonthSelect = passportHandlers.onExpirationMonthSelect,
        onDayTextChange = passportHandlers.onExpirationDayTextChange,
        onYearTextChange = passportHandlers.onExpirationYearTextChange,
        isLastRow = true,
    )
}

/**
 * Renders a month/day/year tuple for one of the Passport's three date fields. The month picker
 * reuses [VaultCardExpirationMonth] for visual parity with the Card item type; the day and year
 * fields are numeric text inputs. Cards are stacked into a single visual block.
 */
@Suppress("LongParameterList")
private fun LazyListScope.passportDateRow(
    labelMonth: Int,
    labelDay: Int,
    labelYear: Int,
    monthValue: String,
    dayValue: String,
    yearValue: String,
    monthTestTag: String,
    dayTestTag: String,
    yearTestTag: String,
    onMonthSelect: (VaultCardExpirationMonth) -> Unit,
    onDayTextChange: (String) -> Unit,
    onYearTextChange: (String) -> Unit,
    isLastRow: Boolean = false,
) {
    item {
        val resources = LocalResources.current
        val selectedMonth = VaultCardExpirationMonth
            .entries
            .find { it.number == monthValue }
            ?: VaultCardExpirationMonth.SELECT
        BitwardenMultiSelectButton(
            label = stringResource(id = labelMonth),
            options = VaultCardExpirationMonth
                .entries
                .map { it.value() }
                .toImmutableList(),
            selectedOption = selectedMonth.value(),
            onOptionSelected = { selectedString ->
                onMonthSelect(
                    VaultCardExpirationMonth
                        .entries
                        .first { it.value.toString(resources) == selectedString },
                )
            },
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .testTag(monthTestTag)
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = labelDay),
            value = dayValue,
            onValueChange = onDayTextChange,
            keyboardType = KeyboardType.Number,
            textFieldTestTag = dayTestTag,
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = labelYear),
            value = yearValue,
            onValueChange = onYearTextChange,
            keyboardType = KeyboardType.Number,
            textFieldTestTag = yearTestTag,
            cardStyle = if (isLastRow) CardStyle.Bottom else CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}
