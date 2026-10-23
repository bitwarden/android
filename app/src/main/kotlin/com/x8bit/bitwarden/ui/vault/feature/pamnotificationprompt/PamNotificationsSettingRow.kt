package com.x8bit.bitwarden.ui.vault.feature.pamnotificationprompt

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import com.bitwarden.core.util.isBuildVersionAtLeast
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
 * Whether the PAM alert notifications can reach the user. From Android 13 the runtime permission
 * must be granted as well as the app level notification switch being on.
 */
fun isPamNotificationsEnabled(
    isRuntimePermissionRequired: Boolean,
    isPermissionGranted: Boolean,
    areAppNotificationsEnabled: Boolean,
): Boolean = areAppNotificationsEnabled && (!isRuntimePermissionRequired || isPermissionGranted)

/**
 * A settings row showing whether the PAM alert notifications are on. Tapping it asks for the
 * runtime permission when the OS can still show the prompt, and otherwise opens the system
 * notification settings for the app.
 */
@Composable
fun PamNotificationsSettingRow(
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val context = LocalContext.current
    val isRuntimePermissionRequired = isBuildVersionAtLeast(Build.VERSION_CODES.TIRAMISU)
    val readState = {
        isPamNotificationsEnabled(
            isRuntimePermissionRequired = isRuntimePermissionRequired,
            isPermissionGranted = !isRuntimePermissionRequired ||
                permissionsManager.checkPermission(NOTIFICATION_PERMISSION),
            areAppNotificationsEnabled = NotificationManagerCompat.from(context)
                .areNotificationsEnabled(),
        )
    }
    var isEnabled by remember { mutableStateOf(readState()) }
    val launcher = permissionsManager.getLauncher { isGranted ->
        isEnabled = readState()
        if (!isGranted &&
            !permissionsManager.shouldShowRequestPermissionRationale(NOTIFICATION_PERMISSION)
        ) {
            intentManager.startAppNotificationSettingsActivity()
        }
    }
    LifecycleEventEffect { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            isEnabled = readState()
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
            if (isRuntimePermissionRequired &&
                !permissionsManager.checkPermission(NOTIFICATION_PERMISSION)
            ) {
                @SuppressLint("InlinedApi")
                launcher.launch(NOTIFICATION_PERMISSION)
            } else {
                intentManager.startAppNotificationSettingsActivity()
            }
        },
        withDivider = false,
        cardStyle = cardStyle,
        modifier = modifier,
    )
}
