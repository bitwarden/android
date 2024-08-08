package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.ui.platform.components.model.IconRes
import com.x8bit.bitwarden.ui.tools.feature.send.SendState
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendStatusIcon
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

class SendDataExtensionsTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(
            SendView::toLabelIcons,
            SendView::toSendUrl,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            SendView::toLabelIcons,
            SendView::toSendUrl,
        )
    }

    @Test
    fun `toViewState should return Empty when SendData is empty`() {
        val sendData = SendData(emptyList())

        val result = sendData.toViewState(DEFAULT_BASE_URL, fixedClock)

        assertEquals(SendState.ViewState.Empty, result)
    }

    @Test
    fun `toViewState should return Content when SendData is not empty`() {
        val textSendView = createMockSendView(number = 2, type = SendType.TEXT)
        val fileSendView = createMockSendView(number = 1, type = SendType.FILE)
        val list = listOf(
            fileSendView,
            textSendView,
        )
        val textSendViewUrl1 = "www.test.com/#/send/mockAccessId-1/mockKey-1"
        val textSendViewUrl2 = "www.test.com/#/send/mockAccessId-2/mockKey-2"
        val sendData = SendData(list)
        every { textSendView.toSendUrl(DEFAULT_BASE_URL) } returns textSendViewUrl2
        every { fileSendView.toSendUrl(DEFAULT_BASE_URL) } returns textSendViewUrl1
        every { textSendView.toLabelIcons(any()) } returns DEFAULT_SEND_STATUS_ICONS
        every { fileSendView.toLabelIcons(any()) } returns DEFAULT_SEND_STATUS_ICONS

        val result = sendData.toViewState(DEFAULT_BASE_URL, fixedClock)

        assertEquals(
            SendState.ViewState.Content(
                textTypeCount = 1,
                fileTypeCount = 1,
                sendItems = listOf(
                    SendState.ViewState.Content.SendItem(
                        id = "mockId-1",
                        name = "mockName-1",
                        deletionDate = "Oct 27, 2023, 12:00 PM",
                        type = SendState.ViewState.Content.SendItem.Type.FILE,
                        iconList = DEFAULT_SEND_STATUS_ICONS,
                        shareUrl = "www.test.com/#/send/mockAccessId-1/mockKey-1",
                        hasPassword = true,
                    ),
                    SendState.ViewState.Content.SendItem(
                        id = "mockId-2",
                        name = "mockName-2",
                        deletionDate = "Oct 27, 2023, 12:00 PM",
                        type = SendState.ViewState.Content.SendItem.Type.TEXT,
                        iconList = DEFAULT_SEND_STATUS_ICONS,
                        shareUrl = "www.test.com/#/send/mockAccessId-2/mockKey-2",
                        hasPassword = true,
                    ),
                ),
            ),
            result,
        )
    }
}

private const val DEFAULT_BASE_URL: String = "www.test.com/"

private val DEFAULT_SEND_STATUS_ICONS: List<IconRes> = listOf(
    IconRes(
        iconRes = SendStatusIcon.DISABLED.iconRes,
        contentDescription = SendStatusIcon.DISABLED.contentDescription,
    ),
    IconRes(
        iconRes = SendStatusIcon.PASSWORD.iconRes,
        contentDescription = SendStatusIcon.PASSWORD.contentDescription,
    ),
    IconRes(
        iconRes = SendStatusIcon.MAX_ACCESS_COUNT_REACHED.iconRes,
        contentDescription = SendStatusIcon.MAX_ACCESS_COUNT_REACHED.contentDescription,
    ),
    IconRes(
        iconRes = SendStatusIcon.EXPIRED.iconRes,
        contentDescription = SendStatusIcon.EXPIRED.contentDescription,
    ),
    IconRes(
        iconRes = SendStatusIcon.PENDING_DELETE.iconRes,
        contentDescription = SendStatusIcon.PENDING_DELETE.contentDescription,
    ),
)
