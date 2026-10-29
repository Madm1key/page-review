package ru.mts.confluence.settings;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.util.Base64;

public class SpaceSettingsService {
    private static final String NS = "ru.mts.confluence.page-review.";
    private static final String CIPHER_KEY = NS + "cipherKey";

    private final PluginSettingsFactory settingsFactory;
    private volatile TokenCipher cipher;

    public SpaceSettingsService(PluginSettingsFactory settingsFactory) {
        this.settingsFactory = settingsFactory;
    }

    public SpaceSettings get(String spaceKey) {
        PluginSettings s = settingsFactory.createSettingsForKey(spaceKey);
        return new SpaceSettings(
                "true".equals(s.get(NS + "enabled")),
                str(s.get(NS + "repoUrl"), ""),
                str(s.get(NS + "targetBranch"), SpaceSettings.DEFAULT_BRANCH),
                str(s.get(NS + "baseFolder"), SpaceSettings.DEFAULT_FOLDER),
                !str(s.get(NS + "token"), "").isEmpty());
    }

    /** A null or empty newToken keeps the stored token. */
    public void save(String spaceKey, boolean enabled, String repoUrl, String targetBranch,
                     String baseFolder, String newToken) {
        PluginSettings s = settingsFactory.createSettingsForKey(spaceKey);
        s.put(NS + "enabled", Boolean.toString(enabled));
        s.put(NS + "repoUrl", repoUrl);
        s.put(NS + "targetBranch", targetBranch);
        s.put(NS + "baseFolder", baseFolder);
        if (newToken != null && !newToken.isEmpty()) {
            s.put(NS + "token", cipher().encrypt(newToken));
        }
    }

    public String getToken(String spaceKey) {
        String stored = str(settingsFactory.createSettingsForKey(spaceKey).get(NS + "token"), "");
        return stored.isEmpty() ? null : cipher().decrypt(stored);
    }

    public static String normalizeFolder(String folder) {
        String f = folder == null ? "" : folder.trim().replace('\\', '/');
        while (f.startsWith("/")) {
            f = f.substring(1);
        }
        while (f.endsWith("/")) {
            f = f.substring(0, f.length() - 1);
        }
        return f;
    }

    public static boolean isSafeFolder(String normalizedFolder) {
        for (String part : normalizedFolder.split("/")) {
            if (part.equals("..") || part.equals(".")) {
                return false;
            }
        }
        return true;
    }

    private TokenCipher cipher() {
        TokenCipher c = cipher;
        if (c == null) {
            synchronized (this) {
                if (cipher == null) {
                    PluginSettings global = settingsFactory.createGlobalSettings();
                    Object stored = global.get(CIPHER_KEY);
                    if (stored == null) {
                        stored = Base64.getEncoder().encodeToString(TokenCipher.generateKey());
                        global.put(CIPHER_KEY, stored);
                    }
                    cipher = new TokenCipher(Base64.getDecoder().decode((String) stored));
                }
                c = cipher;
            }
        }
        return c;
    }

    private static String str(Object value, String def) {
        return value == null ? def : value.toString();
    }
}
