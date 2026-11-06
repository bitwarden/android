package com.x8bit.bitwarden.data.platform.manager.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Required data for the PAM alert pushes. Only ids are carried, never names.
 *
 * @property userId The ID of the user the alert is for.
 * @property requestId The ID of the PAM access request.
 * @property type The kind of alert.
 */
@Parcelize
data class PamAlertData(
    val userId: String,
    val requestId: String,
    val type: PamAlertType,
) : Parcelable

/**
 * The kinds of PAM alert.
 */
enum class PamAlertType {
    /**
     * An approver has a request waiting for their decision.
     */
    ACCESS_REQUESTED,

    /**
     * The requester's access request was approved.
     */
    ACCESS_APPROVED,

    /**
     * The requester's access request was denied.
     */
    ACCESS_DENIED,

    /**
     * The requester's access was revoked by an operator.
     */
    ACCESS_REVOKED,
}
