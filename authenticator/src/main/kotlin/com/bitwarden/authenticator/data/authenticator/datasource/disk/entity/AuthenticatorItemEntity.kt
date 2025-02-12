package com.bitwarden.authenticator.data.authenticator.datasource.disk.entity

import android.net.Uri
import androidx.core.text.htmlEncode
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an authenticator item in the database.
 */
@Entity(tableName = "items")
data class AuthenticatorItemEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "key")
    val key: String,

    @ColumnInfo(name = "type")
    val type: AuthenticatorItemType = AuthenticatorItemType.TOTP,

    @ColumnInfo(name = "algorithm")
    val algorithm: AuthenticatorItemAlgorithm = AuthenticatorItemAlgorithm.SHA1,

    @ColumnInfo(name = "period")
    val period: Int = 30,

    @ColumnInfo(name = "digits")
    val digits: Int = 6,

    @ColumnInfo(name = "issuer")
    val issuer: String,

    @ColumnInfo(name = "userId")
    val userId: String? = null,

    @ColumnInfo(name = "accountName")
    val accountName: String? = null,

    @ColumnInfo(name = "favorite", defaultValue = "0")
    val favorite: Boolean,
) {
    /**
     * Returns the OTP data in a string formatted to match the Google Authenticator specification,
     * https://github.com/google/google-authenticator/wiki/Key-Uri-Format
     */
    fun toOtpAuthUriString(): String {
        return when (type) {
            AuthenticatorItemType.TOTP -> {
                val label = if (accountName.isNullOrBlank()) {
                    issuer
                } else {
                    "$issuer:$accountName"
                }
                Uri.Builder()
                    .scheme("otpauth")
                    .authority("totp")
                    .appendPath(label.htmlEncode())
                    .appendQueryParameter("secret", key)
                    .appendQueryParameter("algorithm", algorithm.name)
                    .appendQueryParameter("digits", digits.toString())
                    .appendQueryParameter("period", period.toString())
                    .appendQueryParameter("issuer", issuer)
                    .build()
                    .toString()
            }

            AuthenticatorItemType.STEAM -> {
                if (key.startsWith("steam://")) {
                    key
                } else {
                    "steam://$key"
                }
            }
        }
    }
}
