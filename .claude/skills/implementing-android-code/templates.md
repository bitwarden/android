# Code Templates - Bitwarden Android

Copy-pasteable templates derived from actual codebase patterns. Replace `Example` with your feature name.

---

## ViewModel Template (State-Action-Event Pattern)

**Based on**: `app/src/main/kotlin/com/x8bit/bitwarden/ui/auth/feature/login/LoginViewModel.kt`

### State Class

```kotlin
@Parcelize
data class ExampleState(
    val isLoading: Boolean = false,
    val data: String? = null,
    @IgnoredOnParcel val sensitiveInput: String = "", // Sensitive data excluded from parcel
    val dialogState: DialogState? = null,
) : Parcelable {

    /**
     * Dialog states for the Example screen.
     */
    sealed class DialogState : Parcelable {
        @Parcelize
        data class Error(
            val title: Text? = null,
            val message: Text,
            val error: Throwable? = null,
        ) : DialogState()

        @Parcelize
        data class Loading(val message: Text) : DialogState()
    }
}
```

### Event Sealed Class

```kotlin
/**
 * One-shot UI events for the Example screen.
 */
sealed class ExampleEvent {
    data object NavigateBack : ExampleEvent()

    data class ShowToast(val message: Text) : ExampleEvent()
}
```

### Action Sealed Class (with Internal)

```kotlin
/**
 * User and system actions for the Example screen.
 */
sealed class ExampleAction {
    data object BackClick : ExampleAction()

    data object SubmitClick : ExampleAction()

    data class InputChanged(val input: String) : ExampleAction()

    /**
     * Internal actions dispatched by the ViewModel from coroutines.
     */
    sealed class Internal : ExampleAction() {
        data class ReceiveDataState(
            val dataState: DataState<ExampleData>,
        ) : Internal()

        data class ReceiveDataResult(
            val result: ExampleResult,
        ) : Internal()
    }
}
```

### ViewModel

```kotlin
private const val KEY_STATE = "state"

/**
 * ViewModel for the Example screen.
 */
@HiltViewModel
class ExampleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exampleRepository: ExampleRepository,
) : BaseViewModel<ExampleState, ExampleEvent, ExampleAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val args = savedStateHandle.toExampleArgs()
            ExampleState(
                data = args.itemId,
            )
        },
) {

    init {
        // Persist state for process death recovery
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)

        // Collect repository flows as internal actions
        exampleRepository.dataFlow
            .map { ExampleAction.Internal.ReceiveDataState(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ExampleAction) {
        when (action) {
            ExampleAction.BackClick -> handleBackClick()
            ExampleAction.SubmitClick -> handleSubmitClick()
            is ExampleAction.InputChanged -> handleInputChanged(action)
            is ExampleAction.Internal.ReceiveDataState -> {
                handleReceiveDataState(action)
            }
            is ExampleAction.Internal.ReceiveDataResult -> {
                handleReceiveDataResult(action)
            }
        }
    }

    private fun handleBackClick() {
        sendEvent(ExampleEvent.NavigateBack)
    }

    private fun handleSubmitClick() {
        viewModelScope.launch {
            val result = exampleRepository.submitData(state.data.orEmpty())
            sendAction(ExampleAction.Internal.ReceiveDataResult(result))
        }
    }

    private fun handleInputChanged(action: ExampleAction.InputChanged) {
        mutableStateFlow.update { it.copy(sensitiveInput = action.input) }
    }

    private fun handleReceiveDataState(
        action: ExampleAction.Internal.ReceiveDataState,
    ) {
        when (action.dataState) {
            is DataState.Loaded -> {
                mutableStateFlow.update {
                    it.copy(
                        isLoading = false,
                        data = action.dataState.data.toString(),
                    )
                }
            }

            is DataState.Loading -> {
                mutableStateFlow.update { it.copy(isLoading = true) }
            }

            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        isLoading = false,
                        dialogState = ExampleState.DialogState.Error(
                            message = BitwardenString.generic_error_message.asText(),
                            error = action.dataState.error,
                        ),
                    )
                }
            }

            else -> Unit
        }
    }

    private fun handleReceiveDataResult(
        action: ExampleAction.Internal.ReceiveDataResult,
    ) {
        when (val result = action.result) {
            is ExampleResult.Success -> {
                mutableStateFlow.update {
                    it.copy(
                        isLoading = false,
                        data = result.data,
                    )
                }
            }

            is ExampleResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        isLoading = false,
                        dialogState = ExampleState.DialogState.Error(
                            message = result.message?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }
        }
    }
}
```

---

## Navigation Template (Type-Safe Routes)

**Based on**: `app/src/main/kotlin/com/x8bit/bitwarden/ui/auth/feature/login/LoginNavigation.kt`

```kotlin
@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.feature.example

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * Route for the Example screen.
 */
@Serializable
@OmitFromCoverage
data class ExampleRoute(
    val itemId: String,
    val isEditMode: Boolean = false,
)

/**
 * Args extracted from [SavedStateHandle] for the Example screen.
 */
@OmitFromCoverage
data class ExampleArgs(
    val itemId: String,
    val isEditMode: Boolean,
)

/**
 * Extracts [ExampleArgs] from the [SavedStateHandle].
 */
fun SavedStateHandle.toExampleArgs(): ExampleArgs {
    val route = this.toRoute<ExampleRoute>()
    return ExampleArgs(
        itemId = route.itemId,
        isEditMode = route.isEditMode,
    )
}

/**
 * Navigate to the Example screen.
 */
fun NavController.navigateToExample(
    itemId: String,
    isEditMode: Boolean = false,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = ExampleRoute(
            itemId = itemId,
            isEditMode = isEditMode,
        ),
        navOptions = navOptions,
    )
}

/**
 * Add the Example screen destination to the navigation graph.
 */
fun NavGraphBuilder.exampleDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<ExampleRoute> {
        ExampleScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}
```

---

## Screen/Compose Template

**Based on**: `app/src/main/kotlin/com/x8bit/bitwarden/ui/auth/feature/login/LoginScreen.kt`

```kotlin
package com.x8bit.bitwarden.ui.feature.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString

/**
 * The Example screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExampleScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExampleViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            ExampleEvent.NavigateBack -> onNavigateBack()
            is ExampleEvent.ShowToast -> {
                // Handle toast
            }
        }
    }

    // Dialogs
    ExampleDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(ExampleAction.ErrorDialogDismiss) }
        },
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.example),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(ExampleAction.BackClick) }
                },
            )
        },
    ) { paddingValues ->
        ExampleScreenContent(
            state = state,
            onInputChanged = remember(viewModel) {
                { viewModel.trySendAction(ExampleAction.InputChanged(it)) }
            },
            onSubmitClick = remember(viewModel) {
                { viewModel.trySendAction(ExampleAction.SubmitClick) }
            },
            modifier = Modifier
                .fillMaxSize(),
        )
    }
}
```

---

## Data Layer Template (Repository + Hilt Module)

**Based on**: `app/src/main/kotlin/com/x8bit/bitwarden/data/tools/generator/repository/di/GeneratorRepositoryModule.kt`

### Interface

```kotlin
/**
 * Provides data operations for the Example feature.
 */
interface ExampleRepository {
    /**
     * Submits data and returns a typed result.
     */
    suspend fun submitData(input: String): ExampleResult

    /**
     * Continuously observed data stream.
     */
    val dataFlow: StateFlow<DataState<ExampleData>>
}
```

### Sealed Result Class

```kotlin
/**
 * Domain-specific result for Example operations.
 */
sealed class ExampleResult {
    data class Success(val data: String) : ExampleResult()
    data class Error(val message: String?) : ExampleResult()
}
```

### Implementation

```kotlin
/**
 * Default implementation of [ExampleRepository].
 */
class ExampleRepositoryImpl(
    private val exampleDiskSource: ExampleDiskSource,
    private val exampleService: ExampleService,
    private val dispatcherManager: DispatcherManager,
) : ExampleRepository {

    override val dataFlow: StateFlow<DataState<ExampleData>>
        get() = // ...

    override suspend fun submitData(input: String): ExampleResult {
        return exampleService
            .postData(input)
            .fold(
                onSuccess = { ExampleResult.Success(it.toModel()) },
                onFailure = { ExampleResult.Error(it.message) },
            )
    }
}
```

### Hilt Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ExampleRepositoryModule {

    @Provides
    @Singleton
    fun provideExampleRepository(
        exampleDiskSource: ExampleDiskSource,
        exampleService: ExampleService,
        dispatcherManager: DispatcherManager,
    ): ExampleRepository = ExampleRepositoryImpl(
        exampleDiskSource = exampleDiskSource,
        exampleService = exampleService,
        dispatcherManager = dispatcherManager,
    )
}
```

---

## Security Templates

**Based on**: `app/src/main/kotlin/com/x8bit/bitwarden/data/auth/datasource/disk/di/AuthDiskModule.kt` and `AuthDiskSourceImpl.kt`

### Encrypted Disk Source (Module)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ExampleDiskModule {

    @Provides
    @Singleton
    fun provideExampleDiskSource(
        @EncryptedPreferences encryptedSharedPreferences: SharedPreferences,
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
        json: Json,
    ): ExampleDiskSource = ExampleDiskSourceImpl(
        encryptedSharedPreferences = encryptedSharedPreferences,
        sharedPreferences = sharedPreferences,
        json = json,
    )
}
```

### Encrypted Disk Source (Implementation)

```kotlin
/**
 * Disk source for Example data using encrypted and unencrypted storage.
 */
class ExampleDiskSourceImpl(
    encryptedSharedPreferences: SharedPreferences,
    sharedPreferences: SharedPreferences,
    private val json: Json,
) : BaseEncryptedDiskSource(
    encryptedSharedPreferences = encryptedSharedPreferences,
    sharedPreferences = sharedPreferences,
),
    ExampleDiskSource {

    private companion object {
        const val ENCRYPTED_TOKEN_KEY = "bwSecureStorage:exampleToken"
        const val UNENCRYPTED_PREF_KEY = "bwPrefs:examplePreference"
    }

    override var authToken: String?
        get() = getEncryptedString(ENCRYPTED_TOKEN_KEY)
        set(value) { putEncryptedString(ENCRYPTED_TOKEN_KEY, value) }

    override var uiPreference: Boolean
        get() = getBoolean(UNENCRYPTED_PREF_KEY, defaultValue = false)
        set(value) { putBoolean(UNENCRYPTED_PREF_KEY, value) }
}
```

---

## Testing Templates

**Based on**: `app/src/test/kotlin/com/x8bit/bitwarden/ui/tools/feature/generator/GeneratorViewModelTest.kt`

### ViewModel Test

```kotlin
class ExampleViewModelTest : BaseViewModelTest() {

    // Mock dependencies
    private val mockRepository = mockk<ExampleRepository>()
    private val mutableDataFlow = MutableStateFlow<DataState<ExampleData>>(DataState.Loading)

    @BeforeEach
    fun setup() {
        every { mockRepository.dataFlow } returns mutableDataFlow
    }

    @Test
    fun `initial state should be correct when there is no saved state`() {
        val viewModel = createViewModel(state = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when there is a saved state`() {
        val savedState = DEFAULT_STATE.copy(data = "saved")
        val viewModel = createViewModel(state = savedState)
        assertEquals(savedState, viewModel.stateFlow.value)
    }

    @Test
    fun `SubmitClick should call repository and update state on success`() = runTest {
        val expected = ExampleResult.Success(data = "result")
        coEvery { mockRepository.submitData(any()) } returns expected

        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            // Initial state
            assertEquals(DEFAULT_STATE, awaitItem())

            viewModel.trySendAction(ExampleAction.SubmitClick)

            // Updated state after result
            assertEquals(
                DEFAULT_STATE.copy(data = "result", isLoading = false),
                awaitItem(),
            )
        }
    }

    @Test
    fun `SubmitClick should show error dialog on failure`() = runTest {
        val expected = ExampleResult.Error(message = "Network error")
        coEvery { mockRepository.submitData(any()) } returns expected

        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())

            viewModel.trySendAction(ExampleAction.SubmitClick)

            val errorState = awaitItem()
            assertTrue(errorState.dialogState is ExampleState.DialogState.Error)
        }
    }

    @Test
    fun `BackClick should emit NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ExampleAction.BackClick)
            assertEquals(ExampleEvent.NavigateBack, awaitItem())
        }
    }

    // Helper to create ViewModel with optional saved state
    private fun createViewModel(
        state: ExampleState? = DEFAULT_STATE,
    ): ExampleViewModel = ExampleViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(KEY_STATE to state),
        ),
        exampleRepository = mockRepository,
    )

    companion object {
        private val DEFAULT_STATE = ExampleState(
            isLoading = false,
            data = null,
        )
    }
}
```

### Flow Testing with stateEventFlow

```kotlin
@Test
fun `SubmitClick should update state and emit event`() = runTest {
    coEvery { mockRepository.submitData(any()) } returns ExampleResult.Success("data")

    val viewModel = createViewModel()
    viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
        viewModel.trySendAction(ExampleAction.SubmitClick)

        // Assert state change
        assertEquals(
            DEFAULT_STATE.copy(data = "data"),
            stateFlow.awaitItem(),
        )

        // Assert event emission
        assertEquals(
            ExampleEvent.ShowToast("Success".asText()),
            eventFlow.awaitItem(),
        )
    }
}
```