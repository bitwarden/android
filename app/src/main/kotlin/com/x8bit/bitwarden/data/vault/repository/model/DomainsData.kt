package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Model for equivalent domain details.
 *
 * @param equivalentDomains A list of equivalent domains to compare URIs to.
 * @param globalEquivalentDomains A list of global equivalent domains to compare URIs to.
 */
data class DomainsData(
    val equivalentDomains: List<List<String>>,
    val globalEquivalentDomains: List<GlobalEquivalentDomain>,
) {
    /**
     * Model for a group of domains that should be matched together.
     *
     * @property isExcluded If the global equivalent domain should be excluded.
     * @property domains A list of domains that should all match a URI.
     * @property type The domain type identifier.
     */
    data class GlobalEquivalentDomain(
        val isExcluded: Boolean,
        val domains: List<String>,
        val type: Int,
    )
}
