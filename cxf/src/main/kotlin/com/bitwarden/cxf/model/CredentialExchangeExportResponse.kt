package com.bitwarden.cxf.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

/**
 * Represents the header of a credential exchange export file.
 * This data class contains metadata about the export, such as the file version,
 * information about the exporting relying party (RP), the export timestamp, and
 * a list of accounts included in the export.
 *
 * See the FIDO Alliance CXF specification for more details:
 * https://fidoalliance.org/specs/cx/cxf-v1.0-rd-20250313.html#sctn-data-structure-specifications
 *
 * @property version The version of the credential exchange file format.
 * @property exporterRpId The relying party ID of the application that exported the credentials.
 * @property exporterDisplayName The display name of the application that exported the credentials.
 * @property timestamp The Unix timestamp (in seconds) when the export was created.
 * @property accounts A list of [Account]s whose credentials are included in this export.
 */
@Serializable
data class CredentialExchangeExportResponse(
    val version: CredentialExchangeVersion,
    val exporterRpId: String,
    val exporterDisplayName: String,
    val timestamp: Long,
    val accounts: List<Account>,
) {
    /**
     * Represents a single account included in the credential exchange export.
     * Each account object contains user identification information, references to collections,
     * and the actual credential items belonging to that user.
     *
     * See the FIDO Alliance CXF specification for more details:
     * https://fidoalliance.org/specs/cx/cxf-v1.0-rd-20250313.html#entity-collection
     *
     * @property id A unique, stable identifier for the account, such as a UUID.
     * @property username The username associated with the account.
     * @property email The email address associated with the account.
     * @property collections A JSON array of
     * [Collection](https://fidoalliance.org/specs/cx/cxf-v1.0-rd-20250313.html#entity-collection)
     * objects associated with this account.
     * @property items A JSON array of credential
     * [Item](https://fidoalliance.org/specs/cx/cxf-v1.0-rd-20250313.html#entity-item) objects
     * associated with this account.
     */
    @Serializable
    data class Account(
        val id: String,
        val username: String,
        val email: String,
        val collections: JsonArray,
        val items: JsonArray,
    )
}
