package com.bitwarden.authenticator.data.authenticator.datasource.disk.convertor

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType

/**
 * A [TypeConverter] to convert [AuthenticatorItemType] to and from a [String].
 */
@ProvidedTypeConverter
class AuthenticatorItemTypeConverter {

    /**
     * A [TypeConverter] to convert an [AuthenticatorItemType] to a [String].
     */
    @TypeConverter
    fun toString(item: AuthenticatorItemType): String = item.name

    /**
     * A [TypeConverter] to convert a [String] to an [AuthenticatorItemType].
     */
    @TypeConverter
    fun fromString(itemName: String) = AuthenticatorItemType
        .entries
        .find { it.name == itemName }
}
