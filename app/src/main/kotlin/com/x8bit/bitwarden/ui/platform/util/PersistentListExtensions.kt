package com.x8bit.bitwarden.ui.platform.util

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Creates an immutable [ImmutableList] of the given [elements] excluding the null ones.
 */
fun <T : Any> persistentListOfNotNull(vararg elements: T?): ImmutableList<T> =
    elements.filterNotNull().toImmutableList()
