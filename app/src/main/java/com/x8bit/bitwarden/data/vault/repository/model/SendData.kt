package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.send.SendView

/**
 * Represents decrypted send data.
 *
 * @param sendViewList List of decrypted sends.
 */
data class SendData(
    val sendViewList: List<SendView>,
)
