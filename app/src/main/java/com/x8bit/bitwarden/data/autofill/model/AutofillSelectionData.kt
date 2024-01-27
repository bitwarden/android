package com.x8bit.bitwarden.data.autofill.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents data for a manual autofill selection.
 *
 * @property type The type of autofill selection that must be made.
 * @property uri A URI representing the location where data should be filled (if available).
 */
@Parcelize
class AutofillSelectionData(
    val type: Type,
    val uri: String?,
) : Parcelable {

    /**
     * The type of selection the user must make.
     */
    enum class Type {
        CARD,
        LOGIN,
    }
}
