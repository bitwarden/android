package com.bitwarden.cxf.parser

import androidx.credentials.providerevents.exception.ImportCredentialsInvalidJsonException
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.cxf.model.CredentialExchangeExportResponse
import com.bitwarden.cxf.model.CredentialExchangePayload
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val SUPPORTED_CXF_VERSIONS = mapOf(
    0 to setOf(0),
    1 to setOf(0),
)

/**
 * Default implementation of the [CredentialExchangePayloadParser].
 */
internal class CredentialExchangePayloadParserImpl(
    private val json: Json,
) : CredentialExchangePayloadParser {

    override fun parse(payload: String): CredentialExchangePayload =
        parseInternal(payload)
            ?: CredentialExchangePayload.Error(
                ImportCredentialsInvalidJsonException(
                    "Invalid Credential Exchange JSON.",
                ),
            )

    /**
     * Attempts to parse the alpha04+ Credential Exchange API JSON payload into a
     * [CredentialExchangePayload] object.
     *
     * @return A [CredentialExchangePayload] object if the payload can be serialized directly into a
     * [CredentialExchangeExportResponse], otherwise `null`.
     */
    private fun parseInternal(payload: String): CredentialExchangePayload? =
        json
            .decodeFromStringOrNull<CredentialExchangeExportResponse>(payload)
            ?.let { exportResponse ->
                when {
                    !isCxfVersionSupported(exportResponse) -> {
                        CredentialExchangePayload.Error(
                            ImportCredentialsInvalidJsonException(
                                "Unsupported CXF version.",
                            ),
                        )
                    }

                    exportResponse.accounts.isEmpty() -> {
                        CredentialExchangePayload.NoItems
                    }

                    else -> {
                        try {
                            // We only support single account import, silently ignore additional
                            // accounts.
                            val accountsJson = json.encodeToString(
                                value = exportResponse.accounts.first(),
                            )
                            CredentialExchangePayload.Importable(
                                accountsJson = accountsJson,
                            )
                        } catch (_: SerializationException) {
                            CredentialExchangePayload.Error(
                                ImportCredentialsInvalidJsonException(
                                    "Unable to serialize accounts.",
                                ),
                            )
                        }
                    }
                }
            }

    private fun isCxfVersionSupported(
        response: CredentialExchangeExportResponse,
    ): Boolean = SUPPORTED_CXF_VERSIONS[response.version.major]
        ?.contains(response.version.minor) == true
}
