# Tile Source Runtime Switching Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a public `TileSource` API on `MapView` so the built-in raster tile source can be changed at runtime without exposing retriever internals.

**Architecture:** Introduce immutable `TileSource` as the built-in HTTP tile configuration, make `SimpleOsmTileRetriever` read the active source dynamically, and extend `TileManager` with a source-version invalidation boundary so stale completions are dropped before cache write or redraw. `MapView` owns the new property only in the built-in pipeline and clears cache plus refreshes tiles on source changes.

**Tech Stack:** Java 21, JavaFX properties, `CompletableFuture`, JUnit 5, Maven

---

### Task 1: Add TileSource and retriever-level tests

**Files:**
- Create: `trionix-map-core/src/main/java/com/trionix/maps/TileSource.java`
- Modify: `trionix-map-core/src/main/java/com/trionix/maps/SimpleOsmTileRetriever.java`
- Test: `trionix-map-core/src/test/java/com/trionix/maps/SimpleOsmTileRetrieverTest.java`

- [ ] Step 1: Add failing tests for default `TileSource` and runtime retriever switching
- [ ] Step 2: Run retriever tests and confirm failure on missing API
- [ ] Step 3: Implement immutable `TileSource` and update `SimpleOsmTileRetriever` to read the active source dynamically
- [ ] Step 4: Re-run retriever tests until green

### Task 2: Add stale-source invalidation in TileManager

**Files:**
- Modify: `trionix-map-core/src/main/java/com/trionix/maps/internal/tiles/TileManager.java`
- Test: `trionix-map-core/src/test/java/com/trionix/maps/internal/tiles/TileManagerTest.java`

- [ ] Step 1: Add failing test proving old-source completion is ignored after source switch
- [ ] Step 2: Run `TileManager` tests and confirm failure
- [ ] Step 3: Implement source-version invalidation plus public reset hook for source changes
- [ ] Step 4: Re-run `TileManager` tests until green

### Task 3: Expose MapView TileSource API

**Files:**
- Modify: `trionix-map-core/src/main/java/com/trionix/maps/MapView.java`
- Test: `trionix-map-core/src/test/java/com/trionix/maps/MapViewTest.java`
- Test: `trionix-map-core/src/test/java/com/trionix/maps/MapViewIntegrationTest.java`

- [ ] Step 1: Add failing tests for default `TileSource`, runtime property updates, and custom-retriever boundary
- [ ] Step 2: Run targeted `MapView` tests and confirm failure
- [ ] Step 3: Implement `tileSourceProperty()`, `getTileSource()`, `setTileSource(...)`, cache clear, and refresh behavior for the built-in pipeline
- [ ] Step 4: Re-run targeted `MapView` tests until green

### Task 4: Update docs and broad verification

**Files:**
- Modify: `README.md`
- Modify: `QUICKSTART.md`

- [ ] Step 1: Update examples to show `TileSource` usage on `MapView`
- [ ] Step 2: Run targeted Maven tests for retriever, tile manager, and map view flows
- [ ] Step 3: Run a broader `trionix-map-core` Maven verification sweep
