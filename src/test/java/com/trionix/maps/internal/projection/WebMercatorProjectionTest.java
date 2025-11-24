package com.trionix.maps.internal.projection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WebMercatorProjectionTest {

    private final Projection projection = new WebMercatorProjection();

    @Test
    void convertsEquatorPrimeMeridianAtZoomOne() {
        Projection.PixelCoordinate coordinate = projection.latLonToPixel(0.0, 0.0, 1);
        assertThat(coordinate.x()).isCloseTo(256.0, withinOnePixel());
        assertThat(coordinate.y()).isCloseTo(256.0, withinOnePixel());
    }

    @Test
    void convertsPixelBackToLatLon() {
        Projection.LatLon latLon = projection.pixelToLatLon(256.0, 256.0, 1);
        assertThat(latLon.latitude()).isCloseTo(0.0, withinOneTenthDegree());
        assertThat(latLon.longitude()).isCloseTo(0.0, withinOneTenthDegree());
    }

    @Test
    void supportsHighZoomLevels() {
        Projection.PixelCoordinate coordinate = projection.latLonToPixel(48.8566, 2.3522, 18);
        double max = Projection.TILE_SIZE * Math.pow(2, 18);
        assertThat(coordinate.x()).isBetween(0.0, max);
        assertThat(coordinate.y()).isBetween(0.0, max);
    }

    private static org.assertj.core.data.Offset<Double> withinOnePixel() {
        return org.assertj.core.data.Offset.offset(1.0);
    }

    private static org.assertj.core.data.Offset<Double> withinOneTenthDegree() {
        return org.assertj.core.data.Offset.offset(0.1);
    }
}
