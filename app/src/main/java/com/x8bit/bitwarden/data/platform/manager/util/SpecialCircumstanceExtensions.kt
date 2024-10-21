package com.x8bit.bitwarden.data.platform.manager.util

import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.autofill.password.model.PasswordCredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.password.model.PasswordCredentialRequest
import com.x8bit.bitwarden.data.autofill.password.model.PasswordGetCredentialsRequest
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
 * Returns [Fido2CredentialRequest] when contained in the given [SpecialCircumstance].
 */
fun SpecialCircumstance.toFido2RequestOrNull(): Fido2CredentialRequest? =
    when (this) {
        is SpecialCircumstance.Fido2Save -> this.fido2CredentialRequest
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
 * Returns [Fido2CredentialAssertionRequest] when contained in the given [SpecialCircumstance].
 */
fun SpecialCircumstance.toFido2GetCredentialsRequestOrNull(): Fido2GetCredentialsRequest? =
    when (this) {
        is SpecialCircumstance.GetCredentials -> this.fido2GetCredentialsRequest
        else -> null
    }

/**
 * Returns [PasswordCredentialRequest] when contained in the given [SpecialCircumstance].
 */
fun SpecialCircumstance.toPasswordCredentialsRequestOrNull(): PasswordCredentialRequest? =
    when (this) {
        is SpecialCircumstance.PasswordSave -> this.passwordCredentialRequest
        else -> null
    }

/**
 * Returns [PasswordCredentialAssertionRequest] when contained in the given [SpecialCircumstance].
 */
fun SpecialCircumstance.toPasswordAssertionRequestOrNull(): PasswordCredentialAssertionRequest? =
    when (this) {
        is SpecialCircumstance.PasswordAssertion -> this.passwordAssertionRequest
        else -> null
    }

/**
 * Returns [PasswordGetCredentialsRequest] when contained in the given [SpecialCircumstance].
 */
fun SpecialCircumstance.toPasswordGetCredentialsRequestOrNull(): PasswordGetCredentialsRequest? =
    when (this) {
        is SpecialCircumstance.GetCredentials -> this.passwordGetCredentialsRequest
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
