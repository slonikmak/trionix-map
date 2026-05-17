## ADDED Requirements

### Requirement: TileSource Value Object
The system SHALL provide a public immutable `TileSource` type describing the built-in HTTP tile source configuration.

#### Scenario: Create default OpenStreetMap source
- **WHEN** a developer requests the default built-in tile source
- **THEN** the returned `TileSource` uses base URL `https://tile.openstreetmap.org/`, the library default user-agent, and the library default connect and read timeouts

#### Scenario: Create custom built-in source
- **WHEN** a developer constructs a `TileSource` with custom HTTP configuration
- **THEN** the resulting value object preserves the provided base URL, user-agent, connect timeout, and read timeout

### Requirement: Built-In Retriever Uses Active TileSource
The system SHALL allow the built-in HTTP tile retriever to read the current `TileSource` dynamically at request time.

#### Scenario: Load tile from active source
- **WHEN** the built-in retriever loads a tile after `MapView` has an active `TileSource`
- **THEN** the HTTP request uses the active source's base URL, user-agent, connect timeout, and read timeout

#### Scenario: Switch source before next request
- **WHEN** the active `TileSource` changes before a missing visible tile is requested
- **THEN** the next tile request uses the new source rather than the previous one

### Requirement: Tile Source Switch Invalidation
The system SHALL prevent results from a previous tile source from being cached or rendered after a runtime source change.

#### Scenario: Drop stale completion from previous source
- **WHEN** a tile request started under tile source A completes after `MapView` has switched to tile source B
- **THEN** the completed image is ignored
- **AND** it is not written to the cache
- **AND** it does not trigger a redraw for tile source B

#### Scenario: Clear cache on source switch
- **WHEN** `MapView` switches from one `TileSource` to another
- **THEN** the shared tile cache for that map instance is cleared before loading tiles from the new source
