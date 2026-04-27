package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the type of bank account as returned by the server.
 */
@Serializable
enum class BankAccountTypeJson {
    /**
     * A standard checking account.
     */
    @SerialName("checking")
    CHECKING,

    /**
     * A standard savings account.
     */
    @SerialName("savings")
    SAVINGS,

    /**
     * A certificate of deposit account.
     */
    @SerialName("certificateOfDeposit")
    CERTIFICATE_OF_DEPOSIT,

    /**
     * A line of credit account.
     */
    @SerialName("lineOfCredit")
    LINE_OF_CREDIT,

    /**
     * An investment or brokerage account.
     */
    @SerialName("investmentBrokerage")
    INVESTMENT_BROKERAGE,

    /**
     * A money market account.
     */
    @SerialName("moneyMarket")
    MONEY_MARKET,

    /**
     * Any other account type not covered by the above.
     */
    @SerialName("other")
    OTHER,
}
