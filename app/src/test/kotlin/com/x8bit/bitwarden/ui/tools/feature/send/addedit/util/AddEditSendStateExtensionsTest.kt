package com.x8bit.bitwarden.ui.tools.feature.send.addedit.util

import com.bitwarden.send.SendType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFileView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class AddEditSendStateExtensionsTest {

    @Test
    fun `toSendView should create an appropriate SendView with file type`() {
        val sendView = createMockSendView(number = 1, type = SendType.FILE).copy(
            id = null,
            accessId = null,
            key = null,
            accessCount = 0U,
            text = null,
            file = createMockFileView(number = 1).copy(
                id = null,
                size = null,
                sizeName = null,
            ),
            hasPassword = false,
        )

        val result = DEFAULT_VIEW_STATE
            .copy(
                selectedType = AddEditSendState.ViewState.Content.SendType.File(
                    name = "mockFileName-1",
                    displaySize = "mockSizeName-1",
                    sizeBytes = 1,
                    uri = null,
                ),
            )
            .toSendView(FIXED_CLOCK)

        assertEquals(sendView, result)
    }

    @Test
    fun `toSendView should create an appropriate SendView with text type`() {
        val sendView = createMockSendView(number = 1, type = SendType.TEXT).copy(
            id = null,
            accessId = null,
            key = null,
            accessCount = 0U,
            file = null,
            hasPassword = false,
        )

        val result = DEFAULT_VIEW_STATE.toSendView(FIXED_CLOCK)

        assertEquals(sendView, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toSendView should create an appropriate SendView with expiration date set to deletion date`() {
        val sendView = createMockSendView(number = 1, type = SendType.TEXT).copy(
            id = null,
            accessId = null,
            key = null,
            accessCount = 0U,
            hasPassword = false,
            deletionDate = ZonedDateTime.parse("2030-10-27T12:00:00Z").toInstant(),
            expirationDate = ZonedDateTime.parse("2030-10-27T12:00:00Z").toInstant(),
        )

        val result = DEFAULT_VIEW_STATE
            .copy(
                common = DEFAULT_COMMON_STATE.copy(
                    deletionDate = ZonedDateTime.parse("2030-10-27T12:00:00Z"),
                    expirationDate = ZonedDateTime.parse("2026-10-27T12:00:00Z"),
                ),
            )
            .toSendView(FIXED_CLOCK)

        assertEquals(sendView, result)
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)

private val DEFAULT_COMMON_STATE = AddEditSendState.ViewState.Content.Common(
    name = "mockName-1",
    currentAccessCount = 1,
    maxAccessCount = 1,
    passwordInput = "mockPassword-1",
    noteInput = "mockNotes-1",
    isHideEmailChecked = false,
    isDeactivateChecked = false,
    deletionDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
    expirationDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
    sendUrl = null,
    hasPassword = false,
    isHideEmailAddressEnabled = true,
)

private val DEFAULT_SELECTED_TYPE_STATE = AddEditSendState.ViewState.Content.SendType.Text(
    input = "mockText-1",
    isHideByDefaultChecked = false,
)

private val DEFAULT_VIEW_STATE = AddEditSendState.ViewState.Content(
    common = DEFAULT_COMMON_STATE,
    selectedType = DEFAULT_SELECTED_TYPE_STATE,
)
