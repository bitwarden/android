package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenMultiSelectButton
import kotlinx.collections.immutable.toImmutableList

/**
 * Displays UX for choosing deletion date of a send.
 *
 * TODO: Implement custom date choosing and send choices to the VM: BIT-1090.
 */
@Composable
fun SendDeletionDateChooser(
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        stringResource(id = R.string.one_hour),
        stringResource(id = R.string.one_day),
        stringResource(id = R.string.two_days),
        stringResource(id = R.string.three_days),
        stringResource(id = R.string.seven_days),
        stringResource(id = R.string.thirty_days),
        stringResource(id = R.string.custom),
    )
    val defaultOption = stringResource(id = R.string.seven_days)
    var selectedOption: String by rememberSaveable { mutableStateOf(defaultOption) }
    Column(
        modifier = modifier,
    ) {
        BitwardenMultiSelectButton(
            label = stringResource(id = R.string.deletion_date),
            options = options.toImmutableList(),
            selectedOption = selectedOption,
            onOptionSelected = { selectedOption = it },
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.deletion_date_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}
