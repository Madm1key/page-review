package ru.mts.confluence.web;

import com.atlassian.confluence.spaces.actions.AbstractSpaceAdminAction;
import com.atlassian.xwork.HttpMethod;
import com.atlassian.xwork.PermittedMethods;
import ru.mts.confluence.gitlab.GitLabClient;
import ru.mts.confluence.gitlab.GitLabException;
import ru.mts.confluence.gitlab.GitLabRepo;
import ru.mts.confluence.settings.SpaceSettings;
import ru.mts.confluence.settings.SpaceSettingsService;

public class SpaceSettingsAction extends AbstractSpaceAdminAction {
    private SpaceSettingsService settingsService;

    private String repoUrl;
    private String targetBranch;
    private String token;
    private boolean tokenSet;
    private boolean saved;
    private boolean checked;
    private boolean checkOk;
    private String checkMessage;

    @Override
    public String doDefault() throws Exception {
        String result = super.doDefault();
        loadFromStore();
        return result;
    }

    @PermittedMethods(HttpMethod.POST)
    public String doSave() {
        String branch = targetBranch == null ? "" : targetBranch.trim();
        String repo = repoUrl == null ? "" : repoUrl.trim();

        try {
            GitLabRepo.parse(repo);
        } catch (IllegalArgumentException e) {
            addFieldError("repoUrl", getText("page-review.error.bad-repo"));
        }
        if (!SpaceSettingsService.isValidBranch(branch)) {
            addFieldError("targetBranch", getText("page-review.settings.error.branch"));
        }

        SpaceSettings current = settingsService.get(getKey());
        boolean hasNewToken = token != null && !token.isEmpty();
        if (!current.isTokenSet() && !hasNewToken) {
            addFieldError("token", getText("page-review.settings.error.token-required"));
        }
        if (hasFieldErrors()) {
            tokenSet = current.isTokenSet();
            return INPUT;
        }

        settingsService.save(getKey(), repo, branch, token);
        token = null;
        loadFromStore();
        saved = true;
        return SUCCESS;
    }

    /** Checks the values currently entered in the form without saving them. */
    @PermittedMethods(HttpMethod.POST)
    public String doValidate() {
        checked = true;
        String repo = repoUrl == null ? "" : repoUrl.trim();
        String branch = targetBranch == null ? "" : targetBranch.trim();

        GitLabRepo gitLabRepo;
        try {
            gitLabRepo = GitLabRepo.parse(repo);
        } catch (IllegalArgumentException e) {
            checkOk = false;
            checkMessage = getText("page-review.error.bad-repo");
            return SUCCESS;
        }
        if (!SpaceSettingsService.isValidBranch(branch)) {
            checkOk = false;
            checkMessage = getText("page-review.settings.error.branch");
            return SUCCESS;
        }

        String effectiveToken;
        try {
            effectiveToken = token == null || token.trim().isEmpty()
                    ? settingsService.getToken(getKey())
                    : token.trim();
        } catch (RuntimeException e) {
            checkOk = false;
            checkMessage = getText("page-review.error.generic");
            return SUCCESS;
        }
        if (effectiveToken == null) {
            checkOk = false;
            checkMessage = getText("page-review.settings.error.token-required");
            return SUCCESS;
        }

        GitLabClient client = new GitLabClient(gitLabRepo, effectiveToken);
        try {
            client.checkProject();
        } catch (GitLabException e) {
            checkOk = false;
            checkMessage = describeFailure(e);
            return SUCCESS;
        }
        try {
            client.checkBranch(branch);
        } catch (GitLabException e) {
            checkOk = false;
            checkMessage = e.getStatus() == 404
                    ? getText("page-review.settings.check.branch-missing", new String[]{branch})
                    : describeFailure(e);
            return SUCCESS;
        }
        checkOk = true;
        checkMessage = getText("page-review.settings.check.ok");
        return SUCCESS;
    }

    private String describeFailure(GitLabException e) {
        switch (e.getStatus()) {
            case 401:
                return getText("page-review.error.gitlab-401");
            case 403:
                return getText("page-review.error.gitlab-403");
            case 404:
                return getText("page-review.error.gitlab-404");
            case -1:
                return getText("page-review.error.gitlab-unreachable");
            default:
                return getText("page-review.error.gitlab-other",
                        new String[]{Integer.toString(e.getStatus())});
        }
    }

    private void loadFromStore() {
        SpaceSettings s = settingsService.get(getKey());
        repoUrl = s.getRepoUrl();
        targetBranch = s.getTargetBranch();
        tokenSet = s.isTokenSet();
    }

    public void setSpaceSettingsService(SpaceSettingsService settingsService) {
        this.settingsService = settingsService;
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

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isTokenSet() {
        return tokenSet;
    }

    public boolean isSaved() {
        return saved;
    }

    public boolean isChecked() {
        return checked;
    }

    public boolean isCheckOk() {
        return checkOk;
    }

    public String getCheckMessage() {
        return checkMessage;
    }
}
