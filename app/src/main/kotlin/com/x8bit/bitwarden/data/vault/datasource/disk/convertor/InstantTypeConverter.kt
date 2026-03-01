package com.x8bit.bitwarden.data.vault.datasource.disk.convertor

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import java.time.Instant

/**
 * A [TypeConverter] to convert an [Instant] to and from a [Long].
 */
@ProvidedTypeConverter
class InstantTypeConverter {
    /**
     * A [TypeConverter] to convert a [Long] to an [Instant].
     */
    @TypeConverter
    fun fromTimestamp(
        value: Long?,
    ): Instant? = value?.let { Instant.ofEpochSecond(it) }

    /**
     * A [TypeConverter] to convert an [Instant] to a [Long].
     */
    @TypeConverter
    fun toTimestamp(
        instant: Instant?,
    ): Long? = instant?.epochSecond
}
