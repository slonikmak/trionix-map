# Change: Add Polyline Layer

## Why
Users need to draw lines on the map to represent routes, boundaries, or paths. The current system only supports point markers. This change adds a dedicated layer for rendering interactive polylines with custom styling and editable vertices.

## What Changes
- Add `PolylineLayer` extending `MapLayer`.
- Support adding multiple `Polyline` objects to the layer.
- Support custom styling (color, width, dash pattern) for lines.
- Support optional markers at polyline vertices.
- Support interactive editing of polyline vertices (drag to move).

## Impact
- **New Capability**: `polyline-layer`
- **Affected Code**: New classes in `com.trionix.maps.layer`.
