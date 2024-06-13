package com.bitwarden.authenticator.data.platform.manager.imports.parsers

import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.platform.manager.imports.model.ExportParseResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.TwoFasJsonExport
import com.bitwarden.authenticator.ui.platform.base.util.asText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.UUID

private const val TOKEN_TYPE_HOTP = "HOTP"

/**
 * An [ExportParser] responsible for transforming 2FAS export files into Bitwarden Authenticator
 * items.
 */
class TwoFasExportParser : ExportParser {
    override fun parse(byteArray: ByteArray): ExportParseResult {
        return import2fasJson(byteArray)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun import2fasJson(byteArray: ByteArray): ExportParseResult {
        val importJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            encodeDefaults = true
        }

        return try {
            val exportData = importJson
                .decodeFromStream<TwoFasJsonExport>(ByteArrayInputStream(byteArray))

            if (!exportData.servicesEncrypted.isNullOrEmpty()) {
                ExportParseResult.Error(
                    message = R.string.import_2fas_password_protected_not_supported.asText(),
                )
            } else {
                ExportParseResult.Success(
                    items = exportData.services.toAuthenticatorItemEntities()
                )
            }
        } catch (e: SerializationException) {
            ExportParseResult.Error()
        } catch (e: IllegalArgumentException) {
            ExportParseResult.Error()
        } catch (e: IOException) {
            ExportParseResult.Error()
        }
    }

    private fun List<TwoFasJsonExport.Service>.toAuthenticatorItemEntities() =
        mapNotNull { it.toAuthenticatorItemEntityOrNull() }

    private fun TwoFasJsonExport.Service.toAuthenticatorItemEntityOrNull(): AuthenticatorItemEntity {

        val type = otp.tokenType
            ?.let { tokenType ->
                // We do not support HOTP codes so we ignore them instead of throwing an exception
                if (tokenType.equals(other = TOKEN_TYPE_HOTP, ignoreCase = true)) {
                    null
                } else {
                    AuthenticatorItemType.fromStringOrNull(tokenType)
                }
            }
            ?: throw IllegalArgumentException("Unsupported OTP type: ${otp.tokenType}.")

        val algorithm = otp.algorithm
            ?.let { algorithm ->
                AuthenticatorItemAlgorithm
                    .entries
                    .find { entry ->
                        entry.name.equals(other = algorithm, ignoreCase = true)
                    }
            }
            ?: throw IllegalArgumentException("Unsupported algorithm: ${otp.algorithm}.")

        return AuthenticatorItemEntity(
            id = UUID.randomUUID().toString(),
            key = secret,
            type = type,
            algorithm = algorithm,
            period = otp.period ?: 30,
            digits = otp.digits ?: 6,
            issuer = otp.issuer.takeUnless { it.isNullOrEmpty() } ?: name.orEmpty(),
            userId = null,
            accountName = otp.account,
            favorite = false,
        )
    }
}
