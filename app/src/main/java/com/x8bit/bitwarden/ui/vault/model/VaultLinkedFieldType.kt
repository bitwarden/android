package com.x8bit.bitwarden.ui.vault.model

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * Represents the types for linked fields.
 *
 * @param id The ID for the linked field.
 * @param label A human-readable label for the linked field.
 */
enum class VaultLinkedFieldType(
    val id: UInt,
    val label: Text,
) {
    USERNAME(id = 100.toUInt(), label = R.string.username.asText()),
    PASSWORD(id = 101.toUInt(), label = R.string.password.asText()),

    CARDHOLDER_NAME(id = 300.toUInt(), label = R.string.cardholder_name.asText()),
    EXPIRATION_MONTH(id = 301.toUInt(), label = R.string.expiration_month.asText()),
    EXPIRATION_YEAR(id = 302.toUInt(), label = R.string.expiration_year.asText()),
    SECURITY_CODE(id = 303.toUInt(), label = R.string.security_code.asText()),
    BRAND(id = 304.toUInt(), label = R.string.brand.asText()),
    NUMBER(id = 305.toUInt(), label = R.string.number.asText()),
    ;

    companion object {
        /**
         * Helper function to get the LinkedCustomFieldType from the id
         */
        fun fromId(id: UInt): VaultLinkedFieldType =
            VaultLinkedFieldType.entries.first { it.id == id }
    }
}
