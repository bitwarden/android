package com.bitwarden.authenticator.data.authenticator.datasource.disk.convertor

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType

/**
 * A [ColumnTypeConverter] to convert [AuthenticatorItemType] to and from a [String].
 */
@ProvidedColumnTypeConverter
class AuthenticatorItemTypeConverter {

    /**
     * A [ColumnTypeConverter] to convert an [AuthenticatorItemType] to a [String].
     */
    @ColumnTypeConverter
    fun toString(item: AuthenticatorItemType): String = item.name

    /**
     * A [ColumnTypeConverter] to convert a [String] to an [AuthenticatorItemType].
     */
    @ColumnTypeConverter
    fun fromString(itemName: String) = AuthenticatorItemType
        .entries
        .find { it.name == itemName }
}
