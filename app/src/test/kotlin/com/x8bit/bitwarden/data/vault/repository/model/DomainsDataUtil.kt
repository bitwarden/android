package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Create a mock [DomainsData] with a given [number].
 */
fun createMockDomainsData(number: Int): DomainsData =
    DomainsData(
        equivalentDomains = listOf(listOf("mockEquivalentDomain-$number")),
        globalEquivalentDomains = listOf(
            DomainsData.GlobalEquivalentDomain(
                isExcluded = false,
                domains = listOf("mockDomain-$number"),
                type = number,
            ),
        ),
    )
