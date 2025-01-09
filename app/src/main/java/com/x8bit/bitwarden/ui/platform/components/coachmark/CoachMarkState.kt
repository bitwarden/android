package com.x8bit.bitwarden.ui.platform.components.coachmark

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages the state of a coach mark sequence, guiding users through a series of highlights.
 *
 * This class handles the ordered list of highlights, the currently active highlight,
 * and the overall visibility of the coach mark overlay. It uses a mutex to ensure
 * thread-safe access to the shared state.
 *
 * @param T The type of the enum used to represent the coach mark keys.
 * @property orderedList The ordered list of coach mark keys that define the sequence.
 * @param initialCoachMarkHighlight The initial coach mark to be highlighted, or null if
 * none should be highlighted at start.
 * @param isCoachMarkVisible is any coach mark currently visible.
 */
@OptIn(ExperimentalMaterial3Api::class)
class CoachMarkState<T : Enum<T>>(
    val orderedList: List<T>,
    initialCoachMarkHighlight: T? = null,
    isCoachMarkVisible: Boolean = false,
) {
    private val mutex = Mutex()
    private val highlights: MutableMap<T, CoachMarkHighlightState<T>?> = mutableMapOf()
    private val _currentHighlight = mutableStateOf(initialCoachMarkHighlight)
    val currentHighlight: State<T?> = _currentHighlight
    private val _currentHighlightBounds = mutableStateOf(Rect.Zero)
    val currentHighlightBounds: State<Rect> = _currentHighlightBounds
    private val _currentHighlightShape = mutableStateOf(CoachMarkHighlightShape.SQUARE)
    val currentHighlightShape: State<CoachMarkHighlightShape> = _currentHighlightShape

    private val _isVisible = mutableStateOf(isCoachMarkVisible)
    val isVisible: State<Boolean> = _isVisible

    /**
     * Updates the highlight information for a given key. If the key matches the current shown
     * [key] then we also update the public state for the highlight bounds and shape.
     *
     * @param key The key of the highlight to update.
     * @param bounds The rectangular bounds of the area to highlight. If null, defaults to
     * Rect.Zero.
     * @param toolTipState The state of the tooltip associated with this highlight.
     * @param shape The shape of the highlight (e.g., square, oval). Defaults to
     * [CoachMarkHighlightShape.SQUARE].
     */
    suspend fun updateHighlight(
        key: T,
        bounds: Rect?,
        toolTipState: TooltipState,
        shape: CoachMarkHighlightShape = CoachMarkHighlightShape.SQUARE,
    ) {
        mutex.withLock {
            highlights[key] = CoachMarkHighlightState(
                key = key,
                highlightBounds = bounds ?: Rect.Zero,
                toolTipState = toolTipState,
                shape = shape,
            ).also {
                if (key == currentHighlight.value) {
                    updateCoachMarkStateInternal(it)
                }
            }
        }
    }

    /**
     * Shows the current coach mark, if it exists. If there is no current highlighted
     * coach mark, attempt to show the "next" available coach mark.
     */
    suspend fun showCoachMark(coachMarkToShow: T? = null) {
        val highlightToShow = mutex.withLock {
            coachMarkToShow?.let {
                _currentHighlight.value = it
            }
            getCurrentHighlight()
        }
        if (highlightToShow != null) {
            _isVisible.value = true
            highlightToShow.toolTipState.show()
        } else {
            showNextCoachMark()
        }
    }

    /**
     * Shows the next highlight in the sequence.
     * If there is no previous highlight, it will show the first highlight.
     * If the previous highlight is the last in the list, nothing will happen.
     */
    suspend fun showNextCoachMark() {
        val highlightToShow = mutex.withLock {
            val previousHighlight = getCurrentHighlight()
            previousHighlight?.toolTipState?.cleanUp()
            val index = orderedList.indexOf(previousHighlight?.key)
            if (index < 0 && previousHighlight != null) return
            _currentHighlight.value = orderedList.getOrNull(index + 1)
            _isVisible.value = currentHighlight.value != null
            getCurrentHighlight()
        }
        updateCoachMarkStateInternal(highlightToShow)
        highlightToShow?.toolTipState?.show()
    }

    /**
     * Shows the previous coach mark in the sequence.
     *  If the current highlighted coach mark is the first in the list, the coach mark will
     * be hidden.
     */
    suspend fun showPreviousCoachMark() {
        val highlightToShow = mutex.withLock {
            val currentHighlight = getCurrentHighlight()
            currentHighlight?.toolTipState?.cleanUp() ?: return
            val index = orderedList.indexOf(currentHighlight.key)
            if (index == 0) {
                _currentHighlight.value = null
                _isVisible.value = false
                return
            }
            _currentHighlight.value = orderedList.getOrNull(index - 1)
            _isVisible.value = this.currentHighlight.value != null
            getCurrentHighlight()
        }
        updateCoachMarkStateInternal(highlightToShow)
        highlightToShow?.toolTipState?.show()
    }

    /**
     * Completes the coaching sequence, clearing all highlights and resetting the state.
     */
    fun coachingComplete() {
        getCurrentHighlight()?.toolTipState?.cleanUp()
        _currentHighlight.value = null
        _currentHighlightBounds.value = Rect.Zero
        _currentHighlightShape.value = CoachMarkHighlightShape.SQUARE
        _isVisible.value = false
    }

    /**
     * Gets the current highlight information.
     *
     * @return The current [CoachMarkHighlightState] or null if no highlight is active.
     */
    private fun getCurrentHighlight(): CoachMarkHighlightState<T>? {
        return highlights[currentHighlight.value]
    }

    private fun updateCoachMarkStateInternal(highlight: CoachMarkHighlightState<T>?) {
        _currentHighlightShape.value = highlight?.shape ?: CoachMarkHighlightShape.SQUARE
        _currentHighlightBounds.value = highlight?.highlightBounds ?: Rect.Zero
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
                    val enumList = restoredList[0] as List<String>
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
    val highlightBounds: Rect,
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
