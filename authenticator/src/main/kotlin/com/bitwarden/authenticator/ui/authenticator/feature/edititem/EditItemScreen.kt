package com.bitwarden.authenticator.ui.authenticator.feature.edititem

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.ui.authenticator.feature.edititem.model.EditItemData
import com.bitwarden.authenticator.ui.platform.components.appbar.AuthenticatorTopAppBar
import com.bitwarden.authenticator.ui.platform.components.button.AuthenticatorTextButton
import com.bitwarden.authenticator.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.authenticator.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.authenticator.ui.platform.components.dialog.BasicDialogState
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.LoadingDialogState
import com.bitwarden.authenticator.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.authenticator.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.authenticator.ui.platform.components.field.BitwardenTextField
import com.bitwarden.authenticator.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.authenticator.ui.platform.components.stepper.BitwardenStepper
import com.bitwarden.authenticator.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import kotlinx.collections.immutable.toImmutableList

/**
 * Displays the edit authenticator item screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    viewModel: EditItemViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = { },
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val resources = context.resources

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            EditItemEvent.NavigateBack -> {
                onNavigateBack()
            }

            is EditItemEvent.ShowToast -> {
                Toast
                    .makeText(
                        context,
                        event.message(resources),
                        Toast.LENGTH_LONG,
                    )
                    .show()
            }
        }
    }

    EditItemDialogs(
        dialogState = state.dialog,
        onDismissRequest = { viewModel.trySendAction(EditItemAction.DismissDialog) },
    )

    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AuthenticatorTopAppBar(
                title = stringResource(
                    id = R.string.edit_item,
                ),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(EditItemAction.CancelClick)
                    }
                },
                actions = {
                    AuthenticatorTextButton(
                        label = stringResource(id = R.string.save),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(EditItemAction.SaveClick) }
                        },
                        modifier = Modifier.semantics { testTag = "SaveButton" },
                    )
                },
            )
        },
        floatingActionButtonPosition = FabPosition.EndOverlay,
    ) { innerPadding ->
        when (val viewState = state.viewState) {
            is EditItemState.ViewState.Content -> {
                EditItemContent(
                    modifier = Modifier
                        .padding(innerPadding),
                    viewState = viewState,
                    onIssuerNameTextChange = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.IssuerNameTextChange(it),
                            )
                        }
                    },
                    onUsernameTextChange = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.UsernameTextChange(it),
                            )
                        }
                    },
                    onToggleFavorite = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.FavoriteToggleClick(it),
                            )
                        }
                    },
                    onTypeOptionClicked = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.TypeOptionClick(it),
                            )
                        }
                    },
                    onTotpCodeTextChange = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.TotpCodeTextChange(it),
                            )
                        }
                    },
                    onAlgorithmOptionClicked = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.AlgorithmOptionClick(it),
                            )
                        }
                    },
                    onRefreshPeriodOptionClicked = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.RefreshPeriodOptionClick(it),
                            )
                        }
                    },
                    onNumberOfDigitsChanged = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.NumberOfDigitsOptionClick(it),
                            )
                        }
                    },
                    onExpandAdvancedOptionsClicked = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                EditItemAction.ExpandAdvancedOptionsClick,
                            )
                        }
                    },
                )
            }

            is EditItemState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = viewState.message(),
                    modifier = Modifier.padding(innerPadding),
                )
            }

            EditItemState.ViewState.Loading -> {
                BitwardenLoadingContent(
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

/**
 * The top level content UI state for the [EditItemScreen].
 */
@Suppress("LongMethod")
@Composable
fun EditItemContent(
    modifier: Modifier = Modifier,
    viewState: EditItemState.ViewState.Content,
    onIssuerNameTextChange: (String) -> Unit = {},
    onUsernameTextChange: (String) -> Unit = {},
    onToggleFavorite: (Boolean) -> Unit = {},
    onTypeOptionClicked: (AuthenticatorItemType) -> Unit = {},
    onTotpCodeTextChange: (String) -> Unit = {},
    onAlgorithmOptionClicked: (AuthenticatorItemAlgorithm) -> Unit = {},
    onRefreshPeriodOptionClicked: (AuthenticatorRefreshPeriodOption) -> Unit = {},
    onNumberOfDigitsChanged: (Int) -> Unit = {},
    onExpandAdvancedOptionsClicked: () -> Unit = {},
) {
    LazyColumn(modifier = modifier) {
        item {
            BitwardenListHeaderText(
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
                label = stringResource(id = R.string.information),
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            BitwardenTextField(
                modifier = Modifier
                    .testTag(tag = "NameTextField")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
                label = stringResource(id = R.string.name),
                value = viewState.itemData.issuer,
                onValueChange = onIssuerNameTextChange,
                singleLine = true,
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenPasswordField(
                modifier = Modifier
                    .testTag(tag = "KeyTextField")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
                label = stringResource(id = R.string.key),
                value = viewState.itemData.totpCode,
                onValueChange = onTotpCodeTextChange,
                singleLine = true,
                capitalization = KeyboardCapitalization.Characters,
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenTextField(
                modifier = Modifier
                    .testTag(tag = "UsernameTextField")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
                label = stringResource(id = R.string.username),
                value = viewState.itemData.username.orEmpty(),
                onValueChange = onUsernameTextChange,
                singleLine = true,
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenSwitch(
                label = stringResource(id = R.string.favorite),
                isChecked = viewState.itemData.favorite,
                onCheckedChange = onToggleFavorite,
                modifier = Modifier
                    .testTag(tag = "ItemFavoriteToggle")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }

        item(key = "AdvancedOptions") {
            val iconRotationDegrees = animateFloatAsState(
                targetValue = if (viewState.isAdvancedOptionsExpanded) 180f else 0f,
                label = "expanderIconRotationAnimation",
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .testTag(tag = "CollapseAdvancedOptions")
                    .standardHorizontalMargin()
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(
                        indication = ripple(
                            bounded = true,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onExpandAdvancedOptionsClicked,
                    )
                    .padding(vertical = 12.dp)
                    .animateItem(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.advanced),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = rememberVectorPainter(id = BitwardenDrawable.ic_chevron_down),
                    contentDescription = if (viewState.isAdvancedOptionsExpanded) {
                        stringResource(R.string.collapse_advanced_options)
                    } else {
                        stringResource(R.string.expand_advanced_options)
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .rotate(degrees = iconRotationDegrees.value),
                )
            }
        }

        if (viewState.isAdvancedOptionsExpanded) {
            advancedOptions(
                viewState = viewState,
                onAlgorithmOptionClicked = onAlgorithmOptionClicked,
                onTypeOptionClicked = onTypeOptionClicked,
                onRefreshPeriodOptionClicked = onRefreshPeriodOptionClicked,
                onNumberOfDigitsChanged = onNumberOfDigitsChanged,
            )
        }

        item {
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Suppress("LongMethod")
private fun LazyListScope.advancedOptions(
    viewState: EditItemState.ViewState.Content,
    onAlgorithmOptionClicked: (AuthenticatorItemAlgorithm) -> Unit,
    onTypeOptionClicked: (AuthenticatorItemType) -> Unit,
    onRefreshPeriodOptionClicked: (AuthenticatorRefreshPeriodOption) -> Unit,
    onNumberOfDigitsChanged: (Int) -> Unit,
) {
    item(key = "OtpItemTypeSelector") {
        val possibleTypeOptions = AuthenticatorItemType.entries
        val typeOptionsWithStrings =
            possibleTypeOptions.associateWith { it.name }
        BitwardenMultiSelectButton(
            modifier = Modifier
                .testTag(tag = "OTPItemTypePicker")
                .standardHorizontalMargin()
                .fillMaxWidth()
                .animateItem(),
            label = stringResource(id = R.string.otp_type),
            options = typeOptionsWithStrings.values.toImmutableList(),
            selectedOption = viewState.itemData.type.name,
            onOptionSelected = { selectedOption ->
                val selectedOptionName = typeOptionsWithStrings
                    .entries
                    .first { it.value == selectedOption }
                    .key
                onTypeOptionClicked(selectedOptionName)
            },
        )
    }

    item(key = "AlgorithmItemTypeSelector") {
        val possibleAlgorithmOptions = AuthenticatorItemAlgorithm.entries
        val algorithmOptionsWithStrings = possibleAlgorithmOptions.associateWith { it.name }
        Spacer(Modifier.height(8.dp))
        BitwardenMultiSelectButton(
            modifier = Modifier
                .testTag(tag = "AlgorithmItemTypePicker")
                .standardHorizontalMargin()
                .fillMaxWidth()
                .animateItem(),
            label = stringResource(id = R.string.algorithm),
            options = algorithmOptionsWithStrings.values.toImmutableList(),
            selectedOption = viewState.itemData.algorithm.name,
            onOptionSelected = { selectedOption ->
                val selectedOptionName = algorithmOptionsWithStrings
                    .entries
                    .first { it.value == selectedOption }
                    .key
                onAlgorithmOptionClicked(selectedOptionName)
            },
        )
    }

    item(key = "RefreshPeriodItemTypePicker") {
        val possibleRefreshPeriodOptions = AuthenticatorRefreshPeriodOption.entries
        val refreshPeriodOptionsWithStrings = possibleRefreshPeriodOptions.associateWith {
            stringResource(id = R.string.refresh_period_seconds, it.seconds)
        }
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenMultiSelectButton(
            modifier = Modifier
                .testTag(tag = "RefreshPeriodItemTypePicker")
                .standardHorizontalMargin()
                .fillMaxWidth()
                .animateItem(),
            label = stringResource(id = R.string.refresh_period),
            options = refreshPeriodOptionsWithStrings.values.toImmutableList(),
            selectedOption = stringResource(
                id = R.string.refresh_period_seconds,
                viewState.itemData.refreshPeriod.seconds,
            ),
            onOptionSelected = remember(viewState) {
                { selectedOption ->
                    val selectedOptionName = refreshPeriodOptionsWithStrings
                        .entries
                        .first { it.value == selectedOption }
                        .key
                    onRefreshPeriodOptionClicked(selectedOptionName)
                }
            },
        )
    }

    item(key = "DigitsCounterItem") {
        Spacer(modifier = Modifier.height(8.dp))
        DigitsCounterItem(
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .animateItem(),
            digits = viewState.itemData.digits,
            onDigitsCounterChange = onNumberOfDigitsChanged,
            minValue = viewState.minDigitsAllowed,
            maxValue = viewState.maxDigitsAllowed,
        )
    }
}

@Composable
private fun EditItemDialogs(
    dialogState: EditItemState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is EditItemState.DialogState.Generic -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = dialogState.title,
                    message = dialogState.message,
                ),
                onDismissRequest = onDismissRequest,
            )
        }

        is EditItemState.DialogState.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(dialogState.message),
            )
        }

        null -> Unit
    }
}

@Composable
private fun DigitsCounterItem(
    digits: Int,
    onDigitsCounterChange: (Int) -> Unit,
    minValue: Int,
    maxValue: Int,
    modifier: Modifier = Modifier,
) {
    BitwardenStepper(
        label = stringResource(id = R.string.number_of_digits),
        value = digits.coerceIn(minValue, maxValue),
        range = minValue..maxValue,
        onValueChange = onDigitsCounterChange,
        increaseButtonTestTag = "DigitsIncreaseButton",
        decreaseButtonTestTag = "DigitsDecreaseButton",
        modifier = modifier.testTag(tag = "DigitsValueLabel"),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EditItemContentExpandedOptionsPreview() {
    EditItemContent(
        viewState = EditItemState.ViewState.Content(
            isAdvancedOptionsExpanded = true,
            itemData = EditItemData(
                refreshPeriod = AuthenticatorRefreshPeriodOption.THIRTY,
                totpCode = "123456",
                type = AuthenticatorItemType.TOTP,
                username = "account name",
                issuer = "issuer",
                algorithm = AuthenticatorItemAlgorithm.SHA1,
                digits = 6,
                favorite = true,
            ),
            minDigitsAllowed = 5,
            maxDigitsAllowed = 10,
        ),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EditItemContentCollapsedOptionsPreview() {
    EditItemContent(
        viewState = EditItemState.ViewState.Content(
            isAdvancedOptionsExpanded = false,
            itemData = EditItemData(
                refreshPeriod = AuthenticatorRefreshPeriodOption.THIRTY,
                totpCode = "123456",
                type = AuthenticatorItemType.TOTP,
                username = "account name",
                issuer = "issuer",
                algorithm = AuthenticatorItemAlgorithm.SHA1,
                digits = 6,
                favorite = false,
            ),
            minDigitsAllowed = 5,
            maxDigitsAllowed = 10,
        ),
    )
}
