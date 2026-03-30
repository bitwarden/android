package com.bitwarden.ui.platform.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents metadata about a selected folder.
 */
@Parcelize
data class FolderData(
    val folderName: String,
    val uri: Uri,
    val fileCount: Int,
    val totalSizeBytes: Long,
) : Parcelable
