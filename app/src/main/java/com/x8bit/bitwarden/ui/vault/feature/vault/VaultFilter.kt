package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.scrolledContainerBackground
import com.x8bit.bitwarden.ui.platform.base.util.scrolledContainerBottomDivider
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenSelectionRow
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import kotlinx.collections.immutable.ImmutableList

/**
 * Displays the current [selectedVaultFilterType] and allows for a new selection from the
 * given [vaultFilterTypes].
 *
 * @param selectedVaultFilterType The currently selected filter type.
 * @param vaultFilterTypes The list of possible filter types.
 * @param onVaultFilterTypeSelect A callback for when a new type is selected.
 * @param topAppBarScrollBehavior Used to derive the background color of the content and keep it in
 * sync with the associated app bar.
 * @param modifier A [Modifier] for the composable.
 * @param windowInsets The insets to be applied to this composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultFilter(
    selectedVaultFilterType: VaultFilterType,
    vaultFilterTypes: ImmutableList<VaultFilterType>,
    onVaultFilterTypeSelect: (VaultFilterType) -> Unit,
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.displayCutout
        .union(WindowInsets.navigationBars)
        .only(WindowInsetsSides.Horizontal),
) {
    var shouldShowSelectionDialog by remember { mutableStateOf(false) }

    if (shouldShowSelectionDialog) {
        BitwardenSelectionDialog(
            title = stringResource(id = R.string.filter_by_vault),
            onDismissRequest = { shouldShowSelectionDialog = false },
        ) {
            vaultFilterTypes.forEach { filterType ->
                BitwardenSelectionRow(
                    text = filterType.description,
                    isSelected = filterType == selectedVaultFilterType,
                    onClick = {
                        shouldShowSelectionDialog = false
                        onVaultFilterTypeSelect(filterType)
                    },
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .scrolledContainerBackground(topAppBarScrollBehavior = topAppBarScrollBehavior)
            .scrolledContainerBottomDivider(topAppBarScrollBehavior = topAppBarScrollBehavior)
            .padding(vertical = 8.dp)
            .testTag("ActiveFilterRow")
            .then(modifier)
            .windowInsetsPadding(insets = windowInsets),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                id = R.string.vault_filter_description,
                selectedVaultFilterType.name(),
            ),
            style = BitwardenTheme.typography.bodyLarge,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .testTag("ActiveFilterLabel")
                .weight(1f),
        )

        Spacer(modifier = Modifier.width(16.dp))

        BitwardenStandardIconButton(
            vectorIconRes = R.drawable.ic_ellipsis_horizontal,
            contentDescription = stringResource(id = R.string.filter_by_vault),
            onClick = { shouldShowSelectionDialog = true },
            modifier = Modifier.testTag(tag = "OpenOrgFilter"),
        )
    }
}
