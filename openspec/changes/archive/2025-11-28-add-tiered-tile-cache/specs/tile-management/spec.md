```markdown
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

```
