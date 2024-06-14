package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.send.Send
import com.bitwarden.send.SendFile
import com.bitwarden.send.SendText
import com.bitwarden.send.SendType
import java.time.ZonedDateTime

/**
 * Create a mock [Send] with a given [number].
 */
fun createMockSdkSend(
    number: Int,
    type: SendType = SendType.FILE,
): Send =
    Send(
        id = "mockId-$number",
        accessId = "mockAccessId-$number",
        name = "mockName-$number",
        notes = "mockNotes-$number",
        key = "mockKey-$number",
        password = "mockPassword-$number",
        type = type,
        file = createMockSdkFile(number = number),
        text = createMockSdkText(number = number),
        maxAccessCount = 1u,
        accessCount = 1u,
        disabled = false,
        hideEmail = false,
        revisionDate = ZonedDateTime.parse("2023-10-27T12:00:00Z").toInstant(),
        deletionDate = ZonedDateTime.parse("2023-10-27T12:00:00Z").toInstant(),
        expirationDate = ZonedDateTime.parse("2023-10-27T12:00:00Z").toInstant(),
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
