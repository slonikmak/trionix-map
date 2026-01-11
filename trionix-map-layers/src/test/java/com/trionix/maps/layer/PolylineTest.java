package com.trionix.maps.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trionix.maps.GeoPoint;
import java.util.List;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class PolylineTest {

    @Test
    void testDefaults() {
        Polyline polyline = new Polyline();
        assertTrue(polyline.getPoints().isEmpty());
        assertEquals(Color.BLUE, polyline.getStrokeColor());
        assertEquals(2.0, polyline.getStrokeWidth());
        assertFalse(polyline.isMarkersVisible());
        assertFalse(polyline.isEditable());
    }

    @Test
    void testPointsManipulation() {
        Polyline polyline = new Polyline();
        GeoPoint p1 = GeoPoint.of(10, 10);
        GeoPoint p2 = GeoPoint.of(20, 20);

        polyline.addPoint(p1);
        assertEquals(1, polyline.getPoints().size());
        assertEquals(p1, polyline.getPoints().get(0));

        polyline.setPoints(List.of(p1, p2));
        assertEquals(2, polyline.getPoints().size());
        assertEquals(p2, polyline.getPoints().get(1));

        GeoPoint p3 = GeoPoint.of(30, 30);
        polyline.updatePoint(1, p3);
        assertEquals(p3, polyline.getPoints().get(1));
    }

    @Test
    void testStyling() {
        Polyline polyline = new Polyline();
        polyline.setStrokeColor(Color.RED);
        assertEquals(Color.RED, polyline.getStrokeColor());

        polyline.setStrokeWidth(5.0);
        assertEquals(5.0, polyline.getStrokeWidth());

        polyline.setStrokeDashArray(List.of(10.0, 5.0));
        assertEquals(List.of(10.0, 5.0), polyline.getStrokeDashArray());
    }
}
