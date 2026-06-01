package com.x8bit.bitwarden.data.autofill.manager

import com.bitwarden.vault.CipherView

/**
 * Manages copying the totp code to the clipboard for autofill.
 */
interface AutofillTotpManager {
    /**
     * Attempt to copy the totp code to clipboard. If it succeeds show a toast.
     */
    suspend fun tryCopyTotpToClipboard(cipherView: CipherView)
}
