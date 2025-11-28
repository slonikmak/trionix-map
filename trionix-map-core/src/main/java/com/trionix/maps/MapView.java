package com.trionix.maps;

import static com.trionix.maps.internal.util.CoordinateNormalizer.clampLatitude;
import static com.trionix.maps.internal.util.CoordinateNormalizer.clampZoom;
import static com.trionix.maps.internal.util.CoordinateNormalizer.normalizeLongitude;

import com.trionix.maps.internal.MapState;
import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.projection.WebMercatorProjection;
import com.trionix.maps.internal.tiles.PlaceholderTileFactory;
import com.trionix.maps.internal.tiles.TileCoordinate;
import com.trionix.maps.internal.tiles.TileManager;
import com.trionix.maps.layer.MapLayer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleUnaryOperator;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * JavaFX {@link Region} that renders OpenStreetMap raster tiles and exposes observable center
 * and zoom properties for binding-based applications. Unless otherwise noted, all mutating API
 * calls must occur on the JavaFX Application Thread. Convenience methods such as
 * {@link #flyTo(double, double, double, javafx.util.Duration)} will internally marshal to the FX
 * thread when necessary.
 */
public final class MapView extends Region {

    private static final int DEFAULT_CACHE_CAPACITY = 500;
    private static final double PREF_SIZE = 512.0;
    private static final Image PLACEHOLDER = PlaceholderTileFactory.placeholder();
    private static final double SCROLL_ZOOM_STEP = 0.5;
    private static final double DOUBLE_CLICK_ZOOM_DELTA = 1.0;

        private final DoubleProperty centerLat = createNormalizedProperty(
            "centerLat", v -> clampLatitude(v), 0.0);
        private final DoubleProperty centerLon = createNormalizedProperty(
            "centerLon", v -> normalizeLongitude(v), 0.0);
        private final DoubleProperty zoom = createNormalizedProperty(
            "zoom", v -> clampZoom(v, MapState.DEFAULT_MIN_ZOOM, MapState.DEFAULT_MAX_ZOOM), 1.0);

    private final MapAnimationConfig animationConfig = new MapAnimationConfig();
    private boolean enableDoubleClickZoom = true;

    private final MapState mapState = new MapState();
    private final TileManager tileManager;
    private final Projection projection = new WebMercatorProjection();
    private final Canvas tileCanvas = new Canvas();
    private final Pane layerPane = new Pane();
    private final GraphicsContext graphics = tileCanvas.getGraphicsContext2D();
    private final ObservableList<MapLayer> layers = FXCollections.observableArrayList();
    private Timeline navigationTimeline;
    private Runnable activeAnimationCleanup;

    private double lastPinchZoomDelta;
    private double lastPinchPivotX;
    private double lastPinchPivotY;

    private List<TileCoordinate> currentVisibleTiles = List.of();
    private boolean refreshPending;
    private boolean dragging;
    private double lastDragX;
    private double lastDragY;

    /**
     * Creates a {@code MapView} that uses {@link SimpleOsmTileRetriever} and an
     * {@link InMemoryTileCache} sized for roughly five hundred tiles.
     */
    public MapView() {
        this(new SimpleOsmTileRetriever(), new InMemoryTileCache(DEFAULT_CACHE_CAPACITY));
    }

    /**
     * Creates a {@code MapView} backed by the provided retriever and cache implementations.
     *
     * @param retriever strategy used to fetch tiles; must be thread-safe because it is invoked from
     *                  background threads
     * @param cache in-memory cache for decoded {@link Image} instances; must be thread-safe because
     *              it is accessed concurrently by the tile loader
     */
    public MapView(TileRetriever retriever, TileCache cache) {
        Objects.requireNonNull(retriever, "retriever");
        Objects.requireNonNull(cache, "cache");
        this.tileManager = new TileManager(cache, retriever);

        initializeProperties();
        initializeSceneGraph();
        initializeInteractionHandlers();
        initializeLayers();
    }

    /**
     * Returns the live list of {@link MapLayer layers} rendered above the tile canvas. The list is
     * observable, and modifications must occur on the JavaFX Application Thread.
     */
    public ObservableList<MapLayer> getLayers() {
        return layers;
    }

    /**
     * Returns the observable latitude property. Values are automatically clamped to the valid
     * Web Mercator latitude range.
     */
    public DoubleProperty centerLatProperty() {
        return centerLat;
    }

    /**
     * Returns the current latitude expressed in degrees.
     */
    public double getCenterLat() {
        return centerLat.get();
    }

    /**
     * Updates the center latitude in degrees. Values outside the projection range are clamped.
     */
    public void setCenterLat(double latitude) {
        centerLat.set(latitude);
    }

    /**
     * Returns the observable longitude property. Longitudes are normalized to the {@code [-180,
     * 180)} range.
     */
    public DoubleProperty centerLonProperty() {
        return centerLon;
    }

    /**
     * Returns the current center longitude in degrees.
     */
    public double getCenterLon() {
        return centerLon.get();
    }

    /**
     * Updates the center longitude in degrees. Values are normalized to the Web Mercator range.
     */
    public void setCenterLon(double longitude) {
        centerLon.set(longitude);
    }

    /**
     * Returns the observable zoom property. Zoom levels are clamped to the supported discrete
     * range (defaults: 1–19).
     */
    public DoubleProperty zoomProperty() {
        return zoom;
    }

    /**
     * Returns the current zoom value. Fractional zooms are supported and rendered via smooth
     * scaling.
     */
    public double getZoom() {
        return zoom.get();
    }

    /**
     * Updates the zoom level. Values are clamped to {@link MapState#DEFAULT_MIN_ZOOM} and
     * {@link MapState#DEFAULT_MAX_ZOOM}.
     */
    public void setZoom(double zoomLevel) {
        zoom.set(zoomLevel);
    }

    /**
     * Returns whether double-click zoom is enabled.
     */
    public boolean isEnableDoubleClickZoom() {
        return enableDoubleClickZoom;
    }

    /**
     * Enables or disables double-click zoom functionality. When enabled (default), double-clicking
     * the primary mouse button zooms in by one level around the cursor position.
     *
     * @param enable {@code true} to enable double-click zoom, {@code false} to disable
     */
    public void setEnableDoubleClickZoom(boolean enable) {
        this.enableDoubleClickZoom = enable;
    }

    /**
     * Returns the animation configuration responsible for scroll, double-click, touch, and fly-to
     * transitions. Changes take effect immediately and allow callers to customize durations,
     * easing, or disable animations entirely.
     */
    public MapAnimationConfig getAnimationConfig() {
        return animationConfig;
    }

    /**
     * Smoothly animates the viewport to the provided center and zoom using a JavaFX
     * {@link Timeline}. A zero or negative duration jumps immediately. This method accepts calls
     * from any thread and will marshal to the JavaFX Application Thread automatically.
     *
     * @param latitude target latitude
     * @param longitude target longitude
     * @param zoomLevel target zoom level
     * @param duration animation duration (must be finite)
     */
    public void flyTo(double latitude, double longitude, double zoomLevel, Duration duration) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isIndefinite() || duration.isUnknown()) {
            throw new IllegalArgumentException("duration must be finite");
        }
        double targetLat = clampLatitude(latitude);
        double targetLon = normalizeLongitude(longitude);
        double targetZoom = clampZoom(zoomLevel, MapState.DEFAULT_MIN_ZOOM, MapState.DEFAULT_MAX_ZOOM);
        Duration effectiveDuration = (!animationConfig.isAnimationsEnabled()
                || !animationConfig.isFlyToAnimationEnabled())
                ? Duration.ZERO
                : duration;
        Runnable action = () -> beginFlyToAnimation(targetLat, targetLon, targetZoom, effectiveDuration);
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        return PREF_SIZE;
    }

    @Override
    protected double computePrefHeight(double width) {
        return PREF_SIZE;
    }

    @Override
    protected void layoutChildren() {
        double width = snapSizeX(getWidth());
        double height = snapSizeY(getHeight());
        tileCanvas.setWidth(width);
        tileCanvas.setHeight(height);
        tileCanvas.relocate(0.0, 0.0);
        layerPane.resizeRelocate(0.0, 0.0, width, height);

        if (width != mapState.getViewportWidth() || height != mapState.getViewportHeight()) {
            mapState.setViewportSize(width, height);
            scheduleRefresh();
        }

        drawTiles(width, height);
        layoutLayerNodes(width, height);
    }

    /**
     * Schedules a tile refresh on the next FX pulse, coalescing multiple rapid
     * property changes into a single refresh operation.
     */
    private void scheduleRefresh() {
        if (refreshPending) {
            return;
        }
        refreshPending = true;
        Platform.runLater(() -> {
            refreshPending = false;
            refreshTiles();
        });
    }

    private void initializeProperties() {
        mapState.setCenterLat(centerLat.get());
        mapState.setCenterLon(centerLon.get());
        mapState.setZoom(zoom.get());

        centerLat.addListener((obs, oldValue, newValue) -> {
            mapState.setCenterLat(newValue.doubleValue());
            scheduleRefresh();
        });
        centerLon.addListener((obs, oldValue, newValue) -> {
            mapState.setCenterLon(newValue.doubleValue());
            scheduleRefresh();
        });
        zoom.addListener((obs, oldValue, newValue) -> {
            mapState.setZoom(newValue.doubleValue());
            scheduleRefresh();
        });
    }

    private void initializeSceneGraph() {
        tileCanvas.setManaged(false);
        tileCanvas.setMouseTransparent(true);
        layerPane.setManaged(false);
        layerPane.setPickOnBounds(false);
        getChildren().addAll(tileCanvas, layerPane);
        getStyleClass().add("map-view");
    }

    private void initializeInteractionHandlers() {
        addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        addEventHandler(MouseEvent.MOUSE_EXITED, this::handleMouseReleased);
        addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClicked);
        addEventHandler(ScrollEvent.SCROLL, this::handleScroll);
        addEventHandler(ZoomEvent.ZOOM, this::handleZoomGesture);
        addEventHandler(ZoomEvent.ZOOM_STARTED, this::handleZoomGestureStarted);
        addEventHandler(ZoomEvent.ZOOM_FINISHED, this::handleZoomGestureFinished);
    }

    private void refreshTiles() {
        if (mapState.getViewportWidth() <= 0 || mapState.getViewportHeight() <= 0) {
            return;
        }
        List<TileCoordinate> visible = mapState.visibleTiles();
        currentVisibleTiles = visible;
        tileManager.refreshTiles(visible, (coordinate, image) -> requestLayout());
        requestLayout();
    }

    private void initializeLayers() {
        layers.addListener((ListChangeListener<MapLayer>) change -> {
            ensureFxThread("Map layers must be modified on the JavaFX Application Thread");
            while (change.next()) {
                if (change.wasRemoved()) {
                    change.getRemoved().forEach(this::detachLayer);
                }
                if (change.wasAdded()) {
                    int insertionIndex = change.getFrom();
                    List<? extends MapLayer> added = change.getAddedSubList();
                    for (int i = 0; i < added.size(); i++) {
                        attachLayer(added.get(i), insertionIndex + i);
                    }
                }
                if (change.wasPermutated()) {
                    reorderLayerNodes();
                }
            }
            requestLayout();
        });
    }

    private void drawTiles(double width, double height) {
        graphics.clearRect(0.0, 0.0, width, height);
        if (currentVisibleTiles.isEmpty()) {
            return;
        }
        int zoomLevel = mapState.discreteZoomLevel();
        Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                mapState.getCenterLat(), mapState.getCenterLon(), zoomLevel);
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        double tileSize = Projection.TILE_SIZE;

        for (TileCoordinate tile : currentVisibleTiles) {
            Image cached = tileManager.cachedTile(tile);
            Image image = cached != null ? cached : PLACEHOLDER;
            double tileOriginX = tile.x() * tileSize;
            double tileOriginY = tile.y() * tileSize;
            double screenX = tileOriginX - centerPixels.x() + halfWidth;
            double screenY = tileOriginY - centerPixels.y() + halfHeight;
            graphics.drawImage(image, screenX, screenY, tileSize, tileSize);
        }
    }

    private void layoutLayerNodes(double width, double height) {
        if (layers.isEmpty()) {
            return;
        }
        for (MapLayer layer : layers) {
            layer.resizeRelocate(0.0, 0.0, width, height);
            layer.layoutLayer(this);
        }
    }

    private void handleMousePressed(MouseEvent event) {
        if (!event.isPrimaryButtonDown()) {
            return;
        }
        cancelActiveAnimation();
        dragging = true;
        lastDragX = event.getX();
        lastDragY = event.getY();
        // Don't consume here - let MOUSE_CLICKED fire for double-click detection
    }

    private void handleMouseDragged(MouseEvent event) {
        if (!dragging || !event.isPrimaryButtonDown()) {
            return;
        }
        double deltaX = event.getX() - lastDragX;
        double deltaY = event.getY() - lastDragY;
        lastDragX = event.getX();
        lastDragY = event.getY();
        panByPixels(deltaX, deltaY);
        // Don't consume - allow event bubbling
    }

    private void handleMouseReleased(MouseEvent event) {
        dragging = false;
    }

    private void handleMouseClicked(MouseEvent event) {
        if (!enableDoubleClickZoom || event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 2) {
            return;
        }
        if (event.isConsumed()) {
            return;
        }
        // Only zoom if it was a clean double-click (not after dragging)
        if (!event.isStillSincePress()) {
            return;
        }
        cancelActiveAnimation();
        // Immediate zoom (no smooth animation for double-click input)
        zoomAroundPoint(DOUBLE_CLICK_ZOOM_DELTA, event.getX(), event.getY());
    }

    private void handleScroll(ScrollEvent event) {
        if (event.isInertia()) {
            return;
        }
        double delta = event.getDeltaY();
        if (delta == 0.0) {
            return;
        }
        cancelActiveAnimation();
        double zoomDelta = SCROLL_ZOOM_STEP * Math.signum(delta);
        // Input zooms are instantaneous; skip animation path
        zoomAroundPoint(zoomDelta, event.getX(), event.getY());
        event.consume();
    }

    private void handleZoomGesture(ZoomEvent event) {
        double factor = event.getZoomFactor();
        if (factor <= 0.0 || factor == 1.0) {
            return;
        }
        cancelActiveAnimation();
        double zoomDelta = Math.log(factor) / Math.log(2.0);
        lastPinchZoomDelta = zoomDelta;
        lastPinchPivotX = event.getX();
        lastPinchPivotY = event.getY();
        // Perform immediate zoom for pinch gestures (no smooth animation)
        zoomAroundPoint(zoomDelta, event.getX(), event.getY());
        event.consume();
    }

    private void handleZoomGestureStarted(ZoomEvent event) {
        lastPinchZoomDelta = 0.0;
        lastPinchPivotX = event.getX();
        lastPinchPivotY = event.getY();
        cancelActiveAnimation();
    }

    private void handleZoomGestureFinished(ZoomEvent event) {
        // No momentum or delayed animation when pinch ends — input zooms are immediate
        lastPinchZoomDelta = 0.0;
    }

    private void attachLayer(MapLayer layer, int index) {
        Objects.requireNonNull(layer, "layer");
        if (layer.getMapView() != null && layer.getMapView() != this) {
            throw new IllegalStateException("MapLayer is already attached to another MapView");
        }
        layer.attachToMapView(this);
        layerPane.getChildren().remove(layer);
        int boundedIndex = Math.max(0, Math.min(index, layerPane.getChildren().size()));
        if (boundedIndex >= layerPane.getChildren().size()) {
            layerPane.getChildren().add(layer);
        } else {
            layerPane.getChildren().add(boundedIndex, layer);
        }
        layer.layerAdded(this);
    }

    private void detachLayer(MapLayer layer) {
        layer.layerRemoved(this);
        layer.detachFromMapView(this);
        layerPane.getChildren().remove(layer);
    }

    private void reorderLayerNodes() {
        if (layerPane.getChildren().isEmpty()) {
            return;
        }
        Map<MapLayer, Integer> order = new IdentityHashMap<>();
        for (int i = 0; i < layers.size(); i++) {
            order.put(layers.get(i), i);
        }
        FXCollections.sort(layerPane.getChildren(), (first, second) -> {
            MapLayer firstLayer = (MapLayer) first;
            MapLayer secondLayer = (MapLayer) second;
            return Integer.compare(order.getOrDefault(firstLayer, Integer.MAX_VALUE),
                    order.getOrDefault(secondLayer, Integer.MAX_VALUE));
        });
    }

    private void panByPixels(double deltaX, double deltaY) {
        if (deltaX == 0.0 && deltaY == 0.0) {
            return;
        }
        cancelActiveAnimation();
        int zoomLevel = mapState.discreteZoomLevel();
        Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                getCenterLat(), getCenterLon(), zoomLevel);
        double newPixelX = centerPixels.x() - deltaX;
        double newPixelY = centerPixels.y() - deltaY;
        Projection.LatLon latLon = projection.pixelToLatLon(newPixelX, newPixelY, zoomLevel);
        setCenterLat(latLon.latitude());
        setCenterLon(latLon.longitude());
    }

    private void zoomAroundPoint(double zoomDelta, double pivotX, double pivotY) {
        zoomAroundPoint(zoomDelta, pivotX, pivotY, false, Duration.ZERO, Interpolator.LINEAR);
    }

    private void zoomAroundPoint(double zoomDelta, double pivotX, double pivotY,
            boolean animate, Duration duration, Interpolator interpolator) {
        if (zoomDelta == 0.0 || getWidth() <= 0.0 || getHeight() <= 0.0) {
            return;
        }
        double targetZoom = getZoom() + zoomDelta;
        if (animate && isZoomAnimationAllowed(duration)) {
            playZoomAnimation(targetZoom, pivotX, pivotY, duration, interpolator);
        } else {
            applyZoom(targetZoom, pivotX, pivotY);
        }
    }

    private boolean isZoomAnimationAllowed(Duration duration) {
        return animationConfig.isAnimationsEnabled()
                && duration != null
                && duration.greaterThan(Duration.ZERO);
    }

    private void playZoomAnimation(double targetZoom, double pivotX, double pivotY,
            Duration duration, Interpolator interpolator) {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0.0 || height <= 0.0) {
            return;
        }
        double clampedZoom = clampZoom(targetZoom, MapState.DEFAULT_MIN_ZOOM, MapState.DEFAULT_MAX_ZOOM);
        double currentZoom = getZoom();
        if (Double.compare(clampedZoom, currentZoom) == 0) {
            return;
        }
        Projection.LatLon focus = latLonAt(pivotX, pivotY);
        if (focus == null) {
            applyZoom(targetZoom, pivotX, pivotY);
            return;
        }
        cancelActiveAnimation();
        SimpleDoubleProperty animationDriver = new SimpleDoubleProperty(currentZoom);
        ChangeListener<Number> listener = (obs, oldValue, newValue) -> {
            setZoom(newValue.doubleValue());
            alignCenterToFocus(focus, pivotX, pivotY);
        };
        animationDriver.addListener(listener);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(animationDriver, currentZoom)),
                new KeyFrame(duration,
                        new KeyValue(animationDriver, clampedZoom,
                                interpolator != null ? interpolator : Interpolator.EASE_BOTH)));
        Runnable cleanup = () -> animationDriver.removeListener(listener);
        timeline.setOnFinished(event -> {
            cleanup.run();
            navigationTimeline = null;
            activeAnimationCleanup = null;
            setZoom(clampedZoom);
            alignCenterToFocus(focus, pivotX, pivotY);
        });
        navigationTimeline = timeline;
        activeAnimationCleanup = cleanup;
        timeline.play();
    }

    private void applyZoom(double targetZoom, double pivotX, double pivotY) {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0.0 || height <= 0.0) {
            return;
        }
        double clampedZoom = clampZoom(targetZoom, MapState.DEFAULT_MIN_ZOOM, MapState.DEFAULT_MAX_ZOOM);
        double currentZoom = getZoom();
        if (Double.compare(clampedZoom, currentZoom) == 0) {
            return;
        }
        cancelActiveAnimation();
        Projection.LatLon focus = latLonAt(pivotX, pivotY);
        setZoom(clampedZoom);
        if (focus != null) {
            alignCenterToFocus(focus, pivotX, pivotY);
        }
    }

    private void beginFlyToAnimation(double latitude, double longitude, double zoomLevel, Duration duration) {
        ensureFxThread("flyTo animations must run on the JavaFX Application Thread");
        cancelActiveAnimation();
        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            setCenterLat(latitude);
            setCenterLon(longitude);
            setZoom(zoomLevel);
            return;
        }
        double startLat = getCenterLat();
        double startLon = getCenterLon();
        double startZoom = getZoom();
        if (Double.compare(startLat, latitude) == 0
                && Double.compare(startLon, longitude) == 0
                && Double.compare(startZoom, zoomLevel) == 0) {
            return;
        }
        Timeline timeline = new Timeline();
        Interpolator interpolator = animationConfig.getFlyToInterpolator();
        timeline.getKeyFrames().addAll(
            new KeyFrame(Duration.ZERO,
                new KeyValue(centerLatProperty(), startLat),
                new KeyValue(centerLonProperty(), startLon),
                new KeyValue(zoomProperty(), startZoom)),
            new KeyFrame(duration,
                new KeyValue(centerLatProperty(), latitude, interpolator),
                new KeyValue(centerLonProperty(), longitude, interpolator),
                new KeyValue(zoomProperty(), zoomLevel, interpolator)));
        timeline.setOnFinished(event -> {
            navigationTimeline = null;
            activeAnimationCleanup = null;
        });
        navigationTimeline = timeline;
        activeAnimationCleanup = null;
        timeline.play();
    }

    private void cancelActiveAnimation() {
        if (navigationTimeline != null) {
            navigationTimeline.stop();
            navigationTimeline = null;
        }
        if (activeAnimationCleanup != null) {
            activeAnimationCleanup.run();
            activeAnimationCleanup = null;
        }
    }

    private void ensureFxThread(String message) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(message);
        }
    }

    private Projection.LatLon latLonAt(double sceneX, double sceneY) {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0.0 || height <= 0.0) {
            return null;
        }
        int zoomLevel = mapState.discreteZoomLevel();
        Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                getCenterLat(), getCenterLon(), zoomLevel);
        double offsetX = sceneX - width / 2.0;
        double offsetY = sceneY - height / 2.0;
        double pixelX = centerPixels.x() + offsetX;
        double pixelY = centerPixels.y() + offsetY;
        return projection.pixelToLatLon(pixelX, pixelY, zoomLevel);
    }

    private void alignCenterToFocus(Projection.LatLon focus, double pivotX, double pivotY) {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0.0 || height <= 0.0) {
            return;
        }
        int zoomLevel = mapState.discreteZoomLevel();
        Projection.PixelCoordinate focusPixels = projection.latLonToPixel(
                focus.latitude(), focus.longitude(), zoomLevel);
        double offsetX = pivotX - width / 2.0;
        double offsetY = pivotY - height / 2.0;
        double centerPixelX = focusPixels.x() - offsetX;
        double centerPixelY = focusPixels.y() - offsetY;
        Projection.LatLon newCenter = projection.pixelToLatLon(centerPixelX, centerPixelY, zoomLevel);
        setCenterLat(newCenter.latitude());
        setCenterLon(newCenter.longitude());
    }

    private DoubleProperty createNormalizedProperty(String name,
            DoubleUnaryOperator normalizer, double initialValue) {
        Objects.requireNonNull(normalizer, "normalizer");
        double normalizedInitial = normalizer.applyAsDouble(initialValue);
        return new DoublePropertyBase(normalizedInitial) {
            @Override
            public void set(double newValue) {
                super.set(normalizer.applyAsDouble(newValue));
            }

            @Override
            public Object getBean() {
                return MapView.this;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }
}
