package com.trionix.maps.internal.projection;

/**
 * Projection abstraction for converting between latitude/longitude and
 * Web Mercator pixel coordinates.
 */
public interface Projection {

    /** Pixel dimension of a single tile. */
    double TILE_SIZE = 256.0;

    /** Maximum latitude supported by the Web Mercator projection. */
    double MAX_LATITUDE = 85.05112878;

    /**
     * Converts latitude/longitude to global pixel coordinates for the given zoom level.
     *
     * @param latitude  latitude in degrees
     * @param longitude longitude in degrees
     * @param zoom      zoom level (integer, >= 0)
     * @return global pixel coordinate pair
     */
    PixelCoordinate latLonToPixel(double latitude, double longitude, int zoom);

    /**
     * Converts global pixel coordinates to latitude/longitude.
     *
     * @param pixelX pixel coordinate on the X axis (global, origin at top-left)
     * @param pixelY pixel coordinate on the Y axis (global, origin at top-left)
     * @param zoom   zoom level (integer, >= 0)
     * @return latitude/longitude pair
     */
    LatLon pixelToLatLon(double pixelX, double pixelY, int zoom);

    /** Simple record to represent a 2D pixel coordinate. */
    record PixelCoordinate(double x, double y) {
    }

    /** Simple record to represent a latitude/longitude pair. */
    record LatLon(double latitude, double longitude) {
    }
}
