package com.x8bit.bitwarden.ui.vault.feature.addedit.handlers

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditViewModel
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth

/**
 * Provides a set of handlers for interactions related to passport types within the vault
 * add/edit screen.
 */
@Suppress("LongParameterList")
data class VaultAddEditPassportTypeHandlers(
    val onSurnameTextChange: (String) -> Unit,
    val onGivenNameTextChange: (String) -> Unit,
    val onDobMonthSelect: (VaultCardExpirationMonth) -> Unit,
    val onDobYearTextChange: (String) -> Unit,
    val onNationalityTextChange: (String) -> Unit,
    val onPassportNumberTextChange: (String) -> Unit,
    val onPassportTypeTextChange: (String) -> Unit,
    val onIssuingCountryTextChange: (String) -> Unit,
    val onIssuingAuthorityTextChange: (String) -> Unit,
    val onIssueMonthSelect: (VaultCardExpirationMonth) -> Unit,
    val onIssueYearTextChange: (String) -> Unit,
    val onExpirationMonthSelect: (VaultCardExpirationMonth) -> Unit,
    val onExpirationYearTextChange: (String) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [VaultAddEditPassportTypeHandlers] with handlers that
         * dispatch actions to the provided ViewModel.
         */
        @Suppress("LongMethod")
        fun create(
            viewModel: VaultAddEditViewModel,
        ): VaultAddEditPassportTypeHandlers =
            VaultAddEditPassportTypeHandlers(
                onSurnameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType
                            .SurnameTextChange(surname = it),
                    )
                },
                onGivenNameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType
                            .GivenNameTextChange(givenName = it),
                    )
                },
                onDobMonthSelect = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType
                            .DobMonthSelect(month = it),
                    )
                },
                onDobYearTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType
                            .DobYearTextChange(year = it),
                    )
                },
                onNationalityTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType
                            .NationalityTextChange(nationality = it),
                    )
                },
                onPassportNumberTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType
                            .PassportNumberTextChange(number = it),
                    )
                },
                onPassportTypeTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType
                            .PassportTypeTextChange(type = it),
                    )
                },
                onIssuingCountryTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType
                            .IssuingCountryTextChange(country = it),
                    )
                },
                onIssuingAuthorityTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType
                            .IssuingAuthorityTextChange(authority = it),
                    )
                },
                onIssueMonthSelect = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType
                            .IssueMonthSelect(month = it),
                    )
                },
                onIssueYearTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType
                            .IssueYearTextChange(year = it),
                    )
                },
                onExpirationMonthSelect = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType
                            .ExpirationMonthSelect(month = it),
                    )
                },
                onExpirationYearTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType
                            .ExpirationYearTextChange(year = it),
                    )
                },
            )
    }
}
