# Quick Start - Trionix Maps

## Installation and Launch

### 1. Check Requirements
- Java 21 or higher
- Maven 3.9+
- Internet connection (to download tiles from OpenStreetMap)

### 2. Run an Example

**Windows (PowerShell):**
```powershell
.\run-examples.ps1
```

**Linux/Mac:**
```bash
./run-examples.sh
```

Or directly via Maven:
```bash
# Advanced example with control panel
mvn compile exec:java -Dexec.mainClass=com.trionix.maps.samples.AdvancedMapExample

# Or this way
mvn -pl trionix-map-demo javafx:run
```

## Minimal Code

```java
import com.trionix.maps.MapView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MyMapApp extends Application {
    @Override
    public void start(Stage stage) {
        MapView mapView = new MapView();
        mapView.setCenterLat(55.7558);  // Moscow
        mapView.setCenterLon(37.6173);
        mapView.setZoom(10.0);
        
        Scene scene = new Scene(new StackPane(mapView), 800, 600);
        stage.setScene(scene);
        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
```

## Basic Operations

### Map Control

```java
MapView map = new MapView();

// Set position
map.setCenterLat(59.9343);    // Latitude
map.setCenterLon(30.3351);     // Longitude
map.setZoom(12.0);             // Zoom Level (1-19)

// Animated transition
map.flyTo(55.7558, 37.6173, 10.0, Duration.seconds(2));
```

### Adding Markers

```java
// Create a layer
MarkerLayer markers = new MarkerLayer();

// Add a marker
Label marker = new Label("My Label");
markers.addMarker(55.7558, 37.6173, marker);

// Add the layer to the map
map.getLayers().add(markers);
```

### Tracking Changes

```java
// Subscribe to changes
map.centerLatProperty().addListener((obs, old, newVal) -> {
    System.out.println("New Latitude: " + newVal);
});

map.zoomProperty().addListener((obs, old, newVal) -> {
    System.out.println("New Zoom: " + newVal);
});
```

## Creating a Layer

```java
import com.trionix.maps.layer.MapLayer;

public class MyLayer extends MapLayer {
    @Override
    public void layoutLayer(MapView mapView) {
        // Your element placement logic
        // Called automatically when the map changes
    }
    
    @Override
    public void layerAdded(MapView mapView) {
        // Called when the layer is added to the map
    }
    
    @Override
    public void layerRemoved(MapView mapView) {
        // Called when the layer is removed from the map
    }
}
```

## Mouse Control

Built-in features:
- **Dragging** - pans the map
- **Mouse Wheel** - changes zoom level
- **Double Click** - quick zoom in
- **Trackpad Gestures** - pinch-to-zoom

## Cache Configuration

```java
// Create a cache for 1000 tiles
TileCache cache = new InMemoryTileCache(1000);
MapView map = new MapView(new SimpleOsmTileRetriever(), cache);
```

## Where to Find More Information?

- `src/main/java/com/trionix/maps/samples/README.md` - detailed documentation with examples
- `src/main/java/com/trionix/maps/samples/` - source code for all examples
- JavaDoc comments in the library code

## Possible Issues

**SLF4J warnings on startup:**
This is normal - the library uses SLF4J for logging, but the logger is not configured in the examples. It does not affect functionality.

**Map does not load:**
Check your internet connection - tiles are loaded from OpenStreetMap servers.

**Low performance:**
Try increasing the cache size or check your internet speed.
