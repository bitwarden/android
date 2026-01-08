package com.bitwarden.ui.platform.components.debug

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.resource.BitwardenString

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
    is FlagKey.DummyInt,
    FlagKey.DummyString,
        -> Unit

    FlagKey.DummyBoolean,
    FlagKey.BitwardenAuthenticationEnabled,
    FlagKey.CredentialExchangeProtocolImport,
    FlagKey.CredentialExchangeProtocolExport,
    FlagKey.CipherKeyEncryption,
    FlagKey.ForceUpdateKdfSettings,
    FlagKey.NoLogoutOnKdfChange,
    FlagKey.MigrateMyVaultToMyItems,
    FlagKey.ArchiveItems,
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

    FlagKey.CredentialExchangeProtocolImport -> stringResource(BitwardenString.cxp_import)
    FlagKey.CredentialExchangeProtocolExport -> stringResource(BitwardenString.cxp_export)
    FlagKey.CipherKeyEncryption -> stringResource(BitwardenString.cipher_key_encryption)
    FlagKey.ForceUpdateKdfSettings -> stringResource(BitwardenString.force_update_kdf_settings)
    FlagKey.NoLogoutOnKdfChange -> stringResource(BitwardenString.avoid_logout_on_kdf_change)
    FlagKey.BitwardenAuthenticationEnabled -> {
        stringResource(BitwardenString.bitwarden_authentication_enabled)
    }

    FlagKey.MigrateMyVaultToMyItems -> stringResource(BitwardenString.migrate_my_vault_to_my_items)
    FlagKey.ArchiveItems -> stringResource(BitwardenString.archive_items)
}
