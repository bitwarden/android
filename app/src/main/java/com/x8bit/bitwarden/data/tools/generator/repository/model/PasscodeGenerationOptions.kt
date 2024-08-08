package com.x8bit.bitwarden.data.tools.generator.repository.model

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A data class representing the configuration options for both password and passphrase generation.
 *
 * @property type The type of passcode to be generated, as defined in PasscodeType.
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
 * @property numWords The number of words in the generated passphrase.
 * @property wordSeparator The character used to separate words in the passphrase.
 * @property allowCapitalize Indicates whether to use capitals in the passphrase.
 * @property allowIncludeNumber Indicates whether to include numbers in the passphrase.
 */
@Serializable
data class PasscodeGenerationOptions(
    @SerialName("type")
    val type: PasscodeType,

    // Password-specific options

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
    val minUppercase: Int? = null,

    @SerialName("lowercase")
    val hasLowercase: Boolean,

    @SerialName("minLowercase")
    val minLowercase: Int? = null,

    @SerialName("special")
    val allowSpecial: Boolean,

    @SerialName("minSpecial")
    val minSpecial: Int,

    // Passphrase-specific options

    @SerialName("numWords")
    val numWords: Int,

    @SerialName("wordSeparator")
    val wordSeparator: String,

    @SerialName("capitalize")
    val allowCapitalize: Boolean,

    @SerialName("includeNumber")
    val allowIncludeNumber: Boolean,
) {
    /**
     * Represents different Passcode types.
     */
    @Serializable(with = PasscodeTypeSerializer::class)
    enum class PasscodeType {
        @SerialName("0")
        PASSWORD,

        @SerialName("1")
        PASSPHRASE,
    }
}

@Keep
private class PasscodeTypeSerializer :
    BaseEnumeratedIntSerializer<PasscodeGenerationOptions.PasscodeType>(
        PasscodeGenerationOptions.PasscodeType.entries.toTypedArray(),
    )
