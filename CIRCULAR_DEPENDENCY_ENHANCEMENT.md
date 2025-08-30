# Circular Dependency Detection Enhancement

## Overview

This enhancement adds comprehensive circular dependency detection to the Knit dependency injection plugin. The plugin now warns users when circular dependencies are detected in their dependency injection setup.

## New Features Added

### 1. Circular Dependency Analyzer (`CircularDependencyAnalyzer.kt`)

- **Purpose**: Analyzes the entire project's dependency graph to detect circular dependencies
- **Key Methods**:
  - `findCircularDependencies(project)`: Finds all circular dependencies in the project
  - `isPartOfCircularDependency(element)`: Checks if a specific element is part of any circular dependency
  - `getCircularDependenciesForType(type, project)`: Gets circular dependencies affecting a specific type

### 2. Circular Dependency Inspection (`KnitCircularDependencyInspection.kt`)

- **Purpose**: Provides IDE inspections that highlight circular dependencies as errors
- **Features**:
  - Registers problems for classes with `@Provides` annotation that are part of circular dependencies
  - Warns about injected properties that participate in circular dependencies
  - Shows detailed cycle information in error messages

### 3. Enhanced Annotator (`KnitAnnotator.kt`)

- **Purpose**: Visual highlighting of circular dependencies in the editor
- **Features**:
  - Normal DI elements get standard highlighting
  - Elements involved in circular dependencies get special red highlighting with bold text
  - Custom text attributes for circular dependency warnings

### 4. Enhanced Inlay Hints (`KnitInlayHintsCollector.kt`)

- **Purpose**: Inline hints that immediately show circular dependency warnings
- **Features**:
  - Shows ⚠️ warning icons for elements in circular dependencies
  - Enhanced hints like "⚠️ Producer (Circular!)" and "⚠️ Circular dependency!"

### 5. Circular Dependency Service (`CircularDependencyService.kt`)

- **Purpose**: Caching and performance optimization for circular dependency detection
- **Features**:
  - Caches analysis results to avoid repeated computation
  - Provides statistics about circular dependencies
  - Cache invalidation when files change

## How It Works

### Detection Algorithm

1. **Graph Building**: The analyzer scans all Kotlin files in the project to build a dependency graph
2. **Node Creation**: Each `@Provides` class/parameter becomes a node in the graph
3. **Edge Creation**: Dependencies (via constructor parameters and `by di` injections) become edges
4. **Cycle Detection**: Uses depth-first search (DFS) to detect cycles in the graph

### Example Circular Dependencies Detected

```kotlin
// Simple A -> B -> A cycle
@Provides
class ServiceA {
    val serviceB: ServiceB by di // A depends on B
}

@Provides
class ServiceB {
    val serviceA: ServiceA by di // B depends on A - CIRCULAR!
}

// Complex A -> B -> C -> A cycle
@Provides
class ComponentA {
    val componentB: ComponentB by di // A depends on B
}

@Provides
class ComponentB {
    val componentC: ComponentC by di // B depends on C
}

@Provides
class ComponentC {
    val componentA: ComponentA by di // C depends on A - CIRCULAR!
}

// Self-dependency
@Provides
class RecursiveService {
    val self: RecursiveService by di // Self-dependency - CIRCULAR!
}
```

## Visual Indicators

### In Editor

- **Normal Elements**: Standard syntax highlighting
- **Circular Dependencies**: Red background, bold dark red text
- **Inlay Hints**: ⚠️ warning icons with descriptive messages

### In Problems Panel

- **Error Level**: Circular dependencies appear as errors
- **Warning Level**: Properties in circular dependencies appear as warnings
- **Detailed Messages**: Show the full dependency cycle (e.g., "A → B → C → A")

### In Tool Window

- Updated to mention circular dependency detection feature
- Legend includes ⚠️ symbol for circular dependencies

## Benefits

1. **Early Detection**: Catch circular dependencies at development time, not runtime
2. **Visual Feedback**: Immediate visual cues in the editor
3. **Detailed Information**: Shows the complete dependency cycle
4. **Performance Optimized**: Uses caching to avoid repeated analysis
5. **IDE Integration**: Works with all standard IntelliJ inspection features

## Testing

The `CircularDependencyExample.kt` file contains various examples of circular dependencies that the plugin will detect:

- Simple two-way circular dependencies
- Complex multi-step circular dependencies
- Self-referencing dependencies
- Non-circular dependencies for comparison

## Future Enhancements

Potential future improvements could include:

- Quick fixes to break circular dependencies
- Dependency graph visualization
- Suggestion of alternative architectures
- Integration with build tools to fail builds on circular dependencies
