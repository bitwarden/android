package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Exception thrown when there is decrypt key found for a user when it would otherwise be
 * expected to be present.
 */
class NoKeyFoundForUserException : IllegalStateException("No key found for user.")
