package com.x8bit.bitwarden.ui.platform.manager.intent

import android.content.Intent

/**
 * A manager interface for handling intents related to file operations.
 */
interface FileIntentManager {
    /**
     * Creates an intent for choosing a file with the specified MIME type.
     *
     * @param withCameraIntents If `true`, includes camera intents for capturing images.
     * @return An [Intent] for choosing a file.
     */
    fun createFileChooserIntent(withCameraIntents: Boolean): Intent

    /**
     * Creates an intent to use when selecting to save an item with [fileName] to disk.
     */
    fun createDocumentIntent(fileName: String): Intent
}
