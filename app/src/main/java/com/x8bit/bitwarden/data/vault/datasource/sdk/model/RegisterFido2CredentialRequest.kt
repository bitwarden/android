package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.fido.ClientData
import com.bitwarden.fido.Origin
import com.bitwarden.vault.CipherView

/**
 * Models a FIDO 2 registration request to the Bitwarden SDK.
 *
 * @param userId User whom the credential is being registered to.
 * @param origin Origin of the Relying Party. This can either be a Relying Party's URL or their
 * application fingerprint.
 * @param requestJson Registration request JSON received from the OS.
 * @param clientData Metadata containing either privileged application certificate hash or Android
 * package name of the Relying Party.
 * @param selectedCipherView [CipherView] the new FIDO 2 credential will be saved to.
 * @param isUserVerificationSupported Whether the device or application are capable of performing
 * user verification.
 */
data class RegisterFido2CredentialRequest(
    val userId: String,
    val origin: Origin,
    val requestJson: String,
    val clientData: ClientData,
    val selectedCipherView: CipherView,
    val isUserVerificationSupported: Boolean,
)
