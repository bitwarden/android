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
    FlagKey.VerifiedSsoDomainEndpoint,
    FlagKey.CredentialExchangeProtocolImport,
    FlagKey.CredentialExchangeProtocolExport,
    FlagKey.AppReviewPrompt,
    FlagKey.CipherKeyEncryption,
    FlagKey.NewDevicePermanentDismiss,
    FlagKey.NewDeviceTemporaryDismiss,
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
    FlagKey.VerifiedSsoDomainEndpoint -> stringResource(R.string.verified_sso_domain_verified)
    FlagKey.CredentialExchangeProtocolImport -> stringResource(R.string.cxp_import)
    FlagKey.CredentialExchangeProtocolExport -> stringResource(R.string.cxp_export)
    FlagKey.AppReviewPrompt -> stringResource(R.string.app_review_prompt)
    FlagKey.CipherKeyEncryption -> stringResource(R.string.cipher_key_encryption)
    FlagKey.NewDevicePermanentDismiss -> stringResource(R.string.new_device_permanent_dismiss)
    FlagKey.NewDeviceTemporaryDismiss -> stringResource(R.string.new_device_temporary_dismiss)
}
