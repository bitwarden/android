package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.core.V2UpgradeToken
import com.bitwarden.network.model.V2UpgradeTokenJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class V2UpgradeTokenExtensionsTest {
    @Test
    fun `toV2UpgradeTokenJson maps all fields to a V2UpgradeTokenJson`() {
        val token = V2UpgradeToken(
            wrappedUserKey1 = "wrappedUserKey1",
            wrappedUserKey2 = "wrappedUserKey2",
        )
        val expected = V2UpgradeTokenJson(
            wrappedUserKey1 = "wrappedUserKey1",
            wrappedUserKey2 = "wrappedUserKey2",
        )

        assertEquals(expected, token.toV2UpgradeTokenJson())
    }

    @Test
    fun `toV2UpgradeToken maps all fields to a V2UpgradeToken`() {
        val json = V2UpgradeTokenJson(
            wrappedUserKey1 = "wrappedUserKey1",
            wrappedUserKey2 = "wrappedUserKey2",
        )
        val expected = V2UpgradeToken(
            wrappedUserKey1 = "wrappedUserKey1",
            wrappedUserKey2 = "wrappedUserKey2",
        )

        assertEquals(expected, json.toV2UpgradeToken())
    }
}
