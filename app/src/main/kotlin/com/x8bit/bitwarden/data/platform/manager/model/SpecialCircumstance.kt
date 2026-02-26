package com.x8bit.bitwarden.data.platform.manager.model

import android.os.Parcelable
import androidx.credentials.CredentialManager
import com.bitwarden.cxf.model.ImportCredentialsRequestData
import com.bitwarden.ui.platform.manager.share.model.ShareData
import com.bitwarden.ui.platform.model.TotpData
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.credentials.model.CreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.credentials.model.GetCredentialsRequest
import com.x8bit.bitwarden.data.credentials.model.ProviderGetPasswordCredentialRequest
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
        val data: ShareData,
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
     * The app was launched via the [CredentialManager] framework in order to allow the user to
     * manually save a credential to their vault.
     */
    @Parcelize
    data class ProviderCreateCredential(
        val createCredentialRequest: CreateCredentialRequest,
    ) : SpecialCircumstance()

    /**
     * The app was launched via the [CredentialManager] framework in order to authenticate a FIDO 2
     * credential saved to the user's vault.
     */
    @Parcelize
    data class Fido2Assertion(
        val fido2AssertionRequest: Fido2CredentialAssertionRequest,
    ) : SpecialCircumstance()

    /**
     * The app was launched via the [CredentialManager] framework in order to retrieve a Password
     * credential saved to the user's vault.
     */
    @Parcelize
    data class ProviderGetPasswordRequest(
        val passwordGetRequest: ProviderGetPasswordCredentialRequest,
    ) : SpecialCircumstance()

    /**
     * The app was launched via the [CredentialManager] framework request to retrieve credentials
     * associated with the requesting entity.
     */
    @Parcelize
    data class ProviderGetCredentials(
        val getCredentialsRequest: GetCredentialsRequest,
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
     * Deeplink to the Send.
     */
    @Parcelize
    data object SendShortcut : SpecialCircumstance()

    /**
     * Deeplink to the Search.
     */
    @Parcelize
    data class SearchShortcut(val searchTerm: String) : SpecialCircumstance()

    /**
     * Deeplink to the Verification Code.
     */
    @Parcelize
    data object VerificationCodeShortcut : SpecialCircumstance()

    /**
     * The app was launched to select an account to export credentials from.
     */
    @Parcelize
    data class CredentialExchangeExport(
        val data: ImportCredentialsRequestData,
    ) : SpecialCircumstance()

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
