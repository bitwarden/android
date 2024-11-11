package com.x8bit.bitwarden.data.vault.repository.util

import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson.Domains
import com.x8bit.bitwarden.data.vault.repository.model.DomainsData

/**
 * Map the API [Domains] model to the internal [DomainsData] model.
 */
fun Domains?.toDomainsData(): DomainsData {
    val globalEquivalentDomains = this
        ?.globalEquivalentDomains
        ?.map { it.toInternalModel() }
        .orEmpty()

    return DomainsData(
        equivalentDomains = this?.equivalentDomains.orEmpty(),
        globalEquivalentDomains = globalEquivalentDomains,
    )
}

/**
 * Map the API [Domains.GlobalEquivalentDomain] model to the internal
 * [DomainsData.GlobalEquivalentDomain] model.
 */
private fun Domains.GlobalEquivalentDomain.toInternalModel(): DomainsData.GlobalEquivalentDomain =
    DomainsData.GlobalEquivalentDomain(
        domains = this.domains.orEmpty(),
        isExcluded = this.isExcluded,
        type = this.type,
    )
