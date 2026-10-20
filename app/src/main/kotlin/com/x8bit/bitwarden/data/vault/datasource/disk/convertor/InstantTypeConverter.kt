package com.x8bit.bitwarden.data.vault.datasource.disk.convertor

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import java.time.Instant

/**
 * A [ColumnTypeConverter] to convert an [Instant] to and from a [Long].
 */
@ProvidedColumnTypeConverter
class InstantTypeConverter {
    /**
     * A [ColumnTypeConverter] to convert a [Long] to an [Instant].
     */
    @ColumnTypeConverter
    fun fromTimestamp(
        value: Long?,
    ): Instant? = value?.let { Instant.ofEpochSecond(it) }

    /**
     * A [ColumnTypeConverter] to convert an [Instant] to a [Long].
     */
    @ColumnTypeConverter
    fun toTimestamp(
        instant: Instant?,
    ): Long? = instant?.epochSecond
}
