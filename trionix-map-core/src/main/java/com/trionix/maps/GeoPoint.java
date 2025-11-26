package com.trionix.maps;

/**
 * An immutable geographic coordinate consisting of latitude and longitude in degrees.
 *
 * @param latitude the latitude in degrees
 * @param longitude the longitude in degrees
 */
public record GeoPoint(double latitude, double longitude) {
    /**
     * Creates a new {@code GeoPoint} with the specified coordinates.
     *
     * @param latitude the latitude in degrees
     * @param longitude the longitude in degrees
     * @return a new {@code GeoPoint}
     */
    public static GeoPoint of(double latitude, double longitude) {
        return new GeoPoint(latitude, longitude);
    }
}
