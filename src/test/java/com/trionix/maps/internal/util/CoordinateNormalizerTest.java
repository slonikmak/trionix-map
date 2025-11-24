package com.trionix.maps.internal.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CoordinateNormalizerTest {

    @Test
    void clampsLatitudeToWebMercatorBounds() {
        assertThat(CoordinateNormalizer.clampLatitude(90.0)).isCloseTo(85.0511, offset());
        assertThat(CoordinateNormalizer.clampLatitude(-90.0)).isCloseTo(-85.0511, offset());
    }

    @Test
    void normalizesLongitudeIntoRange() {
        assertThat(CoordinateNormalizer.normalizeLongitude(200.0)).isEqualTo(-160.0);
        assertThat(CoordinateNormalizer.normalizeLongitude(-200.0)).isEqualTo(160.0);
        assertThat(CoordinateNormalizer.normalizeLongitude(540.0)).isEqualTo(-180.0);
    }

    @Test
    void clampsZoomBetweenBounds() {
        assertThat(CoordinateNormalizer.clampZoom(-1.0, 0.0, 19.0)).isEqualTo(0.0);
        assertThat(CoordinateNormalizer.clampZoom(25.0, 0.0, 19.0)).isEqualTo(19.0);
        assertThat(CoordinateNormalizer.clampZoom(5.5, 0.0, 19.0)).isEqualTo(5.5);
    }

    private static org.assertj.core.data.Offset<Double> offset() {
        return org.assertj.core.data.Offset.offset(0.001);
    }
}
