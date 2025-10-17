package com.bitwarden.authenticator.ui.platform.components.listitem.model

import android.os.Parcelable
import com.bitwarden.ui.util.Text
import kotlinx.collections.immutable.ImmutableList
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
    data class Codes(
        val sections: ImmutableList<SharedCodesAccountSection>,
    ) : SharedCodesDisplayState()

    /**
     * Models a section of shared authenticator codes to be displayed.
     */
    @Parcelize
    data class SharedCodesAccountSection(
        val id: String,
        val label: Text,
        val codes: ImmutableList<VerificationCodeDisplayItem>,
        val isExpanded: Boolean,
    ) : Parcelable

    /**
     * Utility function to determine if there are any codes synced.
     */
    fun isEmpty(): Boolean = when (this) {
        is Codes -> this.sections.isEmpty()
        Error -> true
    }
}
