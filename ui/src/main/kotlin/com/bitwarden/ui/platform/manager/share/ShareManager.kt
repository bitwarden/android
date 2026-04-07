package com.bitwarden.ui.platform.manager.share

import android.content.Intent
import com.bitwarden.ui.platform.manager.share.model.ShareData

/**
 * Manages the retrieving data shared to this app.
 */
interface ShareManager {
    /**
     * Processes the [Intent] and attempts to derive [ShareData] information from it.
     */
    fun getShareDataOrNull(intent: Intent): ShareData?
}
