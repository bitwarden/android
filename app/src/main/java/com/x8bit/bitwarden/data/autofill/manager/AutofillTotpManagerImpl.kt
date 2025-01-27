package com.x8bit.bitwarden.data.autofill.manager

import android.content.Context
import android.widget.Toast
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import java.time.Clock

/**
 * Default implementation of the [AutofillTotpManager].
 */
class AutofillTotpManagerImpl(
    private val context: Context,
    private val clock: Clock,
    private val clipboardManager: BitwardenClipboardManager,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository,
) : AutofillTotpManager {
    override suspend fun tryCopyTotpToClipboard(cipherView: CipherView) {
        if (settingsRepository.isAutoCopyTotpDisabled) return
        val isPremium = authRepository.userStateFlow.value?.activeAccount?.isPremium == true
        if (!isPremium && !cipherView.organizationUseTotp) return
        val totpCode = cipherView.login?.totp ?: return

        val totpResult = vaultRepository.generateTotp(
            time = clock.instant(),
            totpCode = totpCode,
        )

        if (totpResult is GenerateTotpResult.Success) {
            clipboardManager.setText(text = totpResult.code)
            Toast
                .makeText(
                    context.applicationContext,
                    R.string.verification_code_totp,
                    Toast.LENGTH_LONG,
                )
                .show()
        }
    }
}
