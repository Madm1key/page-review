package ru.mts.confluence.settings;

public class SpaceSettings {
    public static final String DEFAULT_BRANCH = "main";
    public static final String DEFAULT_FOLDER = "docs";

    private final boolean enabled;
    private final String repoUrl;
    private final String targetBranch;
    private final String baseFolder;
    private final boolean tokenSet;

    public SpaceSettings(boolean enabled, String repoUrl, String targetBranch, String baseFolder, boolean tokenSet) {
        this.enabled = enabled;
        this.repoUrl = repoUrl;
        this.targetBranch = targetBranch;
        this.baseFolder = baseFolder;
        this.tokenSet = tokenSet;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public String getBaseFolder() {
        return baseFolder;
    }

    public boolean isTokenSet() {
        return tokenSet;
    }

    public boolean isReady() {
        return enabled && !repoUrl.isEmpty() && tokenSet;
    }
}
