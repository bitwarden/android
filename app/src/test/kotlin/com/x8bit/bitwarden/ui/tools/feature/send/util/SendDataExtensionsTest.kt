package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.ui.tools.feature.send.SendState
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendStatusIcon
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
            SendView::toOverflowActions,
            SendView::toSendUrl,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            SendView::toLabelIcons,
            SendView::toOverflowActions,
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
        every {
            textSendView.toOverflowActions(DEFAULT_BASE_URL)
        } returns TEXT_SEND_OVERFLOW_ACTIONS
        every {
            fileSendView.toOverflowActions(DEFAULT_BASE_URL)
        } returns FILE_SEND_OVERFLOW_ACTIONS

        val result = sendData.toViewState(DEFAULT_BASE_URL, fixedClock)

        assertEquals(
            SendState.ViewState.Content(
                textTypeCount = 1,
                fileTypeCount = 1,
                sendItems = listOf(
                    SendState.ViewState.Content.SendItem(
                        id = "mockId-1",
                        name = "mockName-1",
                        deletionDate = "Oct 27, 2023, 12:00\u202FPM",
                        type = SendState.ViewState.Content.SendItem.Type.FILE,
                        iconList = DEFAULT_SEND_STATUS_ICONS,
                        shareUrl = "www.test.com/#/send/mockAccessId-1/mockKey-1",
                        hasPassword = true,
                        overflowItems = FILE_SEND_OVERFLOW_ACTIONS,
                    ),
                    SendState.ViewState.Content.SendItem(
                        id = "mockId-2",
                        name = "mockName-2",
                        deletionDate = "Oct 27, 2023, 12:00\u202FPM",
                        type = SendState.ViewState.Content.SendItem.Type.TEXT,
                        iconList = DEFAULT_SEND_STATUS_ICONS,
                        shareUrl = "www.test.com/#/send/mockAccessId-2/mockKey-2",
                        hasPassword = true,
                        overflowItems = TEXT_SEND_OVERFLOW_ACTIONS,
                    ),
                ),
            ),
            result,
        )
    }
}

private const val DEFAULT_BASE_URL: String = "www.test.com/"

private val FILE_SEND_OVERFLOW_ACTIONS: ImmutableList<ListingItemOverflowAction.SendAction> =
    persistentListOf(
        ListingItemOverflowAction.SendAction.ViewClick(
            sendId = "mockId-1",
            sendType = SendType.FILE,
        ),
        ListingItemOverflowAction.SendAction.DeleteClick(sendId = "mockId-1"),
    )

private val TEXT_SEND_OVERFLOW_ACTIONS: ImmutableList<ListingItemOverflowAction.SendAction> =
    persistentListOf(
        ListingItemOverflowAction.SendAction.ViewClick(
            sendId = "mockId-2",
            sendType = SendType.TEXT,
        ),
        ListingItemOverflowAction.SendAction.DeleteClick(sendId = "mockId-2"),
    )

private val DEFAULT_SEND_STATUS_ICONS: ImmutableList<IconData> = persistentListOf(
    IconData.Local(
        iconRes = SendStatusIcon.DISABLED.iconRes,
        contentDescription = SendStatusIcon.DISABLED.contentDescription,
    ),
    IconData.Local(
        iconRes = SendStatusIcon.PASSWORD.iconRes,
        contentDescription = SendStatusIcon.PASSWORD.contentDescription,
    ),
    IconData.Local(
        iconRes = SendStatusIcon.MAX_ACCESS_COUNT_REACHED.iconRes,
        contentDescription = SendStatusIcon.MAX_ACCESS_COUNT_REACHED.contentDescription,
    ),
    IconData.Local(
        iconRes = SendStatusIcon.EXPIRED.iconRes,
        contentDescription = SendStatusIcon.EXPIRED.contentDescription,
    ),
    IconData.Local(
        iconRes = SendStatusIcon.PENDING_DELETE.iconRes,
        contentDescription = SendStatusIcon.PENDING_DELETE.contentDescription,
    ),
)
