package com.x8bit.bitwarden.data.platform.manager.model

import com.bitwarden.network.model.SendAccessTypeJson
import com.bitwarden.network.model.SendTypeJson

/**
 * The effective, precedence-resolved Send policy for the active user, combining the legacy
 * DisableSend/SendOptions policies with the newer SendControls policy (type 21) per the
 * `pm-31885-send-controls` feature flag.
 *
 * When an organization has an active SendControls policy, that organization's legacy
 * DisableSend/SendOptions policies are ignored in favor of the SendControls values; other
 * organizations' legacy policies remain in effect.
 *
 * @property allowedDomains The allowed recipient email domains, sourced from whichever
 * organization's active SendControls policy has the earliest revision date, if any. Currently
 * unused by the UI.
 * @property allowedSendTypes The types of Sends that are allowed to be created, sourced the same
 * way as [allowedDomains]. Currently unused by the UI.
 * @property deletionHours The enforced number of hours until a Send is deleted, sourced the same
 * way as [allowedDomains]. Currently unused by the UI.
 * @property disableHideEmail Whether the ability to hide one's email address on a Send should be
 * disabled.
 * @property disableSend Whether the ability to create and edit Sends should be disabled.
 * @property whoCanAccess The access type Sends are restricted to, sourced the same way as
 * [allowedDomains]. Currently unused by the UI.
 */
data class EffectiveSendPolicy(
    val allowedDomains: String?,
    val allowedSendTypes: List<SendTypeJson>?,
    val deletionHours: Int?,
    val disableHideEmail: Boolean,
    val disableSend: Boolean,
    val whoCanAccess: SendAccessTypeJson?,
)
