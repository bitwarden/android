package com.x8bit.bitwarden.data.platform.service

import android.app.Service
import android.content.Intent
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.processor.AuthenticatorBridgeProcessor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Service exposed via a custom permission
 */
@AndroidEntryPoint
@OmitFromCoverage
class AuthenticatorBridgeService : Service() {

    @Inject
    lateinit var authenticatorBridgeProcessor: AuthenticatorBridgeProcessor

    /**
     * When binding this service, delegate logic to the [AuthenticatorBridgeProcessor].
     *
     * Note that [AuthenticatorBridgeProcessor.binder] can return a null binder, which the OS
     * will accept but never connect to, effectively making a null binder a noop binder.
     */
    override fun onBind(intent: Intent) = authenticatorBridgeProcessor.binder
}
