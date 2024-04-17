package com.bitwarden.authenticator.data.authenticator.datasource.disk.convertor

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm

/**
 * A [TypeConverter] to convert [AuthenticatorItemAlgorithm] to and from a [String].
 */
@ProvidedTypeConverter
class AuthenticatorItemAlgorithmConverter {

    /**
     * A [TypeConverter] to convert an [AuthenticatorItemAlgorithm] to a [String].
     */
    @TypeConverter
    fun toString(item: AuthenticatorItemAlgorithm): String = item.name

    /**
     * A [TypeConverter] to convert a [String] to an [AuthenticatorItemAlgorithm].
     */
    @TypeConverter
    fun fromString(itemName: String) = AuthenticatorItemAlgorithm
        .entries
        .find { it.name == itemName }
}
