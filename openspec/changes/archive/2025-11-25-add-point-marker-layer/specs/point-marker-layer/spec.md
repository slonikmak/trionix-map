# point-marker-layer Specification

## Purpose
Provide a reusable `PointMarkerLayer` for displaying customizable, interactive markers at geographic coordinates on a `MapView`. This layer supports any JavaFX `Node` as marker visual, programmatic and interactive (drag-and-drop) repositioning, click callbacks, and marker removal.

## ADDED Requirements

### Requirement: PointMarkerLayer Class
The system SHALL provide a `PointMarkerLayer` class extending `MapLayer` for displaying point markers on the map.

#### Scenario: Create empty marker layer
- **WHEN** a developer instantiates `new PointMarkerLayer()`
- **THEN** a valid `MapLayer` instance is created with no markers
- **AND** it can be added to `MapView.getLayers()`

#### Scenario: Add marker with geographic coordinates
- **WHEN** a developer calls `layer.addMarker(latitude, longitude, node)`
- **THEN** the marker is stored and displayed at the specified coordinates
- **AND** `layoutLayer()` positions the node correctly on the map

### Requirement: Customizable Marker Appearance
The system SHALL allow any JavaFX `Node` to be used as a marker visual.

#### Scenario: Use Region as marker
- **WHEN** a developer provides a `Region` (e.g., `Label`, `StackPane`) to `addMarker()`
- **THEN** the provided node is used as the marker visual without modification

#### Scenario: Use Shape as marker
- **WHEN** a developer provides a `Shape` (e.g., `Circle`, `SVGPath`) to `addMarker()`
- **THEN** the provided node is used as the marker visual

#### Scenario: Marker anchor point
- **WHEN** a marker node is positioned on the map
- **THEN** the marker SHALL be centered horizontally at the coordinate
- **AND** the marker's bottom edge SHALL align with the coordinate by default

### Requirement: Programmatic Marker Repositioning
The system SHALL allow programmatic update of marker geographic coordinates.

#### Scenario: Update marker location via API
- **WHEN** a developer calls `marker.setLocation(newLatitude, newLongitude)`
- **THEN** the marker's coordinates are updated
- **AND** `requestLayerLayout()` is called automatically
- **AND** the marker moves to the new position on the next layout pass

#### Scenario: Get current marker location
- **WHEN** a developer calls `marker.getLatitude()` and `marker.getLongitude()`
- **THEN** the current geographic coordinates are returned

### Requirement: Interactive Drag-and-Drop Repositioning
The system SHALL support interactive marker repositioning via mouse drag.

#### Scenario: Enable drag mode
- **WHEN** a developer calls `marker.setDraggable(true)`
- **THEN** the marker responds to mouse drag events

#### Scenario: Drag marker to new location
- **WHEN** a user drags a draggable marker on the map
- **THEN** the marker follows the mouse cursor
- **AND** on mouse release, the marker's coordinates are updated to the new geographic location

#### Scenario: Disable drag mode
- **WHEN** a developer calls `marker.setDraggable(false)`
- **THEN** the marker no longer responds to drag events
- **AND** the marker remains at its current position

#### Scenario: Drag does not trigger map pan
- **WHEN** a user drags a draggable marker
- **THEN** the map SHALL NOT pan in response to the drag gesture
- **SO THAT** only the marker moves

### Requirement: Click Callback Support
The system SHALL support click event callbacks on markers.

#### Scenario: Set click handler
- **WHEN** a developer calls `marker.setOnClick(handler)`
- **THEN** the handler is invoked when the marker is clicked

#### Scenario: Click event provides marker reference
- **WHEN** a marker click handler is invoked
- **THEN** it receives the clicked `PointMarker` instance
- **SO THAT** the handler can identify which marker was clicked

#### Scenario: No click handler set
- **WHEN** a marker has no click handler
- **AND** the user clicks on the marker
- **THEN** no action is taken

### Requirement: Marker Removal
The system SHALL support removing markers from the layer.

#### Scenario: Remove specific marker
- **WHEN** a developer calls `layer.removeMarker(marker)`
- **THEN** the marker is removed from the layer
- **AND** its visual node is removed from the scene graph
- **AND** the marker is no longer positioned during layout

#### Scenario: Remove all markers
- **WHEN** a developer calls `layer.clearMarkers()`
- **THEN** all markers are removed from the layer
- **AND** all visual nodes are removed from the scene graph

#### Scenario: Remove marker by reference returns success
- **WHEN** a developer calls `layer.removeMarker(marker)` with a valid marker
- **THEN** the method returns `true`
- **WHEN** a developer calls `layer.removeMarker(marker)` with an unknown marker
- **THEN** the method returns `false`

### Requirement: Marker Visibility
The system SHALL support showing and hiding individual markers.

#### Scenario: Hide marker
- **WHEN** a developer calls `marker.setVisible(false)`
- **THEN** the marker's visual node is hidden
- **AND** the marker does not respond to click or drag events

#### Scenario: Show marker
- **WHEN** a developer calls `marker.setVisible(true)`
- **THEN** the marker's visual node is displayed
- **AND** the marker responds to events as configured

### Requirement: Thread Safety
The system SHALL ensure all marker operations respect JavaFX threading rules.

#### Scenario: Add marker from FX thread
- **WHEN** `addMarker()` is called from the JavaFX Application Thread
- **THEN** the marker is added immediately

#### Scenario: Coordinate updates from background thread
- **WHEN** marker coordinates are updated from a non-FX thread
- **THEN** the visual update is marshalled to the JavaFX Application Thread via `Platform.runLater()`
