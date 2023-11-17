package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.core.SendFileView
import com.bitwarden.core.SendTextView
import com.bitwarden.core.SendType
import com.bitwarden.core.SendView
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Create a mock [SendView] with a given [number].
 */
fun createMockSendView(number: Int): SendView =
    SendView(
        id = "mockId-$number",
        accessId = "mockAccessId-$number",
        name = "mockName-$number",
        notes = "mockNotes-$number",
        key = "mockKey-$number",
        password = "mockPassword-$number",
        type = SendType.FILE,
        file = createMockFileView(number = number),
        text = createMockTextView(number = number),
        maxAccessCount = 1u,
        accessCount = 1u,
        disabled = false,
        hideEmail = false,
        revisionDate = LocalDateTime.parse("2023-10-27T12:00:00").toInstant(ZoneOffset.UTC),
        deletionDate = LocalDateTime.parse("2023-10-27T12:00:00").toInstant(ZoneOffset.UTC),
        expirationDate = LocalDateTime.parse("2023-10-27T12:00:00").toInstant(ZoneOffset.UTC),
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
