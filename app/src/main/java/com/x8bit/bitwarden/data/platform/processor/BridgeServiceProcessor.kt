package com.x8bit.bitwarden.data.platform.processor

import com.bitwarden.bridge.IBridgeService

/**
 * Provides access to [IBridgeService] APIs in an injectable and testable manner.
 */
interface BridgeServiceProcessor {

    /**
     * Binder that implements [IBridgeService]. Null can be returned to represent a no-op binder.
     */
    val binder: IBridgeService.Stub?
}
