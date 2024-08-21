package com.x8bit.bitwarden.ui.platform.feature.debugmenu.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch

/**
 * Creates a list item for a [FlagKey].
 */
@Suppress("UNCHECKED_CAST")
@Composable
fun <T : Any> FlagKey<T>.ListItemContent(
    currentValue: T,
    onValueChange: (key: FlagKey<T>, value: T) -> Unit,
    modifier: Modifier = Modifier,
) = when (val flagKey = this) {
    FlagKey.DummyBoolean,
    is FlagKey.DummyInt,
    FlagKey.DummyString,
    -> Unit

    FlagKey.EmailVerification,
    FlagKey.OnboardingCarousel,
    FlagKey.OnboardingFlow,
    -> BooleanFlagItem(
        label = flagKey.getDisplayLabel(),
        key = flagKey as FlagKey<Boolean>,
        currentValue = currentValue as Boolean,
        onValueChange = onValueChange as (FlagKey<Boolean>, Boolean) -> Unit,
        modifier = modifier,
    )
}

/**
 * The UI layout for a boolean backed flag key.
 */
@Composable
private fun BooleanFlagItem(
    label: String,
    key: FlagKey<Boolean>,
    currentValue: Boolean,
    onValueChange: (key: FlagKey<Boolean>, value: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                toggleableState = ToggleableState(currentValue)
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = MaterialTheme.colorScheme.primary),
                onClick = { onValueChange(key, !currentValue) },
            )
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label)
        Spacer(modifier = Modifier.size(8.dp))
        BitwardenSwitch(
            label = "",
            isChecked = currentValue,
            onCheckedChange = null,
        )
    }
}

@Composable
private fun <T : Any> FlagKey<T>.getDisplayLabel(): String = when (this) {
    FlagKey.DummyBoolean,
    is FlagKey.DummyInt,
    FlagKey.DummyString,
    -> this.keyName

    FlagKey.EmailVerification -> stringResource(R.string.email_verification)
    FlagKey.OnboardingCarousel -> stringResource(R.string.onboarding_carousel)
    FlagKey.OnboardingFlow -> stringResource(R.string.onboarding_flow)
}
