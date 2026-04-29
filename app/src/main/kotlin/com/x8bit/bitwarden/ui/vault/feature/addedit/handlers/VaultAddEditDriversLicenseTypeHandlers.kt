package com.x8bit.bitwarden.ui.vault.feature.addedit.handlers

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditViewModel
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth

/**
 * A collection of handler functions for managing user interactions on the Driver's License
 * portion of the Add/Edit cipher screen.
 *
 * @property onFirstNameTextChange Handles changes to the first name text input.
 * @property onMiddleNameTextChange Handles changes to the middle name text input.
 * @property onLastNameTextChange Handles changes to the last name text input.
 * @property onLicenseNumberTextChange Handles changes to the license number text input.
 * @property onIssuingCountryTextChange Handles changes to the issuing country text input.
 * @property onIssuingStateTextChange Handles changes to the issuing state/province text input.
 * @property onExpirationMonthSelect Handles selection of the expiration month dropdown.
 * @property onExpirationDayTextChange Handles changes to the expiration day text input.
 * @property onExpirationYearTextChange Handles changes to the expiration year text input.
 * @property onLicenseClassTextChange Handles changes to the license class text input.
 */
@Suppress("LongParameterList")
data class VaultAddEditDriversLicenseTypeHandlers(
    val onFirstNameTextChange: (String) -> Unit,
    val onMiddleNameTextChange: (String) -> Unit,
    val onLastNameTextChange: (String) -> Unit,
    val onLicenseNumberTextChange: (String) -> Unit,
    val onIssuingCountryTextChange: (String) -> Unit,
    val onIssuingStateTextChange: (String) -> Unit,
    val onExpirationMonthSelect: (VaultCardExpirationMonth) -> Unit,
    val onExpirationDayTextChange: (String) -> Unit,
    val onExpirationYearTextChange: (String) -> Unit,
    val onLicenseClassTextChange: (String) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates an instance of [VaultAddEditDriversLicenseTypeHandlers] by binding actions to
         * the provided [VaultAddEditViewModel].
         */
        @Suppress("LongMethod")
        fun create(
            viewModel: VaultAddEditViewModel,
        ): VaultAddEditDriversLicenseTypeHandlers =
            VaultAddEditDriversLicenseTypeHandlers(
                onFirstNameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType.FirstNameTextChange(
                            firstName = it,
                        ),
                    )
                },
                onMiddleNameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType.MiddleNameTextChange(
                            middleName = it,
                        ),
                    )
                },
                onLastNameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType.LastNameTextChange(
                            lastName = it,
                        ),
                    )
                },
                onLicenseNumberTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType.LicenseNumberTextChange(
                            licenseNumber = it,
                        ),
                    )
                },
                onIssuingCountryTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType.IssuingCountryTextChange(
                            country = it,
                        ),
                    )
                },
                onIssuingStateTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType.IssuingStateTextChange(
                            state = it,
                        ),
                    )
                },
                onExpirationMonthSelect = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType.ExpirationMonthSelect(
                            month = it,
                        ),
                    )
                },
                onExpirationDayTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType.ExpirationDayTextChange(
                            day = it,
                        ),
                    )
                },
                onExpirationYearTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType.ExpirationYearTextChange(
                            year = it,
                        ),
                    )
                },
                onLicenseClassTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.DriversLicenseType.LicenseClassTextChange(
                            licenseClass = it,
                        ),
                    )
                },
            )
    }
}
