package com.x8bit.bitwarden.data.pam.repository.model

/**
 * Models the result of requesting access to a gated item.
 */
sealed class RequestPamAccessResult {
    /**
     * The request was submitted.
     */
    data object Success : RequestPamAccessResult()

    /**
     * The request was rejected as invalid.
     *
     * @property message The message describing the problem.
     */
    data class ValidationError(val message: String?) : RequestPamAccessResult()

    /**
     * The request conflicts with the current state of the item (for example it already has a
     * pending request or another member holds the lease).
     *
     * @property message The message describing the conflict.
     */
    data class Conflict(val message: String?) : RequestPamAccessResult()

    /**
     * There was an unexpected error submitting the request.
     */
    data class Error(val error: Throwable) : RequestPamAccessResult()
}
