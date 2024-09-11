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

    TITLE(id = 400.toUInt(), label = R.string.title.asText()),
    MIDDLE_NAME(id = 401.toUInt(), label = R.string.middle_name.asText()),
    ADDRESS_1(id = 402.toUInt(), label = R.string.address1.asText()),
    ADDRESS_2(id = 403.toUInt(), label = R.string.address2.asText()),
    ADDRESS_3(id = 404.toUInt(), label = R.string.address3.asText()),
    CITY(id = 405.toUInt(), label = R.string.city_town.asText()),
    STATE(id = 406.toUInt(), label = R.string.state_province.asText()),
    POSTAL_CODE(id = 407.toUInt(), label = R.string.zip_postal_code.asText()),
    COUNTRY(id = 408.toUInt(), label = R.string.country.asText()),
    COMPANY(id = 409.toUInt(), label = R.string.company.asText()),
    EMAIL(id = 410.toUInt(), label = R.string.email.asText()),
    PHONE(id = 411.toUInt(), label = R.string.phone.asText()),
    SSN(id = 412.toUInt(), label = R.string.ssn.asText()),
    IDENTITY_USERNAME(id = 413.toUInt(), label = R.string.username.asText()),
    PASSPORT_NUMBER(id = 414.toUInt(), label = R.string.passport_number.asText()),
    LICENSE_NUMBER(id = 415.toUInt(), label = R.string.license_number.asText()),
    FIRST_NAME(id = 416.toUInt(), label = R.string.first_name.asText()),
    LAST_NAME(id = 417.toUInt(), label = R.string.last_name.asText()),
    FULL_NAME(id = 418.toUInt(), label = R.string.full_name.asText()),
    ;

    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Helper function to get the LinkedCustomFieldType from the id
         */
        fun fromId(id: UInt): VaultLinkedFieldType =
            VaultLinkedFieldType.entries.first { it.id == id }
    }
}
