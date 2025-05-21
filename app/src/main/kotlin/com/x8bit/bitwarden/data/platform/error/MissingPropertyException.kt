package com.x8bit.bitwarden.data.platform.error

/**
 * An exception indicating that a required property was missing.
 */
class MissingPropertyException(
    propertyName: String,
) : IllegalStateException("Missing the required $propertyName property")
