package com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk

import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.dao.ItemDao
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.x8bit.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

class AuthenticatorDiskSourceImpl @Inject constructor(
    private val itemDao: ItemDao,
) : AuthenticatorDiskSource {

    private val forceItemsFlow = bufferedMutableSharedFlow<List<AuthenticatorItemEntity>>()

    override suspend fun saveItem(authenticatorItem: AuthenticatorItemEntity) {
        itemDao.insert(authenticatorItem)
    }

    override fun getItems(): Flow<List<AuthenticatorItemEntity>> = merge(
        forceItemsFlow,
        itemDao.getAllItems()
    )

    override suspend fun deleteItem(itemId: String) {
        itemDao.deleteItem(itemId)
    }
}
