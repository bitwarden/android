package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.send.SendFileView
import com.bitwarden.send.SendTextView
import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import java.time.ZonedDateTime

/**
 * Create a mock [SendView] with a given [number].
 */
fun createMockSendView(
    number: Int,
    type: SendType = SendType.FILE,
): SendView =
    SendView(
        id = "mockId-$number",
        accessId = "mockAccessId-$number",
        name = "mockName-$number",
        notes = "mockNotes-$number",
        key = "mockKey-$number",
        newPassword = "mockPassword-$number",
        hasPassword = true,
        type = type,
        file = createMockFileView(number = number).takeIf { type == SendType.FILE },
        text = createMockTextView(number = number).takeIf { type == SendType.TEXT },
        maxAccessCount = 1u,
        accessCount = 1u,
        disabled = false,
        hideEmail = false,
        revisionDate = ZonedDateTime.parse("2023-10-27T12:00:00Z").toInstant(),
        deletionDate = ZonedDateTime.parse("2023-10-27T12:00:00Z").toInstant(),
        expirationDate = ZonedDateTime.parse("2023-10-27T12:00:00Z").toInstant(),
    )

/**
 * Create a mock [SendFileView] with a given [number].
 */
fun createMockFileView(number: Int): SendFileView =
    SendFileView(
        fileName = "mockFileName-$number",
        size = "1",
        sizeName = "mockSizeName-$number",
        id = "mockId-$number",
    )

/**
 * Create a mock [SendTextView] with a given [number].
 */
fun createMockTextView(number: Int): SendTextView =
    SendTextView(
        hidden = false,
        text = "mockText-$number",
    )
