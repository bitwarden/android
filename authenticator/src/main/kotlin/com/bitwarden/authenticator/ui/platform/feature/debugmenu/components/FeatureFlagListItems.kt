package com.bitwarden.authenticator.ui.platform.feature.debugmenu.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.platform.manager.model.FlagKey
import com.bitwarden.authenticator.ui.platform.components.toggle.BitwardenWideSwitch

/**
 * Creates a list item for a [FlagKey].
 */
@Suppress("UNCHECKED_CAST")
@Composable
fun <T : Any> FlagKey<T>.ListItemContent(
    currentValue: T,
    onValueChange: (key: FlagKey<T>, value: T) -> Unit,
    modifier: Modifier = Modifier,
) = when (val flagKey = this) {
    FlagKey.DummyBoolean,
    is FlagKey.DummyInt,
    FlagKey.DummyString,
        -> Unit

    FlagKey.BitwardenAuthenticationEnabled,
    FlagKey.PasswordManagerSync,
        -> BooleanFlagItem(
        label = flagKey.getDisplayLabel(),
        key = flagKey as FlagKey<Boolean>,
        currentValue = currentValue as Boolean,
        onValueChange = onValueChange as (FlagKey<Boolean>, Boolean) -> Unit,
        modifier = modifier,
    )
}

/**
 * The UI layout for a boolean backed flag key.
 */
@Composable
private fun BooleanFlagItem(
    label: String,
    key: FlagKey<Boolean>,
    currentValue: Boolean,
    onValueChange: (key: FlagKey<Boolean>, value: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenWideSwitch(
        label = label,
        isChecked = currentValue,
        onCheckedChange = {
            onValueChange(key, it)
        },
        modifier = modifier,
    )
}

@Composable
private fun <T : Any> FlagKey<T>.getDisplayLabel(): String = when (this) {
    FlagKey.DummyBoolean,
    is FlagKey.DummyInt,
    FlagKey.DummyString,
        -> this.keyName

    FlagKey.BitwardenAuthenticationEnabled ->
        stringResource(R.string.bitwarden_authentication_enabled)

    FlagKey.PasswordManagerSync -> stringResource(R.string.password_manager_sync)
}
