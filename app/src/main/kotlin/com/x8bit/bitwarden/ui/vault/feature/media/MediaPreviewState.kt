package com.x8bit.bitwarden.ui.vault.feature.media

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Base media type definition for attachment content classification.
 *
 * Designed to be extended as new media capabilities are added (e.g., [PDF], video).
 */
enum class MediaType {
    IMAGE,
    PDF,
    UNKNOWN,
    ;

    companion object {
        /**
         * Infers the [MediaType] from a file name's extension.
         */
        fun fromFileName(fileName: String): MediaType {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "png", "jpg", "jpeg", "gif", "webp", "bmp" -> IMAGE
                "pdf" -> PDF
                else -> UNKNOWN
            }
        }
    }
}

/**
 * A highly extensible sealed interface for media preview state management.
 *
 * ## State Machine
 * ```
 * [Masked] → (user taps) → [Loading] → [ImageReady] | [PdfReady] | [Error]
 *                                       ↕ (shared cache hit = instant)
 * Vault lock / ViewModel clear → [Masked] + file deletion
 * ```
 */
sealed interface MediaPreviewState : Parcelable {

    /**
     * Initial privacy mask state. The attachment is not yet decrypted.
     */
    @Parcelize
    data object Masked : MediaPreviewState

    /**
     * The attachment is being downloaded and decrypted.
     */
    @Parcelize
    data object Loading : MediaPreviewState

    /**
     * The image has been successfully decrypted and is ready for display.
     * Holds the path to the decrypted file in the app's internal cache directory
     * so Glide can load it with downsampling (safe for files up to 100 MB+).
     *
     * The file path is [IgnoredOnParcel] because decrypted files are ephemeral
     * and must not survive process death.
     */
    @Parcelize
    data class ImageReady(
        @IgnoredOnParcel
        val decryptedFilePath: String = "",
    ) : MediaPreviewState

    /**
     * A PDF has been successfully decrypted and is ready for display.
     * Reserved for future PDF viewer integration.
     */
    @Parcelize
    data class PdfReady(
        @IgnoredOnParcel
        val decryptedFilePath: String = "",
        val pageCount: Int = 0,
    ) : MediaPreviewState

    /**
     * Decryption or download failed.
     */
    @Parcelize
    data class Error(
        val message: String? = null,
    ) : MediaPreviewState
}
