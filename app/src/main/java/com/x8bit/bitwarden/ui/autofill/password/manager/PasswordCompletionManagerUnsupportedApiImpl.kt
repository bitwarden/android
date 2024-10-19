package com.x8bit.bitwarden.ui.autofill.password.manager

import com.x8bit.bitwarden.data.autofill.password.model.PasswordGetCredentialsResult
import com.x8bit.bitwarden.data.autofill.password.model.PasswordRegisterCredentialResult

/**
 * A no-op implementation of [PasswordCompletionManagerImpl] provided when the build version is below
 * UPSIDE_DOWN_CAKE (34). These versions do not support [androidx.credentials.CredentialProvider].
 */
object PasswordCompletionManagerUnsupportedApiImpl : PasswordCompletionManager {
    override fun completePasswordRegistration(result: PasswordRegisterCredentialResult) = Unit

    override fun completePasswordGetCredentialRequest(result: PasswordGetCredentialsResult) = Unit
}
