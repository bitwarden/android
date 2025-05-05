package com.bitwarden.authenticator.data.authenticator.repository.model

import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem

/**
 * Represents the state of verification codes shared from the main Bitwarden app.
 */
sealed class SharedVerificationCodesState {

    /**
     * The Bitwarden app is not installed and therefore accounts cannot be synced.
     */
    data object AppNotInstalled : SharedVerificationCodesState()

    /**
     * Something went wrong syncing accounts.
     */
    data object Error : SharedVerificationCodesState()

    /**
     * The feature flag for authenticator sync is not enabled.
     */
    data object FeatureNotEnabled : SharedVerificationCodesState()

    /**
     * State is loading.
     */
    data object Loading : SharedVerificationCodesState()

    /**
     * OS version can't support account syncing.
     */
    data object OsVersionNotSupported : SharedVerificationCodesState()

    /**
     * Successfully synced items.
     */
    data class Success(
        val items: List<VerificationCodeItem>,
    ) : SharedVerificationCodesState()

    /**
     * The user needs to enable authenticator syncing from the bitwarden app.
     */
    data object SyncNotEnabled : SharedVerificationCodesState()
}
