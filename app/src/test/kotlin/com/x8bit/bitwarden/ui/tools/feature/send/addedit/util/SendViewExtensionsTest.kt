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

    @Test
    fun `toCopyViewState should carry over the copyable fields of a text send`() {
        val sendView = createMockSendView(number = 1, type = SendType.TEXT)

        val result = sendView.toCopyViewState(
            deletionDate = COPY_DELETION_DATE,
            isHideEmailAddressEnabled = true,
            sendAuth = SendAuth.None,
        )

        assertEquals(
            AddEditSendState.ViewState.Content(
                common = COPY_COMMON,
                selectedType = DEFAULT_TEXT_TYPE,
            ),
            result,
        )
    }

    @Test
    fun `toCopyViewState should not carry over anything belonging to the original send`() {
        val sendView = createMockSendView(
            number = 1,
            type = SendType.TEXT,
            disabled = true,
            hasPassword = true,
        )

        val result = sendView.toCopyViewState(
            deletionDate = COPY_DELETION_DATE,
            isHideEmailAddressEnabled = true,
            sendAuth = SendAuth.None,
        )

        // The copy is a brand new Send, so the original's URL, access count, expiration date,
        // password and deactivated state are all left behind.
        assertEquals(
            AddEditSendState.ViewState.Content(
                common = COPY_COMMON,
                selectedType = DEFAULT_TEXT_TYPE,
            ),
            result,
        )
    }

    @Test
    fun `toCopyViewState should use the deletion date it is given`() {
        val sendView = createMockSendView(number = 1, type = SendType.TEXT)

        val result = sendView.toCopyViewState(
            deletionDate = OTHER_DELETION_DATE,
            isHideEmailAddressEnabled = true,
            sendAuth = SendAuth.None,
        )

        assertEquals(
            AddEditSendState.ViewState.Content(
                common = COPY_COMMON.copy(deletionDate = OTHER_DELETION_DATE),
                selectedType = DEFAULT_TEXT_TYPE,
            ),
            result,
        )
    }

    @Test
    fun `toCopyViewState should use the access type it is given`() {
        val sendView = createMockSendView(number = 1, type = SendType.TEXT)
        val sendAuth = SendAuth.Email(
            emails = persistentListOf(AuthEmail(id = "id", value = "test@example.com")),
        )

        val result = sendView.toCopyViewState(
            deletionDate = COPY_DELETION_DATE,
            isHideEmailAddressEnabled = true,
            sendAuth = sendAuth,
        )

        assertEquals(
            AddEditSendState.ViewState.Content(
                common = COPY_COMMON.copy(sendAuth = sendAuth),
                selectedType = DEFAULT_TEXT_TYPE,
            ),
            result,
        )
    }

    @Test
    fun `toCopyViewState should not hide the email when the policy disallows it`() {
        val sendView = createMockSendView(number = 1, type = SendType.TEXT, hideEmail = true)

        val result = sendView.toCopyViewState(
            deletionDate = COPY_DELETION_DATE,
            isHideEmailAddressEnabled = false,
            sendAuth = SendAuth.None,
        )

        assertEquals(
            AddEditSendState.ViewState.Content(
                common = COPY_COMMON.copy(isHideEmailAddressEnabled = false),
                selectedType = DEFAULT_TEXT_TYPE,
            ),
            result,
        )
    }

    @Test
    fun `toCopyViewState should drop the attachment of a file send`() {
        val sendView = createMockSendView(number = 1, type = SendType.FILE)

        val result = sendView.toCopyViewState(
            deletionDate = COPY_DELETION_DATE,
            isHideEmailAddressEnabled = true,
            sendAuth = SendAuth.None,
        )

        assertEquals(
            AddEditSendState.ViewState.Content(
                common = COPY_COMMON,
                selectedType = AddEditSendState.ViewState.Content.SendType.File(
                    uri = null,
                    name = null,
                    displaySize = null,
                    sizeBytes = null,
                ),
            ),
            result,
        )
    }
}

private val COPY_DELETION_DATE: Instant = Instant.parse("2023-11-03T12:00:00Z")

private val OTHER_DELETION_DATE: Instant = Instant.parse("2023-12-25T12:00:00Z")

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

private val COPY_COMMON: AddEditSendState.ViewState.Content.Common =
    AddEditSendState.ViewState.Content.Common(
        originalSendView = null,
        name = "mockName-1",
        currentAccessCount = null,
        maxAccessCount = 1,
        passwordInput = "",
        noteInput = "mockNotes-1",
        isHideEmailChecked = false,
        isDeactivateChecked = false,
        deletionDate = COPY_DELETION_DATE,
        expirationDate = null,
        sendUrl = null,
        hasPassword = false,
        isHideEmailAddressEnabled = true,
        sendAuth = SendAuth.None,
    )
