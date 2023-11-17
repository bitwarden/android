package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.core.Send
import com.bitwarden.core.SendFile
import com.bitwarden.core.SendText
import com.bitwarden.core.SendType
import com.x8bit.bitwarden.data.vault.datasource.network.model.SendTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import java.time.ZoneOffset

/**
 * Converts a list of [SyncResponseJson.Send] objects to a list of corresponding
 * Bitwarden SDK [Send] objects.
 */
fun List<SyncResponseJson.Send>.toEncryptedSdkSendList(): List<Send> =
    map { it.toEncryptedSdkSend() }

/**
 * Converts a [SyncResponseJson.Send] object to a corresponding
 * Bitwarden SDK [Send] object.
 */
fun SyncResponseJson.Send.toEncryptedSdkSend(): Send =
    Send(
        id = id,
        accessId = accessId.toString(),
        name = name.toString(),
        notes = notes,
        key = key.toString(),
        password = password,
        type = type.toSdkSendType(),
        file = file.toEncryptedSdkFile(),
        text = text.toEncryptedSdkText(),
        maxAccessCount = maxAccessCount?.toUInt(),
        accessCount = accessCount.toUInt(),
        disabled = isDisabled,
        hideEmail = shouldHideEmail,
        revisionDate = revisionDate.toInstant(ZoneOffset.UTC),
        deletionDate = deletionDate.toInstant(ZoneOffset.UTC),
        expirationDate = expirationDate?.toInstant(ZoneOffset.UTC),
    )

/**
 * Converts a [SyncResponseJson.Send.Text] object to a corresponding
 * Bitwarden SDK [SendText] object.
 */
private fun SyncResponseJson.Send.Text.toEncryptedSdkText(): SendText =
    SendText(
        text = text,
        hidden = isHidden,
    )

/**
 * Converts a [SyncResponseJson.Send.File] objects to a corresponding
 * Bitwarden SDK [SendFile] object.
 */
private fun SyncResponseJson.Send.File.toEncryptedSdkFile(): SendFile =
    SendFile(
        id = id.toString(),
        fileName = fileName.toString(),
        size = size.toString(),
        sizeName = sizeName.toString(),
    )

/**
 * Converts a [SendTypeJson] objects to a corresponding
 * Bitwarden SDK [SendType].
 */
private fun SendTypeJson.toSdkSendType(): SendType =
    when (this) {
        SendTypeJson.TEXT -> SendType.TEXT
        SendTypeJson.FILE -> SendType.FILE
    }
