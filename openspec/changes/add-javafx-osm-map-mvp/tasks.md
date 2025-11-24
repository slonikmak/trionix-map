## 1. Foundation and Utilities

- [x] 1.1 Set up Maven project structure with JavaFX 21+ and Java 21+ dependencies
- [x] 1.2 Create package structure: `com.trionix.maps`, `com.trionix.maps.internal`, `com.trionix.maps.layer`
- [x] 1.3 Implement `Projection` interface and `WebMercatorProjection` class with lat/lon ↔ pixel conversions
- [x] 1.4 Add unit tests for projection math (equator, poles, tile boundaries, zoom levels 0-19)
- [x] 1.5 Implement coordinate normalization utilities (latitude clamping, longitude wrapping)
- [x] 1.6 Add unit tests for coordinate normalization edge cases

## 2. Tile Management Infrastructure

- [x] 2.1 Define `TileRetriever` interface with `loadTile(int zoom, long x, long y)` method
- [x] 2.2 Define `TileCache` interface with `get()`, `put()`, and `clear()` methods
- [x] 2.3 Implement `InMemoryTileCache` with LRU eviction using `LinkedHashMap`
- [x] 2.4 Add concurrency tests for `InMemoryTileCache` (parallel get/put operations)
- [x] 2.5 Implement `SimpleOsmTileRetriever` using `HttpClient` with configurable timeouts and user-agent
- [x] 2.6 Add integration tests for `SimpleOsmTileRetriever` (mock HTTP server or actual OSM tiles)
- [x] 2.7 Create `TileExecutors` utility class with shared fixed thread pool
- [x] 2.8 Generate placeholder tile image (256×256 light gray PNG) as static resource

## 3. Map State and Tile Manager

- [x] 3.1 Implement `MapState` class with center lat/lon, zoom, viewport dimensions, and visible tile calculation
- [x] 3.2 Add unit tests for visible tile calculation (various center/zoom/viewport combinations)
- [x] 3.3 Implement `TileManager` with generation-based invalidation logic
- [x] 3.4 Add cache lookup logic in `TileManager` before initiating network retrieval
- [x] 3.5 Implement async tile loading with `CompletableFuture` and result handling
- [x] 3.6 Add error logging for failed tile loads using SLF4J
- [x] 3.7 Implement cache storage of successfully loaded tiles
- [x] 3.8 Add unit tests for generation-based invalidation (stale tile rejection)

## 4. MapView Core Component

- [x] 4.1 Create `MapView` class extending `Region` with observable `centerLat`, `centerLon`, and `zoom` properties
- [x] 4.2 Implement default constructor (OSM retriever + in-memory cache with 500 tile capacity)
- [x] 4.3 Implement custom constructor accepting `TileRetriever` and `TileCache` parameters
- [x] 4.4 Add property validation and normalization in setters (clamp lat, wrap lon, clamp zoom)
- [x] 4.5 Integrate `MapState` and `TileManager` into `MapView` lifecycle
- [x] 4.6 Implement tile rendering in `layoutChildren()` method
- [x] 4.7 Add mouse drag handler for pan interaction with center coordinate updates
- [x] 4.8 Add mouse scroll handler for zoom interaction around cursor position
- [x] 4.9 Add touch/trackpad zoom gesture handler (JavaFX `ZoomEvent`)
- [x] 4.10 Test basic MapView instantiation and property binding

## 5. Layer System

- [x] 5.1 Create abstract `MapLayer` class extending `Pane` with `layoutLayer()` abstract method
- [x] 5.2 Add `layerAdded(MapView)` and `layerRemoved(MapView)` lifecycle hooks with default no-op implementations
- [x] 5.3 Add `requestLayerLayout()` method to trigger layout on next pulse
- [x] 5.4 Integrate `ObservableList<MapLayer>` into `MapView` with add/remove listeners
- [x] 5.5 Implement layer rendering above tile layer in correct z-order
- [x] 5.6 Implement automatic `layoutLayer()` calls on map state changes (batched per pulse)
- [x] 5.7 Add integration test with sample marker layer

## 6. Animation System

- [x] 6.1 Implement `flyTo(double lat, double lon, double zoom, Duration duration)` method in `MapView`
- [x] 6.2 Create JavaFX `Timeline` animation with ease-in-out interpolation for center and zoom
- [x] 6.3 Implement animation interruption logic (cancel previous on new flyTo or manual interaction)
- [x] 6.4 Ensure property change notifications fire at each animation frame
- [x] 6.5 Add integration test for flyTo animation (verify final position and property updates)

## 7. Testing and Validation

- [x] 7.1 Create headless JavaFX test harness for integration tests
- [x] 7.2 Write integration test: create MapView, set center/zoom, verify tile requests
- [x] 7.3 Write integration test: pan map, verify center coordinate changes
- [x] 7.4 Write integration test: zoom map, verify zoom level changes and tile updates
- [x] 7.5 Write integration test: add/remove layers, verify lifecycle hooks called
- [x] 7.6 Add performance smoke test: measure FPS during pan/zoom (target ~60 FPS)
- [x] 7.7 Test error handling: verify placeholder rendering for failed tile loads

## 8. Documentation and Polish

- [x] 8.1 Write Javadoc for all public API classes and methods (`MapView`, `MapLayer`, `TileRetriever`, `TileCache`)
- [x] 8.2 Document thread safety requirements in Javadoc (e.g., `layoutLayer` on JavaFX thread)
- [x] 8.3 Create sample application demonstrating basic MapView usage with markers
- [x] 8.4 Add README with quick start guide and API overview
- [x] 8.5 Verify all code follows Google Java Style (4-space indent, naming conventions)
- [x] 8.6 Run `openspec validate add-javafx-osm-map-mvp --strict` and resolve any issues
