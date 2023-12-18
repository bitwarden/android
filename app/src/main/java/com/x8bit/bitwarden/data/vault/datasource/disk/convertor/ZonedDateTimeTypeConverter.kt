package com.x8bit.bitwarden.data.vault.datasource.disk.convertor

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * A [TypeConverter] to convert a [ZonedDateTime] to and from a [Long].
 */
@ProvidedTypeConverter
class ZonedDateTimeTypeConverter {
    /**
     * A [TypeConverter] to convert a [Long] to a [ZonedDateTime].
     */
    @TypeConverter
    fun fromTimestamp(
        value: Long?,
    ): ZonedDateTime? = value?.let {
        ZonedDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneOffset.UTC)
    }

    /**
     * A [TypeConverter] to convert a [ZonedDateTime] to a [Long].
     */
    @TypeConverter
    fun toTimestamp(
        localDateTime: ZonedDateTime?,
    ): Long? = localDateTime?.toEpochSecond()
}
