package com.x8bit.bitwarden.ui.platform.base.util

/**
 * Returns null if all entries in a given list are equal to a provided [value], otherwise
 * the original list is returned.
 */
fun <T> List<T>.nullIfAllEqual(value: T): List<T>? =
    if (all { it == value }) {
        null
    } else {
        this
    }
