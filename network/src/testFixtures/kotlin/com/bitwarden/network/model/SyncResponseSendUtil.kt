package com.bitwarden.network.model

import java.time.ZonedDateTime

/**
 * Create a mock [CreateFileSendResponseJson] with a given data.
 */
fun createMockFileSendResponseJson(
    number: Int,
    url: String = "www.test.com",
    fileUploadType: FileUploadType = FileUploadType.AZURE,
    sendResponse: SyncResponseJson.Send = createMockSend(number = number, type = SendTypeJson.FILE),
): CreateFileSendResponseJson =
    CreateFileSendResponseJson(
        url = url,
        fileUploadType = fileUploadType,
        sendResponse = sendResponse,
    )

/**
 * Create a mock [SyncResponseJson.Send] with a given [number].
 */
@Suppress("LongParameterList")
fun createMockSend(
    number: Int,
    accessCount: Int = 1,
    maxAccessCount: Int? = 1,
    notes: String? = "mockNotes-$number",
    revisionDate: ZonedDateTime = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
    shouldHideEmail: Boolean = false,
    type: SendTypeJson = SendTypeJson.FILE,
    accessId: String? = "mockAccessId-$number",
    password: String? = "mockPassword-$number",
    file: SyncResponseJson.Send.File? = createMockFile(number = number),
    deletionDate: ZonedDateTime = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
    name: String? = "mockName-$number",
    isDisabled: Boolean = false,
    id: String = "mockId-$number",
    text: SyncResponseJson.Send.Text? = createMockText(number = number),
    key: String? = "mockKey-$number",
    expirationDate: ZonedDateTime? = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
): SyncResponseJson.Send =
    SyncResponseJson.Send(
        accessCount = accessCount,
        notes = notes,
        revisionDate = revisionDate,
        maxAccessCount = maxAccessCount,
        shouldHideEmail = shouldHideEmail,
        type = type,
        accessId = accessId,
        password = password,
        file = file,
        deletionDate = deletionDate,
        name = name,
        isDisabled = isDisabled,
        id = id,
        text = text,
        key = key,
        expirationDate = expirationDate,
    )

/**
 * Create a mock [SyncResponseJson.Send.File] with a given [number].
 */
fun createMockFile(
    number: Int,
    fileName: String? = "mockFileName-$number",
    size: Int? = 1,
    sizeName: String? = "mockSizeName-$number",
    id: String? = "mockId-$number",
): SyncResponseJson.Send.File =
    SyncResponseJson.Send.File(
        fileName = fileName,
        size = size,
        sizeName = sizeName,
        id = id,
    )

/**
 * Create a mock [SyncResponseJson.Send.Text] with a given [number].
 */
fun createMockText(
    number: Int,
    isHidden: Boolean = false,
    text: String? = "mockText-$number",
): SyncResponseJson.Send.Text =
    SyncResponseJson.Send.Text(
        isHidden = isHidden,
        text = text,
    )
