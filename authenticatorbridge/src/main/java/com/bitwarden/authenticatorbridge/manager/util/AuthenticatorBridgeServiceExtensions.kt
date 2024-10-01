package com.bitwarden.authenticatorbridge.manager.util

import com.bitwarden.authenticatorbridge.IAuthenticatorBridgeService

/**
 * Helper function for wrapping all calls to [IAuthenticatorBridgeService] around try catch.
 *
 * This is important because all calls to [IAuthenticatorBridgeService] can throw
 * DeadObjectExceptions as well as RemoteExceptions.
 */
internal fun <T> IAuthenticatorBridgeService?.safeCall(action: IAuthenticatorBridgeService.() -> T): Result<T?> =
    runCatching {
        this?.let { action.invoke(it) }
    }
