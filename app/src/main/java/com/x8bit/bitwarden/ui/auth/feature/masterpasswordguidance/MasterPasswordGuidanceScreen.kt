package com.x8bit.bitwarden.ui.auth.feature.masterpasswordguidance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.bitwardenBoldSpanStyle
import com.x8bit.bitwarden.ui.platform.base.util.createAnnotatedString
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.base.util.toAnnotatedString
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenContentCard
import com.x8bit.bitwarden.ui.platform.components.model.ContentBlockData
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.persistentListOf

private const val BULLET_TWO_TAB = "\u2022\t\t"

/**
 * The top level composable for the Master Password Guidance screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun MasterPasswordGuidanceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGeneratePassword: () -> Unit,
    viewModel: MasterPasswordGuidanceViewModel = hiltViewModel(),
) {
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            MasterPasswordGuidanceEvent.NavigateBack -> onNavigateBack()
            MasterPasswordGuidanceEvent.NavigateToPasswordGenerator -> {
                onNavigateToGeneratePassword()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.master_password),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(MasterPasswordGuidanceAction.CloseAction)
                    }
                },
            )
        },
    ) {
        MasterPasswordGuidanceContent(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .standardHorizontalMargin(),
            viewModel = viewModel,
        )
    }
}

@Composable
private fun MasterPasswordGuidanceContent(
    modifier: Modifier = Modifier,
    viewModel: MasterPasswordGuidanceViewModel,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.a_secure_memorable_password),
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.one_of_the_best_ways_to_create_a_secure_and_memorable_password_is_to_use_a_passphrase,
            ),
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))

        BitwardenContentCard(
            contentItems = persistentListOf(
                ContentBlockData(
                    headerText = stringResource(R.string.choose_three_or_four_random_words)
                        .toAnnotatedString(),
                    subtitleText = createAnnotatedString(
                        mainString = stringResource(
                            R.string.pick_three_or_four_random_unrelated_words_that_you_can_easily_remember,
                        ),
                        highlights = listOf(
                            stringResource(R.string.pick_three_or_four_random_unrelated_words_highlight),
                        ),
                        highlightStyle = bitwardenBoldSpanStyle,
                    ),
                    iconVectorResource = R.drawable.ic_number1,
                ),
                ContentBlockData(
                    headerText = stringResource(R.string.combine_those_words_together)
                        .toAnnotatedString(),
                    subtitleText = createAnnotatedString(
                        mainString = stringResource(
                            R.string.put_the_words_together_in_any_order_to_form_your_passphrase_use_hyphens_spaces_or_leave_them_as_one_long_word_your_choice,
                        ),
                        highlights = listOf(stringResource(R.string.use_hyphens_spaces_or_leave_them_as_one_long_word_choice_highlight)),
                        highlightStyle = bitwardenBoldSpanStyle,
                    ),
                    iconVectorResource = R.drawable.ic_number2,
                ),
                ContentBlockData(
                    headerText = stringResource(R.string.make_it_yours)
                        .toAnnotatedString(),
                    subtitleText = createAnnotatedString(
                        mainString = stringResource(R.string.add_a_number_or_symbol_to_make_it_even_stronger_now_you_have_a_unique_secure_and_memorable_passphrase),
                        highlights = listOf(stringResource(R.string.add_a_number_or_symbol_highlight)),
                        highlightStyle = bitwardenBoldSpanStyle,
                    ),
                    iconVectorResource = R.drawable.ic_number3,
                ),
            ),
        )
        Spacer(modifier = Modifier.height(24.dp))
        NeedSomeInspirationCard(
            onActionClicked = remember(viewModel) {
                {
                    viewModel.trySendAction(
                        MasterPasswordGuidanceAction.TryPasswordGeneratorAction,
                    )
                }
            },
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun NeedSomeInspirationCard(
    onActionClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenActionCard(
        cardTitle = "Need some inspiration?",
        actionText = "Check out the passphrase generator",
        onActionClick = onActionClicked,
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview
@Composable
private fun MasterPasswordGuidanceScreenPreview() {
    BitwardenTheme {
        MasterPasswordGuidanceScreen(
            onNavigateBack = {},
            onNavigateToGeneratePassword = {},
        )
    }
}
