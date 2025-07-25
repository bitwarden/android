package com.bitwarden.authenticator.ui.platform.feature.debugmenu.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bitwarden.authenticator.ui.platform.components.toggle.BitwardenWideSwitch
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.ui.platform.resource.BitwardenString

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
    FlagKey.CipherKeyEncryption,
    FlagKey.CredentialExchangeProtocolExport,
    FlagKey.CredentialExchangeProtocolImport,
    FlagKey.EmailVerification,
    FlagKey.RemoveCardPolicy,
    FlagKey.RestrictCipherItemDeletion,
    FlagKey.UserManagedPrivilegedApps,
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

    FlagKey.EmailVerification -> stringResource(BitwardenString.email_verification)
    FlagKey.CredentialExchangeProtocolImport -> stringResource(BitwardenString.cxp_import)
    FlagKey.CredentialExchangeProtocolExport -> stringResource(BitwardenString.cxp_export)
    FlagKey.CipherKeyEncryption -> stringResource(BitwardenString.cipher_key_encryption)
    FlagKey.RestrictCipherItemDeletion -> stringResource(BitwardenString.restrict_item_deletion)
    FlagKey.UserManagedPrivilegedApps -> {
        stringResource(BitwardenString.user_trusted_privileged_app_management)
    }

    FlagKey.RemoveCardPolicy -> stringResource(BitwardenString.remove_card_policy)
    FlagKey.BitwardenAuthenticationEnabled -> {
        stringResource(BitwardenString.bitwarden_authentication_enabled)
    }
}
