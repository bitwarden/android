package com.bitwarden.ui.platform.components.coachmark.model

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * A [CoachMarkState] that depends on a [LazyListState] to automatically scroll to the current
 * Coach Mark if not on currently on the screen.
 */
@Stable
class LazyListCoachMarkState<T : Enum<T>>(
    private val lazyListState: LazyListState,
    orderedList: List<T>,
    initialCoachMarkHighlight: T? = null,
    isCoachMarkVisible: Boolean = false,
) : CoachMarkState<T>(orderedList, initialCoachMarkHighlight, isCoachMarkVisible) {

    override suspend fun showCoachMark(coachMarkToShow: T) {
        lazyListState.searchForKey(coachMarkToShow)
        super.showCoachMark(coachMarkToShow)
    }

    private suspend fun LazyListState.searchForKey(keyToFind: T) {
        val keyFound = layoutInfo
            .visibleItemsInfo
            .any { it.key == keyToFind }
            .takeIf { itemAlreadyVisible ->
                if (itemAlreadyVisible) {
                    val offset =
                        layoutInfo
                            .visibleItemsInfo
                            .find { visItem ->
                                visItem.key == keyToFind
                            }
                            ?.offset
                    when {
                        offset == null -> Unit
                        ((layoutInfo.viewportEndOffset - offset) <
                            END_VIEW_PORT_PIXEL_THRESHOLD) -> {
                            scrollBy(layoutInfo.quarterViewPortScrollAmount())
                        }

                        ((offset - layoutInfo.viewportStartOffset) <
                            START_VIEW_PORT_PIXEL_THRESHOLD) -> {
                            scrollBy(-(layoutInfo.quarterViewPortScrollAmount()))
                        }

                        else -> Unit
                    }
                }
                itemAlreadyVisible
            }
            ?: scrollUpToKey(keyToFind).takeIf { it }
            ?: scrollDownToKey(keyToFind)
        if (!keyFound) {
            // if key not found scroll back to the top.
            scrollToItem(index = 0)
        }
    }

    private suspend fun LazyListState.scrollUpToKey(
        targetKey: T,
    ): Boolean {
        val scrollAmount = (-1).toFloat()
        var found = false
        var keepSearching = true
        while (keepSearching && !found) {
            val layoutInfo = this.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.any { it.key == targetKey }) {
                scrollBy(-(layoutInfo.halfViewPortScrollAmount()))
                found = true
            } else if (!canScrollBackward) {
                keepSearching = false
            } else {
                this.scrollBy(scrollAmount)
            }
        }
        return found
    }

    private suspend fun LazyListState.scrollDownToKey(
        targetKey: T,
    ): Boolean {
        val scrollAmount = 1.toFloat()
        var found = false
        var keepSearching = true
        while (keepSearching && !found) {
            val layoutInfo = this.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.any { it.key == targetKey }) {
                scrollBy(layoutInfo.halfViewPortScrollAmount())
                found = true
            } else if (!this.canScrollForward) {
                // Reached the end of the list without finding the key
                keepSearching = false
            } else {
                this.scrollBy(scrollAmount)
            }
        }
        return found
    }

    private fun LazyListLayoutInfo.halfViewPortScrollAmount(): Float = when (this.orientation) {
        Orientation.Vertical -> (viewportSize.height / 2f)
        Orientation.Horizontal -> (viewportSize.width / 2f)
    }

    @Suppress("MagicNumber")
    private fun LazyListLayoutInfo.quarterViewPortScrollAmount(): Float = when (this.orientation) {
        Orientation.Vertical -> (viewportSize.height / 4f)
        Orientation.Horizontal -> (viewportSize.width / 4f)
    }

    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates a [Saver] for [CoachMarkState] to enable saving and restoring its state.
         *
         * @return A [Saver] that can save and restore [CoachMarkState].
         */
        inline fun <reified T : Enum<T>> saver(
            lazyListState: LazyListState,
        ): Saver<CoachMarkState<T>, Any> =
            listSaver(
                save = { coachMarkState ->
                    listOf(
                        coachMarkState.orderedList.map { it.name },
                        coachMarkState.currentHighlight.value?.name,
                        coachMarkState.isVisible.value,
                    )
                },
                restore = { restoredList ->
                    val enumList = restoredList[0] as List<*>
                    val currentHighlightName = restoredList[1] as String?
                    val enumValues = enumValues<T>()
                    val list = enumList.mapNotNull { name ->
                        enumValues.find { it.name == name }
                    }
                    val currentHighlight = currentHighlightName?.let { name ->
                        enumValues.find { it.name == name }
                    }
                    val isVisible = restoredList[2] as Boolean
                    LazyListCoachMarkState(
                        lazyListState = lazyListState,
                        orderedList = list,
                        initialCoachMarkHighlight = currentHighlight,
                        isCoachMarkVisible = isVisible,
                    )
                },
            )
    }
}

/**
 * Remembers and saves the state of a [LazyListCoachMarkState].
 *
 * @param T The type of the enum used to represent the coach mark keys.
 * @param orderedList The ordered list of coach mark keys.
 * @param lazyListState The lazy list state to be used by the created instance.
 * @return A [LazyListCoachMarkState] instance.
 */
@Composable
inline fun <reified T : Enum<T>> rememberLazyListCoachMarkState(
    orderedList: List<T>,
    lazyListState: LazyListState,
): CoachMarkState<T> {
    return rememberSaveable(saver = LazyListCoachMarkState.saver<T>(lazyListState)) {
        LazyListCoachMarkState(lazyListState = lazyListState, orderedList = orderedList)
    }
}

private const val END_VIEW_PORT_PIXEL_THRESHOLD = 150
private const val START_VIEW_PORT_PIXEL_THRESHOLD = 40
