package ru.mts.confluence.gitlab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class GitLabClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class FileAction {
        private final String filePath;
        private final byte[] content;
        private final boolean update;

        public FileAction(String filePath, byte[] content, boolean update) {
            this.filePath = filePath;
            this.content = content;
            this.update = update;
        }
    }

    public static class MergeRequest {
        private final String webUrl;
        private final long iid;

        public MergeRequest(String webUrl, long iid) {
            this.webUrl = webUrl;
            this.iid = iid;
        }

        public String getWebUrl() {
            return webUrl;
        }

        public long getIid() {
            return iid;
        }
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final String apiBase;
    private final String project;
    private final String token;

    public GitLabClient(GitLabRepo repo, String token) {
        this.apiBase = repo.getApiBase();
        this.project = enc(repo.getProjectPath());
        this.token = token;
    }

    /** Checks that the token can read the project; returns its web URL. */
    public String checkProject() throws GitLabException {
        return call("GET", "/projects/" + project, null).path("web_url").asText("");
    }

    public void checkBranch(String branch) throws GitLabException {
        call("GET", "/projects/" + project + "/repository/branches/" + enc(branch), null);
    }

    public Optional<byte[]> getFile(String path, String ref) throws GitLabException {
        try {
            JsonNode node = call("GET", "/projects/" + project + "/repository/files/" + enc(path) + "?ref=" + enc(ref), null);
            return Optional.of(Base64.getMimeDecoder().decode(node.path("content").asText("")));
        } catch (GitLabException e) {
            if (e.getStatus() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public void createCommit(String branch, String startBranch, String message, List<FileAction> actions)
            throws GitLabException {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("branch", branch);
        body.put("start_branch", startBranch);
        body.put("commit_message", message);
        ArrayNode arr = body.putArray("actions");
        for (FileAction a : actions) {
            ObjectNode n = arr.addObject();
            n.put("action", a.update ? "update" : "create");
            n.put("file_path", a.filePath);
            n.put("encoding", "base64");
            n.put("content", Base64.getEncoder().encodeToString(a.content));
        }
        call("POST", "/projects/" + project + "/repository/commits", body);
    }

    public MergeRequest createMergeRequest(String sourceBranch, String targetBranch, String title, String description)
            throws GitLabException {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("source_branch", sourceBranch);
        body.put("target_branch", targetBranch);
        body.put("title", title);
        body.put("description", description);
        body.put("remove_source_branch", true);
        JsonNode mr = call("POST", "/projects/" + project + "/merge_requests", body);
        return new MergeRequest(mr.path("web_url").asText(""), mr.path("iid").asLong());
    }

    private JsonNode call(String method, String path, ObjectNode body) throws GitLabException {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(apiBase + path))
                .timeout(Duration.ofSeconds(30))
                .header("PRIVATE-TOKEN", token)
                .header("Accept", "application/json");
        if (body == null) {
            req.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            req.header("Content-Type", "application/json");
            req.method(method, HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        }
        HttpResponse<String> res;
        try {
            res = http.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new GitLabException("Cannot connect to GitLab: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitLabException("Interrupted", e);
        }
        int status = res.statusCode();
        if (status / 100 != 2) {
            throw new GitLabException(status, errorMessage(res.body()));
        }
        try {
            return res.body() == null || res.body().isEmpty() ? MAPPER.createObjectNode() : MAPPER.readTree(res.body());
        } catch (IOException e) {
            throw new GitLabException("Unexpected response from GitLab", e);
        }
    }

    private static String errorMessage(String body) {
        try {
            JsonNode n = MAPPER.readTree(body);
            JsonNode m = n.has("message") ? n.get("message") : n.get("error");
            if (m != null) {
                return m.isTextual() ? m.asText() : m.toString();
            }
        } catch (IOException | RuntimeException ignored) {
            // fall through
        }
        return "";
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
