---
name: core-conventions
description: Code style, naming conventions, and anti-patterns for Bitwarden Android. Use when checking code style, naming rules, string resources, formatting, KDoc, or reviewing for anti-patterns. Triggered by "code style", "naming", "conventions", "formatting", "string resources", "KDoc", "anti-pattern", "style guide".
---

# Core Conventions

## Code Style

- **Formatter**: Android Studio with `bitwarden-style.xml`
- **Line Limit**: 100 characters
- **Detekt**: Enabled for static analysis

> Full rules: `docs/STYLE_AND_BEST_PRACTICES.md`

---

## Naming Conventions

| Context | Convention | Example |
|---------|-----------|---------|
| Variables, functions | `camelCase` | `vaultItems`, `getUserState()` |
| Classes, interfaces | `PascalCase` | `VaultRepository`, `AuthDiskSource` |
| Constants | `SCREAMING_SNAKE_CASE` | `MAX_RETRY_COUNT` |
| Implementations | `...Impl` suffix | `VaultRepositoryImpl` |

---

## String Resources

- Add new strings to `:ui` module: `ui/src/main/res/values/strings.xml`
- Use **typographic quotes/apostrophes**: `\u201c` `\u201d` `\u2019` (not escaped ASCII `\"` `\'`)
- Search existing strings before adding new ones to avoid duplicates

---

## KDoc Requirements

- **Required** for all public APIs (classes, functions, properties)
- Include `@param`, `@return`, `@throws` where applicable
- Keep descriptions concise and focused on behavior, not implementation

---

## DO

- Use `remember(viewModel)` for lambdas passed to composables
- Map async results to internal actions before updating state
- Inject `Clock` for time-dependent operations
- Return early to reduce nesting
- Use interface/`...Impl` pairs with Hilt injection

## DON'T

- Update state directly inside coroutines (use internal actions)
- Use `any` types or suppress null safety
- Catch generic `Exception` (catch specific types)
- Use `e.printStackTrace()` (use Timber logging)
- Create new patterns when established ones exist
- Skip KDoc for public APIs
- Throw exceptions from the data layer (return `Result<T>` or sealed classes)

---

## Import Ordering

Follow the project's configured import ordering in `bitwarden-style.xml`. Android Studio will auto-sort when properly configured.

> For complete style rules including Compose conventions, see `docs/STYLE_AND_BEST_PRACTICES.md`.
