# Change: Add JavaFX OSM Map MVP

## Why
Desktop JavaFX applications need a simple, embeddable component to display OpenStreetMap raster tiles without the complexity of existing map libraries. Current solutions require external configuration, disk caching, and compatibility layers that add unnecessary overhead for MVP use cases.

## What Changes
- Add core `MapView` JavaFX control with pan/zoom interaction and observable center/zoom properties
- Add tile loading pipeline with async HTTP retrieval, in-memory LRU caching, and placeholder handling for failed loads
- Add extensible layer system (`MapLayer`) for overlays like markers, paths, and heatmaps
- Add Web Mercator projection utilities for lat/lon ↔ pixel conversions and tile coordinate calculations
- Add smooth `flyTo` animation for programmatic navigation
- Add code-only configuration via constructor injection (no system properties, ENV vars, or config files)
- Add concurrent tile loading with generation-based invalidation to prevent stale tile rendering

## Impact
- Affected specs: This is a greenfield MVP creating five new capabilities:
  - `map-view-control` - Core UI component
  - `tile-management` - Tile retrieval and caching
  - `layer-system` - Custom overlay support
  - `coordinate-projection` - Geographic coordinate transformations
  - `animation-system` - Smooth navigation animations
- Affected code: New library under `com.trionix.maps` package
- Dependencies: JavaFX 21+, Java 21+ LTS, `java.net.http.HttpClient`
- Performance target: ~60 FPS pan/zoom on typical desktop hardware
- OSM usage policy compliance: User-agent header, reasonable request throttling
