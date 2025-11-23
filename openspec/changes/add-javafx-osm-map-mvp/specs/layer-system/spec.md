# Layer System

## ADDED Requirements

### Requirement: MapLayer Base Class
The system SHALL provide an abstract `MapLayer` class (extending `Pane`) for custom overlays.

#### Scenario: Define layer contract
- **WHEN** a developer extends `MapLayer`
- **THEN** they must implement the abstract `layoutLayer(MapView mapView)` method

#### Scenario: Override lifecycle hooks
- **WHEN** a developer extends `MapLayer`
- **THEN** they can optionally override `layerAdded(MapView mapView)` and `layerRemoved(MapView mapView)` with default no-op implementations

### Requirement: Layer Addition Lifecycle
The system SHALL notify layers when added to a `MapView`.

#### Scenario: Call layerAdded on addition
- **WHEN** a `MapLayer` is added to `mapView.getLayers()`
- **THEN** the layer's `layerAdded(mapView)` method is called on the JavaFX Application Thread

#### Scenario: Initialize layer resources
- **WHEN** `layerAdded()` is called
- **THEN** the layer can initialize JavaFX nodes, bind to map properties, or set up listeners

### Requirement: Layer Removal Lifecycle
The system SHALL notify layers when removed from a `MapView`.

#### Scenario: Call layerRemoved on removal
- **WHEN** a `MapLayer` is removed from `mapView.getLayers()`
- **THEN** the layer's `layerRemoved(mapView)` method is called on the JavaFX Application Thread

#### Scenario: Clean up layer resources
- **WHEN** `layerRemoved()` is called
- **THEN** the layer can dispose resources, unbind properties, or remove listeners

### Requirement: Layer Layout Updates
The system SHALL trigger layer layout when map state changes.

#### Scenario: Call layoutLayer on viewport change
- **WHEN** the map's center, zoom, or viewport size changes
- **THEN** `layoutLayer(mapView)` is called for all layers in the list

#### Scenario: Batch layout updates per frame
- **WHEN** multiple map state changes occur within a single JavaFX pulse
- **THEN** `layoutLayer()` is called at most once per layer per pulse

#### Scenario: Access map state during layout
- **WHEN** `layoutLayer(mapView)` is called
- **THEN** the layer can read `mapView.getCenterLat()`, `mapView.getCenterLon()`, `mapView.getZoom()`, and viewport dimensions to position nodes

### Requirement: Manual Layout Request
The system SHALL allow layers to request layout updates.

#### Scenario: Request layout from layer
- **WHEN** a layer calls `requestLayerLayout()`
- **THEN** `layoutLayer()` is scheduled for the next JavaFX pulse

#### Scenario: Update layer content on data change
- **WHEN** a layer's internal data changes (e.g., marker positions)
- **THEN** the layer calls `requestLayerLayout()` to trigger repositioning

### Requirement: Layer Rendering Order
The system SHALL render layers in list order above the base tile layer.

#### Scenario: Render first layer closest to tiles
- **WHEN** multiple layers exist in `getLayers()`
- **THEN** the layer at index 0 renders directly above the tiles

#### Scenario: Render last layer on top
- **WHEN** multiple layers exist in `getLayers()`
- **THEN** the layer at the highest index renders on top of all other layers

### Requirement: Thread Safety for Layer Methods
The system SHALL invoke all layer lifecycle and layout methods on the JavaFX Application Thread.

#### Scenario: Call layoutLayer on JavaFX thread
- **WHEN** `layoutLayer()` is invoked
- **THEN** it executes on the JavaFX Application Thread

#### Scenario: Avoid blocking in layoutLayer
- **WHEN** a layer implements `layoutLayer()`
- **THEN** heavy computation or I/O should be offloaded to background threads, with UI updates via `Platform.runLater()`
