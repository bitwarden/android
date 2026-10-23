package com.bitwarden.ui.platform.navigation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/**
 * Remembers the standard [NavEntryDecorator]s used by every navigation display in the app.
 *
 * These ensure that each [NavEntry] has its own saveable state as well as its own `ViewModelStore`,
 * so that ViewModels (and their `SavedStateHandle`s) are scoped to the entry and cleared when the
 * entry is popped off of the back stack.
 */
@Composable
fun <T : Any> rememberBitwardenNavEntryDecorators(): List<NavEntryDecorator<T>> {
    val saveableStateHolderDecorator = rememberSaveableStateHolderNavEntryDecorator<T>()
    val viewModelStoreDecorator = rememberViewModelStoreNavEntryDecorator<T>()
    return remember(key1 = saveableStateHolderDecorator, key2 = viewModelStoreDecorator) {
        listOf(saveableStateHolderDecorator, viewModelStoreDecorator)
    }
}
