package com.x8bit.bitwarden.authenticator.ui.platform.components.appbar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.mirrorIfRtl

/**
 * Represents a Bitwarden styled [TopAppBar] that assumes the following components:
 *
 * - an optional single navigation control in the upper-left defined by [navigationIcon].
 * - an editable [TextField] populated by a [searchTerm] in the middle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitwardenSearchTopAppBar(
    searchTerm: String,
    placeholder: String,
    onSearchTermChange: (String) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    navigationIcon: NavigationIcon?,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }
    TopAppBar(
        modifier = modifier.semantics { testTag = "HeaderBarComponent" },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            navigationIcon?.let {
                IconButton(
                    onClick = it.onNavigationIconClick,
                    modifier = Modifier.semantics { testTag = "CloseButton" },
                ) {
                    Icon(
                        modifier = Modifier.mirrorIfRtl(),
                        painter = it.navigationIcon,
                        contentDescription = it.navigationIconContentDescription,
                    )
                }
            }
        },
        title = {
            TextField(
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                placeholder = { Text(text = placeholder) },
                value = searchTerm,
                onValueChange = onSearchTermChange,
                trailingIcon = {
                    IconButton(
                        onClick = { onSearchTermChange("") },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = stringResource(id = R.string.clear),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth(),
            )
        },
    )
    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}
