package com.bitwarden.authenticator.ui.authenticator.feature.edititem

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.ui.authenticator.feature.edititem.model.EditItemData
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.header.BitwardenExpandingHeader
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.stepper.BitwardenStepper
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenPlurals
import com.bitwarden.ui.platform.resource.BitwardenString
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
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            EditItemEvent.NavigateBack -> onNavigateBack()
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
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.edit),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = BitwardenString.close),
                onNavigationIconClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(EditItemAction.CancelClick)
                    }
                },
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = BitwardenString.save),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(EditItemAction.SaveClick) }
                        },
                        modifier = Modifier.semantics { testTag = "SaveButton" },
                    )
                },
            )
        },
        floatingActionButtonPosition = FabPosition.EndOverlay,
    ) {
        when (val viewState = state.viewState) {
            is EditItemState.ViewState.Content -> {
                EditItemContent(
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
                BitwardenErrorContent(message = viewState.message())
            }

            EditItemState.ViewState.Loading -> {
                BitwardenLoadingContent()
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
            Spacer(modifier = Modifier.height(height = 12.dp))
            BitwardenListHeaderText(
                modifier = Modifier
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                label = stringResource(id = BitwardenString.information),
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            BitwardenTextField(
                modifier = Modifier
                    .testTag(tag = "NameTextField")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
                label = stringResource(id = BitwardenString.name),
                value = viewState.itemData.issuer,
                onValueChange = onIssuerNameTextChange,
                singleLine = true,
                cardStyle = CardStyle.Top(),
            )
        }

        item {
            BitwardenPasswordField(
                modifier = Modifier
                    .testTag(tag = "KeyTextField")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
                label = stringResource(id = BitwardenString.key),
                value = viewState.itemData.totpCode,
                onValueChange = onTotpCodeTextChange,
                singleLine = true,
                cardStyle = CardStyle.Middle(),
            )
        }

        item {
            BitwardenTextField(
                modifier = Modifier
                    .testTag(tag = "UsernameTextField")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
                label = stringResource(id = BitwardenString.username),
                value = viewState.itemData.username.orEmpty(),
                onValueChange = onUsernameTextChange,
                singleLine = true,
                cardStyle = CardStyle.Middle(),
            )
        }

        item {
            BitwardenSwitch(
                label = stringResource(id = BitwardenString.favorite),
                isChecked = viewState.itemData.favorite,
                onCheckedChange = onToggleFavorite,
                cardStyle = CardStyle.Bottom,
                modifier = Modifier
                    .testTag(tag = "ItemFavoriteToggle")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }

        item(key = "AdvancedOptions") {
            BitwardenExpandingHeader(
                isExpanded = viewState.isAdvancedOptionsExpanded,
                onClick = onExpandAdvancedOptionsClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
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
        val typeOptionsWithStrings = possibleTypeOptions.associateWith { it.name }
        BitwardenMultiSelectButton(
            modifier = Modifier
                .testTag(tag = "OTPItemTypePicker")
                .standardHorizontalMargin()
                .fillMaxWidth()
                .animateItem(),
            label = stringResource(id = BitwardenString.otp_type),
            options = typeOptionsWithStrings.values.toImmutableList(),
            selectedOption = viewState.itemData.type.name,
            cardStyle = CardStyle.Top(),
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
        BitwardenMultiSelectButton(
            modifier = Modifier
                .testTag(tag = "AlgorithmItemTypePicker")
                .standardHorizontalMargin()
                .fillMaxWidth()
                .animateItem(),
            label = stringResource(id = BitwardenString.algorithm),
            options = algorithmOptionsWithStrings.values.toImmutableList(),
            selectedOption = viewState.itemData.algorithm.name,
            cardStyle = CardStyle.Middle(),
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
            pluralStringResource(
                id = BitwardenPlurals.refresh_period_seconds,
                count = it.seconds,
                formatArgs = arrayOf(it.seconds),
            )
        }
        BitwardenMultiSelectButton(
            modifier = Modifier
                .testTag(tag = "RefreshPeriodItemTypePicker")
                .standardHorizontalMargin()
                .fillMaxWidth()
                .animateItem(),
            label = stringResource(id = BitwardenString.refresh_period),
            options = refreshPeriodOptionsWithStrings.values.toImmutableList(),
            selectedOption = refreshPeriodOptionsWithStrings.getValue(
                key = viewState.itemData.refreshPeriod,
            ),
            cardStyle = CardStyle.Middle(),
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
                title = dialogState.title(),
                message = dialogState.message(),
                onDismissRequest = onDismissRequest,
            )
        }

        is EditItemState.DialogState.Loading -> {
            BitwardenLoadingDialog(
                text = dialogState.message(),
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
        label = stringResource(id = BitwardenString.number_of_digits),
        value = digits.coerceIn(minValue, maxValue),
        range = minValue..maxValue,
        onValueChange = onDigitsCounterChange,
        cardStyle = CardStyle.Bottom,
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
