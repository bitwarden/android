package com.x8bit.bitwarden.data.vault.datasource.network.model

import java.time.LocalDateTime

fun createMockSend(number: Int): SyncResponseJson.Send =
    SyncResponseJson.Send(
        accessCount = 1,
        notes = "mockNotes-$number",
        revisionDate = LocalDateTime.parse("2023-10-27T12:00:00"),
        maxAccessCount = 1,
        shouldHideEmail = false,
        type = SendTypeJson.FILE,
        accessId = "mockAccessId-$number",
        password = "mockPassword-$number",
        file = createMockFile(number = 1),
        deletionDate = LocalDateTime.parse("2023-10-27T12:00:00"),
        name = "mockName-$number",
        isDisabled = false,
        id = "mockId-$number",
        text = createMockText(number = number),
        key = "mockKey-$number",
        expirationDate = LocalDateTime.parse("2023-10-27T12:00:00"),
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
