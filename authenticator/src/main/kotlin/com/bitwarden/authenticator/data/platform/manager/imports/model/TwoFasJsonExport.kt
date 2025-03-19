package com.bitwarden.authenticator.data.platform.manager.imports.model

import kotlinx.serialization.Serializable

/**
 * Models the JSON export file from 2FAS.
 */
@Serializable
data class TwoFasJsonExport(
    val schemaVersion: Int?,
    val appVersionCode: Int?,
    val appOrigin: String?,
    val services: List<Service>,
    val servicesEncrypted: String?,
    val groups: List<Group>?,
) {
    /**
     * Models a service account contained within a [TwoFasJsonExport].
     */
    @Serializable
    data class Service(
        val otp: Otp,
        val order: Order?,
        val updatedAt: Long?,
        val name: String?,
        val icon: Icon?,
        val secret: String,
        val badge: Badge?,
        val serviceTypeId: String?,
    ) {
        /**
         * Models OTP auth data for a 2fas [Service].
         */
        @Serializable
        data class Otp(
            val counter: Int?,
            val period: Int?,
            val digits: Int?,
            val account: String?,
            val source: String?,
            val tokenType: String?,
            val algorithm: String?,
            val link: String?,
            val issuer: String?,
        )

        /**
         * Models ordinal information for a 2fas [Service].
         */
        @Serializable
        data class Order(
            val position: Int?,
        )

        /**
         * Models the icon for a 2fas [Service].
         */
        @Serializable
        data class Icon(
            val iconCollection: IconCollection?,
            val label: Label?,
            val selected: String?,
        ) {
            /**
             * Models a collection that can be associated to a 2fas [Icon].
             */
            @Serializable
            data class IconCollection(
                val id: String?,
            )

            /**
             * Models label data for a 2fas [Icon].
             */
            @Serializable
            data class Label(
                val backgroundColor: String?,
                val text: String?,
            )
        }

        /**
         * Models badge data about a 2fas [Service].
         */
        @Serializable
        data class Badge(
            val color: String,
        )
    }

    /**
     * Models a collection of 2fas [Service] objects.
     */
    @Serializable
    data class Group(
        val id: String,
        val name: String,
        val isExpanded: Boolean?,
    )
}
