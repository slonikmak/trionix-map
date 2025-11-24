# Change: Default TileExecutors Virtual Threads

## Why
- The current fixed-size platform thread pool in `TileExecutors` can stall under blocking I/O or heavy decoding work and no longer matches the Java 21+ baseline declared in project.md.
- Virtual threads in Java 21 provide cheap concurrency and simplify meeting OSM friendliness requirements without custom executor configuration.

## What Changes
- Require Java 21+ for building/running the library (aligning build, docs, and samples).
- Update the tile pipeline spec so the shared executor defaults to a virtual-thread-per-task model while preserving FX-thread marshalling rules.
- Document the new threading model in developer guides (`CODE_REVIEW_MapView_guide.md`, README) and highlight how FX vs non-FX responsibilities remain unchanged.
- Add regression tests (or update existing ones) to prove tile loads still execute off the FX thread.

## Impact
- Affected specs: `tile-management`, `map-view-control`.
- Affected code: `TileExecutors`, `TileManager`, `SimpleOsmTileRetriever`, `MapView`, JavaFX test harnesses, documentation.
