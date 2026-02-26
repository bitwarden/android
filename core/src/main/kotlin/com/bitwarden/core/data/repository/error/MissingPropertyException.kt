package com.bitwarden.core.data.repository.error

/**
 * An exception indicating that a required property was missing.
 */
class MissingPropertyException(
    propertyName: String,
) : IllegalStateException("Missing the required $propertyName property")
