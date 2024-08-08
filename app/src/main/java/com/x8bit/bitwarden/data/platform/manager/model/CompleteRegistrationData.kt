package com.x8bit.bitwarden.data.platform.manager.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Required data to complete ongoing registration process.
 *
 * @property email The email of the user creating the account.
 * @property verificationToken The token required to finish the registration process.
 * @property fromEmail indicates that this information came from an email AppLink.
 */
@Parcelize
data class CompleteRegistrationData(
    val email: String,
    val verificationToken: String,
    val fromEmail: Boolean,
) : Parcelable
