# Design Document: JavaFX OSM Map MVP

## Context

Desktop applications built with JavaFX lack a simple, embeddable component for displaying OpenStreetMap tiles. Existing solutions (e.g., GMapsFX, older JavaFX map libraries) impose complexity through:
- Legacy API compatibility requirements
- External configuration files or system properties
- Disk caching with cleanup/TTL management
- ServiceLoader-based plugin architectures

The MVP targets small-to-medium desktop applications that need basic map display without operational overhead. Users include location-based tools, data visualization dashboards, and logistics applications.

## Goals / Non-Goals

### Goals
- Provide a single JavaFX `Region` component (`MapView`) that "just works" with zero configuration beyond instantiation
- Support interactive pan/zoom with mouse and touch input matching standard map UX expectations
- Enable custom overlays (markers, paths, heatmaps) via a simple layer abstraction
- Maintain smooth performance (~60 FPS) on typical desktop hardware (Intel i5+, 8GB RAM)
- Keep the public API surface minimal and stable for future extension

### Non-Goals
- Compatibility with existing map library APIs (GMapsFX, Leaflet.js, Mapbox)
- Disk caching (in-memory only for MVP; disk caching deferred to post-MVP)
- Vector tiles, custom tile styles, or non-OSM tile sources (extensible but not implemented)
- Geocoding, routing, search, or other map services (out of scope)
- Mobile/embedded JavaFX platforms (focus on desktop JVM)

## Decisions

### Decision: Code-Only Configuration
**What:** All configuration happens through constructor parameters; no system properties, environment variables, or config files.

**Why:**
- Eliminates deployment complexity (no need to ship/manage separate config files)
- Makes behavior explicit and testable (dependencies are injected, not discovered)
- Prevents global state issues (multiple `MapView` instances with different configs)

**Alternatives Considered:**
- System properties: Rejected due to global state and unclear precedence rules
- Config files (JSON/XML): Rejected due to deployment overhead and parsing complexity
- ServiceLoader: Rejected due to classpath coupling and implicit behavior

### Decision: In-Memory Tile Cache Only
**What:** `InMemoryTileCache` uses LRU eviction with configurable capacity (default 500 tiles ≈ 50-100MB).

**Why:**
- Simplifies MVP by avoiding filesystem I/O, TTL management, and disk space monitoring
- Sufficient for typical desktop sessions (few hours of use)
- Modern desktops have ample RAM (8-16GB+), making memory cache viable

**Alternatives Considered:**
- Disk cache: Deferred to post-MVP; requires cleanup policies, TTL, and error recovery
- Hybrid (memory + disk): Adds complexity without clear MVP benefit

**Trade-offs:**
- Cold start penalty: No persistence across application restarts
- Memory footprint: ~100MB for 500 tiles (acceptable for desktop apps)

### Decision: Web Mercator Projection Only
**What:** Hardcode EPSG:3857 (Web Mercator) for all coordinate calculations.

**Why:**
- OSM standard tile server uses Web Mercator exclusively
- Other projections require tile re-projection or vector tiles (out of MVP scope)
- 99% of web map use cases use Web Mercator

**Alternatives Considered:**
- Pluggable projection interface: Over-engineering for MVP; defer until alternate tile sources are needed

### Decision: Shared Thread Pool for Tile Loading
**What:** Single static `ExecutorService` with `max(2, availableProcessors)` threads shared across all `MapView` instances.

**Why:**
- Prevents thread explosion when multiple map views exist
- Simplifies resource management (no per-instance executor shutdown)
- HTTP clients benefit from connection pooling at JVM level

**Alternatives Considered:**
- Per-MapView executor: Rejected due to thread overhead and complex shutdown semantics
- Virtual threads (Java 21+): Considered for future; not critical for MVP with limited concurrency

**Trade-offs:**
- Global resource: All maps share bandwidth/thread capacity
- No graceful shutdown: Executor lives for JVM lifetime (acceptable for desktop apps)

### Decision: Generation-Based Tile Invalidation
**What:** Each viewport state change increments a generation counter; tile load results are tagged with generation and ignored if stale.

**Why:**
- Prevents visual artifacts from out-of-order tile arrivals during rapid pan/zoom
- Simpler than cancelling HTTP requests (which may not be immediately interruptible)
- Low overhead (single integer increment + comparison)

**Alternatives Considered:**
- HTTP request cancellation: Partial support in `HttpClient`; adds complexity without eliminating race conditions
- Queue draining: Doesn't prevent already-dispatched requests from completing

### Decision: JavaFX Properties for Observable State
**What:** `centerLatProperty()`, `centerLonProperty()`, `zoomProperty()` expose map state as JavaFX properties.

**Why:**
- Idiomatic JavaFX API (developers expect properties for binding/listening)
- Enables declarative UI (FXML binding, CSS pseudo-classes if needed later)
- Zero-cost abstraction (JavaFX properties are framework standard)

**Alternatives Considered:**
- Custom listener interfaces: Reinvents JavaFX wheel; worse integration with FXML/bindings
- Immutable state objects: Breaks JavaFX binding conventions

### Decision: Abstract MapLayer with Lifecycle Hooks
**What:** Layers extend `Pane`, implement `layoutLayer()`, and receive `layerAdded`/`layerRemoved` callbacks.

**Why:**
- Composition over inheritance: Layers are first-class JavaFX nodes
- Clear lifecycle: `layerAdded` for setup, `layoutLayer` for positioning, `layerRemoved` for cleanup
- Flexibility: Layers can use any JavaFX nodes (shapes, images, canvas, etc.)

**Alternatives Considered:**
- Marker/Path-specific classes: Too restrictive; doesn't cover heatmaps, custom overlays
- Canvas-only rendering: Lower-level; harder for users to leverage JavaFX scene graph

## Architecture Overview

```
MapView (JavaFX Region)
├─ MapState (internal)
│  ├─ centerLat, centerLon, zoom (observable properties)
│  ├─ viewportWidth, viewportHeight
│  └─ calculateVisibleTiles() → Set<TileCoordinate>
├─ TileManager (internal)
│  ├─ generation counter
│  ├─ TileCache (interface → InMemoryTileCache impl)
│  ├─ TileRetriever (interface → SimpleOsmTileRetriever impl)
│  └─ loadTiles(Set<TileCoordinate>) → Map<TileCoordinate, Image>
├─ Projection (internal)
│  └─ WebMercatorProjection impl
└─ ObservableList<MapLayer>
   └─ layerAdded/layoutLayer/layerRemoved lifecycle
```

### Data Flow: Tile Loading
1. User pans/zooms → `MapState` updates → `TileManager.onStateChange()`
2. `TileManager` increments generation, calls `state.calculateVisibleTiles()`
3. For each tile coordinate:
   - Check `TileCache.get(z, x, y)` → if hit, use cached `Image`
   - If miss, submit `retriever.loadTile(z, x, y)` to `TileExecutors.TILE_EXECUTOR`
4. On `CompletableFuture` completion:
   - If generation matches, call `cache.put()` and schedule UI update via `Platform.runLater()`
   - If generation stale, discard result
5. `MapView.layoutChildren()` renders tiles + layers in z-order

### Data Flow: Layer Layout
1. `MapState` changes trigger `MapView.requestLayout()`
2. JavaFX calls `layoutChildren()` on next pulse
3. For each layer in `getLayers()`:
   - Call `layer.layoutLayer(this)` (max once per pulse)
4. Layer reads `getCenterLat()`, `getCenterLon()`, `getZoom()`, viewport size
5. Layer positions child nodes using projection math

## Risks / Trade-offs

### Risk: Memory Exhaustion from Large Cache
**Mitigation:** Default to 500 tiles (~50-100MB); document capacity tuning in Javadoc. Consider adding max memory limit (MB) in post-MVP.

### Risk: OSM Tile Server Rate Limiting
**Mitigation:** Document user-agent requirement, add delay between requests if needed. Consider server-side throttling detection (HTTP 429) in post-MVP.

### Risk: Thread Pool Starvation
**Mitigation:** Use `max(2, availableProcessors)` to scale with hardware. Monitor task queue depth in logs if issues arise.

### Trade-off: No Inertial Panning
**Justification:** Adds complexity (velocity tracking, deceleration curves). Defer to post-MVP based on user feedback.

### Trade-off: No Disk Cache
**Impact:** Cold starts load all tiles from network. Acceptable for MVP; disk cache is #1 post-MVP priority.

## Migration Plan

Not applicable (greenfield implementation). Future API stability:
- Public API (`MapView`, `MapLayer`, `TileRetriever`, `TileCache`) is stable; breaking changes require major version bump.
- Internal classes (`MapState`, `TileManager`, `Projection`) may change without notice.

## Open Questions

1. **Should we expose tile load progress?** (e.g., `loadingProperty()` or event listeners)
   - **Decision:** Defer to post-MVP; placeholder tiles provide basic feedback.

2. **Should we support retries for failed tile loads?**
   - **Decision:** No automatic retries in MVP; log errors and show placeholder. Add retry policy in post-MVP if needed.

3. **Should we limit concurrent HTTP requests per host?**
   - **Decision:** Rely on `HttpClient` default connection pool limits. Monitor in testing; add explicit limit if OSM server complains.

4. **Should we support custom tile size (e.g., 512×512)?**
   - **Decision:** No; OSM standard is 256×256. Future tile sources may differ, but defer until real use case emerges.
