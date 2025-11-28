# tile-management Specification

## Purpose
TBD - created by archiving change add-javafx-osm-map-mvp. Update Purpose after archive.
## Requirements
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

### Requirement: Error Logging
The system SHALL log tile loading errors for debugging.

#### Scenario: Log failed tile load


## ADDED Requirements

### Requirement: FileTileCache Implementation
The system SHALL provide a disk-based tile cache using file system storage.

#### Scenario: Create disk cache with directory and capacity
- **WHEN** a developer instantiates `FileTileCache(Path cacheDir, int maxFiles)`
- **THEN** the cache stores tiles in the specified directory up to the maximum file count

#### Scenario: Store tile on disk
- **WHEN** `put(12, 2048, 1536, image)` is called
- **THEN** the tile is written to `{cacheDir}/12/2048/1536.png`
- **AND** parent directories are created if they do not exist

#### Scenario: Retrieve tile from disk
- **WHEN** `get(12, 2048, 1536)` is called and the file exists
- **THEN** the tile image is loaded and returned
- **AND** the file's last-modified time is updated (touch) for LRU tracking

#### Scenario: Cache miss on disk
- **WHEN** `get(12, 2048, 1536)` is called and the file does not exist
- **THEN** `null` is returned

#### Scenario: LRU eviction by file count
- **WHEN** a new tile is stored and the cache exceeds `maxFiles`
- **THEN** the oldest files (by last-modified time) are deleted until within capacity

#### Scenario: Clear disk cache
- **WHEN** `clear()` is called
- **THEN** all cached tile files and directories are deleted

#### Scenario: Thread-safe file operations
- **WHEN** multiple threads call `get()` and `put()` concurrently
- **THEN** file operations complete without corruption using atomic write patterns

#### Scenario: Invalid constructor arguments
- **WHEN** `FileTileCache` is instantiated with null path or non-positive maxFiles
- **THEN** `IllegalArgumentException` is thrown

### Requirement: TieredTileCache Implementation
The system SHALL provide a composite cache that chains multiple `TileCache` instances.

#### Scenario: Create tiered cache with ordered tiers
- **WHEN** a developer instantiates `TieredTileCache(List<TileCache> tiers)`
- **THEN** the cache delegates to tiers in the provided order (L1, L2, ...)

#### Scenario: Cascading lookup
- **WHEN** `get(z, x, y)` is called
- **THEN** tiers are checked in order until a hit is found or all return null

#### Scenario: Promote on L2 hit
- **WHEN** `get(z, x, y)` finds the tile in L2 but not L1
- **THEN** the tile is promoted to L1 via `put()` before returning

#### Scenario: Write to all tiers
- **WHEN** `put(z, x, y, image)` is called
- **THEN** the tile is stored in all tiers

#### Scenario: Clear all tiers
- **WHEN** `clear()` is called
- **THEN** all tiers are cleared

#### Scenario: Empty or null tiers rejected
- **WHEN** `TieredTileCache` is instantiated with an empty list or a list containing null
- **THEN** `IllegalArgumentException` is thrown

### Requirement: TileCacheBuilder
The system SHALL provide a fluent builder for constructing tile caches.

#### Scenario: Build memory-only cache
- **WHEN** `TileCacheBuilder.create().memory(500).build()` is called
- **THEN** an `InMemoryTileCache` with capacity 500 is returned

#### Scenario: Build disk-only cache
- **WHEN** `TileCacheBuilder.create().disk(path, 10000).build()` is called
- **THEN** a `FileTileCache` with the specified path and capacity is returned

#### Scenario: Build tiered cache
- **WHEN** `TileCacheBuilder.create().memory(500).disk(path, 10000).build()` is called
- **THEN** a `TieredTileCache` with L1 memory and L2 disk is returned

#### Scenario: Build with no configuration
- **WHEN** `TileCacheBuilder.create().build()` is called with no caches configured
- **THEN** `IllegalStateException` is thrown

