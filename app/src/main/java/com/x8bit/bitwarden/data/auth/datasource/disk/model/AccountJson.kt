package com.x8bit.bitwarden.data.auth.datasource.disk.model

import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.UserDecryptionOptionsJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the current account information for a given user.
 *
 * @property profile Information about a user's personal profile.
 * @property tokens Information about a user's access tokens.
 * @property settings Information about a user's app settings.
 */
@Serializable
data class AccountJson(
    @SerialName("profile")
    val profile: Profile,

    @SerialName("tokens")
    val tokens: Tokens,

    @SerialName("settings")
    val settings: Settings,
) {

    /**
     * Represents a user's personal profile.
     *
     * @property userId The ID of the user.
     * @property email The user's email address.
     * @property isEmailVerified Whether or not the user's email is verified.
     * @property name The user's name (if applicable).
     * @property stamp The account's security stamp (if applicable).
     * @property organizationId The ID of the associated organization (if applicable).
     * @property hasPremium True if the user has a premium account.
     * @property avatarColorHex Hex color value for a user's avatar in the "#AARRGGBB" format.
     * @property forcePasswordResetReason Describes the reason for a forced password reset.
     * @property kdfType The KDF type.
     * @property kdfIterations The number of iterations when calculating a user's password.
     * @property kdfMemory The amount of memory to use when calculating a password hash (MB).
     * @property kdfParallelism The number of threads to use when calculating a password hash.
     * @property userDecryptionOptions The options available to a user for decryption.
     */
    @Serializable
    data class Profile(
        @SerialName("userId")
        val userId: String,

        @SerialName("email")
        val email: String,

        @SerialName("emailVerified")
        val isEmailVerified: Boolean?,

        @SerialName("name")
        val name: String?,

        @SerialName("stamp")
        val stamp: String?,

        @SerialName("orgIdentifier")
        val organizationId: String?,

        @SerialName("avatarColor")
        val avatarColorHex: String?,

        @SerialName("hasPremiumPersonally")
        val hasPremium: Boolean?,

        @SerialName("forcePasswordResetReason")
        val forcePasswordResetReason: ForcePasswordResetReason?,

        @SerialName("kdfType")
        val kdfType: KdfTypeJson?,

        @SerialName("kdfIterations")
        val kdfIterations: Int?,

        @SerialName("kdfMemory")
        val kdfMemory: Int?,

        @SerialName("kdfParallelism")
        val kdfParallelism: Int?,

        @SerialName("accountDecryptionOptions")
        val userDecryptionOptions: UserDecryptionOptionsJson?,
    )

    /**
     * Container for the user's API tokens.
     *
     * @property accessToken The user's primary access token.
     * @property refreshToken The user's refresh token.
     */
    @Serializable
    data class Tokens(
        @SerialName("accessToken")
        val accessToken: String,

        @SerialName("refreshToken")
        val refreshToken: String,
    )

    /**
     * Container for various user settings.
     *
     * @property environmentUrlData Data concerning the current environment associated with the
     * current user.
     */
    @Serializable
    data class Settings(
        @SerialName("environmentUrls")
        val environmentUrlData: EnvironmentUrlDataJson?,
    )
}
