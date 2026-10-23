package com.x8bit.bitwarden.ui.vault.feature.pamscreenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.pam.repository.model.PamDecisionVerdict
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.feature.settings.vault.VaultSettingsScreen
import com.x8bit.bitwarden.ui.platform.feature.settings.vault.VaultSettingsState
import com.x8bit.bitwarden.ui.platform.feature.settings.vault.VaultSettingsViewModel
import com.x8bit.bitwarden.ui.platform.feature.settings.vault.myaccess.MyAccessScreen
import com.x8bit.bitwarden.ui.platform.feature.settings.vault.myaccess.MyAccessState
import com.x8bit.bitwarden.ui.platform.feature.settings.vault.myaccess.MyAccessViewModel
import com.x8bit.bitwarden.ui.platform.manager.permissions.FakePermissionManager
import com.x8bit.bitwarden.ui.platform.model.FeatureFlagsState
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemScreen
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemState
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemViewModel
import com.x8bit.bitwarden.ui.vault.feature.item.model.VaultItemPamRequestForm
import com.x8bit.bitwarden.ui.vault.feature.item.model.VaultItemPamState
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.PamApprovalsScreen
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.PamApprovalsState
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.PamApprovalsViewModel
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.detail.PamApprovalDetailScreen
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.detail.PamApprovalDetailState
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.detail.PamApprovalDetailViewModel
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultScreen
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultViewModel
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Renders the key privileged access (PAM) screens to PNG files, in light and dark theme, so they
 * can be reviewed without an emulator or device.
 *
 * This is NOT a regular test. It does nothing (the tests are skipped) unless the
 * [ENABLE_PROPERTY] system property is set, which the `-PpamScreenshots` Gradle property does.
 * The output directory defaults to [DEFAULT_OUTPUT_DIR] and can be changed with the
 * [OUTPUT_DIR_PROPERTY] system property (Gradle: `-PpamScreenshotsDir=<dir>`).
 *
 * Each test sets the content once, then walks through several hand-built states of the real
 * screen and captures each one for both themes by flipping a theme state.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
@Suppress("LargeClass", "TooManyFunctions")
class PamScreenshotTest : BitwardenComposeTest() {

    private val outputDir: File = File(
        System.getProperty(OUTPUT_DIR_PROPERTY)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_OUTPUT_DIR,
    )
    private val clock: Clock = Clock.fixed(NOW, ZoneOffset.UTC)
    private var currentTheme by mutableStateOf(AppTheme.LIGHT)
    private var previousTimeZone: TimeZone? = null

    @Before
    fun setUp() {
        assumeTrue(
            "PAM screenshots are on demand: run with -PpamScreenshots",
            System.getProperty(ENABLE_PROPERTY) != null,
        )
        previousTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        outputDir.mkdirs()
        // Espresso is only on the classpath transitively, so it is reached by reflection.
        Class.forName("androidx.test.espresso.IdlingPolicies")
            .getMethod("setMasterPolicyTimeout", Long::class.java, TimeUnit::class.java)
            .invoke(null, IDLE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    @After
    fun tearDown() {
        previousTimeZone?.let { TimeZone.setDefault(it) }
    }

    //region Vault item
    @Test
    fun vaultItemScreens() {
        val mutableState = MutableStateFlow(gatedItemState(pam = PAM_CAN_REQUEST))
        val viewModel = mockk<VaultItemViewModel>(relaxed = true) {
            every { eventFlow } returns bufferedMutableSharedFlow()
            every { stateFlow } returns mutableState
        }
        setScreenContent {
            VaultItemScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onNavigateToVaultAddEditItem = {},
                onNavigateToMoveToOrganization = { _, _ -> },
                onNavigateToAttachments = {},
                onNavigateToPasswordHistory = {},
                onNavigateToPreviewAttachment = {},
                onNavigateToPlan = {},
            )
        }

        capture("01-item-request-access")

        mutableState.value = gatedItemState(
            pam = PAM_CAN_REQUEST.copy(
                isSlotHeldByAnotherMember = true,
                slotFreesAt = Instant.parse("2026-10-22T15:45:00Z"),
            ),
        )
        capture("02-item-slot-held-by-another-member")

        mutableState.value = gatedItemState(pam = VaultItemPamState.Pending)
        capture("03-item-request-pending")

        mutableState.value = gatedItemState(
            pam = VaultItemPamState.Approved(requestId = "request"),
        )
        capture("04-item-request-approved")

        mutableState.value = activeItemState(remainingMinutes = 84L)
        capture("05-item-active-access")

        mutableState.value = gatedItemState(
            pam = PAM_CAN_REQUEST,
            dialog = VaultItemState.DialogState.PamRequestAccess(form = AUTOMATIC_FORM),
        )
        capture("06-request-form-automatic")

        mutableState.value = gatedItemState(
            pam = PAM_CAN_REQUEST,
            dialog = VaultItemState.DialogState.PamRequestAccess(
                form = AUTOMATIC_FORM.copy(durationMinutes = "900"),
            ),
        )
        capture("07-request-form-automatic-over-maximum")

        mutableState.value = gatedItemState(
            pam = PAM_CAN_REQUEST.copy(isHumanApproval = true),
            dialog = VaultItemState.DialogState.PamRequestAccess(form = HUMAN_FORM),
        )
        capture("08-request-form-human-empty")

        mutableState.value = gatedItemState(
            pam = PAM_CAN_REQUEST.copy(isHumanApproval = true),
            dialog = VaultItemState.DialogState.PamRequestAccess(
                form = HUMAN_FORM.copy(reason = "Rotating the database credentials after INC-4821"),
            ),
        )
        capture("09-request-form-human-filled")
    }
    //endregion Vault item

    //region My access
    @Test
    fun myAccessScreens() {
        val mutableState = MutableStateFlow(MY_ACCESS_DEFAULT)
        val viewModel = mockk<MyAccessViewModel> {
            every { eventFlow } returns bufferedMutableSharedFlow()
            every { stateFlow } returns mutableState
            every { trySendAction(any()) } just runs
        }
        setScreenContent {
            MyAccessScreen(
                onNavigateBack = {},
                onNavigateToVaultItem = {},
                viewModel = viewModel,
            )
        }

        mutableState.value = MY_ACCESS_DEFAULT.copy(viewState = myAccessContent())
        capture("10-my-access-current")

        mutableState.value = MY_ACCESS_DEFAULT.copy(
            viewState = myAccessContent(),
            selectedTab = MyAccessState.Tab.HISTORY,
        )
        capture("11-my-access-history")

        mutableState.value = MY_ACCESS_DEFAULT.copy(
            viewState = MyAccessState.ViewState.Content(requests = emptyList()),
        )
        capture("12-my-access-empty")

        mutableState.value = MY_ACCESS_DEFAULT.copy(
            viewState = myAccessContent(),
            dialogState = MyAccessState.DialogState.Extend(
                leaseId = ACTIVE_LEASE_ID,
                durationMinutes = "30",
                reason = "Migration is taking longer than planned",
                maxDurationMinutes = 60L,
            ),
        )
        capture("13-my-access-extend-dialog")

        mutableState.value = MY_ACCESS_DEFAULT.copy(
            viewState = myAccessContent(),
            dialogState = MyAccessState.DialogState.ConfirmEnd(leaseId = ACTIVE_LEASE_ID),
        )
        capture("14-my-access-end-access-confirmation")

        mutableState.value = MY_ACCESS_DEFAULT.copy(
            viewState = myAccessContent(),
            dialogState = MyAccessState.DialogState.ConfirmCancel(requestId = "pending-1"),
        )
        capture("15-my-access-cancel-request-confirmation")

        mutableState.value = MY_ACCESS_DEFAULT.copy(
            viewState = myAccessContent(),
            showNotificationPrompt = true,
        )
        capture("16-my-access-notification-card")

        // The fake permission manager denies the permission permanently, which opens the dialog.
        composeTestRule.onNodeWithTag("PamEnableNotificationsButton").performClick()
        capture("17-my-access-notifications-off-dialog")
    }
    //endregion My access

    //region Settings
    @Test
    fun settingsNotificationsRowOn() {
        captureVaultSettings(isPermissionGranted = true, name = "25-settings-notifications-on")
    }

    @Test
    fun settingsNotificationsRowOff() {
        captureVaultSettings(isPermissionGranted = false, name = "26-settings-notifications-off")
    }

    private fun captureVaultSettings(isPermissionGranted: Boolean, name: String) {
        val permissionsManager = FakePermissionManager().apply {
            checkPermissionResult = isPermissionGranted
        }
        val viewModel = mockk<VaultSettingsViewModel> {
            every { eventFlow } returns bufferedMutableSharedFlow()
            every { stateFlow } returns MutableStateFlow(
                VaultSettingsState(
                    showImportActionCard = false,
                    showImportItemsChevron = true,
                    showMyAccessRequests = true,
                    showPamNotificationsRow = true,
                ),
            )
            every { trySendAction(any()) } just runs
        }
        setScreenContent(permissionsManager = permissionsManager) {
            VaultSettingsScreen(
                onNavigateBack = {},
                onNavigateToExportVault = {},
                onNavigateToFolders = {},
                onNavigateToImportLogins = {},
                onNavigateToImportItems = {},
                onNavigateToMyAccess = {},
                viewModel = viewModel,
            )
        }
        capture(name)
    }
    //endregion Settings

    //region Approver inbox
    @Test
    fun approverInboxScreens() {
        val permissionsManager = FakePermissionManager().apply {
            getPermissionsResult = false
            shouldShowRequestRationale = false
        }
        val mutableState = MutableStateFlow(INBOX_DEFAULT)
        val viewModel = mockk<PamApprovalsViewModel> {
            every { eventFlow } returns bufferedMutableSharedFlow()
            every { stateFlow } returns mutableState
            every { trySendAction(any()) } just runs
        }
        setScreenContent(permissionsManager = permissionsManager) {
            PamApprovalsScreen(
                onNavigateBack = {},
                onNavigateToDetail = {},
                viewModel = viewModel,
            )
        }

        mutableState.value = INBOX_DEFAULT.copy(viewState = inboxContent())
        capture("20-inbox-requests")

        mutableState.value = INBOX_DEFAULT.copy(
            viewState = PamApprovalsState.ViewState.Content(
                inbox = emptyList(),
                history = inboxContent().history,
            ),
        )
        capture("21-inbox-zero")

        mutableState.value = INBOX_DEFAULT.copy(
            viewState = inboxContent(),
            selectedTab = PamApprovalsState.Tab.HISTORY,
        )
        capture("22-inbox-history")

        mutableState.value = INBOX_DEFAULT.copy(
            viewState = inboxContent(),
            showNotificationPrompt = true,
        )
        capture("23-inbox-notification-card")

        // The fake permission manager denies the permanently, which opens the settings dialog.
        composeTestRule.onNodeWithTag("PamEnableNotificationsButton").performClick()
        capture("24-inbox-notifications-off-dialog")
    }
    //endregion Approver inbox

    //region Approval detail
    @Test
    fun approvalDetailScreens() {
        val mutableState = MutableStateFlow(DETAIL_DEFAULT)
        val viewModel = mockk<PamApprovalDetailViewModel> {
            every { eventFlow } returns bufferedMutableSharedFlow()
            every { stateFlow } returns mutableState
            every { trySendAction(any()) } just runs
        }
        setScreenContent {
            PamApprovalDetailScreen(onNavigateBack = {}, viewModel = viewModel)
        }

        mutableState.value = detailState(detail())
        capture("30-detail-pending")

        mutableState.value = detailState(
            detail = detail(),
            dialog = PamApprovalDetailState.DialogState.Decide(
                verdict = PamDecisionVerdict.APPROVE,
                comment = "Approved for the incident window",
            ),
        )
        capture("31-detail-approve-dialog")

        mutableState.value = detailState(
            detail = detail(),
            dialog = PamApprovalDetailState.DialogState.Decide(
                verdict = PamDecisionVerdict.DENY,
                comment = "",
            ),
        )
        capture("32-detail-deny-dialog-reason-required")

        mutableState.value = detailState(
            detail = detail(),
            dialog = PamApprovalDetailState.DialogState.Decide(
                verdict = PamDecisionVerdict.DENY,
                comment = "Use the break-glass process for incidents",
            ),
        )
        capture("33-detail-deny-dialog-filled")

        mutableState.value = detailState(
            detail(
                requesterName = "Ada Lovelace",
                requesterEmail = "ada@example.com",
                isOwnRequest = true,
                canDecide = false,
            ),
        )
        capture("34-detail-own-request")

        mutableState.value = detailState(
            detail(
                status = PamRequestStatus.APPROVED,
                canDecide = false,
                decidedBy = "Alan Turing",
                decisionComment = "Approved for the incident window",
            ),
        )
        capture("35-detail-already-approved")

        mutableState.value = detailState(
            detail(
                status = PamRequestStatus.DENIED,
                canDecide = false,
                decidedBy = "Alan Turing",
                decisionComment = "Use the break-glass process for incidents",
            ),
        )
        capture("36-detail-already-denied")

        mutableState.value = detailState(
            detail = detail(canDecide = false),
            dialog = PamApprovalDetailState.DialogState.Error(
                message = "This request was already decided by another approver.".asText(),
            ),
        )
        capture("37-detail-already-decided-error")

        val revocable = detail(
            status = PamRequestStatus.APPROVED,
            canDecide = false,
            decidedBy = "Alan Turing",
            canRevoke = true,
            accessEndsAt = Instant.parse("2026-01-01T13:30:00Z"),
        )
        mutableState.value = detailState(revocable)
        capture("38-detail-active-access-revoke")

        mutableState.value = detailState(
            detail = revocable,
            dialog = PamApprovalDetailState.DialogState.ConfirmRevoke,
        )
        capture("39-detail-revoke-confirmation")
    }
    //endregion Approval detail

    //region Vault tab
    @Test
    fun vaultListAccessRequests() {
        val mutableState = MutableStateFlow(VAULT_DEFAULT.copy(pamApprovalCount = 0))
        setVaultContent(mutableState)
        capture("40-vault-list-access-requests-none-pending")

        mutableState.value = VAULT_DEFAULT.copy(pamApprovalCount = 3)
        capture("41-vault-list-access-requests-pending")

        mutableState.value = VAULT_DEFAULT.copy(pamApprovalCount = null)
        capture("42-vault-list-no-access-requests-for-non-approver")
    }

    private fun setVaultContent(mutableState: MutableStateFlow<VaultState>) {
        val viewModel = mockk<VaultViewModel>(relaxed = true) {
            every { eventFlow } returns bufferedMutableSharedFlow()
            every { stateFlow } returns mutableState
        }
        setScreenContent {
            VaultScreen(
                viewModel = viewModel,
                onNavigateToVaultAddItemScreen = {},
                onNavigateToVaultItemScreen = {},
                onNavigateToVaultEditItemScreen = {},
                onNavigateToVaultItemListingScreen = {},
                onDimBottomNavBarRequest = {},
                onNavigateToVerificationCodeScreen = {},
                onNavigateToSearchVault = {},
                onNavigateToImportLogins = {},
                onNavigateToAddFolderScreen = {},
                onNavigateToAboutScreen = {},
                onNavigateToAutofillScreen = {},
                onNavigateToPlan = {},
                onNavigateToPamApprovals = {},
                onNavigateToPamMyAccess = {},
            )
        }
    }
    //endregion Vault tab

    //region Capture helpers
    private fun setScreenContent(
        permissionsManager: FakePermissionManager = FakePermissionManager(),
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        setContent(
            theme = AppTheme.LIGHT,
            clock = clock,
            intentManager = mockk<IntentManager>(relaxed = true),
            permissionsManager = permissionsManager,
            featureFlagsState = FeatureFlagsState(isVfo1FoundationEnabled = true),
        ) {
            // The theme is state so one composition can be captured in both themes.
            BitwardenTheme(theme = currentTheme, content = content)
        }
    }

    /**
     * Captures the current screen in light and dark theme, composing every window (the activity
     * and any dialog or popup, which live in windows of their own) over each other.
     *
     * The windows are drawn directly instead of with `captureToImage`, which waits for the app
     * to be idle and never is while a focused text field blinks its cursor.
     */
    private fun capture(name: String) {
        listOf(AppTheme.LIGHT to "light", AppTheme.DARK to "dark").forEach { (theme, suffix) ->
            currentTheme = theme
            settle()
            var bitmap: Bitmap? = null
            composeTestRule.runOnUiThread { bitmap = drawWindows() }
            File(outputDir, "$name-$suffix.png").outputStream().use {
                check(requireNotNull(bitmap).compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it)) {
                    "Could not write $name-$suffix.png"
                }
            }
        }
    }

    /**
     * Lets the UI settle. A focused text field blinks its cursor forever, and the app is never
     * idle while it does, so focus is cleared in every window first.
     */
    private fun settle() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnUiThread {
            Snapshot.sendApplyNotifications()
            windowViews().forEach { it.clearFocus() }
        }
        composeTestRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeTestRule.runOnUiThread { windowViews().forEach { it.clearFocus() } }
        composeTestRule.mainClock.autoAdvance = true
        // A window that keeps animating never gets idle; the layout is done by then anyway.
        @Suppress("SwallowedException")
        try {
            composeTestRule.waitForIdle()
        } catch (e: RuntimeException) {
            check(e.javaClass.simpleName == "AppNotIdleException") { "Unexpected: $e" }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun windowViews(): List<View> {
        val global = Class.forName("android.view.WindowManagerGlobal")
        val instance = global.getMethod("getInstance").invoke(null)
        val names = global.getMethod("getViewRootNames").invoke(instance) as Array<String>
        val getRootView = global.getMethod("getRootView", String::class.java)
        return names.map { getRootView.invoke(instance, it) as View }
    }

    private fun drawWindows(): Bitmap {
        val windows = windowViews()
        val main = windows.first {
            (it.layoutParams as? WindowManager.LayoutParams)?.type ==
                WindowManager.LayoutParams.TYPE_BASE_APPLICATION
        }
        val base = main.drawToBitmap()
        val overlays = windows.filter { it !== main }
        if (overlays.isNotEmpty()) {
            Canvas(base).drawColor(Color.argb(SCRIM_ALPHA, 0, 0, 0))
        }
        overlays.forEach { view ->
            val params = view.layoutParams as? WindowManager.LayoutParams
            val gravity = params?.gravity ?: Gravity.CENTER
            val isCenteredX = gravity and Gravity.HORIZONTAL_GRAVITY_MASK ==
                Gravity.CENTER_HORIZONTAL
            val isCenteredY = gravity and Gravity.VERTICAL_GRAVITY_MASK ==
                Gravity.CENTER_VERTICAL
            val left = if (isCenteredX) (base.width - view.width) / 2 else params?.x ?: 0
            val top = if (isCenteredY) (base.height - view.height) / 2 else params?.y ?: 0
            Canvas(base).drawBitmap(view.drawToBitmap(), left.toFloat(), top.toFloat(), Paint())
        }
        return base
    }

    private fun View.drawToBitmap(): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { draw(Canvas(it)) }
    //endregion Capture helpers
}

private const val ENABLE_PROPERTY = "pam.screenshots"
private const val OUTPUT_DIR_PROPERTY = "pam.screenshots.dir"
private const val DEFAULT_OUTPUT_DIR =
    "/Users/demo/Documents/development/bitwarden/pam-262-notes/screenshots"
private const val IDLE_TIMEOUT_SECONDS = 20L
private const val SETTLE_MILLIS = 1_000L
private const val PNG_QUALITY = 100
private const val SCRIM_ALPHA = 100
private const val ACTIVE_LEASE_ID = "lease-1"
private val NOW: Instant = Instant.parse("2026-01-01T11:30:00Z")

//region Fake data
private const val ACTIVE_TOTAL_MINUTES = 120L

private val PAM_CAN_REQUEST = VaultItemPamState.CanRequest(
    isHumanApproval = false,
    defaultDurationMinutes = 60L,
    maxDurationMinutes = 480L,
    isSlotHeldByAnotherMember = false,
)

private val AUTOMATIC_FORM = VaultItemPamRequestForm(
    isHumanApproval = false,
    durationMinutes = "60",
    maxDurationMinutes = 480L,
    startDate = LocalDate.of(2026, 1, 1),
    startMinutes = 12 * 60,
    endDate = LocalDate.of(2026, 1, 1),
    endMinutes = 13 * 60,
    reason = "",
)

private val HUMAN_FORM = AUTOMATIC_FORM.copy(isHumanApproval = true)

private val ITEM_COMMON = VaultItemState.ViewState.Content.Common(
    name = "Prod DB",
    created = BitwardenString.created.asText("Dec 1, 2025, 9:00 AM"),
    lastUpdated = BitwardenString.last_edited.asText("Dec 20, 2025, 4:15 PM"),
    notes = null,
    customFields = persistentListOf(),
    requiresCloneConfirmation = false,
    attachments = persistentListOf(),
    canDelete = false,
    canRestore = false,
    canAssignToCollections = false,
    canEdit = false,
    favorite = false,
    archived = false,
    passwordHistoryCount = null,
    iconData = IconData.Local(iconRes = BitwardenDrawable.ic_globe),
    relatedLocations = persistentListOf(),
    hasOrganizations = true,
)

private val MASKED_LOGIN = VaultItemState.ViewState.Content.ItemType.Login(
    username = null,
    passwordData = null,
    uris = listOf(
        VaultItemState.ViewState.Content.ItemType.Login.UriData(
            uri = "https://db.prod.example.com",
            isCopyable = true,
            isLaunchable = true,
        ),
    ),
    passwordRevisionDate = null,
    totpCodeItemData = null,
    isPremiumUser = true,
    canViewTotpCode = false,
    fido2CredentialCreationDateText = null,
)

private val UNMASKED_LOGIN = MASKED_LOGIN.copy(
    username = "svc-prod-admin",
    passwordData = VaultItemState.ViewState.Content.ItemType.Login.PasswordData(
        password = "correct-horse-battery-staple",
        isVisible = false,
        canViewPassword = true,
    ),
)

private fun itemState(
    login: VaultItemState.ViewState.Content.ItemType.Login,
    pam: VaultItemPamState?,
    dialog: VaultItemState.DialogState? = null,
) = VaultItemState(
    vaultItemId = "item-1",
    cipherType = VaultItemCipherType.LOGIN,
    viewState = VaultItemState.ViewState.Content(common = ITEM_COMMON, type = login),
    dialog = dialog,
    baseIconUrl = "https://icons.example.com/",
    isIconLoadingDisabled = true,
    hasPremium = true,
    pamAccess = pam,
)

private fun gatedItemState(
    pam: VaultItemPamState,
    dialog: VaultItemState.DialogState? = null,
) = itemState(login = MASKED_LOGIN, pam = pam, dialog = dialog)

private fun activeItemState(remainingMinutes: Long) = itemState(
    login = UNMASKED_LOGIN,
    pam = VaultItemPamState.Active(
        remainingMinutes = remainingMinutes,
        totalMinutes = ACTIVE_TOTAL_MINUTES,
    ),
)

private val MY_ACCESS_DEFAULT = MyAccessState(
    viewState = MyAccessState.ViewState.Loading,
    selectedTab = MyAccessState.Tab.CURRENT,
    isRefreshing = false,
    dialogState = null,
    showNotificationPrompt = false,
)

@Suppress("LongParameterList")
private fun myAccessRequest(
    id: String,
    itemName: String,
    stage: MyAccessState.Stage,
    submittedAt: String,
    reason: String? = null,
    leaseId: String? = null,
    leaseEndsAt: Instant? = null,
    canExtend: Boolean = false,
) = MyAccessState.RequestItem(
    id = id,
    cipherId = "cipher-$id",
    itemName = itemName,
    itemIcon = IconData.Local(iconRes = BitwardenDrawable.ic_globe),
    stage = stage,
    isExtension = false,
    reason = reason,
    submittedAt = Instant.parse(submittedAt),
    leaseId = leaseId,
    leaseEndsAt = leaseEndsAt,
    canExtend = canExtend,
    maxExtensionMinutes = 60L.takeIf { canExtend },
)

private fun myAccessContent() = MyAccessState.ViewState.Content(
    requests = listOf(
        myAccessRequest(
            id = "active-1",
            itemName = "Prod DB",
            stage = MyAccessState.Stage.ACTIVE,
            submittedAt = "2026-01-01T10:00:00Z",
            reason = "Rotating credentials after INC-4821",
            leaseId = ACTIVE_LEASE_ID,
            // The countdown reads the wall clock, so it is relative to now.
            leaseEndsAt = Instant.now().plusSeconds(90 * 60L),
            canExtend = true,
        ),
        myAccessRequest(
            id = "ready-1",
            itemName = "Staging Admin",
            stage = MyAccessState.Stage.READY,
            submittedAt = "2026-01-01T09:30:00Z",
            reason = "Deploy the release candidate",
        ),
        myAccessRequest(
            id = "pending-1",
            itemName = "Billing Portal",
            stage = MyAccessState.Stage.PENDING,
            submittedAt = "2026-01-01T11:15:00Z",
            reason = "Quarterly invoice reconciliation",
        ),
        myAccessRequest(
            id = "denied-1",
            itemName = "Prod DB",
            stage = MyAccessState.Stage.DENIED,
            submittedAt = "2025-12-28T14:00:00Z",
            reason = "Check replication lag",
        ),
        myAccessRequest(
            id = "expired-1",
            itemName = "Staging Admin",
            stage = MyAccessState.Stage.EXPIRED,
            submittedAt = "2025-12-22T08:45:00Z",
        ),
        myAccessRequest(
            id = "revoked-1",
            itemName = "Analytics Warehouse",
            stage = MyAccessState.Stage.REVOKED,
            submittedAt = "2025-12-20T16:20:00Z",
            reason = "Export the Q4 usage report",
        ),
        myAccessRequest(
            id = "cancelled-1",
            itemName = "Billing Portal",
            stage = MyAccessState.Stage.CANCELLED,
            submittedAt = "2025-12-18T13:05:00Z",
        ),
    ),
)

private val INBOX_DEFAULT = PamApprovalsState(
    viewState = PamApprovalsState.ViewState.Loading,
    selectedTab = PamApprovalsState.Tab.INBOX,
    isRefreshing = false,
    showNotificationPrompt = false,
)

@Suppress("LongParameterList")
private fun inboxRequest(
    id: String,
    itemName: String,
    collectionName: String,
    requesterName: String,
    status: PamRequestStatus,
    submittedAt: String,
    isOwnRequest: Boolean = false,
) = PamApprovalsState.RequestItem(
    id = id,
    itemName = itemName,
    itemIcon = IconData.Local(iconRes = BitwardenDrawable.ic_globe),
    collectionName = collectionName,
    requesterName = requesterName,
    status = status,
    windowStart = Instant.parse("2026-01-01T12:00:00Z"),
    windowEnd = Instant.parse("2026-01-01T13:30:00Z"),
    submittedAt = Instant.parse(submittedAt),
    isOwnRequest = isOwnRequest,
)

private fun inboxContent() = PamApprovalsState.ViewState.Content(
    inbox = listOf(
        inboxRequest(
            id = "r1",
            itemName = "Prod DB",
            collectionName = "Production",
            requesterName = "Grace Hopper",
            status = PamRequestStatus.PENDING,
            submittedAt = "2026-01-01T10:45:00Z",
        ),
        inboxRequest(
            id = "r2",
            itemName = "Staging Admin",
            collectionName = "Staging",
            requesterName = "Alan Turing",
            status = PamRequestStatus.PENDING,
            submittedAt = "2026-01-01T11:05:00Z",
        ),
        inboxRequest(
            id = "r3",
            itemName = "Billing Portal",
            collectionName = "Finance",
            requesterName = "Ada Lovelace (you)",
            status = PamRequestStatus.PENDING,
            submittedAt = "2026-01-01T11:20:00Z",
            isOwnRequest = true,
        ),
    ),
    history = listOf(
        inboxRequest(
            id = "h1",
            itemName = "Prod DB",
            collectionName = "Production",
            requesterName = "Grace Hopper",
            status = PamRequestStatus.APPROVED,
            submittedAt = "2025-12-30T09:00:00Z",
        ),
        inboxRequest(
            id = "h2",
            itemName = "Analytics Warehouse",
            collectionName = "Data",
            requesterName = "Alan Turing",
            status = PamRequestStatus.DENIED,
            submittedAt = "2025-12-29T15:30:00Z",
        ),
        inboxRequest(
            id = "h3",
            itemName = "Staging Admin",
            collectionName = "Staging",
            requesterName = "Grace Hopper",
            status = PamRequestStatus.EXPIRED,
            submittedAt = "2025-12-27T08:10:00Z",
        ),
    ),
)

private val DETAIL_DEFAULT = PamApprovalDetailState(
    requestId = "r1",
    viewState = PamApprovalDetailState.ViewState.Loading,
    dialogState = null,
)

private fun detailState(
    detail: PamApprovalDetailState.Detail,
    dialog: PamApprovalDetailState.DialogState? = null,
) = DETAIL_DEFAULT.copy(
    viewState = PamApprovalDetailState.ViewState.Content(detail = detail),
    dialogState = dialog,
)

@Suppress("LongParameterList")
private fun detail(
    requesterName: String = "Grace Hopper",
    requesterEmail: String = "grace@example.com",
    status: PamRequestStatus = PamRequestStatus.PENDING,
    isOwnRequest: Boolean = false,
    canDecide: Boolean = true,
    decidedBy: String? = null,
    decisionComment: String? = null,
    canRevoke: Boolean = false,
    accessEndsAt: Instant? = null,
) = PamApprovalDetailState.Detail(
    itemName = "Prod DB",
    itemIcon = IconData.Network(
        uri = "https://icons.bitwarden.net/proddb.example.com/icon.png",
        fallbackIconRes = BitwardenDrawable.ic_globe,
    ),
    organizationName = "Meridian Group",
    collectionName = "Production",
    requesterName = requesterName,
    requesterEmail = requesterEmail,
    durationMinutes = 90L,
    windowStart = Instant.parse("2026-01-01T12:00:00Z"),
    windowEnd = Instant.parse("2026-01-01T13:30:00Z"),
    reason = "Rotating the database credentials after INC-4821",
    status = status,
    isOwnRequest = isOwnRequest,
    canDecide = canDecide,
    decidedBy = decidedBy,
    decisionComment = decisionComment,
    canRevoke = canRevoke,
    accessEndsAt = accessEndsAt,
)

private val VAULT_DEFAULT = VaultState(
    appBarTitle = BitwardenString.my_vault.asText(),
    avatarColorString = "#aa00aa",
    initials = "AL",
    accountSummaries = persistentListOf(),
    viewState = VaultState.ViewState.Content(
        loginItemsCount = 12,
        cardItemsCount = 2,
        identityItemsCount = 1,
        secureNoteItemsCount = 3,
        favoriteItems = emptyList(),
        folderItems = emptyList(),
        noFolderItems = emptyList(),
        collectionItems = emptyList(),
        trashItemsCount = 0,
        totpItemsCount = 4,
        itemTypesCount = 4,
        sshKeyItemsCount = 0,
        bankAccountItemsCount = 0,
        licenseItemsCount = 0,
        passportItemsCount = 0,
        archivedItemsCount = 0,
        archiveSubText = null,
        archiveEndIcon = null,
        showCardGroup = true,
        showBankAccountGroup = false,
        showLicenseGroup = false,
        showPassportGroup = false,
    ),
    isPremium = true,
    isPullToRefreshSettingEnabled = false,
    baseIconUrl = "https://icons.example.com/",
    isIconLoadingDisabled = true,
    hasMasterPassword = true,
    isRefreshing = false,
    showImportActionCard = false,
    flightRecorderSnackBar = null,
    cipherDecryptionFailureIds = persistentListOf(),
    hasShownDecryptionFailureAlert = false,
    restrictItemTypesPolicyOrgIds = emptyList(),
    isIntroducingArchiveActionCardDismissed = false,
    validTotpIds = persistentSetOf(),
    policyBanner = null,
    pamApprovalCount = 3,
)
//endregion Fake data
