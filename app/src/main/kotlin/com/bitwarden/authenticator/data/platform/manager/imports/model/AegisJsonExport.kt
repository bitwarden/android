package com.bitwarden.authenticator.data.platform.manager.imports.model

import kotlinx.serialization.Serializable

@Serializable
data class AegisJsonExport(
    val version: Int,
    val db: Database,
) {

    @Serializable
    data class Database(
        val version: Int,
        val entries: List<Entry>,
        val groups: List<Group>,
    ) {

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

            @Serializable
            data class Info(
                val secret: String,
                val algo: String,
                val digits: Int,
                val period: Int,
            )
        }

        @Serializable
        data class Group(
            val uuid: String,
            val name: String,
        )
    }
}
