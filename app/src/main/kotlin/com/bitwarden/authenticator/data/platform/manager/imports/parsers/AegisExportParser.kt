package com.bitwarden.authenticator.data.platform.manager.imports.parsers

import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.platform.manager.imports.model.AegisJsonExport
import com.bitwarden.authenticator.data.platform.util.asFailure
import com.bitwarden.authenticator.data.platform.util.asSuccess
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.UUID

class AegisExportParser : ExportParser {
    @OptIn(ExperimentalSerializationApi::class)
    override fun parse(byteArray: ByteArray): Result<List<AuthenticatorItemEntity>> {
        val importJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

        return try {
            importJson
                .decodeFromStream<AegisJsonExport>(ByteArrayInputStream(byteArray))
                .asSuccess()
                .mapCatching { exportData ->
                    exportData
                        .db
                        .entries
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

    private fun List<AegisJsonExport.Database.Entry>.toAuthenticatorItemEntities() =
        map { it.toAuthenticatorItemEntity() }

    private fun AegisJsonExport.Database.Entry.toAuthenticatorItemEntity(): AuthenticatorItemEntity {

        // Lastpass only supports TOTP codes.
        val type = AuthenticatorItemType.fromStringOrNull(type)
            ?: throw IllegalArgumentException("Unsupported OTP type")

        val algorithmEnum = AuthenticatorItemAlgorithm
            .fromStringOrNull(info.algo)
            ?: throw IllegalArgumentException("Unsupported algorithm.")

        return AuthenticatorItemEntity(
            id = UUID.randomUUID().toString(),
            key = info.secret,
            type = type,
            algorithm = algorithmEnum,
            period = info.period,
            digits = info.digits,
            issuer = issuer,
            userId = null,
            accountName = name,
            favorite = favorite,
        )
    }
}
