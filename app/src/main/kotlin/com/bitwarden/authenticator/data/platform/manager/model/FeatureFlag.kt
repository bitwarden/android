package com.bitwarden.authenticator.data.platform.manager.model

/**
 * Represents feature flags used by the application.
 *
 * @property name The string value of the given key.
 * @property defaultValue The value to be used if the flags value cannot be determined or is not
 * remotely configured.
 * @property isRemotelyConfigured Indicates if the flag should respect the network value or not.
 */
sealed class FeatureFlag<out T : Any>(
    val name: String,
    val defaultValue: T,
    val isRemotelyConfigured: Boolean,
)

/**
 * Models feature flags that are managed locally. E.g., [isRemotelyConfigured] is `false`.
 */
sealed class LocalFeatureFlag<out T : Any>(
    name: String,
    defaultValue: T,
) : FeatureFlag<T>(name, defaultValue, isRemotelyConfigured = false) {

    /**
     * Indicates the state of Bitwarden authentication.
     */
    data object BitwardenAuthenticationEnabled : LocalFeatureFlag<Boolean>(
        name = "bitwarden-authentication-enabled",
        defaultValue = false,
    )
}

/**
 * Models feature flags that are managed remotely. E.g., [isRemotelyConfigured] is `true`.
 */
sealed class RemoteFeatureFlag<out T : Any>(
    name: String,
    defaultValue: T,
) : FeatureFlag<T>(name, defaultValue, isRemotelyConfigured = true)
