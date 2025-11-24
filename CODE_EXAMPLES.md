# Примеры кода для копирования

## 1. Минимальное приложение (20 строк)

```java
package com.example;

import com.trionix.maps.MapView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MinimalMapApp extends Application {
    @Override
    public void start(Stage stage) {
        MapView map = new MapView();
        map.setCenterLat(55.7558);
        map.setCenterLon(37.6173);
        map.setZoom(10.0);
        
        stage.setScene(new Scene(new StackPane(map), 800, 600));
        stage.show();
    }
    
    public static void main(String[] args) { launch(args); }
}
```

## 2. Карта с информационной панелью

```java
package com.example;

import com.trionix.maps.MapView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MapWithInfo extends Application {
    @Override
    public void start(Stage stage) {
        MapView map = new MapView();
        map.setCenterLat(55.7558);
        map.setCenterLon(37.6173);
        map.setZoom(10.0);
        
        Label info = new Label();
        info.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-padding: 10;");
        
        Runnable updateInfo = () -> info.setText(String.format(
            "Lat: %.4f, Lon: %.4f, Zoom: %.1f",
            map.getCenterLat(), map.getCenterLon(), map.getZoom()
        ));
        
        map.centerLatProperty().addListener((o, old, n) -> updateInfo.run());
        map.centerLonProperty().addListener((o, old, n) -> updateInfo.run());
        map.zoomProperty().addListener((o, old, n) -> updateInfo.run());
        updateInfo.run();
        
        StackPane root = new StackPane(map, info);
        StackPane.setAlignment(info, Pos.TOP_LEFT);
        StackPane.setMargin(info, new Insets(10));
        
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }
    
    public static void main(String[] args) { launch(args); }
}
```

## 3. Карта с кнопками навигации

```java
package com.example;

import com.trionix.maps.MapView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MapWithButtons extends Application {
    @Override
    public void start(Stage stage) {
        MapView map = new MapView();
        map.setCenterLat(55.7558);
        map.setCenterLon(37.6173);
        map.setZoom(10.0);
        
        VBox controls = new VBox(10);
        controls.setPadding(new Insets(10));
        controls.setStyle("-fx-background-color: #f0f0f0;");
        
        Button moscow = new Button("Москва");
        moscow.setOnAction(e -> map.flyTo(55.7558, 37.6173, 12, Duration.seconds(1)));
        
        Button spb = new Button("С-Петербург");
        spb.setOnAction(e -> map.flyTo(59.9343, 30.3351, 12, Duration.seconds(1)));
        
        Button zoomIn = new Button("Zoom +");
        zoomIn.setOnAction(e -> map.setZoom(map.getZoom() + 1));
        
        Button zoomOut = new Button("Zoom -");
        zoomOut.setOnAction(e -> map.setZoom(map.getZoom() - 1));
        
        controls.getChildren().addAll(moscow, spb, zoomIn, zoomOut);
        
        BorderPane root = new BorderPane(map);
        root.setRight(controls);
        
        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }
    
    public static void main(String[] args) { launch(args); }
}
```

## 4. Простой слой с маркерами

```java
package com.example;

import com.trionix.maps.MapView;
import com.trionix.maps.layer.MapLayer;
import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.projection.WebMercatorProjection;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import java.util.ArrayList;
import java.util.List;

public class SimpleMarkerLayer extends MapLayer {
    private final Projection projection = new WebMercatorProjection();
    private final List<MarkerData> markers = new ArrayList<>();
    
    public void addMarker(double lat, double lon, String text) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-padding: 5;");
        label.setManaged(false);
        getChildren().add(label);
        markers.add(new MarkerData(lat, lon, label));
        requestLayerLayout();
    }
    
    @Override
    public void layoutLayer(MapView mapView) {
        int zoom = (int) Math.round(mapView.getZoom());
        Projection.PixelCoordinate center = projection.latLonToPixel(
            mapView.getCenterLat(), mapView.getCenterLon(), zoom);
        
        double halfW = getWidth() / 2.0;
        double halfH = getHeight() / 2.0;
        
        for (MarkerData m : markers) {
            Projection.PixelCoordinate pos = projection.latLonToPixel(m.lat, m.lon, zoom);
            double x = pos.x() - center.x() + halfW;
            double y = pos.y() - center.y() + halfH;
            m.node.relocate(x - m.node.getWidth() / 2, y - m.node.getHeight());
        }
    }
    
    private record MarkerData(double lat, double lon, Region node) {}
}

// Использование:
// SimpleMarkerLayer layer = new SimpleMarkerLayer();
// layer.addMarker(55.7558, 37.6173, "Москва");
// mapView.getLayers().add(layer);
```

## 5. Слой с линиями (маршруты)

```java
package com.example;

import com.trionix.maps.MapView;
import com.trionix.maps.layer.MapLayer;
import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.projection.WebMercatorProjection;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import java.util.ArrayList;
import java.util.List;

public class RouteLayer extends MapLayer {
    private final Projection projection = new WebMercatorProjection();
    private final List<RouteData> routes = new ArrayList<>();
    
    public void addRoute(double lat1, double lon1, double lat2, double lon2, Color color) {
        Line line = new Line();
        line.setStroke(color);
        line.setStrokeWidth(3);
        line.setManaged(false);
        getChildren().add(line);
        routes.add(new RouteData(lat1, lon1, lat2, lon2, line));
        requestLayerLayout();
    }
    
    @Override
    public void layoutLayer(MapView mapView) {
        int zoom = (int) Math.round(mapView.getZoom());
        Projection.PixelCoordinate center = projection.latLonToPixel(
            mapView.getCenterLat(), mapView.getCenterLon(), zoom);
        
        double halfW = getWidth() / 2.0;
        double halfH = getHeight() / 2.0;
        
        for (RouteData r : routes) {
            Projection.PixelCoordinate p1 = projection.latLonToPixel(r.lat1, r.lon1, zoom);
            Projection.PixelCoordinate p2 = projection.latLonToPixel(r.lat2, r.lon2, zoom);
            
            r.line.setStartX(p1.x() - center.x() + halfW);
            r.line.setStartY(p1.y() - center.y() + halfH);
            r.line.setEndX(p2.x() - center.x() + halfW);
            r.line.setEndY(p2.y() - center.y() + halfH);
        }
    }
    
    private record RouteData(double lat1, double lon1, double lat2, double lon2, Line line) {}
}

// Использование:
// RouteLayer layer = new RouteLayer();
// layer.addRoute(55.7558, 37.6173, 59.9343, 30.3351, Color.BLUE);
// mapView.getLayers().add(layer);
```

## 6. Настройка кэша и источника тайлов

```java
import com.trionix.maps.InMemoryTileCache;
import com.trionix.maps.MapView;
import com.trionix.maps.SimpleOsmTileRetriever;
import com.trionix.maps.TileCache;
import com.trionix.maps.TileRetriever;

// Кастомный размер кэша
TileCache cache = new InMemoryTileCache(2000); // 2000 тайлов
TileRetriever retriever = new SimpleOsmTileRetriever();
MapView map = new MapView(retriever, cache);
```

## 7. Пользовательский источник тайлов

```java
import com.trionix.maps.TileRetriever;
import javafx.scene.image.Image;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class CustomTileRetriever implements TileRetriever {
    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    
    @Override
    public CompletableFuture<Image> loadTile(int zoom, long x, long y) {
        // Пример: другой сервер тайлов
        String url = String.format(
            "https://tile.example.com/%d/%d/%d.png", 
            zoom, x, y
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .build();
        
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
            .thenApply(response -> {
                if (response.statusCode() == 200) {
                    return new Image(response.body());
                }
                throw new RuntimeException("HTTP " + response.statusCode());
            });
    }
}

// Использование:
// MapView map = new MapView(new CustomTileRetriever(), new InMemoryTileCache(500));
```

## 8. Отслеживание кликов по карте

```java
import com.trionix.maps.MapView;
import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.projection.WebMercatorProjection;

MapView map = new MapView();
Projection projection = new WebMercatorProjection();

map.setOnMouseClicked(event -> {
    int zoom = (int) Math.round(map.getZoom());
    Projection.PixelCoordinate center = projection.latLonToPixel(
        map.getCenterLat(), map.getCenterLon(), zoom);
    
    double clickX = event.getX() - map.getWidth() / 2.0 + center.x();
    double clickY = event.getY() - map.getHeight() / 2.0 + center.y();
    
    Projection.LatLon location = projection.pixelToLatLon(clickX, clickY, zoom);
    
    System.out.printf("Клик по координатам: %.4f, %.4f%n", 
        location.latitude(), location.longitude());
});
```

## 9. Ограничение области просмотра

```java
MapView map = new MapView();

// Ограничение для России
double minLat = 41.0;
double maxLat = 82.0;
double minLon = 19.0;
double maxLon = 180.0;

map.centerLatProperty().addListener((obs, old, newVal) -> {
    if (newVal.doubleValue() < minLat || newVal.doubleValue() > maxLat) {
        map.setCenterLat(Math.max(minLat, Math.min(maxLat, newVal.doubleValue())));
    }
});

map.centerLonProperty().addListener((obs, old, newVal) -> {
    if (newVal.doubleValue() < minLon || newVal.doubleValue() > maxLon) {
        map.setCenterLon(Math.max(minLon, Math.min(maxLon, newVal.doubleValue())));
    }
});
```

## 10. Анимация полета по точкам

```java
import javafx.util.Duration;
import java.util.List;

record Point(double lat, double lon, String name) {}

List<Point> points = List.of(
    new Point(55.7558, 37.6173, "Москва"),
    new Point(59.9343, 30.3351, "Санкт-Петербург"),
    new Point(55.7887, 49.1221, "Казань")
);

MapView map = new MapView();
int[] currentIndex = {0};

javafx.animation.Timeline timeline = new javafx.animation.Timeline(
    new javafx.animation.KeyFrame(Duration.seconds(3), e -> {
        Point p = points.get(currentIndex[0]);
        map.flyTo(p.lat, p.lon, 12, Duration.seconds(2));
        currentIndex[0] = (currentIndex[0] + 1) % points.size();
    })
);
timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
timeline.play();
```
