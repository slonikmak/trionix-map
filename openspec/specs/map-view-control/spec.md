# map-view-control Specification

## Purpose
TBD - created by archiving change add-javafx-osm-map-mvp. Update Purpose after archive.
## Requirements
### Requirement: MapView Component
The system SHALL provide a `MapView` JavaFX control (extending `Region`) that displays OpenStreetMap raster tiles with interactive pan and zoom capabilities.

#### Scenario: Instantiate MapView with defaults
- **WHEN** a developer creates a `MapView` instance using the default constructor
- **THEN** the component initializes with standard OSM tile retriever, in-memory LRU cache, center at (0.0, 0.0), and zoom level 1.0

#### Scenario: Instantiate MapView with custom configuration
- **WHEN** a developer creates a `MapView` instance with explicit `TileRetriever` and `TileCache` parameters
- **THEN** the component uses the provided retriever and cache implementations

### Requirement: Observable Center Coordinates
The system SHALL expose center latitude and longitude as observable JavaFX `DoubleProperty` instances.

#### Scenario: Read center coordinates
- **WHEN** a developer calls `getCenterLat()` and `getCenterLon()`
- **THEN** the current center coordinates are returned

#### Scenario: Set center coordinates programmatically
- **WHEN** a developer calls `setCenterLat(48.8566)` and `setCenterLon(2.3522)`
- **THEN** the map center moves to Paris and property listeners are notified

#### Scenario: Bind to center coordinates
- **WHEN** a developer binds UI elements to `centerLatProperty()` or `centerLonProperty()`
- **THEN** the bindings update automatically when the map is panned

### Requirement: Observable Zoom Level
The system SHALL expose zoom level as an observable JavaFX `DoubleProperty`.

#### Scenario: Read zoom level
- **WHEN** a developer calls `getZoom()`
- **THEN** the current zoom level is returned as a double

#### Scenario: Set zoom level programmatically
- **WHEN** a developer calls `setZoom(12.5)`
- **THEN** the map zooms to level 12.5 and property listeners are notified

#### Scenario: Bind to zoom level
- **WHEN** a developer binds UI elements to `zoomProperty()`
- **THEN** the bindings update automatically when the map is zoomed

### Requirement: Coordinate Range Constraints
The system SHALL enforce valid coordinate ranges for latitude, longitude, and zoom.

#### Scenario: Clamp latitude to Web Mercator bounds
- **WHEN** a developer sets `centerLat` to 90.0
- **THEN** the latitude is clamped to approximately 85.0 (Web Mercator limit)

#### Scenario: Wrap longitude at antimeridian
- **WHEN** a developer sets `centerLon` to 200.0
- **THEN** the longitude wraps to -160.0

#### Scenario: Enforce minimum zoom
- **WHEN** a developer sets `zoom` to -1.0
- **THEN** the zoom is clamped to 0.0

#### Scenario: Enforce maximum zoom
- **WHEN** a developer sets `zoom` to 25.0
- **THEN** the zoom is clamped to 19.0

### Requirement: Mouse Pan Interaction
The system SHALL support panning the map by dragging with the left mouse button.

#### Scenario: Pan map by dragging
- **WHEN** a user presses the left mouse button and drags 100 pixels east
- **THEN** the map center longitude decreases (map content moves east) and `centerLonProperty` updates

#### Scenario: Release after pan
- **WHEN** a user releases the left mouse button after dragging
- **THEN** the map stops panning at the current position

### Requirement: Mouse Zoom Interaction
The system SHALL support zooming the map with the mouse scroll wheel around the cursor position.

#### Scenario: Zoom in with scroll wheel
- **WHEN** a user scrolls the mouse wheel up by one notch with cursor at map center
- **THEN** the zoom level increases by approximately 0.5 and the map scales around the cursor position

#### Scenario: Zoom out with scroll wheel
- **WHEN** a user scrolls the mouse wheel down by one notch with cursor at map center
- **THEN** the zoom level decreases by approximately 0.5 and the map scales around the cursor position

### Requirement: Touch Zoom Interaction
The system SHALL support pinch-to-zoom gestures on platforms with touch/trackpad support.

#### Scenario: Pinch to zoom in
- **WHEN** a user performs a pinch-out gesture on a touch-enabled device
- **THEN** the zoom level increases around the gesture center

#### Scenario: Pinch to zoom out
- **WHEN** a user performs a pinch-in gesture on a touch-enabled device
- **THEN** the zoom level decreases around the gesture center

### Requirement: Layer Management
The system SHALL provide an observable list of `MapLayer` instances for managing custom overlays.

#### Scenario: Access empty layer list
- **WHEN** a developer calls `getLayers()` on a newly created `MapView`
- **THEN** an empty `ObservableList<MapLayer>` is returned

#### Scenario: Add layer to map
- **WHEN** a developer adds a custom `MapLayer` to `getLayers()`
- **THEN** the layer is rendered above the base tile layer and its `layerAdded(mapView)` method is called

#### Scenario: Remove layer from map
- **WHEN** a developer removes a `MapLayer` from `getLayers()`
- **THEN** the layer is no longer rendered and its `layerRemoved(mapView)` method is called

#### Scenario: Layer rendering order
- **WHEN** multiple layers exist in `getLayers()`
- **THEN** layers are rendered in list order (index 0 closest to tiles, higher indices on top)

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

### Requirement: Library Distribution Model
The `trionix-map-core` library artifact SHALL NOT include JavaFX dependencies transitively, allowing consumers to provide their own JavaFX version and platform classifier.

#### Scenario: Library artifact excludes JavaFX
- **WHEN** a consumer adds `trionix-map-core` as a Maven dependency
- **THEN** JavaFX artifacts are NOT pulled transitively and the consumer must declare their own JavaFX dependencies

#### Scenario: Library compiles with provided JavaFX
- **WHEN** the library module is built
- **THEN** JavaFX classes are available at compile time (via `provided` scope) but are not bundled in the resulting artifact

