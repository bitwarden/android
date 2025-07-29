package com.bitwarden.ui.platform.util

import com.bitwarden.core.data.manager.BitwardenBuildConfigManager
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

/**
 * Converts a [Throwable] to a shareable error report.
 *
 * Used in conjunction with [BitwardenBasicDialog] to share error details when requested by the
 * user.
 *
 * @param buildConfigManager used to provide version and device information.
 */
fun Throwable.toShareableText(
    buildConfigManager: BitwardenBuildConfigManager,
): Text = StringBuilder()
    .append("Stacktrace:\n")
    .append("$this\n")
    .apply { stackTrace.forEach { append("\t$it\n") } }
    .append("\n")
    .append("Version: ${buildConfigManager.versionData}\n")
    .append("Device: ${buildConfigManager.deviceData}\n")
    .apply { buildConfigManager.ciBuildInfo?.let { append("CI: $it\n") } }
    .append("\n")
    .toString()
    .asText()
