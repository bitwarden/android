# Architecture

- [Overview](#overview)
- [Data Layer](#data-layer)
  - [Data Sources](#data-sources)
  - [Managers](#managers)
  - [Repositories](#repositories)
  - [Note on Dependency Injection](#note-on-dependency-injection)
- [UI Layer](#ui-layer)
  - [ViewModels / MVVM](#viewmodels--mvvm)
    - [Example](#example)
  - [Screens / Compose](#screens--compose)
    - [State Hoisting](#state-hoisting)
    - [Example](#example-1)
  - [Navigation](#navigation)
    - [State-based Navigation](#state-based-navigation)
    - [Event-based Navigation](#event-based-navigation)
    - [Navigation Implementation](#navigation-implementation)
    - [Example](#example-2)

## Overview

The app is broadly divided into the **data layer** and the **UI layer** and this is reflected in the two top-level packages of the app, `data` and `ui`. Each of these packages is then subdivided into the following sub-packages:

- `auth`
- `autofill`
- `platform`
- `tools`
- `vault`

Note that these packages are currently aligned with the [CODEOWNERS](../.github/CODEOWNERS) files for the project; no additional direct sub-packages of `ui` or `data` should be added. While this top-level structure is deliberately inflexible, the package structure within each `auth`, `autofill`, etc. are not specifically prescribed.

The responsibilities of the data layer are to manage the storage and retrieval of data from low-level sources (such as from the network, persistence, or Bitwarden SDK) and to expose them in a more ready-to-consume manner by the UI layer via "repository" and "manager" classes. The UI layer is then responsible for any final processing of this data for display in the UI as well for receiving events from the UI, updating the tracked state accordingly.

## Data Layer

The data layer is where all the UI-independent data is stored and retrieved. It consists of both raw data sources as well as higher-level "repository" and "manager" classes.

Note that any functions exposed by a data layer class that must perform asynchronous work do so by exposing **suspending functions** that may run inside [coroutines](https://kotlinlang.org/docs/coroutines-guide.html) while any streaming sources of data are handled by exposing [Flows](https://kotlinlang.org/docs/flow.html).

### Data Sources

The lowest level of the data layer are the "data source" classes. These are the raw sources of data that include data persisted in [Room](https://developer.android.com/jetpack/androidx/releases/room) / [SharedPreferences](https://developer.android.com/reference/android/content/SharedPreferences), data retrieved from network requests using [Retrofit](https://github.com/square/retrofit), and data retrieved via interactions with the [Bitwarden SDK](https://github.com/bitwarden/sdk).

Note that these data sources are constructed in a manner that adheres to a very important principle of the app: **that function calls should not throw exceptions** (see the [style and best practices documentation](STYLE_AND_BEST_PRACTICES.md#best-practices--kotlin) for more details.) In the case of data sources, this tends to mean that suspending functions like those representing network requests or Bitwarden SDK calls should return a [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/) type. This is an important responsibility of the data layer as a wrapper around other third party libraries, as dependencies like Retrofit and the Bitwarden SDK tend to throw exceptions to indicate errors instead.

### Managers

Manager classes represent something of a middle level of the data layer. While some manager classes like [VaultLockManager](../app/src/main/java/com/x8bit/bitwarden/data/vault/manager/VaultLockManager.kt) depend on the the lower-level data sources, others are wrappers around OS-level classes (ex: [AppStateManager](../app/src/main/java/com/x8bit/bitwarden/data/platform/manager/AppStateManager.kt)) while others have no dependencies at all (ex: [SpecialCircumstanceManager](../app/src/main/java/com/x8bit/bitwarden/data/platform/manager/SpecialCircumstanceManager.kt)). The commonality amongst the manager classes is that they tend to have a single discrete responsibility. These classes may also exist solely in the data layer for use inside a repository or manager class, like [AppStateManager](../app/src/main/java/com/x8bit/bitwarden/data/platform/manager/AppStateManager.kt), or may be exposed directly to the UI layer, like [SpecialCircumstanceManager](../app/src/main/java/com/x8bit/bitwarden/data/platform/manager/SpecialCircumstanceManager.kt).

### Repositories

Repository classes represent the outermost level of the data layer. They can take data sources, managers, and in rare cases even other repositories as dependencies and are meant to be exposed directly to the UI layer. They synthesize data from multiple sources and combine various asynchronous requests as necessary in order to expose data to the UI layer in a more appropriate form. These classes tend to have broad responsibilities that generally cover a major domain of the app, such as authentication ([AuthRepository](../app/src/main/java/com/x8bit/bitwarden/data/auth/repository/AuthRepository.kt)) or vault access ([VaultRepository](../app/src/main/java/com/x8bit/bitwarden/data/vault/repository/VaultRepository.kt)).

Repository classes also feature functions that do not throw exceptions, but unlike the lower levels of the data layer the `Result` type should be avoided in favor of custom sealed classes that represent the various success/error cases in a more processed form. Returning raw `Throwable`/`Exception` instances as part of "error" states should be avoided when possible.

In some cases a source of data may be continuously observed and in these cases a repository may choose to expose a [StateFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/) that emits data updates using the [DataState](../app/src/main/java/com/x8bit/bitwarden/data/platform/repository/model/DataState.kt) wrapper.

### Note on Dependency Injection

Nearly all classes in the data layer consist of interfaces representing exposed behavior and a corresponding `...Impl` class implementing that interface (ex: [AuthDiskSource](../app/src/main/java/com/x8bit/bitwarden/data/auth/datasource/disk/AuthDiskSource.kt) / [AuthDiskSourceImpl](../app/src/main/java/com/x8bit/bitwarden/data/auth/datasource/disk/AuthDiskSourceImpl.kt)). All `...Impl` classes are intended to be manually constructed while their associated interfaces are provided for dependency injection via a [Hilt Module](https://dagger.dev/hilt/modules.html) (ex: [PlatformNetworkModule](../app/src/main/java/com/x8bit/bitwarden/data/platform/datasource/network/di/PlatformNetworkModule.kt)). This prevents the `...Impl` classes from being injected by accident and allows the interfaces to be easily mocked/faked in tests.

## UI Layer

The UI layer adheres to the concept of [unidirectional data flow](https://developer.android.com/develop/ui/compose/architecture#udf) and makes use of the MVVM design pattern. Both concepts are in line what Google currently recommends as the best approach for building the UI-layer of a modern Android application and this allows us to make use of all the available tooling Google provides as part of the [Jetpack suite of libraries](https://developer.android.com/jetpack). The MVVM implementation is built around the Android [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) class and the UI itself is constructed using the [Jetpack Compose](https://developer.android.com/develop/ui/compose), a declarative UI framework specifically built around the unidirectional data flow approach.

Each screen in the app is associated with at least the following three classes/files:

- A `...ViewModel` class responsible for managing the data and state for the screen.
- A `...Screen` class that contains the Compose implementation of the UI.
- A `...Navigation` file containing the details for how to add the screen to the overall navigation graph and how navigate to it within the graph.

### ViewModels / MVVM

The app's approach to MVVM is based around the handling of "state", "actions", and "events" and is encoded in the [BaseViewModel](../app/src/main/java/com/x8bit/bitwarden/ui/platform/base/BaseViewModel.kt) class.

- **State:** The "state" represents the complete internal and external **total state** of the ViewModel (VM). Any and all information needed to configure the UI that is associated with the VM should be included in this state and is exposed via the `BaseViewModel.stateFlow` property as `StateFlow<S>` (where `S` represents the unique type of state associated with the VM). This state is typically a combination of state from the data layer (such as account information) and state from the UI layer (such as data entered into a text field).

  There should be no additional `StateFlow` exposed from a VM that would represent some other kind of state; all state should be represented by `S`. Additionally, any internal state not directly needed by the UI but which influences the behavior of the VM should be included as well in order to keep all state managed by the VM in a single place.

- **Actions:** The "actions" represent interactions with the VM in some way that could potentially cause an update to that total state. These can be external actions coming from the user's interaction with the UI, like click events, or internal actions coming from some asynchronous process internal to the VM itself, like the result of some suspending functions. Actions are sent by interacting directly with `BaseViewModel.actionChannel` or by using the `BaseViewModel.sendAction` and `BaseViewModel.trySendAction` helpers. All actions are then processed synchronously in a queue in the `handleAction` function.

  It is worth emphasizing that state should never be updated inside a coroutine in a VM; all asynchronous work that results in a state update should do so by posting an internal action. This ensures that the only place that state changes can occur is synchronously inside the `handleAction` function. This makes the process of finding and reasoning about state changes easier and simplifies debugging.

- **Events:** The "events" represent discrete, one-shot side-effects and are typically associated with navigation events triggered by some user action. They are sent internally using `BaseViewModel.sendEvent` and may be consumed by the UI layer via the `BaseViewModel.eventFlow` property. An [EventsEffect](../app/src/main/java/com/x8bit/bitwarden/ui/platform/base/util/EventsEffect.kt) should typically be used to simplify the consumption of these events.

VMs are typically injected using [Dagger Hilt](https://developer.android.com/training/dependency-injection/hilt-android) by annotating them with `@HiltViewModel`. This allows them to be constructed, retrieved, and cached in the Compose UI layer by calling `hiltViewModel()` with the appropriate type. Dependencies passed into the VM constructor will typically be singletons of the graph, such as [AuthRepository](../app/src/main/java/com/x8bit/bitwarden/data/auth/repository/AuthRepository.kt). In cases where the VM needs to initialized with some specific data (like an ID) that is sent from the previous screen, this data should be retrieved by injecting the [SavedStateHandle](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate) and pulling out data using a type-safe wrapper (ex: [LoginArgs](../app/src/main/java/com/x8bit/bitwarden/ui/auth/feature/login/LoginNavigation.kt)).

#### Example

The following is an example that demonstrates many of the above principles and best practices when using `BaseViewModel` to implement a VM. It has the following features:

- It is injected with a repository (`ExampleRepository`) that provides streaming `ExampleData` and a `SavedStateHandle` in order to pull initial data using a `ExampleArgs` wrapper class.
- It has state that manages several properties of interest to the UI: `exampleData`, `isToggledEnabled`, and `dialogState`.
- It receives external actions from the UI (`ContinueButtonClick` and `ToggleValueUpdate`) as well as internal actions launch from inside different coroutines coroutines (`Internal.ExampleDataReceive`, `Internal.ToggleValueSync`). These actions result in state updates or the emission of an event (`NavigateToNextScreen`).
- It saves the current state to the `SavedStateHandle` in order to restore it later after process death and restoration. This ensures the user's actions and associated state will not be lost if the app goes into the background and is temporarily killed by the OS to conserve memory.


<details>
<summary>Show example</summary>

```kotlin
private const val KEY_STATE = "state"

@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val exampleRepository: ExampleRepository,
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<ExampleState, ExampleEvent, ExampleAction>(
    // If previously saved state data is present in the SavedStateHandle, use that as the initial
    // state, otherwise initialize from repository and navigation data.
    initialState = savedStateHandle[KEY_STATE] ?: ExampleState(
        exampleData = exampleRepository.exampleDataStateFlow.value,
        isToggleEnabled = ExampleArgs(savedStateHandle).isToggleEnabledInitialValue,
        dialogState = null,
    )
) {
    init {
        // As the state updates, write to saved state handle for retrieval after process death and
        // restoration.
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)

        exampleRepository
            .exampleDataStateFlow
            // Asynchronously received data is converted to an internal action and sent in order to
            // be handled by `handleAction`.
            .map { ExampleAction.Internal.ExampleDataReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ExampleAction) {
        when (action) {
            is ExampleAction.ContinueButtonClick -> handleContinueButtonClick()
            is ExampleAction.ToggleValueUpdate -> handleToggleValueUpdate(action)
            is ExampleAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleContinueButtonClick() {
        // Update the state to show a dialog
        mutableStateFlow.update {
            it.copy(
                dialogState = ExampleState.DialogState.Loading,
            )
        }

        // Run a suspending call in a coroutine to fetch data and post the result back as an
        // internal action for further processing so that all state changes and event emissions
        // happen synchronously in `handleAction`.
        viewModelScope.launch {
            val completionData = exampleRepository
                .fetchCompletionData(isToggleEnabled = state.isToggleEnabled)

            sendAction(
                ExampleAction.Internal.CompletionDataReceive(
                    completionData = completionData,
                )
            )
        }
    }

    private fun handleToggleValueUpdate(action: ExampleAction.ToggleValueUpdate) {
        // Update the state
        mutableStateFlow.update {
            it.copy(
                isToggleEnabled = action.isToggleEnabled,
            )
        }
    }

    private fun handleInternalAction(action: ExampleAction.Internal) {
        when (action) {
          is ExampleAction.Internal.CompletionDataReceive -> handleCompletionDataReceive(action)
          is ExampleAction.Internal.ExampleDataReceive -> handleExampleDataReceive(action)
        }
    }

    private fun handleCompletionDataReceive(action: ExampleAction.Internal.CompletionDataReceive) {
        // Update the state to clear the dialog
        mutableStateFlow.update {
            it.copy(
                dialogState = null,
            )
        }

        // Send event with data from the action to navigate
        sendEvent(
            ExampleEvent.NavigateToNextScreen(
                completionData = action.completionData,
            )
        )
    }

    private fun handleExampleDataReceive(action: ExampleAction.Internal.ExampleDataReceive) {
        // Update the state
        mutableStateFlow.update {
            it.copy(
                exampleData = action.exampleData,
            )
        }
    }
}

@Parcelize
data class ExampleState(
    val exampleData: String,
    val isToggleEnabled: Boolean,
    val dialogState: DialogState?,
) : Parcelable {

    sealed class DialogState : Parcelable {
        @Parcelize
        data object Loading : DialogState()
    }
}

sealed class ExampleEvent {
    data class NavigateToNextScreen(
        val completionData: CompletionData,
    ) : ExampleEvent()
}

sealed class ExampleAction {
    data object ContinueButtonClick : ExampleAction()

    data class ToggleValueUpdate(
        val isToggleEnabled: Boolean,
    ) : ExampleAction()

    sealed class Internal : ExampleAction() {
        data class CompletionDataReceive(
          val completionData: CompletionData,
        ) : Internal()

        data class ExampleDataReceive(
            val exampleData: String,
        ) : Internal()
    }
}
```
</details>


### Screens / Compose

Each unique screen destination is represented by a composable `...Screen` function annotated with `@Composable`. The responsibilities of this layer include:

- Receiving "state" data from the associated `ViewModel` and rendering the UI accordingly.
- Receiving "events" from the associated `ViewModel` and taking the corresponding action, which is typically to trigger a passed-in callback function for navigation purposes. An [EventsEffect](../app/src/main/java/com/x8bit/bitwarden/ui/platform/base/util/EventsEffect.kt) should be used to simplify this process.
- Sending "actions" to the `ViewModel` indicating user interactions or UI-layer events as necessary.
- Interacting with purely UI-layer "manager" classes that are not appropriate or possible to be injected into the `ViewModel` (such as those concerning permissions or camera access).

In order to both provide consistency to the app and to simplify the development of new screens, an extensive system of reusable composable components has been developed and may be found in the [components package](../app/src/main/java/com/x8bit/bitwarden/ui/platform/components). These tend to bear the prefix `Bitwarden...` to easily distinguish them from similar OS-level components. If there is any new unique UI component that is added, it should always be considered if it should be made a shareable component.

Refer to the [style and best practices documentation](STYLE_AND_BEST_PRACTICES.md#best-practices--jetpack-compose) for additional information on best practices when using Compose.

#### State-hoisting

Jetpack Compose is built around the idea that the state required to render any given composable function can be "hoisted" to higher levels that may need access to that state. This means that the responsibility for keeping track of the current state of a component may not typically reside with the component itself. It is important, therefore, to understand where the best place is to manage any given piece of UI state.

This is discussed in detail in [Google's state-hoisting documentation](https://developer.android.com/develop/ui/compose/state-hoisting) and can be summarized roughly as follows: **state should be hoisted as high as is necessary to perform any relevant logic and no higher.** This is a pattern that is followed in the Bitwarden app and tends to lead to the following:

- Any state that will eventually need to be used by the VM should be hoisted to the VM. For example, any text input, toggle values, etc. that may be updated by user interactions should be completely controlled by the VM. These state changes are communicated via the "actions" that the Compose layer sends, which trigger corresponding updates the VM's total "state".
- Any UI state that will not be used by logic inside the VM should remain in the UI layer. For example, visibility toggle states for password inputs should typically remain out of the VM.

These rules can lead to some notable differences in how certain dialogs are handled. For example, loading dialogs are controlled by events that occur in the VM, such as when a network request is started and when it completes. This requires the VM to be in charge of managing the visibility state of the dialog. However, some dialogs appear as the result of clicking on an item in the UI in order to simply display information or to ask for the user's confirmation before some action is taken. Because there is no logic in the VM that depends on the visibility of these particular dialogs, their visibility state can be controlled by the UI. Note, however, that any user interaction that results in a navigation to a new screen should always be routed to the VM first, as the VM should always be in charge of triggering changes at the per-screen level.

#### Example

The following shows off the basic structure of a composable `...Screen` implementation. Note that:

- The VM is "injected" using the `hiltViewModel()` helper function. This will correctly create, cache, and scope the VM in production code while making it easier to pass in mock/fake implementations in test code.
- An `onNavigateToNextScreen` function is also passed in, which can be called to trigger a navigation via a call to a [NavController](https://developer.android.com/reference/androidx/navigation/NavController) in the outer layers of the navigation graph. Passing in a function rather than the `NavController` itself decouples the screen code from the navigation framework and greatly simplifies the testing of screens.
- The VM "state" is consumed using `viewModel.stateFlow.collectAsStateWithLifecycle()`. This will cause the composable to "recompose" and update whenever updates are pushed to the `viewModel.stateFlow`.
- The VM "events" are consumed using an [EventsEffect](../app/src/main/java/com/x8bit/bitwarden/ui/platform/base/util/EventsEffect.kt) and demonstrate how the `onNavigateToNextScreen` may be triggered.
- The current state of the text, switch, and button are hoisted to the VM and pulled out from the `state`. User interactions with the switch and button result in "actions" being sent to the VM using `viewModel.trySendAction`.
- Reusable components (`BitwardenLoadingDialog`, `BitwardenSwitch`, and `BitwardenFilledButton`) are used where possible in order to build the screen using the correct theming and reduce code duplication. When this is not possible (such as when rending the `Text` composable) all colors and styles are pulled from the BitwardenTheme object.

<details>
<summary>Show example</summary>

```kotlin
@Composable
fun ExampleScreen(
    onNavigateToNextScreen: (CompletionData) -> Unit,
    viewModel: ExampleViewModel = hiltViewModel(),
) {
    // Collect state
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    // Handle events
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is ExampleEvent.NavigateToNextScreen -> {
                onNavigateToNextScreen(event.completionData)
            }
        }
    }

    // Show state-based dialogs if necessary
    when (state.dialogState) {
        ExampleState.DialogState.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(
                    text = R.string.loading.asText(),
                )
            )
        }

        else -> Unit
    }

    // Render the remaining state
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = state.exampleData,
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.headlineSmall,
            color = BitwardenTheme.colorScheme.textColors.primary,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .wrapContentHeight(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        BitwardenSwitch(
            label = stringResource(id = R.string.toggle_label),
            isChecked = state.isToggleEnabled,
            // Use remember(viewModel) to ensure the unstable lambda doesn't trigger unnecessary
            // recompositions.
            onCheckedChange = remember(viewModel) {
                { viewModel.trySendAction(ExampleAction.ToggleValueUpdate(it)) }
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        BitwardenFilledButton(
            label = stringResource(id = R.string.continue_text),
            onClick = remember(viewModel) {
                { viewModel.trySendAction(ExampleAction.ContinueButtonClick) }
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
    }
}
```
</details>

### Navigation

Navigation in the app is achieved via the [Compose Navigation](https://developer.android.com/develop/ui/compose/navigation) component. This involves specifying "destinations" within a [NavHost](https://developer.android.com/reference/androidx/navigation/NavHost) and using a [NavController](https://developer.android.com/reference/androidx/navigation/NavController) to trigger actual navigation events. The app uses a mix of state-based navigation at the highest level (in order to determine the overall app flow the user is in) and event-based navigation (to handle navigation within a flow based on user interactions).

#### State-based Navigation

State-based navigation is handled by the [RootNavViewModel](../app/src/main/java/com/x8bit/bitwarden/ui/platform/feature/rootnav/RootNavViewModel.kt) and [RootNavScreen](../app/src/main/java/com/x8bit/bitwarden/ui/platform/feature/rootnav/RootNavScreen.kt). The current navigation state is determined within the `RootNavViewModel` by monitoring the overall [UserState](../app/src/main/java/com/x8bit/bitwarden/data/auth/repository/model/UserState.kt) along with any [SpecialCircumstance](../app/src/main/java/com/x8bit/bitwarden/data/platform/manager/model/SpecialCircumstance.kt). The `UserState` encodes whether there are any user accounts, whether they are logged in, etc., while the `SpecialCircumstance` describes a particular circumstance under which the app may have been launched (such as being opened in order to manually select an autofill value). The `RootNavState` calculated by the `RootNavViewModel` is then used by the `RootNavScreen` in order to trigger a navigation only if the overall state has changed.

The benefit of state-based navigation at this level is that it allows for a variety of state changes that may happen deep within the data layer to automatically trigger navigation to the correct top-level flow without the rest of the UI needing to be concerned with these details. For example, when logging out the current user, the data layer can decide the new state and the `RootNavViewModel` can decide where to go next, rather than having the screen that triggered the logout action choose the next navigation state (which would then need to be redundantly handled in multiple places in the app).

State-based navigation should be limited to specific cases that affect the overall flow of the app and are driven by data emitted from the data layer. All other navigation should use event-based navigation.

#### Event-based Navigation

Event-based navigation is ultimately the result of some "action" sent to a VM, whether that comes directly from a user-interaction with the current screen or from some internal action of the VM sent as the result of some asynchronous call. The VM will send an "event" to the Compose layer, which will trigger a callback that will eventually make a call to a [NavController](https://developer.android.com/reference/androidx/navigation/NavController).

#### Navigation Implementation

At its most basic level, Compose Navigation relies on constructing String-based URIs with a root path to specify a destination and both path and query parameters to handle the passing of required and optional parameters. This process can typically be very error prone. The Bitwarden app borrows a pattern best exemplified by the [Now In Android sample project](https://github.com/android/nowinandroid) in order to make this process more type-safe. This consists of:

- A type-safe `...Destination` extension function on [NavGraphBuilder](https://developer.android.com/reference/kotlin/androidx/navigation/NavGraphBuilder) for any single screen destination or a type-safe `...Graph` extension function on [NavGraphBuilder](https://developer.android.com/reference/kotlin/androidx/navigation/NavGraphBuilder) for any nested graph destination.
- A type-safe `navigateTo...` extension function on [NavController](https://developer.android.com/reference/androidx/navigation/NavController) for any destination that is not simply the root of a graph.
- A type-safe `...Args` class that pulls data from a `SavedStateHandle` for consumption by a VM.

These are all then grouped into a single `...Navigation.kt` file where all of the raw String-based details can be hidden and only the exposed type-safe functions must be used by the rest of the app.

#### Example

The following example demonstrates a sample `ExampleNavigation.kt` file with:

- An `ExampleArgs` class that wraps `SavedStateHandle` and allows for the `isToggleEnabledInitialValue` to be extracted in a type-safe way.
- A `NavGraphBuilder.exampleDestination` extension function that can be used to safely add the `ExampleScreen` to the navigation graph.
- A `NavController.navigateToExample` extension function that can be called to navigate to the `ExampleScreen` in a type-safe way.

Note in particular how consumers of the above **do not need to know the details of the actual route or path parameter**.

<details>
<summary>Show example</summary>

```kotlin
private const val IS_TOGGLE_ENABLED: String = "is_toggle_enabled"
private const val EXAMPLE_ROUTE_PREFIX = "example"
private const val EXAMPLE_ROUTE = "$EXAMPLE_ROUTE_PREFIX/{$IS_TOGGLE_ENABLED}"

data class ExampleArgs(
    val isToggleEnabledInitialValue: Boolean,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        isToggleEnabledInitialValue = checkNotNull(savedStateHandle[IS_TOGGLE_ENABLED]) as Boolean,
    )
}

fun NavGraphBuilder.exampleDestination(
    onNavigateToNextScreen: (CompletionData) -> Unit,
) {
    composableWithSlideTransitions(
        route = EXAMPLE_ROUTE,
        arguments = listOf(
            navArgument(IS_TOGGLE_ENABLED) { type = NavType.BoolType }
        ),
    ) {
        ExampleScreen(onNavigateToNextScreen = onNavigateToNextScreen)
    }
}

fun NavController.navigateToExample(
    isToggleEnabled: Boolean,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        "$EXAMPLE_ROUTE_PREFIX/$isToggleEnabled",
        navOptions,
    )
}
```

</details>
