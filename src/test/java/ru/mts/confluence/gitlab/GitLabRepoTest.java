package ru.mts.confluence.gitlab;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GitLabRepoTest {
    @After
    public void clear() {
        System.clearProperty(GitLabRepo.ALLOWED_HOSTS_PROPERTY);
    }

    @Test
    public void parsesNestedGroupsAndGitSuffix() {
        GitLabRepo r = GitLabRepo.parse("https://gitlab.example.com:8443/a/b/repo.git/");
        assertEquals("https://gitlab.example.com:8443/api/v4", r.getApiBase());
        assertEquals("a/b/repo", r.getProjectPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsHttp() {
        GitLabRepo.parse("http://gitlab.example.com/a/b");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingProject() {
        GitLabRepo.parse("https://gitlab.example.com/onlygroup");
    }

    @Test(expected = IllegalArgumentException.class)
    public void enforcesAllowedHosts() {
        System.setProperty(GitLabRepo.ALLOWED_HOSTS_PROPERTY, "git.corp.local, other.local");
        GitLabRepo.parse("https://gitlab.example.com/a/b");
    }

    @Test
    public void allowsListedHost() {
        System.setProperty(GitLabRepo.ALLOWED_HOSTS_PROPERTY, "git.corp.local");
        assertEquals("a/b", GitLabRepo.parse("https://GIT.corp.local/a/b").getProjectPath());
    }
}
