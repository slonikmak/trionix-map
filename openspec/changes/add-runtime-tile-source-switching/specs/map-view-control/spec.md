## MODIFIED Requirements

### Requirement: MapView Component
The system SHALL provide a `MapView` JavaFX control (extending `Region`) that displays OpenStreetMap raster tiles with interactive pan and zoom capabilities.

#### Scenario: Instantiate MapView with defaults
- **WHEN** a developer creates a `MapView` instance using the default constructor
- **THEN** the component initializes with the built-in OpenStreetMap `TileSource`, in-memory LRU cache, center at (0.0, 0.0), and zoom level 1.0

#### Scenario: Instantiate MapView with custom configuration
- **WHEN** a developer creates a `MapView` instance with explicit `TileRetriever` and `TileCache` parameters
- **THEN** the component uses the provided retriever and cache implementations

## ADDED Requirements

### Requirement: Runtime Tile Source Configuration
`MapView` SHALL expose a high-level `TileSource` API for configuring the built-in raster tile pipeline without requiring applications to manage retriever internals directly.

#### Scenario: Read active tile source
- **WHEN** a developer calls `getTileSource()` on a default `MapView`
- **THEN** the method returns the current built-in `TileSource`
- **AND** the default value represents the OpenStreetMap standard tile server

#### Scenario: Observe tile source changes
- **WHEN** a developer binds or listens to `tileSourceProperty()`
- **THEN** the property reflects changes to the active built-in `TileSource`

#### Scenario: Replace tile source at runtime
- **WHEN** a developer calls `setTileSource(...)` with a different built-in `TileSource`
- **THEN** `MapView` switches subsequent tile requests to the new source
- **AND** initiates a refresh of visible tiles

#### Scenario: Use custom retriever mode
- **WHEN** a developer creates `MapView` with an explicit `TileRetriever`
- **THEN** custom retrieval remains the advanced extension path
- **AND** the built-in `TileSource` API is not required to reconfigure that custom retriever
