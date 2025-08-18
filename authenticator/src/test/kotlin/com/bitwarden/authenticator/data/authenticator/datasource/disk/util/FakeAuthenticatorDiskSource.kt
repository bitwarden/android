package com.bitwarden.authenticator.data.authenticator.datasource.disk.util

import com.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeAuthenticatorDiskSource : AuthenticatorDiskSource {
    private val mutableItemFlow = MutableSharedFlow<List<AuthenticatorItemEntity>>()
    private val storedItems = mutableListOf<AuthenticatorItemEntity>()

    override suspend fun saveItem(vararg authenticatorItem: AuthenticatorItemEntity) {
        storedItems.addAll(authenticatorItem)
        mutableItemFlow.emit(storedItems)
    }

    override fun getItems(): Flow<List<AuthenticatorItemEntity>> = mutableItemFlow

    override suspend fun deleteItem(itemId: String) {
        storedItems.removeIf { it.id == itemId }
        mutableItemFlow.emit(storedItems)
    }
}
