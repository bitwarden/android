package com.x8bit.bitwarden.data.vault.datasource.sdk

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class BitwardenFeatureFlagManagerTest {

    private val bitwardenFeatureFlagManager = BitwardenFeatureFlagManagerImpl()

    @Test
    fun `featureFlags should return set feature flags`() {
        val expected = mapOf("enableCipherKeyEncryption" to true)

        val actual = bitwardenFeatureFlagManager.featureFlags

        assertEquals(expected, actual)
    }
}
