# Change: Add Reusable PointMarkerLayer to Core Library

## Why
The demo module contains a useful `MarkerLayer` implementation that places `Region` nodes on the map at geographic coordinates. This pattern is common across map applications. Extracting it into a reusable `PointMarkerLayer` in the core library will eliminate code duplication and provide users with a ready-to-use layer for markers with customization and interaction support.

## What Changes
- Add `PointMarkerLayer` class to `trionix-map-core` under `com.trionix.maps.layer` package
- Add `PointMarker` record/class representing a single marker with latitude, longitude, and visual node
- Support customizable marker appearance (any `Node` can be used as marker visual)
- Support programmatic marker location updates via API
- Support interactive drag-and-drop marker repositioning
- Add click callback support for marker interaction
- Add marker removal capability
- Update demo samples to use the new library class instead of inline implementation

## Impact
- Affected specs: layer-system, map-layer-guidelines (follows existing patterns)
- New spec: point-marker-layer (new capability)
- Affected code: 
  - `trionix-map-core/src/main/java/com/trionix/maps/layer/` (new classes)
  - `trionix-map-demo/src/main/java/com/trionix/maps/samples/` (refactor to use new class)
