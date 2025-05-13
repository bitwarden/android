package com.x8bit.bitwarden.data.credentials.builder

import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.fido.Fido2CredentialAutofillView

/**
 * Builder for credential entries.
 */
interface CredentialEntryBuilder {

    /**
     * Build public key credential entries from the given cipher views and options.
     */
    fun buildPublicKeyCredentialEntries(
        userId: String,
        fido2CredentialAutofillViews: List<Fido2CredentialAutofillView>,
        beginGetPublicKeyCredentialOptions: List<BeginGetPublicKeyCredentialOption>,
        isUserVerified: Boolean,
    ): List<PublicKeyCredentialEntry>
}
