package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Required data for sync send delete operations.
 */
data class SyncSendDeleteData(
    val userId: String,
    val sendId: String,
)
