package com.x8bit.bitwarden.ui.vault.feature.pamnotificationprompt

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.Lifecycle
import com.bitwarden.ui.platform.base.util.LifecycleEventEffect
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.util.startAppNotificationSettingsActivity
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.composition.LocalPermissionsManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager

private const val NOTIFICATION_PERMISSION: String = Manifest.permission.POST_NOTIFICATIONS

/**
 * A settings row showing whether the PAM alert notifications are on. Tapping it asks for the
 * notification permission when the OS can still show the prompt, and otherwise opens the system
 * notification settings for the app.
 *
 * The On/Off state comes from the notification permission alone: before Android 13 the
 * [PermissionsManager] reports it as granted whenever the app's notifications are enabled.
 */
@Composable
fun PamNotificationsSettingRow(
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
    intentManager: IntentManager = LocalIntentManager.current,
) {
    var isEnabled by remember {
        mutableStateOf(permissionsManager.checkPermission(NOTIFICATION_PERMISSION))
    }
    val launcher = permissionsManager.getLauncher { isGranted ->
        isEnabled = isGranted
        if (!isGranted &&
            !permissionsManager.shouldShowRequestPermissionRationale(NOTIFICATION_PERMISSION)
        ) {
            intentManager.startAppNotificationSettingsActivity()
        }
    }
    LifecycleEventEffect { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            isEnabled = permissionsManager.checkPermission(NOTIFICATION_PERMISSION)
        }
    }
    BitwardenTextRow(
        text = stringResource(id = BitwardenString.pam_access_request_notifications),
        description = AnnotatedString(
            stringResource(
                id = if (isEnabled) {
                    BitwardenString.pam_notifications_state_on
                } else {
                    BitwardenString.pam_notifications_state_off
                },
            ),
        ),
        onClick = {
            if (permissionsManager.checkPermission(NOTIFICATION_PERMISSION)) {
                intentManager.startAppNotificationSettingsActivity()
            } else {
                @SuppressLint("InlinedApi")
                launcher.launch(NOTIFICATION_PERMISSION)
            }
        },
        cardStyle = cardStyle,
        modifier = modifier,
    )
}
