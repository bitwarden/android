package com.bitwarden.network.model

/**
 * Create a mock [SyncResponseJson.Domains] with a given [number].
 */
fun createMockDomains(
    number: Int,
    globalEquivalentDomains: List<SyncResponseJson.Domains.GlobalEquivalentDomain> = listOf(
        createMockGlobalEquivalentDomain(number = number),
    ),
    equivalentDomains: List<List<String>>? = listOf(listOf("mockEquivalentDomain-$number")),
): SyncResponseJson.Domains =
    SyncResponseJson.Domains(
        globalEquivalentDomains = globalEquivalentDomains,
        equivalentDomains = equivalentDomains,
    )

/**
 * Create a mock [SyncResponseJson.Domains.GlobalEquivalentDomain] with a given [number].
 */
fun createMockGlobalEquivalentDomain(
    number: Int,
    isExcluded: Boolean = false,
    domains: List<String>? = listOf("mockDomain-$number"),
    type: Int = 1,
): SyncResponseJson.Domains.GlobalEquivalentDomain =
    SyncResponseJson.Domains.GlobalEquivalentDomain(
        isExcluded = isExcluded,
        domains = domains,
        type = type,
    )
