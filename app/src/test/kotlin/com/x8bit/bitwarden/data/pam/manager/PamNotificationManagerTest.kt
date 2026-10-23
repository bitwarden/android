package com.x8bit.bitwarden.data.pam.manager

import android.app.Notification
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.x8bit.bitwarden.data.pam.repository.PamRepository
import com.x8bit.bitwarden.data.pam.repository.model.PamApprovalRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.model.PamAlertData
import com.x8bit.bitwarden.data.platform.manager.model.PamAlertType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class PamNotificationManagerTest {
    private val mutableAlertFlow = MutableSharedFlow<PamAlertData>(extraBufferCapacity = 4)
    private val mutableRefreshFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    private val mutableAccessChangedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    private val pushManager = mockk<PushManager> {
        every { pamAlertFlow } returns mutableAlertFlow
        every { pamRefreshFlow } returns mutableRefreshFlow
    }
    private val pamRepository = mockk<PamRepository> {
        every { accessChangedFlow } returns mutableAccessChangedFlow
        coEvery { getApprovalInbox() } returns emptyList<PamApprovalRequest>().asSuccess()
    }
    private val featureFlagManager = mockk<FeatureFlagManager> {
        every { getFeatureFlag(FlagKey.PamMobilePrivilegedControls) } returns true
    }
    private val channel = mockk<NotificationChannelCompat>()
    private val notification = mockk<Notification>()
    private val notificationFactory = mockk<PamAlertNotificationFactory> {
        every { createChannel() } returns channel
        every { createNotification(any()) } returns notification
    }
    private val notificationManager = mockk<NotificationManagerCompat> {
        every { createNotificationChannel(any<NotificationChannelCompat>()) } just runs
        every { areNotificationsEnabled() } returns true
        every { getNotificationChannelCompat(PAM_ALERT_CHANNEL_ID) } returns null
        every { notify(any<String>(), any(), any()) } just runs
        every { cancel(any<String>(), any()) } just runs
    }

    @Suppress("UnusedPrivateProperty")
    private val manager: PamNotificationManager = PamNotificationManagerImpl(
        notificationManager = notificationManager,
        notificationFactory = notificationFactory,
        featureFlagManager = featureFlagManager,
        pamRepository = pamRepository,
        pushManager = pushManager,
        dispatcherManager = FakeDispatcherManager(),
    )

    @Test
    fun `an alert should show one notification tagged with the request id`() {
        mutableAlertFlow.tryEmit(alert(requestId = "request-1"))

        verify(exactly = 1) {
            notificationManager.notify(pamAlertNotificationTag("request-1"), any(), notification)
        }
    }

    @Test
    fun `alerts for different requests should not replace each other`() {
        mutableAlertFlow.tryEmit(alert(requestId = "request-1"))
        mutableAlertFlow.tryEmit(alert(requestId = "request-2"))

        verify(exactly = 1) {
            notificationManager.notify(pamAlertNotificationTag("request-1"), any(), any())
        }
        verify(exactly = 1) {
            notificationManager.notify(pamAlertNotificationTag("request-2"), any(), any())
        }
    }

    @Test
    fun `alert notification tags should never match a lease reminder tag`() {
        assertNotEquals("pam_lease_reminder:id", pamAlertNotificationTag("id"))
    }

    @Test
    fun `an alert should do nothing when the feature flag is off`() {
        every {
            featureFlagManager.getFeatureFlag(FlagKey.PamMobilePrivilegedControls)
        } returns false

        mutableAlertFlow.tryEmit(alert())

        verify(exactly = 0) { notificationManager.notify(any<String>(), any(), any()) }
        verify(exactly = 0) {
            notificationManager.createNotificationChannel(any<NotificationChannelCompat>())
        }
    }

    @Test
    fun `an alert should do nothing when notifications are disabled`() {
        every { notificationManager.areNotificationsEnabled() } returns false

        mutableAlertFlow.tryEmit(alert())

        verify(exactly = 0) { notificationManager.notify(any<String>(), any(), any()) }
    }

    @Test
    fun `an alert should do nothing when the channel is disabled`() {
        every {
            notificationManager.getNotificationChannelCompat(PAM_ALERT_CHANNEL_ID)
        } returns mockk<NotificationChannelCompat> {
            every { importance } returns NotificationManagerCompat.IMPORTANCE_NONE
        }

        mutableAlertFlow.tryEmit(alert())

        verify(exactly = 0) { notificationManager.notify(any<String>(), any(), any()) }
    }

    @Test
    fun `an alert should create the channel before notifying`() {
        mutableAlertFlow.tryEmit(alert())

        verify(exactly = 1) { notificationManager.createNotificationChannel(channel) }
    }

    @Test
    fun `a decided request should cancel its notification when access changes`() {
        mutableAlertFlow.tryEmit(alert(requestId = "decided"))
        mutableAlertFlow.tryEmit(alert(requestId = "still-pending"))
        coEvery { pamRepository.getApprovalInbox() } returns
            listOf(createApprovalRequest(id = "still-pending")).asSuccess()

        mutableAccessChangedFlow.tryEmit(Unit)

        verify(exactly = 1) {
            notificationManager.cancel(pamAlertNotificationTag("decided"), any())
        }
        verify(exactly = 0) {
            notificationManager.cancel(pamAlertNotificationTag("still-pending"), any())
        }
    }

    @Test
    fun `a request decided elsewhere should cancel its notification on a refresh push`() {
        mutableAlertFlow.tryEmit(alert(requestId = "decided"))

        mutableRefreshFlow.tryEmit(Unit)

        verify(exactly = 1) {
            notificationManager.cancel(pamAlertNotificationTag("decided"), any())
        }
    }

    @Test
    fun `a failed inbox refresh should not cancel any notification`() {
        mutableAlertFlow.tryEmit(alert(requestId = "request-1"))
        coEvery { pamRepository.getApprovalInbox() } returns IllegalStateException().asFailure()

        mutableAccessChangedFlow.tryEmit(Unit)

        verify(exactly = 0) { notificationManager.cancel(any<String>(), any()) }
    }

    @Test
    fun `non-request alerts should not be cancelled by inbox refreshes`() {
        mutableAlertFlow.tryEmit(alert(requestId = "approved", type = PamAlertType.ACCESS_APPROVED))

        mutableAccessChangedFlow.tryEmit(Unit)

        verify(exactly = 0) { notificationManager.cancel(any<String>(), any()) }
    }

    private fun alert(
        requestId: String = "requestId",
        type: PamAlertType = PamAlertType.ACCESS_REQUESTED,
    ) = PamAlertData(userId = "userId", requestId = requestId, type = type)
}

private fun createApprovalRequest(id: String): PamApprovalRequest = PamApprovalRequest(
    id = id,
    cipherId = "cipherId",
    collectionId = "collectionId",
    organizationId = null,
    requesterId = "requesterId",
    requesterName = null,
    requesterEmail = null,
    status = PamRequestStatus.PENDING,
    reason = null,
    submittedAt = Instant.EPOCH,
    resolvedAt = null,
    windowStart = Instant.EPOCH,
    windowEnd = Instant.EPOCH.plusSeconds(60),
    isExtension = false,
    isOwnRequest = false,
    decision = null,
)
