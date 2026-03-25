package com.x8bit.bitwarden.ui.platform.feature.settings.collections.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * The data for the collection being displayed.
 *
 * @param id The id of the collection.
 * @param name The name of the collection.
 * @param organizationName The name of the organization the collection belongs to.
 * @param organizationId The id of the organization.
 * @param canManage Whether the user can manage (edit/delete) this collection.
 */
@Parcelize
data class CollectionDisplayItem(
    val id: String,
    val name: String,
    val organizationName: String,
    val organizationId: String,
    val canManage: Boolean,
) : Parcelable
