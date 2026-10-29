package ru.mts.confluence.gitlab;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class GitLabRepo {
    public static final String ALLOWED_HOSTS_PROPERTY = "pagereview.gitlab.allowed-hosts";

    private final String apiBase;
    private final String projectPath;

    private GitLabRepo(String apiBase, String projectPath) {
        this.apiBase = apiBase;
        this.projectPath = projectPath;
    }

    public String getApiBase() {
        return apiBase;
    }

    public String getProjectPath() {
        return projectPath;
    }

    /** Parses https://host[:port]/group/[subgroup/]repo[.git]; throws IllegalArgumentException if invalid. */
    public static GitLabRepo parse(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("empty");
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("syntax");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("https");
        }
        checkAllowedHost(uri.getHost());
        String path = uri.getPath() == null ? "" : uri.getPath();
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.endsWith(".git")) {
            path = path.substring(0, path.length() - 4);
        }
        if (!path.contains("/")) {
            throw new IllegalArgumentException("path");
        }
        String base = "https://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "") + "/api/v4";
        return new GitLabRepo(base, path);
    }

    private static void checkAllowedHost(String host) {
        String prop = System.getProperty(ALLOWED_HOSTS_PROPERTY, "").trim();
        if (prop.isEmpty()) {
            return;
        }
        List<String> allowed = Arrays.stream(prop.split(","))
                .map(h -> h.trim().toLowerCase(Locale.ROOT))
                .filter(h -> !h.isEmpty())
                .collect(Collectors.toList());
        if (!allowed.contains(host.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("host");
        }
    }
}
