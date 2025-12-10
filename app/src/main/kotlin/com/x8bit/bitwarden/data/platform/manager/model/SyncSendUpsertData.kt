package com.x8bit.bitwarden.data.platform.manager.model

import java.time.ZonedDateTime

/**
 * Required data for sync send upsert operations.
 *
 * @property userId The user ID associated with this update.
 * @property sendId The send ID.
 * @property revisionDate The send's revision date. This is used to determine if the local copy of
 * the send is out-of-date.
 * @property isUpdate Whether or not this is an update of an existing send.
 */
data class SyncSendUpsertData(
    val userId: String,
    val sendId: String,
    val revisionDate: ZonedDateTime,
    val isUpdate: Boolean,
)
