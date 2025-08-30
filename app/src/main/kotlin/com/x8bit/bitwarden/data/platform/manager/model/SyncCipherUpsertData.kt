package com.x8bit.bitwarden.data.platform.manager.model

import java.time.ZonedDateTime

/**
 * Required data for sync cipher upsert operations.
 *
 * @property cipherId The cipher ID.
 * @property revisionDate The cipher's revision date. This is used to determine if the local copy of
 * the cipher is out-of-date.
 * @property isUpdate Whether or not this is an update of an existing cipher.
 */
data class SyncCipherUpsertData(
    val cipherId: String,
    val revisionDate: ZonedDateTime,
    val organizationId: String?,
    val collectionIds: List<String>?,
    val isUpdate: Boolean,
)
