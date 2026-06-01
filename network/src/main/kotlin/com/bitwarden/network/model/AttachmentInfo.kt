package com.bitwarden.network.model

/**
 * Information about a cipher attachment to share.
 *
 * @property id The attachment's ID.
 * @property key The attachment's encrypted key value.
 * @property fileName The attachment's encrypted file name.
 */
data class AttachmentInfo(
    val id: String,
    val key: String,
    val fileName: String?,
)
