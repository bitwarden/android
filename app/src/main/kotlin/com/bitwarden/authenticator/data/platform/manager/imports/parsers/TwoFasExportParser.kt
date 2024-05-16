package com.bitwarden.authenticator.data.platform.manager.imports.parsers

import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.platform.manager.imports.model.TwoFasJsonExport
import com.bitwarden.authenticator.data.platform.util.asFailure
import com.bitwarden.authenticator.data.platform.util.asSuccess
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
    override fun parse(byteArray: ByteArray): Result<List<AuthenticatorItemEntity>> {
        return import2fasJson(byteArray)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun import2fasJson(byteArray: ByteArray): Result<List<AuthenticatorItemEntity>> {
        val importJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

        return try {
            importJson
                .decodeFromStream<TwoFasJsonExport>(ByteArrayInputStream(byteArray))
                .asSuccess()
                .mapCatching { exportData ->
                    exportData
                        .services
                        .toAuthenticatorItemEntities()
                }
        } catch (e: SerializationException) {
            e.asFailure()
        } catch (e: IllegalArgumentException) {
            e.asFailure()
        } catch (e: IOException) {
            e.asFailure()
        }
    }

    private fun List<TwoFasJsonExport.Service>.toAuthenticatorItemEntities() =
        mapNotNull { it.toAuthenticatorItemEntityOrNull() }

    private fun TwoFasJsonExport.Service.toAuthenticatorItemEntityOrNull(): AuthenticatorItemEntity? {

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
            period = otp.period,
            digits = otp.digits,
            issuer = otp.issuer ?: name,
            userId = null,
            accountName = otp.account,
            favorite = false,
        )
    }
}
