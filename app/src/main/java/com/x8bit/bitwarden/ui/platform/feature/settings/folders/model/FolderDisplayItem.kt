package com.x8bit.bitwarden.ui.platform.feature.settings.folders.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * The data for the folder being displayed.
 *
 * @param id The id of the folder.
 * @param name The name of the folder.
 */
@Parcelize
data class FolderDisplayItem(
    val id: String,
    val name: String,
) : Parcelable
