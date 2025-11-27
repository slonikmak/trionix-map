# scale-ruler-widget Specification

## Purpose
TBD - created by archiving change add-grid-layer-and-scale-ruler. Update Purpose after archive.
## Requirements
### Requirement: ScaleRulerControl Class
The system SHALL provide a `ScaleRulerControl` class as a JavaFX `Region` for displaying a scale ruler.

#### Scenario: Create scale ruler
- **WHEN** a developer instantiates `new ScaleRulerControl(mapView)`
- **THEN** a valid JavaFX control is created and linked to the specified `MapView`

#### Scenario: Add ruler to scene
- **WHEN** a `ScaleRulerControl` is added to a scene (e.g., as overlay on `StackPane` containing `MapView`)
- **THEN** the ruler is displayed and shows the current scale

### Requirement: Scale Calculation
The system SHALL calculate the displayed distance based on the map's current zoom level and center latitude.

#### Scenario: Update on zoom change
- **WHEN** the `MapView` zoom level changes
- **THEN** the `ScaleRulerControl` recalculates and updates the displayed distance

#### Scenario: Latitude-aware calculation
- **WHEN** the scale is calculated
- **THEN** the calculation SHALL account for Web Mercator distortion at the map's center latitude
- **SO THAT** the displayed distance is accurate for the visible map region

#### Scenario: Nice distance values
- **WHEN** the scale ruler calculates the display distance
- **THEN** it SHALL round to "nice" values (e.g., 1m, 5m, 10m, 50m, 100m, 500m, 1km, 5km, 10km, 50km, 100km)
- **SO THAT** the ruler is easy to read and interpret

### Requirement: Ruler Visual Appearance
The system SHALL render a horizontal bar with distance text.

#### Scenario: Display distance label
- **WHEN** the scale ruler is rendered
- **THEN** a text label shows the distance (e.g., "500 m", "1 km", "10 km")

#### Scenario: Display ruler bar
- **WHEN** the scale ruler is rendered
- **THEN** a horizontal bar is displayed whose pixel length corresponds to the labeled distance

#### Scenario: Default appearance
- **WHEN** a `ScaleRulerControl` is created with default settings
- **THEN** the ruler has a clean, minimal appearance suitable for map overlays

### Requirement: Ruler Width Configuration
The system SHALL allow configuration of the preferred ruler width.

#### Scenario: Set preferred width
- **WHEN** a developer calls `scaleRuler.setPreferredWidth(150)`
- **THEN** the ruler bar width is approximately 150 pixels (adjusted to match a "nice" distance value)

#### Scenario: Default width
- **WHEN** a `ScaleRulerControl` is created with default settings
- **THEN** the preferred ruler width is approximately 100 pixels

### Requirement: Unit Configuration
The system SHALL support metric and imperial distance units.

#### Scenario: Metric units
- **WHEN** a developer calls `scaleRuler.setUnit(ScaleUnit.METRIC)`
- **THEN** distances are displayed in meters and kilometers (e.g., "500 m", "1 km")

#### Scenario: Imperial units
- **WHEN** a developer calls `scaleRuler.setUnit(ScaleUnit.IMPERIAL)`
- **THEN** distances are displayed in feet and miles (e.g., "500 ft", "1 mi")

#### Scenario: Default unit
- **WHEN** a `ScaleRulerControl` is created with default settings
- **THEN** metric units are used by default

### Requirement: Visibility Toggle
The system SHALL support showing and hiding the scale ruler.

#### Scenario: Hide ruler
- **WHEN** a developer calls `scaleRuler.setVisible(false)`
- **THEN** the ruler is not displayed

#### Scenario: Show ruler
- **WHEN** a developer calls `scaleRuler.setVisible(true)` after hiding
- **THEN** the ruler is displayed again with current scale

### Requirement: Observable Properties
The system SHALL expose ruler configuration as observable JavaFX properties.

#### Scenario: Observe current distance
- **WHEN** a developer binds to `scaleRuler.distanceMetersProperty()`
- **THEN** the binding updates when the displayed distance changes

#### Scenario: Observe display text
- **WHEN** a developer binds to `scaleRuler.displayTextProperty()`
- **THEN** the binding provides the formatted distance string (e.g., "500 m")

