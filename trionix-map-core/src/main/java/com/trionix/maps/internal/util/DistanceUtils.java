package com.trionix.maps.internal.util;

/**
 * Utilities for calculating geographic distances and scale.
 */
public final class DistanceUtils {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final int TILE_SIZE_PIXELS = 256;

    private DistanceUtils() {
        // utility class
    }

    /**
     * Calculates the great-circle distance between two geographic points using the haversine formula.
     *
     * @param lat1 latitude of the first point in degrees
     * @param lon1 longitude of the first point in degrees
     * @param lat2 latitude of the second point in degrees
     * @param lon2 longitude of the second point in degrees
     * @return distance in meters
     */
    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Calculates the number of meters represented by one pixel at a given latitude and zoom level,
     * assuming Web Mercator projection.
     *
     * @param latitude the latitude at which to calculate the scale
     * @param zoomLevel the zoom level (integer)
     * @return meters per pixel
     */
    public static double metersPerPixel(double latitude, int zoomLevel) {
        double mapWidthPixels = TILE_SIZE_PIXELS * (1L << zoomLevel);
        double equatorMeters = 2.0 * Math.PI * EARTH_RADIUS_METERS;
        double metersPerPixelAtEquator = equatorMeters / mapWidthPixels;
        return metersPerPixelAtEquator * Math.cos(Math.toRadians(latitude));
    }

    /**
     * Returns a "nice" rounded distance value suitable for display. Chooses from standard increments
     * like 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, etc.
     *
     * @param meters the raw distance in meters
     * @return a rounded "nice" value in meters
     */
    public static double getNiceDistance(double meters) {
        if (meters <= 0) {
            return 0;
        }

        // Standard nice increments: 1, 2, 5, 10, 20, 50, 100, ...
        double magnitude = Math.pow(10, Math.floor(Math.log10(meters)));
        double normalized = meters / magnitude;

        double niceFactor;
        if (normalized < 1.5) {
            niceFactor = 1;
        } else if (normalized < 3.5) {
            niceFactor = 2;
        } else if (normalized < 7.5) {
            niceFactor = 5;
        } else {
            niceFactor = 10;
        }

        return niceFactor * magnitude;
    }

    /**
     * Formats a distance in meters to a human-readable string with appropriate units.
     *
     * @param meters the distance in meters
     * @return formatted string (e.g., "500 m", "2 km")
     */
    public static String formatDistance(double meters) {
        if (meters >= 1000) {
            double km = meters / 1000.0;
            if (km >= 10) {
                return String.format("%d km", (int) Math.round(km));
            } else {
                return String.format("%.1f km", km);
            }
        } else {
            return String.format("%d m", (int) Math.round(meters));
        }
    }
}
