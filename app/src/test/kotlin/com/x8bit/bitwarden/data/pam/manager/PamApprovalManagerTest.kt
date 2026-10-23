package com.x8bit.bitwarden.data.pam.manager

import app.cash.turbine.test
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.util.FakeLifecycleOwner
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.createMockOrganization
import com.x8bit.bitwarden.data.pam.repository.PamRepository
import com.x8bit.bitwarden.data.pam.repository.model.PamApprovalRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.manager.VaultSyncManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class PamApprovalManagerTest {
    private val mutableFlagFlow = MutableStateFlow(true)
    private val featureFlagManager = mockk<FeatureFlagManager> {
        every {
            getFeatureFlagFlow(FlagKey.PamMobilePrivilegedControls)
        } returns mutableFlagFlow
    }
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(
        createUserState(listOf(PAM_ORGANIZATION)),
    )
    private val authRepository = mockk<AuthRepository> {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val mutableCollectionsFlow = MutableStateFlow<DataState<List<CollectionView>>>(
        DataState.Loaded(listOf(createMockCollectionView(number = 1, manage = true))),
    )
    private val vaultSyncManager = mockk<VaultSyncManager> {
        every { collectionsStateFlow } returns mutableCollectionsFlow
    }
    private val mutableAccessChangedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val pamRepository = mockk<PamRepository> {
        every { accessChangedFlow } returns mutableAccessChangedFlow
        coEvery { getApprovalInbox() } returns listOf(createRequest("1"), createRequest("2"))
            .asSuccess()
        coEvery { getCurrentAccessRequestCount() } returns 3.asSuccess()
    }
    private val mutablePamRefreshFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val pushManager = mockk<PushManager> {
        every { pamRefreshFlow } returns mutablePamRefreshFlow
    }
    private val fakeLifecycleOwner = FakeLifecycleOwner()

    private fun createManager(): PamApprovalManager = PamApprovalManagerImpl(
        authRepository = authRepository,
        vaultSyncManager = vaultSyncManager,
        featureFlagManager = featureFlagManager,
        pamRepository = pamRepository,
        pushManager = pushManager,
        dispatcherManager = FakeDispatcherManager(),
        processLifecycleOwner = fakeLifecycleOwner,
    )

    @Test
    fun `an approver with the flag on should be able to approve and have the count loaded`() {
        val manager = createManager()

        assertTrue(manager.canApproveFlow.value)
        assertEquals(2, manager.pendingCountFlow.value)
    }

    @Test
    fun `with the flag off the user cannot approve and nothing is loaded`() {
        mutableFlagFlow.value = false

        val manager = createManager()

        assertFalse(manager.canApproveFlow.value)
        assertEquals(0, manager.pendingCountFlow.value)
        coVerify(exactly = 0) { pamRepository.getApprovalInbox() }
    }

    @Test
    fun `a user without manage on a collection cannot approve`() {
        mutableCollectionsFlow.value = DataState.Loaded(
            listOf(createMockCollectionView(number = 1, manage = false)),
        )

        val manager = createManager()

        assertFalse(manager.canApproveFlow.value)
        assertEquals(0, manager.pendingCountFlow.value)
    }

    @Test
    fun `losing approval privileges should reset the count`() = runTest {
        val manager = createManager()
        assertEquals(2, manager.pendingCountFlow.value)

        mutableFlagFlow.value = false

        assertFalse(manager.canApproveFlow.value)
        assertEquals(0, manager.pendingCountFlow.value)
    }

    @Test
    fun `switching between two approver accounts should reset and reload the count`() = runTest {
        coEvery { pamRepository.getApprovalInbox() } returnsMany listOf(
            listOf(createRequest("1"), createRequest("2")).asSuccess(),
            listOf(createRequest("3")).asSuccess(),
        )
        val manager = createManager()
        assertEquals(2, manager.pendingCountFlow.value)

        mutableUserStateFlow.value = createUserState(
            organizations = listOf(PAM_ORGANIZATION),
            userId = "otherUserId",
        )

        assertTrue(manager.canApproveFlow.value)
        assertEquals(1, manager.pendingCountFlow.value)
        coVerify(exactly = 2) { pamRepository.getApprovalInbox() }
    }

    @Test
    fun `switching to an account that cannot approve should reset the count`() = runTest {
        val manager = createManager()
        assertEquals(2, manager.pendingCountFlow.value)

        mutableUserStateFlow.value = createUserState(
            organizations = emptyList(),
            userId = "otherUserId",
        )

        assertFalse(manager.canApproveFlow.value)
        assertEquals(0, manager.pendingCountFlow.value)
    }

    @Test
    fun `logging out should reset the count`() = runTest {
        val manager = createManager()
        assertEquals(2, manager.pendingCountFlow.value)

        mutableUserStateFlow.value = null

        assertEquals(0, manager.pendingCountFlow.value)
    }

    @Test
    fun `becoming an approver should load the count`() = runTest {
        mutableCollectionsFlow.value = DataState.Loaded(emptyList())
        val manager = createManager()
        assertEquals(0, manager.pendingCountFlow.value)

        mutableCollectionsFlow.value = DataState.Loaded(
            listOf(createMockCollectionView(number = 1, manage = true)),
        )

        assertTrue(manager.canApproveFlow.value)
        assertEquals(2, manager.pendingCountFlow.value)
    }

    @Test
    fun `coming to the foreground should refresh the count`() = runTest {
        val manager = createManager()
        coEvery { pamRepository.getApprovalInbox() } returns listOf(createRequest("1")).asSuccess()

        fakeLifecycleOwner.lifecycle.dispatchOnStart()

        assertEquals(1, manager.pendingCountFlow.value)
    }

    @Test
    fun `a push refresh signal should refresh the count`() = runTest {
        val manager = createManager()
        coEvery { pamRepository.getApprovalInbox() } returns emptyList<PamApprovalRequest>()
            .asSuccess()

        mutablePamRefreshFlow.emit(Unit)

        assertEquals(0, manager.pendingCountFlow.value)
    }

    @Test
    fun `a decision made in the app should refresh the count`() = runTest {
        val manager = createManager()
        coEvery { pamRepository.getApprovalInbox() } returns listOf(createRequest("1")).asSuccess()

        mutableAccessChangedFlow.emit(Unit)

        assertEquals(1, manager.pendingCountFlow.value)
    }

    @Test
    fun `a failed refresh should keep the last known count`() = runTest {
        val manager = createManager()
        coEvery { pamRepository.getApprovalInbox() } returns IllegalStateException().asFailure()

        manager.refreshPendingCount()

        assertEquals(2, manager.pendingCountFlow.value)
    }

    @Test
    fun `refreshPendingCount should do nothing for a user who cannot approve`() = runTest {
        mutableFlagFlow.value = false
        val manager = createManager()

        manager.refreshPendingCount()

        coVerify(exactly = 0) { pamRepository.getApprovalInbox() }
    }

    @Test
    fun `pendingCountFlow should emit the new count`() = runTest {
        val manager = createManager()
        coEvery { pamRepository.getApprovalInbox() } returns listOf(createRequest("1")).asSuccess()

        manager.pendingCountFlow.test {
            assertEquals(2, awaitItem())
            manager.refreshPendingCount()
            assertEquals(1, awaitItem())
        }
    }

    @Test
    fun `a PAM user with the flag on should have PAM access and the request count loaded`() {
        val manager = createManager()

        assertTrue(manager.hasPamAccessFlow.value)
        assertEquals(3, manager.myRequestCountFlow.value)
    }

    @Test
    fun `a PAM user who cannot approve should still have PAM access and a request count`() {
        mutableCollectionsFlow.value = DataState.Loaded(emptyList())

        val manager = createManager()

        assertFalse(manager.canApproveFlow.value)
        assertTrue(manager.hasPamAccessFlow.value)
        assertEquals(3, manager.myRequestCountFlow.value)
    }

    @Test
    fun `with the flag off there is no PAM access and no request count is loaded`() {
        mutableFlagFlow.value = false

        val manager = createManager()

        assertFalse(manager.hasPamAccessFlow.value)
        assertEquals(0, manager.myRequestCountFlow.value)
        coVerify(exactly = 0) { pamRepository.getCurrentAccessRequestCount() }
    }

    @Test
    fun `a user without a PAM organization has no PAM access`() {
        mutableUserStateFlow.value = createUserState(
            listOf(PAM_ORGANIZATION.copy(usePam = false)),
        )

        val manager = createManager()

        assertFalse(manager.hasPamAccessFlow.value)
        assertEquals(0, manager.myRequestCountFlow.value)
    }

    @Test
    fun `a provider user has no PAM access`() {
        mutableUserStateFlow.value = createUserState(
            listOf(PAM_ORGANIZATION.copy(isProviderUser = true)),
        )

        val manager = createManager()

        assertFalse(manager.hasPamAccessFlow.value)
    }

    @Test
    fun `switching accounts should reset and reload the request count`() = runTest {
        coEvery { pamRepository.getCurrentAccessRequestCount() } returnsMany listOf(
            3.asSuccess(),
            1.asSuccess(),
        )
        val manager = createManager()
        assertEquals(3, manager.myRequestCountFlow.value)

        mutableUserStateFlow.value = createUserState(
            organizations = listOf(PAM_ORGANIZATION),
            userId = "otherUserId",
        )

        assertEquals(1, manager.myRequestCountFlow.value)
        coVerify(exactly = 2) { pamRepository.getCurrentAccessRequestCount() }
    }

    @Test
    fun `switching to an account without PAM access should reset the request count`() = runTest {
        val manager = createManager()
        assertEquals(3, manager.myRequestCountFlow.value)

        mutableUserStateFlow.value = createUserState(
            organizations = emptyList(),
            userId = "otherUserId",
        )

        assertEquals(0, manager.myRequestCountFlow.value)
    }

    @Test
    fun `turning the flag off should reset the request count`() = runTest {
        val manager = createManager()

        mutableFlagFlow.value = false

        assertEquals(0, manager.myRequestCountFlow.value)
    }

    @Test
    fun `coming to the foreground should refresh the request count`() = runTest {
        val manager = createManager()
        coEvery { pamRepository.getCurrentAccessRequestCount() } returns 0.asSuccess()

        fakeLifecycleOwner.lifecycle.dispatchOnStart()

        assertEquals(0, manager.myRequestCountFlow.value)
    }

    @Test
    fun `a push refresh signal should refresh the request count`() = runTest {
        val manager = createManager()
        coEvery { pamRepository.getCurrentAccessRequestCount() } returns 5.asSuccess()

        mutablePamRefreshFlow.emit(Unit)

        assertEquals(5, manager.myRequestCountFlow.value)
    }

    @Test
    fun `a change to the user's own request made in the app should refresh the request count`() =
        runTest {
            val manager = createManager()
            coEvery { pamRepository.getCurrentAccessRequestCount() } returns 2.asSuccess()

            mutableAccessChangedFlow.emit(Unit)

            assertEquals(2, manager.myRequestCountFlow.value)
        }

    @Test
    fun `a failed request count refresh should keep the last known count`() = runTest {
        val manager = createManager()
        coEvery { pamRepository.getCurrentAccessRequestCount() } returns
            IllegalStateException().asFailure()

        manager.refreshMyRequestCount()

        assertEquals(3, manager.myRequestCountFlow.value)
    }

    @Test
    fun `refreshMyRequestCount should do nothing without PAM access`() = runTest {
        mutableFlagFlow.value = false
        val manager = createManager()

        manager.refreshMyRequestCount()

        coVerify(exactly = 0) { pamRepository.getCurrentAccessRequestCount() }
    }

    @Test
    fun `hasApprovalPrivileges should require PAM on the organization`() {
        assertFalse(
            hasApprovalPrivileges(
                organizations = listOf(PAM_ORGANIZATION.copy(usePam = false)),
                collections = listOf(createMockCollectionView(number = 1, manage = true)),
            ),
        )
    }

    @Test
    fun `hasApprovalPrivileges should exclude provider users even with manage`() {
        assertFalse(
            hasApprovalPrivileges(
                organizations = listOf(PAM_ORGANIZATION.copy(isProviderUser = true)),
                collections = listOf(createMockCollectionView(number = 1, manage = true)),
            ),
        )
    }

    @Test
    fun `hasApprovalPrivileges should require manage on a collection of the same organization`() {
        assertFalse(
            hasApprovalPrivileges(
                organizations = listOf(PAM_ORGANIZATION),
                collections = listOf(
                    createMockCollectionView(number = 2, manage = true),
                    createMockCollectionView(number = 1, manage = false),
                ),
            ),
        )
        assertTrue(
            hasApprovalPrivileges(
                organizations = listOf(PAM_ORGANIZATION),
                collections = listOf(createMockCollectionView(number = 1, manage = true)),
            ),
        )
    }

    @Test
    fun `hasApprovalPrivileges should allow users who can manage all collections`() {
        assertTrue(
            hasApprovalPrivileges(
                organizations = listOf(PAM_ORGANIZATION.copy(canEditAllCiphers = true)),
                collections = emptyList(),
            ),
        )
    }

    @Test
    fun `hasApprovalPrivileges should be false without organizations`() {
        assertFalse(
            hasApprovalPrivileges(
                organizations = emptyList(),
                collections = listOf(createMockCollectionView(number = 1, manage = true)),
            ),
        )
    }
}

private val PAM_ORGANIZATION: Organization = createMockOrganization(
    number = 1,
    id = "mockOrganizationId-1",
    usePam = true,
)

private fun createUserState(
    organizations: List<Organization>,
    userId: String = "userId",
): UserState {
    val account = mockk<UserState.Account> {
        every { this@mockk.organizations } returns organizations
    }
    return mockk<UserState> {
        every { activeAccount } returns account
        every { activeUserId } returns userId
    }
}

private fun createRequest(id: String): PamApprovalRequest = PamApprovalRequest(
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
