package com.x8bit.bitwarden.ui.tools.feature.send.viewsend.util

import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.ui.tools.feature.send.util.toSendUrl
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.ViewSendState
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SendViewExtensionsTest {

    @BeforeEach
    fun setup() {
        mockkStatic(SendView::toSendUrl)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SendView::toSendUrl)
    }

    @Test
    fun `toViewSendViewStateContent should create content with file data`() {
        val fileSendView = createMockSendView(number = 1, type = SendType.FILE)
        val baseWebSendUrl = "https://send.bitwarden.com/#"
        val sendUrl = "send_url"
        every { fileSendView.toSendUrl(baseWebSendUrl = baseWebSendUrl) } returns sendUrl

        val result = fileSendView.toViewSendViewStateContent(
            baseWebSendUrl = baseWebSendUrl,
            clock = FIXED_CLOCK,
        )

        assertEquals(
            ViewSendState.ViewState.Content(
                sendType = ViewSendState.ViewState.Content.SendType.FileType(
                    fileName = "mockFileName-1",
                    fileSize = "mockSizeName-1",
                ),
                shareLink = sendUrl,
                sendName = "mockName-1",
                deletionDate = "Oct 27, 2023, 12:00\u202FPM",
                maxAccessCount = 1,
                currentAccessCount = 1,
                notes = "mockNotes-1",
            ),
            result,
        )
    }

    @Test
    fun `toViewSendViewStateContent should create content with text data`() {
        val fileSendView = createMockSendView(number = 2, type = SendType.TEXT)
        val baseWebSendUrl = "https://send.bitwarden.com/#"
        val sendUrl = "send_url"
        every { fileSendView.toSendUrl(baseWebSendUrl = baseWebSendUrl) } returns sendUrl

        val result = fileSendView.toViewSendViewStateContent(
            baseWebSendUrl = baseWebSendUrl,
            clock = FIXED_CLOCK,
        )

        assertEquals(
            ViewSendState.ViewState.Content(
                sendType = ViewSendState.ViewState.Content.SendType.TextType(
                    textToShare = "mockText-2",
                ),
                shareLink = sendUrl,
                sendName = "mockName-2",
                deletionDate = "Oct 27, 2023, 12:00\u202FPM",
                maxAccessCount = 1,
                currentAccessCount = 1,
                notes = "mockNotes-2",
            ),
            result,
        )
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
