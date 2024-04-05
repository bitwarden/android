package com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity

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

    @ColumnInfo(name = "type")
    val type: AuthenticatorItemType = AuthenticatorItemType.TOTP,

    @ColumnInfo(name = "algorithm")
    val algorithm: AuthenticatorItemAlgorithm = AuthenticatorItemAlgorithm.SHA1,

    @ColumnInfo(name = "period")
    val period: Int = 30,

    @ColumnInfo(name = "digits")
    val digits: Int = 6,

    @ColumnInfo(name = "key")
    val key: String,

    @ColumnInfo(name = "issuer")
    val issuer: String?,

    @ColumnInfo(name = "userId")
    val userId: String?,

    @ColumnInfo(name = "username")
    val username: String?,
)
