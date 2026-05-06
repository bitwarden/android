package com.bitwarden.network.model

import androidx.annotation.Keep
import com.bitwarden.core.data.serializer.BaseEnumeratedIntSerializer
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

    // region BANK_ACCOUNT
    /**
     * The field is linked to the bank account's bank name.
     */
    @SerialName("600")
    BANK_ACCOUNT_BANK_NAME(value = 600U),

    /**
     * The field is linked to the bank account's name on account.
     */
    @SerialName("601")
    BANK_ACCOUNT_NAME_ON_ACCOUNT(value = 601U),

    /**
     * The field is linked to the bank account's account type.
     */
    @SerialName("602")
    BANK_ACCOUNT_ACCOUNT_TYPE(value = 602U),

    /**
     * The field is linked to the bank account's account number.
     */
    @SerialName("603")
    BANK_ACCOUNT_ACCOUNT_NUMBER(value = 603U),

    /**
     * The field is linked to the bank account's routing number.
     */
    @SerialName("604")
    BANK_ACCOUNT_ROUTING_NUMBER(value = 604U),

    /**
     * The field is linked to the bank account's branch number.
     */
    @SerialName("605")
    BANK_ACCOUNT_BRANCH_NUMBER(value = 605U),

    /**
     * The field is linked to the bank account's PIN.
     */
    @SerialName("606")
    BANK_ACCOUNT_PIN(value = 606U),

    /**
     * The field is linked to the bank account's SWIFT code.
     */
    @SerialName("607")
    BANK_ACCOUNT_SWIFT_CODE(value = 607U),

    /**
     * The field is linked to the bank account's IBAN.
     */
    @SerialName("608")
    BANK_ACCOUNT_IBAN(value = 608U),

    /**
     * The field is linked to the bank account's contact phone.
     */
    @SerialName("609")
    BANK_ACCOUNT_BANK_CONTACT_PHONE(value = 609U),
    // endregion BANK_ACCOUNT

    // region DRIVERS_LICENSE
    /**
     * The field is linked to the driver's license first name.
     */
    @SerialName("700")
    DRIVERS_LICENSE_FIRST_NAME(value = 700U),

    /**
     * The field is linked to the driver's license middle name.
     */
    @SerialName("701")
    DRIVERS_LICENSE_MIDDLE_NAME(value = 701U),

    /**
     * The field is linked to the driver's license last name.
     */
    @SerialName("702")
    DRIVERS_LICENSE_LAST_NAME(value = 702U),

    /**
     * The field is linked to the driver's license number.
     */
    @SerialName("703")
    DRIVERS_LICENSE_LICENSE_NUMBER(value = 703U),

    /**
     * The field is linked to the driver's license issuing country.
     */
    @SerialName("704")
    DRIVERS_LICENSE_ISSUING_COUNTRY(value = 704U),

    /**
     * The field is linked to the driver's license issuing state.
     */
    @SerialName("705")
    DRIVERS_LICENSE_ISSUING_STATE(value = 705U),

    /**
     * The field is linked to the driver's license expiration month.
     */
    @SerialName("706")
    DRIVERS_LICENSE_EXPIRATION_MONTH(value = 706U),

    /**
     * The field is linked to the driver's license expiration year.
     */
    @SerialName("707")
    DRIVERS_LICENSE_EXPIRATION_YEAR(value = 707U),

    /**
     * The field is linked to the driver's license class.
     */
    @SerialName("708")
    DRIVERS_LICENSE_LICENSE_CLASS(value = 708U),
    // endregion DRIVERS_LICENSE

    // region PASSPORT
    /**
     * The field is linked to the passport surname.
     */
    @SerialName("800")
    PASSPORT_SURNAME(value = 800U),

    /**
     * The field is linked to the passport given name.
     */
    @SerialName("801")
    PASSPORT_GIVEN_NAME(value = 801U),

    /**
     * The field is linked to the passport date of birth month.
     */
    @SerialName("802")
    PASSPORT_DOB_MONTH(value = 802U),

    /**
     * The field is linked to the passport date of birth year.
     */
    @SerialName("803")
    PASSPORT_DOB_YEAR(value = 803U),

    /**
     * The field is linked to the passport nationality.
     */
    @SerialName("804")
    PASSPORT_NATIONALITY(value = 804U),

    /**
     * The field is linked to the passport number.
     */
    @SerialName("805")
    PASSPORT_PASSPORT_NUMBER(value = 805U),

    /**
     * The field is linked to the passport type.
     */
    @SerialName("806")
    PASSPORT_PASSPORT_TYPE(value = 806U),

    /**
     * The field is linked to the passport issuing country.
     */
    @SerialName("807")
    PASSPORT_ISSUING_COUNTRY(value = 807U),

    /**
     * The field is linked to the passport issuing authority.
     */
    @SerialName("808")
    PASSPORT_ISSUING_AUTHORITY(value = 808U),

    /**
     * The field is linked to the passport issue month.
     */
    @SerialName("809")
    PASSPORT_ISSUE_MONTH(value = 809U),

    /**
     * The field is linked to the passport issue year.
     */
    @SerialName("810")
    PASSPORT_ISSUE_YEAR(value = 810U),

    /**
     * The field is linked to the passport expiration month.
     */
    @SerialName("811")
    PASSPORT_EXPIRATION_MONTH(value = 811U),

    /**
     * The field is linked to the passport expiration year.
     */
    @SerialName("812")
    PASSPORT_EXPIRATION_YEAR(value = 812U),
    // endregion PASSPORT
}

@Keep
private class LinkedIdTypeSerializer : BaseEnumeratedIntSerializer<LinkedIdTypeJson>(
    className = "LinkedIdTypeJson",
    values = LinkedIdTypeJson.entries.toTypedArray(),
)
