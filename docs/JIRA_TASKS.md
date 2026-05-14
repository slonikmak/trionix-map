# Jira Tasks — JFX-Map-Engine & ROV-Mission-Control

> Экспорт задач из ROADMAP.md для импорта в Jira.
> Формат: CSV-совместимые таблицы + детальное описание каждой задачи.

---

## Быстрый импорт (CSV формат)

Скопируй таблицу ниже в CSV файл для импорта в Jira:

```csv
Summary,Component,Epic,Priority,Status,Story Points,Labels
"[LIB] Реализация математики проекции (Web Mercator)","JFX-Map-Engine","Этап 1: Ядро","Low","Done",3,"core,projection"
"[LIB] Создание виджета MapView (Canvas loop)","JFX-Map-Engine","Этап 1: Ядро","Low","Done",8,"core,mapview"
"[LIB] Реализация загрузчика тайлов и кэша в памяти","JFX-Map-Engine","Этап 1: Ядро","Low","Done",5,"core,tiles,cache"
"[LIB] Базовый контроллер ввода (Pan/Zoom мыши)","JFX-Map-Engine","Этап 1: Ядро","Low","Done",5,"core,interaction"
"[LIB] Реализация слоя маркеров PointMarkerLayer","JFX-Map-Engine","Этап 2: Слежение","Low","Done",5,"layers,markers"
"[LIB] Реализация вращения иконок маркеров","JFX-Map-Engine","Этап 2: Слежение","High","To Do",3,"layers,markers,rotation"
"[LIB] Реализация GradientPathLayer (цветные линии)","JFX-Map-Engine","Этап 2: Слежение","High","To Do",8,"layers,track,gradient"
"[LIB] Интерполяция движения (сглаживание рывков)","JFX-Map-Engine","Этап 2: Слежение","Medium","To Do",5,"animation,interpolation"
"[LIB] Обработка кликов с географическими координатами","JFX-Map-Engine","Этап 3: Планирование","Medium","In Progress",3,"interaction,events"
"[LIB] Drag & Drop API для маркеров","JFX-Map-Engine","Этап 3: Планирование","Low","Done",5,"interaction,markers,dnd"
"[LIB] Отрисовка полигонов (полупрозрачная заливка)","JFX-Map-Engine","Этап 3: Планирование","Low","To Do",5,"layers,polygons"
"[LIB] Реализация CachedBitmapLayer (буферизация)","JFX-Map-Engine","Этап 4: Оптимизация","Medium","To Do",8,"layers,cache,dxf"
"[LIB] API для перерисовки (Invalidate/MarkDirty)","JFX-Map-Engine","Этап 4: Оптимизация","Medium","To Do",3,"api,invalidation"
"[LIB] Режим инверсии цветов (Contrast Mode)","JFX-Map-Engine","Этап 4: Оптимизация","Low","To Do",3,"ux,accessibility"
"[LIB] Тултипы (всплывающие подсказки при наведении)","JFX-Map-Engine","Этап 5: UX","Low","To Do",3,"ux,tooltips"
"[LIB] Визуальная отладка (FPS counter, Tile Grid)","JFX-Map-Engine","Этап 5: UX","Low","In Progress",3,"debug,ux"
"[LIB] Стресс-тесты (100k точек трека)","JFX-Map-Engine","Этап 5: UX","Medium","To Do",5,"testing,performance"
"[APP] UI Менеджера проектов (Создать/Открыть папку)","ROV-Mission-Control","Этап 1: Ядро","High","To Do",5,"ui,project-manager"
"[APP] Поднятие SQLite базы данных","ROV-Mission-Control","Этап 1: Ядро","High","To Do",5,"database,sqlite"
"[APP] Реализация FileSystemTileSource (чтение файлов)","ROV-Mission-Control","Этап 1: Ядро","Medium","To Do",5,"tiles,filesystem"
"[APP] Интеграция библиотеки в окно приложения","ROV-Mission-Control","Этап 1: Ядро","High","To Do",3,"integration,mapview"
"[APP] Парсер телеметрии и запись в БД (Mock-генератор)","ROV-Mission-Control","Этап 2: Телеметрия","High","To Do",8,"telemetry,parser,mock"
"[APP] Логика Follow Me (центровка карты по событию)","ROV-Mission-Control","Этап 2: Телеметрия","Medium","To Do",3,"tracking,follow-me"
"[APP] Маппинг Глубина -> Цвет","ROV-Mission-Control","Этап 2: Телеметрия","Medium","To Do",3,"telemetry,color-mapping"
"[APP] Передача накопленного трека в библиотеку","ROV-Mission-Control","Этап 2: Телеметрия","Medium","To Do",3,"integration,track"
"[APP] Режимы UI: Monitor vs Edit","ROV-Mission-Control","Этап 3: Планирование","Medium","To Do",5,"ui,modes"
"[APP] CRUD операции с Waypoints в БД","ROV-Mission-Control","Этап 3: Планирование","High","To Do",5,"database,waypoints,crud"
"[APP] Инструмент Линейка (расчет дистанции)","ROV-Mission-Control","Этап 3: Планирование","Low","To Do",3,"tools,measurement"
"[APP] Генератор галсов (математика внутри полигона)","ROV-Mission-Control","Этап 3: Планирование","Medium","To Do",8,"tools,survey,algorithm"
"[APP] Парсер DXF файлов","ROV-Mission-Control","Этап 4: DXF","Medium","To Do",8,"dxf,parser"
"[APP] UI настройки привязки чертежа (Offset/Rotation)","ROV-Mission-Control","Этап 4: DXF","Medium","To Do",5,"ui,dxf,georeferencing"
"[APP] Менеджер слоев (галочки видимости)","ROV-Mission-Control","Этап 4: DXF","Low","To Do",3,"ui,layers"
"[APP] HUD (Оверлей с цифрами поверх карты)","ROV-Mission-Control","Этап 5: UX","Low","To Do",5,"ui,hud,overlay"
"[APP] Экспорт проекта в ZIP/GeoJSON","ROV-Mission-Control","Этап 5: UX","Medium","To Do",5,"export,geojson"
"[APP] Краш-тесты (выдергивание питания)","ROV-Mission-Control","Этап 5: UX","Medium","To Do",5,"testing,reliability"
```

---

## Детальные описания задач

---

### Epic: Этап 1 — Ядро (Core & Storage)

---

#### LIB-001: Реализация математики проекции (Web Mercator)

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 1: Ядро |
| **Приоритет** | Low (Done) |
| **Story Points** | 3 |
| **Статус** | ✅ Done |
| **Labels** | `core`, `projection` |

**Описание:**
Реализовать интерфейс `Projection` и его реализацию `WebMercatorProjection` для конвертации Lat/Lon ↔ Pixel координат.

**Acceptance Criteria:**
- [x] Интерфейс `Projection` с методами `latLonToPixel()` и `pixelToLatLon()`
- [x] Реализация Web Mercator (EPSG:3857)
- [x] Singleton паттерн для `WebMercatorProjection`
- [x] Unit-тесты на граничные значения широты (±85.05°)

**Реализация:** `com.trionix.maps.internal.projection.Projection`, `WebMercatorProjection`

---

#### LIB-002: Создание виджета MapView (Canvas loop)

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 1: Ядро |
| **Приоритет** | Low (Done) |
| **Story Points** | 8 |
| **Статус** | ✅ Done |
| **Labels** | `core`, `mapview` |

**Описание:**
Создать основной виджет `MapView` — JavaFX Region, который рендерит тайлы и управляет слоями.

**Acceptance Criteria:**
- [x] Наследование от `javafx.scene.layout.Region`
- [x] Observable properties: `centerLat`, `centerLon`, `zoom`
- [x] Метод `flyTo()` с анимацией
- [x] Lifecycle для слоёв (`getLayers()`, `add()`, `remove()`)
- [x] Координатные конвертации (`geoPointToLocal()`, `sceneToGeoPoint()`)

**Реализация:** `com.trionix.maps.MapView`

---

#### LIB-003: Реализация загрузчика тайлов и кэша в памяти

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 1: Ядро |
| **Приоритет** | Low (Done) |
| **Story Points** | 5 |
| **Статус** | ✅ Done |
| **Labels** | `core`, `tiles`, `cache` |

**Описание:**
Реализовать асинхронную загрузку тайлов и многоуровневое кэширование.

**Acceptance Criteria:**
- [x] Интерфейс `TileRetriever` с асинхронным `loadTile()`
- [x] `SimpleOsmTileRetriever` для загрузки из OSM
- [x] `InMemoryTileCache` — LRU кэш в памяти
- [x] `FileTileCache` — персистентный кэш на диске
- [x] `TieredTileCache` — двухуровневый кэш
- [x] `TileCacheBuilder` — fluent API

**Реализация:** `TileRetriever`, `TileCache`, `InMemoryTileCache`, `FileTileCache`, `TieredTileCache`

---

#### LIB-004: Базовый контроллер ввода (Pan/Zoom мыши)

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 1: Ядро |
| **Приоритет** | Low (Done) |
| **Story Points** | 5 |
| **Статус** | ✅ Done |
| **Labels** | `core`, `interaction` |

**Описание:**
Реализовать обработку мыши и тач-событий для навигации по карте.

**Acceptance Criteria:**
- [x] Pan (перетаскивание) мышью
- [x] Zoom (scroll wheel)
- [x] Double-click zoom
- [x] Pinch-to-zoom (touch)
- [x] Анимированные переходы

**Реализация:** `MapInteractionHandler` (внутренний класс в `MapView`)

---

#### APP-001: UI Менеджера проектов (Создать/Открыть папку)

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 1: Ядро |
| **Приоритет** | High |
| **Story Points** | 5 |
| **Статус** | ❌ To Do |
| **Labels** | `ui`, `project-manager` |

**Описание:**
Создать экран запуска приложения с возможностью создать новый проект или открыть существующий.

**Acceptance Criteria:**
- [ ] Стартовый экран с кнопками "Создать проект" / "Открыть проект"
- [ ] Диалог выбора папки для нового проекта
- [ ] Список недавних проектов
- [ ] Валидация структуры папки проекта

---

#### APP-002: Поднятие SQLite базы данных

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 1: Ядро |
| **Приоритет** | High |
| **Story Points** | 5 |
| **Статус** | ❌ To Do |
| **Labels** | `database`, `sqlite` |

**Описание:**
Интегрировать SQLite для хранения данных проекта (waypoints, телеметрия, настройки).

**Acceptance Criteria:**
- [ ] Подключение SQLite JDBC
- [ ] Схема базы данных (миграции)
- [ ] DAO для основных сущностей
- [ ] Инициализация БД при создании проекта

---

#### APP-003: Реализация FileSystemTileSource (чтение файлов)

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 1: Ядро |
| **Приоритет** | Medium |
| **Story Points** | 5 |
| **Статус** | ❌ To Do |
| **Labels** | `tiles`, `filesystem` |

**Описание:**
Реализовать `TileRetriever`, который читает тайлы из локальной файловой системы (offline карты).

**Acceptance Criteria:**
- [ ] Реализация `TileRetriever` для чтения `{z}/{x}/{y}.png`
- [ ] Поддержка разных структур папок (TMS, Slippy)
- [ ] Fallback на placeholder при отсутствии тайла

---

#### APP-004: Интеграция библиотеки в окно приложения

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 1: Ядро |
| **Приоритет** | High |
| **Story Points** | 3 |
| **Статус** | ❌ To Do |
| **Labels** | `integration`, `mapview` |

**Описание:**
Встроить `MapView` в главное окно приложения.

**Acceptance Criteria:**
- [ ] MapView занимает центральную область
- [ ] Сайдбар для инструментов
- [ ] Toolbar сверху
- [ ] Статус-бар снизу (координаты курсора)

---

### Epic: Этап 2 — Слежение (Tracking & Telemetry)

---

#### LIB-005: Реализация слоя маркеров PointMarkerLayer

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 2: Слежение |
| **Приоритет** | Low (Done) |
| **Story Points** | 5 |
| **Статус** | ✅ Done |
| **Labels** | `layers`, `markers` |

**Описание:**
Слой для отображения точечных маркеров на карте.

**Acceptance Criteria:**
- [x] Добавление/удаление маркеров
- [x] Позиционирование маркеров по Lat/Lon
- [x] Callback при клике на маркер
- [x] Drag & Drop для маркеров

**Реализация:** `PointMarkerLayer`, `PointMarker`

---

#### LIB-006: Реализация вращения иконок маркеров

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 2: Слежение |
| **Приоритет** | 🔴 High |
| **Story Points** | 3 |
| **Статус** | ❌ To Do |
| **Labels** | `layers`, `markers`, `rotation` |

**Описание:**
Добавить возможность вращения иконки маркера для отображения направления движения (heading).

**Acceptance Criteria:**
- [ ] Свойство `heading` / `rotation` в `PointMarker`
- [ ] Автоматическое вращение Node маркера
- [ ] Поддержка анимации поворота
- [ ] Unit-тест на корректность угла

**Примечания:**
Приоритетная задача для отображения направления робота.

---

#### LIB-007: Реализация GradientPathLayer (цветные линии)

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 2: Слежение |
| **Приоритет** | 🔴 High |
| **Story Points** | 8 |
| **Статус** | ❌ To Do |
| **Labels** | `layers`, `track`, `gradient` |

**Описание:**
Слой для отображения трека робота с градиентной цветовой индикацией (например, глубина → цвет).

**Acceptance Criteria:**
- [ ] Класс `PathSegment(start, end, color)`
- [ ] Метод `setPath(List<PathSegment>)`
- [ ] Метод `appendSegment()` для инкрементального добавления
- [ ] Градиентная интерполяция цвета между точками
- [ ] Оптимизация для 10k+ сегментов

**Примечания:**
Ключевая функция для Этапа 2. Рассмотреть использование `LinearGradient` или пошаговой отрисовки.

---

#### LIB-008: Интерполяция движения (сглаживание рывков)

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 2: Слежение |
| **Приоритет** | 🟡 Medium |
| **Story Points** | 5 |
| **Статус** | ❌ To Do |
| **Labels** | `animation`, `interpolation` |

**Описание:**
Плавная анимация перемещения маркера между позициями телеметрии.

**Acceptance Criteria:**
- [ ] Метод `animateTo(lat, lon, duration)` в `PointMarker`
- [ ] Интерполяция координат (lerp)
- [ ] Опциональная интерполяция heading
- [ ] Отмена текущей анимации при новом вызове

---

#### APP-005: Парсер телеметрии и запись в БД (Mock-генератор)

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 2: Телеметрия |
| **Приоритет** | High |
| **Story Points** | 8 |
| **Статус** | ❌ To Do |
| **Labels** | `telemetry`, `parser`, `mock` |

**Описание:**
Парсер входящих данных телеметрии + mock-генератор для тестирования.

**Acceptance Criteria:**
- [ ] Интерфейс `TelemetryParser`
- [ ] Mock-генератор случайного движения
- [ ] Запись в SQLite (lat, lon, depth, heading, timestamp)
- [ ] Потоковое чтение из источника

---

#### APP-006: Логика Follow Me (центровка карты по событию)

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 2: Телеметрия |
| **Приоритет** | Medium |
| **Story Points** | 3 |
| **Статус** | ❌ To Do |
| **Labels** | `tracking`, `follow-me` |

**Описание:**
Автоматическая центровка карты на текущей позиции робота.

**Acceptance Criteria:**
- [ ] Кнопка включения/выключения режима Follow Me
- [ ] Плавная анимация центровки
- [ ] Автоотключение при ручном Pan

---

#### APP-007: Маппинг Глубина -> Цвет

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 2: Телеметрия |
| **Приоритет** | Medium |
| **Story Points** | 3 |
| **Статус** | ❌ To Do |
| **Labels** | `telemetry`, `color-mapping` |

**Описание:**
Функция преобразования значения глубины в цвет для `GradientPathLayer`.

**Acceptance Criteria:**
- [ ] Конфигурируемый диапазон глубины (min/max)
- [ ] Настраиваемая палитра (cold-warm, rainbow)
- [ ] Легенда цветов в UI

---

#### APP-008: Передача накопленного трека в библиотеку

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 2: Телеметрия |
| **Приоритет** | Medium |
| **Story Points** | 3 |
| **Статус** | ❌ To Do |
| **Labels** | `integration`, `track` |

**Описание:**
Интеграция телеметрии с `GradientPathLayer`.

**Acceptance Criteria:**
- [ ] Подписка на события телеметрии
- [ ] Инкрементальное добавление сегментов
- [ ] Загрузка исторического трека при открытии проекта

---

### Epic: Этап 3 — Планирование (Editor Tools)

---

#### LIB-009: Обработка кликов с географическими координатами

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 3: Планирование |
| **Приоритет** | 🟡 Medium |
| **Story Points** | 3 |
| **Статус** | ⚠️ In Progress |
| **Labels** | `interaction`, `events` |

**Описание:**
Унифицированный API для обработки кликов по карте с координатами.

**Acceptance Criteria:**
- [ ] Интерфейс `MapEventListener`
- [ ] Событие `onMapClick(GeoPoint)`
- [ ] Событие `onMapLongPress(GeoPoint)`
- [ ] Событие `onMapHover(GeoPoint)` (опционально)

**Примечания:**
Частично работает через JavaFX events, но нет унифицированного API.

---

#### LIB-010: Drag & Drop API для маркеров

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 3: Планирование |
| **Приоритет** | Low (Done) |
| **Story Points** | 5 |
| **Статус** | ✅ Done |
| **Labels** | `interaction`, `markers`, `dnd` |

**Описание:**
Возможность перетаскивания маркеров мышью.

**Acceptance Criteria:**
- [x] Свойство `draggable` в `PointMarker`
- [x] Обновление координат во время drag
- [x] События начала/конца drag

**Реализация:** `PointMarkerLayer.installHandlers()`

---

#### LIB-011: Отрисовка полигонов (полупрозрачная заливка)

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 3: Планирование |
| **Приоритет** | 🟢 Low |
| **Story Points** | 5 |
| **Статус** | ❌ To Do |
| **Labels** | `layers`, `polygons` |

**Описание:**
Слой для отображения полигонов (зоны, области обследования).

**Acceptance Criteria:**
- [ ] Класс `Polygon` с списком вершин
- [ ] `PolygonLayer` для управления полигонами
- [ ] Полупрозрачная заливка
- [ ] Stroke (контур)
- [ ] Редактирование вершин (опционально)

---

#### APP-009: Режимы UI: Monitor vs Edit

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 3: Планирование |
| **Приоритет** | Medium |
| **Story Points** | 5 |
| **Статус** | ❌ To Do |
| **Labels** | `ui`, `modes` |

**Описание:**
Переключение между режимом мониторинга и редактирования миссии.

**Acceptance Criteria:**
- [ ] Переключатель режимов в toolbar
- [ ] Monitor mode: только просмотр
- [ ] Edit mode: создание/редактирование waypoints
- [ ] Разная цветовая схема для режимов

---

#### APP-010: CRUD операции с Waypoints в БД

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 3: Планирование |
| **Приоритет** | High |
| **Story Points** | 5 |
| **Статус** | ❌ To Do |
| **Labels** | `database`, `waypoints`, `crud` |

**Описание:**
Сохранение и загрузка waypoints из базы данных.

**Acceptance Criteria:**
- [ ] Таблица `waypoints` в SQLite
- [ ] Create/Read/Update/Delete операции
- [ ] Синхронизация с UI (маркеры на карте)
- [ ] Undo/Redo (опционально)

---

#### APP-011: Инструмент Линейка (расчет дистанции)

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 3: Планирование |
| **Приоритет** | Low |
| **Story Points** | 3 |
| **Статус** | ❌ To Do |
| **Labels** | `tools`, `measurement` |

**Описание:**
Инструмент для измерения расстояния между точками на карте.

**Acceptance Criteria:**
- [ ] Клик по двум точкам
- [ ] Отображение линии и расстояния
- [ ] Формула Haversine для расчёта

---

#### APP-012: Генератор галсов (математика внутри полигона)

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 3: Планирование |
| **Приоритет** | Medium |
| **Story Points** | 8 |
| **Статус** | ❌ To Do |
| **Labels** | `tools`, `survey`, `algorithm` |

**Описание:**
Автоматическая генерация параллельных линий (галсов) внутри полигона.

**Acceptance Criteria:**
- [ ] Выбор полигона
- [ ] Настройка шага между галсами
- [ ] Настройка угла галсов
- [ ] Генерация waypoints вдоль галсов

---

### Epic: Этап 4 — Тяжелые данные и Оптимизация

---

#### LIB-012: Реализация CachedBitmapLayer (буферизация)

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 4: Оптимизация |
| **Приоритет** | 🟡 Medium |
| **Story Points** | 8 |
| **Статус** | ❌ To Do |
| **Labels** | `layers`, `cache`, `dxf` |

**Описание:**
Слой для тяжёлой статики (DXF, чертежи) с буферизацией в память.

**Acceptance Criteria:**
- [ ] Абстрактный метод `renderToBuffer(GraphicsContext)`
- [ ] Перерисовка только при зуме
- [ ] Сдвиг буфера при pan (без перерисовки)
- [ ] Метод `markDirty()` для принудительной перерисовки

---

#### LIB-013: API для перерисовки (Invalidate/MarkDirty)

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 4: Оптимизация |
| **Приоритет** | 🟡 Medium |
| **Story Points** | 3 |
| **Статус** | ❌ To Do |
| **Labels** | `api`, `invalidation` |

**Описание:**
Унифицированный механизм инвалидации для всех слоёв.

**Acceptance Criteria:**
- [ ] Метод `invalidate()` в `MapLayer`
- [ ] Оптимизация: батчинг перерисовок
- [ ] Документация паттерна использования

---

#### LIB-014: Режим инверсии цветов (Contrast Mode)

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 4: Оптимизация |
| **Приоритет** | 🟢 Low |
| **Story Points** | 3 |
| **Статус** | ❌ To Do |
| **Labels** | `ux`, `accessibility` |

**Описание:**
Инверсия цветов карты для улучшения контраста.

**Acceptance Criteria:**
- [ ] Метод `setContrastMode(boolean)` в `MapView`
- [ ] ColorAdjust effect на тайлы
- [ ] Сохранение настройки

---

#### APP-013: Парсер DXF файлов

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 4: DXF |
| **Приоритет** | Medium |
| **Story Points** | 8 |
| **Статус** | ❌ To Do |
| **Labels** | `dxf`, `parser` |

**Описание:**
Импорт DXF чертежей для отображения на карте.

**Acceptance Criteria:**
- [ ] Парсинг LINE, POLYLINE, CIRCLE
- [ ] Парсинг LAYER информации
- [ ] Конвертация в графические примитивы
- [ ] Обработка ошибок парсинга

---

#### APP-014: UI настройки привязки чертежа (Offset/Rotation)

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 4: DXF |
| **Приоритет** | Medium |
| **Story Points** | 5 |
| **Статус** | ❌ To Do |
| **Labels** | `ui`, `dxf`, `georeferencing` |

**Описание:**
Интерфейс для привязки DXF чертежа к географическим координатам.

**Acceptance Criteria:**
- [ ] Ввод точки привязки (Lat/Lon)
- [ ] Настройка смещения (X/Y)
- [ ] Настройка поворота
- [ ] Настройка масштаба

---

#### APP-015: Менеджер слоев (галочки видимости)

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 4: DXF |
| **Приоритет** | Low |
| **Story Points** | 3 |
| **Статус** | ❌ To Do |
| **Labels** | `ui`, `layers` |

**Описание:**
Панель управления видимостью слоёв.

**Acceptance Criteria:**
- [ ] Список всех слоёв
- [ ] Checkbox для видимости
- [ ] Drag & Drop для изменения порядка
- [ ] Настройка прозрачности (опционально)

---

### Epic: Этап 5 — UX и Релиз (Final Polish)

---

#### LIB-015: Тултипы (всплывающие подсказки при наведении)

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 5: UX |
| **Приоритет** | 🟢 Low |
| **Story Points** | 3 |
| **Статус** | ❌ To Do |
| **Labels** | `ux`, `tooltips` |

**Описание:**
Всплывающие подсказки при наведении на объекты карты.

**Acceptance Criteria:**
- [ ] Tooltip для маркеров
- [ ] Tooltip для точек полилинии
- [ ] Настраиваемый контент tooltip

---

#### LIB-016: Визуальная отладка (FPS counter, Tile Grid)

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 5: UX |
| **Приоритет** | 🟢 Low |
| **Story Points** | 3 |
| **Статус** | ⚠️ In Progress |
| **Labels** | `debug`, `ux` |

**Описание:**
Инструменты для отладки производительности.

**Acceptance Criteria:**
- [x] Сетка тайлов (`GridLayer`)
- [ ] FPS counter overlay
- [ ] Отображение границ тайлов
- [ ] Debug mode toggle

**Примечания:**
`GridLayer` уже реализован.

---

#### LIB-017: Стресс-тесты (100k точек трека)

| Поле | Значение |
|------|----------|
| **Компонент** | JFX-Map-Engine |
| **Epic** | Этап 5: UX |
| **Приоритет** | 🟡 Medium |
| **Story Points** | 5 |
| **Статус** | ❌ To Do |
| **Labels** | `testing`, `performance` |

**Описание:**
Тестирование производительности на больших объёмах данных.

**Acceptance Criteria:**
- [ ] Тест с 100k точек трека
- [ ] Тест с 1000 маркеров
- [ ] Замеры FPS и памяти
- [ ] Отчёт о узких местах

---

#### APP-016: HUD (Оверлей с цифрами поверх карты)

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 5: UX |
| **Приоритет** | Low |
| **Story Points** | 5 |
| **Статус** | ❌ To Do |
| **Labels** | `ui`, `hud`, `overlay` |

**Описание:**
Головной дисплей с ключевыми показателями.

**Acceptance Criteria:**
- [ ] Глубина
- [ ] Скорость
- [ ] Курс
- [ ] Время миссии
- [ ] Полупрозрачный фон

---

#### APP-017: Экспорт проекта в ZIP/GeoJSON

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 5: UX |
| **Приоритет** | Medium |
| **Story Points** | 5 |
| **Статус** | ❌ To Do |
| **Labels** | `export`, `geojson` |

**Описание:**
Экспорт данных проекта в универсальные форматы.

**Acceptance Criteria:**
- [ ] Экспорт трека в GeoJSON
- [ ] Экспорт waypoints в GeoJSON
- [ ] Упаковка всего проекта в ZIP
- [ ] Диалог выбора расположения файла

---

#### APP-018: Краш-тесты (выдергивание питания)

| Поле | Значение |
|------|----------|
| **Компонент** | ROV-Mission-Control |
| **Epic** | Этап 5: UX |
| **Приоритет** | Medium |
| **Story Points** | 5 |
| **Статус** | ❌ To Do |
| **Labels** | `testing`, `reliability` |

**Описание:**
Тестирование устойчивости к сбоям.

**Acceptance Criteria:**
- [ ] Периодическое автосохранение
- [ ] Восстановление после краша
- [ ] Валидация целостности БД при старте
- [ ] Логирование ошибок

---

## Сводка по компонентам

### JFX-Map-Engine (Библиотека)

| Статус | Количество |
|--------|------------|
| ✅ Done | 6 |
| ⚠️ In Progress | 2 |
| ❌ To Do | 9 |
| **Всего** | **17** |

### ROV-Mission-Control (Приложение)

| Статус | Количество |
|--------|------------|
| ✅ Done | 0 |
| ⚠️ In Progress | 0 |
| ❌ To Do | 18 |
| **Всего** | **18** |

---

## Jira Custom Fields Mapping

| Поле в документе | Jira Field |
|-----------------|------------|
| Summary | Summary |
| Component | Component/s |
| Epic | Epic Link (или Labels) |
| Priority | Priority |
| Status | Status |
| Story Points | Story Points |
| Labels | Labels |
| Описание | Description |
| Acceptance Criteria | Description (или Custom field) |
