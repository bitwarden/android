package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the fill-assist manifest returned by the fill-assist service.
 *
 * @property buildId The unique identifier for this build.
 * @property timestamp The ISO-8601 timestamp when this build was produced.
 * @property gitSha The git commit SHA for this build.
 * @property maps The map data entries keyed by map type.
 */
@Serializable
data class FillAssistManifestJson(
    @SerialName("buildId")
    val buildId: String,

    @SerialName("timestamp")
    val timestamp: String,

    @SerialName("gitSha")
    val gitSha: String,

    @SerialName("maps")
    val maps: MapsJson,
) {
    /**
     * Container for all available maps.
     *
     * @property forms Map of schema version string (e.g. "v1", "v2") to [FileEntryJson].
     * Using a [Map] allows new versions to appear automatically without model changes.
     */
    @Serializable
    data class MapsJson(
        @SerialName("forms")
        val forms: Map<String, FileEntryJson>,
    )

    /**
     * Metadata for a single versioned file in a map.
     *
     * @property filename The filename to fetch (e.g. "forms.v1.json").
     * @property cid The SHA-256 content hash in "sha256:<hex>" format. Used as a staleness key
     * to detect when the forms file has changed on the server, avoiding unnecessary re-downloads.
     * @property schema The schema filename associated with this file version.
     * @property deprecated When true, this version has entered its end-of-life support window.
     * Consumers should plan migration but may continue using the version until it is removed.
     */
    @Serializable
    data class FileEntryJson(
        @SerialName("filename")
        val filename: String,

        @SerialName("cid")
        val cid: String,

        @SerialName("schema")
        val schema: String,

        @SerialName("deprecated")
        val deprecated: Boolean? = null,
    )
}
