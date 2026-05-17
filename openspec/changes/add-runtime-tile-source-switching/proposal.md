# Change: Add runtime tile source switching via MapView TileSource API

## Why
The library currently allows alternate tile servers only by constructing a custom `SimpleOsmTileRetriever` or providing a fully custom `TileRetriever`. That is flexible for advanced integrations but too low-level for the common case where an application needs to switch the raster tile source at runtime without exposing tile pipeline internals.

## What Changes
- Add a public `TileSource` value object for configuring the built-in HTTP tile pipeline.
- Add `MapView` property-based API for reading and updating the active `TileSource` at runtime.
- Keep `MapView(TileRetriever, TileCache)` as the advanced extension path for custom retrievers.
- Define runtime switching semantics so source changes invalidate stale in-flight tile loads and prevent cache mixing across different tile sources.

## Impact
- Affected specs: `map-view-control`, `tile-management`
- Affected code: `MapView`, `SimpleOsmTileRetriever`, `TileManager`, related tests and documentation
