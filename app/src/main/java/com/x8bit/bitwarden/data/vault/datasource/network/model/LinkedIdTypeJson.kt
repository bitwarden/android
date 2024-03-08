package com.x8bit.bitwarden.data.vault.datasource.network.model

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents different fields that a custom cipher field can be linked to.
 */
@Serializable(LinkedIdTypeSerializer::class)
enum class LinkedIdTypeJson(val value: UInt) {
    // region LOGIN
    /**
     * The field is linked to the login's username.
     */
    @SerialName("100")
    LOGIN_USERNAME(value = 100U),

    /**
     * The field is linked to the login's password.
     */
    @SerialName("101")
    LOGIN_PASSWORD(value = 101U),
    // endregion LOGIN

    // region CARD
    /**
     * The field is linked to the card's cardholder name.
     */
    @SerialName("300")
    CARD_CARDHOLDER_NAME(value = 300U),

    /**
     * The field is linked to the card's expiration month.
     */
    @SerialName("301")
    CARD_EXP_MONTH(value = 301U),

    /**
     * The field is linked to the card's expiration year.
     */
    @SerialName("302")
    CARD_EXP_YEAR(value = 302U),

    /**
     * The field is linked to the card's code.
     */
    @SerialName("303")
    CARD_CODE(value = 303U),

    /**
     * The field is linked to the card's brand.
     */
    @SerialName("304")
    CARD_BRAND(value = 304U),

    /**
     * The field is linked to the card's number.
     */
    @SerialName("305")
    CARD_NUMBER(value = 305U),
    // endregion CARD

    // region IDENTITY
    /**
     * The field is linked to the identity's title.
     */
    @SerialName("400")
    IDENTITY_TITLE(value = 400U),

    /**
     * The field is linked to the identity's middle name.
     */
    @SerialName("401")
    IDENTITY_MIDDLE_NAME(value = 401U),

    /**
     * The field is linked to the identity's address line 1.
     */
    @SerialName("402")
    IDENTITY_ADDRESS_1(value = 402U),

    /**
     * The field is linked to the identity's address line 2.
     */
    @SerialName("403")
    IDENTITY_ADDRESS_2(value = 403U),

    /**
     * The field is linked to the identity's address line 3.
     */
    @SerialName("404")
    IDENTITY_ADDRESS_3(value = 404U),

    /**
     * The field is linked to the identity's city.
     */
    @SerialName("405")
    IDENTITY_CITY(value = 405U),

    /**
     * The field is linked to the identity's state.
     */
    @SerialName("406")
    IDENTITY_STATE(value = 406U),

    /**
     * The field is linked to the identity's postal code
     */
    @SerialName("407")
    IDENTITY_POSTAL_CODE(value = 407U),

    /**
     * The field is linked to the identity's country.
     */
    @SerialName("408")
    IDENTITY_COUNTRY(value = 408U),

    /**
     * The field is linked to the identity's company.
     */
    @SerialName("409")
    IDENTITY_COMPANY(value = 409U),

    /**
     * The field is linked to the identity's email.
     */
    @SerialName("410")
    IDENTITY_EMAIL(value = 410U),

    /**
     * The field is linked to the identity's phone.
     */
    @SerialName("411")
    IDENTITY_PHONE(value = 411U),

    /**
     * The field is linked to the identity's SSN.
     */
    @SerialName("412")
    IDENTITY_SSN(value = 412U),

    /**
     * The field is linked to the identity's username.
     */
    @SerialName("413")
    IDENTITY_USERNAME(value = 413U),

    /**
     * The field is linked to the identity's passport number.
     */
    @SerialName("414")
    IDENTITY_PASSPORT_NUMBER(value = 414U),

    /**
     * The field is linked to the identity's license number.
     */
    @SerialName("415")
    IDENTITY_LICENSE_NUMBER(value = 415U),

    /**
     * The field is linked to the identity's first name.
     */
    @SerialName("416")
    IDENTITY_FIRST_NAME(value = 416U),

    /**
     * The field is linked to the identity's last name.
     */
    @SerialName("417")
    IDENTITY_LAST_NAME(value = 417U),

    /**
     * The field is linked to the identity's full name.
     */
    @SerialName("418")
    IDENTITY_FULL_NAME(value = 418U),
    // endregion IDENTITY
}

@Keep
private class LinkedIdTypeSerializer :
    BaseEnumeratedIntSerializer<LinkedIdTypeJson>(LinkedIdTypeJson.entries.toTypedArray())
