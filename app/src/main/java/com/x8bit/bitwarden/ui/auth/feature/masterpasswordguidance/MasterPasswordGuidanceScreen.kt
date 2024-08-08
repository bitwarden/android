package com.x8bit.bitwarden.ui.auth.feature.masterpasswordguidance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

private const val BULLET_TWO_TAB = "\u2022\t\t"
private const val TWO_THIRDS_WEIGHT = 0.66f

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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(size = 4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 24.dp),
                ) {

                    Text(
                        text = stringResource(R.string.what_makes_a_password_strong),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = stringResource(
                            R.string.the_longer_your_password_the_more_difficult_to_hack,
                        ),
                    )
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.the_strongest_passwords_are_usually),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BulletTextRow(text = stringResource(R.string.twelve_or_more_characters))
                    BulletTextRow(
                        text = stringResource(
                            R.string.random_and_complex_using_numbers_and_special_characters,
                        ),
                    )
                    BulletTextRow(
                        text = stringResource(R.string.totally_different_from_your_other_passwords),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TryGeneratorCard(
                onCardClicked = remember(viewModel) {
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
}

@Composable
private fun TryGeneratorCard(
    onCardClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onCardClicked,
        shape = RoundedCornerShape(size = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Icon(
                painter = rememberVectorPainter(id = R.drawable.ic_generator),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(TWO_THIRDS_WEIGHT),
            ) {
                Text(
                    text = stringResource(
                        R.string.use_the_generator_to_create_a_strong_unique_password,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.try_it_out),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                painter = rememberVectorPainter(id = R.drawable.ic_navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(16.dp),
            )
        }
    }
}

@Composable
private fun BulletTextRow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Text(
            text = BULLET_TWO_TAB,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
