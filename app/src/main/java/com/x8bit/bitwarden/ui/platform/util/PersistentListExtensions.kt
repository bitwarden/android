package com.x8bit.bitwarden.ui.platform.util

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/**
 * Creates an immutable [PersistentList] of the given [elements] excluding the null ones.
 */
fun <T : Any> persistentListOfNotNull(vararg elements: T?): PersistentList<T> =
    elements.filterNotNull().toPersistentList()
