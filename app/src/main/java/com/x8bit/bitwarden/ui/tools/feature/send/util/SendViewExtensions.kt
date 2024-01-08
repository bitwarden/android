package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.bitwarden.core.SendView

/**
 * Creates a sharable url from a [SendView].
 */
fun SendView.toSendUrl(
    baseWebSendUrl: String,
): String = "$baseWebSendUrl$accessId/$key"
