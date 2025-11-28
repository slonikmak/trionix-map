```markdown
# Tasks: Add Tiered Tile Cache System

## 1. Implementation

### 1.1 FileTileCache
- [x] 1.1.1 Create `FileTileCache` class implementing `TileCache`
- [x] 1.1.2 Implement OSM-style directory structure: `{cacheDir}/{zoom}/{x}/{y}.png`
- [x] 1.1.3 Implement `get()` with file existence check and Image loading
- [x] 1.1.4 Implement `put()` with directory creation and PNG file writing
- [x] 1.1.5 Implement `clear()` to delete all cached files
- [x] 1.1.6 Implement LRU eviction based on file count limit (track via last-modified time)
- [x] 1.1.7 Add constructor validation (non-null path, positive capacity)

### 1.2 TieredTileCache
- [x] 1.2.1 Create `TieredTileCache` class implementing `TileCache`
- [x] 1.2.2 Accept ordered list of `TileCache` instances (L1, L2, ...)
- [x] 1.2.3 Implement `get()` with cascading lookup and promotion to higher tiers
- [x] 1.2.4 Implement `put()` to write to all tiers
- [x] 1.2.5 Implement `clear()` to clear all tiers
- [x] 1.2.6 Add constructor validation (non-empty list, no nulls)

### 1.3 TileCacheBuilder (Optional Convenience)
- [x] 1.3.1 Create `TileCacheBuilder` with fluent API
- [x] 1.3.2 Add `.memory(int capacity)` method
- [x] 1.3.3 Add `.disk(Path cacheDir, int maxFiles)` method
- [x] 1.3.4 Add `.build()` returning configured `TileCache`

## 2. Testing

### 2.1 FileTileCache Tests
- [x] 2.1.1 Test: put/get roundtrip stores and retrieves tile correctly
- [x] 2.1.2 Test: get returns null for missing tile
- [x] 2.1.3 Test: clear removes all cached files
- [x] 2.1.4 Test: LRU eviction removes oldest files when capacity exceeded
- [x] 2.1.5 Test: concurrent access is thread-safe
- [x] 2.1.6 Test: invalid constructor arguments throw IllegalArgumentException

### 2.2 TieredTileCache Tests
- [x] 2.2.1 Test: get checks L1 first, then L2
- [x] 2.2.2 Test: L2 hit promotes tile to L1
- [x] 2.2.3 Test: put writes to all tiers
- [x] 2.2.4 Test: clear clears all tiers
- [x] 2.2.5 Test: works with single cache (degenerate case)

### 2.3 Integration Tests
- [x] 2.3.1 Test: MapView works with TieredTileCache (memory + disk)
- [x] 2.3.2 Test: Tiles persist across cache clear of L1 when L2 is disk

## 3. Documentation
- [x] 3.1 Update README.md with tiered cache usage example
- [x] 3.2 Add Javadoc to new public classes

## 4. Verification
- [x] 4.1 Run `mvn clean install` to verify build
- [x] 4.2 Run all tests pass
- [x] 4.3 Manual test with demo app

``` 
