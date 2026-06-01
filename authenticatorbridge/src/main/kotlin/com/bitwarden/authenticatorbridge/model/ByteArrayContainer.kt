package com.bitwarden.authenticatorbridge.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Wraps a [ByteArray] and implements [equals], [hashCode], and [Parcelable] so that it can more
 * easily be included in [Parcelable] models.
 *
 * @param byteArray Wrapped byte array
 */
@Parcelize
data class ByteArrayContainer(
    val byteArray: ByteArray,
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ByteArrayContainer

        return byteArray.contentEquals(other.byteArray)
    }

    override fun hashCode(): Int {
        return byteArray.contentHashCode()
    }
}

/**
 * Helper function for converting [ByteArray] to [ByteArrayContainer].
 */
fun ByteArray.toByteArrayContainer(): ByteArrayContainer =
    ByteArrayContainer(this)
