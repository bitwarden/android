package com.x8bit.bitwarden.ui.tools.feature.send.addsend.util

import com.bitwarden.core.SendFileView
import com.bitwarden.core.SendType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.AddSendState
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class AddSendStateExtensionsTest {

    private val fixedInstant: Instant = Instant.parse("2023-10-27T12:00:00Z")

    @AfterEach
    fun tearDown() {
        // Some individual tests call mockkStatic so we will make sure this is always undone.
        unmockkStatic(Instant::class)
    }

    @Test
    fun `toSendView should create an appropriate SendView with file type`() {
        val sendView = createMockSendView(number = 1, type = SendType.FILE).copy(
            id = null,
            accessId = null,
            key = "",
            accessCount = 0U,
            expirationDate = null,
            text = null,
            file = SendFileView(
                id = "",
                fileName = "",
                size = "",
                sizeName = "",
            ),
        )
        mockkStatic(Instant::class)
        every { Instant.now() } returns fixedInstant

        val result = DEFAULT_VIEW_STATE
            .copy(selectedType = AddSendState.ViewState.Content.SendType.File)
            .toSendView()

        assertEquals(sendView, result)
    }

    @Test
    fun `toSendView should create an appropriate SendView with text type`() {
        val sendView = createMockSendView(number = 1, type = SendType.TEXT).copy(
            id = null,
            accessId = null,
            key = "",
            accessCount = 0U,
            expirationDate = null,
            file = null,
        )
        mockkStatic(Instant::class)
        every { Instant.now() } returns fixedInstant

        val result = DEFAULT_VIEW_STATE.toSendView()

        assertEquals(sendView, result)
    }
}

private val DEFAULT_COMMON_STATE = AddSendState.ViewState.Content.Common(
    name = "mockName-1",
    maxAccessCount = 1,
    passwordInput = "mockPassword-1",
    noteInput = "mockNotes-1",
    isHideEmailChecked = false,
    isDeactivateChecked = false,
)

private val DEFAULT_SELECTED_TYPE_STATE = AddSendState.ViewState.Content.SendType.Text(
    input = "mockText-1",
    isHideByDefaultChecked = false,
)

private val DEFAULT_VIEW_STATE = AddSendState.ViewState.Content(
    common = DEFAULT_COMMON_STATE,
    selectedType = DEFAULT_SELECTED_TYPE_STATE,
)
