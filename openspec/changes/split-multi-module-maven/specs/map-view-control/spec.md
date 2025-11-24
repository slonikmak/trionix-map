# map-view-control Spec Delta

## ADDED Requirements

### Requirement: Library Distribution Model
The `trionix-map-core` library artifact SHALL NOT include JavaFX dependencies transitively, allowing consumers to provide their own JavaFX version and platform classifier.

#### Scenario: Library artifact excludes JavaFX
- **WHEN** a consumer adds `trionix-map-core` as a Maven dependency
- **THEN** JavaFX artifacts are NOT pulled transitively and the consumer must declare their own JavaFX dependencies

#### Scenario: Library compiles with provided JavaFX
- **WHEN** the library module is built
- **THEN** JavaFX classes are available at compile time (via `provided` scope) but are not bundled in the resulting artifact
