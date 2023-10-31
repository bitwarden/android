package com.x8bit.bitwarden.data.vault.datasource.network.model

/**
 * Create a mock [SyncResponseJson.Domains] with a given [number].
 */
fun createMockDomains(number: Int): SyncResponseJson.Domains =
    SyncResponseJson.Domains(
        globalEquivalentDomains = listOf(
            SyncResponseJson.Domains.GlobalEquivalentDomain(
                isExcluded = false,
                domains = listOf(
                    "mockDomain-$number",
                ),
                type = 1,
            ),
        ),
        equivalentDomains = listOf(
            listOf(
                "mockEquivalentDomain-$number",
            ),
        ),
    )
