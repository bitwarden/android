package com.bitwarden.authenticator.data.authenticator.manager

import android.content.Context
import android.net.Uri
import com.bitwarden.authenticator.data.platform.manager.DispatcherManager
import kotlinx.coroutines.withContext

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
}
