# Map View Control – Java 21 Baseline

## ADDED Requirements

### Requirement: Java 21 Runtime Baseline
`MapView` and its supporting infrastructure SHALL require Java 21 or newer so that virtual threads and JavaFX 21 APIs are available.

#### Scenario: Build-time enforcement
- **WHEN** the library is compiled or unit tests are executed
- **THEN** the toolchain SHALL target Java 21 bytecode (or higher) and fail if an older runtime is used

#### Scenario: Runtime expectation
- **WHEN** an application instantiates `MapView`
- **THEN** the application SHALL run on a Java 21+ runtime that supports virtual threads and JavaFX 21 APIs

### Requirement: JavaFX Thread Coordination
`MapView` SHALL guarantee that all scene-graph mutations run on the JavaFX Application Thread while background work executes on non-FX threads (virtual or platform).

#### Scenario: UI updates stay on FX thread
- **WHEN** `MapView` mutates its internal nodes (tile canvas, layer pane, gesture handlers)
- **THEN** the operations SHALL occur on the JavaFX Application Thread, even if the originating call came from a background thread (for example, `flyTo` or tile refresh)

#### Scenario: Background work never blocks FX thread
- **WHEN** `MapView` schedules tile refreshes, placeholder painting, or cache pruning that involve I/O or CPU-heavy work
- **THEN** these tasks SHALL execute on background threads (such as the virtual-thread executor) and only marshal minimal state changes back to the FX thread
