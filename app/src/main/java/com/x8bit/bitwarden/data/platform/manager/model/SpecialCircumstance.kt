package com.x8bit.bitwarden.data.platform.manager.model

import android.os.Parcelable
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.vault.model.TotpData
import kotlinx.parcelize.Parcelize

/**
 * Represents a special circumstance the app may be in. These circumstances could require some kind
 * of navigation that is counter to what otherwise may happen based on the state of the app.
 */
sealed class SpecialCircumstance : Parcelable {
    /**
     * The app was launched in order to add a new TOTP to a cipher.
     */
    @Parcelize
    data class AddTotpLoginItem(
        val data: TotpData,
    ) : SpecialCircumstance()

    /**
     * The app was launched in order to create/share a new Send using the given [data].
     */
    @Parcelize
    data class ShareNewSend(
        val data: IntentManager.ShareData,
        val shouldFinishWhenComplete: Boolean,
    ) : SpecialCircumstance()

    /**
     * The app was launched via the autofill framework in order to allow the user to manually save
     * data that was entered in an external form.
     */
    @Parcelize
    data class AutofillSave(
        val autofillSaveItem: AutofillSaveItem,
    ) : SpecialCircumstance()

    /**
     * The app was launched in order to allow the user to manually select data for autofill.
     */
    @Parcelize
    data class AutofillSelection(
        val autofillSelectionData: AutofillSelectionData,
        val shouldFinishWhenComplete: Boolean,
    ) : SpecialCircumstance()

    /**
     * The app was launched in order to allow the user to authorize a passwordless login.
     */
    @Parcelize
    data class PasswordlessRequest(
        val passwordlessRequestData: PasswordlessRequestData,
        val shouldFinishWhenComplete: Boolean,
    ) : SpecialCircumstance()

    /**
     * The app was launched via the credential manager framework in order to allow the user to
     * manually save a passkey to their vault.
     */
    @Parcelize
    data class Fido2Save(
        val fido2CreateCredentialRequest: Fido2CreateCredentialRequest,
    ) : SpecialCircumstance()

    /**
     * The app was launched via the credential manager framework in order to authenticate a FIDO 2
     * credential saved to the user's vault.
     */
    @Parcelize
    data class Fido2Assertion(
        val fido2AssertionRequest: Fido2CredentialAssertionRequest,
    ) : SpecialCircumstance()

    /**
     * The app was launched via the credential manager framework request to retrieve passkeys
     * associated with the requesting entity.
     */
    @Parcelize
    data class Fido2GetCredentials(
        val fido2GetCredentialsRequest: Fido2GetCredentialsRequest,
    ) : SpecialCircumstance()

    /**
     * The app was launched via deeplink to the generator.
     */
    @Parcelize
    data object GeneratorShortcut : SpecialCircumstance()

    /**
     * The app was launched via deeplink to the vault.
     */
    @Parcelize
    data object VaultShortcut : SpecialCircumstance()

    /**
     * The app was launched via deeplink to the account security screen.
     */
    @Parcelize
    data object AccountSecurityShortcut : SpecialCircumstance()

    /**
     * A subset of [SpecialCircumstance] that are only relevant in a pre-login state and should be
     * cleared after a successful login.
     */
    @Parcelize
    sealed class RegistrationEvent : SpecialCircumstance() {
        /**
         * The app was launched via AppLink in order to allow the user complete an ongoing
         * registration.
         */
        @Parcelize
        data class CompleteRegistration(
            val completeRegistrationData: CompleteRegistrationData,
            val timestamp: Long,
        ) : RegistrationEvent()

        /**
         * The app was launched via AppLink in order to allow the user to complete registration but,
         * the registration link has expired.
         */
        @Parcelize
        data object ExpiredRegistrationLink : RegistrationEvent()
    }
}
