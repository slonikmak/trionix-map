# Trionix JavaFX MapView

Lightweight JavaFX control for rendering OpenStreetMap raster tiles with smooth pan/zoom, in-memory caching, and a pluggable overlay system.

```
┌─────────────────────────────────────────────┐
│  ┌───────────────────────────────────────┐  │
│  │     MapView (JavaFX Region)           │  │
│  │  ┌─────────────────────────────────┐  │  │
│  │  │   Tile Layer (OSM Tiles)        │  │  │
│  │  │   ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓    │  │  │
│  │  │   ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓    │  │  │
│  │  └─────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────┐  │  │
│  │  │   Layer 1: Routes (Lines)       │  │  │
│  │  │   ───────────→                  │  │  │
│  │  └─────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────┐  │  │
│  │  │   Layer 2: Markers (Custom UI)  │  │  │
│  │  │   📍 📍 📍                       │  │  │
│  │  └─────────────────────────────────┘  │  │
│  └───────────────────────────────────────┘  │
│   centerLat, centerLon, zoom properties     │
└─────────────────────────────────────────────┘
```

## Quick Start

1. **Requirements** – Java 21+, Maven 3.9+, and an internet connection for live OSM tiles.
2. **Build and test** – `mvn clean verify` compiles the library and runs the full unit/integration suite.
3. **Run the samples** – Choose one of the following:
   - **Simple example:** `mvn compile exec:java -Dexec.mainClass=com.trionix.maps.samples.SimpleMapExample`
     - Minimal 20-line code example showing basic map display
   - **Basic example:** `mvn compile exec:java -Dexec.mainClass=com.trionix.maps.samples.MapViewSampleApp`
     - Simple map centered on San Francisco with labeled markers
   - **Advanced example:** `mvn compile exec:java -Dexec.mainClass=com.trionix.maps.samples.AdvancedMapExample`
     - Interactive map with control panel, multiple layers, and route drawing
   - **Quick launcher:** Run `.\run-examples.ps1` (Windows) or `./run-examples.sh` (Linux/Mac) for an interactive menu

Use the mouse to drag (pan) and scroll (zoom) the map.

📚 **New to the library?** Check out:
- [QUICKSTART.md](QUICKSTART.md) - Fast introduction with minimal examples
- [CODE_EXAMPLES.md](CODE_EXAMPLES.md) - Ready-to-copy code snippets
- [src/main/java/com/trionix/maps/samples/README.md](src/main/java/com/trionix/maps/samples/README.md) - Detailed examples documentation

## Public API Overview

- `MapView` – JavaFX `Region` with observable `centerLat`, `centerLon`, and `zoom` properties along with a `flyTo` helper for animated navigation.
- `MapLayer` – Abstract pane you subclass to render overlays (markers, paths, heatmaps). Layers live inside `MapView#getLayers()` and receive per-frame `layoutLayer` callbacks.
- `TileRetriever` – Interface for asynchronous tile fetchers. The default `SimpleOsmTileRetriever` streams tiles from OpenStreetMap via `HttpClient`.
- `TileCache` – Interface for thread-safe tile caches. `InMemoryTileCache` ships with an LRU implementation sized for a configurable number of tiles.

## Using `MapView`

```java
MapView mapView = new MapView();
mapView.setCenterLat(37.7749);
mapView.setCenterLon(-122.4194);
mapView.setZoom(12.0);
mapView.getLayers().add(new MarkerLayer());
```

`MapView` must be accessed from the JavaFX Application Thread. Property setters perform latitude/longitude normalization and clamp zoom to the supported range (defaults 1–19). The control automatically requests tiles, caches them, and re-renders when you change the viewport or layer stack.

## Writing Layers

Subclass `MapLayer` and override `layoutLayer(MapView mapView)` to position child nodes using the map state. Always mutate the scene graph on the JavaFX Application Thread and call `requestLayerLayout()` when your layer needs a refresh between pulses. See `com.trionix.maps.samples.MapViewSampleApp` for a simple `MarkerLayer` that converts lat/lon coordinates into on-screen positions using Web Mercator math.

## Threading Notes

- Modify `MapView` properties and its layer list on the JavaFX Application Thread.
- `TileRetriever` and `TileCache` implementations must be thread-safe; they are invoked from background tasks running on virtual threads.
- `MapLayer#layoutLayer` is always called on the JavaFX Application Thread, so long-running computations should be moved off-thread.
- `TileExecutors` uses a shared virtual-thread-per-task executor by default, so blocking HTTP or decoding inside a retriever is acceptable as long as it never touches JavaFX scene graph APIs.

## Testing

Run `mvn clean verify` to execute unit tests, integration tests, and JavaFX headless harness checks. Use the same command before contributing changes so the OpenSpec checklist can be marked complete confidently.

## Examples

The project includes two example applications in `src/main/java/com/trionix/maps/samples/`:

1. **MapViewSampleApp** – Basic example demonstrating simple marker placement
2. **AdvancedMapExample** – Comprehensive example with:
   - Multiple layers (markers and routes)
   - Animated navigation with `flyTo()`
   - Interactive control panel
   - Real-time coordinate display
   - Custom styled markers and route lines

See `src/main/java/com/trionix/maps/samples/README.md` for detailed documentation and code snippets.

## License

See LICENSE file for details.
