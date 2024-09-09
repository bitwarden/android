package com.x8bit.bitwarden.data.platform.service

import android.app.Service
import android.content.Intent
import com.x8bit.bitwarden.data.platform.manager.BridgeServiceProcessor
import com.bitwarden.bridge.IBridgeService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Service exposed via a custom permission
 */
@AndroidEntryPoint
class BridgeService : Service() {

    @Inject
    lateinit var bridgeServiceProcessor: BridgeServiceProcessor

    /**
     * When binding this service, logic to the [BridgeServiceProcessor], which implements
     * [IBridgeService].
     *
     * Note that [BridgeServiceProcessor.binder] can return a null binder, which the OS will accept
     * but never connect to, effectively making a null binder a noop binder.
     */
    override fun onBind(intent: Intent) = bridgeServiceProcessor.binder
}
