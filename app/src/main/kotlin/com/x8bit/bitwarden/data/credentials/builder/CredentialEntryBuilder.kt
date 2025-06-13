package com.x8bit.bitwarden.data.credentials.builder

import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher

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

    /**
     * Build password credential entries from the given cipher views and options.
     */
    fun buildPasswordCredentialEntries(
        userId: String,
        passwordCredentialAutofillViews: List<AutofillCipher.Login>,
        beginGetPasswordCredentialOptions: List<BeginGetPasswordOption>,
        isUserVerified: Boolean,
    ): List<PasswordCredentialEntry>

}
