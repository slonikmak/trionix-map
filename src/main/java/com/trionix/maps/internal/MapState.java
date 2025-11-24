package com.trionix.maps.internal;

import static com.trionix.maps.internal.util.CoordinateNormalizer.clampLatitude;
import static com.trionix.maps.internal.util.CoordinateNormalizer.clampZoom;
import static com.trionix.maps.internal.util.CoordinateNormalizer.normalizeLongitude;

import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.projection.WebMercatorProjection;
import com.trionix.maps.internal.tiles.TileCoordinate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Maintains the current viewport state (center, zoom, viewport size) and
 * calculates the set of visible tiles for rendering.
 */
public final class MapState {

    public static final double DEFAULT_MIN_ZOOM = 0.0;
    public static final double DEFAULT_MAX_ZOOM = 19.0;

    private final Projection projection;
    private final double minZoom;
    private final double maxZoom;

    private double centerLat;
    private double centerLon;
    private double zoom;
    private double viewportWidth;
    private double viewportHeight;

    public MapState() {
        this(new WebMercatorProjection(), DEFAULT_MIN_ZOOM, DEFAULT_MAX_ZOOM);
    }

    public MapState(Projection projection, double minZoom, double maxZoom) {
        this.projection = Objects.requireNonNull(projection, "projection");
        if (minZoom > maxZoom) {
            throw new IllegalArgumentException("minZoom must be <= maxZoom");
        }
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.zoom = clampZoom(1.0, minZoom, maxZoom);
    }

    public double getCenterLat() {
        return centerLat;
    }

    public void setCenterLat(double centerLat) {
        this.centerLat = clampLatitude(centerLat);
    }

    public double getCenterLon() {
        return centerLon;
    }

    public void setCenterLon(double centerLon) {
        this.centerLon = normalizeLongitude(centerLon);
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = clampZoom(zoom, minZoom, maxZoom);
    }

    public double getViewportWidth() {
        return viewportWidth;
    }

    public double getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportSize(double width, double height) {
        this.viewportWidth = Math.max(0.0, width);
        this.viewportHeight = Math.max(0.0, height);
    }

    public int discreteZoomLevel() {
        return Math.max(0, (int) Math.floor(zoom));
    }

    /**
     * Calculates a list of visible tile coordinates (row-major order) for the
     * current state. Longitude wrapping is handled automatically.
     */
    public List<TileCoordinate> visibleTiles() {
        if (viewportWidth <= 0.0 || viewportHeight <= 0.0) {
            return List.of();
        }
        int zoomLevel = discreteZoomLevel();
        double tileSize = Projection.TILE_SIZE;
        var centerPixels = projection.latLonToPixel(centerLat, centerLon, zoomLevel);
        double halfWidth = viewportWidth / 2.0;
        double halfHeight = viewportHeight / 2.0;

        double minPixelX = centerPixels.x() - halfWidth;
        double maxPixelXExclusive = centerPixels.x() + halfWidth;
        double minPixelY = centerPixels.y() - halfHeight;
        double maxPixelYExclusive = centerPixels.y() + halfHeight;

        long tileCount = Math.max(1L, 1L << zoomLevel);
        long startX = (long) Math.floor(minPixelX / tileSize);
        long endX = (long) Math.ceil(maxPixelXExclusive / tileSize) - 1;
        long startY = Math.max(0L, (long) Math.floor(minPixelY / tileSize));
        long endY = Math.min(tileCount - 1, (long) Math.ceil(maxPixelYExclusive / tileSize) - 1);

        if (endX < startX || endY < startY) {
            return List.of();
        }

        int columns = (int) (endX - startX + 1);
        int rows = (int) (endY - startY + 1);
        List<TileCoordinate> tiles = new ArrayList<>(columns * rows);
        for (long tileY = startY; tileY <= endY; tileY++) {
            for (long tileX = startX; tileX <= endX; tileX++) {
                long wrappedX = wrapTileX(tileX, tileCount);
                tiles.add(new TileCoordinate(zoomLevel, wrappedX, tileY));
            }
        }
        return List.copyOf(tiles);
    }

    private static long wrapTileX(long tileX, long tileCount) {
        long wrapped = tileX % tileCount;
        return wrapped < 0 ? wrapped + tileCount : wrapped;
    }
}
