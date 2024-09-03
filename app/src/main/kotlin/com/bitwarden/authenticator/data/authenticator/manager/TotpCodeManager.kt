package com.bitwarden.authenticator.data.authenticator.manager

import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.platform.repository.model.DataState
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the flows for getting verification codes.
 */
interface TotpCodeManager {

    /**
     * Flow for getting a DataState with multiple verification code items.
     */
    fun getTotpCodesStateFlow(
        itemList: List<AuthenticatorItemEntity>,
    ): StateFlow<DataState<List<VerificationCodeItem>>>

    /**
     * Flow for getting a DataState with a single verification code item.
     */
    fun getTotpCodeStateFlow(
        item: AuthenticatorItemEntity,
    ): StateFlow<DataState<VerificationCodeItem?>>

    companion object {
        const val ALGORITHM_PARAM = "algorithm"
        const val DIGITS_PARAM = "digits"
        const val PERIOD_PARAM = "period"
        const val SECRET_PARAM = "secret"
        const val ISSUER_PARAM = "issuer"

        /**
         * URI query parameter containing export data from Google Authenticator.
         */
        const val DATA_PARAM = "data"
        const val TOTP_CODE_PREFIX = "otpauth://totp"
        const val STEAM_CODE_PREFIX = "steam://"
        const val GOOGLE_EXPORT_PREFIX = "otpauth-migration://"
        const val TOTP_DIGITS_DEFAULT = 6
        const val TOTP_DIGITS_MIN = 5
        const val TOTP_DIGITS_MAX = 10
        const val STEAM_DIGITS_DEFAULT = 5
        const val PERIOD_SECONDS_DEFAULT = 30
        val TOTP_DIGITS_RANGE = TOTP_DIGITS_MIN..TOTP_DIGITS_MAX
        val ALGORITHM_DEFAULT = AuthenticatorItemAlgorithm.SHA1
    }
}
