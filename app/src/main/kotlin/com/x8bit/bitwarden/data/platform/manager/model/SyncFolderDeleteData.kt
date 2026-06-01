package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Required data for sync folder delete operations.
 */
data class SyncFolderDeleteData(
    val userId: String,
    val folderId: String,
)
