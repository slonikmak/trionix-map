## 1. Implementation
- [x] 1.1 Add public `TileSource` type with immutable built-in HTTP tile source configuration
- [x] 1.2 Add `MapView` tile source property/getter/setter for the built-in tile pipeline
- [x] 1.3 Update built-in tile retrieval internals to consume the active `TileSource` dynamically
- [x] 1.4 Invalidate stale in-flight completions and clear cache when the tile source changes
- [x] 1.5 Add or update tests covering runtime source switching and custom-retriever behavior boundaries
- [x] 1.6 Update README/quickstart examples for the new API
