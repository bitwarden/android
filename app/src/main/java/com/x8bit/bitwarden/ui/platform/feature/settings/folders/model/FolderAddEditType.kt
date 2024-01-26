package com.x8bit.bitwarden.ui.platform.feature.settings.folders.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the difference between creating a
 * completely new folder and editing an existing one.
 */
sealed class FolderAddEditType : Parcelable {

    /**
     *  The ID of the folder (nullable).
     */
    abstract val folderId: String?

    /**
     * Indicates that we want to create a completely new folder.
     */
    @Parcelize
    data object AddItem : FolderAddEditType() {
        override val folderId: String?
            get() = null
    }

    /**
     * Indicates that we want to edit an existing folder.
     */
    @Parcelize
    data class EditItem(
        override val folderId: String,
    ) : FolderAddEditType()
}
