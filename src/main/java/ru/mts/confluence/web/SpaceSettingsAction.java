package ru.mts.confluence.web;

import com.atlassian.xwork.HttpMethod;
import com.atlassian.xwork.PermittedMethods;
import com.atlassian.confluence.spaces.actions.AbstractSpaceAdminAction;
import ru.mts.confluence.gitlab.GitLabRepo;
import ru.mts.confluence.settings.SpaceSettings;
import ru.mts.confluence.settings.SpaceSettingsService;

public class SpaceSettingsAction extends AbstractSpaceAdminAction {
    private SpaceSettingsService settingsService;

    private boolean enabled;
    private String repoUrl;
    private String targetBranch;
    private String baseFolder;
    private String token;
    private boolean tokenSet;
    private boolean saved;

    @Override
    public String doDefault() throws Exception {
        String result = super.doDefault();
        loadFromStore();
        return result;
    }

    @PermittedMethods(HttpMethod.POST)
    public String doSave() {
        String folder = SpaceSettingsService.normalizeFolder(baseFolder);
        String branch = targetBranch == null ? "" : targetBranch.trim();
        String repo = repoUrl == null ? "" : repoUrl.trim();

        if (!repo.isEmpty()) {
            try {
                GitLabRepo.parse(repo);
            } catch (IllegalArgumentException e) {
                addFieldError("repoUrl", getText("page-review.error.bad-repo"));
            }
        }
        if (branch.isEmpty()) {
            addFieldError("targetBranch", getText("page-review.settings.error.branch-required"));
        }
        if (!SpaceSettingsService.isSafeFolder(folder)) {
            addFieldError("baseFolder", getText("page-review.settings.error.folder"));
        }
        SpaceSettings current = settingsService.get(getKey());
        boolean hasToken = current.isTokenSet() || (token != null && !token.isEmpty());
        if (enabled && (repo.isEmpty() || !hasToken)) {
            addActionError(getText("page-review.error.not-configured"));
        }
        if (hasFieldErrors() || hasActionErrors()) {
            tokenSet = current.isTokenSet();
            return INPUT;
        }
        settingsService.save(getKey(), enabled, repo, branch, folder, token);
        token = null;
        loadFromStore();
        saved = true;
        return SUCCESS;
    }

    private void loadFromStore() {
        SpaceSettings s = settingsService.get(getKey());
        enabled = s.isEnabled();
        repoUrl = s.getRepoUrl();
        targetBranch = s.getTargetBranch();
        baseFolder = s.getBaseFolder();
        tokenSet = s.isTokenSet();
    }

    public void setSpaceSettingsService(SpaceSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public String getBaseFolder() {
        return baseFolder;
    }

    public void setBaseFolder(String baseFolder) {
        this.baseFolder = baseFolder;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isTokenSet() {
        return tokenSet;
    }

    public boolean isSaved() {
        return saved;
    }
}
