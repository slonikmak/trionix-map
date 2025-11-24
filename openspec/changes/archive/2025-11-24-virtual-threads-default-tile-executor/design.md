# Default Virtual Threads for TileExecutors

## Context
- `TileExecutors` currently owns a static fixed thread pool sized `max(2, availableProcessors)`.
- Tile loads involve blocking HTTP I/O plus PNG decoding, so stalls propagate quickly when only a handful of platform threads are available.
- Project conventions (project.md) already list Java 21+ and "VirtualThread" as part of the concurrency toolkit, but specs still describe a fixed pool.

## Goals
- Use Java 21 virtual threads by default for all tile retrieval/decoding tasks.
- Keep the single shared executor abstraction so existing `MapView` and `TileManager` code does not change API surface.
- Maintain JavaFX Application Thread guarantees: all scene graph changes stay on FX thread regardless of executor choice.

## Non-Goals
- Providing a public API to inject a custom `ExecutorService` (can be a follow-up change if needed).
- Reworking generation-based invalidation or cache semantics.

## Decisions
1. **Executor Implementation**: `TileExecutors` will build a singleton `ExecutorService` via `Executors.newVirtualThreadPerTaskExecutor()` (Java 21). We keep lazy initialization to avoid unnecessary threads before the first `MapView` appears.
2. **Fallback Strategy**: No fallback path is planned; the module now requires Java 21+. If the runtime cannot create virtual threads, initialization fails fast (acceptable because build target is Java 21).
3. **FX Thread Marshalling**: `TileManager` continues to invoke tile consumer callbacks on the FX thread using `Platform.runLater` when needed. Virtual threads only change the background execution context.
4. **Documentation Updates**: `CODE_REVIEW_MapView_guide.md`, README, and relevant specs will explicitly call out the virtual-thread default and Java 21 requirement.

## Risks & Mitigations
- **Increased concurrency**: Virtual threads may spawn hundreds of concurrent tile loads. Rely on existing generation-based invalidation plus `TileManager`'s pruning of in-flight requests to avoid runaway memory use. If tests show regressions, add guardrails (separate change).
- **FX Thread Violations**: Tests will assert that `TileRetriever` is never invoked on the FX thread and that UI callbacks stay on FX thread, preventing regressions.

## Migration Notes
- Build tooling already targets Java 21; we will update specs and docs to state this explicitly. No additional migration is required for library consumers already on Java 21.
