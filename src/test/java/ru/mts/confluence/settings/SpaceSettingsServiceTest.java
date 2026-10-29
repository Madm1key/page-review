package ru.mts.confluence.settings;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SpaceSettingsServiceTest {
    private static final String NS = "ru.mts.confluence.page-review.";

    @Test
    public void readsExistingKeysWithoutMigration() {
        MemoryFactory factory = new MemoryFactory();
        factory.space("A").put(NS + "repoUrl", "https://gitlab.example.com/group/repo.git");
        factory.space("A").put(NS + "targetBranch", "develop");
        factory.space("A").put(NS + "token", "ciphertext");

        SpaceSettings settings = new SpaceSettingsService(factory).get("A");

        assertEquals("https://gitlab.example.com/group/repo.git", settings.getRepoUrl());
        assertEquals("develop", settings.getTargetBranch());
        assertTrue(settings.isTokenSet());
    }

    @Test
    public void usesDefaultsAndKeepsSpacesIsolated() {
        MemoryFactory factory = new MemoryFactory();
        SpaceSettingsService service = new SpaceSettingsService(factory);
        service.save("A", "https://gitlab.example.com/a/repo", "main", "secret");

        assertEquals("https://gitlab.example.com/a/repo", service.get("A").getRepoUrl());
        assertTrue(service.get("A").isTokenSet());
        assertEquals("", service.get("B").getRepoUrl());
        assertEquals("main", service.get("B").getTargetBranch());
        assertFalse(service.get("B").isTokenSet());
    }

    @Test
    public void encryptsTokenAndBlankTokenPreservesCiphertext() {
        MemoryFactory factory = new MemoryFactory();
        SpaceSettingsService service = new SpaceSettingsService(factory);
        service.save("A", "https://gitlab.example.com/a/repo", "main", "secret");
        String first = (String) factory.space("A").get(NS + "token");

        assertNotEquals("secret", first);
        assertEquals("secret", service.getToken("A"));

        service.save("A", "https://gitlab.example.com/a/repo", "develop", "");
        assertEquals(first, factory.space("A").get(NS + "token"));

        service.save("A", "https://gitlab.example.com/a/repo", "develop", "replacement");
        assertNotEquals(first, factory.space("A").get(NS + "token"));
        assertEquals("replacement", service.getToken("A"));
    }

    @Test
    public void successfulSaveRemovesOnlyObsoleteKeys() {
        MemoryFactory factory = new MemoryFactory();
        MapSettings settings = factory.space("A");
        settings.put(NS + "enabled", "true");
        settings.put(NS + "baseFolder", "docs");
        settings.put(NS + "other", "keep");

        new SpaceSettingsService(factory).save(
                "A", "https://gitlab.example.com/a/repo", "main", "secret");

        assertNull(settings.get(NS + "enabled"));
        assertNull(settings.get(NS + "baseFolder"));
        assertEquals("keep", settings.get(NS + "other"));
    }

    @Test
    public void validatesGitBranchNames() {
        assertTrue(SpaceSettingsService.isValidBranch("main"));
        assertTrue(SpaceSettingsService.isValidBranch("release/8.5"));

        String[] invalid = {"", "HEAD", "/main", "main/", "main.", "a//b", "a..b",
                "a@{b", "with space", "a\\b", "a~b", "a^b", "a:b", "a?b", "a*b", "a[b"};
        for (String branch : invalid) {
            assertFalse(branch, SpaceSettingsService.isValidBranch(branch));
        }
        assertFalse(SpaceSettingsService.isValidBranch("x".repeat(256)));
    }

    private static final class MemoryFactory implements PluginSettingsFactory {
        private final Map<String, MapSettings> spaces = new HashMap<>();
        private final MapSettings global = new MapSettings();

        MapSettings space(String key) {
            return spaces.computeIfAbsent(key, ignored -> new MapSettings());
        }

        @Override
        public PluginSettings createSettingsForKey(String key) {
            return space(key);
        }

        @Override
        public PluginSettings createGlobalSettings() {
            return global;
        }
    }

    private static final class MapSettings implements PluginSettings {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Object get(String key) {
            return values.get(key);
        }

        @Override
        public Object put(String key, Object value) {
            return values.put(key, value);
        }

        @Override
        public Object remove(String key) {
            return values.remove(key);
        }
    }
}
