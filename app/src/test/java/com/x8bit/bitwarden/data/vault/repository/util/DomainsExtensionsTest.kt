package com.x8bit.bitwarden.data.vault.repository.util

import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.repository.model.DomainsData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DomainsExtensionsTest {
    @Test
    fun `toDomainsData returns populated DomainsData when domains are non-null`() {
        // Setup
        val equivalentDomains = listOf(listOf(EQUIVALENT_DOMAIN))
        val globalDomains = listOf(GLOBAL_EQUIVALENT_DOMAIN)
        val globalEquivalentDomain = SyncResponseJson.Domains.GlobalEquivalentDomain(
            isExcluded = false,
            domains = globalDomains,
            type = 0,
        )
        val domains = SyncResponseJson.Domains(
            equivalentDomains = equivalentDomains,
            globalEquivalentDomains = listOf(globalEquivalentDomain),
        )
        val expectedGlobalEquivalentDomain = DomainsData.GlobalEquivalentDomain(
            isExcluded = false,
            domains = globalDomains,
            type = 0,
        )
        val expected = DomainsData(
            equivalentDomains = equivalentDomains,
            globalEquivalentDomains = listOf(expectedGlobalEquivalentDomain),
        )

        // Test
        val actual = domains.toDomainsData()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toDomainsData returns empty DomainsData when domains are null`() {
        // Setup
        val globalEquivalentDomain = SyncResponseJson.Domains.GlobalEquivalentDomain(
            isExcluded = false,
            domains = null,
            type = 0,
        )
        val domains = SyncResponseJson.Domains(
            equivalentDomains = null,
            globalEquivalentDomains = listOf(globalEquivalentDomain),
        )
        val expectedGlobalEquivalentDomain = DomainsData.GlobalEquivalentDomain(
            isExcluded = false,
            domains = emptyList(),
            type = 0,
        )
        val expected = DomainsData(
            equivalentDomains = emptyList(),
            globalEquivalentDomains = listOf(expectedGlobalEquivalentDomain),
        )

        // Test
        val actual = domains.toDomainsData()

        // Verify
        assertEquals(expected, actual)
    }
}

private const val EQUIVALENT_DOMAIN: String = "EQUIVALENT_DOMAIN"
private const val GLOBAL_EQUIVALENT_DOMAIN: String = "GLOBAL_EQUIVALENT_DOMAIN"
