package com.x8bit.bitwarden.ui.platform.util

import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import java.net.URI

/**
 * Returns the [Environment.label] for non-custom values. Otherwise returns the host of the
 * custom base URL.
 */
val Environment.labelOrBaseUrlHost: Text
    get() = when (this) {
        is Environment.Us -> this.label
        is Environment.Eu -> this.label
        is Environment.SelfHosted -> {
            // Grab the domain
            // Ex:
            // - "https://www.abc.com/path-1/path-1" -> "www.abc.com"
            URI
                .create(this.environmentUrlData.base)
                .host
                .orEmpty()
                .asText()
        }
    }
