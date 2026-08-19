package com.x8bit.bitwarden.data.auth.manager.util

import com.bitwarden.network.model.AuthRequestsResponseJson
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest
import java.time.Instant

/**
 * Converts the given [AuthRequestsResponseJson.AuthRequest] to an [AuthRequest], given the
 * [fingerprint] that the response itself does not carry.
 *
 * The [publicKey], [responseDate], and [isRequestApproved] are supplied by the caller.
 */
fun AuthRequestsResponseJson.AuthRequest.toAuthRequest(
    fingerprint: String,
    publicKey: String,
    responseDate: Instant?,
    isRequestApproved: Boolean,
): AuthRequest = AuthRequest(
    id = this.id,
    publicKey = publicKey,
    platform = this.platform,
    ipAddress = this.ipAddress,
    key = this.key,
    masterPasswordHash = this.masterPasswordHash,
    creationDate = this.creationDate,
    responseDate = responseDate,
    requestApproved = isRequestApproved,
    originUrl = this.originUrl,
    fingerprint = fingerprint,
)
