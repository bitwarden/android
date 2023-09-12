package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Top level composable for the generator screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                title = {
                    Text(
                        text = stringResource(id = R.string.generator),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                },
                navigationIcon = {
                    Spacer(Modifier.width(40.dp))
                },
                actions = {
                    OverflowMenu()
                },
            )
        },
    ) { innerPadding ->
        ScrollContent(modifier = Modifier.padding(innerPadding))
    }
}

@Composable
private fun OverflowMenu() {
    IconButton(
        onClick = {},
    ) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(id = R.string.options),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun ScrollContent(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item { DynamicStringItem() }
        item { TextItem(title = stringResource(id = R.string.what_would_you_like_to_generate)) }
        item { TextItem(title = stringResource(id = R.string.password_type), showOptions = true) }
        item { LengthSliderItem() }
        item { ToggleItem(stringResource(id = R.string.uppercase_ato_z)) }
        item { ToggleItem(stringResource(id = R.string.lowercase_ato_z)) }
        item { ToggleItem(stringResource(id = R.string.numbers_zero_to_nine)) }
        item { ToggleItem(stringResource(id = R.string.special_characters)) }
        item { CounterItem(label = stringResource(id = R.string.min_numbers)) }
        item { CounterItem(label = stringResource(id = R.string.min_special)) }
        item { ToggleItem(stringResource(id = R.string.avoid_ambiguous_characters)) }
    }
}

@Composable
private fun DynamicStringItem() {
    // TODO(BIT-276): Move this state to ViewModel
    val placeholderPassword = "PLACEHOLDER"
    val dynamicString = remember { mutableStateOf(placeholderPassword) }

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = dynamicString.value)

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {},
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.copy),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = {},
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(id = R.string.generate_password),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun TextItem(title: String, showOptions: Boolean = false) {
    // TODO(BIT-276): Move this state to ViewModel
    val defaultType = stringResource(id = R.string.password)
    val content = remember { mutableStateOf(defaultType) }

    CommonPadding {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (showOptions) {
                Text(
                    stringResource(id = R.string.options),
                    style = TextStyle(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(title, style = TextStyle(fontSize = 10.sp))
            Text(content.value)
        }
    }
}

@Composable
private fun LengthSliderItem() {
    // TODO(BIT-276): Move this state to ViewModel
    val sliderPosition = remember { mutableStateOf(0f) }
    CommonPadding {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(id = R.string.length))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(sliderPosition.value.toInt().toString())
                Slider(
                    value = sliderPosition.value,
                    onValueChange = {},
                )
            }
        }
    }
}

@Composable
private fun ToggleItem(title: String) {
    // TODO(BIT-276): Move this state to ViewModel
    val isToggled = remember { mutableStateOf(false) }
    CommonPadding {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title)
            Switch(checked = isToggled.value, onCheckedChange = { isToggled.value = it })
        }
    }
}

@Composable
private fun CounterItem(label: String) {
    // TODO(BIT-276): Move this state to ViewModel
    val counter = remember { mutableStateOf(1) }

    CommonPadding {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(label)
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(counter.value.toString())
                IconButton(
                    onClick = {},
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = {},
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommonPadding(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        content()
        Divider()
    }
}

@Preview(showBackground = true)
@Composable
private fun GeneratorPreview() {
    BitwardenTheme {
        GeneratorScreen()
    }
}
