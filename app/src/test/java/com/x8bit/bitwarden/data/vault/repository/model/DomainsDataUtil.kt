package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Create a mock [DomainsData] with a given [number].
 */
fun createMockDomainsData(number: Int): DomainsData =
    DomainsData(
        equivalentDomains = listOf(listOf("mockEquivalentDomains-$number")),
        globalEquivalentDomains = listOf(
            DomainsData.GlobalEquivalentDomain(
                isExcluded = false,
                domains = listOf("domains-$number"),
                type = number,
            ),
        ),
    )
