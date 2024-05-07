package com.bitwarden.authenticator.data.authenticator.datasource.disk

import com.bitwarden.authenticator.data.authenticator.datasource.disk.dao.ItemDao
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

class AuthenticatorDiskSourceImpl @Inject constructor(
    private val itemDao: ItemDao,
) : AuthenticatorDiskSource {

    private val forceItemsFlow = bufferedMutableSharedFlow<List<AuthenticatorItemEntity>>()

    override suspend fun saveItem(vararg authenticatorItem: AuthenticatorItemEntity) {
        itemDao.insert(*authenticatorItem)
    }

    override fun getItems(): Flow<List<AuthenticatorItemEntity>> = merge(
        forceItemsFlow,
        itemDao.getAllItems()
    )

    override suspend fun deleteItem(itemId: String) {
        itemDao.deleteItem(itemId)
    }
}
