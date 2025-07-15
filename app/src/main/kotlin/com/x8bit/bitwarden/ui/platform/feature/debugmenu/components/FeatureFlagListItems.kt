package com.x8bit.bitwarden.ui.platform.feature.debugmenu.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey

/**
 * Creates a list item for a [FlagKey].
 */
@Composable
fun <T : Any> FlagKey<T>.ListItemContent(
    currentValue: T,
    onValueChange: (key: FlagKey<T>, value: T) -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) = when (val flagKey = this) {
    FlagKey.DummyBoolean,
    is FlagKey.DummyInt,
    FlagKey.DummyString,
        -> {
        Unit
    }

    FlagKey.EmailVerification,
    FlagKey.ImportLoginsFlow,
    FlagKey.CredentialExchangeProtocolImport,
    FlagKey.CredentialExchangeProtocolExport,
    FlagKey.CipherKeyEncryption,
    FlagKey.MutualTls,
    FlagKey.SingleTapPasskeyCreation,
    FlagKey.SingleTapPasskeyAuthentication,
    FlagKey.AnonAddySelfHostAlias,
    FlagKey.SimpleLoginSelfHostAlias,
    FlagKey.ChromeAutofill,
    FlagKey.MobileErrorReporting,
    FlagKey.RestrictCipherItemDeletion,
    FlagKey.UserManagedPrivilegedApps,
    FlagKey.RemoveCardPolicy,
        -> {
        @Suppress("UNCHECKED_CAST")
        BooleanFlagItem(
            label = flagKey.getDisplayLabel(),
            key = flagKey as FlagKey<Boolean>,
            currentValue = currentValue as Boolean,
            onValueChange = onValueChange as (FlagKey<Boolean>, Boolean) -> Unit,
            cardStyle = cardStyle,
            modifier = modifier,
        )
    }
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
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    BitwardenSwitch(
        label = label,
        isChecked = currentValue,
        onCheckedChange = { onValueChange(key, it) },
        cardStyle = cardStyle,
        modifier = modifier,
    )
}

@Composable
private fun <T : Any> FlagKey<T>.getDisplayLabel(): String = when (this) {
    FlagKey.DummyBoolean,
    is FlagKey.DummyInt,
    FlagKey.DummyString,
        -> this.keyName

    FlagKey.EmailVerification -> stringResource(R.string.email_verification)
    FlagKey.ImportLoginsFlow -> stringResource(R.string.import_logins_flow)
    FlagKey.CredentialExchangeProtocolImport -> stringResource(R.string.cxp_import)
    FlagKey.CredentialExchangeProtocolExport -> stringResource(R.string.cxp_export)
    FlagKey.CipherKeyEncryption -> stringResource(R.string.cipher_key_encryption)
    FlagKey.MutualTls -> stringResource(R.string.mutual_tls)
    FlagKey.SingleTapPasskeyCreation -> stringResource(R.string.single_tap_passkey_creation)
    FlagKey.SingleTapPasskeyAuthentication -> {
        stringResource(R.string.single_tap_passkey_authentication)
    }

    FlagKey.AnonAddySelfHostAlias -> stringResource(R.string.anon_addy_self_hosted_aliases)
    FlagKey.SimpleLoginSelfHostAlias -> stringResource(R.string.simple_login_self_hosted_aliases)
    FlagKey.ChromeAutofill -> stringResource(R.string.enable_chrome_autofill)
    FlagKey.MobileErrorReporting -> stringResource(R.string.enable_error_reporting_dialog)
    FlagKey.RestrictCipherItemDeletion -> stringResource(R.string.restrict_item_deletion)
    FlagKey.UserManagedPrivilegedApps -> {
        stringResource(R.string.user_trusted_privileged_app_management)
    }

    FlagKey.RemoveCardPolicy -> stringResource(R.string.remove_card_policy)
}
