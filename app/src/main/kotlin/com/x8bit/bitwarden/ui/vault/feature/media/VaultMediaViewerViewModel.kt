package com.x8bit.bitwarden.ui.vault.feature.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.vault.manager.CipherManager
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.DownloadAttachmentResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Cross-page shared ViewModel for media attachment preview and fullscreen viewing.
 *
 * ## Architecture Highlights
 *
 * 1. **Single Source of Truth**: Owns ALL decryption state and file lifecycle.
 *
 * 2. **Dual-Decryption Strategy**: Inline thumbnails keep their decrypted
 *    files alive for the duration of the VaultItemScreen session (purged
 *    on page exit via [purgeAllDecryptedFiles]). The fullscreen viewer
 *    performs a completely independent decrypt cycle from route params
 *    via [requestFullscreenPreview], with immediate burn-after-reading.
 *
 * 3. **Vault Lock Cleanup**: Subscribes to [AuthRepository.userStateFlow].
 *    When `isVaultUnlocked` transitions to `false`, immediately purges all
 *    decrypted files and resets states to [MediaPreviewState.Masked].
 *
 * 4. **Concurrency Control**: Uses [ConcurrentHashMap] to track in-flight
 *    decryption [Job]s per attachment ID, preventing duplicate downloads.
 *
 * 5. **Auto-Unmask All**: When [isAutoUnmaskAllEnabled] is `true`,
 *    decrypting any single attachment triggers decryption of all other
 *    image attachments in the same cipher.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class VaultMediaViewerViewModel @Inject constructor(
    private val cipherManager: CipherManager,
    private val authRepository: AuthRepository,
) : ViewModel() {

    // Determines whether clicking one image auto-decrypts siblings.
    @Suppress("PrivatePropertyName")
    private val isAutoUnmaskAllEnabled = true

    // When true, entering the page auto-decrypts all image attachments
    // without requiring the user to tap the privacy mask.
    @Suppress("PrivatePropertyName")
    private val isAutoLoadAttachmentsEnabled = false

    // ---- Fullscreen State ----

    private val _activeFullscreenState =
        MutableStateFlow<MediaPreviewState>(MediaPreviewState.Masked)
    val activeFullscreenState: StateFlow<MediaPreviewState> =
        _activeFullscreenState.asStateFlow()

    private val _fullscreenTitle = MutableStateFlow("")
    val fullscreenTitle: StateFlow<String> = _fullscreenTitle.asStateFlow()

    // ---- Inline Preview State ----

    private val _inlineStates =
        MutableStateFlow<Map<String, MediaPreviewState>>(emptyMap())
    val inlineStates: StateFlow<Map<String, MediaPreviewState>> =
        _inlineStates.asStateFlow()

    // ---- File Registries ----

    /**
     * Tracks decrypted files for inline thumbnails.
     * Files persist for the VaultItemScreen session and are purged
     * on page exit (DisposableEffect) or vault lock.
     */
    private val inlineFilesRegistry = ConcurrentHashMap<String, File>()

    /**
     * Tracks the decrypted file for fullscreen viewing.
     * Burned immediately after Glide renders the bitmap into RAM.
     */
    private val fullscreenFilesRegistry = ConcurrentHashMap<String, File>()

    // ---- Concurrency ----

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private var fullscreenJob: Job? = null

    // ---- Stored Context ----

    /**
     * The CipherView associated with the current inline preview session.
     */
    private var activeCipherView: CipherView? = null

    init {
        observeVaultLockState()
    }

    private fun observeVaultLockState() {
        authRepository.userStateFlow
            .map { it?.activeAccount?.isVaultUnlocked == true }
            .distinctUntilChanged()
            .onEach { isUnlocked ->
                if (!isUnlocked) {
                    purgeAllDecryptedFiles()
                }
            }
            .launchIn(viewModelScope)
    }

    // ---- Public API: Inline Preview ----

    /**
     * Requests decryption for an inline attachment thumbnail.
     * When [isAutoUnmaskAllEnabled] is true, also triggers
     * decryption for all other image-type attachments.
     */
    fun requestPreview(
        cipherView: CipherView,
        attachmentId: String,
        fileName: String,
        allImageAttachmentIds: List<Pair<String, String>> = emptyList(),
    ) {
        activeCipherView = cipherView

        requestSinglePreview(
            cipherView = cipherView,
            attachmentId = attachmentId,
            fileName = fileName,
        )

        if (isAutoUnmaskAllEnabled) {
            allImageAttachmentIds
                .filter { (id, _) -> id != attachmentId }
                .forEach { (otherId, otherFileName) ->
                    requestSinglePreview(
                        cipherView = cipherView,
                        attachmentId = otherId,
                        fileName = otherFileName,
                    )
                }
        }
    }

    /**
     * Attempts to auto-load all image attachments on page entry.
     * Only triggers when [isAutoLoadAttachmentsEnabled] is `true`.
     */
    fun tryAutoLoadAttachments(
        cipherView: CipherView,
        allImageAttachmentIds: List<Pair<String, String>>,
    ) {
        if (!isAutoLoadAttachmentsEnabled) return
        if (allImageAttachmentIds.isEmpty()) return

        activeCipherView = cipherView
        allImageAttachmentIds.forEach { (id, fileName) ->
            requestSinglePreview(
                cipherView = cipherView,
                attachmentId = id,
                fileName = fileName,
            )
        }
    }

    // ---- Public API: Fullscreen ----

    /**
     * Triggers an independent download/decrypt cycle for the fullscreen
     * viewer. Fetches the [CipherView] by [cipherId] from [CipherManager],
     * then downloads and decrypts the attachment.
     *
     * This is fully decoupled from the inline thumbnail state — the
     * fullscreen page does not rely on any file created by the thumbnail.
     */
    fun requestFullscreenPreview(
        cipherId: String,
        attachmentId: String,
        fileName: String,
    ) {
        Timber.d("requestFullscreenPreview: cipherId=%s, attachmentId=%s", cipherId, attachmentId)
        _fullscreenTitle.value = fileName
        _activeFullscreenState.value = MediaPreviewState.Loading

        // Cancel any previous fullscreen job.
        fullscreenJob?.cancel()

        val mediaType = MediaType.fromFileName(fileName)
        if (mediaType == MediaType.UNKNOWN) {
            _activeFullscreenState.value = MediaPreviewState.Error(
                message = "Unsupported media type",
            )
            return
        }

        fullscreenJob = viewModelScope.launch {
            // Step 1: Resolve CipherView from cipherId.
            val cipherView = when (
                val result = cipherManager.getCipher(cipherId)
            ) {
                is GetCipherResult.Success -> result.cipherView
                is GetCipherResult.CipherNotFound -> {
                    _activeFullscreenState.value = MediaPreviewState.Error(
                        message = "Cipher not found",
                    )
                    return@launch
                }
                is GetCipherResult.Failure -> {
                    _activeFullscreenState.value = MediaPreviewState.Error(
                        message = "Failed to load cipher",
                    )
                    return@launch
                }
            }

            // Step 2: Download and decrypt the attachment.
            when (
                val downloadResult = cipherManager.downloadAttachment(
                    cipherView = cipherView,
                    attachmentId = attachmentId,
                )
            ) {
                is DownloadAttachmentResult.Failure -> {
                    _activeFullscreenState.value = MediaPreviewState.Error(
                        message = downloadResult.errorMessage,
                    )
                }

                is DownloadAttachmentResult.Success -> {
                    fullscreenFilesRegistry[attachmentId] = downloadResult.file

                    _activeFullscreenState.value = when (mediaType) {
                        MediaType.IMAGE -> MediaPreviewState.ImageReady(
                            decryptedFilePath = downloadResult.file.absolutePath,
                        )
                        MediaType.PDF -> MediaPreviewState.PdfReady(
                            decryptedFilePath = downloadResult.file.absolutePath,
                        )
                        MediaType.UNKNOWN -> MediaPreviewState.Error(
                            "Unsupported media type",
                        )
                    }
                }
            }
        }
    }

    /**
     * Burns the fullscreen decrypted file from disk.
     * Called by the fullscreen UI after Glide renders the bitmap into RAM.
     */
    fun deleteFullscreenDecryptedFile() {
        Timber.d("deleteFullscreenDecryptedFile: burning fullscreen file")
        purgeRegistry(fullscreenFilesRegistry)
    }

    /**
     * Clears the fullscreen viewer state.
     * Called by the fullscreen UI via DisposableEffect on page exit.
     */
    fun clearFullscreenState() {
        Timber.d("clearFullscreenState: resetting fullscreen")
        fullscreenJob?.cancel()
        fullscreenJob = null
        purgeRegistry(fullscreenFilesRegistry)
        _activeFullscreenState.value = MediaPreviewState.Masked
        _fullscreenTitle.value = ""
    }

    // ---- Public API: Cleanup ----

    /**
     * Securely deletes all decrypted media files and resets all states.
     *
     * Called by:
     * - Vault lock observer (automatic)
     * - [onCleared] (automatic)
     * - UI layer [DisposableEffect] on VaultItemScreen exit (manual)
     */
    fun purgeAllDecryptedFiles() {
        Timber.d("purgeAllDecryptedFiles: cleaning ALL inline + fullscreen files")
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        fullscreenJob?.cancel()
        fullscreenJob = null

        purgeRegistry(inlineFilesRegistry)
        purgeRegistry(fullscreenFilesRegistry)

        activeCipherView = null

        _inlineStates.value = emptyMap()
        _activeFullscreenState.value = MediaPreviewState.Masked
        _fullscreenTitle.value = ""
    }

    // ---- Private: Inline Decryption ----

    private fun requestSinglePreview(
        cipherView: CipherView,
        attachmentId: String,
        fileName: String,
    ) {
        val mediaType = MediaType.fromFileName(fileName)
        if (mediaType == MediaType.UNKNOWN) return

        val existingState = _inlineStates.value[attachmentId]
        if (existingState is MediaPreviewState.ImageReady ||
            existingState is MediaPreviewState.PdfReady
        ) {
            return
        }

        if (activeJobs.containsKey(attachmentId)) return

        updateInlineState(attachmentId, MediaPreviewState.Loading)

        val job = viewModelScope.launch {
            try {
                val result = cipherManager.downloadAttachment(
                    cipherView = cipherView,
                    attachmentId = attachmentId,
                )

                when (result) {
                    is DownloadAttachmentResult.Failure -> {
                        updateInlineState(
                            attachmentId,
                            MediaPreviewState.Error(result.errorMessage),
                        )
                    }

                    is DownloadAttachmentResult.Success -> {
                        inlineFilesRegistry[attachmentId] = result.file

                        val readyState = when (mediaType) {
                            MediaType.IMAGE -> MediaPreviewState.ImageReady(
                                decryptedFilePath = result.file.absolutePath,
                            )
                            MediaType.PDF -> MediaPreviewState.PdfReady(
                                decryptedFilePath = result.file.absolutePath,
                            )
                            MediaType.UNKNOWN -> MediaPreviewState.Error(
                                "Unsupported media type",
                            )
                        }

                        updateInlineState(attachmentId, readyState)
                    }
                }
            } finally {
                activeJobs.remove(attachmentId)
            }
        }

        activeJobs[attachmentId] = job
    }

    // ---- Private Helpers ----

    private fun updateInlineState(
        attachmentId: String,
        state: MediaPreviewState,
    ) {
        _inlineStates.update {
            it.toMutableMap().apply { put(attachmentId, state) }
        }
    }

    private fun purgeRegistry(registry: ConcurrentHashMap<String, File>) {
        registry.values.forEach { file ->
            try {
                if (file.exists()) file.delete()
            } catch (e: SecurityException) {
                Timber.e(e, "Failed to delete file: ${file.name}")
            }
        }
        registry.clear()
    }

    override fun onCleared() {
        super.onCleared()
        purgeAllDecryptedFiles()
    }
}
