package com.x8bit.bitwarden.data.platform.datasource.network.util

import java.util.Base64

/**
 * Base 64 encode the string as well as make special modifications required by the backend:
 *
 * - replace all "+" with "-"
 * - replace all "/" with "_"
 * - replace all "=" with ""
 */
fun String.base64UrlEncode(): String =
    Base64.getEncoder()
        .encodeToString(toByteArray())
        .replace("+", "-")
        .replace("/", "_")
        .replace("=", "")
