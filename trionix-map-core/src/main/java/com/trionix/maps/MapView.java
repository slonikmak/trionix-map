package com.trionix.maps;

import static com.trionix.maps.internal.util.CoordinateNormalizer.clampLatitude;
import static com.trionix.maps.internal.util.CoordinateNormalizer.clampZoom;
import static com.trionix.maps.internal.util.CoordinateNormalizer.normalizeLongitude;

import com.trionix.maps.internal.MapState;
import com.trionix.maps.internal.interaction.MapInteractionHandler;
import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.projection.WebMercatorProjection;
import com.trionix.maps.internal.tiles.PlaceholderTileFactory;
import com.trionix.maps.internal.tiles.TileCoordinate;
import com.trionix.maps.internal.tiles.TileManager;
import com.trionix.maps.layer.MapLayer;
import javafx.animation.AnimationTimer;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * JavaFX {@link Region} that renders OpenStreetMap raster tiles and exposes
 * observable center and zoom properties for binding-based applications.
 */
public final class MapView extends Region {

    private static final int DEFAULT_CACHE_CAPACITY = 500;
    private static final double PREF_SIZE = 512.0;
    private static final Image PLACEHOLDER = PlaceholderTileFactory.placeholder();
    private static final double REDRAW_EPSILON = 0.001;
    private static final long REDRAW_WINDOW_NANOS = 1_500_000_000L;

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
    private final Projection projection = WebMercatorProjection.INSTANCE;
    private final Canvas tileCanvas = new Canvas();
    private final Pane layerPane = new Pane();
    private final GraphicsContext graphics = tileCanvas.getGraphicsContext2D();
    private final ObservableList<MapLayer> layers = FXCollections.observableArrayList();
    private final MapInteractionHandler interactionHandler;
    private Rectangle viewportClip;
    private final AnimationTimer redrawTimer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (now >= redrawUntilNanos) {
                stop();
                return;
            }
            forceCanvasInvalidation();
        }
    };
    private Timeline navigationTimeline;
    private Runnable activeAnimationCleanup;

    private List<TileCoordinate> currentVisibleTiles = List.of();
    private boolean refreshPending;
    private boolean redrawToggle;
    private long redrawUntilNanos;

    public MapView() {
        this(new SimpleOsmTileRetriever(), new InMemoryTileCache(DEFAULT_CACHE_CAPACITY));
    }

    public MapView(TileRetriever retriever, TileCache cache) {
        Objects.requireNonNull(retriever, "retriever");
        Objects.requireNonNull(cache, "cache");
        this.tileManager = new TileManager(cache, retriever);
        this.interactionHandler = new MapInteractionHandler(this);

        initializeProperties();
        initializeSceneGraph();
        interactionHandler.install();
        initializeLayers();
    }

    public ObservableList<MapLayer> getLayers() {
        return layers;
    }

    public Projection getProjection() {
        return projection;
    }

    public DoubleProperty centerLatProperty() {
        return centerLat;
    }

    public double getCenterLat() {
        return centerLat.get();
    }

    public void setCenterLat(double latitude) {
        centerLat.set(latitude);
    }

    public DoubleProperty centerLonProperty() {
        return centerLon;
    }

    public double getCenterLon() {
        return centerLon.get();
    }

    public void setCenterLon(double longitude) {
        centerLon.set(longitude);
    }

    public DoubleProperty zoomProperty() {
        return zoom;
    }

    public double getZoom() {
        return zoom.get();
    }

    public int getDiscreteZoomLevel() {
        return mapState.discreteZoomLevel();
    }

    public void setZoom(double zoomLevel) {
        zoom.set(zoomLevel);
    }

    public boolean isEnableDoubleClickZoom() {
        return enableDoubleClickZoom;
    }

    public void setEnableDoubleClickZoom(boolean enable) {
        this.enableDoubleClickZoom = enable;
    }

    public MapAnimationConfig getAnimationConfig() {
        return animationConfig;
    }

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
        forceCanvasInvalidation();
        layoutLayerNodes(width, height);
    }

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
        viewportClip = new Rectangle();
        viewportClip.widthProperty().bind(widthProperty());
        viewportClip.heightProperty().bind(heightProperty());
        setClip(viewportClip);
        getChildren().addAll(tileCanvas, layerPane);
        getStyleClass().add("map-view");
    }

    private void refreshTiles() {
        if (mapState.getViewportWidth() <= 0 || mapState.getViewportHeight() <= 0) {
            return;
        }
        List<TileCoordinate> visible = mapState.visibleTiles();
        currentVisibleTiles = visible;
        tileManager.refreshTiles(visible, (coordinate, image) -> redrawLoadedTile(coordinate));
        extendRedrawWindow();
        requestLayout();
    }

    private void redrawLoadedTile(TileCoordinate coordinate) {
        if (!currentVisibleTiles.contains(coordinate)) {
            return;
        }

        double width = tileCanvas.getWidth();
        double height = tileCanvas.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        drawTiles(width, height);
        forceCanvasInvalidation();
        extendRedrawWindow();
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

    private void extendRedrawWindow() {
        redrawUntilNanos = System.nanoTime() + REDRAW_WINDOW_NANOS;
        redrawTimer.start();
    }

    private void forceCanvasInvalidation() {
        double offset = redrawToggle ? REDRAW_EPSILON : 0.0;
        tileCanvas.setTranslateX(offset);
        layerPane.setTranslateX(offset);
        redrawToggle = !redrawToggle;
        Platform.requestNextPulse();
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

    public void panByPixelsDelta(double deltaX, double deltaY) {
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

    public void zoomAroundPointBy(double zoomDelta, double pivotX, double pivotY) {
        zoomAroundPoint(zoomDelta, pivotX, pivotY);
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

    public void cancelActiveAnimation() {
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

    private Projection.LatLon latLonAt(double localX, double localY) {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0.0 || height <= 0.0) {
            return null;
        }
        int zoomLevel = mapState.discreteZoomLevel();
        Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                getCenterLat(), getCenterLon(), zoomLevel);
        double offsetX = localX - width / 2.0;
        double offsetY = localY - height / 2.0;
        double pixelX = centerPixels.x() + offsetX;
        double pixelY = centerPixels.y() + offsetY;
        return projection.pixelToLatLon(pixelX, pixelY, zoomLevel);
    }

    public GeoPoint localToGeoPoint(double localX, double localY) {
        Projection.LatLon latLon = latLonAt(localX, localY);
        return latLon != null ? GeoPoint.of(latLon.latitude(), latLon.longitude()) : null;
    }

    public GeoPoint sceneToGeoPoint(double sceneX, double sceneY) {
        var local = sceneToLocal(sceneX, sceneY);
        return localToGeoPoint(local.getX(), local.getY());
    }

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
