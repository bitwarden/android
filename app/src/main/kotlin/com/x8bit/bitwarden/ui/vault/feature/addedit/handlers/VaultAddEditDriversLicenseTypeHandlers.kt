package com.x8bit.bitwarden.ui.vault.feature.addedit.handlers

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditViewModel
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth

/**
 * Provides a set of handlers for interactions related to driver's license types within the
 * vault add/edit screen.
 */
data class VaultAddEditDriversLicenseTypeHandlers(
    val onFirstNameTextChange: (String) -> Unit,
    val onMiddleNameTextChange: (String) -> Unit,
    val onLastNameTextChange: (String) -> Unit,
    val onLicenseNumberTextChange: (String) -> Unit,
    val onIssuingCountryTextChange: (String) -> Unit,
    val onIssuingStateTextChange: (String) -> Unit,
    val onExpirationMonthSelect: (VaultCardExpirationMonth) -> Unit,
    val onExpirationYearTextChange: (String) -> Unit,
    val onLicenseClassTextChange: (String) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [VaultAddEditDriversLicenseTypeHandlers] with handlers that
         * dispatch actions to the provided ViewModel.
         */
        @Suppress("LongMethod")
        fun create(
            viewModel: VaultAddEditViewModel,
        ): VaultAddEditDriversLicenseTypeHandlers =
            VaultAddEditDriversLicenseTypeHandlers(
                onFirstNameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType
                            .FirstNameTextChange(firstName = it),
                    )
                },
                onMiddleNameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType
                            .MiddleNameTextChange(middleName = it),
                    )
                },
                onLastNameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType
                            .LastNameTextChange(lastName = it),
                    )
                },
                onLicenseNumberTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType
                            .LicenseNumberTextChange(licenseNumber = it),
                    )
                },
                onIssuingCountryTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType
                            .IssuingCountryTextChange(country = it),
                    )
                },
                onIssuingStateTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType
                            .IssuingStateTextChange(state = it),
                    )
                },
                onExpirationMonthSelect = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType
                            .ExpirationMonthSelect(month = it),
                    )
                },
                onExpirationYearTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType
                            .ExpirationYearTextChange(year = it),
                    )
                },
                onLicenseClassTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType
                            .LicenseClassTextChange(licenseClass = it),
                    )
                },
            )
    }
}
