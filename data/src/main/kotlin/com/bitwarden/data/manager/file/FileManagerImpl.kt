@file:OmitFromCoverage

package com.bitwarden.data.manager.file

import android.content.Context
import android.net.Uri
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.util.sdkAgnosticTransferTo
import com.bitwarden.data.manager.model.DownloadResult
import com.bitwarden.data.manager.model.ZipFileResult
import com.bitwarden.network.service.DownloadService
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The buffer size to be used when reading from an input stream.
 */
private const val BUFFER_SIZE: Int = 1024

/**
 * The default implementation of the [FileManager] interface.
 */
internal class FileManagerImpl(
    private val context: Context,
    private val downloadService: DownloadService,
    private val dispatcherManager: DispatcherManager,
) : FileManager {

    override val filesDirectory: String
        get() = context.filesDir.absolutePath

    override val logsDirectory: String
        get() = "${context.dataDir}/logs"

    override suspend fun delete(vararg files: File) {
        withContext(dispatcherManager.io) {
            files.forEach { it.delete() }
        }
    }

    @Suppress("NestedBlockDepth")
    override suspend fun downloadFileToCache(url: String): DownloadResult {
        val response = downloadService
            .getDataStream(url)
            .fold(
                onSuccess = { it },
                onFailure = { return DownloadResult.Failure(error = it) },
            )

        // Create a temporary file in cache to write to
        val file = File(context.cacheDir, UUID.randomUUID().toString())

        withContext(dispatcherManager.io) {
            val stream = response.byteStream()
            stream.use {
                val buffer = ByteArray(BUFFER_SIZE)
                var progress = 0
                FileOutputStream(file).use { fos ->
                    @Suppress("TooGenericExceptionCaught")
                    try {
                        var read = stream.read(buffer)
                        while (read > 0) {
                            fos.write(buffer, 0, read)
                            progress += read
                            read = stream.read(buffer)
                        }
                        fos.flush()
                    } catch (e: RuntimeException) {
                        return@withContext DownloadResult.Failure(error = e)
                    }
                }
            }
        }

        return DownloadResult.Success(file)
    }

    @Suppress("NestedBlockDepth")
    override suspend fun fileToUri(fileUri: Uri, file: File): Boolean {
        @Suppress("TooGenericExceptionCaught")
        return try {
            withContext(dispatcherManager.io) {
                context
                    .contentResolver
                    .openOutputStream(fileUri)
                    ?.use { outputStream ->
                        FileInputStream(file).use { inputStream ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var length: Int
                            while (inputStream.read(buffer).also { length = it } != -1) {
                                outputStream.write(buffer, 0, length)
                            }
                        }
                    }
            }
            true
        } catch (_: RuntimeException) {
            false
        }
    }

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
        } catch (_: RuntimeException) {
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

    override suspend fun zipUriToCache(
        uri: Uri,
    ): ZipFileResult =
        runCatching {
            withContext(dispatcherManager.io) {
                val sourceFile = File(uri.toString())
                if (!sourceFile.exists()) {
                    ZipFileResult.NothingToZip
                } else {
                    val zipFile = File.createTempFile(
                        "bitwarden_flight_recorder",
                        ".zip",
                        context.cacheDir,
                    )
                    FileOutputStream(zipFile).use { fos ->
                        BufferedOutputStream(fos).use { bos ->
                            ZipOutputStream(bos).use { zos ->
                                zos.zipFiles(sourceFile = sourceFile)
                            }
                        }
                    }
                    ZipFileResult.Success(file = zipFile)
                }
            }
        }
            .fold(
                onFailure = { ZipFileResult.Failure(error = it) },
                onSuccess = { it },
            )

    override suspend fun writeUriToCache(fileUri: Uri): Result<File> =
        runCatching {
            withContext(dispatcherManager.io) {
                val tempFileName = "temp_send_file.bw"
                context
                    .contentResolver
                    .openInputStream(fileUri)
                    ?.use { inputStream ->
                        context.openFileOutput(tempFileName, Context.MODE_PRIVATE)
                            .use { outputStream ->
                                inputStream.sdkAgnosticTransferTo(outputStream)
                            }
                    }
                    ?: throw IllegalStateException("Stream has crashed")

                File(context.filesDir, tempFileName)
            }
        }
}

/**
 * A helper function to write the files to a zip file.
 */
@Suppress("NestedBlockDepth")
private fun ZipOutputStream.zipFiles(
    sourceFile: File,
    parentDir: String? = null,
) {
    if (sourceFile.isDirectory) {
        val parentDirName = sourceFile.name + File.separator
        val entry = ZipEntry(parentDirName).apply {
            this.time = sourceFile.lastModified()
            this.size = sourceFile.length()
        }
        this.putNextEntry(entry)
        sourceFile.listFiles().orEmpty().forEach { file ->
            this.zipFiles(sourceFile = file, parentDir = parentDirName)
        }
    } else {
        FileInputStream(sourceFile).use { fis ->
            BufferedInputStream(fis).use { bis ->
                val entry = ZipEntry("${parentDir.orEmpty()}/${sourceFile.name}").apply {
                    this.time = sourceFile.lastModified()
                    this.size = sourceFile.length()
                }
                this.putNextEntry(entry)
                val data = ByteArray(BUFFER_SIZE)
                while (true) {
                    val readBytes = bis.read(data)
                    if (readBytes == -1) break
                    this.write(data, 0, readBytes)
                }
            }
        }
    }
}
