package com.bitwarden.authenticator.data.platform.manager.imports.model

import kotlinx.serialization.Serializable

/**
 * Models the Aegis JSON export file.
 */
@Serializable
data class AegisJsonExport(
    val version: Int,
    val db: Database,
) {

    /**
     * Models the Aegis database in JSON format.
     */
    @Serializable
    data class Database(
        val version: Int,
        val entries: List<Entry>,
        val groups: List<Group>,
    ) {

        /**
         * Models an Aegis database entry.
         */
        @Serializable
        data class Entry(
            val type: String,
            val uuid: String,
            val name: String,
            val issuer: String,
            val note: String,
            val favorite: Boolean,
            val info: Info,
            val groups: List<String>,
        ) {

            /**
             * Models key information for an [Entry].
             */
            @Serializable
            data class Info(
                val secret: String,
                val algo: String,
                val digits: Int,
                val period: Int,
            )
        }

        /**
         * Models a collection that can be associated with an [Entry].
         */
        @Serializable
        data class Group(
            val uuid: String,
            val name: String,
        )
    }
}
