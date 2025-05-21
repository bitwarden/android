package com.x8bit.bitwarden.data.auth.manager.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

/**
 * Represents a Login Approval request.
 *
 * @param id The id of this request.
 * @param publicKey The user's public key.
 * @param platform The platform from which this request was sent.
 * @param ipAddress The IP address of the device from which this request was sent.
 * @param key The key of this request.
 * @param masterPasswordHash The hash for this user's master password.
 * @param creationDate The date & time on which this request was created.
 * @param responseDate The date & time on which this request was responded to.
 * @param requestApproved Whether this request was approved.
 * @param originUrl The origin URL of this auth request.
 * @param fingerprint The fingerprint of this auth request.
 */
@Parcelize
data class AuthRequest(
    val id: String,
    val publicKey: String,
    val platform: String,
    val ipAddress: String,
    val key: String?,
    val masterPasswordHash: String?,
    val creationDate: ZonedDateTime,
    val responseDate: ZonedDateTime?,
    val requestApproved: Boolean,
    val originUrl: String,
    val fingerprint: String,
) : Parcelable
