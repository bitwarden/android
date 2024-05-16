package com.bitwarden.authenticator.data.platform.manager.imports.model

import kotlinx.serialization.Serializable

@Serializable
data class TwoFasJsonExport(
    val schemaVersion: Int,
    val appVersionCode: Int,
    val appOrigin: String,
    val services: List<Service>,
    val groups: List<Group>,
) {
    @Serializable
    data class Service(
        val otp: Otp,
        val order: Order,
        val updatedAt: Long,
        val name: String,
        val icon: Icon?,
        val secret: String,
        val badge: Badge,
        val serviceTypeId: String?,
    ) {
        @Serializable
        data class Otp(
            val counter: Int,
            val period: Int,
            val digits: Int,
            val account: String,
            val source: String?,
            val tokenType: String?,
            val algorithm: String?,
            val link: String?,
            val issuer: String?,
        )

        @Serializable
        data class Order(
            val position: Int,
        )

        @Serializable
        data class Icon(
            val iconCollection: IconCollection,
            val label: Label,
            val selected: String,
        ) {
            @Serializable
            data class IconCollection(
                val id: String,
            )

            @Serializable
            data class Label(
                val backgroundColor: String,
                val text: String,
            )
        }

        @Serializable
        data class Badge(
            val color: String,
        )
    }

    @Serializable
    data class Group(
        val id: String,
        val name: String,
        val isExpanded: Boolean,
    )
}
