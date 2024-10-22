package com.x8bit.bitwarden.ui.vault.feature.addedit.model

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import java.util.UUID

/**
 * The Enum representing the Custom Field type that is being added by the user.
 */
enum class CustomFieldType(val typeText: Text) {
    LINKED(R.string.field_type_linked.asText()),
    HIDDEN(R.string.field_type_hidden.asText()),
    BOOLEAN(R.string.field_type_boolean.asText()),
    TEXT(R.string.field_type_text.asText()),
}

/**
 * A function that converts [CustomFieldType] and a string to [VaultAddEditState.Custom].
 */
fun CustomFieldType.toCustomField(
    name: String,
    itemType: VaultAddEditState.ViewState.Content.ItemType,
): VaultAddEditState.Custom {
    return when (this) {
        CustomFieldType.BOOLEAN -> {
            VaultAddEditState.Custom.BooleanField(
                itemId = UUID.randomUUID().toString(),
                name = name,
                value = false,
            )
        }

        CustomFieldType.LINKED -> {
            VaultAddEditState.Custom.LinkedField(
                itemId = UUID.randomUUID().toString(),
                name = name,
                vaultLinkedFieldType = itemType.defaultLinkedFieldTypeOrNull,
            )
        }

        CustomFieldType.HIDDEN -> {
            VaultAddEditState.Custom.HiddenField(
                itemId = UUID.randomUUID().toString(),
                name = name,
                value = "",
            )
        }

        CustomFieldType.TEXT -> {
            VaultAddEditState.Custom.TextField(
                itemId = UUID.randomUUID().toString(),
                name = name,
                value = "",
            )
        }
    }
}

@Suppress("MaxLineLength")
private val VaultAddEditState.ViewState.Content.ItemType.defaultLinkedFieldTypeOrNull: VaultLinkedFieldType?
    get() = when (this) {
        is VaultAddEditState.ViewState.Content.ItemType.Card -> VaultLinkedFieldType.CARDHOLDER_NAME
        is VaultAddEditState.ViewState.Content.ItemType.Identity -> VaultLinkedFieldType.TITLE
        is VaultAddEditState.ViewState.Content.ItemType.Login -> VaultLinkedFieldType.USERNAME
        is VaultAddEditState.ViewState.Content.ItemType.SecureNotes -> null
        is VaultAddEditState.ViewState.Content.ItemType.SshKey -> null
    }
