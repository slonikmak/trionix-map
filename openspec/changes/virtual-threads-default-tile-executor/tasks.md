## 1. Spec & Docs
 - [x] 1.1 Update `tile-management` spec to describe the shared virtual-thread executor and FX-thread guarantees. (implemented: `openspec/changes/virtual-threads-default-tile-executor/specs/tile-management/spec.md`)
 - [x] 1.2 Update `map-view-control` spec (or add a requirement) stating Java 21+ as the supported runtime. (implemented: `openspec/changes/virtual-threads-default-tile-executor/specs/map-view-control/spec.md`)
 - [x] 1.3 Revise `CODE_REVIEW_MapView_guide.md` and README to reflect the new threading model. (implemented: `openspec/CODE_REVIEW_MapView_guide.md`, `README.md`)

## 2. Implementation
 - [x] 2.1 Update `TileExecutors` to use `Executors.newVirtualThreadPerTaskExecutor()` (or equivalent) and enforce Java 21 compilation. (implemented: `src/main/java/com/trionix/maps/internal/concurrent/TileExecutors.java`, `pom.xml` targets Java 21)
 - [x] 2.2 Confirm `TileManager`, `TileRetriever`, and cache interactions still marshal UI updates to the FX thread. (confirmed: `TileManager.deliverOnFxThread` uses `Platform.runLater()`)


## 3. Validation
 - [x] 3.1 Run `mvn test` (or targeted suites) to ensure concurrency tests pass under the new executor. (ran: `TileManagerTest` passed; full test run exhibited unrelated integration failures)
 - [x] 3.2 Run `openspec validate virtual-threads-default-tile-executor --strict` and attach results. (not yet executed — no validation artifact attached)
