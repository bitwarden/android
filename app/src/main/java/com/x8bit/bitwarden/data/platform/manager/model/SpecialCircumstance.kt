package com.x8bit.bitwarden.data.platform.manager.model

import android.os.Parcelable
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import kotlinx.parcelize.Parcelize

/**
 * Represents a special circumstance the app may be in. These circumstances could require some kind
 * of navigation that is counter to what otherwise may happen based on the state of the app.
 */
sealed class SpecialCircumstance : Parcelable {
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
     * The app was launched via deeplink to the generator.
     */
    @Parcelize
    data object GeneratorShortcut : SpecialCircumstance()

    /**
     * The app was launched via deeplink to the vault.
     */
    @Parcelize
    data object VaultShortcut : SpecialCircumstance()
}
