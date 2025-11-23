# Project Context

## Purpose
Deliver a focused JavaFX component library that embeds OpenStreetMap raster tiles inside desktop applications. The MVP goal is a polished `MapView` control with built-in pan/zoom, smooth fly-to animations, and an extensible layer system while intentionally omitting complex configuration, disk caching, or legacy compatibility requirements.

## Tech Stack
- Language: Java 21+ (LTS) with modular builds, JavaFX 21+.
- UI Toolkit: JavaFX (Region-based control hierarchy, CSS support).
- Concurrency: `CompletableFuture`, `ExecutorService`, `VirtualThread` and JavaFX Application Thread coordination.
- Networking: `java.net.http.HttpClient` for HTTPS tile download with configurable timeouts and user-agent.
- Imaging: JavaFX `Image`/`ImageView` pipeline with 256×256 PNG tiles.
- Build/Test: Maven + JUnit 5 (unit + integration harness).

## Project Conventions

### Code Style
- Follow Google Java Style (4-space indent, upper camel case types, lower camel case members).
- Prefer `final` classes/methods for public API types unless extension is expected (e.g., `MapLayer`).
- Avoid reflection, ServiceLoader, or implicit configuration. Every dependency must be constructor-injected.
- Keep public API surface minimal; internal helpers live under `internal` packages and stay package-private where possible.
- All asynchronous callbacks must guard JavaFX state via `Platform.runLater` and document thread expectations in Javadoc.
- Use record types for simple data carriers (e.g., `TileCoordinate`, `GeoPoint`).

### Architecture Patterns
- Core control `MapView` is a `Region` that delegates to internal `MapState`, `Projection`, and `TileManager` services.
- Tile pipeline follows a clean separation: state calculation → cache lookup → async retrieval → UI update.
- Layering uses composition: `MapLayer` subclasses (extending `Pane`) render on top of tiles; their lifecycle hooks (`layerAdded`, `layoutLayer`, `layerRemoved`) keep coupling low.
- Configuration is code-only: constructors accept strategy interfaces (`TileRetriever`, `TileCache`) for future extensibility without configuration files.
- Executors are centralized (`TileExecutors`) to limit thread creation and keep shutdown semantics simple during MVP.

### Engineering Principles
- KISS: keep features minimal, prefer straightforward control flow, and default to built-in JavaFX/Java libraries before introducing abstractions.
- DRY: consolidate shared math, tile logic, and network helpers into reusable utilities to avoid divergence between layers or future capabilities.
- SOLID: honor single-responsibility boundaries (e.g., `MapView` vs `TileManager`), rely on interfaces for key behaviors (`TileRetriever`, `TileCache`), and keep dependencies inverted so alternate implementations can plug in without refactoring.

### Testing Strategy
- Unit tests cover projection math, `MapState` normalization, tile visibility calculations, LRU cache behavior, and generation-based invalidation in `TileManager`.
- Concurrency-sensitive components (cache, tile manager) require stress-style tests that assert thread safety.
- Integration smoke tests spin up a headless JavaFX app, instantiate `MapView`, and verify tile loading/pan/zoom interactions using mocked retrievers.
- Manual exploratory testing validates 60 FPS pan/zoom targets on reference hardware and visually inspects placeholder handling for failed tiles.

### Git Workflow
- Trunk-based with short-lived feature branches named `feature/<topic>` or `fix/<bug>`; rebase before merge.
- Commits follow Conventional Commits (`feat:`, `fix:`, `docs:`, etc.) referencing OpenSpec change IDs when relevant.
- Specs and implementation evolve together: no code merge without corresponding spec/task updates when behavior changes.
- Pull requests must include `openspec validate --strict` output and test results in the description.

## Domain Context
The library renders Web Mercator tiles from OpenStreetMap. Latitude is clamped to roughly ±85°; longitude wraps at ±180°. Zoom is a double but maps to discrete tile zoom levels (rounded). Tiles are always 256×256 pixels. Pan/zoom gestures are mouse-centric (drag + scroll) with optional touch support supplied by JavaFX. Layers provide a hook for overlays such as markers, paths, or heatmaps without altering base tile logic.

## Important Constraints
- MVP must not introduce disk caching, environment-based configuration, or compatibility layers with existing map libraries.
- All configuration happens through constructors/APIs; no system properties or external files.
- Maintain smooth interaction (~60 FPS) by limiting simultaneous tile loads and bounding in-memory cache size via LRU.
- Public API stability: `MapView`, `MapLayer`, `TileRetriever`, and `TileCache` form the contract; breaking changes require new OpenSpec proposals.
- Respect OpenStreetMap usage policy: friendly user-agent, reasonable parallel requests, and optional throttling.

## External Dependencies
- OpenStreetMap standard tile server (`https://tile.openstreetmap.org/{z}/{x}/{y}.png`) as the default raster source.
- JavaFX runtime for UI composition, animation, and image handling.
- `java.net.http.HttpClient` for HTTPS tile retrieval with configurable timeouts and headers.
- Optional test doubles (e.g., WireMock/MockWebServer) to simulate tile endpoints during automated tests.
