package ru.mts.confluence.settings;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import java.nio.charset.StandardCharsets;
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
        str(s.get(NS + "repoUrl"), ""),
        str(s.get(NS + "targetBranch"), SpaceSettings.DEFAULT_BRANCH),
        !str(s.get(NS + "token"), "").isEmpty());
  }

  /** A null or empty newToken keeps the stored token. */
  public void save(String spaceKey, String repoUrl, String targetBranch, String newToken) {
    PluginSettings s = settingsFactory.createSettingsForKey(spaceKey);
    s.put(NS + "repoUrl", repoUrl);
    s.put(NS + "targetBranch", targetBranch);
    if (newToken != null && !newToken.isEmpty()) {
      s.put(NS + "token", cipher().encrypt(newToken));
    }
    s.remove(NS + "enabled");
    s.remove(NS + "baseFolder");
  }

  public String getToken(String spaceKey) {
    String stored = str(settingsFactory.createSettingsForKey(spaceKey).get(NS + "token"), "");
    return stored.isEmpty() ? null : cipher().decrypt(stored);
  }

  public static boolean isValidBranch(String branch) {
    if (branch == null
        || branch.isEmpty()
        || "HEAD".equals(branch)
        || branch.getBytes(StandardCharsets.UTF_8).length > 255
        || branch.startsWith("/")
        || branch.endsWith("/")
        || branch.endsWith(".")
        || branch.contains("//")
        || branch.contains("..")
        || branch.contains("@{")) {
      return false;
    }
    for (int i = 0; i < branch.length(); i++) {
      char c = branch.charAt(i);
      if (c <= 0x20 || c == 0x7f || "\\~^:?*[".indexOf(c) >= 0) {
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
