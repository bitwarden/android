package com.x8bit.bitwarden.data.auth.manager.util

import com.bitwarden.network.model.AuthRequestsResponseJson
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest

/**
 * Converts the given [AuthRequestsResponseJson.AuthRequest] to an [AuthRequest], given the
 * [fingerprint] that the response itself does not carry.
 */
fun AuthRequestsResponseJson.AuthRequest.toAuthRequest(
    fingerprint: String,
): AuthRequest = AuthRequest(
    id = id,
    publicKey = publicKey,
    platform = platform,
    ipAddress = ipAddress,
    key = key,
    masterPasswordHash = masterPasswordHash,
    creationDate = creationDate,
    responseDate = responseDate,
    requestApproved = requestApproved ?: false,
    originUrl = originUrl,
    fingerprint = fingerprint,
)
