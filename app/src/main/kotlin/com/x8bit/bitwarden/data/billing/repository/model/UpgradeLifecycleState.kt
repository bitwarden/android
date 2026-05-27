package com.x8bit.bitwarden.data.billing.repository.model

/**
 * Represents the active user's position in the Premium upgrade lifecycle.
 *
 * Transitions:
 * - [Free] → [UpgradePending] when the user completes Stripe checkout and the post-checkout
 *   sync still reports the user as non-premium — checkout is done, backend reconciliation
 *   is in flight.
 * - [UpgradePending] → [Premium] when the server flips `isPremium` to `true`.
 *
 * Cancellation, expiration, and other terminal substates are surfaced via
 * [Premium.subscriptionStatus] rather than as separate leaves.
 */
sealed class UpgradeLifecycleState {

    /**
     * The user has no Premium subscription and no upgrade is in flight.
     */
    data object Free : UpgradeLifecycleState()

    /**
     * Stripe checkout completed but the server has not yet flipped `isPremium`.
     */
    data object UpgradePending : UpgradeLifecycleState()

    /**
     * The user holds Premium; [subscriptionStatus] carries the substate (active, canceled, etc).
     */
    data class Premium(
        val subscriptionStatus: SubscriptionStatusState,
    ) : UpgradeLifecycleState()
}
