## Context
The `PolylineLayer` needs to render lines that scale and move with the map. Unlike point markers which are single nodes, a polyline spans across the map space.

## Goals
- Efficient rendering of lines.
- Smooth interaction when dragging vertices.
- Flexible styling.

## Decisions
- **Rendering**: Use JavaFX `Polyline` nodes. Since the map can wrap or be very large, we might need to handle clipping or segmentation, but for MVP we will assume a single `Polyline` node per logical polyline, re-calculated on layout.
- **Coordinate System**: Convert GeoPoints to pixel coordinates relative to the layer's local coordinate system (which matches the MapView's).
- **Vertex Markers**: These will be separate `Node`s (like `Circle` or custom) managed by the `PolylineLayer` alongside the `Polyline` node. They need to stay in sync with the line points.
- **Interaction**: Mouse events on vertex markers will trigger updates to the `Polyline` points and the underlying data model.

## Risks
- **Performance**: Rebuilding large `Polyline` nodes on every frame/pan might be slow.
    - *Mitigation*: Only update points on pan/zoom. Use `Managed` false to avoid full scene graph layout passes if possible, or just rely on `layoutLayer`.
- **Projection**: Long lines on a Mercator projection might look straight in pixels but should be curved (geodesic).
    - *Decision*: For MVP, we draw straight lines between points in the projected pixel space (Mercator straight lines). This is standard for web maps (Leaflet/Google Maps default behavior).

## Open Questions
- Should we support adding/removing vertices by clicking on the line?
    - *Decision*: Out of scope for this change. Only moving existing vertices.
