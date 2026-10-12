package com.bitwarden.ui.platform.base.util

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager

/**
 * Subscribes to this [InteractionSource] and returns a [State] representing whether this
 * component is focused or not.
 *
 * This is based on the [collectIsFocusedAsState] function but only indicates focus if the keyboard
 * input mode used when focus was gained.
 */
@Composable
fun InteractionSource.collectIsFocusedByKeyboardAsState(
    inputModeManager: InputModeManager = LocalInputModeManager.current,
): State<Boolean> {
    val isFocused = remember { mutableStateOf(value = false) }
    LaunchedEffect(key1 = this) {
        val focusInteractions = mutableListOf<FocusInteraction.Focus>()
        interactions.collect { interaction ->
            when (interaction) {
                is FocusInteraction.Focus -> {
                    if (inputModeManager.inputMode == InputMode.Keyboard) {
                        focusInteractions.add(element = interaction)
                    }
                }

                is FocusInteraction.Unfocus -> {
                    focusInteractions.remove(element = interaction.focus)
                }
            }
            isFocused.value = focusInteractions.isNotEmpty()
        }
    }
    return isFocused
}
