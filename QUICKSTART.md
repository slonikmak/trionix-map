# Быстрый старт - Trionix Maps

## Установка и запуск

### 1. Проверьте требования
- Java 21 или выше
- Maven 3.9+
- Интернет-соединение (для загрузки тайлов с OpenStreetMap)

### 2. Запустите пример

**Windows (PowerShell):**
```powershell
.\run-examples.ps1
```

**Linux/Mac:**
```bash
./run-examples.sh
```

Или напрямую через Maven:
```bash
# Продвинутый пример с панелью управления
mvn compile exec:java -Dexec.mainClass=com.trionix.maps.samples.AdvancedMapExample

# Или так
mvn -pl trionix-map-demo javafx:run
```

## Минимальный код

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
        mapView.setCenterLat(55.7558);  // Москва
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

## Основные операции

### Управление картой

```java
MapView map = new MapView();

// Установка позиции
map.setCenterLat(59.9343);    // Широта
map.setCenterLon(30.3351);     // Долгота
map.setZoom(12.0);             // Масштаб (1-19)

// Анимированный переход
map.flyTo(55.7558, 37.6173, 10.0, Duration.seconds(2));
```

### Добавление маркеров

```java
// Создайте слой
MarkerLayer markers = new MarkerLayer();

// Добавьте маркер
Label marker = new Label("Моя метка");
markers.addMarker(55.7558, 37.6173, marker);

// Добавьте слой на карту
map.getLayers().add(markers);
```

### Отслеживание изменений

```java
// Подписка на изменения
map.centerLatProperty().addListener((obs, old, newVal) -> {
    System.out.println("Новая широта: " + newVal);
});

map.zoomProperty().addListener((obs, old, newVal) -> {
    System.out.println("Новый масштаб: " + newVal);
});
```

## Создание слоя

```java
import com.trionix.maps.layer.MapLayer;

public class MyLayer extends MapLayer {
    @Override
    public void layoutLayer(MapView mapView) {
        // Ваша логика размещения элементов
        // Вызывается автоматически при изменении карты
    }
    
    @Override
    public void layerAdded(MapView mapView) {
        // Вызывается при добавлении слоя на карту
    }
    
    @Override
    public void layerRemoved(MapView mapView) {
        // Вызывается при удалении слоя с карты
    }
}
```

## Управление мышью

Встроенные функции:
- **Перетаскивание** - панорамирование карты
- **Колесико мыши** - изменение масштаба
- **Двойной клик** - быстрое увеличение масштаба
- **Жесты трекпада** - масштабирование (pinch-to-zoom)

## Настройка кэша

```java
// Создание кэша на 1000 тайлов
TileCache cache = new InMemoryTileCache(1000);
MapView map = new MapView(new SimpleOsmTileRetriever(), cache);
```

## Где найти больше информации?

- `src/main/java/com/trionix/maps/samples/README.md` - подробная документация с примерами
- `src/main/java/com/trionix/maps/samples/` - исходный код всех примеров
- JavaDoc комментарии в коде библиотеки

## Возможные проблемы

**Предупреждения SLF4J при запуске:**
Это нормально - библиотека использует SLF4J для логирования, но в примерах логгер не настроен. Это не влияет на работу.

**Карта не загружается:**
Проверьте интернет-соединение - тайлы загружаются с серверов OpenStreetMap.

**Низкая производительность:**
Попробуйте увеличить размер кэша или проверьте скорость интернета.
