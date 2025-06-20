package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.send.SendFileView
import com.bitwarden.send.SendTextView
import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Create a mock [SendView] with a given [number].
 */
@Suppress("LongParameterList")
fun createMockSendView(
    number: Int,
    id: String? = "mockId-$number",
    accessId: String? = "mockAccessId-$number",
    name: String = "mockName-$number",
    notes: String? = "mockNotes-$number",
    key: String = "mockKey-$number",
    newPassword: String? = "mockPassword-$number",
    hasPassword: Boolean = true,
    type: SendType = SendType.FILE,
    file: SendFileView = createMockFileView(number = number),
    text: SendTextView = createMockTextView(number = number),
    maxAccessCount: UInt? = 1U,
    accessCount: UInt = 1U,
    disabled: Boolean = false,
    hideEmail: Boolean = false,
    revisionDate: Instant = ZonedDateTime.parse("2023-10-27T12:00:00Z").toInstant(),
    deletionDate: Instant = ZonedDateTime.parse("2023-10-27T12:00:00Z").toInstant(),
    expirationDate: Instant? = ZonedDateTime.parse("2023-10-27T12:00:00Z").toInstant(),
): SendView =
    SendView(
        id = id,
        accessId = accessId,
        name = name,
        notes = notes,
        key = key,
        newPassword = newPassword,
        hasPassword = hasPassword,
        type = type,
        file = file.takeIf { type == SendType.FILE },
        text = text.takeIf { type == SendType.TEXT },
        maxAccessCount = maxAccessCount,
        accessCount = accessCount,
        disabled = disabled,
        hideEmail = hideEmail,
        revisionDate = revisionDate,
        deletionDate = deletionDate,
        expirationDate = expirationDate,
    )

/**
 * Create a mock [SendFileView] with a given [number].
 */
fun createMockFileView(
    number: Int,
    id: String? = "mockId-$number",
    fileName: String = "mockFileName-$number",
    size: String? = "1",
    sizeName: String? = "mockSizeName-$number",
): SendFileView =
    SendFileView(
        id = id,
        fileName = fileName,
        size = size,
        sizeName = sizeName,
    )

/**
 * Create a mock [SendTextView] with a given [number].
 */
fun createMockTextView(
    number: Int,
    text: String? = "mockText-$number",
    hidden: Boolean = false,
): SendTextView =
    SendTextView(
        text = text,
        hidden = hidden,
    )
