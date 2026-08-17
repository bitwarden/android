package com.x8bit.bitwarden.ui.tools.feature.send.addedit.util

import com.bitwarden.send.SendType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendState
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.model.AuthEmail
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.model.SendAuth
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class SendViewCopyExtensionsTest {

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
                common = EXPECTED_COMMON,
                selectedType = AddEditSendState.ViewState.Content.SendType.Text(
                    input = "mockText-1",
                    isHideByDefaultChecked = false,
                ),
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

        val common = result.common
        // The copy is a brand new Send, so none of these follow it over.
        assertNull(common.originalSendView)
        assertNull(common.sendUrl)
        assertNull(common.currentAccessCount)
        assertNull(common.expirationDate)
        assertEquals("", common.passwordInput)
        assertFalse(common.hasPassword)
        assertFalse(common.isDeactivateChecked)
    }

    @Test
    fun `toCopyViewState should use the deletion date it is given`() {
        val sendView = createMockSendView(number = 1, type = SendType.TEXT)

        val result = sendView.toCopyViewState(
            deletionDate = COPY_DELETION_DATE,
            isHideEmailAddressEnabled = true,
            sendAuth = SendAuth.None,
        )

        assertEquals(COPY_DELETION_DATE, result.common.deletionDate)
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

        assertEquals(sendAuth, result.common.sendAuth)
    }

    @Test
    fun `toCopyViewState should not hide the email when the policy disallows it`() {
        val sendView = createMockSendView(number = 1, type = SendType.TEXT, hideEmail = true)

        val result = sendView.toCopyViewState(
            deletionDate = COPY_DELETION_DATE,
            isHideEmailAddressEnabled = false,
            sendAuth = SendAuth.None,
        )

        assertFalse(result.common.isHideEmailChecked)
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
            AddEditSendState.ViewState.Content.SendType.File(
                uri = null,
                name = null,
                displaySize = null,
                sizeBytes = null,
            ),
            result.selectedType,
        )
    }
}

private val COPY_DELETION_DATE: Instant = Instant.parse("2023-11-03T12:00:00Z")

private val EXPECTED_COMMON: AddEditSendState.ViewState.Content.Common =
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
