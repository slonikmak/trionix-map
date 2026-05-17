## Context
The current API exposes two levels of tile configuration:
- default `MapView()` with built-in OSM retriever
- advanced `MapView(TileRetriever, TileCache)` for full custom behavior

What is missing is a stable middle layer for the common case: changing the built-in raster tile source without forcing consumers to instantiate or own a retriever implementation. The user requirement is explicit that tile pipeline internals should not leak into the normal public API.

## Goals / Non-Goals
- Goals:
  - Provide a high-level `TileSource` API on `MapView`
  - Allow changing the tile source at any time during map lifetime
  - Keep stale requests and cached images from the previous source from polluting the new source
  - Preserve the existing custom `TileRetriever` extension path
- Non-Goals:
  - Replace or deprecate `TileRetriever`
  - Add attribution UI, API key management, or non-HTTP tile schemes in this change
  - Support `TileSource` mutation for user-supplied custom retrievers

## Decisions
- Decision: Introduce a public immutable `TileSource` type rather than a `setTileBaseUrl(String)`-only API.
  - Why: the source configuration already conceptually includes base URL, user-agent, and timeouts. Encoding only a URL would under-model the built-in retriever and create pressure for follow-up breaking changes.
  - Alternatives considered:
    - `setTileBaseUrl(String)` only: simpler now, weaker long-term API
    - expose `SimpleOsmTileRetriever` mutators directly: leaks implementation details into application code

- Decision: `MapView` owns the `TileSource` property only for the built-in tile pipeline.
  - Why: this keeps the common API simple while preserving the existing advanced retriever constructor for escape-hatch use cases.
  - Alternatives considered:
    - require every `TileRetriever` to support runtime reconfiguration: too invasive and not meaningful for offline or non-HTTP retrievers
    - make `TileSource` always available even with a custom retriever: misleading because the retriever might ignore it

- Decision: runtime source changes clear cache and invalidate previous-source completions.
  - Why: mixing tiles from different servers in a shared cache would produce incorrect visuals and stale redraws.
  - Alternatives considered:
    - keep cache entries and trust URL equality: fragile and couples cache correctness to retriever internals
    - recreate the whole tile manager on every switch: heavier lifecycle churn than needed

## Risks / Trade-offs
- Runtime source switching adds another invalidation axis beyond viewport generation changes.
  - Mitigation: track a source version/token alongside existing refresh logic so completions from an old source are dropped before cache write and delivery.

- `MapView` now has two operating modes: built-in source-managed mode and custom-retriever mode.
  - Mitigation: document that `tileSourceProperty()` is only supported when the built-in pipeline is active; custom-retriever mode remains constructor-driven.

## Migration Plan
1. Add `TileSource` and built-in OSM defaults.
2. Extend `MapView` default pipeline to expose and react to `tileSourceProperty()`.
3. Update tile retrieval/manager internals to reject stale completions after source changes and clear cache on source switch.
4. Update specs, docs, and tests for default-mode runtime switching and custom-retriever boundaries.

## Open Questions
- None for MVP. Future enhancements like attribution metadata or API-key headers can extend `TileSource` without changing the direction of this design.
