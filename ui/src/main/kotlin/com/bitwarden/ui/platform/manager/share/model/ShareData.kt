package com.bitwarden.ui.platform.manager.share.model

import android.os.Parcelable
import com.bitwarden.ui.platform.model.FileData
import kotlinx.parcelize.Parcelize

/**
 * Represents data for a share request coming from outside the app.
 */
sealed class ShareData : Parcelable {
    /**
     * The data required to create a new Text Send.
     */
    @Parcelize
    data class TextSend(
        val subject: String?,
        val text: String,
    ) : ShareData()

    /**
     * The data required to create a new File Send.
     */
    @Parcelize
    data class FileSend(
        val fileData: FileData,
    ) : ShareData()
}
