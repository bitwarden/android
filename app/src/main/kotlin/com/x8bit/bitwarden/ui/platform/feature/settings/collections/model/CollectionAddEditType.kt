package com.x8bit.bitwarden.ui.platform.feature.settings.collections.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the difference between creating a
 * completely new collection and editing an existing one.
 */
sealed class CollectionAddEditType : Parcelable {

    /**
     * The ID of the collection (nullable).
     */
    abstract val collectionId: String?

    /**
     * The ID of the organization this collection belongs to.
     */
    abstract val organizationId: String

    /**
     * Indicates that we want to create a completely new collection.
     */
    @Parcelize
    data class AddItem(
        override val organizationId: String,
    ) : CollectionAddEditType() {
        override val collectionId: String?
            get() = null
    }

    /**
     * Indicates that we want to edit an existing collection.
     */
    @Parcelize
    data class EditItem(
        override val collectionId: String,
        override val organizationId: String,
    ) : CollectionAddEditType()
}
