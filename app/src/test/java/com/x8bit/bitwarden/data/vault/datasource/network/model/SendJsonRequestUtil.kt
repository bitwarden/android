package com.x8bit.bitwarden.data.vault.datasource.network.model

import java.time.ZonedDateTime

/**
 * Create a mock [SendJsonRequest] with a given [number].
 */
fun createMockSendJsonRequest(
    number: Int,
    type: SendTypeJson = SendTypeJson.FILE,
): SendJsonRequest =
    SendJsonRequest(
        name = "mockName-$number",
        notes = "mockNotes-$number",
        type = type,
        key = "mockKey-$number",
        maxAccessCount = 1,
        expirationDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
        deletionDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
        fileLength = 1,
        file = createMockFile(number),
        text = createMockText(number),
        password = "mockPassword-$number",
        isDisabled = false,
        shouldHideEmail = false,
    )
