package com.bitwarden.authenticator.data.platform.manager.imports.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LastPassJsonExport(
    val deviceId: String,
    val deviceSecret: String,
    val localDeviceId: String,
    val deviceName: String,
    val version: Int,
    val accounts: List<Account>,
    val folders: List<Folder>,
) {
    @Serializable
    data class Account(
        @SerialName("accountID")
        val accountId: String,
        val issuerName: String,
        val originalIssuerName: String,
        val userName: String,
        val originalUserName: String,
        val pushNotification: Boolean,
        val secret: String,
        val timeStep: Int,
        val digits: Int,
        val creationTimestamp: Long,
        val isFavorite: Boolean,
        val algorithm: String,
        val folderData: FolderData?,
        val backupInfo: BackupInfo?,
    ) {
        @Serializable
        data class FolderData(
            val folderId: String,
            val position: Int,
        )

        @Serializable
        data class BackupInfo(
            val creationDate: String,
            val deviceOs: String,
            val appVersion: String,
        )
    }

    @Serializable
    data class Folder(
        val id: Int,
        val name: String,
        val isOpened: Boolean,
    )
}
