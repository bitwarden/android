package com.x8bit.bitwarden.ui.vault.feature.addedit.handlers

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditViewModel

/**
 * A collection of handler functions for managing user interactions on the License portion of the
 * Add/Edit cipher screen.
 *
 * @property onFirstNameTextChange Handles changes to the first name text input.
 * @property onMiddleNameTextChange Handles changes to the middle name text input.
 * @property onLastNameTextChange Handles changes to the last name text input.
 * @property onLicenseNumberTextChange Handles changes to the license number text input.
 * @property onIssuingCountryTextChange Handles changes to the issuing country text input.
 * @property onIssuingStateTextChange Handles changes to the issuing state/province text input.
 * @property onIssuingAuthorityTextChange Handles changes to the issuing authority text input.
 * @property onLicenseClassTextChange Handles changes to the license class text input.
 */
@Suppress("LongParameterList")
data class VaultAddEditLicenseTypeHandlers(
    val onFirstNameTextChange: (String) -> Unit,
    val onMiddleNameTextChange: (String) -> Unit,
    val onLastNameTextChange: (String) -> Unit,
    val onLicenseNumberTextChange: (String) -> Unit,
    val onIssuingCountryTextChange: (String) -> Unit,
    val onIssuingStateTextChange: (String) -> Unit,
    val onIssuingAuthorityTextChange: (String) -> Unit,
    val onLicenseClassTextChange: (String) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates an instance of [VaultAddEditLicenseTypeHandlers] by binding actions to
         * the provided [VaultAddEditViewModel].
         */
        @Suppress("LongMethod")
        fun create(
            viewModel: VaultAddEditViewModel,
        ): VaultAddEditLicenseTypeHandlers =
            VaultAddEditLicenseTypeHandlers(
                onFirstNameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LicenseType.FirstNameTextChange(
                            firstName = it,
                        ),
                    )
                },
                onMiddleNameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LicenseType.MiddleNameTextChange(
                            middleName = it,
                        ),
                    )
                },
                onLastNameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LicenseType.LastNameTextChange(
                            lastName = it,
                        ),
                    )
                },
                onLicenseNumberTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LicenseType.LicenseNumberTextChange(
                            licenseNumber = it,
                        ),
                    )
                },
                onIssuingCountryTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LicenseType.IssuingCountryTextChange(
                            country = it,
                        ),
                    )
                },
                onIssuingStateTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LicenseType.IssuingStateTextChange(
                            state = it,
                        ),
                    )
                },
                onIssuingAuthorityTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LicenseType.IssuingAuthorityTextChange(
                            authority = it,
                        ),
                    )
                },
                onLicenseClassTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LicenseType.LicenseClassTextChange(
                            licenseClass = it,
                        ),
                    )
                },
            )
    }
}
