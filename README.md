# Knit Grass

## Table of Contents
- [Description](#description)
- [Installation](#build-and-installation)
- [Current Features](#current-features)
- [Planned Features for Future Releases](#planned-features-for-future-releases)
- [Tools and Libraries Used](#tools-and-libraries-used)

## Description
Knit Grass is a JetBrains IDE plugin that provides enhanced support for projects using the Knit Dependency Injection (DI) framework. It offers features such as inline warnings, refactoring support, and circular dependency detection to help developers manage their dependencies more effectively.

## Build and Installation

- **IMPORTANT: Disabling K2 mode**
1. Open Settings/Preferences: Go to File > Settings (on Windows/Linux) or IntelliJ IDEA > Preferences (on macOS).
2. Navigate to Kotlin settings: In the Settings/Preferences dialog, navigate to Languages & Frameworks > Kotlin.
3. Disable K2 mode: Locate the Enable K2 mode checkbox and deselect it.
4. Apply changes: Click Apply and then OK to save the changes and close the dialog.

- Build the Plugin using Gradle with the command: `./gradlew buildPlugin` from the project root directory.
- After building, the plugin ZIP file can be found in `build/distributions/`.
- Install the plugin manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd> and selecting the plugin's ZIP file (do not extract the ZIP).


## Current Features

### Gutter Icons and Inline Warnings
- Providers and Injections have their own gutter icons for easy identification.
- More information about Providers / Injections will be shown when hovering over the gutter icon.
  - Clicking a Producer's gutter icon will navigate to all Injections of that Provider.
  - Clicking an Injection's gutter icon will navigate to the corresponding Provider.
- Warnings will be have their own inline annotations to alert the user of potential issues.

### Refactoring Support
- Fixes for common DI issues (missing providers, unused providers)
- Automatically generates a placeholder provider in the case of missing providers.
- Allows for removal of annotations or deletes the class for unused providers.

### Circular Dependency Detection
- Provider and Injections will be stored and analysed to detect any circular dependencies which will cause runtime errors. 
- Providers / Injections involved in circular dependencies will have inline warnings in the editor to alert the user.

## Planned Features for Future Releases

### Support for all Knit annotations
- The current version only supports `@Provides` and `by di`.
- Support for more advanced usages, for annotations such as `@Singleton`, `@Component` etc.

### Dependency Graph Visualization
- Visualize the dependency graph of Providers and Injections in the project.
- Allow users to interact with the graph to explore dependencies and identify potential issues.

### Additional Refactoring Support
- **Quick fixes** for a wider variety of DI issues ( scope mismatches)
- **Auto-import** suggestions for Knit annotations
- **Extract to provider** refactoring for repeated dependency patterns
- **Bulk operations** for updating multiple providers at once



## Tools and Libraries Used
- [![IntelliJ IDEA][intellij-logo]][intellij-url]
- [![Kotlin][kotlin-logo]][kotlin-url]
- [![JetBrains Plugin DevKit][devkit-logo]][devkit-url]


[intellij-logo]: https://img.shields.io/badge/IntelliJIDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white
[intellij-url]: https://www.jetbrains.com/idea/

[kotlin-logo]: https://img.shields.io/badge/Kotlin-0095D5.svg?style=for-the-badge&logo=kotlin&logoColor=white
[kotlin-url]: https://kotlinlang.org/

[devkit-logo]: https://img.shields.io/badge/JetBrains%20Plugin%20DevKit-000000.svg?style=for-the-badge&logo=jetbrains&logoColor=white
[devkit-url]: https://plugins.jetbrains.com/docs/intellij/plugin-development.html

---

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
