package com.x8bit.bitwarden.data.pam.manager

import androidx.work.WorkManager
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class PamLeaseReminderManagerTest {
    private val workManager = mockk<WorkManager> {
        every { cancelUniqueWork(any()) } returns mockk()
        every { cancelAllWorkByTag(any()) } returns mockk()
    }
    private val mutableUserStateFlow = MutableStateFlow<UserStateJson?>(
        createUserState("userA", "userB"),
    )
    private val authDiskSource = mockk<AuthDiskSource> {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val mutableFlagFlow = MutableStateFlow(true)
    private val featureFlagManager = mockk<FeatureFlagManager> {
        every { getFeatureFlagFlow(FlagKey.PamMobilePrivilegedControls) } returns mutableFlagFlow
    }

    private val manager = PamLeaseReminderManagerImpl(
        workManager = workManager,
        clock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC),
        authDiskSource = authDiskSource,
        featureFlagManager = featureFlagManager,
        dispatcherManager = FakeDispatcherManager(),
    )

    @Test
    fun `nothing should be cancelled while accounts and the flag are unchanged`() {
        verify(exactly = 0) { workManager.cancelAllWorkByTag(any()) }
    }

    @Test
    fun `removing an account should cancel only that user's reminders`() {
        mutableUserStateFlow.value = createUserState("userA")

        verify(exactly = 1) { workManager.cancelAllWorkByTag(pamLeaseReminderUserTag("userB")) }
        verify(exactly = 0) { workManager.cancelAllWorkByTag(pamLeaseReminderUserTag("userA")) }
    }

    @Test
    fun `logging out the last account should cancel its reminders`() {
        mutableUserStateFlow.value = createUserState("userA")
        mutableUserStateFlow.value = null

        verify(exactly = 1) { workManager.cancelAllWorkByTag(pamLeaseReminderUserTag("userA")) }
    }

    @Test
    fun `switching the active account should not cancel reminders`() {
        mutableUserStateFlow.value = createUserState("userA", "userB", activeUserId = "userB")

        verify(exactly = 0) { workManager.cancelAllWorkByTag(any()) }
    }

    @Test
    fun `turning the feature flag off should cancel every reminder`() {
        mutableFlagFlow.value = false

        verify(exactly = 1) { workManager.cancelAllWorkByTag(PAM_LEASE_REMINDER_TAG) }
    }

    @Test
    fun `cancelReminder should cancel the unique work of the lease`() {
        manager.cancelReminder(leaseId = "lease")

        verify(exactly = 1) { workManager.cancelUniqueWork("PamLeaseReminder_lease") }
    }

    @Test
    fun `scheduleReminder should cancel instead of scheduling when the time has passed`() {
        manager.scheduleReminder(
            userId = "userA",
            leaseId = "lease",
            leaseEndsAt = Instant.parse("2026-01-01T12:01:00Z"),
        )

        verify(exactly = 1) { workManager.cancelUniqueWork("PamLeaseReminder_lease") }
    }

    @Test
    fun `reminders are shown only for a signed in user with the flag on`() {
        assertTrue(shouldShowPamLeaseReminder(true, "userA", setOf("userA")))
        assertFalse(shouldShowPamLeaseReminder(false, "userA", setOf("userA")))
        assertFalse(shouldShowPamLeaseReminder(true, "userA", setOf("userB")))
        assertFalse(shouldShowPamLeaseReminder(true, null, setOf("userA")))
        assertFalse(shouldShowPamLeaseReminder(true, "userA", emptySet()))
    }

    @Test
    fun `user tags should be distinct per user`() {
        assertEquals("PamLeaseReminder_user_a", pamLeaseReminderUserTag("a"))
    }
}

private fun createUserState(
    vararg userIds: String,
    activeUserId: String = userIds.first(),
): UserStateJson = mockk<UserStateJson> {
    every { accounts } returns userIds.associateWith { mockk<AccountJson>() }
    every { this@mockk.activeUserId } returns activeUserId
}
