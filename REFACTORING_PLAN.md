# 🔧 План рефакторинга trionix-map-openspec

**Дата создания:** 2026-01-10  
**Версия:** 1.0  
**Статус:** В ожидании

---

## 📋 Обзор

Данный план описывает этапы рефакторинга проекта для устранения нарушений принципов DRY, KISS, SOLID и улучшения Maven-конфигурации. Каждый этап независим и может быть применён отдельно.

**Условные обозначения:**
- 🔴 Критический приоритет (влияет на качество/стабильность)
- 🟡 Средний приоритет (улучшение maintainability)
- 🟢 Низкий приоритет (косметические улучшения)

---

## Этап 1: Устранение дублирования Projection (DRY) 🔴

**Цель:** Избавиться от создания множественных экземпляров `WebMercatorProjection` в каждом слое.

### Задачи:

#### 1.1 Сделать WebMercatorProjection синглтоном

**Файл:** `trionix-map-core/src/main/java/com/trionix/maps/internal/projection/WebMercatorProjection.java`

```java
// Добавить:
public static final Projection INSTANCE = new WebMercatorProjection();

// Сделать конструктор package-private (не private для обратной совместимости)
WebMercatorProjection() {
}
```

#### 1.2 Добавить метод getProjection() в MapView

**Файл:** `trionix-map-core/src/main/java/com/trionix/maps/MapView.java`

```java
/**
 * Returns the projection used by this map view.
 * @return the Web Mercator projection instance
 */
public Projection getProjection() {
    return projection;
}
```

#### 1.3 Добавить protected метод getProjection() в MapLayer

**Файл:** `trionix-map-core/src/main/java/com/trionix/maps/layer/MapLayer.java`

```java
/**
 * Returns the projection from the attached MapView, or the default Web Mercator
 * projection if the layer is not attached.
 */
protected final Projection getProjection() {
    MapView view = getMapView();
    return view != null ? view.getProjection() : WebMercatorProjection.INSTANCE;
}
```

#### 1.4 Обновить слои для использования общего Projection

**Файлы для изменения:**
- `PointMarkerLayer.java` — удалить `private final Projection projection = new WebMercatorProjection();`, использовать `getProjection()`
- `PolylineLayer.java` — аналогично
- `GridLayer.java` — если использует projection

### Тесты:

- [ ] Убедиться, что все существующие тесты проходят
- [ ] Добавить тест на `MapView.getProjection()`
- [ ] Добавить тест на `MapLayer.getProjection()` когда слой не привязан

### Ожидаемый результат:
- Удалено ~4 дублирующих инстанцирования Projection
- Единая точка получения Projection через MapView

---

## Этап 2: Устранение дублирования вычисления zoomLevel (DRY) 🔴

**Цель:** Централизовать вычисление дискретного уровня зума.

### Задачи:

#### 2.1 Добавить публичный метод getDiscreteZoomLevel() в MapView

**Файл:** `trionix-map-core/src/main/java/com/trionix/maps/MapView.java`

```java
/**
 * Returns the current zoom level as a discrete integer (floor of the zoom value).
 * This is the zoom level used for tile calculations.
 * 
 * @return discrete zoom level, always >= 0
 */
public int getDiscreteZoomLevel() {
    return mapState.discreteZoomLevel();
}
```

#### 2.2 Заменить все вычисления на вызов getDiscreteZoomLevel()

**Файлы для изменения:**

| Файл | Строка | Старый код | Новый код |
|------|--------|------------|-----------|
| `PointMarkerLayer.java` | ~87 | `int zoomLevel = Math.max(0, (int) Math.floor(mapView.getZoom()));` | `int zoomLevel = mapView.getDiscreteZoomLevel();` |
| `PolylineLayer.java` | ~64 | `int zoomLevel = Math.max(0, (int) Math.floor(mapView.getZoom()));` | `int zoomLevel = mapView.getDiscreteZoomLevel();` |
| `GridLayer.java` | ~104 | `int zoomLevel = Math.max(0, (int) Math.floor(mapView.getZoom()));` | `int zoomLevel = mapView.getDiscreteZoomLevel();` |
| `ScaleRulerControl.java` | ~158 | `int zoomLevel = Math.max(0, (int) Math.floor(mapView.getZoom()));` | `int zoomLevel = mapView.getDiscreteZoomLevel();` |

#### 2.3 Обновить внутренние методы MapView

В `MapView.java` заменить внутренние вызовы `mapState.discreteZoomLevel()` на использование приватных методов там, где это уместно (для консистентности).

### Тесты:

- [ ] Добавить тест `MapViewTest.getDiscreteZoomLevel_returnsFlooredValue()`
- [ ] Убедиться, что все существующие тесты проходят

### Ожидаемый результат:
- Удалено ~7 дублирующих вычислений
- Единая точка истины для дискретного зума

---

## Этап 3: Публичный API для преобразования координат (DRY) 🔴

**Цель:** Выделить преобразование экранных координат в географические в публичный API.

### Задачи:

#### 3.1 Добавить публичные методы преобразования в MapView

**Файл:** `trionix-map-core/src/main/java/com/trionix/maps/MapView.java`

```java
/**
 * Converts local (map-relative) coordinates to geographic coordinates.
 * 
 * @param localX x coordinate relative to the map view
 * @param localY y coordinate relative to the map view
 * @return GeoPoint at the specified location, or null if the view has no size
 */
public GeoPoint localToGeoPoint(double localX, double localY) {
    Projection.LatLon latLon = latLonAt(localX, localY);
    return latLon != null ? GeoPoint.of(latLon.latitude(), latLon.longitude()) : null;
}

/**
 * Converts scene coordinates to geographic coordinates.
 * 
 * @param sceneX x coordinate in the scene
 * @param sceneY y coordinate in the scene
 * @return GeoPoint at the specified location, or null if the view has no size
 */
public GeoPoint sceneToGeoPoint(double sceneX, double sceneY) {
    var local = sceneToLocal(sceneX, sceneY);
    return localToGeoPoint(local.getX(), local.getY());
}

/**
 * Converts geographic coordinates to local (map-relative) coordinates.
 * 
 * @param latitude latitude in degrees
 * @param longitude longitude in degrees
 * @return Point2D with local x,y coordinates, or null if the view has no size
 */
public javafx.geometry.Point2D geoPointToLocal(double latitude, double longitude) {
    double width = getWidth();
    double height = getHeight();
    if (width <= 0.0 || height <= 0.0) {
        return null;
    }
    int zoomLevel = getDiscreteZoomLevel();
    Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
            getCenterLat(), getCenterLon(), zoomLevel);
    Projection.PixelCoordinate targetPixels = projection.latLonToPixel(
            latitude, longitude, zoomLevel);
    double localX = targetPixels.x() - centerPixels.x() + width / 2.0;
    double localY = targetPixels.y() - centerPixels.y() + height / 2.0;
    return new javafx.geometry.Point2D(localX, localY);
}
```

#### 3.2 Обновить слои для использования нового API

**Файлы для изменения:**

**PointMarkerLayer.java** (метод installHandlers, ~136-145):
```java
// Было:
var local = view.sceneToLocal(ev.getSceneX(), ev.getSceneY());
int zoomLevel = Math.max(0, (int) Math.floor(view.getZoom()));
Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
        view.getCenterLat(), view.getCenterLon(), zoomLevel);
double offsetX = local.getX() - view.getWidth() / 2.0;
double offsetY = local.getY() - view.getHeight() / 2.0;
double pixelX = centerPixels.x() + offsetX;
double pixelY = centerPixels.y() + offsetY;
var latlon = projection.pixelToLatLon(pixelX, pixelY, zoomLevel);
marker.setLocation(latlon.latitude(), latlon.longitude());

// Стало:
GeoPoint geo = view.sceneToGeoPoint(ev.getSceneX(), ev.getSceneY());
if (geo != null) {
    marker.setLocation(geo.latitude(), geo.longitude());
}
```

**PolylineLayer.java** (метод installMarkerHandlers, ~188-199):
```java
// Аналогичная замена
GeoPoint geo = view.sceneToGeoPoint(ev.getSceneX(), ev.getSceneY());
if (geo != null) {
    polyline.updatePoint(index, geo);
}
```

#### 3.3 Упростить layoutLayer методы в слоях

**PointMarkerLayer.java** (метод layoutLayer):
```java
// Можно использовать geoPointToLocal вместо ручного вычисления
Point2D screenPos = mapView.geoPointToLocal(marker.getLatitude(), marker.getLongitude());
if (screenPos != null) {
    double layoutX = screenPos.getX() - width / 2.0;
    double layoutY = screenPos.getY() - height;
    node.resizeRelocate(layoutX, layoutY, width, height);
}
```

### Тесты:

- [ ] `MapViewTest.localToGeoPoint_returnsCorrectCoordinates()`
- [ ] `MapViewTest.sceneToGeoPoint_handlesSceneOffset()`
- [ ] `MapViewTest.geoPointToLocal_reverseOfLocalToGeo()`
- [ ] `MapViewTest.localToGeoPoint_returnsNullForZeroSize()`

### Ожидаемый результат:
- Удалено ~20 строк дублирующего кода в слоях
- Чистый публичный API для преобразования координат
- Упрощённый код слоёв

---

## Этап 4: Исправление race condition в FileTileCache 🔴

**Цель:** Устранить потенциальную race condition между проверкой существования файла и его чтением.

### Задачи:

#### 4.1 Рефакторинг метода get()

**Файл:** `trionix-map-core/src/main/java/com/trionix/maps/FileTileCache.java`

```java
@Override
public Image get(int zoom, long x, long y) {
    Path tilePath = tilePath(zoom, x, y);
    try {
        // Touch file for LRU tracking - this also verifies existence
        Files.setLastModifiedTime(tilePath, FileTime.fromMillis(System.currentTimeMillis()));
        return new Image(tilePath.toUri().toString());
    } catch (IOException e) {
        // File doesn't exist or was deleted by concurrent eviction
        return null;
    } catch (IllegalArgumentException e) {
        // Invalid image file
        return null;
    }
}
```

#### 4.2 Добавить try-with-resources для безопасного чтения (опционально)

Для более надёжного чтения можно использовать:

```java
@Override
public Image get(int zoom, long x, long y) {
    Path tilePath = tilePath(zoom, x, y);
    try {
        if (!Files.exists(tilePath)) {
            return null;
        }
        // Touch file for LRU tracking
        Files.setLastModifiedTime(tilePath, FileTime.fromMillis(System.currentTimeMillis()));
        // Read bytes to avoid file locking issues
        byte[] imageData = Files.readAllBytes(tilePath);
        return new Image(new ByteArrayInputStream(imageData));
    } catch (IOException e) {
        // File may have been deleted by concurrent eviction
        return null;
    }
}
```

### Тесты:

- [ ] `FileTileCacheTest.get_returnsNullWhenFileDeletedConcurrently()`
- [ ] Проверить многопоточный доступ

### Ожидаемый результат:
- Устранена race condition
- Более надёжное поведение при конкурентном доступе

---

## Этап 5: Рефакторинг PolylineLayer.layoutLayer() (KISS) 🟡

**Цель:** Разбить сложный метод на меньшие, более понятные методы.

### Задачи:

#### 5.1 Выделить вспомогательные методы

**Файл:** `trionix-map-core/src/main/java/com/trionix/maps/layer/PolylineLayer.java`

```java
@Override
public void layoutLayer(MapView mapView) {
    LayoutContext ctx = createLayoutContext(mapView);
    if (ctx == null) return;
    
    for (Polyline polyline : polylines) {
        PolylineVisual visual = visuals.get(polyline);
        if (visual == null) continue;
        
        updateVisualStyle(polyline, visual);
        updateMarkers(polyline, visual, ctx);
        updateLinePoints(polyline, visual, ctx);
        visual.lineNode.toBack();
    }
}

private record LayoutContext(
    int zoomLevel,
    double centerX,
    double centerY,
    double halfWidth,
    double halfHeight,
    Projection projection
) {}

private LayoutContext createLayoutContext(MapView mapView) {
    if (mapView.getWidth() <= 0 || mapView.getHeight() <= 0) {
        return null;
    }
    int zoomLevel = mapView.getDiscreteZoomLevel();
    Projection projection = getProjection();
    Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
            mapView.getCenterLat(), mapView.getCenterLon(), zoomLevel);
    return new LayoutContext(
        zoomLevel,
        centerPixels.x(),
        centerPixels.y(),
        mapView.getWidth() / 2.0,
        mapView.getHeight() / 2.0,
        projection
    );
}

private void updateVisualStyle(Polyline polyline, PolylineVisual visual) {
    visual.lineNode.setStroke(polyline.getStrokeColor());
    visual.lineNode.setStrokeWidth(polyline.getStrokeWidth());
    visual.lineNode.getStrokeDashArray().setAll(polyline.getStrokeDashArray());
}

private void updateMarkers(Polyline polyline, PolylineVisual visual, LayoutContext ctx) {
    boolean markersNeeded = polyline.isMarkersVisible() || polyline.isEditable();
    List<GeoPoint> points = polyline.getPoints();
    
    if (!markersNeeded) {
        removeAllMarkers(visual);
        return;
    }
    
    if (visual.markerNodes.size() != points.size()) {
        rebuildMarkers(polyline, visual, points);
    }
}

private void removeAllMarkers(PolylineVisual visual) {
    for (Node m : visual.markerNodes) {
        getChildren().remove(m);
    }
    visual.markerNodes.clear();
}

private void rebuildMarkers(Polyline polyline, PolylineVisual visual, List<GeoPoint> points) {
    removeAllMarkers(visual);
    for (int i = 0; i < points.size(); i++) {
        Node markerNode = polyline.getMarkerFactory().apply(points.get(i));
        markerNode.setManaged(false);
        installMarkerHandlers(markerNode, polyline, i);
        visual.markerNodes.add(markerNode);
        getChildren().add(markerNode);
    }
}

private void updateLinePoints(Polyline polyline, PolylineVisual visual, LayoutContext ctx) {
    List<GeoPoint> points = polyline.getPoints();
    visual.lineNode.getPoints().clear();
    
    boolean markersNeeded = polyline.isMarkersVisible() || polyline.isEditable();
    
    for (int i = 0; i < points.size(); i++) {
        GeoPoint gp = points.get(i);
        double[] screenPos = toScreenPosition(gp, ctx);
        
        visual.lineNode.getPoints().addAll(screenPos[0], screenPos[1]);
        
        if (markersNeeded && i < visual.markerNodes.size()) {
            positionMarker(visual.markerNodes.get(i), screenPos, polyline);
        }
    }
}

private double[] toScreenPosition(GeoPoint gp, LayoutContext ctx) {
    Projection.PixelCoordinate pixel = ctx.projection().latLonToPixel(
            gp.latitude(), gp.longitude(), ctx.zoomLevel());
    double screenX = pixel.x() - ctx.centerX() + ctx.halfWidth();
    double screenY = pixel.y() - ctx.centerY() + ctx.halfHeight();
    return new double[]{screenX, screenY};
}

private void positionMarker(Node markerNode, double[] screenPos, Polyline polyline) {
    boolean showHandle = polyline.isMarkersVisible() || polyline.isEditable();
    markerNode.setVisible(showHandle);
    markerNode.setMouseTransparent(!polyline.isEditable());
    
    if (markerNode.isVisible()) {
        double w = markerNode.prefWidth(-1);
        double h = markerNode.prefHeight(-1);
        markerNode.resizeRelocate(screenPos[0] - w / 2.0, screenPos[1] - h / 2.0, w, h);
    }
}
```

### Тесты:

- [ ] Убедиться, что все существующие тесты `PolylineLayerTest` проходят
- [ ] Проверить визуально работу polylines

### Ожидаемый результат:
- Метод `layoutLayer` сокращён с ~80 до ~15 строк
- Каждый вспомогательный метод имеет одну ответственность
- Код легче читать и поддерживать

---

## Этап 6: Выделение констант (Magic Numbers) 🟢

**Цель:** Заменить магические числа именованными константами.

### Задачи:

#### 6.1 GridLayer.java

```java
// Добавить константу:
private static final double SCALE_RULER_TARGET_WIDTH_PIXELS = 150.0;

// Заменить:
double targetMeters = mpp * SCALE_RULER_TARGET_WIDTH_PIXELS;
```

#### 6.2 TileExecutors.java

```java
// Добавить комментарий:
/**
 * Maximum concurrent tile loading tasks.
 * <p>
 * Using HTTP/1.1 with blocking calls allows higher parallelism.
 * Value chosen to balance download speed vs. server load.
 * Typical connection pool limits are 6-12 connections.
 */
private static final int MAX_CONCURRENT_TILES = 12;
```

#### 6.3 Polyline.java

```java
// Добавить константы:
private static final double DEFAULT_STROKE_WIDTH = 2.0;
private static final double DEFAULT_MARKER_RADIUS = 5.0;
private static final double DEFAULT_MARKER_STROKE_WIDTH = 1.0;
private static final Color DEFAULT_STROKE_COLOR = Color.BLUE;
private static final Color DEFAULT_MARKER_FILL = Color.RED;
private static final Color DEFAULT_MARKER_STROKE = Color.WHITE;
```

#### 6.4 ScaleRulerControl.java

Файл уже имеет константы — проверить полноту.

### Тесты:

- [ ] Убедиться, что все тесты проходят после замены

### Ожидаемый результат:
- Все магические числа имеют описательные имена
- Легче изменять значения в одном месте

---

## Этап 7: Maven улучшения 🟡

**Цель:** Улучшить Maven-конфигурацию для production-ready библиотеки.

### Задачи:

#### 7.1 Добавить maven-source-plugin

**Файл:** `pom.xml` (parent)

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-source-plugin</artifactId>
    <version>3.3.1</version>
    <executions>
        <execution>
            <id>attach-sources</id>
            <goals>
                <goal>jar-no-fork</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### 7.2 Добавить maven-javadoc-plugin

**Файл:** `pom.xml` (parent)

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-javadoc-plugin</artifactId>
    <version>3.10.1</version>
    <configuration>
        <doclint>none</doclint>
        <quiet>true</quiet>
    </configuration>
    <executions>
        <execution>
            <id>attach-javadocs</id>
            <goals>
                <goal>jar</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### 7.3 Добавить maven-jar-plugin с Automatic-Module-Name

**Файл:** `trionix-map-core/pom.xml`

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <version>3.4.2</version>
    <configuration>
        <archive>
            <manifestEntries>
                <Automatic-Module-Name>com.trionix.maps</Automatic-Module-Name>
            </manifestEntries>
        </archive>
    </configuration>
</plugin>
```

#### 7.4 Исправить Linux профиль

**Файл:** `pom.xml` (parent)

```xml
<profile>
    <id>linux</id>
    <activation>
        <os>
            <family>unix</family>
            <name>Linux</name>
        </os>
    </activation>
    <properties>
        <javafx.platform>linux</javafx.platform>
    </properties>
</profile>
```

### Тесты:

- [ ] `mvn clean verify` проходит
- [ ] `mvn clean package` создаёт source и javadoc JARs
- [ ] Проверить на разных платформах (если доступно)

### Ожидаемый результат:
- Source JAR создаётся при сборке
- Javadoc JAR создаётся при сборке
- Правильная работа профилей на всех платформах

---

## Этап 8: Рефакторинг MapView (SOLID - SRP) 🟢

**Цель:** Уменьшить размер MapView, выделив обработчики событий в отдельный класс.

### Задачи:

#### 8.1 Создать MapInteractionHandler

**Новый файл:** `trionix-map-core/src/main/java/com/trionix/maps/internal/interaction/MapInteractionHandler.java`

```java
package com.trionix.maps.internal.interaction;

import com.trionix.maps.MapView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;

/**
 * Handles user interaction events (drag, scroll, click, pinch) for MapView.
 */
public final class MapInteractionHandler {
    
    private final MapView mapView;
    private boolean dragging;
    private double lastDragX;
    private double lastDragY;
    private double lastPinchZoomDelta;
    private double lastPinchPivotX;
    private double lastPinchPivotY;
    
    public MapInteractionHandler(MapView mapView) {
        this.mapView = mapView;
    }
    
    public void install() {
        mapView.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        mapView.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        mapView.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        mapView.addEventHandler(MouseEvent.MOUSE_EXITED, this::handleMouseReleased);
        mapView.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClicked);
        mapView.addEventHandler(ScrollEvent.SCROLL, this::handleScroll);
        mapView.addEventHandler(ZoomEvent.ZOOM, this::handleZoomGesture);
        mapView.addEventHandler(ZoomEvent.ZOOM_STARTED, this::handleZoomGestureStarted);
        mapView.addEventHandler(ZoomEvent.ZOOM_FINISHED, this::handleZoomGestureFinished);
    }
    
    // ... методы handleMousePressed, handleMouseDragged, etc.
    // Переместить из MapView
}
```

#### 8.2 Обновить MapView для использования handler

```java
// В MapView.java
private final MapInteractionHandler interactionHandler;

public MapView(TileRetriever retriever, TileCache cache) {
    // ...
    this.interactionHandler = new MapInteractionHandler(this);
    // ...
    interactionHandler.install();
}

// Удалить методы handleMousePressed, handleMouseDragged, etc.
// Добавить package-private методы для callback от handler:
void panByPixelsDelta(double deltaX, double deltaY) { ... }
void zoomAroundPointBy(double zoomDelta, double pivotX, double pivotY) { ... }
```

**Примечание:** Этот этап опциональный и может быть отложен, так как требует значительных изменений в структуре кода.

### Тесты:

- [ ] Все интеграционные тесты MapView проходят
- [ ] Тесты drag, zoom, scroll работают

### Ожидаемый результат:
- MapView сокращён на ~150 строк
- Логика взаимодействия инкапсулирована
- Легче тестировать отдельно

---

## Этап 9: Улучшение слоёв - callback вместо owner (SOLID - DIP) 🟢

**Цель:** Заменить package-private поле `owner` на callback-интерфейс.

### Задачи:

#### 9.1 Создать интерфейсы слушателей

**Файл:** `trionix-map-core/src/main/java/com/trionix/maps/layer/MarkerChangeListener.java`

```java
package com.trionix.maps.layer;

/**
 * Listener for marker property changes.
 */
@FunctionalInterface
public interface MarkerChangeListener {
    void onMarkerChanged(PointMarker marker);
}
```

#### 9.2 Обновить PointMarker

```java
public final class PointMarker {
    private MarkerChangeListener changeListener;
    
    // Package-private setter for layer
    void setChangeListener(MarkerChangeListener listener) {
        this.changeListener = listener;
    }
    
    public void setLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        if (changeListener != null) {
            changeListener.onMarkerChanged(this);
        }
        if (onLocationChanged != null) {
            onLocationChanged.accept(this);
        }
    }
}
```

#### 9.3 Обновить PointMarkerLayer

```java
public PointMarker addMarker(double latitude, double longitude, Node node) {
    // ...
    marker.setChangeListener(m -> requestLayerLayout());
    // ...
}
```

**Примечание:** Этот рефакторинг обратно совместим, так как `owner` поле остаётся, но логика меняется.

### Тесты:

- [ ] Существующие тесты PointMarkerLayer проходят
- [ ] Тест на callback при изменении позиции

### Ожидаемый результат:
- Более чистая архитектура
- Слабое связывание между Marker и Layer

---

## 📅 Рекомендуемый порядок выполнения

| Порядок | Этап | Приоритет | Оценка (часы) | Зависимости |
|---------|------|-----------|---------------|-------------|
| 1 | Этап 2: zoomLevel | 🔴 | 0.5 | — |
| 2 | Этап 1: Projection | 🔴 | 1 | — |
| 3 | Этап 3: Координаты API | 🔴 | 2 | Этап 1, 2 |
| 4 | Этап 4: FileTileCache | 🔴 | 0.5 | — |
| 5 | Этап 7: Maven | 🟡 | 1 | — |
| 6 | Этап 6: Константы | 🟢 | 0.5 | — |
| 7 | Этап 5: PolylineLayer | 🟡 | 2 | Этап 1, 2, 3 |
| 8 | Этап 8: MapView SRP | 🟢 | 3 | Все предыдущие |
| 9 | Этап 9: Callbacks | 🟢 | 1 | — |

**Итого:** ~11.5 часов

---

## ✅ Чеклист для каждого этапа

Перед завершением этапа убедитесь:

- [ ] Все изменения компилируются: `mvn compile`
- [ ] Все тесты проходят: `mvn test`
- [ ] Интеграционные тесты проходят: `mvn verify`
- [ ] Демо-приложение работает: `mvn -pl trionix-map-demo javafx:run`
- [ ] Изменения задокументированы (при необходимости)
- [ ] Git commit с описательным сообщением

---

## 📝 Примечания

1. **Обратная совместимость:** Все изменения спроектированы для сохранения обратной совместимости API. Новые методы добавляются, старые не удаляются.

2. **Инкрементальность:** Каждый этап независим. Можно применять по одному и проверять стабильность.

3. **Тестирование:** После каждого этапа запускайте полный набор тестов.

4. **Откат:** При проблемах можно откатить отдельный этап без влияния на другие.

---

*Последнее обновление: 2026-01-10*
