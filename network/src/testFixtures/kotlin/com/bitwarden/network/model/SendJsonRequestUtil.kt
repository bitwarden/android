package com.bitwarden.network.model

import java.time.ZonedDateTime

/**
 * Create a mock [SendJsonRequest] with a given [number].
 */
@Suppress("LongParameterList")
fun createMockSendJsonRequest(
    number: Int,
    name: String? = "mockName-$number",
    notes: String? = "mockNotes-$number",
    type: SendTypeJson = SendTypeJson.FILE,
    key: String = "mockKey-$number",
    maxAccessCount: Int? = 1,
    expirationDate: ZonedDateTime? = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
    deletionDate: ZonedDateTime = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
    fileLength: Long? = 1,
    file: SyncResponseJson.Send.File? = createMockFile(number = number),
    text: SyncResponseJson.Send.Text? = createMockText(number = number),
    password: String? = "mockPassword-$number",
    isDisabled: Boolean = false,
    shouldHideEmail: Boolean? = false,
): SendJsonRequest =
    SendJsonRequest(
        name = name,
        notes = notes,
        type = type,
        key = key,
        maxAccessCount = maxAccessCount,
        expirationDate = expirationDate,
        deletionDate = deletionDate,
        fileLength = fileLength,
        file = file,
        text = text,
        password = password,
        isDisabled = isDisabled,
        shouldHideEmail = shouldHideEmail,
    )
