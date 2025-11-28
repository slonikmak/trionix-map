# map-view-control Specification (Delta: 2025-11-27-add-double-click-zoom)

## Purpose
Introduce double-click zoom interaction to improve usability and align with common map control conventions.

## ADDED Requirements

### Requirement: Mouse Double-Click Zoom Interaction
The system SHALL support zooming in the map by double-clicking the primary mouse button within the `MapView` viewport, focusing zoom around the cursor's geographic location.

#### Scenario: Double-click zoom in
- **WHEN** a user double-clicks the primary mouse button at screen position (x, y) within the map and current zoom is below max
- **THEN** the zoom level increases by 1.0 (or clamped to max)
- **AND** the geographic point under the cursor remains under the cursor (allowing <=2 pixels drift)

#### Scenario: Double-click at max zoom
- **WHEN** a user double-clicks and the current zoom is already at maximum
- **THEN** the zoom level does not change

#### Scenario: Double-click zoom disabled
- **WHEN** a developer has disabled double-click zoom via a configuration flag/property
- **AND** a user double-clicks the map
- **THEN** no zoom action occurs

#### Scenario: Event consumed by layer
- **WHEN** a layer intercepts and consumes the double-click event (e.g., for selection)
- **THEN** the map does not perform the zoom

#### Scenario: Center preservation tolerance
- **WHEN** a double-click zoom occurs
- **THEN** the geographic coordinate at the cursor before zoom maps to a screen position within ±2 pixels of the original cursor position after zoom

### Requirement: Double-Click Zoom Behavior
Double-click zoom SHALL apply immediately by default. When a double-click zoom occurs the map SHALL change zoom level by +1 (clamped at max) and preserve the geographic point under the cursor within ±2 pixel tolerance.

#### Scenario: Immediate behavior & center preservation
- **WHEN** a user double-clicks the primary mouse button at screen position (x, y) within the map and the map supports a change in zoom
- **THEN** the map immediately increments zoom by +1 (or clamps to max)
- **AND** the geographic coordinate under the cursor prior to zoom maps to a screen position within ±2 pixels of the original cursor position after the zoom
- **AND** the change is applied even when no animation subsystem exists (instantaneous fallback)
