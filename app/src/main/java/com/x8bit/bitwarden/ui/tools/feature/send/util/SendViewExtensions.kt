package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.bitwarden.core.SendView

/**
 * Creates a sharable url from a [SendView].
 */
fun SendView.toSendUrl(
    baseWebSendUrl: String,
    // TODO: The `key` being used here is not correct and should be updated (BIT-1386)
): String = "$baseWebSendUrl$accessId/$key"
