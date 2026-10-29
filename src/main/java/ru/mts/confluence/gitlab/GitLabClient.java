package ru.mts.confluence.gitlab;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Minimal GitLab API client used to check the repository settings. */
public class GitLabClient {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final String apiBase;
    private final String projectPath;
    private final String token;

    public GitLabClient(GitLabRepo repo, String token) {
        this(repo.getApiBase(), repo.getProjectPath(), token);
    }

    GitLabClient(String apiBase, String projectPath, String token) {
        this.apiBase = apiBase;
        this.projectPath = projectPath;
        this.token = token;
    }

    /** Checks that the token can read the project. */
    public void checkProject() throws GitLabException {
        get("/projects/" + encode(projectPath));
    }

    /** Checks that the branch exists and is readable. */
    public void checkBranch(String branch) throws GitLabException {
        get("/projects/" + encode(projectPath) + "/repository/branches/" + encode(branch));
    }

    private void get(String path) throws GitLabException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase + path))
                .timeout(REQUEST_TIMEOUT)
                .header("PRIVATE-TOKEN", token)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new GitLabException("Cannot connect to GitLab: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitLabException("Interrupted while contacting GitLab", e);
        }
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new GitLabException(status, "GitLab returned HTTP " + status);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
