# UI Refinement Review Checklist

## Multi-Pass Strategy

### First Pass: Visual Changes

**1. Understand the changes:**
- What visual/UX problem is being solved?
- Are there designs or screenshots to reference?
- Is this a bug fix or enhancement?

**2. Component usage:**
- Using existing components from `:ui` module?
- Any new custom components created?
- Could existing components be reused?

### Second Pass: Implementation Review

**3. Compose best practices:**
- Composables properly structured?
- State hoisted correctly?
- Preview composables included?

**4. Accessibility:**
- Content descriptions for images/icons?
- Semantic properties for screen readers?
- Touch targets meet minimum size (48dp)?

**5. Design consistency:**
- Using theme colors, spacing, typography?
- Consistent with other screens?
- Responsive to different screen sizes?

## What to CHECK

✅ **Compose Best Practices**
- Composables are stateless where possible
- State hoisting follows patterns
- Side effects (LaunchedEffect, DisposableEffect) used correctly
- Preview composables provided for development

✅ **Component Reuse**
- Using existing BitwardenButton, BitwardenTextField, etc.?
- Could custom UI be replaced with existing components?
- New reusable components placed in `:ui` module?

✅ **Accessibility**
- `contentDescription` for icons and images
- `semantics` for custom interactions
- Sufficient contrast ratios
- Touch targets ≥ 48dp minimum

✅ **Design Consistency**
- Using `BitwardenTheme` colors (not hardcoded)
- Using `BitwardenTheme` spacing (16.dp, 8.dp, etc.)
- Using `BitwardenTheme` typography styles
- Consistent with existing screen patterns

✅ **Responsive Design**
- Handles different screen sizes?
- Scrollable content where appropriate?
- Landscape orientation considered?

## What to SKIP

❌ **Deep Architecture Review** - Unless ViewModel changes are substantial
❌ **Business Logic Review** - Focus is on presentation, not logic
❌ **Security Review** - Unless UI exposes sensitive data improperly

## Red Flags

🚩 **Duplicating existing components** - Should reuse from `:ui` module
🚩 **Hardcoded colors/dimensions** - Should use theme
🚩 **Missing accessibility properties** - Critical for screen readers
🚩 **State management in UI** - Should be hoisted to ViewModel

## Key Questions to Ask

Use `reference/review-psychology.md` for phrasing:

- "Can we use BitwardenButton here instead of this custom button?"
- "Should this color come from BitwardenTheme instead of being hardcoded?"
- "How will this look on a small screen?"
- "Is there a contentDescription for this icon?"

## Common Patterns

### Composable Structure

```kotlin
// ✅ GOOD - Stateless, hoisted state
@Composable
fun FeatureScreen(
    state: FeatureState,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // UI rendering only
}

// ❌ BAD - Business state in composable
@Composable
fun FeatureScreen() {
    var userData by remember { mutableStateOf<User?>(null) }  // Business state should be in ViewModel
    var isLoading by remember { mutableStateOf(false) }  // App state should be in ViewModel
    // ...
}

// ✅ OK - UI-local state in composable
@Composable
fun LoginForm(onSubmit: (String, String) -> Unit) {
    var username by remember { mutableStateOf("") }  // UI-local input state is fine
    var password by remember { mutableStateOf("") }
    // Hoist only as high as needed
}
```

### Theme Usage

```kotlin
// ✅ GOOD - Using theme
Text(
    text = "Title",
    style = BitwardenTheme.typography.titleLarge,
    color = BitwardenTheme.colorScheme.primary
)

// Design system uses 4.dp increments (4, 8, 12, 16, 24, 32, etc.)
Spacer(modifier = Modifier.height(16.dp))

// ❌ BAD - Hardcoded
Text(
    text = "Title",
    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),  // Should use theme
    color = Color(0xFF0000FF)  // Should use theme color
)

Spacer(modifier = Modifier.height(17.dp))  // Non-standard spacing
```

### Accessibility

```kotlin
// ✅ GOOD - Interactive element with description
Icon(
    painter = painterResource(R.drawable.ic_password),
    contentDescription = "Password visibility toggle",
    modifier = Modifier.clickable { onToggle() }
)

// ✅ GOOD - Decorative icon with explicit null
Icon(
    painter = painterResource(R.drawable.ic_check),
    contentDescription = null,  // Decorative icon next to descriptive text
    tint = BitwardenTheme.colorScheme.success
)

// ❌ BAD - Interactive element missing description
Icon(
    painter = painterResource(R.drawable.ic_delete),
    contentDescription = null,  // Interactive elements need descriptions
    modifier = Modifier.clickable { onDelete() }
)
```

## Prioritizing Findings

Use `reference/priority-framework.md` to classify findings as Critical/Important/Suggested/Optional.

## Output Format

See `examples/review-outputs.md` for the required output format and inline comment structure.

## Example Review

```markdown
## Summary
Updates login screen layout for improved visual hierarchy and touch targets

## Critical Issues
None

## Suggested Improvements

**app/auth/LoginScreen.kt:67** - Can we use BitwardenTextField?
This custom text field looks very similar to `ui/components/BitwardenTextField.kt:89`.
Would using the existing component maintain consistency?

**app/auth/LoginScreen.kt:123** - Add contentDescription
```kotlin
Icon(
    painter = painterResource(R.drawable.ic_visibility),
    contentDescription = "Show password",  // Add for accessibility
    modifier = Modifier.clickable { onToggleVisibility() }
)
```

**app/auth/LoginScreen.kt:145** - Use design system spacing
```kotlin
// Current
Spacer(modifier = Modifier.height(17.dp))

// Design system uses 4.dp increments (4, 8, 12, 16, 24, 32, etc.)
Spacer(modifier = Modifier.height(16.dp))
```
```
