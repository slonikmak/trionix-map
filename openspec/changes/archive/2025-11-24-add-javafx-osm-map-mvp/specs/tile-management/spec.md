# Tile Management

## ADDED Requirements

### Requirement: TileRetriever Interface
The system SHALL define a `TileRetriever` interface for asynchronous tile loading.

#### Scenario: Define retriever contract
- **WHEN** a developer implements `TileRetriever`
- **THEN** the interface requires a `loadTile(int zoom, long x, long y)` method returning `CompletableFuture<Image>`

#### Scenario: Non-blocking tile load
- **WHEN** `loadTile()` is called on any implementation
- **THEN** the method returns immediately without blocking the JavaFX Application Thread

### Requirement: SimpleOsmTileRetriever Implementation
The system SHALL provide a standard OSM tile retriever using HTTP.

#### Scenario: Create retriever with defaults
- **WHEN** a developer instantiates `SimpleOsmTileRetriever()` with no arguments
- **THEN** it uses base URL `https://tile.openstreetmap.org/{z}/{x}/{y}.png`, a library-specific user-agent, and default timeouts

#### Scenario: Create retriever with custom configuration
- **WHEN** a developer instantiates `SimpleOsmTileRetriever(baseUrl, userAgent, connectTimeout, readTimeout)`
- **THEN** all HTTP requests use the provided parameters

#### Scenario: Load tile via HTTP
- **WHEN** `loadTile(12, 2048, 1536)` is called on `SimpleOsmTileRetriever`
- **THEN** an HTTP GET request is made to the corresponding tile URL and the future completes with the decoded `Image`

#### Scenario: Handle HTTP error
- **WHEN** the HTTP request fails (timeout, 404, network error)
- **THEN** the `CompletableFuture` completes exceptionally with the underlying exception

### Requirement: TileCache Interface
The system SHALL define a `TileCache` interface for thread-safe tile storage.

#### Scenario: Define cache contract
- **WHEN** a developer implements `TileCache`
- **THEN** the interface requires `get(int zoom, long x, long y)`, `put(int zoom, long x, long y, Image image)`, and `clear()` methods

#### Scenario: Concurrent cache access
- **WHEN** multiple threads call `get()` and `put()` simultaneously
- **THEN** the cache implementation handles requests safely without corruption

### Requirement: InMemoryTileCache Implementation
The system SHALL provide an LRU in-memory tile cache.

#### Scenario: Create cache with capacity
- **WHEN** a developer instantiates `InMemoryTileCache(500)`
- **THEN** the cache stores up to 500 tiles

#### Scenario: Cache hit
- **WHEN** a tile is requested that exists in the cache
- **THEN** `get()` returns the cached `Image` instance

#### Scenario: Cache miss
- **WHEN** a tile is requested that does not exist in the cache
- **THEN** `get()` returns `null`

#### Scenario: LRU eviction
- **WHEN** the cache is full and a new tile is added
- **THEN** the least recently used tile is removed to make space

### Requirement: Tile Loading Pipeline
The system SHALL coordinate cache lookups and asynchronous retrieval internally.

#### Scenario: Load visible tiles on viewport change
- **WHEN** the map center, zoom, or viewport size changes
- **THEN** the system calculates the set of visible tiles and initiates loading for missing tiles

#### Scenario: Prioritize cache over network
- **WHEN** a visible tile is needed
- **THEN** the system checks the cache first and only initiates network retrieval if not found

#### Scenario: Store retrieved tile in cache
- **WHEN** a tile is successfully loaded from the retriever
- **THEN** the system stores it in the cache via `put()` before rendering

### Requirement: Generation-Based Invalidation
The system SHALL prevent rendering of stale tiles when the viewport changes rapidly.

#### Scenario: Invalidate pending loads on zoom change
- **WHEN** the user zooms to level 10, initiating tile loads, then immediately zooms to level 12
- **THEN** tiles from the level 10 request are ignored when they complete

#### Scenario: Render current generation tiles
- **WHEN** tiles complete for the current viewport state
- **THEN** they are rendered immediately

### Requirement: Placeholder Rendering
The system SHALL display placeholder tiles for failed or pending loads.

#### Scenario: Show placeholder for failed tile
- **WHEN** a tile load fails (network error, HTTP 404, etc.)
- **THEN** a light gray 256×256 placeholder image is rendered in the tile position

#### Scenario: Show placeholder for pending tile
- **WHEN** a tile is loading but not yet available
- **THEN** a placeholder is rendered until the tile completes

### Requirement: Thread Pool Management
The system SHALL use a shared executor for all tile loading operations.

#### Scenario: Initialize shared executor
- **WHEN** the first `MapView` is created
- **THEN** a fixed thread pool is created with size `max(2, availableProcessors)`

#### Scenario: Execute tile loads in background
- **WHEN** `loadTile()` is called
- **THEN** network and image decoding work executes in the shared thread pool, not on the JavaFX Application Thread

#### Scenario: Update UI on JavaFX thread
- **WHEN** a tile load completes
- **THEN** UI updates (setting image on `ImageView`) occur via `Platform.runLater()`

### Requirement: Error Logging
The system SHALL log tile loading errors for debugging.

#### Scenario: Log failed tile load
- **WHEN** a tile load fails with an exception
- **THEN** the error is logged via SLF4J with tile coordinates and exception details
