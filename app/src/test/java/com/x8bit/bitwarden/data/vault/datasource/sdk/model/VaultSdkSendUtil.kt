package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.core.Send
import com.bitwarden.core.SendFile
import com.bitwarden.core.SendText
import com.bitwarden.core.SendType
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Create a mock [Send] with a given [number].
 */
fun createMockSdkSend(number: Int): Send =
    Send(
        id = "mockId-$number",
        accessId = "mockAccessId-$number",
        name = "mockName-$number",
        notes = "mockNotes-$number",
        key = "mockKey-$number",
        password = "mockPassword-$number",
        type = SendType.FILE,
        file = createMockSdkFile(number = number),
        text = createMockSdkText(number = number),
        maxAccessCount = 1u,
        accessCount = 1u,
        disabled = false,
        hideEmail = false,
        revisionDate = LocalDateTime.parse("2023-10-27T12:00:00").toInstant(ZoneOffset.UTC),
        deletionDate = LocalDateTime.parse("2023-10-27T12:00:00").toInstant(ZoneOffset.UTC),
        expirationDate = LocalDateTime.parse("2023-10-27T12:00:00").toInstant(ZoneOffset.UTC),
    )

/**
 * Create a mock [SendFile] with a given [number].
 */
fun createMockSdkFile(number: Int): SendFile =
    SendFile(
        fileName = "mockFileName-$number",
        size = "1",
        sizeName = "mockSizeName-$number",
        id = "mockId-$number",
    )

/**
 * Create a mock [SendText] with a given [number].
 */
fun createMockSdkText(number: Int): SendText =
    SendText(
        hidden = false,
        text = "mockText-$number",
    )
