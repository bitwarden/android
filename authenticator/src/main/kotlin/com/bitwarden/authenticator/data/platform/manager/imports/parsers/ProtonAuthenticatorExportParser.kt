package com.bitwarden.authenticator.data.platform.manager.imports.parsers

import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.bitwarden.authenticator.data.platform.manager.BitwardenEncodingManager
import com.bitwarden.authenticator.data.platform.manager.imports.model.ExportParseResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ProtonAuthenticatorJsonExport
import com.bitwarden.core.data.manager.UuidManager
import com.bitwarden.ui.platform.base.util.isBase32
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.URLDecoder

private const val SUPPORTED_VERSION = 1
private const val ENTRY_TYPE_TOTP = "Totp"
private const val ENTRY_TYPE_STEAM = "Steam"
private const val STEAM_ISSUER = "Steam"
private const val TOTP_SCHEME = "otpauth"
private const val TOTP_HOST = "totp"
private const val SECRET_PADDING = '='
private val SECRET_SEPARATORS = setOf(' ', '-', '_')
private val STRICT_BASE32_REGEX = "^[A-Z2-7]+$".toRegex()

/**
 * An [ExportParser] responsible for transforming Proton Authenticator export files into Bitwarden
 * Authenticator items.
 *
 * Secrets are converted the way Proton Authenticator reads them, so imported items generate the
 * same codes. Entries of an unknown type are skipped. A TOTP or Steam entry that cannot be read
 * fails the whole import.
 */
class ProtonAuthenticatorExportParser(
    private val uuidManager: UuidManager,
    private val encodingManager: BitwardenEncodingManager,
) : ExportParser() {
    @OptIn(ExperimentalSerializationApi::class)
    override fun parse(byteArray: ByteArray): ExportParseResult {
        val importJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

        val exportData = importJson.decodeFromStream<ProtonAuthenticatorJsonExport>(
            ByteArrayInputStream(byteArray),
        )
        val entries = exportData.entries

        return when {
            exportData.version != SUPPORTED_VERSION -> fileCouldNotBeProcessedError()

            entries == null && exportData.salt != null -> {
                ExportParseResult.Error(
                    message = BitwardenString
                        .import_proton_authenticator_password_protected_not_supported
                        .asText(),
                )
            }

            entries == null -> {
                ExportParseResult.Error(
                    title = BitwardenString.required_information_missing.asText(),
                    message = BitwardenString.required_information_missing_message.asText(),
                )
            }

            else -> {
                val items = entries.mapNotNull { entry ->
                    when {
                        entry.content.entryType.equals(ENTRY_TYPE_TOTP, ignoreCase = true) -> {
                            entry.toTotpOrNull() ?: return fileCouldNotBeProcessedError()
                        }

                        entry.content.entryType.equals(ENTRY_TYPE_STEAM, ignoreCase = true) -> {
                            entry.toSteamOrNull() ?: return fileCouldNotBeProcessedError()
                        }

                        else -> null
                    }
                }
                ExportParseResult.Success(items = items)
            }
        }
    }

    private fun ProtonAuthenticatorJsonExport.Entry.toTotpOrNull(): AuthenticatorItemEntity? {
        val uri = content.uri.toTotpUriOrNull() ?: return null
        val params = uri.rawQuery?.toQueryParametersOrNull() ?: return null

        val secret = params[TotpCodeManager.SECRET_PARAM]?.toTotpSecretOrNull() ?: return null
        val algorithm = params[TotpCodeManager.ALGORITHM_PARAM]
            ?.let { AuthenticatorItemAlgorithm.fromStringOrNull(it) ?: return null }
            ?: TotpCodeManager.ALGORITHM_DEFAULT
        val digits = params[TotpCodeManager.DIGITS_PARAM]
            ?.let { value ->
                value
                    .toIntOrNull()
                    ?.takeIf { it in TotpCodeManager.TOTP_DIGITS_RANGE }
                    ?: return null
            }
            ?: TotpCodeManager.TOTP_DIGITS_DEFAULT
        val period = params[TotpCodeManager.PERIOD_PARAM]
            ?.let { value -> value.toIntOrNull()?.takeIf { it > 0 } ?: return null }
            ?: TotpCodeManager.PERIOD_SECONDS_DEFAULT

        val label = uri.path.orEmpty().removePrefix("/").trim()
        val name = content.name?.trim().orEmpty()
        val issuer = params[TotpCodeManager.ISSUER_PARAM]
            ?.trim()
            .orEmpty()
            .ifEmpty { name }
            .ifEmpty { label }
        val accountName = label
            .removePrefix("$issuer:")
            .trim()
            .ifEmpty { name }
            .takeUnless { it.isEmpty() || it == issuer }

        return AuthenticatorItemEntity(
            id = uuidManager.generateUuid(),
            key = secret,
            type = AuthenticatorItemType.TOTP,
            algorithm = algorithm,
            period = period,
            digits = digits,
            issuer = issuer,
            userId = null,
            accountName = accountName,
            favorite = false,
        )
    }

    private fun ProtonAuthenticatorJsonExport.Entry.toSteamOrNull(): AuthenticatorItemEntity? {
        if (!content.uri.startsWith(TotpCodeManager.STEAM_CODE_PREFIX, ignoreCase = true)) {
            return null
        }
        val secret = content.uri
            .substring(TotpCodeManager.STEAM_CODE_PREFIX.length)
            .toSteamSecretOrNull()
            ?: return null

        return AuthenticatorItemEntity(
            id = uuidManager.generateUuid(),
            key = secret,
            type = AuthenticatorItemType.STEAM,
            algorithm = TotpCodeManager.ALGORITHM_DEFAULT,
            period = TotpCodeManager.PERIOD_SECONDS_DEFAULT,
            digits = TotpCodeManager.STEAM_DIGITS_DEFAULT,
            issuer = STEAM_ISSUER,
            userId = null,
            accountName = content.name?.trim()?.takeUnless { it.isEmpty() },
            favorite = false,
        )
    }

    private fun String.toTotpSecretOrNull(): String? {
        val sanitized = filterNot { it in SECRET_SEPARATORS }
            .trimEnd(SECRET_PADDING)
            .map { if (it in 'a'..'z') it.uppercaseChar() else it }
            .joinToString(separator = "")
        return when {
            sanitized.isEmpty() -> null
            sanitized.isBase32() -> sanitized
            else -> sanitized.toByteArray(Charsets.UTF_8).toBase32Secret()
        }
    }

    private fun String.toSteamSecretOrNull(): String? {
        val trimmed = trim().trimEnd(SECRET_PADDING)
        if (STRICT_BASE32_REGEX.matches(trimmed)) return trimmed
        return runCatching { encodingManager.base64Decode(trimmed) }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?.toBase32Secret()
    }

    private fun ByteArray.toBase32Secret(): String =
        encodingManager.base32Encode(this).trimEnd(SECRET_PADDING)
}

private fun fileCouldNotBeProcessedError(): ExportParseResult.Error =
    ExportParseResult.Error(
        title = BitwardenString.file_could_not_be_processed.asText(),
        message = BitwardenString.file_could_not_be_processed_message.asText(),
    )

private fun String.toTotpUriOrNull(): URI? =
    runCatching { URI(this) }
        .getOrNull()
        ?.takeIf {
            it.scheme.equals(TOTP_SCHEME, ignoreCase = true) &&
                it.host.equals(TOTP_HOST, ignoreCase = true)
        }

private fun String.toQueryParametersOrNull(): Map<String, String>? = runCatching {
    split("&")
        .filter { it.isNotEmpty() }
        .associate { pair ->
            val key = pair.substringBefore("=")
            val value = pair.substringAfter("=", missingDelimiterValue = "")
            URLDecoder.decode(key, Charsets.UTF_8.name()) to
                URLDecoder.decode(value, Charsets.UTF_8.name())
        }
}.getOrNull()
