package com.x8bit.bitwarden.ui.vault.feature.addedit.handlers

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditViewModel
import com.x8bit.bitwarden.ui.vault.model.VaultIdentityTitle

/**
 * A collection of handler functions specifically tailored for managing actions
 * within the context of adding identity items to a vault.
 *
 * @property onFirstNameTextChange Handles the action when the first name text is changed.
 * @property onMiddleNameTextChange Handles the action when the middle name text is changed.
 * @property onLastNameTextChange Handles the action when the last name text is changed.
 * @property onUsernameTextChange Handles the action when the username text is changed.
 * @property onCompanyTextChange Handles the action when the company text is changed.
 * @property onSsnTextChange Handles the action when the SSN text is changed.
 * @property onPassportNumberTextChange Handles the action when the passport number text is changed.
 * @property onLicenseNumberTextChange Handles the action when the license number text is changed.
 * @property onEmailTextChange Handles the action when the email text is changed.
 * @property onPhoneTextChange Handles the action when the phone text is changed.
 * @property onAddress1TextChange Handles the action when the address1 text is changed.
 * @property onAddress2TextChange Handles the action when the address2 text is changed.
 * @property onAddress3TextChange Handles the action when the address3 text is changed.
 * @property onCityTextChange Handles the action when the city text is changed.
 * @property onZipTextChange Handles the action when the zip text is changed.
 * @property onCountryTextChange Handles the action when the country text is changed.
 */
@Suppress("LongParameterList")
data class VaultAddEditIdentityTypeHandlers(
    val onTitleSelected: (VaultIdentityTitle) -> Unit,
    val onFirstNameTextChange: (String) -> Unit,
    val onMiddleNameTextChange: (String) -> Unit,
    val onLastNameTextChange: (String) -> Unit,
    val onUsernameTextChange: (String) -> Unit,
    val onCompanyTextChange: (String) -> Unit,
    val onSsnTextChange: (String) -> Unit,
    val onPassportNumberTextChange: (String) -> Unit,
    val onLicenseNumberTextChange: (String) -> Unit,
    val onEmailTextChange: (String) -> Unit,
    val onPhoneTextChange: (String) -> Unit,
    val onAddress1TextChange: (String) -> Unit,
    val onAddress2TextChange: (String) -> Unit,
    val onAddress3TextChange: (String) -> Unit,
    val onCityTextChange: (String) -> Unit,
    val onStateTextChange: (String) -> Unit,
    val onZipTextChange: (String) -> Unit,
    val onCountryTextChange: (String) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates an instance of [VaultAddEditIdentityTypeHandlers] by binding actions
         * to the provided [VaultAddEditViewModel].
         */
        @Suppress("LongMethod")
        fun create(viewModel: VaultAddEditViewModel): VaultAddEditIdentityTypeHandlers {
            return VaultAddEditIdentityTypeHandlers(
                onTitleSelected = { newTitle ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.TitleSelect(
                            title = newTitle,
                        ),
                    )
                },
                onFirstNameTextChange = { newFirstName ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.FirstNameTextChange(
                            firstName = newFirstName,
                        ),
                    )
                },
                onMiddleNameTextChange = { newMiddleName ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.MiddleNameTextChange(
                            middleName = newMiddleName,
                        ),
                    )
                },
                onLastNameTextChange = { newLastName ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.LastNameTextChange(
                            lastName = newLastName,
                        ),
                    )
                },
                onUsernameTextChange = { newUsername ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.UsernameTextChange(
                            username = newUsername,
                        ),
                    )
                },
                onCompanyTextChange = { newCompany ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.CompanyTextChange(
                            company = newCompany,
                        ),
                    )
                },
                onSsnTextChange = { newSsn ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.SsnTextChange(
                            ssn = newSsn,
                        ),
                    )
                },
                onPassportNumberTextChange = { newPassportNumber ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.PassportNumberTextChange(
                            passportNumber = newPassportNumber,
                        ),
                    )
                },
                onLicenseNumberTextChange = { newLicenseNumber ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.LicenseNumberTextChange(
                            licenseNumber = newLicenseNumber,
                        ),
                    )
                },
                onEmailTextChange = { newEmail ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.EmailTextChange(
                            email = newEmail,
                        ),
                    )
                },
                onPhoneTextChange = { newPhone ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.PhoneTextChange(
                            phone = newPhone,
                        ),
                    )
                },
                onAddress1TextChange = { newAddress1 ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.Address1TextChange(
                            address1 = newAddress1,
                        ),
                    )
                },
                onAddress2TextChange = { newAddress2 ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.Address2TextChange(
                            address2 = newAddress2,
                        ),
                    )
                },
                onAddress3TextChange = { newAddress3 ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.Address3TextChange(
                            address3 = newAddress3,
                        ),
                    )
                },
                onCityTextChange = { newCity ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.CityTextChange(
                            city = newCity,
                        ),
                    )
                },
                onStateTextChange = { newState ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.StateTextChange(
                            state = newState,
                        ),
                    )
                },
                onZipTextChange = { newZip ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.ZipTextChange(
                            zip = newZip,
                        ),
                    )
                },
                onCountryTextChange = { newCountry ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.IdentityType.CountryTextChange(
                            country = newCountry,
                        ),
                    )
                },
            )
        }
    }
}
