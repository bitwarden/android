package com.bitwarden.authenticator.data.platform.manager.imports.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models the JSON export file from LastPass.
 */
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
    /**
     * Models an account contained within a [LastPassJsonExport].
     */
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
        /**
         * Models metadata for a [Folder].
         */
        @Serializable
        data class FolderData(
            val folderId: String,
            val position: Int,
        )

        /**
         * Models backup file information for an [Account].
         */
        @Serializable
        data class BackupInfo(
            val creationDate: String,
            val deviceOs: String,
            val appVersion: String,
        )
    }

    /**
     * Models a collection of [Account] objects.
     */
    @Serializable
    data class Folder(
        val id: Int,
        val name: String,
        val isOpened: Boolean,
    )
}
