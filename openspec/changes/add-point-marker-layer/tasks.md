## 1. Core Layer Implementation
- [x] 1.1 Create `PointMarker` class with latitude, longitude, node, and draggable/visible properties
- [x] 1.2 Create `PointMarkerLayer` class extending `MapLayer` with marker collection
- [x] 1.3 Implement `addMarker(double lat, double lon, Node node)` method
- [x] 1.4 Implement `layoutLayer(MapView)` using WebMercatorProjection for positioning
- [x] 1.5 Add unit tests for marker positioning calculations

## 2. Marker Management
- [x] 2.1 Implement `removeMarker(PointMarker)` method with boolean return
- [x] 2.2 Implement `clearMarkers()` method
- [x] 2.3 Implement `getMarkers()` returning unmodifiable list
- [x] 2.4 Add unit tests for marker add/remove operations

## 3. Programmatic Repositioning
- [x] 3.1 Implement `PointMarker.setLocation(double lat, double lon)` method
- [x] 3.2 Implement `PointMarker.getLatitude()` and `getLongitude()` methods
- [x] 3.3 Wire `setLocation()` to call `requestLayerLayout()` on owning layer
- [x] 3.4 Add unit tests for location updates

## 4. Interactive Drag Support
- [x] 4.1 Add `draggable` property to `PointMarker` with getter/setter
- [x] 4.2 Implement mouse press/drag/release handlers in `PointMarkerLayer`
- [x] 4.3 Convert screen coordinates to geographic coordinates during drag
- [x] 4.4 Consume drag events to prevent map panning
- [ ] 4.5 Add integration tests for drag behavior

## 5. Click Callback Support
- [x] 5.1 Add `onClickHandler` property to `PointMarker`
- [x] 5.2 Implement `setOnClick(Consumer<PointMarker>)` method
- [x] 5.3 Wire mouse click events to invoke handler
- [x] 5.4 Add unit tests for click callback invocation

## 6. Visibility Control
 - [x] 6.1 Add `visible` property to `PointMarker` with getter/setter
 - [x] 6.2 Implement visibility toggle affecting node and event handling
 - [x] 6.3 Add unit tests for visibility behavior

## 7. Demo Updates
## 7. Demo Updates
- [x] 7.1 Refactor `MapViewSampleApp` to use library `PointMarkerLayer`
- [x] 7.2 Refactor `AdvancedMapExample` to use library `PointMarkerLayer`
- [x] 7.3 Add drag-and-drop demonstration to one of the samples
- [ ] 7.4 Verify samples run correctly with new implementation

## 8. Documentation
## 8. Documentation
- [x] 8.1 Add Javadoc to `PointMarker` and `PointMarkerLayer` classes
- [x] 8.2 Update README.md with PointMarkerLayer usage example
- [ ] 8.3 Update CODE_EXAMPLES.md if needed
