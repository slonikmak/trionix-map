# JFX-Map-Engine — Спецификация Библиотеки (v4.1)

## Обзор

**JFX-Map-Engine** — универсальный графический движок для отображения гео-данных. Не имеет зависимостей от бизнес-логики, баз данных или протоколов робота.

Библиотека предоставляет "Черный ящик" (View), который управляется через строго определенные интерфейсы.

---

## Легенда статусов

| Иконка | Значение |
|--------|----------|
| ✅ | Реализовано |
| ⚠️ | Частично реализовано |
| ❌ | Не реализовано |

---

## 1. Контракт: Система Координат и Проекция (`Projection`) ✅

> **Реализация:** `com.trionix.maps.internal.projection.Projection` + `WebMercatorProjection`

Отвечает за математику перевода географических координат в экранные.

*   **Вход:** Географическая координата (Lat/Lon).
*   **Параметр:** Уровень зума (Zoom Level).
*   **Выход:** Экранная координата (Pixel X/Y) относительно мирового начала координат.
*   **Реализация по умолчанию:** Web Mercator (EPSG:3857) — `WebMercatorProjection.INSTANCE` (синглтон).

**API:**
```java
PixelCoordinate latLonToPixel(double latitude, double longitude, int zoom);
LatLon pixelToLatLon(double pixelX, double pixelY, int zoom);
```

---

## 2. Контракт: Поставщик Тайлов (`TileRetriever`) ✅

> **Реализация:** `com.trionix.maps.TileRetriever` + `SimpleOsmTileRetriever`

Абстракция источника растровых данных. Библиотека не знает, откуда берутся картинки (Интернет, Диск, Генерация).

*   **Запрос:** Индексы тайла `(X, Y, Zoom)`.
*   **Ответ:** `CompletableFuture<Image>` — асинхронный результат.
*   **Поведение:** Библиотека запрашивает тайлы асинхронно. Поставщик должен вернуть результат, когда он готов, не блокируя UI.

**API:**
```java
CompletableFuture<Image> loadTile(int zoom, long x, long y);
```

### 2.1. Кэширование тайлов (`TileCache`) ✅

> **Реализация:** `TileCache`, `InMemoryTileCache`, `FileTileCache`, `TieredTileCache`

*   `InMemoryTileCache` — LRU-кэш в памяти.
*   `FileTileCache` — персистентный кэш на диске.
*   `TieredTileCache` — двухуровневый (память + диск).
*   `TileCacheBuilder` — fluent API для создания кэшей.

---

## 3. Контракт: Слой карты (`MapLayer`) ✅

> **Реализация:** `com.trionix.maps.layer.MapLayer` (абстрактный класс)

Базовый примитив, который может быть добавлен на карту.

*   **Свойства** (наследуются от JavaFX `Node`):
    *   `viewOrder` (порядок наложения, аналог Z-Index).
    *   `opacity` (прозрачность).
    *   `visible` (видимость).
*   **Метод:** `layoutLayer(MapView mapView)` — команда отрисовки содержимого в текущей области видимости.
*   **Lifecycle hooks:** `layerAdded()`, `layerRemoved()`.

**API:**
```java
public abstract void layoutLayer(MapView mapView);
protected final Projection getProjection();
public final MapView getMapView();
public final void requestLayerLayout();
```

---

## 4. Специализированные слои

### 4.1. Слой Маркеров (`PointMarkerLayer`) ✅

> **Реализация:** `com.trionix.maps.layer.PointMarkerLayer` + `PointMarker`

*   **Назначение:** Отрисовка точечных маркеров (иконки, пины).
*   **Данные:** Список `PointMarker`, каждый содержит координату и JavaFX `Node`.
*   **Интерактивность:** Drag & Drop, Click callback.

**API:**
```java
PointMarker addMarker(double lat, double lon, Node node);
boolean removeMarker(PointMarker marker);
void clearMarkers();
List<PointMarker> getMarkers();
```

**TODO:**
- [ ] Вращение иконок маркеров (для отображения направления)
- [ ] Culling (отсечение невидимых маркеров)

---

### 4.2. Слой Полилиний (`PolylineLayer`) ✅

> **Реализация:** `com.trionix.maps.layer.PolylineLayer` + `Polyline`

*   **Назначение:** Отрисовка линий на карте.
*   **Данные:** Список `Polyline` с точками и стилями.
*   **Интерактивность:** Редактирование вершин, перетаскивание.

**API:**
```java
void addPolyline(Polyline polyline);
void removePolyline(Polyline polyline);
ObservableList<Polyline> getPolylines();
```

---

### 4.3. Слой Векторных Объектов (`VectorLayer<T>`) ❌

> **Статус:** Не реализовано

*   **Назначение:** Generic слой для отрисовки множества однотипных объектов.
*   **Данные:** Коллекция объектов, реализующих интерфейс `Drawable`.
*   **Оптимизация:** Должен поддерживать отсечение (Culling) объектов вне экрана.

**Планируемый API:**
```java
interface Drawable {
    GeoPoint getLocation();
    Node render();
}

class VectorLayer<T extends Drawable> extends MapLayer {
    void addItem(T item);
    void removeItem(T item);
    void clear();
}
```

**Рекомендация:** Рефакторинг `PointMarkerLayer` для извлечения базовой логики в `VectorLayer<T>`.

---

### 4.4. Слой Градиентного Пути (`GradientPathLayer`) ❌

> **Статус:** Не реализовано

*   **Назначение:** Отрисовка трека робота с цветовой индикацией (например, глубина → цвет).
*   **Данные:** Список сегментов пути. Каждый сегмент имеет: `StartPoint`, `EndPoint`, `Color`.
*   **Особенность:** Линия должна интерполировать цвет между точками (градиентная заливка по длине).

**Планируемый API:**
```java
record PathSegment(GeoPoint start, GeoPoint end, Color color) {}

class GradientPathLayer extends MapLayer {
    void setPath(List<PathSegment> segments);
    void appendSegment(PathSegment segment);
    void clear();
}
```

**Приоритет:** 🔴 Высокий — ключевая функция для Этапа 2 (Tracking).

---

### 4.5. Кэшированный Слой (`CachedBitmapLayer`) ❌

> **Статус:** Не реализовано

*   **Назначение:** Для тяжелой статики (DXF, чертежи).
*   **Поведение:**
    *   Рисует контент один раз во внутренний буфер (изображение большого размера).
    *   При сдвиге карты (Pan) просто перемещает буфер.
    *   При зуме (Zoom) вызывает перерисовку буфера.
*   **Контракт обновления:** Метод `markDirty()` сообщает слою, что данные изменились и буфер нужно перерисовать.

**Планируемый API:**
```java
abstract class CachedBitmapLayer extends MapLayer {
    protected abstract void renderToBuffer(GraphicsContext gc, MapView view);
    public void markDirty();
}
```

**Приоритет:** 🟡 Средний — нужен для Этапа 4.

---

### 4.6. Сетка координат (`GridLayer`) ✅

> **Реализация:** `com.trionix.maps.layer.GridLayer`

*   **Назначение:** Отображение координатной сетки.
*   **Особенность:** Адаптивный шаг сетки в зависимости от зума.

---

### 4.7. Слой тайлов (`TileLayer`) ✅

> **Реализация:** `com.trionix.maps.layer.TileLayer`

*   **Назначение:** Рендеринг растровых тайлов (базовая карта).
*   **Интеграция:** Используется внутри `MapView`.

---

## 5. Контракт: Взаимодействие (`MapInteraction`) ⚠️

> **Частичная реализация:** `MapInteractionHandler` (внутренний класс)

### 5.1. События карты

| Событие | Статус | Описание |
|---------|--------|----------|
| Клик | ⚠️ | Через JavaFX events, нет единого API |
| Долгое нажатие | ❌ | Не реализовано |
| Скролл (Zoom) | ✅ | Работает |
| Pan (перетаскивание) | ✅ | Работает |

### 5.2. События объектов

| Событие | Статус | Описание |
|---------|--------|----------|
| Клик по маркеру | ✅ | `PointMarker.setOnClick()` |
| Hover на линии | ❌ | Не реализовано |
| Drag & Drop маркера | ✅ | `PointMarker.setDraggable(true)` |

**TODO:**
- [ ] Создать унифицированный интерфейс `MapEventListener`
- [ ] Добавить Long Press событие
- [ ] Добавить Hover для `Polyline`

---

## 6. Вспомогательные компоненты

### 6.1. MapView ✅

> **Реализация:** `com.trionix.maps.MapView`

Главный виджет карты.

**API:**
```java
// Свойства
DoubleProperty centerLatProperty();
DoubleProperty centerLonProperty();
DoubleProperty zoomProperty();
ObservableList<MapLayer> getLayers();

// Навигация
void flyTo(double lat, double lon, double zoom, Duration duration);

// Координаты
Point2D geoPointToLocal(double lat, double lon);
GeoPoint sceneToGeoPoint(double sceneX, double sceneY);
```

### 6.2. GeoPoint ✅

> **Реализация:** `com.trionix.maps.GeoPoint`

```java
record GeoPoint(double latitude, double longitude) {}
```

### 6.3. MapAnimationConfig ✅

> **Реализация:** `com.trionix.maps.MapAnimationConfig`

Конфигурация анимаций (scroll, fly-to, double-click zoom).

---

## Roadmap Библиотеки

### Этап 1: Ядро (Core) ✅

| Задача | Статус |
|--------|--------|
| 1. Реализация математики проекции (Mercator) | ✅ |
| 2. Создание виджета `MapView` (Canvas loop) | ✅ |
| 3. Реализация загрузчика тайлов и кэша в памяти | ✅ |
| 4. Базовый контроллер ввода (Pan/Zoom мыши) | ✅ |

### Этап 2: Слежение (Tracking) ⚠️

| Задача | Статус | Приоритет |
|--------|--------|-----------|
| 1. Реализация слоя маркеров `PointMarkerLayer` | ✅ | — |
| 2. Реализация вращения иконок | ❌ | 🔴 Высокий |
| 3. Реализация `GradientPathLayer` (цветные линии) | ❌ | 🔴 Высокий |
| 4. Интерполяция движения (сглаживание рывков) | ❌ | 🟡 Средний |

### Этап 3: Планирование (Editor Tools) ⚠️

| Задача | Статус | Приоритет |
|--------|--------|-----------|
| 1. Обработка кликов (Click Listener) с координатами | ⚠️ | 🟡 Средний |
| 2. Drag & Drop API для маркеров | ✅ | — |
| 3. Отрисовка полигонов (полупрозрачная заливка) | ❌ | 🟢 Низкий |

### Этап 4: Тяжелые данные и Оптимизация ❌

| Задача | Статус | Приоритет |
|--------|--------|-----------|
| 1. Реализация `CachedBitmapLayer` (буферизация) | ❌ | 🟡 Средний |
| 2. API для перерисовки (Invalidate/MarkDirty) | ❌ | 🟡 Средний |
| 3. Режим инверсии цветов (Contrast Mode) | ❌ | 🟢 Низкий |

### Этап 5: UX и Релиз (Final Polish) ❌

| Задача | Статус | Приоритет |
|--------|--------|-----------|
| 1. Тултипы (всплывающие подсказки при наведении) | ❌ | 🟢 Низкий |
| 2. Визуальная отладка (FPS counter, Tile Grid) | ⚠️ | 🟢 Низкий |
| 3. Стресс-тесты (100k точек трека) | ❌ | 🟡 Средний |

---

## Рекомендуемые действия (TODO)

### 🔴 Приоритет 1: Этап 2 — Tracking

1. **Реализовать `GradientPathLayer`**
   - Ключевая фича для отображения трека робота
   - Цветовая индикация глубины/скорости

2. **Добавить вращение иконок в `PointMarker`**
   - Для отображения направления движения робота
   - Свойство `rotation` или `heading`

### 🟡 Приоритет 2: Архитектура

3. **Создать интерфейс `Drawable`** и generic `VectorLayer<T>`
   - Унификация слоёв маркеров, иконок, точек
   - Избежание дублирования кода

4. **Добавить Culling в слои**
   - Отсечение объектов вне видимой области
   - Оптимизация для большого количества маркеров

5. **Унифицировать события взаимодействия**
   - Создать `MapEventListener` интерфейс
   - Long Press, Hover, координатные клики

### 🟢 Приоритет 3: Этап 4

6. **Реализовать `CachedBitmapLayer`**
   - Для DXF и тяжёлых overlay
   - `WritableImage` или off-screen `Canvas`

---

## Соответствие Java Naming Conventions

| Спецификация (старая) | Код (фактический) |
|----------------------|-------------------|
| `IGeoProjection` | `Projection` ✅ |
| `ITileProvider` | `TileRetriever` ✅ |
| `IMapLayer` | `MapLayer` ✅ |
| `IMapInteractive` | — (не реализовано) |
| `VectorLayer<T>` | `PointMarkerLayer` (конкретный) |

> **Примечание:** Java-стиль не использует `I`-prefix для интерфейсов.
