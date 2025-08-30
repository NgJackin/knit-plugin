# knit-plugin

## Table of Contents
- [Description](#description)
- [Installation](#build-and-installation)
- [Current Features](#current-features)
- [Planned Features for Future Releases](#planned-features-for-future-releases)
- [Tools and Libraries Used](#tools-and-libraries-used)

## Description
Knit-Plugin is a JetBrains IDE plugin that provides enhanced support for projects using the Knit Dependency Injection (DI) framework. It offers features such as inline warnings, automatic detection of new files and classes, and circular dependency detection to help developers manage their dependencies more effectively.

## Build and Installation

- Build the Plugin using Gradle with the command: `./gradlew buildPlugin` from the project root directory.
- After building, the plugin ZIP file can be found in `build/distributions/`.

- Install the plugin manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd> and selecting the plugin's ZIP file (do not extract the ZIP).

### Running without Building

- Click on the Run Plugin Button on the top right of the IDE window to launch a new instance of the IDE with the plugin enabled.


## Current Features

### Gutter Icons and Inline Warnings
- Providers and Injections have their own gutter icons for easy identification.
- More information about Providers / Injections will be shown when hovering over the gutter icon.
  - Clicking a Producer's gutter icon will navigate to all Injections of that Provider.
  - Clicking an Injection's gutter icon will navigate to the corresponding Provider.
- Warnings will be have their own inline annotations to alert the user of potential issues.

### Automatic Detection of New Files and Classes
- Our plugin will automatically detect new files and classes, ensuring that DIs in the project is kept track of without any manual intervention.

### Circular Dependency Detection
- Provider and Injections will be stored and analysed to detect any circular dependencies which will cause runtime errors. 
- Providers / Injections involved in circular dependencies will have inline warnings in the editor to alert the user.

## Planned Features for Future Releases

### Dependency Graph Visualization
- Visualize the dependency graph of Providers and Injections in the project.
- Allow users to interact with the graph to explore dependencies and identify potential issues.

### Refactoring Support
- Automatic refactoring for Providers and Injections when Providers are renamed or updated.
- Ensure that users will be prompted to remove all affected Injections when Providers are deleted.
- Other refactoring features to resolve common errors related to Providers and Injections (e.g. injecting a private provider in a different class).


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
