package ru.mts.confluence.settings;

public class SpaceSettings {
  public static final String DEFAULT_BRANCH = "main";

  private final String repoUrl;
  private final String targetBranch;
  private final boolean tokenSet;

  public SpaceSettings(String repoUrl, String targetBranch, boolean tokenSet) {
    this.repoUrl = repoUrl;
    this.targetBranch = targetBranch;
    this.tokenSet = tokenSet;
  }

  public String getRepoUrl() {
    return repoUrl;
  }

  public String getTargetBranch() {
    return targetBranch;
  }

  public boolean isTokenSet() {
    return tokenSet;
  }
}
