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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.annotatedStringResource
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toAnnotatedString
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.bitwarden.ui.platform.components.card.BitwardenContentCard
import com.bitwarden.ui.platform.components.content.model.ContentBlockData
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.persistentListOf

/**
 * The top level composable for the Master Password Guidance screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                title = stringResource(id = BitwardenString.master_password),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = BitwardenString.close),
                onNavigationIconClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(MasterPasswordGuidanceAction.CloseAction)
                    }
                },
            )
        },
    ) {
        MasterPasswordGuidanceContent(
            onTryPasswordGeneratorAction = remember(viewModel) {
                {
                    viewModel.trySendAction(
                        MasterPasswordGuidanceAction.TryPasswordGeneratorAction,
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .standardHorizontalMargin(),
        )
    }
}

@Composable
private fun MasterPasswordGuidanceContent(
    onTryPasswordGeneratorAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(BitwardenString.a_secure_memorable_password),
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
                BitwardenString.one_of_the_best_ways_to_create_a_secure_and_memorable_password,
            ),
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        MasterPasswordGuidanceContentBlocks()
        NeedSomeInspirationCard(
            onActionClicked = {
                onTryPasswordGeneratorAction()
            },
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Suppress("MaxLineLength")
@Composable
private fun MasterPasswordGuidanceContentBlocks(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        BitwardenContentCard(
            contentItems = persistentListOf(
                ContentBlockData(
                    headerText = stringResource(BitwardenString.choose_three_or_four_random_words)
                        .toAnnotatedString(),
                    subtitleText = annotatedStringResource(
                        id = BitwardenString.pick_three_or_four_random_unrelated_words,
                    ),
                    iconVectorResource = BitwardenDrawable.ic_number1,
                ),
                ContentBlockData(
                    headerText = stringResource(BitwardenString.combine_those_words_together)
                        .toAnnotatedString(),
                    subtitleText = annotatedStringResource(
                        id = BitwardenString.put_the_words_together_in_any_order_to_form_your_passphrase,
                    ),
                    iconVectorResource = BitwardenDrawable.ic_number2,
                ),
                ContentBlockData(
                    headerText = stringResource(BitwardenString.make_it_yours).toAnnotatedString(),
                    subtitleText = annotatedStringResource(
                        id = BitwardenString.add_a_number_or_symbol_to_make_it_even_stronger,
                    ),
                    iconVectorResource = BitwardenDrawable.ic_number3,
                ),
            ),
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun NeedSomeInspirationCard(
    onActionClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenActionCard(
        cardTitle = stringResource(BitwardenString.need_some_inspiration),
        actionText = stringResource(BitwardenString.check_out_the_passphrase_generator),
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
