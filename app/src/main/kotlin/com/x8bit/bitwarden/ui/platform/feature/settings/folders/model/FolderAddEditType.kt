package com.x8bit.bitwarden.ui.platform.feature.settings.folders.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Represents the difference between creating a
 * completely new folder and editing an existing one.
 */
@Serializable
sealed class FolderAddEditType : Parcelable {

    /**
     *  The ID of the folder (nullable).
     */
    abstract val folderId: String?

    /**
     * Indicates that we want to create a completely new folder.
     */
    @Parcelize
    @Serializable
    data object AddItem : FolderAddEditType() {
        override val folderId: String?
            get() = null
    }

    /**
     * Indicates that we want to edit an existing folder.
     */
    @Parcelize
    @Serializable
    data class EditItem(
        override val folderId: String,
    ) : FolderAddEditType()
}
