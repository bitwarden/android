package com.x8bit.bitwarden.ui.vault.feature.additem.model

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.additem.VaultAddItemState
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
 * A function that converts [CustomFieldType] and a string to [VaultAddItemState.Custom].
 */
fun CustomFieldType.toCustomField(
    name: String,
): VaultAddItemState.Custom {
    return when (this) {
        CustomFieldType.BOOLEAN -> {
            VaultAddItemState.Custom.BooleanField(
                itemId = UUID.randomUUID().toString(),
                name = name,
                value = false,
            )
        }

        CustomFieldType.LINKED -> {
            VaultAddItemState.Custom.LinkedField(
                itemId = UUID.randomUUID().toString(),
                name = name,
                vaultLinkedFieldType = VaultLinkedFieldType.USERNAME,
            )
        }

        CustomFieldType.HIDDEN -> {
            VaultAddItemState.Custom.HiddenField(
                itemId = UUID.randomUUID().toString(),
                name = name,
                value = "",
            )
        }

        CustomFieldType.TEXT -> {
            VaultAddItemState.Custom.TextField(
                itemId = UUID.randomUUID().toString(),
                name = name,
                value = "",
            )
        }
    }
}
