# implementing-android-code

Bitwarden Android implementation patterns skill for Claude Code. Provides critical patterns, gotchas, anti-patterns, and copy-pasteable templates unique to the Bitwarden Android codebase.

## Features

- **ViewModel SAE Pattern** - State-Action-Event with `BaseViewModel`, `SavedStateHandle` persistence, process death recovery
- **Type-Safe Navigation** - `@Serializable` routes, `composableWithSlideTransitions`, `NavGraphBuilder`/`NavController` extensions
- **Screen/Compose** - Stateless composables, `EventsEffect`, `remember(viewModel)` lambda patterns
- **Data Layer** - Repository pattern, `DataState<T>` streaming, `Result` sealed classes, Flow collection via Internal actions
- **UI Components** - Bitwarden component library usage, theming, string resources
- **Security Patterns** - Zero-knowledge architecture, encrypted storage, SDK isolation
- **Testing Patterns** - ViewModel, repository, compose, and data source test structure
- **Clock/Time Handling** - `Clock` injection for deterministic time operations

## Skill Structure

```
implementing-android-code/
├── SKILL.md          # Quick reference for patterns, gotchas, and anti-patterns
├── templates.md      # Copy-pasteable code templates for all layer types
├── README.md         # This file
├── CHANGELOG.md      # Version history
└── CONTRIBUTING.md   # Contribution guidelines
```

## Usage

Claude triggers this skill automatically when conversations involve implementing features, screens, ViewModels, data sources, or navigation in the Bitwarden Android app.

**Example trigger phrases:**
- "How do I implement a ViewModel?"
- "Create a new screen"
- "Add navigation"
- "Write a repository"
- "BaseViewModel pattern"
- "State-Action-Event"
- "type-safe navigation"
- "Clock injection"

## Content Summary

| Section | Description |
|---------|-------------|
| A. ViewModel Implementation | SAE pattern, `handleAction`, `sendAction`, `SavedStateHandle` |
| B. Type-Safe Navigation | `@Serializable` routes, transitions, `NavGraphBuilder` extensions |
| C. Screen Implementation | Stateless composables, `EventsEffect`, action lambdas |
| D. Data Layer | Repositories, data sources, `DataState`, error handling |
| E. UI Components | Bitwarden component library, theming, string resources |
| F. Security Patterns | Zero-knowledge, encrypted storage, SDK isolation |
| G. Testing Quick Reference | ViewModel, repository, compose, data source tests |
| H. Clock/Time Patterns | `Clock` injection, deterministic time testing |

## References

- [`docs/ARCHITECTURE.md`](../../../docs/ARCHITECTURE.md) - Comprehensive architecture patterns and examples
- [`docs/STYLE_AND_BEST_PRACTICES.md`](../../../docs/STYLE_AND_BEST_PRACTICES.md) - Code style, formatting, Compose conventions

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines, versioning, and pull request requirements.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history.

## License

This skill is part of the [Bitwarden Android](https://github.com/bitwarden/android) project and follows its licensing terms.

## Maintainers

- Bitwarden Android team

## Support

For issues or questions, open an issue in the [bitwarden/android](https://github.com/bitwarden/android) repository.