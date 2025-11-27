# Tasks: Add Grid Layer and Scale Ruler Widget

## 1. Core Distance Utilities
- [x] 1.1 Add `DistanceUtils` class with haversine distance calculation
- [x] 1.2 Add method to calculate meters-per-pixel at given lat/zoom
- [x] 1.3 Unit tests for distance calculations

## 2. Grid Layer Implementation
- [x] 2.1 Create `GridLayer` class extending `MapLayer`
- [x] 2.2 Implement configurable properties: `stepDegrees`, `strokeColor`, `strokeWidth`
- [x] 2.3 Implement automatic step calculation based on zoom level
- [x] 2.4 Render grid lines using `Canvas` in `layoutLayer()`
- [x] 2.5 Add visibility toggle (`setVisible()` inherited from Node)
- [x] 2.6 Unit tests for `GridLayer`

## 3. Scale Ruler Control Implementation
- [x] 3.1 Create `ScaleRulerControl` class as JavaFX `Region`
- [x] 3.2 Implement binding to `MapView` (center lat, zoom)
- [x] 3.3 Implement distance label formatting (metric by default)
- [x] 3.4 Render ruler bar with distance text
- [x] 3.5 Add configurable properties: `preferredWidth`, `unit`
- [x] 3.6 Unit tests for `ScaleRulerControl`

## 4. Synchronization
- [x] 4.1 Ensure grid step and ruler distance use consistent calculations
- [x] 4.2 Add helper to get "nice" distance values (1m, 5m, 10m, 50m, 100m, 500m, 1km, etc.)
- [x] 4.3 Integration test verifying consistency

## 5. Demo Integration
- [x] 5.1 Add `GridLayer` to `AdvancedMapExample`
- [x] 5.2 Add `ScaleRulerControl` overlay to demo
- [x] 5.3 Add toggle buttons for grid and ruler visibility
- [x] 5.4 Add configuration controls (grid color, width, step mode)
- [x] 5.5 Verify visual consistency between grid and ruler

## Dependencies
- Tasks 2.* and 3.* depend on 1.* (distance utilities)
- Task 4.* depends on 2.* and 3.*
- Task 5.* depends on 2.*, 3.*, and 4.*

## Post-implementation notes (2025-11-27)

- ✅ Major refactor: the `GridLayer` was redesigned from a geographic (latitude/longitude) grid to a metric, screen-space grid. The grid now renders in pixel space with cell sizes that represent fixed metric distances (meters/kilometers) rather than fixed degree intervals.
- 🔧 Removed obsolete API: `stepDegrees` and its helpers (calculateAutoStep, getNiceDegreeStep) were removed because the grid no longer uses degree-based steps.
- 🎯 Synchronization: `GridLayer` spacing is computed from `DistanceUtils` (meters-per-pixel at current center latitude and zoom) and is aligned with the `ScaleRulerControl`. The ruler width target is 150px and grid cell size uses the same "nice" metric distance values (e.g., 100 m, 500 m, 1 km, etc.).
- 🧩 Visual/UX improvements:
	- Both `GridLayer` and `ScaleRulerControl` are hidden by default in the demo (off by default).
	- Scale ruler was redesigned to be visually non-intrusive: no full opaque background, checkered black/white segments and a subtle text background for readability.
	- Grid visuals use translucent stroke color and configurable stroke width.
- 🧪 Tests & verification: updated unit tests and demo code to remove degree-step controls and reflect the new automatic metric behavior. All existing tests in the project pass after the refactor (full test run: 76 tests passing on 2025-11-27).
- 🔁 Demo updates: `AdvancedMapExample` was cleaned up — manual `stepDegrees` UI removed, replaced by automatic grid behavior and a grid color picker.

Notes and rationale:
- The metric screen-space grid improves visual scale perception because a cell now corresponds to a predictable physical size regardless of map center latitude. This better matches the purpose of a scale ruler and simplifies the API for end users.

If you want, I can:
- Add a small demo toggle to switch between geographic and metric grids for comparison (separate optional feature).
- Update documentationSPEC files or add diagrams showing how pixel spacing and meters-per-pixel interact at different zooms.
