package com.x8bit.bitwarden.data.platform.manager.model

import java.time.ZonedDateTime

/**
 * Required data for sync folder upsert operations.
 *
 * @property folderId The folder ID.
 * @property revisionDate The folder's revision date. This is used to determine if the local copy of
 * the folder is out-of-date.
 * @property isUpdate Whether or not this is an update of an existing folder.
 */
data class SyncFolderUpsertData(
    val folderId: String,
    val revisionDate: ZonedDateTime,
    val isUpdate: Boolean,
)
