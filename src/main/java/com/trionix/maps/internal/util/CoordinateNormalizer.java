package com.trionix.maps.internal.util;

import com.trionix.maps.internal.projection.Projection;

/** Utility methods for clamping and normalizing map coordinates. */
public final class CoordinateNormalizer {

    private CoordinateNormalizer() {
    }

    /**
     * Clamps the latitude to the valid Web Mercator band.
     *
     * @param latitude latitude in degrees
     * @return latitude clamped to [-MAX_LATITUDE, MAX_LATITUDE]
     */
    public static double clampLatitude(double latitude) {
        double max = Projection.MAX_LATITUDE;
        if (latitude > max) {
            return max;
        }
        if (latitude < -max) {
            return -max;
        }
        return latitude;
    }

    /**
     * Normalizes longitude to the [-180, 180) range.
     *
     * @param longitude longitude in degrees
     * @return normalized longitude
     */
    public static double normalizeLongitude(double longitude) {
        double result = longitude;
        while (result < -180.0) {
            result += 360.0;
        }
        while (result >= 180.0) {
            result -= 360.0;
        }
        return result;
    }

    /**
     * Clamps zoom to the provided bounds.
     *
     * @param zoom    zoom level
     * @param minZoom minimum zoom inclusive
     * @param maxZoom maximum zoom inclusive
     * @return clamped zoom
     */
    public static double clampZoom(double zoom, double minZoom, double maxZoom) {
        if (zoom < minZoom) {
            return minZoom;
        }
        if (zoom > maxZoom) {
            return maxZoom;
        }
        return zoom;
    }
}
