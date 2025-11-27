# Change: Add Grid Layer and Scale Ruler Widget

## Why
Map applications often need visual reference aids for users to understand distances and locations. A coordinate grid overlay helps users quickly identify map regions and understand spatial relationships, while a scale ruler provides an intuitive visual indicator of real-world distances at the current zoom level. These two elements should be synchronized so that the grid spacing corresponds to the ruler's displayed distance.

## What Changes
- Add new `GridLayer` class for displaying a configurable coordinate grid on the map
- Add new `ScaleRulerControl` widget for displaying a visual scale bar
- Grid layer configuration: step (in degrees), line color, and line width
- Scale ruler configuration: preferred width, metric/imperial units
- Synchronization between grid step and scale ruler distance
- Demo updates to toggle visibility and configure both components

## Impact
- Affected specs: 
  - New `grid-layer` capability (similar pattern to `point-marker-layer` and `polyline-layer`)
  - New `scale-ruler-widget` capability
- Affected code:
  - `trionix-map-core/src/main/java/com/trionix/maps/layer/GridLayer.java` (new)
  - `trionix-map-core/src/main/java/com/trionix/maps/control/ScaleRulerControl.java` (new)
  - `trionix-map-demo/src/main/java/com/trionix/maps/samples/AdvancedMapExample.java` (modified)
