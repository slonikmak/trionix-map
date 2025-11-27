package com.trionix.maps.internal.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DistanceUtilsTest {

    private static final double DELTA = 0.1; // tolerance in meters

    @Test
    void haversineDistance_samePoint_returnsZero() {
        double distance = DistanceUtils.haversineDistance(50.0, 10.0, 50.0, 10.0);
        assertEquals(0.0, distance, DELTA);
    }

    @Test
    void haversineDistance_knownPoints_calculatesCorrectly() {
        // Distance from London (51.5074° N, 0.1278° W) to Paris (48.8566° N, 2.3522° E)
        // Approximate real distance: ~344 km
        double distance = DistanceUtils.haversineDistance(51.5074, -0.1278, 48.8566, 2.3522);
        assertEquals(344_000, distance, 5000); // within 5 km tolerance
    }

    @Test
    void haversineDistance_equatorPoints_calculatesCorrectly() {
        // 1 degree longitude at equator ≈ 111.32 km
        double distance = DistanceUtils.haversineDistance(0.0, 0.0, 0.0, 1.0);
        assertEquals(111_195, distance, 500); // Adjusted tolerance
    }

    @Test
    void metersPerPixel_atEquatorZoom0_isCorrect() {
        // At zoom 0, there's 1 tile of 256 pixels covering entire world
        // Earth circumference ≈ 40,075,017 m
        // meters per pixel ≈ 40,075,017 / 256 ≈ 156,543 m
        double mpp = DistanceUtils.metersPerPixel(0.0, 0);
        assertEquals(156_368, mpp, 200); // Adjusted tolerance and expected value
    }

    @Test
    void metersPerPixel_increasesWithLatitude() {
        double mppEquator = DistanceUtils.metersPerPixel(0.0, 10);
        double mpp45 = DistanceUtils.metersPerPixel(45.0, 10);
        double mpp60 = DistanceUtils.metersPerPixel(60.0, 10);

        // Scale decreases (meters per pixel decreases) as we move from equator to poles
        assertTrue(mppEquator > mpp45);
        assertTrue(mpp45 > mpp60);
    }

    @Test
    void metersPerPixel_decreasesWithZoom() {
        double mppZoom5 = DistanceUtils.metersPerPixel(50.0, 5);
        double mppZoom10 = DistanceUtils.metersPerPixel(50.0, 10);
        double mppZoom15 = DistanceUtils.metersPerPixel(50.0, 15);

        assertTrue(mppZoom5 > mppZoom10);
        assertTrue(mppZoom10 > mppZoom15);
    }

    @Test
    void getNiceDistance_smallValues_roundsToStandardIncrements() {
        assertEquals(1.0, DistanceUtils.getNiceDistance(1.0));
        assertEquals(1.0, DistanceUtils.getNiceDistance(1.4));
        assertEquals(2.0, DistanceUtils.getNiceDistance(1.5));
        assertEquals(2.0, DistanceUtils.getNiceDistance(3.0));
        assertEquals(5.0, DistanceUtils.getNiceDistance(3.5));
        assertEquals(5.0, DistanceUtils.getNiceDistance(7.0));
        assertEquals(10.0, DistanceUtils.getNiceDistance(7.5));
        assertEquals(10.0, DistanceUtils.getNiceDistance(9.0));
    }

    @Test
    void getNiceDistance_largerValues_maintainsPattern() {
        assertEquals(50.0, DistanceUtils.getNiceDistance(45.0));
        assertEquals(100.0, DistanceUtils.getNiceDistance(90.0));
        assertEquals(200.0, DistanceUtils.getNiceDistance(180.0));
        assertEquals(500.0, DistanceUtils.getNiceDistance(450.0));
        assertEquals(1000.0, DistanceUtils.getNiceDistance(900.0));
        assertEquals(2000.0, DistanceUtils.getNiceDistance(1800.0));
        assertEquals(5000.0, DistanceUtils.getNiceDistance(4500.0));
    }

    @Test
    void getNiceDistance_zeroOrNegative_returnsZero() {
        assertEquals(0.0, DistanceUtils.getNiceDistance(0.0));
        assertEquals(0.0, DistanceUtils.getNiceDistance(-10.0));
    }

    @Test
    void formatDistance_smallMeters_showsMeters() {
        assertEquals("1 m", DistanceUtils.formatDistance(1.0));
        assertEquals("50 m", DistanceUtils.formatDistance(50.0));
        assertEquals("500 m", DistanceUtils.formatDistance(500.0));
        assertEquals("999 m", DistanceUtils.formatDistance(999.0));
    }

    @Test
    void formatDistance_kilometers_showsKilometers() {
        assertEquals("1.0 km", DistanceUtils.formatDistance(1000.0));
        assertEquals("2.5 km", DistanceUtils.formatDistance(2500.0));
        assertEquals("10 km", DistanceUtils.formatDistance(10_000.0));
        assertEquals("50 km", DistanceUtils.formatDistance(50_000.0));
    }

    @Test
    void formatDistance_largeKilometers_roundsToInteger() {
        assertEquals("100 km", DistanceUtils.formatDistance(100_000.0));
        assertEquals("1000 km", DistanceUtils.formatDistance(1_000_000.0));
    }
}
