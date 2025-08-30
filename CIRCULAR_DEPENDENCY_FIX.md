# Circular Dependency Detection - Fixed Logic

## Issue Fixed

The previous implementation incorrectly detected circular dependencies by treating ALL constructor parameters of `@Provides` classes as injection dependencies. This caused false positives.

## Example of False Positive (Now Fixed)

```kotlin
@Provides
class User(val name: String) // name is NOT an injection dependency

class UserService(
    @Provides val name: String // Provides String
) {
    val user: User by di // Needs User
}
```

**Before Fix**: Detected as circular `User -> String -> User` ❌
**After Fix**: No circular dependency detected ✅

## What Actually Constitutes a Dependency

### ✅ **Real Dependencies (Will be analyzed)**

1. **Properties using `by di`**:
   ```kotlin
   val service: SomeService by di  // This creates a dependency
   ```

### ❌ **NOT Dependencies (Will be ignored)**

1. **Constructor parameters** (unless they're also injected elsewhere):

   ```kotlin
   @Provides
   class User(val name: String)  // name is NOT a dependency
   ```

2. **Regular properties**:
   ```kotlin
   val regularProp: String = "value"  // Not a dependency
   ```

## Updated Detection Logic

The analyzer now only creates dependency edges for:

- Properties that use `by di` delegation
- This represents actual runtime injection dependencies
- Constructor parameters are treated as external inputs, not DI dependencies

## Test Cases

### ✅ **Valid Non-Circular** (Should not trigger warnings)

```kotlin
@Provides class User(val name: String)
class UserService(@Provides val name: String) {
    val user: User by di  // Linear dependency chain
}
```

### ❌ **Actual Circular** (Should trigger warnings)

```kotlin
@Provides class A {
    val b: B by di  // A depends on B
}
@Provides class B {
    val a: A by di  // B depends on A - CIRCULAR!
}
```

This fix ensures that only actual injection dependencies are considered for circular dependency analysis.
