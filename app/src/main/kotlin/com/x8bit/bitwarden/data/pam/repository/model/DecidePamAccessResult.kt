package com.x8bit.bitwarden.data.pam.repository.model

/**
 * Models the result of deciding on an access request.
 */
sealed class DecidePamAccessResult {
    /**
     * The decision was recorded.
     */
    data object Success : DecidePamAccessResult()

    /**
     * The request was already decided (or cancelled) by someone else.
     */
    data object AlreadyDecided : DecidePamAccessResult()

    /**
     * The decision was not accepted because the active user may not decide on this request, for
     * example because it is their own.
     */
    data object NotAllowed : DecidePamAccessResult()

    /**
     * The decision could not be sent.
     */
    data class Error(val error: Throwable) : DecidePamAccessResult()
}
