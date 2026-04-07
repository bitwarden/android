package com.bitwarden.ui.platform.util

private const val BASE_DATA_SIZE: Long = 1024L
private val DATA_SIZE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB")

/**
 * Formats the a long, representing size in bytes, into a human readable string.
 *
 * Note: This uses base-2 to determine the size of the file but uses base-10 units.
 */
fun Long.formatBytes(): String {
    if (this < BASE_DATA_SIZE) return "$this ${DATA_SIZE_UNITS[0]}"

    var value = this.toDouble()
    var unitIndex = 0
    while (value >= BASE_DATA_SIZE && unitIndex < DATA_SIZE_UNITS.lastIndex) {
        value /= BASE_DATA_SIZE
        unitIndex++
    }

    return String.format(locale = null, format = "%.2f ${DATA_SIZE_UNITS[unitIndex]}", value)
}
