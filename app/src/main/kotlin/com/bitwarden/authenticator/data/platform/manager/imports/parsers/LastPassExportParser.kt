package com.bitwarden.authenticator.data.platform.manager.imports.parsers

import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.platform.manager.imports.model.ExportParseResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.LastPassJsonExport
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.UUID

/**
 * An [ExportParser] responsible for transforming LastPass export files into Bitwarden Authenticator
 * items.
 */
class LastPassExportParser : ExportParser {

    @OptIn(ExperimentalSerializationApi::class)
    override fun parse(byteArray: ByteArray): ExportParseResult {
        val importJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

        return try {
            val exportData =
                importJson.decodeFromStream<LastPassJsonExport>(ByteArrayInputStream(byteArray))
            ExportParseResult.Success(
                items = exportData
                    .accounts
                    .toAuthenticatorItemEntities(),
            )
        } catch (e: SerializationException) {
            ExportParseResult.Error()
        } catch (e: IllegalArgumentException) {
            ExportParseResult.Error()
        } catch (e: IOException) {
            ExportParseResult.Error()
        }
    }

    private fun List<LastPassJsonExport.Account>.toAuthenticatorItemEntities() =
        map { it.toAuthenticatorItemEntity() }

    private fun LastPassJsonExport.Account.toAuthenticatorItemEntity(): AuthenticatorItemEntity {

        // Lastpass only supports TOTP codes.
        val type = AuthenticatorItemType.TOTP

        val algorithmEnum = AuthenticatorItemAlgorithm
            .fromStringOrNull(algorithm)
            ?: throw IllegalArgumentException("Unsupported algorithm.")

        return AuthenticatorItemEntity(
            id = UUID.randomUUID().toString(),
            key = secret,
            type = type,
            algorithm = algorithmEnum,
            period = timeStep,
            digits = digits,
            issuer = originalIssuerName,
            userId = null,
            accountName = originalUserName,
            favorite = isFavorite,
        )
    }
}
