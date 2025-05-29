package com.x8bit.bitwarden.data.platform.manager.util

import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.credentials.model.CreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.credentials.model.GetCredentialsRequest
import com.x8bit.bitwarden.data.credentials.model.PasswordCredentialGetRequest
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.ui.vault.model.TotpData

/**
 * Returns [AutofillSaveItem] when contained in the given [SpecialCircumstance].
 */
fun SpecialCircumstance.toAutofillSaveItemOrNull(): AutofillSaveItem? =
    when (this) {
        is SpecialCircumstance.AutofillSave -> this.autofillSaveItem
        else -> null
    }

/**
 * Returns [AutofillSelectionData] when contained in the given [SpecialCircumstance].
 */
fun SpecialCircumstance.toAutofillSelectionDataOrNull(): AutofillSelectionData? =
    when (this) {
        is SpecialCircumstance.AutofillSelection -> this.autofillSelectionData
        else -> null
    }

/**
 * Returns [CreateCredentialRequest] when contained in the given [SpecialCircumstance].
 */
fun SpecialCircumstance.toCreateCredentialRequestOrNull(): CreateCredentialRequest? =
    when (this) {
        is SpecialCircumstance.ProviderCreateCredential -> this.createCredentialRequest
        else -> null
    }

/**
 * Returns [Fido2CredentialAssertionRequest] when contained in the given [SpecialCircumstance].
 */
fun SpecialCircumstance.toFido2AssertionRequestOrNull(): Fido2CredentialAssertionRequest? =
    when (this) {
        is SpecialCircumstance.Fido2Assertion -> this.fido2AssertionRequest
        else -> null
    }

/**
 * Returns [PasswordCredentialGetRequest] when contained in the given [SpecialCircumstance].
 */
fun SpecialCircumstance.toPasswordGetRequestOrNull(): PasswordCredentialGetRequest? =
    when (this) {
        is SpecialCircumstance.ProviderGetPasswordRequest -> this.passwordGetRequest
        else -> null
    }

/**
 * Returns [GetCredentialsRequest] when contained in the given [SpecialCircumstance].
 */
fun SpecialCircumstance.toGetCredentialsRequestOrNull(): GetCredentialsRequest? =
    when (this) {
        is SpecialCircumstance.ProviderGetCredentials -> this.getCredentialsRequest
        else -> null
    }

/**
 * Returns the [TotpData] when contained in the given [SpecialCircumstance].
 */
fun SpecialCircumstance.toTotpDataOrNull(): TotpData? =
    when (this) {
        is SpecialCircumstance.AddTotpLoginItem -> this.data
        else -> null
    }
