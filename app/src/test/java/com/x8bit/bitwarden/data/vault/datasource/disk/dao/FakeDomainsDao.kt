package com.x8bit.bitwarden.data.vault.datasource.disk.dao

import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.DomainsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

class FakeDomainsDao : DomainsDao {
    var storedDomains: DomainsEntity? = null

    var deleteDomainsCalled: Boolean = false
    var getDomainsCalled: Boolean = false
    var insertDomainsCalled: Boolean = false

    private val mutableDomainsFlow = bufferedMutableSharedFlow<DomainsEntity?>()

    override suspend fun deleteDomains(userId: String) {
        deleteDomainsCalled = true
    }

    override fun getDomains(userId: String): Flow<DomainsEntity> {
        getDomainsCalled = true
        return mutableDomainsFlow.filterNotNull()
    }

    override suspend fun insertDomains(domains: DomainsEntity) {
        insertDomainsCalled = true
        storedDomains = domains
        mutableDomainsFlow.tryEmit(domains)
    }
}
