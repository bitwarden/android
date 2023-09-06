package com.x8bit.bitwarden.ui.auth.feature.createaccount

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField

/**
 * Top level composable for the create account screen.
 */
@Composable
fun CreateAccountScreen(
    viewModel: CreateAccountViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    EventsEffect(viewModel) { event ->
        when (event) {
            is CreateAccountEvent.ShowToast -> {
                Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                text = stringResource(id = R.string.title_create_account),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                modifier = Modifier
                    .clickable {
                        viewModel.trySendAction(CreateAccountAction.SubmitClick)
                    }
                    .padding(16.dp),
                text = stringResource(id = R.string.button_submit),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        BitwardenTextField(label = stringResource(id = R.string.input_label_email))
        BitwardenTextField(label = stringResource(id = R.string.input_label_master_password))
        BitwardenTextField(label = stringResource(id = R.string.input_label_re_type_master_password))
        BitwardenTextField(label = stringResource(id = R.string.input_label_master_password_hint))
    }
}
