package com.x8bit.bitwarden.data.autofill.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents data for a manual autofill selection.
 *
 * @property framework The framework being used to fulfill this autofill request.
 * @property type The type of autofill selection that must be made.
 * @property uri A URI representing the location where data should be filled (if available).
 */
@Parcelize
data class AutofillSelectionData(
    val framework: Framework,
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

    /**
     * The type of framework to use with this autofill.
     */
    enum class Framework {
        ACCESSIBILITY,
        AUTOFILL,
    }
}
