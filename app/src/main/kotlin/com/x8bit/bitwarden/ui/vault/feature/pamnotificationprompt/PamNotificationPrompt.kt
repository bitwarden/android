package com.x8bit.bitwarden.ui.vault.feature.pamnotificationprompt

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import com.bitwarden.ui.platform.base.util.LifecycleEventEffect
import com.bitwarden.ui.platform.components.button.model.BitwardenButtonData
import com.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.util.startAppSettingsActivity
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.composition.LocalPermissionsManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager

private const val NOTIFICATION_PERMISSION: String = Manifest.permission.POST_NOTIFICATIONS

/**
 * Handles the Android 13+ notification permission for the PAM alert prompt cards.
 *
 * @property hasPermission Whether the permission is currently granted.
 * @property onEnableClick Asks the OS for the permission.
 */
@Stable
class PamNotificationPromptState internal constructor(
    private val hasPermissionState: () -> Boolean,
    val onEnableClick: () -> Unit,
) {
    val hasPermission: Boolean get() = hasPermissionState()
}

/**
 * Remembers a [PamNotificationPromptState] and shows the "notifications are off" dialog when the
 * OS will not ask again. [onPermissionGranted] is called when the user allows notifications.
 */
@Composable
fun rememberPamNotificationPromptState(
    onPermissionGranted: () -> Unit,
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
    intentManager: IntentManager = LocalIntentManager.current,
): PamNotificationPromptState {
    var hasPermission by remember {
        mutableStateOf(permissionsManager.checkPermission(NOTIFICATION_PERMISSION))
    }
    var shouldShowSettingsDialog by rememberSaveable { mutableStateOf(false) }
    val launcher = permissionsManager.getLauncher { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            onPermissionGranted()
        } else if (
            !permissionsManager.shouldShowRequestPermissionRationale(NOTIFICATION_PERMISSION)
        ) {
            shouldShowSettingsDialog = true
        }
    }
    LifecycleEventEffect { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            hasPermission = permissionsManager.checkPermission(NOTIFICATION_PERMISSION)
        }
    }
    if (shouldShowSettingsDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = BitwardenString.pam_notifications_off_title),
            message = stringResource(id = BitwardenString.pam_notifications_off_message),
            confirmButtonText = stringResource(id = BitwardenString.go_to_settings),
            dismissButtonText = stringResource(id = BitwardenString.no_thanks),
            onConfirmClick = {
                shouldShowSettingsDialog = false
                intentManager.startAppSettingsActivity()
            },
            onDismissClick = { shouldShowSettingsDialog = false },
            onDismissRequest = { shouldShowSettingsDialog = false },
        )
    }
    return remember(launcher) {
        PamNotificationPromptState(
            hasPermissionState = { hasPermission },
            onEnableClick = {
                @SuppressLint("InlinedApi")
                launcher.launch(NOTIFICATION_PERMISSION)
            },
        )
    }
}

/**
 * The card asking the user to enable notifications for the PAM alerts.
 */
@Composable
fun PamNotificationActionCard(
    title: String,
    description: String,
    onEnableClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenActionCard(
        cardTitle = title,
        cardSubtitle = description,
        actionButton = BitwardenButtonData(
            label = BitwardenString.enable_notifications.asText(),
            onClick = onEnableClick,
            testTag = "PamEnableNotificationsButton",
        ),
        onDismissClick = onDismissClick,
        modifier = modifier.testTag("PamNotificationActionCard"),
    )
}
