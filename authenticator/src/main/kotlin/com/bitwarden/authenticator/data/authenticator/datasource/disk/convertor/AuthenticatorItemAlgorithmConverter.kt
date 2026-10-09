package com.bitwarden.authenticator.data.authenticator.datasource.disk.convertor

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm

/**
 * A [ColumnTypeConverter] to convert [AuthenticatorItemAlgorithm] to and from a [String].
 */
@ProvidedColumnTypeConverter
class AuthenticatorItemAlgorithmConverter {

    /**
     * A [ColumnTypeConverter] to convert an [AuthenticatorItemAlgorithm] to a [String].
     */
    @ColumnTypeConverter
    fun toString(item: AuthenticatorItemAlgorithm): String = item.name

    /**
     * A [ColumnTypeConverter] to convert a [String] to an [AuthenticatorItemAlgorithm].
     */
    @ColumnTypeConverter
    fun fromString(itemName: String) = AuthenticatorItemAlgorithm
        .entries
        .find { it.name == itemName }
}
