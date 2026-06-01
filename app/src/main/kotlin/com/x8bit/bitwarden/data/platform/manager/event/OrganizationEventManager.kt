package com.x8bit.bitwarden.data.platform.manager.event

import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent

/**
 * A manager for tracking events.
 */
interface OrganizationEventManager {
    /**
     * Tracks a specific event to be uploaded at a different time.
     */
    fun trackEvent(event: OrganizationEvent)
}
