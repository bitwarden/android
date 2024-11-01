package com.x8bit.bitwarden.ui.platform.feature.debugmenu.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch

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

    FlagKey.AuthenticatorSync,
    FlagKey.EmailVerification,
    FlagKey.OnboardingCarousel,
    FlagKey.OnboardingFlow,
    FlagKey.ImportLoginsFlow,
    FlagKey.SshKeyCipherItems,
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
    BitwardenSwitch(
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

    FlagKey.AuthenticatorSync -> stringResource(R.string.authenticator_sync)
    FlagKey.EmailVerification -> stringResource(R.string.email_verification)
    FlagKey.OnboardingCarousel -> stringResource(R.string.onboarding_carousel)
    FlagKey.OnboardingFlow -> stringResource(R.string.onboarding_flow)
    FlagKey.ImportLoginsFlow -> stringResource(R.string.import_logins_flow)
    FlagKey.SshKeyCipherItems -> stringResource(R.string.ssh_key_cipher_item_types)
}
