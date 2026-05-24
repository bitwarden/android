# Style Patterns Quick Reference

Project-specific Kotlin style rules to catch during code review. These supplement (not replace) `docs/STYLE_AND_BEST_PRACTICES.md`.

## `when` branches with wrapped right-hand side require curly braces

When a `when` branch's expression is too long to fit on the same line as `->` and is wrapped to its own line, the body must be wrapped in `{ }`. A bare `->` followed by an indented expression on the next line should be flagged.

**Flag this:**

```kotlin
when (type) {
    VaultItemCipherType.LOGIN -> VaultAddEditState.ViewState.Content.ItemType.Login()
    VaultItemCipherType.BANK_ACCOUNT ->
        VaultAddEditState.ViewState.Content.ItemType.BankAccount()
}
```

**Accept this:**

```kotlin
when (type) {
    VaultItemCipherType.LOGIN -> VaultAddEditState.ViewState.Content.ItemType.Login()
    VaultItemCipherType.BANK_ACCOUNT -> {
        VaultAddEditState.ViewState.Content.ItemType.BankAccount()
    }
}
```

Single-line branches (body fits alongside `->`) do **not** require braces.

**Suggested classification:** SUGGESTED (style consistency, not correctness).
