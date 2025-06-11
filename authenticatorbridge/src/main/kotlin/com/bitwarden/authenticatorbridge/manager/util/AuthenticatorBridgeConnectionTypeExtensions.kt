package com.bitwarden.authenticatorbridge.manager.util

import com.bitwarden.authenticatorbridge.manager.model.AuthenticatorBridgeConnectionType

/**
 * Convert a [AuthenticatorBridgeConnectionType] to raw package name for connection.
 */
internal fun AuthenticatorBridgeConnectionType.toPackageName() =
    when (this) {
        AuthenticatorBridgeConnectionType.RELEASE -> "com.x8bit.bitwarden"
        AuthenticatorBridgeConnectionType.DEV -> "com.x8bit.bitwarden.dev"
    }
