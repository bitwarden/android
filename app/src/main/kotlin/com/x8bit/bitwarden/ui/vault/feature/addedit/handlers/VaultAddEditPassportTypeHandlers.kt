package com.x8bit.bitwarden.ui.vault.feature.addedit.handlers

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditViewModel

/**
 * A collection of handler functions for managing user interactions on the Passport portion of the
 * Add/Edit cipher screen.
 *
 * @property onGivenNameTextChange Handles changes to the given name (first name) text input.
 * @property onSurnameTextChange Handles changes to the surname (last name) text input.
 * @property onDateOfBirthTextChange Handles changes to the date of birth text input.
 * @property onSexTextChange Handles changes to the sex text input.
 * @property onBirthPlaceTextChange Handles changes to the birth place text input.
 * @property onNationalityTextChange Handles changes to the nationality text input.
 * @property onPassportNumberTextChange Handles changes to the passport number text input.
 * @property onPassportTypeTextChange Handles changes to the passport type text input.
 * @property onNationalIdentificationNumberTextChange Handles changes to the national identification
 * number text input.
 * @property onIssuingCountryTextChange Handles changes to the issuing country text input.
 * @property onIssuingAuthorityTextChange Handles changes to the issuing authority text input.
 * @property onIssueDateTextChange Handles changes to the issue date text input.
 * @property onExpirationDateTextChange Handles changes to the expiration date text input.
 */
@Suppress("LongParameterList")
data class VaultAddEditPassportTypeHandlers(
    val onGivenNameTextChange: (String) -> Unit,
    val onSurnameTextChange: (String) -> Unit,
    val onDateOfBirthTextChange: (String) -> Unit,
    val onSexTextChange: (String) -> Unit,
    val onBirthPlaceTextChange: (String) -> Unit,
    val onNationalityTextChange: (String) -> Unit,
    val onPassportNumberTextChange: (String) -> Unit,
    val onPassportTypeTextChange: (String) -> Unit,
    val onNationalIdentificationNumberTextChange: (String) -> Unit,
    val onIssuingCountryTextChange: (String) -> Unit,
    val onIssuingAuthorityTextChange: (String) -> Unit,
    val onIssueDateTextChange: (String) -> Unit,
    val onExpirationDateTextChange: (String) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates an instance of [VaultAddEditPassportTypeHandlers] by binding actions to
         * the provided [VaultAddEditViewModel].
         */
        @Suppress("LongMethod")
        fun create(
            viewModel: VaultAddEditViewModel,
        ): VaultAddEditPassportTypeHandlers =
            VaultAddEditPassportTypeHandlers(
                onGivenNameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.GivenNameTextChange(
                            givenName = it,
                        ),
                    )
                },
                onSurnameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.SurnameTextChange(
                            surname = it,
                        ),
                    )
                },
                onDateOfBirthTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.DateOfBirthTextChange(
                            dateOfBirth = it,
                        ),
                    )
                },
                onSexTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.SexTextChange(sex = it),
                    )
                },
                onBirthPlaceTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.BirthPlaceTextChange(
                            birthPlace = it,
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
                onNationalIdentificationNumberTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction
                            .ItemType
                            .PassportType
                            .NationalIdentificationNumberTextChange(
                                nationalIdentificationNumber = it,
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
                onIssueDateTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.IssueDateTextChange(
                            issueDate = it,
                        ),
                    )
                },
                onExpirationDateTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.PassportType.ExpirationDateTextChange(
                            expirationDate = it,
                        ),
                    )
                },
            )
    }
}
