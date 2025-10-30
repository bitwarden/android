# UI Refinement Review Checklist

**Review Depth**: Design-focused (Compose patterns, accessibility, design system compliance)
**Risk Level**: MEDIUM

## Inline Comment Requirement

Create separate inline comment for EACH specific issue on the exact line (`file:line_number`).
Do NOT create one large summary comment. Do NOT update existing comments.
After inline comments, provide one summary comment.

---

## Multi-Pass Strategy

### First Pass: Visual Changes

<thinking>
Analyze the UI changes:
1. What visual/UX problem is being solved?
2. Are there designs or screenshots to reference?
3. Is this affecting existing screens or new ones?
4. What's the scope of visual changes?
5. Are design tokens (colors, spacing, typography) being used correctly?
</thinking>

**1. Understand the changes:**
- What visual/UX problem is being solved?
- Are there designs or screenshots to reference?
- Is this a bug fix or enhancement?

**2. Component usage:**
- Using existing components from `:ui` module?
- Any new custom components created?
- Could existing components be reused?

### Second Pass: Implementation Review

<thinking>
Check implementation quality:
1. Are Compose best practices followed?
2. Is state hoisting applied correctly?
3. Are existing components reused where possible?
4. Is accessibility properly handled?
5. Does this follow design system patterns?
</thinking>

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

// ❌ BAD - Stateful, no hoisting
@Composable
fun FeatureScreen() {
    var text by remember { mutableStateOf("") }  // State should be in ViewModel
    // ...
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

Spacer(modifier = Modifier.height(16.dp))  // Standard spacing

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
// ✅ GOOD - Accessible
Icon(
    painter = painterResource(R.drawable.ic_password),
    contentDescription = "Password visibility toggle",
    modifier = Modifier.clickable { onToggle() }
)

// ❌ BAD - Not accessible
Icon(
    painter = painterResource(R.drawable.ic_password),
    contentDescription = null,  // Screen readers can't announce this
    modifier = Modifier.clickable { onToggle() }
)
```

## Prioritizing Findings

Use `reference/priority-framework.md` to classify findings as Critical/Important/Suggested/Optional.

## Output Format

```markdown
## Summary
[Brief description of UI changes]

## Critical Issues
[Any blocking UI/accessibility issues]

## Suggested Improvements

**[file:line]** - Use existing BitwardenButton component
This custom button duplicates functionality in `ui/components/BitwardenButton.kt:45`.
Using the existing component maintains consistency across the app.

**[file:line]** - Add contentDescription for accessibility
```kotlin
Icon(
    painter = painterResource(R.drawable.ic_close),
    contentDescription = "Close dialog",  // Screen readers need this
    modifier = modifier
)
```

**[file:line]** - Use theme color instead of hardcoded
```kotlin
// Current
color = Color(0xFF0066FF)

// Should be
color = BitwardenTheme.colorScheme.primary
```

## Good Practices
[List 2-3 if applicable]
- Uses BitwardenTheme throughout
- Preview composables included

## Action Items
1. Replace custom button with BitwardenButton
2. Add contentDescription for icon
3. Use theme color instead of hardcoded value
```

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

**app/auth/LoginScreen.kt:145** - Use theme spacing
```kotlin
// Current
Spacer(modifier = Modifier.height(17.dp))

// Standard theme spacing
Spacer(modifier = Modifier.height(16.dp))
```

## Good Practices
- Proper state hoisting to ViewModel
- Preview composables included
- Responsive layout with ScrollableColumn

## Action Items
1. Evaluate using BitwardenTextField for consistency
2. Add contentDescription for visibility icon
3. Use standard 16.dp spacing
```
