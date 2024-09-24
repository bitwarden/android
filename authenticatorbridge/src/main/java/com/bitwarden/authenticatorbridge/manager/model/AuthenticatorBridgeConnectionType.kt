package com.bitwarden.authenticatorbridge.manager.model

import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager

/**
 * Models different connection types for [AuthenticatorBridgeManager].
 */
enum class AuthenticatorBridgeConnectionType {

    /**
     * Connect to release build variant.
     */
    RELEASE,

    /**
     * Connect to dev build variant.
     */
    DEV,
}

/**
 * Convert a [AuthenticatorBridgeConnectionType] to raw package name for connection.
 */
internal fun AuthenticatorBridgeConnectionType.toPackageName() =
    when (this) {
        AuthenticatorBridgeConnectionType.RELEASE -> "com.x8bit.bitwarden"
        AuthenticatorBridgeConnectionType.DEV -> "com.x8bit.bitwarden.dev"
    }
