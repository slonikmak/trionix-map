package com.trionix.maps.layer;

import com.trionix.maps.MapView;
import com.trionix.maps.TileCache;
import com.trionix.maps.TileRetriever;
import com.trionix.maps.internal.MapState;
import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.tiles.TileCoordinate;
import com.trionix.maps.internal.tiles.TileManager;
import com.trionix.maps.internal.tiles.PlaceholderTileFactory;
import javafx.beans.value.ChangeListener;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.List;
import java.util.Objects;

/**
 * A layer that renders raster tiles from a {@link TileRetriever}.
 * This layer manages its own {@link TileManager} and caching strategy.
 */
public class TileLayer extends MapLayer {

    private static final Image PLACEHOLDER = PlaceholderTileFactory.placeholder();

    private final TileManager tileManager;
    private final Canvas canvas = new Canvas();
    private final GraphicsContext graphics = canvas.getGraphicsContext2D();
    private final MapState mapState = new MapState();

    private List<TileCoordinate> currentVisibleTiles = List.of();
    private boolean refreshPending;

    // Listeners to sync MapState
    private final ChangeListener<Number> centerLatListener = (obs, old, val) -> requestRefresh();
    private final ChangeListener<Number> centerLonListener = (obs, old, val) -> requestRefresh();
    private final ChangeListener<Number> zoomListener = (obs, old, val) -> requestRefresh();

    public TileLayer(TileRetriever retriever, TileCache cache) {
        Objects.requireNonNull(retriever, "retriever");
        Objects.requireNonNull(cache, "cache");
        this.tileManager = new TileManager(cache, retriever);

        getChildren().add(canvas);
        canvas.setManaged(false);
        canvas.setMouseTransparent(true);
    }

    @Override
    public void layerAdded(MapView mapView) {
        mapView.centerLatProperty().addListener(centerLatListener);
        mapView.centerLonProperty().addListener(centerLonListener);
        mapView.zoomProperty().addListener(zoomListener);
        requestRefresh();
    }

    @Override
    public void layerRemoved(MapView mapView) {
        mapView.centerLatProperty().removeListener(centerLatListener);
        mapView.centerLonProperty().removeListener(centerLonListener);
        mapView.zoomProperty().removeListener(zoomListener);
    }

    @Override
    public void layoutLayer(MapView mapView) {
        double width = mapView.getWidth();
        double height = mapView.getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        canvas.setWidth(width);
        canvas.setHeight(height);

        // Update local map state
        mapState.setViewportSize(width, height);
        mapState.setCenterLat(mapView.getCenterLat());
        mapState.setCenterLon(mapView.getCenterLon());
        mapState.setZoom(mapView.getZoom());

        refreshTiles();
        drawTiles(width, height);
    }

    private void requestRefresh() {
        requestLayerLayout();
    }

    private void refreshTiles() {
        if (mapState.getViewportWidth() <= 0 || mapState.getViewportHeight() <= 0) {
            return;
        }
        List<TileCoordinate> visible = mapState.visibleTiles();
        currentVisibleTiles = visible;
        // The consumer is called on FX thread when a tile loads
        tileManager.refreshTiles(visible, (coordinate, image) -> requestLayerLayout());
    }

    private void drawTiles(double width, double height) {
        graphics.clearRect(0.0, 0.0, width, height);
        if (currentVisibleTiles.isEmpty()) {
            return;
        }

        Projection projection = getProjection();
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

    /**
     * Clears the tile cache.
     */
    public void clearCache() {
        tileManager.clearCache();
        requestRefresh();
    }
}
