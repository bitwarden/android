package com.x8bit.bitwarden.data.vault.manager

import android.content.Context
import android.net.Uri
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import java.io.ByteArrayOutputStream

/**
 * The buffer size to be used when reading from an input stream.
 */
private const val BUFFER_SIZE: Int = 1024

/**
 * The default implementation of the [FileManager] interface.
 */
@OmitFromCoverage
class FileManagerImpl(
    private val context: Context,
) : FileManager {

    override fun uriToByteArray(fileUri: Uri): ByteArray =
        context
            .contentResolver
            .openInputStream(fileUri)
            ?.use { inputStream ->
                ByteArrayOutputStream().use { outputStream ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } != -1) {
                        outputStream.write(buffer, 0, length)
                    }
                    outputStream.toByteArray()
                }
            }
            ?: byteArrayOf()
}
