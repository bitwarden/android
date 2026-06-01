package com.bitwarden.core.data.manager.realtime

/**
 * An manager interface for accessing the system realtime clock.
 */
interface RealtimeManager {
    /**
     * Returns milliseconds since the device has booted up, this includes time spent in sleep.
     */
    val elapsedRealtimeMs: Long
}
