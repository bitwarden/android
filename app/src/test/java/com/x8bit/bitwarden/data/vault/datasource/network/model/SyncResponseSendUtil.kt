package com.x8bit.bitwarden.data.vault.datasource.network.model

import java.time.ZonedDateTime

fun createMockFileSendResponseJson(number: Int, type: SendTypeJson = SendTypeJson.FILE) =
    CreateFileSendResponseJson(
        url = "www.test.com",
        fileUploadType = FileUploadType.AZURE,
        sendResponse = createMockSend(number = number, type = type),
    )

fun createMockSend(
    number: Int,
    type: SendTypeJson = SendTypeJson.FILE,
): SyncResponseJson.Send =
    SyncResponseJson.Send(
        accessCount = 1,
        notes = "mockNotes-$number",
        revisionDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
        maxAccessCount = 1,
        shouldHideEmail = false,
        type = type,
        accessId = "mockAccessId-$number",
        password = "mockPassword-$number",
        file = createMockFile(number = number),
        deletionDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
        name = "mockName-$number",
        isDisabled = false,
        id = "mockId-$number",
        text = createMockText(number = number),
        key = "mockKey-$number",
        expirationDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
    )

fun createMockFile(number: Int): SyncResponseJson.Send.File =
    SyncResponseJson.Send.File(
        fileName = "mockFileName-$number",
        size = 1,
        sizeName = "mockSizeName-$number",
        id = "mockId-$number",
    )

fun createMockText(number: Int): SyncResponseJson.Send.Text =
    SyncResponseJson.Send.Text(
        isHidden = false,
        text = "mockText-$number",
    )
