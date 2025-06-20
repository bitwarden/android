package com.x8bit.bitwarden.ui.tools.feature.send.model

import kotlinx.serialization.Serializable

/**
 * Represents different types of sends that can be added/viewed.
 */
@Serializable
enum class SendItemType {
    FILE,
    TEXT,
}
