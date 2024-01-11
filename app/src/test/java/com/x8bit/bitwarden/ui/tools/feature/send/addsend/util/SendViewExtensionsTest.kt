package com.x8bit.bitwarden.ui.tools.feature.send.addsend.util

import com.bitwarden.core.SendType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.AddSendState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class SendViewExtensionsTest {

    @Test
    fun `toViewState should create an appropriate ViewState for file type`() {
        val sendView = createMockSendView(number = 1, type = SendType.FILE)

        val result = sendView.toViewState(
            clock = FIXED_CLOCK,
            baseWebSendUrl = "www.test.com/",
        )

        assertEquals(DEFAULT_STATE, result)
    }

    @Test
    fun `toViewState should create an appropriate ViewState for text type`() {
        val sendView = createMockSendView(number = 1, type = SendType.TEXT)

        val result = sendView.toViewState(
            clock = FIXED_CLOCK,
            baseWebSendUrl = "www.test.com/",
        )

        assertEquals(DEFAULT_STATE.copy(selectedType = DEFAULT_TEXT_TYPE), result)
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)

private val DEFAULT_COMMON: AddSendState.ViewState.Content.Common =
    AddSendState.ViewState.Content.Common(
        name = "mockName-1",
        currentAccessCount = 1,
        maxAccessCount = 1,
        passwordInput = "",
        noteInput = "mockNotes-1",
        isHideEmailChecked = false,
        isDeactivateChecked = false,
        deletionDate = ZonedDateTime.ofInstant(
            Instant.parse("2023-10-27T12:00:00Z"),
            ZoneOffset.UTC,
        ),
        expirationDate = ZonedDateTime.ofInstant(
            Instant.parse("2023-10-27T12:00:00Z"),
            ZoneOffset.UTC,
        ),
        sendUrl = "www.test.com/mockAccessId-1/mockKey-1",
    )

private val DEFAULT_TEXT_TYPE: AddSendState.ViewState.Content.SendType.Text =
    AddSendState.ViewState.Content.SendType.Text(
        input = "mockText-1",
        isHideByDefaultChecked = false,
    )

private val DEFAULT_FILE_TYPE: AddSendState.ViewState.Content.SendType.File =
    AddSendState.ViewState.Content.SendType.File

private val DEFAULT_STATE: AddSendState.ViewState.Content =
    AddSendState.ViewState.Content(
        common = DEFAULT_COMMON,
        selectedType = DEFAULT_FILE_TYPE,
    )
