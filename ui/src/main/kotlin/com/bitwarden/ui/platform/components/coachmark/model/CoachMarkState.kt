package com.bitwarden.ui.platform.components.coachmark.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Rect
import com.bitwarden.core.data.util.concurrentMapOf
import com.bitwarden.ui.platform.components.tooltip.model.BitwardenToolTipState
import kotlin.math.max
import kotlin.math.min

/**
 * Manages the state of a coach mark sequence, guiding users through a series of highlights.
 *
 * This class handles the ordered list of highlights, the currently active highlight,
 * and the overall visibility of the coach mark overlay.
 *
 * @param T The type of the enum used to represent the coach mark keys.
 * @property orderedList The ordered list of coach mark keys that define the sequence.
 * @param initialCoachMarkHighlight The initial coach mark to be highlighted, or null if
 * none should be highlighted at start.
 * @param isCoachMarkVisible is any coach mark currently visible.
 */
@Stable
open class CoachMarkState<T : Enum<T>>(
    val orderedList: List<T>,
    initialCoachMarkHighlight: T? = null,
    isCoachMarkVisible: Boolean = false,
) {
    private val highlights: MutableMap<T, CoachMarkHighlightState<T>?> = concurrentMapOf()
    private val mutableCurrentHighlight = mutableStateOf(initialCoachMarkHighlight)
    val currentHighlight: State<T?> = mutableCurrentHighlight
    private val mutableCurrentHighlightBounds = mutableStateOf(Rect.Zero)
    val currentHighlightBounds: State<Rect> = mutableCurrentHighlightBounds
    private val mutableCurrentHighlightShape = mutableStateOf<CoachMarkHighlightShape>(
        CoachMarkHighlightShape.RoundedRectangle(),
    )
    val currentHighlightShape: State<CoachMarkHighlightShape> = mutableCurrentHighlightShape

    private val mutableIsVisible = mutableStateOf(isCoachMarkVisible)
    val isVisible: State<Boolean> = mutableIsVisible

    /**
     * Updates the highlight information for a given key. If the key matches the current shown
     * [key] then also update the public state for the highlight bounds and shape.
     *
     * @param key The key of the highlight to update.
     * @param bounds The rectangular bounds of the area to highlight. If null, defaults to
     * Rect.Zero.
     * @param toolTipState The state of the tooltip associated with this highlight.
     * @param shape The shape of the highlight (e.g., square, oval). Defaults to
     * [CoachMarkHighlightShape.RoundedRectangle].
     */
    fun updateHighlight(
        key: T,
        bounds: Rect?,
        toolTipState: BitwardenToolTipState,
        shape: CoachMarkHighlightShape = CoachMarkHighlightShape.RoundedRectangle(),
    ) {
        highlights[key] = CoachMarkHighlightState(
            key = key,
            highlightBounds = bounds,
            toolTipState = toolTipState,
            shape = shape,
        )
            .also {
                if (key == currentHighlight.value) {
                    updateCoachMarkStateInternal(it)
                }
            }
    }

    /**
     * For the provided [key] add a new rectangle to any existing bounds unless it is
     * the first item then it is used as the "starting" rectangle.
     *
     * @param key the [CoachMarkHighlightState] to modify.
     * @param isFirstItem if this new calculation is coming from the "first" or base item.
     * @param additionalBounds the rectangle to add to the existing bounds.
     */
    fun addToExistingBounds(key: T, isFirstItem: Boolean, additionalBounds: Rect) {
        highlights[key]?.let {
            val newRect = it.highlightBounds
                ?.union(additionalBounds)
                .takeIf { !isFirstItem }
                ?: additionalBounds
            highlights[key] = it.copy(highlightBounds = newRect)
            if (key == currentHighlight.value) {
                updateCoachMarkStateInternal(getCurrentHighlight())
            }
        }
    }

    /**
     * Show the the tooltip for the currently shown tooltip.
     */
    suspend fun showToolTipForCurrentCoachMark() {
        val currentCoachMark = getCurrentHighlight()
        currentCoachMark?.toolTipState?.show()
    }

    /**
     * Indicates that the coach mark associated with the provided key should be shown and
     * starts that process of updating the state.
     *
     * @param coachMarkToShow The key of the coach mark to show.
     */
    open suspend fun showCoachMark(coachMarkToShow: T) {
        // Clean up the previous tooltip if one is showing.
        if (currentHighlight.value != coachMarkToShow && isVisible.value) {
            getCurrentHighlight()?.toolTipState?.cleanUp()
        }
        mutableCurrentHighlight.value = coachMarkToShow
        val highlightToShow = getCurrentHighlight()
        updateCoachMarkStateInternal(highlightToShow)
    }

    /**
     * Shows the next highlight in the sequence.
     * If there is no previous highlight, it will show the first highlight.
     * If the previous highlight is the last in the list, nothing will happen.
     */
    suspend fun showNextCoachMark() {
        val previousHighlight = getCurrentHighlight()
        previousHighlight?.toolTipState?.cleanUp()
        val index = orderedList.indexOf(previousHighlight?.key)
        // We return early here if the the previous highlight does exist but is somehow not
        // present in the list. If the previous highlight is null we resolve that the next
        // coach mark to show is the first item in the orderedList.
        if (index < 0 && previousHighlight != null) return
        mutableCurrentHighlight.value = orderedList.getOrNull(index + 1)
        mutableCurrentHighlight.value?.let {
            showCoachMark(it)
        }
    }

    /**
     * Shows the previous coach mark in the sequence.
     * If the current highlighted coach mark is the first in the list, the coach mark will
     * be hidden.
     */
    suspend fun showPreviousCoachMark() {
        val currentHighlight = getCurrentHighlight() ?: return
        currentHighlight.toolTipState.cleanUp()
        val index = orderedList.indexOf(currentHighlight.key)
        if (index == 0) {
            mutableCurrentHighlight.value = null
            mutableIsVisible.value = false
            return
        }
        mutableCurrentHighlight.value = orderedList.getOrNull(index - 1)
        mutableCurrentHighlight.value?.let {
            showCoachMark(it)
        }
    }

    /**
     * Completes the coaching sequence, clearing all highlights and resetting the state.
     *
     * @param onComplete An optional callback to invoke once all the other clean up logic has
     * taken place.
     */
    fun coachingComplete(onComplete: (() -> Unit)? = null) {
        getCurrentHighlight()?.toolTipState?.cleanUp()
        mutableCurrentHighlight.value = null
        mutableCurrentHighlightBounds.value = Rect.Zero
        mutableCurrentHighlightShape.value = CoachMarkHighlightShape.RoundedRectangle()
        mutableIsVisible.value = false
        onComplete?.invoke()
    }

    /**
     * Gets the current highlight information.
     *
     * @return The current [CoachMarkHighlightState] or null if no highlight is active.
     */
    private fun getCurrentHighlight(): CoachMarkHighlightState<T>? {
        return currentHighlight.value?.let { highlights[it] }
    }

    private fun updateCoachMarkStateInternal(highlight: CoachMarkHighlightState<T>?) {
        mutableIsVisible.value = highlight != null
        mutableCurrentHighlightShape.value =
            highlight?.shape ?: CoachMarkHighlightShape.RoundedRectangle()
        if (currentHighlightBounds.value != highlight?.highlightBounds) {
            mutableCurrentHighlightBounds.value = highlight?.highlightBounds ?: Rect.Zero
        }
    }

    /**
     * Cleans up the tooltip state by dismissing it if visible and calling onDispose.
     */
    private fun BitwardenToolTipState.cleanUp() {
        if (isVisible) {
            dismissBitwardenToolTip()
        }
        onDispose()
    }

    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates a [Saver] for [CoachMarkState] to enable saving and restoring its state.
         *
         * @return A [Saver] that can save and restore [CoachMarkState].
         */
        inline fun <reified T : Enum<T>> saver(): Saver<CoachMarkState<T>, Any> =
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
                    CoachMarkState(
                        orderedList = list,
                        initialCoachMarkHighlight = currentHighlight,
                        isCoachMarkVisible = isVisible,
                    )
                },
            )
    }
}

/**
 * Remembers and saves the state of a [CoachMarkState].
 *
 * @param T The type of the enum used to represent the coach mark keys.
 * @param orderedList The ordered list of coach mark keys.
 * @return A [CoachMarkState] instance.
 */
@Composable
inline fun <reified T : Enum<T>> rememberCoachMarkState(orderedList: List<T>): CoachMarkState<T> {
    return rememberSaveable(saver = CoachMarkState.saver<T>()) {
        CoachMarkState(orderedList)
    }
}

/**
 * Combine two [Rect] to create the largest result rectangle between them.
 * This will include any space between the [Rect] as well.
 */
private fun Rect.union(other: Rect): Rect {
    return Rect(
        left = min(left, other.left),
        top = min(top, other.top),
        right = max(right, other.right),
        bottom = max(bottom, other.bottom),
    )
}
