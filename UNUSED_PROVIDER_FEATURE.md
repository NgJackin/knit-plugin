# Unused Provider Detection Feature

## Overview
This feature automatically detects `@Provides` annotations that are not being used anywhere in the project and offers quick fixes to clean them up.

## What Gets Detected

### ✅ **Unused Providers** (Will be flagged)
1. **Classes with @Provides** that are never injected:
   ```kotlin
   @Provides
   class UnusedService  // No 'by di' usages found
   ```

2. **Constructor parameters with @Provides** that are never injected:
   ```kotlin
   class SomeService(
       @Provides val unusedConfig: String  // No 'by di' usages found
   )
   ```

3. **Properties with @Provides** that are never injected:
   ```kotlin
   @Provides
   val unusedProperty: Database = Database()  // No 'by di' usages found
   ```

### ❌ **Used Providers** (Will NOT be flagged)
```kotlin
@Provides
class UserService  // Used below

class Controller {
    val userService: UserService by di  // Uses UserService - NOT flagged
}
```

## Quick Fix Options

When you click on an unused provider warning, you get **two quick fix options**:

### 1. **Remove @Provides annotation**
- Removes only the `@Provides` annotation
- Keeps the class/property/parameter intact
- Use when you want to keep the code but remove DI capability

### 2. **Remove entire provider**
- Removes the entire class/property/parameter
- Use when the provider is completely unnecessary

## Example Usage

```kotlin
// BEFORE: Plugin detects unused providers
@Provides
class UnusedService {  // ← Warning: "Unused provider: No injection found for 'UnusedService'"
    fun doSomething() = "unused"
}

class SomeClass(
    @Provides val unusedParam: Int  // ← Warning: "Unused provider: No injection found for parameter type 'Int'"
) {
    @Provides
    val unusedProp: String = "test"  // ← Warning: "Unused provider: No injection found for property type 'String'"
}

// AFTER: Quick fixes applied
// Option 1: Remove just @Provides annotations
class UnusedService {  // @Provides removed, class kept
    fun doSomething() = "unused"
}

class SomeClass(
    val unusedParam: Int  // @Provides removed, parameter kept
) {
    val unusedProp: String = "test"  // @Provides removed, property kept
}

// Option 2: Remove entire providers
// (Entire UnusedService class, unusedParam, and unusedProp would be deleted)
```

## Integration

- **Inspection Level**: Warning (shows in Problems panel)
- **Highlight**: Unused symbol styling (grayed out)
- **Quick Fixes**: Available via Alt+Enter or lightbulb icon
- **Real-time**: Updates as you add/remove injections

## Benefits

1. **Code Cleanup**: Easily identify and remove dead DI code
2. **Maintenance**: Keep dependency injection clean and minimal
3. **Performance**: Reduce unnecessary provider registrations
4. **Clarity**: Focus on providers that are actually being used

This feature helps maintain a clean and efficient dependency injection setup by automatically identifying unused providers and offering convenient cleanup options.
