package com.x8bit.bitwarden.data.platform.manager.util

import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
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
 * Returns [Fido2CreateCredentialRequest] when contained in the given [SpecialCircumstance].
 */
fun SpecialCircumstance.toFido2CreateRequestOrNull(): Fido2CreateCredentialRequest? =
    when (this) {
        is SpecialCircumstance.Fido2Save -> this.fido2CreateCredentialRequest
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
        is SpecialCircumstance.Fido2GetCredentials -> this.fido2GetCredentialsRequest
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
