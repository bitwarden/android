package com.bitwarden.ui.platform.util

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.model.FileData

/**
 * Retrieves the local [FileData] from the given [uri].
 */
@OmitFromCoverage
fun Context.getLocalFileData(
    uri: Uri,
): FileData? = this
    .contentResolver
    .query(
        uri,
        arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE),
        null,
        null,
        null,
    )
    ?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val fileName = cursor
            .getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            .takeIf { it >= 0 }
            ?.let { cursor.getString(it) }
            ?: return@use null
        val fileSize = cursor
            .getColumnIndex(MediaStore.MediaColumns.SIZE)
            .takeIf { it >= 0 }
            ?.let { cursor.getLong(it) }
            ?: return@use null
        FileData(fileName = fileName, uri = uri, sizeBytes = fileSize)
    }
