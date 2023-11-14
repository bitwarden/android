package com.x8bit.bitwarden.data.tools.generator.repository.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A data class representing the configuration options for password generation.
 *
 * @property length The total length of the generated password.
 * @property allowAmbiguousChar Indicates whether ambiguous characters are allowed in the password.
 * @property hasNumbers Indicates whether the password should contain numbers.
 * @property minNumber The minimum number of numeric characters required in the password.
 * @property hasUppercase Indicates whether the password should contain uppercase characters.
 * @property minUppercase The minimum number of uppercase characters required in the password.
 * @property hasLowercase Indicates whether the password should contain lowercase characters.
 * @property minLowercase The minimum number of lowercase characters required in the password.
 * @property allowSpecial Indicates whether special characters are allowed in the password.
 * @property minSpecial The minimum number of special characters required in the password.
 */
@Serializable
data class PasswordGenerationOptions(
    @SerialName("length")
    val length: Int,

    @SerialName("allowAmbiguousChar")
    val allowAmbiguousChar: Boolean,

    @SerialName("number")
    val hasNumbers: Boolean,

    @SerialName("minNumber")
    val minNumber: Int,

    @SerialName("uppercase")
    val hasUppercase: Boolean,

    @SerialName("minUppercase")
    val minUppercase: Int?,

    @SerialName("lowercase")
    val hasLowercase: Boolean,

    @SerialName("minLowercase")
    val minLowercase: Int?,

    @SerialName("special")
    val allowSpecial: Boolean,

    @SerialName("minSpecial")
    val minSpecial: Int,
)
