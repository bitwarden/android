package com.x8bit.bitwarden.data.autofill.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents data for a TOTP copying during the autofill flow via authentication intents.
 *
 * @property cipherId The cipher for which we are copying a TOTP to the clipboard.
 */
@Parcelize
data class AutofillTotpCopyData(
    val cipherId: String,
) : Parcelable
