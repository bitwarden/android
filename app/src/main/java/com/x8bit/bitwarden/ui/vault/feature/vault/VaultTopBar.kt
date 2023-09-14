package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R

/**
 * The top bar for the [VaultScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    accountIconClickAction: () -> Unit,
    searchIconClickAction: () -> Unit,
) {
    // TODO Create overflow menu and syncing BIT-217
    var overFlowMenuVisible by rememberSaveable {
        mutableStateOf(false)
    }
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.my_vault),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        actions = {
            IconButton(
                onClick = accountIconClickAction,
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = stringResource(id = R.string.account),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            IconButton(
                onClick = searchIconClickAction,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(id = R.string.search_vault),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Box {
                IconButton(
                    onClick = { overFlowMenuVisible = !overFlowMenuVisible },
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(id = R.string.more),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                // TODO Create overflow menu and syncing BIT-217
                DropdownMenu(
                    expanded = overFlowMenuVisible,
                    onDismissRequest = { overFlowMenuVisible = false },
                ) {
                    Text(text = "PLACEHOLDER")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}
