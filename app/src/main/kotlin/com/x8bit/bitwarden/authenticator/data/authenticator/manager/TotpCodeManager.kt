package com.x8bit.bitwarden.authenticator.data.authenticator.manager

import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.authenticator.data.platform.repository.model.DataState
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the flows for getting verification codes.
 */
interface TotpCodeManager {

    /**
     * Flow for getting a DataState with multiple verification code items.
     */
    fun getTotpCodesStateFlow(
        cipherList: List<CipherView>,
    ): StateFlow<DataState<List<VerificationCodeItem>>>

    /**
     * Flow for getting a DataState with a single verification code item.
     */
    fun getTotpCodeStateFlow(
        cipher: CipherView,
    ): StateFlow<DataState<VerificationCodeItem?>>
}
