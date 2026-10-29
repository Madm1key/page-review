package ru.mts.confluence.settings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpaceSettingsServiceTest {
    @Test
    public void normalizesFolder() {
        assertEquals("docs/a", SpaceSettingsService.normalizeFolder("  /docs\\a/ "));
        assertEquals("", SpaceSettingsService.normalizeFolder(null));
        assertEquals("", SpaceSettingsService.normalizeFolder("/"));
    }

    @Test
    public void rejectsPathTraversal() {
        assertTrue(SpaceSettingsService.isSafeFolder("docs/sub"));
        assertTrue(SpaceSettingsService.isSafeFolder(""));
        assertFalse(SpaceSettingsService.isSafeFolder("docs/../etc"));
        assertFalse(SpaceSettingsService.isSafeFolder("./docs"));
    }
}
