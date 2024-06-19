package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the flows for getting verification codes.
 */
interface TotpCodeManager {

    /**
     * Flow for getting a DataState with multiple verification code items.
     */
    fun getTotpCodesStateFlow(
        userId: String,
        cipherList: List<CipherView>,
    ): StateFlow<DataState<List<VerificationCodeItem>>>

    /**
     * Flow for getting a DataState with a single verification code item.
     */
    fun getTotpCodeStateFlow(
        userId: String,
        cipher: CipherView,
    ): StateFlow<DataState<VerificationCodeItem?>>
}
