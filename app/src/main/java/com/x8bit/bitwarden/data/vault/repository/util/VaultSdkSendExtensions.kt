package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.send.Send
import com.bitwarden.send.SendFile
import com.bitwarden.send.SendText
import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.x8bit.bitwarden.data.platform.util.SpecialCharWithPrecedenceComparator
import com.x8bit.bitwarden.data.vault.datasource.network.model.SendJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SendTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Converts a Bitwarden SDK [Send] object to a corresponding [SyncResponseJson.Send] object.
 */
fun Send.toEncryptedNetworkSend(fileLength: Long? = null): SendJsonRequest =
    SendJsonRequest(
        type = type.toNetworkSendType(),
        name = name,
        notes = notes,
        key = key,
        maxAccessCount = maxAccessCount?.toInt(),
        expirationDate = expirationDate?.let { ZonedDateTime.ofInstant(it, ZoneOffset.UTC) },
        deletionDate = ZonedDateTime.ofInstant(deletionDate, ZoneOffset.UTC),
        fileLength = fileLength,
        file = file?.toNetworkSendFile(),
        text = text?.toNetworkSendText(),
        password = password,
        isDisabled = disabled,
        shouldHideEmail = hideEmail,
    )

/**
 * Converts a Bitwarden SDK [SendFile] object to a corresponding [SyncResponseJson.Send.File]
 * object.
 */
private fun SendFile.toNetworkSendFile(): SyncResponseJson.Send.File =
    SyncResponseJson.Send.File(
        fileName = fileName,
        size = size?.toInt(),
        sizeName = sizeName,
        id = id,
    )

/**
 * Converts a Bitwarden SDK [SendText] object to a corresponding [SyncResponseJson.Send.Text]
 * object.
 */
private fun SendText.toNetworkSendText(): SyncResponseJson.Send.Text =
    SyncResponseJson.Send.Text(
        isHidden = hidden,
        text = text,
    )

/**
 * Converts a Bitwarden SDK [SendType] object to a corresponding [SendTypeJson] object.
 */
private fun SendType.toNetworkSendType(): SendTypeJson =
    when (this) {
        SendType.TEXT -> SendTypeJson.TEXT
        SendType.FILE -> SendTypeJson.FILE
    }

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
        file = file?.toEncryptedSdkFile(),
        text = text?.toEncryptedSdkText(),
        maxAccessCount = maxAccessCount?.toUInt(),
        accessCount = accessCount.toUInt(),
        disabled = isDisabled,
        hideEmail = shouldHideEmail,
        revisionDate = revisionDate.toInstant(),
        deletionDate = deletionDate.toInstant(),
        expirationDate = expirationDate?.toInstant(),
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

/**
 * Sorts the data in alphabetical order by name.
 */
@JvmName("toAlphabeticallySortedSendList")
fun List<SendView>.sortAlphabetically(): List<SendView> {
    return this.sortedWith(
        comparator = { send1, send2 ->
            SpecialCharWithPrecedenceComparator.compare(send1.name, send2.name)
        },
    )
}
