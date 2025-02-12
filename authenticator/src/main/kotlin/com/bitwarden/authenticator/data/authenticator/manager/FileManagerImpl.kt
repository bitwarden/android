package com.bitwarden.authenticator.data.authenticator.manager

import android.content.Context
import android.net.Uri
import com.bitwarden.authenticator.data.platform.manager.DispatcherManager
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * The buffer size to be used when reading from an input stream.
 */
private const val BUFFER_SIZE: Int = 1024

/**
 * Manages reading and writing files.
 */
class FileManagerImpl(
    private val context: Context,
    private val dispatcherManager: DispatcherManager,
) : FileManager {

    override suspend fun stringToUri(fileUri: Uri, dataString: String): Boolean {
        @Suppress("TooGenericExceptionCaught")
        return try {
            withContext(dispatcherManager.io) {
                context
                    .contentResolver
                    .openOutputStream(fileUri)
                    ?.use { outputStream ->
                        outputStream.write(dataString.toByteArray())
                    }
            }
            true
        } catch (exception: RuntimeException) {
            false
        }
    }

    override suspend fun uriToByteArray(fileUri: Uri): Result<ByteArray> =
        runCatching {
            withContext(dispatcherManager.io) {
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
                    ?: throw IllegalStateException("Stream has crashed")
            }
        }
}
