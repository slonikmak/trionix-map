package com.trionix.maps.internal.projection;

import static com.trionix.maps.internal.util.CoordinateNormalizer.clampLatitude;
import static com.trionix.maps.internal.util.CoordinateNormalizer.normalizeLongitude;

/**
 * Web Mercator projection utilities for converting between geographic
 * coordinates and global pixel coordinates used by slippy map tiles.
 */
public final class WebMercatorProjection implements Projection {

    /**
     * Shared singleton instance of the Web Mercator projection.
     * Use this instead of creating new instances.
     */
    public static final Projection INSTANCE = new WebMercatorProjection();

    /**
     * Package-private constructor. Use {@link #INSTANCE} instead.
     */
    WebMercatorProjection() {
    }

    @Override
    public PixelCoordinate latLonToPixel(double latitude, double longitude, int zoom) {
        double lat = Math.toRadians(clampLatitude(latitude));
        double lon = Math.toRadians(normalizeLongitude(longitude));

        double scale = TILE_SIZE * Math.pow(2, zoom);
        double x = (lon + Math.PI) / (2 * Math.PI) * scale;
        double y = (1 - Math.log(Math.tan(lat) + 1 / Math.cos(lat)) / Math.PI) / 2 * scale;
        return new PixelCoordinate(x, y);
    }

    @Override
    public LatLon pixelToLatLon(double pixelX, double pixelY, int zoom) {
        double scale = TILE_SIZE * Math.pow(2, zoom);
        double lon = pixelX / scale * 360.0 - 180.0;
        double n = Math.PI - 2.0 * Math.PI * pixelY / scale;
        double lat = Math.toDegrees(Math.atan(Math.sinh(n)));
        return new LatLon(lat, lon);
    }
}
