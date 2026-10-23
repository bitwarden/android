package com.x8bit.bitwarden.data.pam.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * Tracks whether the active user can decide on other members' privileged access requests (PAM
 * approver) and how many requests are waiting for them, plus how many requests of their own are
 * still current.
 */
interface PamApprovalManager {

    /**
     * Whether the active user is an approver: the PAM mobile feature flag is on and the user has
     * Manage on a collection of a PAM-enabled organization (provider users are excluded).
     */
    val canApproveFlow: StateFlow<Boolean>

    /**
     * The number of requests waiting for the active user's decision. Always zero for users who
     * cannot approve.
     */
    val pendingCountFlow: StateFlow<Int>

    /**
     * Reloads [pendingCountFlow] from the server. Does nothing for users who cannot approve.
     */
    fun refreshPendingCount()

    /**
     * Whether the active user can make and see their own privileged access requests: the PAM
     * mobile feature flag is on and the user belongs to a PAM-enabled organization (provider users
     * are excluded).
     */
    val hasPamAccessFlow: StateFlow<Boolean>

    /**
     * The number of the active user's own access requests that are pending, approved and ready to
     * start, or active. Always zero for users without PAM access.
     */
    val myRequestCountFlow: StateFlow<Int>

    /**
     * Reloads [myRequestCountFlow] from the server. Does nothing for users without PAM access.
     */
    fun refreshMyRequestCount()
}
