package com.x8bit.bitwarden.data.platform.datasource.network.util

import okio.ByteString.Companion.decodeBase64
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.security.cert.CertPathValidatorException
import java.util.Base64
import javax.net.ssl.SSLHandshakeException

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

/**
 * Base 64 decode the given string after making the following replacements:
 *
 * - replace all "-" with "+"
 * - replace all "_" with "/"
 *
 * A value of `null` will be returned if the decoding fails.
 */
fun String.base64UrlDecodeOrNull(): String? =
    this
        .replace("-", "+")
        .replace("_", "/")
        .decodeBase64()
        ?.string(Charset.defaultCharset())

/**
 * Returns true if the throwable represents a no network error.
 */
fun Throwable?.isNoConnectionError(): Boolean {
    return this is UnknownHostException ||
        this?.cause?.isNoConnectionError() ?: false
}

/**
 * Returns true if the throwable represents a SSL handshake error.
 */
fun Throwable?.isSslHandShakeError(): Boolean {
    return this is SSLHandshakeException ||
        this is CertPathValidatorException ||
        this?.cause?.isSslHandShakeError() ?: false
}
