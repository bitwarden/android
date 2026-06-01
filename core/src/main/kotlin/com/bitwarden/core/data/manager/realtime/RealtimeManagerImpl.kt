package com.bitwarden.core.data.manager.realtime

import android.os.SystemClock
import com.bitwarden.annotation.OmitFromCoverage

/**
 * The default implementation of the [RealtimeManager].
 */
@OmitFromCoverage
class RealtimeManagerImpl : RealtimeManager {
    override val elapsedRealtimeMs: Long get() = SystemClock.elapsedRealtime()
}
