package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

@ExtendWith(ApplicationExtension.class)
class TieredTileCacheTest {

    private Image sampleImage;

    @Start
    private void start(Stage stage) {
        // Initialize JavaFX toolkit
    }

    @BeforeEach
    void setupImage() {
        WaitForAsyncUtils.waitForFxEvents();
        sampleImage = new WritableImage(1, 1);
    }

    @AfterEach
    void cleanup() {
        sampleImage = null;
    }

    @Test
    void getChecksL1FirstThenL2() {
        var l1 = new InMemoryTileCache(10);
        var l2 = new InMemoryTileCache(10);
        var tiered = new TieredTileCache(List.of(l1, l2));

        l1.put(1, 1, 1, sampleImage);
        assertThat(tiered.get(1, 1, 1)).isNotNull();

        l2.put(2, 2, 2, sampleImage);
        assertThat(tiered.get(2, 2, 2)).isNotNull();

        assertThat(tiered.get(3, 3, 3)).isNull();
    }

    @Test
    void l2HitPromotesTileToL1() {
        var l1 = new InMemoryTileCache(10);
        var l2 = new InMemoryTileCache(10);
        var tiered = new TieredTileCache(List.of(l1, l2));

        l2.put(1, 1, 1, sampleImage);
        assertThat(l1.get(1, 1, 1)).isNull();

        Image result = tiered.get(1, 1, 1);
        assertThat(result).isNotNull();
        assertThat(l1.get(1, 1, 1)).isNotNull();
    }

    @Test
    void putWritesToAllTiers() {
        var l1 = new InMemoryTileCache(10);
        var l2 = new InMemoryTileCache(10);
        var tiered = new TieredTileCache(List.of(l1, l2));

        tiered.put(1, 1, 1, sampleImage);

        assertThat(l1.get(1, 1, 1)).isNotNull();
        assertThat(l2.get(1, 1, 1)).isNotNull();
    }

    @Test
    void clearClearsAllTiers() {
        var l1 = new InMemoryTileCache(10);
        var l2 = new InMemoryTileCache(10);
        var tiered = new TieredTileCache(List.of(l1, l2));

        tiered.put(1, 1, 1, sampleImage);
        tiered.clear();

        assertThat(l1.get(1, 1, 1)).isNull();
        assertThat(l2.get(1, 1, 1)).isNull();
    }

    @Test
    void worksWithSingleCache() {
        var single = new InMemoryTileCache(10);
        var tiered = new TieredTileCache(List.of(single));

        tiered.put(1, 1, 1, sampleImage);

        assertThat(tiered.get(1, 1, 1)).isNotNull();
        assertThat(single.get(1, 1, 1)).isNotNull();
    }

    @Test
    void emptyListThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> new TieredTileCache(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null or empty");
    }

    @Test
    void nullListThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> new TieredTileCache(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null or empty");
    }

    @Test
    void listWithNullElementThrowsIllegalArgumentException() {
        List<TileCache> tiersWithNull = new ArrayList<>();
        tiersWithNull.add(new InMemoryTileCache(10));
        tiersWithNull.add(null);

        assertThatThrownBy(() -> new TieredTileCache(tiersWithNull))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain null");
    }

    @Test
    void promotionWorksWithThreeTiers() {
        var l1 = new InMemoryTileCache(10);
        var l2 = new InMemoryTileCache(10);
        var l3 = new InMemoryTileCache(10);
        var tiered = new TieredTileCache(List.of(l1, l2, l3));

        l3.put(1, 1, 1, sampleImage);

        assertThat(l1.get(1, 1, 1)).isNull();
        assertThat(l2.get(1, 1, 1)).isNull();

        Image result = tiered.get(1, 1, 1);
        assertThat(result).isNotNull();

        assertThat(l1.get(1, 1, 1)).isNotNull();
        assertThat(l2.get(1, 1, 1)).isNotNull();
    }

    @Test
    void nullImageThrowsNullPointerException() {
        var l1 = new InMemoryTileCache(10);
        var tiered = new TieredTileCache(List.of(l1));

        assertThatThrownBy(() -> tiered.put(1, 1, 1, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("image");
    }
}
