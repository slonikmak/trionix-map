# Capability: Map Layer Guidelines

## Purpose

This capability defines guidelines and constraints for implementing `MapLayer` overlays on top of `MapView`. It focuses on code structure, JavaFX threading rules, and performance best practices so that new layers behave consistently and do not degrade map interaction.

## Requirements

### Requirement: MapLayer Lifecycle and Ownership
`MapLayer` instances SHALL integrate with `MapView` through a clear lifecycle and ownership model.

#### Scenario: Layer attaches to MapView
- **WHEN** a `MapLayer` is added to `MapView.getLayers()`
- **THEN** `MapView` SHALL call `attachToMapView(MapView)` on the layer
- **AND** SHALL add the layer node into its internal `layerPane` at the corresponding index
- **AND** SHALL invoke `layerAdded(MapView)` on the JavaFX Application Thread.

#### Scenario: Layer detaches from MapView
- **WHEN** a `MapLayer` is removed from `MapView.getLayers()`
- **THEN** `MapView` SHALL invoke `layerRemoved(MapView)` on the JavaFX Application Thread
- **AND** SHALL call `detachFromMapView(MapView)` to clear the owner reference
- **AND** SHALL remove the layer node from its internal `layerPane`.

#### Scenario: Single MapView ownership
- **WHEN** a `MapLayer` is already attached to a `MapView`
- **AND** an attempt is made to attach it to a different `MapView`
- **THEN** the operation SHALL fail with an `IllegalStateException`.

### Requirement: MapLayer Layout Contract
`MapLayer` subclasses SHALL implement the `layoutLayer(MapView)` method to position and update their visuals relative to the current map state.

#### Scenario: Layout called during MapView layout pass
- **WHEN** `MapView` performs its layout pass (`layoutChildren`)
- **THEN** `MapView` SHALL resize and relocate each attached `MapLayer` to cover the full viewport size
- **AND** SHALL call `layoutLayer(MapView)` for each layer on the JavaFX Application Thread.

#### Scenario: Layout is light-weight
- **WHEN** `layoutLayer(MapView)` executes
- **THEN** it SHOULD avoid blocking I/O and heavy computations
- **AND** it SHOULD minimize allocations and node churn
- **SO THAT** map panning, zooming, and animation remain smooth at interactive frame rates.

### Requirement: Layer Layout Requests
`MapLayer` subclasses SHALL use `requestLayerLayout()` to schedule layout updates instead of directly manipulating `MapView` layout.

#### Scenario: Request layer layout from FX thread
- **WHEN** a `MapLayer` running on the JavaFX Application Thread needs to reflect data changes
- **THEN** it MAY call `requestLayerLayout()`
- **AND** `requestLayerLayout()` SHALL call `requestLayout()` on the owning `MapView`
- **SO THAT** `layoutLayer(MapView)` is invoked on the next JavaFX pulse.

#### Scenario: Request layer layout from background thread
- **WHEN** a `MapLayer` receives data or signals from a non-FX thread
- **THEN** calling `requestLayerLayout()` SHALL marshal the underlying `MapView.requestLayout()` call onto the JavaFX Application Thread
- **SO THAT** no scene-graph APIs are invoked off the FX thread.

### Requirement: JavaFX Thread Safety
All scene graph mutations performed by `MapLayer` SHALL occur on the JavaFX Application Thread.

#### Scenario: Scene mutations from FX thread
- **WHEN** a `MapLayer` needs to add, remove, or update JavaFX nodes in its scene graph
- **AND** the calling thread is the JavaFX Application Thread
- **THEN** the layer MAY directly perform these mutations.

#### Scenario: Scene mutations from background thread
- **WHEN** a `MapLayer` needs to react to background work (I/O, computation)
- **AND** the current thread is not the JavaFX Application Thread
- **THEN** the layer SHALL wrap any scene-graph mutations in `Platform.runLater` (or an equivalent mechanism)
- **SO THAT** JavaFX threading rules are respected.

### Requirement: MapLayer Performance Constraints
`MapLayer` implementations SHALL be designed so that they do not significantly degrade overall map performance.

#### Scenario: Node count kept under control
- **WHEN** a `MapLayer` visualizes many domain objects (such as markers or shapes)
- **THEN** it SHOULD avoid creating an excessive number of JavaFX nodes
- **AND** it SHOULD consider using a `Canvas` or batched rendering techniques
- **SO THAT** layout and rendering overhead stay acceptable.

#### Scenario: Work proportional to visible content
- **WHEN** `layoutLayer(MapView)` is called for a large dataset
- **THEN** the layer SHOULD limit per-pulse work to objects that are visible in the current viewport
- **SO THAT** complexity grows with the number of visible items rather than the total dataset size.

### Requirement: Use of MapView State
`MapLayer` implementations SHALL use `MapView` state consistently to compute their visual positions.

#### Scenario: Accessing map state
- **WHEN** a `MapLayer` needs to position visuals based on the map
- **THEN** it SHALL use `MapView` properties such as `getCenterLat()`, `getCenterLon()`, and `getZoom()`
- **AND** it SHALL respect the current viewport width and height
- **SO THAT** visuals remain aligned with the rendered tiles.

#### Scenario: Projection reuse
- **WHEN** a `MapLayer` needs to convert between geographic coordinates and screen coordinates
- **THEN** it SHOULD reuse the same projection model employed by the map (for example via shared utilities)
- **INSTEAD OF** duplicating projection logic inside the layer
- **SO THAT** visual overlays stay consistent with base map rendering.

### Requirement: Resource Management for Layers
`MapLayer` implementations SHALL manage subscriptions, background tasks, and resources over their lifecycle.

#### Scenario: Resource initialization on add
- **WHEN** a `MapLayer` is attached to a `MapView`
- **THEN** it MAY allocate resources such as event subscriptions, caches, or background tasks in `layerAdded(MapView)`
- **AND** these operations SHALL occur on the JavaFX Application Thread when they touch the scene graph.

#### Scenario: Resource cleanup on remove
- **WHEN** a `MapLayer` is removed from a `MapView`
- **THEN** it SHALL release subscriptions and stop background tasks in `layerRemoved(MapView)`
- **SO THAT** no stale references or leaks remain after the layer is detached.
