```markdown
# Change: Add Tiered Tile Cache System

## Why
The current implementation only supports in-memory tile caching (`InMemoryTileCache`), which means tiles must be re-downloaded after application restart or when memory cache evicts them. Users need persistent disk caching to reduce network traffic and improve load times for frequently visited areas. A composable cache architecture allows flexible combinations of memory (fast, volatile) and disk (persistent, larger capacity) caches.

## What Changes
- Add `FileTileCache` implementation that stores tiles on disk using OSM-style directory structure `{cacheDir}/{zoom}/{x}/{y}.png`
- Add `TieredTileCache` decorator that chains multiple `TileCache` instances (e.g., L1 memory → L2 disk)
- Add `TileCacheBuilder` for fluent cache configuration
- Disk cache uses LRU eviction based on file count limit with lazy cleanup
- Reading from L2 promotes tiles to L1 automatically

## Impact
- Affected specs: `tile-management`
- Affected code: 
  - New classes: `FileTileCache`, `TieredTileCache`, `TileCacheBuilder`
  - No breaking changes to existing `TileCache` interface or `InMemoryTileCache`
- Public API surface: Additive only (new classes implementing existing interface)

``` 
