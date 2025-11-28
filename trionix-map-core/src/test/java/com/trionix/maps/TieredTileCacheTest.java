package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trionix.maps.testing.FxTestHarness;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TieredTileCacheTest {

    private static Image sampleImage;

    @BeforeAll
    static void setupImage() {
        sampleImage = FxTestHarness.callOnFxThread(() -> new WritableImage(1, 1));
    }

    @AfterAll
    static void cleanup() {
        sampleImage = null;
    }

    @Test
    void getChecksL1FirstThenL2() {
        var l1 = new InMemoryTileCache(10);
        var l2 = new InMemoryTileCache(10);
        var tiered = new TieredTileCache(List.of(l1, l2));

        // Put only in L1
        l1.put(1, 1, 1, sampleImage);

        // Should find in L1
        assertThat(tiered.get(1, 1, 1)).isNotNull();

        // Put only in L2
        l2.put(2, 2, 2, sampleImage);

        // Should find in L2
        assertThat(tiered.get(2, 2, 2)).isNotNull();

        // Miss in both
        assertThat(tiered.get(3, 3, 3)).isNull();
    }

    @Test
    void l2HitPromotesTileToL1() {
        var l1 = new InMemoryTileCache(10);
        var l2 = new InMemoryTileCache(10);
        var tiered = new TieredTileCache(List.of(l1, l2));

        // Put only in L2
        l2.put(1, 1, 1, sampleImage);

        // Verify L1 is empty
        assertThat(l1.get(1, 1, 1)).isNull();

        // Get from tiered cache - should promote to L1
        Image result = tiered.get(1, 1, 1);
        assertThat(result).isNotNull();

        // Now L1 should have it too
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

        // Put only in L3
        l3.put(1, 1, 1, sampleImage);

        // Verify L1 and L2 are empty
        assertThat(l1.get(1, 1, 1)).isNull();
        assertThat(l2.get(1, 1, 1)).isNull();

        // Get from tiered cache - should promote to L1 and L2
        Image result = tiered.get(1, 1, 1);
        assertThat(result).isNotNull();

        // Now L1 and L2 should have it too
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
