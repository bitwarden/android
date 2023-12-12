package com.x8bit.bitwarden.ui.vault.feature.additem.model

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * The Enum representing the Custom Field type that is being added by the user.
 */
enum class CustomFieldType(val typeText: Text) {
    LINKED(R.string.field_type_linked.asText()),
    HIDDEN(R.string.field_type_hidden.asText()),
    BOOLEAN(R.string.field_type_boolean.asText()),
    TEXT(R.string.field_type_text.asText()),
}
