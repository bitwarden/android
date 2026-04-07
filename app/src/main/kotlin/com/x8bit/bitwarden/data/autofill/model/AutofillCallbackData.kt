package com.x8bit.bitwarden.data.autofill.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents data for the autofill flow via authentication intents.
 *
 * @property cipherId The ID of the cipher associated with this Autofill instance.
 */
@Parcelize
data class AutofillCallbackData(
    val cipherId: String,
) : Parcelable
