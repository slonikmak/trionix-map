# v0.1.0-beta — First public beta

> Java 21+ • JavaFX 21+ • Maven multi-module • OSM raster tiles

This is the first public beta release of `trionix-map`: a lightweight JavaFX `MapView` for rendering OpenStreetMap raster tiles with smooth programmatic navigation and an extensible overlay layer system.

## Highlights

- **MapView for OSM raster tiles**: drag (pan), wheel/gesture zoom, and **double-click zoom**.
- **Smooth programmatic navigation**: `flyTo(...)` for animated transitions.
- **Layered architecture**: an extensible `MapLayer` system for overlays without changing tile rendering.
- **Tile caching**: LRU in-memory cache, disk cache, and **tiered cache** (L1 memory → L2 disk).
- **Performance**: asynchronous tile fetching and decoding on **virtual threads**.
- **Demos and examples**: the `trionix-map-demo` module includes simple/basic/advanced examples.

## What’s inside

### Core (`trionix-map-core`)

- `MapView` (JavaFX `Region`) with observable center and zoom properties.
- Web Mercator math and coordinate normalization.
- Tile pipeline: visible tile calculation → cache lookup → retrieval → UI update.
- Extension interfaces:
  - `TileRetriever` (default: `SimpleOsmTileRetriever` using `HttpClient`)
  - `TileCache` (thread-safe cache implementations)

### Layers (`trionix-map-layers`)

- **Point markers**: `PointMarkerLayer` for interactive markers.
- **Polylines**: layer for routes and lines.
- **Grid + scale ruler**: coordinate grid and scale ruler widget.

### Demo (`trionix-map-demo`)

- Ready-to-run sample applications (simple/basic/advanced) and launch scripts.

## Installation

Artifacts are published to **GitHub Packages** (Maven registry):

- `https://maven.pkg.github.com/slonikmak/trionix-map`

> Note: GitHub Packages often requires authentication even for public packages; without a token you may get a `401` when resolving dependencies.

Add the dependency (replace version with `0.1.0-beta.2`):

```xml
<dependency>
  <groupId>com.trionix</groupId>
  <artifactId>trionix-map-core</artifactId>
  <version>0.1.0-beta.2</version>
</dependency>

<dependency>
  <groupId>com.trionix</groupId>
  <artifactId>trionix-map-layers</artifactId>
  <version>0.1.0-beta.2</version>
</dependency>
```

Provide JavaFX as a runtime dependency for your application (choose the appropriate platform classifier).

## Quick start

- Build and test: `mvn clean verify`
- Run examples (Windows): `./run-examples.ps1`

## Known limitations

- The public API may change during the beta cycle.
- The default tile source is the public OpenStreetMap tile endpoint — please follow OpenStreetMap usage guidelines (reasonable concurrency, proper User-Agent).

## Changelog (high-level)

- JavaFX OSM MapView MVP
- Multi-module Maven project structure
- Virtual threads used by default for tile loading
- Point marker layer
- Polyline layer
- Grid layer + scale ruler
- Double-click zoom
- Tiered tile cache (memory + disk)
