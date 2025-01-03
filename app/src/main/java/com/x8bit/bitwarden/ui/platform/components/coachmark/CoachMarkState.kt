package com.x8bit.bitwarden.ui.platform.components.coachmark

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Rect
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * Represents a highlight within a coach mark sequence.
 *
 * @param T The type of the enum key used to identify the highlight.
 * @property key The unique key identifying this highlight.
 * @property highlightBounds The rectangular bounds of the area to highlight.
 * @property toolTipState The state of the tooltip associated with this highlight.
 * @property shape The shape of the highlight (e.g., square, oval).
 */
@OptIn(ExperimentalMaterial3Api::class)
data class CoachMarkHighlightState<T : Enum<T>>(
    val key: T,
    val highlightBounds: Rect?,
    val toolTipState: TooltipState,
    val shape: CoachMarkHighlightShape,
)

/**
 * Defines the available shapes for a coach mark highlight.
 */
enum class CoachMarkHighlightShape {
    /**
     * A square-shaped highlight.
     */
    SQUARE,

    /**
     * An oval-shaped highlight.
     */
    OVAL,
}

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
@OptIn(ExperimentalMaterial3Api::class)
open class CoachMarkState<T : Enum<T>>(
    val orderedList: List<T>,
    initialCoachMarkHighlight: T? = null,
    isCoachMarkVisible: Boolean = false,
) {
    private val highlights: MutableMap<T, CoachMarkHighlightState<T>?> = ConcurrentHashMap()
    private val mutableCurrentHighlight = mutableStateOf(initialCoachMarkHighlight)
    val currentHighlight: State<T?> = mutableCurrentHighlight
    private val mutableCurrentHighlightBounds = mutableStateOf(Rect.Zero)
    val currentHighlightBounds: State<Rect> = mutableCurrentHighlightBounds
    private val mutableCurrentHighlightShape = mutableStateOf(CoachMarkHighlightShape.SQUARE)
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
     * [CoachMarkHighlightShape.SQUARE].
     */
    fun updateHighlight(
        key: T,
        bounds: Rect?,
        toolTipState: TooltipState,
        shape: CoachMarkHighlightShape = CoachMarkHighlightShape.SQUARE,
    ) {
        highlights[key] = CoachMarkHighlightState(
            key = key,
            highlightBounds = bounds,
            toolTipState = toolTipState,
            shape = shape,
        ).also {
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
        val highlight = highlights[key]
        highlight?.let {
            val newRect = it.highlightBounds?.union(additionalBounds)
                .takeIf { !isFirstItem } ?: additionalBounds
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
        highlightToShow?.let {
            updateCoachMarkStateInternal(it)
        }
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
        if (index < 0 && previousHighlight != null) return
        mutableCurrentHighlight.value = orderedList.getOrNull(index + 1)
        mutableCurrentHighlight.value?.let {
            showCoachMark(it)
        }
    }

    /**
     * Shows the previous coach mark in the sequence.
     *  If the current highlighted coach mark is the first in the list, the coach mark will
     * be hidden.
     */
    suspend fun showPreviousCoachMark() {
        val currentHighlight = getCurrentHighlight()
        currentHighlight?.toolTipState?.cleanUp() ?: return
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
        mutableCurrentHighlightShape.value = CoachMarkHighlightShape.SQUARE
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
        mutableCurrentHighlightShape.value = highlight?.shape ?: CoachMarkHighlightShape.SQUARE
        if (currentHighlightBounds.value != highlight?.highlightBounds) {
            mutableCurrentHighlightBounds.value = highlight?.highlightBounds ?: Rect.Zero
        }
    }

    /**
     * Cleans up the tooltip state by dismissing it if visible and calling onDispose.
     */
    private fun TooltipState.cleanUp() {
        if (isVisible) {
            dismiss()
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
 * A [CoachMarkState] that depends on a [LazyListState] to automatically scroll to the current
 * Coach Mark if not on currently on the screen.
 */
class LazyListCoachMarkState<T : Enum<T>>(
    private val lazyListState: LazyListState,
    orderedList: List<T>,
    initialCoachMarkHighlight: T? = null,
    isCoachMarkVisible: Boolean = false,
) : CoachMarkState<T>(orderedList, initialCoachMarkHighlight, isCoachMarkVisible) {

    override suspend fun showCoachMark(coachMarkToShow: T) {
        super.showCoachMark(coachMarkToShow)
        lazyListState.searchForKey(coachMarkToShow)
    }

    private suspend fun LazyListState.searchForKey(keyToFind: T): Boolean =
        layoutInfo.visibleItemsInfo.any { it.key == keyToFind }
            .takeIf { itemAlreadyVisible ->
                if (itemAlreadyVisible) {
                    val offset =
                        layoutInfo.visibleItemsInfo.find { visItem ->
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
            } else {
                if (!canScrollBackward) {
                    keepSearching = false
                } else {
                    this.scrollBy(scrollAmount)
                }
            }
        }
        Timber.i("$targetKey has been found: $found by scrolling up.")
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
            } else {
                if (!this.canScrollForward) {
                    // Reached the end of the list without finding the key
                    keepSearching = false
                } else {
                    this.scrollBy(scrollAmount)
                }
            }
        }
        Timber.i("$targetKey has been found: $found by scrolling down.")
        return found
    }

    @Suppress("MagicNumber")
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

private const val END_VIEW_PORT_PIXEL_THRESHOLD = 150
private const val START_VIEW_PORT_PIXEL_THRESHOLD = 40
