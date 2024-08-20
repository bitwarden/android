package com.x8bit.bitwarden.data.vault.repository.util

import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSend
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSendJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkSend
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultSdkSendExtensionsTest {

    @Suppress("MaxLineLength")
    @Test
    fun `toEncryptedNetworkSend should convert a SDK-based Send to network-based Send with file length`() {
        val sdkSend = createMockSdkSend(number = 1)
        val networkSend = sdkSend.toEncryptedNetworkSend(fileLength = 1)
        assertEquals(createMockSendJsonRequest(number = 1), networkSend)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toEncryptedNetworkSend should convert a SDK-based Send to network-based Send without file length`() {
        val sdkSend = createMockSdkSend(number = 1)
        val networkSend = sdkSend.toEncryptedNetworkSend(fileLength = null)
        assertEquals(createMockSendJsonRequest(number = 1).copy(fileLength = null), networkSend)
    }

    @Test
    fun `toEncryptedSdkSend should convert a network-based Send to SDK-based Send`() {
        val syncSend = createMockSend(number = 1)
        val sdkSend = syncSend.toEncryptedSdkSend()
        assertEquals(
            createMockSdkSend(number = 1),
            sdkSend,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toEncryptedSdkSendList should convert list of network-based Send to List of SDK-based Send`() {
        val syncSends = listOf(
            createMockSend(number = 1),
            createMockSend(number = 2),
        )
        val sdkSends = syncSends.toEncryptedSdkSendList()
        assertEquals(
            listOf(
                createMockSdkSend(number = 1),
                createMockSdkSend(number = 2),
            ),
            sdkSends,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toSortAlphabetically should sort SendView by name`() {
        val list = listOf(
            createMockSendView(1).copy(name = "c"),
            createMockSendView(1).copy(name = "B"),
            createMockSendView(1).copy(name = "z"),
            createMockSendView(1).copy(name = "4"),
            createMockSendView(1).copy(name = "A"),
            createMockSendView(1).copy(name = "#"),
            createMockSendView(1).copy(name = "D"),
        )

        val expected = listOf(
            createMockSendView(1).copy(name = "#"),
            createMockSendView(1).copy(name = "4"),
            createMockSendView(1).copy(name = "A"),
            createMockSendView(1).copy(name = "B"),
            createMockSendView(1).copy(name = "c"),
            createMockSendView(1).copy(name = "D"),
            createMockSendView(1).copy(name = "z"),
        )

        assertEquals(
            expected,
            list.sortAlphabetically(),
        )
    }
}
