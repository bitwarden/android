package com.bitwarden.ui.platform.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents file information.
 */
@Parcelize
data class FileData(
    val fileName: String,
    val uri: Uri,
    val sizeBytes: Long,
) : Parcelable
