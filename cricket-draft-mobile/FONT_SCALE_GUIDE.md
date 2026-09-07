# 📱 Font Scaling Guide — Cricket Draft Mobile

## Problem (Before)
Every screen hardcoded `fontSize = XX.sp` directly. Many values were way too large for mobile (28.sp, 36.sp). Font sizes were inconsistent across the app and didn't scale with screen size.

## Solution (After)
Two-layer responsive system:
1. **Material3 Typography** (`Type.kt`) — sets base text styles
2. **ScreenConfig** (`ScreenSize.kt`) — responsive text sizes that scale per device

---

## 🎯 Quick Reference — Which Size to Use

| Use Case | Property | Small (320dp) | Medium (360dp) | Large (400dp) | Tablet (500dp+) |
|----------|----------|:---:|:---:|:---:|:---:|
| App logo / hero numbers | `screenConfig.displayTextSize` | 24.sp | 28.sp | 32.sp | 36.sp |
| Score numbers (live match) | `screenConfig.scoreTextSize` | 20.sp | 22.sp | 26.sp | 30.sp |
| Screen titles / section headers | `screenConfig.headingTextSize` | 15.sp | 17.sp | 19.sp | 21.sp |
| Card titles / list titles | `screenConfig.subheadingTextSize` | 13.sp | 14.sp | 15.sp | 16.sp |
| Tab titles / toolbar | `screenConfig.titleTextSize` | 14.sp | 16.sp | 18.sp | 20.sp |
| Body text / descriptions | `screenConfig.bodyTextSize` | 12.sp | 13.sp | 14.sp | 15.sp |
| Buttons / chips | `screenConfig.buttonTextSize` | 11.sp | 12.sp | 13.sp | 14.sp |
| Captions / timestamps | `screenConfig.captionTextSize` | 10.sp | 11.sp | 12.sp | 12.sp |

---

## ✅ Correct Usage

```kotlin
// ✅ GOOD — responsive, scales with screen
import com.devwithguru.cricket.ui.theme.screenConfig

Text("STUMPS", fontSize = screenConfig.headingTextSize, fontWeight = FontWeight.Bold)
Text("128", fontSize = screenConfig.scoreTextSize, fontWeight = FontWeight.Bold)
Text("Last updated 2m ago", fontSize = screenConfig.captionTextSize)
Button(onClick = {}) { Text("Create Match", fontSize = screenConfig.buttonTextSize) }
```

```kotlin
// ✅ ALSO GOOD — use Material3 typography style directly
Text("Hello", style = MaterialTheme.typography.bodyLarge)
Text("Title", style = MaterialTheme.typography.titleLarge)
Text("Label", style = MaterialTheme.typography.labelMedium)
```

## ❌ Wrong Usage

```kotlin
// ❌ BAD — hardcoded size, doesn't scale, often too large
Text("STUMPS", fontSize = 28.sp, fontWeight = FontWeight.Bold)
Text("Score", fontSize = 36.sp)
Text("Button", fontSize = 20.sp)
```

---

## 📐 Material3 Typography Scale (Type.kt)

When you DON'T specify `fontSize`, Text() uses these defaults:

| Style | Size | Weight | Use For |
|-------|------|--------|---------|
| `displayLarge` | 34.sp | Bold | Hero numbers (sparingly) |
| `displayMedium` | 28.sp | Bold | Large scores |
| `displaySmall` | 22.sp | Bold | Medium displays |
| `headlineLarge` | 20.sp | Bold | Section headers |
| `headlineMedium` | 17.sp | SemiBold | Sub-headers |
| `headlineSmall` | 15.sp | SemiBold | Card headers |
| `titleLarge` | 15.sp | SemiBold | List item titles |
| `titleMedium` | 13.sp | Medium | Secondary titles |
| `titleSmall` | 12.sp | Medium | Small titles |
| `bodyLarge` | 14.sp | Normal | Main text |
| `bodyMedium` | 13.sp | Normal | Secondary text |
| `bodySmall` | 11.sp | Normal | Fine print |
| `labelLarge` | 13.sp | Medium | Button text |
| `labelMedium` | 11.sp | Medium | Chip/badge text |
| `labelSmall` | 10.sp | Medium | Tiny labels |

---

## 🔧 ScreenConfig Text Properties

Access via `screenConfig` (composable):

```kotlin
val screenConfig: ScreenConfig  // @Composable accessor
```

Properties:
- `screenConfig.displayTextSize` — hero numbers, scores, big counters
- `screenConfig.scoreTextSize` — live match scores (slightly smaller than display)
- `screenConfig.headingTextSize` — screen titles, section headers
- `screenConfig.subheadingTextSize` — card titles, list item titles
- `screenConfig.titleTextSize` — tab titles, toolbar titles
- `screenConfig.bodyTextSize` — main readable text, descriptions
- `screenConfig.buttonTextSize` — action buttons, chips
- `screenConfig.captionTextSize` — timestamps, helper text, fine print

---

## 📋 Rules

1. **NEVER hardcode `fontSize = XX.sp`** — always use `screenConfig.xxxTextSize` or `MaterialTheme.typography.xxx`
2. **Pick the right semantic size** — don't use `displayTextSize` for body text
3. **For Material3 TextStyles**, prefer `style = MaterialTheme.typography.bodyLarge` over manual `fontSize`
4. **Exception**: Score numbers in live match can use `scoreTextSize` directly for maximum visibility
5. **Exception**: Very small labels (10.sp max) can use hardcoded `10.sp` for absolute minimum

---

## 🗂 Files

| File | Purpose |
|------|---------|
| `ui/theme/Type.kt` | Material3 Typography definitions |
| `ui/theme/ScreenSize.kt` | ScreenConfig with responsive text sizes |
| `FONT_SCALE_GUIDE.md` | This guide |

---

## Migration Checklist (for existing screens)

- [ ] Search for `fontSize = 2[0-9].sp` and `fontSize = 3[0-9].sp` — these are likely wrong
- [ ] Replace with appropriate `screenConfig.xxxTextSize`
- [ ] Add `import com.devwithguru.cricket.ui.theme.screenConfig` if missing
- [ ] Verify the screen looks good on Small (320dp) and Large (400dp+) devices
