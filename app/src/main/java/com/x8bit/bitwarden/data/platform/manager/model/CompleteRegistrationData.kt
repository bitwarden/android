package com.x8bit.bitwarden.data.platform.manager.model

import android.os.Parcelable
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import kotlinx.parcelize.Parcelize

/**
 * Required data to complete ongoing registration process.
 *
 * @property email The email of the user creating the account.
 * @property verificationToken The token required to finish the registration process.
 */
@Parcelize
data class CompleteRegistrationData(
    val email: String,
    val verificationToken: String,
    val fromEmail: Boolean,
    val region: Environment.Type
) : Parcelable
