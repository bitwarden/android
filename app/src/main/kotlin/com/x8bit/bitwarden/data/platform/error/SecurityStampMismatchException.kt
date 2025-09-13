package com.x8bit.bitwarden.data.platform.error

/**
 * An exception indicating that the security stamps for the current user do not match.
 */
class SecurityStampMismatchException : IllegalStateException("Security stamps do not match!")
