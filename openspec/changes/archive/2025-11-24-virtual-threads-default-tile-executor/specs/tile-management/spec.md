# Tile Management – Virtual Thread Update

## MODIFIED Requirements

### Requirement: Thread Pool Management
The system SHALL use a shared executor for all tile loading operations, and the reference implementation SHALL create tasks on Java 21 virtual threads.

#### Scenario: Initialize shared virtual-thread executor
- **WHEN** the first `MapView` is created
- **THEN** the system builds a singleton executor using `Executors.newVirtualThreadPerTaskExecutor()` (or an equivalent virtual-thread-per-task implementation)
- **AND** the executor remains shared across all map instances for the life of the JVM

#### Scenario: Execute tile loads in background
- **WHEN** `loadTile()` is called
- **THEN** network and image decoding work executes on the shared virtual-thread executor, not on the JavaFX Application Thread

#### Scenario: Update UI on JavaFX thread
- **WHEN** a tile load completes (success or failure)
- **THEN** UI updates (cache writes that influence rendering, canvas redraws, layer layout requests) occur via the JavaFX Application Thread using `Platform.runLater()` or an equivalent mechanism

#### Scenario: Guard against non-FX callbacks
- **WHEN** a background tile task finishes on a virtual thread
- **THEN** `TileManager` SHALL marshal any `TileConsumer` callbacks back to the FX thread before they mutate scene graph state

#### Scenario: Tile retrievers execute off the FX thread
- **WHEN** `TileManager` invokes `TileRetriever.loadTile(...)`
- **THEN** the call SHALL occur on a virtual thread from the shared executor, never on the JavaFX Application Thread
- **SO THAT** blocking I/O inside retrievers cannot freeze UI input or animations
