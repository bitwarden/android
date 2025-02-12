package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model

import android.os.Parcelable
import com.bitwarden.authenticator.ui.platform.base.util.Text
import kotlinx.parcelize.Parcelize

/**
 * Models how shared codes should be displayed.
 */
sealed class SharedCodesDisplayState : Parcelable {

    /**
     * There was an error syncing codes.
     */
    @Parcelize
    data object Error : SharedCodesDisplayState()

    /**
     * Display the given [sections] of verification codes.
     */
    @Parcelize
    data class Codes(val sections: List<SharedCodesAccountSection>) : SharedCodesDisplayState()

    /**
     * Models a section of shared authenticator codes to be displayed.
     */
    @Parcelize
    data class SharedCodesAccountSection(
        val label: Text,
        val codes: List<VerificationCodeDisplayItem>,
    ) : Parcelable

    /**
     * Utility function to determine if there are any codes synced.
     */
    fun isEmpty() = when (this) {
        is Codes -> this.sections.isEmpty()
        Error -> true
    }
}
