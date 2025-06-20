package com.x8bit.bitwarden.ui.vault.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Models a collection.
 *
 * @property id the collection id.
 * @property name the collection name.
 * @property isSelected if the collection is selected or not.
 */
@Parcelize
data class VaultCollection(
    val id: String,
    val name: String,
    val isSelected: Boolean,
) : Parcelable
