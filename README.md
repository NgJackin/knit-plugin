# knit-plugin

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "knit-plugin"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/NgJackin/knit-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


## Current Features

### Inline Annotation
- Providers and Injections will be annotated inline in the editor and have their own gutter icons for easy identification.
- Injections will be annotated with the provider's class name and package so that the Provider can be easily located and verified.

### Automatic Detection of New Files and Classes
- Our plugin will automatically detect new files and classes, ensuring that DIs in the project is kept track of without any manual intervention.

### Circular Dependency Detection
- Provider and Injections will be stored and analysed to detect any circular dependencies which will cause runtime errors. 
- Providers / Injections involved in circular dependencies will be highlighted in the editor to alert the user.


## Planned Features for Future Releases

### Dependency Graph Visualization
- Visualize the dependency graph of Providers and Injections in the project.
- Allow users to interact with the graph to explore dependencies and identify potential issues.

### Refactoring Support
- Automatic refactoring for Providers and Injections when Providers are renamed or updated.
- Ensure that users will be prompted to remove all affected Injections when Providers are deleted.
- Other refactoring features to resolve common errors related to Providers and Injections (e.g. injecting a private provider in a different class).

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
