# Примеры использования Trionix Maps

Эта папка содержит примеры приложений, демонстрирующих возможности библиотеки Trionix Maps для JavaFX.

## Примеры

### 1. SimpleMapExample - Самый простой пример

**Файл:** `SimpleMapExample.java`

**Описание:** Минималистичный код для отображения карты - буквально 20 строк.

**Возможности:**
- Создание карты с настройками по умолчанию
- Установка начальной позиции и масштаба
- Базовая навигация мышью

**Запуск:**
```bash
mvn compile exec:java -Dexec.mainClass="com.trionix.maps.samples.SimpleMapExample"
```

### 2. MapViewSampleApp - Базовый пример

**Файл:** `MapViewSampleApp.java`

**Описание:** Приложение с демонстрацией работы со слоями и маркерами.

**Возможности:**
- Создание базового компонента карты
- Добавление слоя с маркерами
- Отображение нескольких маркеров на карте
- Базовая навигация (перетаскивание и масштабирование)

**Запуск:**
```bash
mvn compile exec:java -Dexec.mainClass="com.trionix.maps.samples.MapViewSampleApp"
```

### 3. AdvancedMapExample - Продвинутый пример

**Файл:** `AdvancedMapExample.java`

**Описание:** Полнофункциональное приложение с демонстрацией расширенных возможностей.

**Возможности:**
- Множественные слои (маркеры и маршруты)
- Анимированная навигация с помощью `flyTo()`
- Панель управления с кнопками быстрой навигации
- Отображение текущих координат и масштаба
- Программное управление масштабом
- Рисование линий между точками (слой маршрутов)
- Цветовая кодировка маркеров

**Запуск:**
```bash
mvn compile exec:java -Dexec.mainClass="com.trionix.maps.samples.AdvancedMapExample"
```

## Основные концепции

### Создание карты

```java
// Создание карты с настройками по умолчанию
MapView mapView = new MapView();
mapView.setPrefSize(800.0, 600.0);
mapView.setCenterLat(55.7558);  // Широта
mapView.setCenterLon(37.6173);   // Долгота
mapView.setZoom(12.0);           // Уровень масштаба
```

### Работа со слоями

```java
// Создание пользовательского слоя
public class MyLayer extends MapLayer {
    @Override
    public void layoutLayer(MapView mapView) {
        // Логика размещения элементов на слое
    }
}

// Добавление слоя на карту
MyLayer layer = new MyLayer();
mapView.getLayers().add(layer);
```

### Навигация и анимация

```java
// Мгновенный переход
mapView.setCenterLat(59.9343);
mapView.setCenterLon(30.3351);
mapView.setZoom(10.0);

// Анимированный переход
mapView.flyTo(59.9343, 30.3351, 10.0, Duration.seconds(2.0));
```

### Подписка на изменения

```java
// Отслеживание изменений координат
mapView.centerLatProperty().addListener((obs, oldVal, newVal) -> {
    System.out.println("Новая широта: " + newVal);
});

mapView.centerLonProperty().addListener((obs, oldVal, newVal) -> {
    System.out.println("Новая долгота: " + newVal);
});

mapView.zoomProperty().addListener((obs, oldVal, newVal) -> {
    System.out.println("Новый масштаб: " + newVal);
});
```

### Создание пользовательских слоев

#### Слой с маркерами

```java
private static class MarkerLayer extends MapLayer {
    private final Projection projection = new WebMercatorProjection();
    private final List<Marker> markers = new ArrayList<>();

    void addMarker(double lat, double lon, Region node) {
        node.setManaged(false);
        getChildren().add(node);
        markers.add(new Marker(lat, lon, node));
        requestLayerLayout();
    }

    @Override
    public void layoutLayer(MapView mapView) {
        int zoomLevel = (int) Math.round(mapView.getZoom());
        Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
            mapView.getCenterLat(), mapView.getCenterLon(), zoomLevel);
        
        double halfWidth = getWidth() / 2.0;
        double halfHeight = getHeight() / 2.0;

        for (Marker marker : markers) {
            Projection.PixelCoordinate markerPixels = projection.latLonToPixel(
                marker.latitude(), marker.longitude(), zoomLevel);
            
            double screenX = markerPixels.x() - centerPixels.x() + halfWidth;
            double screenY = markerPixels.y() - centerPixels.y() + halfHeight;
            
            // Позиционирование элемента
            marker.node().relocate(screenX, screenY);
        }
    }

    private record Marker(double latitude, double longitude, Region node) {}
}
```

## Интеграция в ваш проект

### Maven

Добавьте зависимость в `pom.xml`:

```xml
<dependency>
    <groupId>com.trionix</groupId>
    <artifactId>trionix-maps</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Требования

- Java 21 или выше
- JavaFX 21 или выше

## Полезные советы

1. **Производительность:** Используйте `requestLayerLayout()` вместо прямых вызовов `layoutLayer()` для батчинга обновлений.

2. **Потокобезопасность:** Все операции с UI должны выполняться в JavaFX Application Thread.

3. **Кэширование тайлов:** По умолчанию используется кэш на 500 тайлов. Можно настроить:
   ```java
   TileCache cache = new InMemoryTileCache(1000);
   MapView mapView = new MapView(new SimpleOsmTileRetriever(), cache);
   ```

4. **Пользовательские источники тайлов:** Реализуйте интерфейс `TileRetriever` для использования других источников карт.

## Дополнительная информация

Смотрите полную документацию в JavaDoc классов:
- `MapView` - основной компонент карты
- `MapLayer` - базовый класс для слоев
- `TileRetriever` - интерфейс для загрузки тайлов
- `TileCache` - интерфейс для кэширования

## Лицензия

См. основной файл LICENSE проекта.
