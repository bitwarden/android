package com.x8bit.bitwarden.ui.tools.feature.send.addedit.util

import com.bitwarden.send.SendType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendState
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.model.AuthEmail
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.model.SendAuth
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class SendViewExtensionsTest {

    @BeforeEach
    fun setup() {
        mockkStatic(UUID::randomUUID)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UUID::randomUUID)
    }

    @Test
    fun `toViewState should create an appropriate ViewState for file type`() {
        val sendView = createMockSendView(number = 1, type = SendType.FILE)

        val result = sendView.toViewState(
            baseWebSendUrl = "www.test.com/",
            isHideEmailAddressEnabled = true,
        )

        assertEquals(
            DEFAULT_STATE.copy(
                common = DEFAULT_COMMON.copy(
                    originalSendView = sendView,
                    sendAuth = SendAuth.Password,
                ),
            ),
            result,
        )
    }

    @Test
    fun `toViewState should create an appropriate ViewState for text type`() {
        val sendView = createMockSendView(number = 1, type = SendType.TEXT)

        val result = sendView.toViewState(
            baseWebSendUrl = "www.test.com/",
            isHideEmailAddressEnabled = true,
        )

        assertEquals(
            DEFAULT_STATE.copy(
                common = DEFAULT_COMMON.copy(
                    originalSendView = sendView,
                    sendAuth = SendAuth.Password,
                ),
                selectedType = DEFAULT_TEXT_TYPE,
            ),
            result,
        )
    }

    @Test
    fun `toSendAuth should use a password when the original has one`() {
        val sendView = createMockSendView(number = 1, hasPassword = true)

        assertEquals(SendAuth.Password, sendView.toSendAuth())
    }

    @Test
    fun `toSendAuth should carry over the original recipients`() {
        every { UUID.randomUUID().toString() } returns "uuid"
        val sendView = createMockSendView(number = 1, hasPassword = false)
            .copy(emails = listOf("one@example.com", "two@example.com"))

        assertEquals(
            SendAuth.Email(
                emails = persistentListOf(
                    AuthEmail(id = "uuid", value = "one@example.com"),
                    AuthEmail(id = "uuid", value = "two@example.com"),
                ),
            ),
            sendView.toSendAuth(),
        )
    }

    @Test
    fun `toSendAuth should use link access when the original has neither`() {
        val sendView = createMockSendView(number = 1, hasPassword = false)

        assertEquals(SendAuth.None, sendView.toSendAuth())
    }
}

private val DEFAULT_COMMON: AddEditSendState.ViewState.Content.Common =
    AddEditSendState.ViewState.Content.Common(
        name = "mockName-1",
        currentAccessCount = 1,
        maxAccessCount = 1,
        passwordInput = "",
        noteInput = "mockNotes-1",
        isHideEmailChecked = false,
        isDeactivateChecked = false,
        deletionDate = Instant.parse("2023-10-27T12:00:00Z"),
        expirationDate = Instant.parse("2023-10-27T12:00:00Z"),
        sendUrl = "www.test.com/mockAccessId-1/mockKey-1",
        hasPassword = true,
        isHideEmailAddressEnabled = true,
        sendAuth = SendAuth.None,
    )

private val DEFAULT_TEXT_TYPE: AddEditSendState.ViewState.Content.SendType.Text =
    AddEditSendState.ViewState.Content.SendType.Text(
        input = "mockText-1",
        isHideByDefaultChecked = false,
    )

private val DEFAULT_FILE_TYPE: AddEditSendState.ViewState.Content.SendType.File =
    AddEditSendState.ViewState.Content.SendType.File(
        name = "mockFileName-1",
        displaySize = "mockSizeName-1",
        sizeBytes = null,
        uri = null,
    )

private val DEFAULT_STATE: AddEditSendState.ViewState.Content =
    AddEditSendState.ViewState.Content(
        common = DEFAULT_COMMON,
        selectedType = DEFAULT_FILE_TYPE,
    )
