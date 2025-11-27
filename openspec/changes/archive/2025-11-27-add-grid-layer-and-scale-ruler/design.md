# Design: Grid Layer and Scale Ruler Widget

## Context
Users need visual reference aids on the map:
1. A coordinate grid to understand geographic location and spatial relationships
2. A scale ruler to understand real-world distances at the current zoom level

Both elements should work together - the grid spacing should correlate with the scale ruler's displayed distance for consistent user experience.

## Goals / Non-Goals

### Goals
- Provide a `GridLayer` extending `MapLayer` for rendering latitude/longitude grid lines
- Provide a `ScaleRulerControl` as a JavaFX control overlaying the map
- Make grid configurable: step (degrees), line color, line width
- Make ruler configurable: preferred width (pixels), display units
- Calculate grid step based on zoom level for optimal readability
- Synchronize grid step with ruler distance display

### Non-Goals
- Projections other than Web Mercator (out of MVP scope)
- Grid labels (lat/lon values at grid lines) - can be added later
- Customizable ruler appearance (beyond basic styling)
- User-interactive resizing of ruler

## Decisions

### GridLayer Architecture
- **Decision**: Extend `MapLayer` and use `Canvas` for rendering grid lines
- **Rationale**: Canvas provides efficient rendering for potentially many lines without creating separate JavaFX nodes; follows performance guidelines from `map-layer-guidelines`
- **Alternatives considered**:
  - Use `javafx.scene.shape.Line` nodes: Would create many nodes, potentially impacting performance at fine grid steps
  - Use `Polyline`: Less control over individual line styling

### Grid Step Calculation
- **Decision**: Provide automatic step calculation based on zoom level with "nice" intervals (1°, 5°, 10°, 15°, 30°, etc.), but allow manual override
- **Rationale**: Automatic calculation ensures readable grid at all zoom levels; manual override gives developers control
- **Alternatives considered**:
  - Fixed step only: Would result in too dense or too sparse grids at different zoom levels

### ScaleRulerControl Architecture
- **Decision**: Implement as a standalone JavaFX `Control` (not a `MapLayer`) that observes `MapView` properties
- **Rationale**: The ruler is a UI control overlay, not a geographic layer; it doesn't participate in map coordinate space
- **Alternatives considered**:
  - Extend `MapLayer`: Semantically incorrect as ruler doesn't project to geographic coordinates

### Synchronization Approach
- **Decision**: Both components calculate their display values from `MapView.zoomProperty()` independently using shared distance calculation utilities
- **Rationale**: Loose coupling; ruler and grid can be used independently; shared math ensures consistency
- **Alternatives considered**:
  - Direct binding between components: Would force users to always use both together

### Distance Calculation
- **Decision**: Use haversine formula for accurate distance at current map center latitude
- **Rationale**: Web Mercator distortion varies with latitude; haversine provides accurate ground distances
- **Alternatives considered**:
  - Simple pixel-to-meter ratio: Would be inaccurate at high latitudes

## Risks / Trade-offs

- **Risk**: Grid rendering performance at low zoom levels with fine step
  - **Mitigation**: Clamp minimum step based on zoom level; use Canvas for efficient batch rendering
  
- **Risk**: Scale ruler accuracy varies across viewport
  - **Mitigation**: Calculate distance at map center; document that scale is approximate for large viewports

- **Trade-off**: Auto vs manual grid step
  - Chose auto with manual override to serve both ease-of-use and advanced control needs

## Open Questions
- Should grid support different colors for major/minor lines? (Recommend: defer to future enhancement)
- Should ruler support both metric and imperial simultaneously? (Recommend: single unit mode, switchable)
