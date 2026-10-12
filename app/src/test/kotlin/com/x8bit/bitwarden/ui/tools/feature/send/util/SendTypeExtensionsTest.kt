package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.bitwarden.network.model.SendTypeJson
import com.bitwarden.send.SendType
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SendTypeExtensionsTest {

    @Test
    fun `toSendItemType should map each SendType to its matching SendItemType`() {
        assertEquals(SendItemType.FILE, SendType.FILE.toSendItemType())
        assertEquals(SendItemType.TEXT, SendType.TEXT.toSendItemType())
    }

    @Test
    fun `toSendItemType should map each SendTypeJson to its matching SendItemType`() {
        assertEquals(SendItemType.FILE, SendTypeJson.FILE.toSendItemType())
        assertEquals(SendItemType.TEXT, SendTypeJson.TEXT.toSendItemType())
    }
}
