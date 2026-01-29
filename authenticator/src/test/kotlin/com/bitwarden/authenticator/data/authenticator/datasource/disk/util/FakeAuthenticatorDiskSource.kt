package com.bitwarden.authenticator.data.authenticator.datasource.disk.util

import com.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription

class FakeAuthenticatorDiskSource : AuthenticatorDiskSource {
    private val mutableItemFlow = bufferedMutableSharedFlow<List<AuthenticatorItemEntity>>()
    private val storedItems = mutableListOf<AuthenticatorItemEntity>()

    override suspend fun saveItem(vararg authenticatorItem: AuthenticatorItemEntity) {
        storedItems.addAll(authenticatorItem)
        mutableItemFlow.emit(storedItems)
    }

    override fun getItems(): Flow<List<AuthenticatorItemEntity>> = mutableItemFlow
        .onSubscription { emit(storedItems) }

    override suspend fun deleteItem(itemId: String) {
        storedItems.removeIf { it.id == itemId }
        mutableItemFlow.emit(storedItems)
    }
}
