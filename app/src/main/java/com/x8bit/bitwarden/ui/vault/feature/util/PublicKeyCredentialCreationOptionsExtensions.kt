@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.util

import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.model.PublicKeyCredentialCreationOptions
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Returns true if the user verification should be prompted when registering this FIDO 2 credential.
 */
@Suppress("MaxLineLength")
val PublicKeyCredentialCreationOptions.promptForUserVerification: Boolean
    get() {
        return when (this.authenticatorSelection.userVerification) {
            PublicKeyCredentialCreationOptions.AuthenticatorSelectionCriteria.UserVerificationRequirement.PREFERRED,
            PublicKeyCredentialCreationOptions.AuthenticatorSelectionCriteria.UserVerificationRequirement.REQUIRED,
            -> true

            PublicKeyCredentialCreationOptions.AuthenticatorSelectionCriteria.UserVerificationRequirement.DISCOURAGED,
            null,
            -> false
        }
    }
