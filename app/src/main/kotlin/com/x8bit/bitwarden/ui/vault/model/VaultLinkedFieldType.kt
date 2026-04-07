package com.x8bit.bitwarden.ui.vault.model

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

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
    USERNAME(id = 100.toUInt(), label = BitwardenString.username.asText()),
    PASSWORD(id = 101.toUInt(), label = BitwardenString.password.asText()),

    CARDHOLDER_NAME(id = 300.toUInt(), label = BitwardenString.cardholder_name.asText()),
    EXPIRATION_MONTH(id = 301.toUInt(), label = BitwardenString.expiration_month.asText()),
    EXPIRATION_YEAR(id = 302.toUInt(), label = BitwardenString.expiration_year.asText()),
    SECURITY_CODE(id = 303.toUInt(), label = BitwardenString.security_code.asText()),
    BRAND(id = 304.toUInt(), label = BitwardenString.brand.asText()),
    NUMBER(id = 305.toUInt(), label = BitwardenString.number.asText()),

    TITLE(id = 400.toUInt(), label = BitwardenString.title.asText()),
    MIDDLE_NAME(id = 401.toUInt(), label = BitwardenString.middle_name.asText()),
    ADDRESS_1(id = 402.toUInt(), label = BitwardenString.address1.asText()),
    ADDRESS_2(id = 403.toUInt(), label = BitwardenString.address2.asText()),
    ADDRESS_3(id = 404.toUInt(), label = BitwardenString.address3.asText()),
    CITY(id = 405.toUInt(), label = BitwardenString.city_town.asText()),
    STATE(id = 406.toUInt(), label = BitwardenString.state_province.asText()),
    POSTAL_CODE(id = 407.toUInt(), label = BitwardenString.zip_postal_code.asText()),
    COUNTRY(id = 408.toUInt(), label = BitwardenString.country.asText()),
    COMPANY(id = 409.toUInt(), label = BitwardenString.company.asText()),
    EMAIL(id = 410.toUInt(), label = BitwardenString.email.asText()),
    PHONE(id = 411.toUInt(), label = BitwardenString.phone.asText()),
    SSN(id = 412.toUInt(), label = BitwardenString.ssn.asText()),
    IDENTITY_USERNAME(id = 413.toUInt(), label = BitwardenString.username.asText()),
    PASSPORT_NUMBER(id = 414.toUInt(), label = BitwardenString.passport_number.asText()),
    LICENSE_NUMBER(id = 415.toUInt(), label = BitwardenString.license_number.asText()),
    FIRST_NAME(id = 416.toUInt(), label = BitwardenString.first_name.asText()),
    LAST_NAME(id = 417.toUInt(), label = BitwardenString.last_name.asText()),
    FULL_NAME(id = 418.toUInt(), label = BitwardenString.full_name.asText()),
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
