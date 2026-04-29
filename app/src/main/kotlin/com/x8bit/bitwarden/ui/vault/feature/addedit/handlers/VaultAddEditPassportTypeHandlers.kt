package com.x8bit.bitwarden.ui.vault.feature.addedit.handlers

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditViewModel
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth

/**
 * A collection of handler functions for managing user interactions on the Passport portion of the
 * Add/Edit cipher screen.
 *
 * @property onSurnameTextChange Handles changes to the surname text input.
 * @property onGivenNameTextChange Handles changes to the given name text input.
 * @property onDateOfBirthMonthSelect Handles selection of the date of birth month dropdown.
 * @property onDateOfBirthDayTextChange Handles changes to the date of birth day text input.
 * @property onDateOfBirthYearTextChange Handles changes to the date of birth year text input.
 * @property onNationalityTextChange Handles changes to the nationality text input.
 * @property onPassportNumberTextChange Handles changes to the passport number text input.
 * @property onPassportTypeTextChange Handles changes to the passport type text input.
 * @property onIssuingCountryTextChange Handles changes to the issuing country text input.
 * @property onIssuingAuthorityTextChange Handles changes to the issuing authority text input.
 * @property onIssueMonthSelect Handles selection of the issue month dropdown.
 * @property onIssueDayTextChange Handles changes to the issue day text input.
 * @property onIssueYearTextChange Handles changes to the issue year text input.
 * @property onExpirationMonthSelect Handles selection of the expiration month dropdown.
 * @property onExpirationDayTextChange Handles changes to the expiration day text input.
 * @property onExpirationYearTextChange Handles changes to the expiration year text input.
 */
@Suppress("LongParameterList")
data class VaultAddEditPassportTypeHandlers(
    val onSurnameTextChange: (String) -> Unit,
    val onGivenNameTextChange: (String) -> Unit,
    val onDateOfBirthMonthSelect: (VaultCardExpirationMonth) -> Unit,
    val onDateOfBirthDayTextChange: (String) -> Unit,
    val onDateOfBirthYearTextChange: (String) -> Unit,
    val onNationalityTextChange: (String) -> Unit,
    val onPassportNumberTextChange: (String) -> Unit,
    val onPassportTypeTextChange: (String) -> Unit,
    val onIssuingCountryTextChange: (String) -> Unit,
    val onIssuingAuthorityTextChange: (String) -> Unit,
    val onIssueMonthSelect: (VaultCardExpirationMonth) -> Unit,
    val onIssueDayTextChange: (String) -> Unit,
    val onIssueYearTextChange: (String) -> Unit,
    val onExpirationMonthSelect: (VaultCardExpirationMonth) -> Unit,
    val onExpirationDayTextChange: (String) -> Unit,
    val onExpirationYearTextChange: (String) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates an instance of [VaultAddEditPassportTypeHandlers] by binding actions to the
         * provided [VaultAddEditViewModel].
         */
        @Suppress("LongMethod")
        fun create(
            viewModel: VaultAddEditViewModel,
        ): VaultAddEditPassportTypeHandlers =
            VaultAddEditPassportTypeHandlers(
                onSurnameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.SurnameTextChange(
                            surname = it,
                        ),
                    )
                },
                onGivenNameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.GivenNameTextChange(
                            givenName = it,
                        ),
                    )
                },
                onDateOfBirthMonthSelect = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.DateOfBirthMonthSelect(
                            month = it,
                        ),
                    )
                },
                onDateOfBirthDayTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.DateOfBirthDayTextChange(
                            day = it,
                        ),
                    )
                },
                onDateOfBirthYearTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.DateOfBirthYearTextChange(
                            year = it,
                        ),
                    )
                },
                onNationalityTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.NationalityTextChange(
                            nationality = it,
                        ),
                    )
                },
                onPassportNumberTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.PassportNumberTextChange(
                            passportNumber = it,
                        ),
                    )
                },
                onPassportTypeTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.PassportTypeTextChange(
                            passportType = it,
                        ),
                    )
                },
                onIssuingCountryTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.IssuingCountryTextChange(
                            country = it,
                        ),
                    )
                },
                onIssuingAuthorityTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.IssuingAuthorityTextChange(
                            authority = it,
                        ),
                    )
                },
                onIssueMonthSelect = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.IssueMonthSelect(
                            month = it,
                        ),
                    )
                },
                onIssueDayTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.IssueDayTextChange(
                            day = it,
                        ),
                    )
                },
                onIssueYearTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.IssueYearTextChange(
                            year = it,
                        ),
                    )
                },
                onExpirationMonthSelect = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.ExpirationMonthSelect(
                            month = it,
                        ),
                    )
                },
                onExpirationDayTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.ExpirationDayTextChange(
                            day = it,
                        ),
                    )
                },
                onExpirationYearTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.ExpirationYearTextChange(
                            year = it,
                        ),
                    )
                },
            )
    }
}
