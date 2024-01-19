package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.ui.platform.components.model.IconRes
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendStatusIcon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SendViewExtensionsTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun `toLabelIcons should return all of the icons when all checks have passed`() {
        val sendView = createMockSendView(number = 1).copy(
            // Show the password icon when true
            hasPassword = true,
            // Show the disabled icon when true
            disabled = true,
            // Show the max access count reached icon when accessCount is greater than or equal to
            // the maxAccessCount
            maxAccessCount = 1u,
            accessCount = 1u,
            // Show the pending deletion icon when the deletion date is in the past
            deletionDate = Instant.parse("2023-10-26T12:00:00Z"),
            // Show the expired icon when the expiration date is in the past
            expirationDate = Instant.parse("2023-10-26T12:00:00Z"),
        )

        val result = sendView.toLabelIcons(clock)

        assertEquals(ALL_SEND_STATUS_ICONS, result)
    }

    @Test
    fun `toLabelIcons should return none of the icons when none of the checks have passed`() {
        val sendView = createMockSendView(number = 1).copy(
            // Hide the password icon when false
            hasPassword = false,
            // Hide the disabled icon when false
            disabled = false,
            // Hide the max access count reached icon when accessCount is less than the
            // maxAccessCount
            maxAccessCount = 10u,
            accessCount = 1u,
            // Hide the pending deletion icon when the deletion date is in the future
            deletionDate = Instant.parse("2023-10-28T12:00:00Z"),
            // Hide the expired icon when the expiration date is in the future
            expirationDate = Instant.parse("2023-10-28T12:00:00Z"),
        )

        val result = sendView.toLabelIcons(clock)

        assertEquals(emptyList<IconRes>(), result)
    }

    @Test
    fun `toSendUrl should create an appropriate url`() {
        val sendView = createMockSendView(number = 1)

        val result = sendView.toSendUrl(baseWebSendUrl = "www.test.com/")

        assertEquals("www.test.com/mockAccessId-1/mockKey-1", result)
    }
}

private val ALL_SEND_STATUS_ICONS: List<IconRes> = listOf(
    IconRes(
        iconRes = SendStatusIcon.DISABLED.iconRes,
        contentDescription = SendStatusIcon.DISABLED.contentDescription,
    ),
    IconRes(
        iconRes = SendStatusIcon.PASSWORD.iconRes,
        contentDescription = SendStatusIcon.PASSWORD.contentDescription,
    ),
    IconRes(
        iconRes = SendStatusIcon.MAX_ACCESS_COUNT_REACHED.iconRes,
        contentDescription = SendStatusIcon.MAX_ACCESS_COUNT_REACHED.contentDescription,
    ),
    IconRes(
        iconRes = SendStatusIcon.EXPIRED.iconRes,
        contentDescription = SendStatusIcon.EXPIRED.contentDescription,
    ),
    IconRes(
        iconRes = SendStatusIcon.PENDING_DELETE.iconRes,
        contentDescription = SendStatusIcon.PENDING_DELETE.contentDescription,
    ),
)
