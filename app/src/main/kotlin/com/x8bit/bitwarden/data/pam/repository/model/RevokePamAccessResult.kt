package com.x8bit.bitwarden.data.pam.repository.model

/**
 * Models the result of an approver revoking access that was granted.
 */
sealed class RevokePamAccessResult {
    /**
     * The access was revoked.
     */
    data object Success : RevokePamAccessResult()

    /**
     * The access had already ended, so there was nothing left to revoke.
     */
    data object AlreadyEnded : RevokePamAccessResult()

    /**
     * The active user may not revoke this access, for example because it is their own or the
     * request produced no active lease.
     */
    data object NotAllowed : RevokePamAccessResult()

    /**
     * The access could not be revoked.
     */
    data class Error(val error: Throwable) : RevokePamAccessResult()
}
