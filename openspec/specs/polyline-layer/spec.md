# polyline-layer Specification

## Purpose
TBD - created by archiving change add-polyline-layer. Update Purpose after archive.
## Requirements
### Requirement: PolylineLayer Class
The system SHALL provide a `PolylineLayer` class extending `MapLayer` for displaying polylines on the map.

#### Scenario: Create empty polyline layer
- **WHEN** a developer instantiates `new PolylineLayer()`
- **THEN** a valid `MapLayer` instance is created with no polylines
- **AND** it can be added to `MapView.getLayers()`

#### Scenario: Add polyline with coordinates
- **WHEN** a developer creates a `Polyline` with a list of `GeoPoint`s
- **AND** adds it to the `PolylineLayer`
- **THEN** the polyline is stored and displayed connecting the coordinates
- **AND** `layoutLayer()` positions the line segments correctly on the map

### Requirement: Polyline Styling
The system SHALL allow customization of polyline visual appearance.

#### Scenario: Set stroke color and width
- **WHEN** a developer sets the stroke color and width on a `Polyline`
- **THEN** the rendered line on the map reflects these properties

#### Scenario: Set stroke dash pattern
- **WHEN** a developer sets a dash array (e.g., `[10.0, 5.0]`) on a `Polyline`
- **THEN** the rendered line displays the dash pattern

### Requirement: Vertex Markers
The system SHALL support displaying markers at polyline vertices.

#### Scenario: Enable vertex markers
- **WHEN** a developer enables markers for a `Polyline`
- **THEN** a visual marker is rendered at each coordinate of the polyline

#### Scenario: Custom marker node
- **WHEN** a developer provides a custom `Node` factory for vertices
- **THEN** that node type is used for each vertex marker

### Requirement: Interactive Vertex Editing
The system SHALL support interactive modification of polyline geometry via vertex dragging.

#### Scenario: Drag vertex to move
- **WHEN** a user drags a vertex marker of an editable `Polyline`
- **THEN** the marker follows the mouse
- **AND** the connected line segments update in real-time to follow the marker
- **AND** the underlying `GeoPoint` for that vertex is updated on release

#### Scenario: Disable editing
- **WHEN** a `Polyline` is set to non-editable
- **THEN** vertices cannot be dragged
- **AND** markers (if visible) do not respond to mouse events

### Requirement: Programmatic Geometry Updates
The system SHALL allow programmatic updates to polyline coordinates.

#### Scenario: Update coordinates list
- **WHEN** a developer modifies the list of coordinates for a `Polyline`
- **THEN** the map visualization updates to reflect the new path

