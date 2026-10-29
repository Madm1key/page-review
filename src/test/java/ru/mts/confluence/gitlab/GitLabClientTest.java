package ru.mts.confluence.gitlab;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class GitLabClientTest {
    private HttpServer server;
    private String apiBase;
    private final AtomicReference<String> lastToken = new AtomicReference<>();
    private final AtomicReference<String> lastPath = new AtomicReference<>();

    @Before
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v4", this::handle);
        server.start();
        apiBase = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v4";
    }

    @After
    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void checkProjectSendsTokenAndEncodesProjectPath() throws Exception {
        new GitLabClient(apiBase, "group/sub/repo", "secret").checkProject();

        assertEquals("secret", lastToken.get());
        assertEquals("/api/v4/projects/group%2Fsub%2Frepo", lastPath.get());
    }

    @Test
    public void checkBranchEncodesBranchPath() throws Exception {
        new GitLabClient(apiBase, "group/repo", "secret").checkBranch("feature/foo");

        assertEquals("/api/v4/projects/group%2Frepo/repository/branches/feature%2Ffoo", lastPath.get());
    }

    @Test
    public void reportsStatusWhenTokenIsRejected() {
        try {
            new GitLabClient(apiBase, "group/repo", "bad").checkProject();
            fail("Expected GitLabException");
        } catch (GitLabException e) {
            assertEquals(401, e.getStatus());
        }
    }

    @Test
    public void reportsStatusWhenBranchIsMissing() {
        try {
            new GitLabClient(apiBase, "group/repo", "secret").checkBranch("missing");
            fail("Expected GitLabException");
        } catch (GitLabException e) {
            assertEquals(404, e.getStatus());
        }
    }

    @Test
    public void reportsUnreachableGitLab() {
        server.stop(0);
        server = null;

        try {
            new GitLabClient(apiBase, "group/repo", "secret").checkProject();
            fail("Expected GitLabException");
        } catch (GitLabException e) {
            assertEquals(-1, e.getStatus());
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        String token = exchange.getRequestHeaders().getFirst("PRIVATE-TOKEN");
        String path = exchange.getRequestURI().getRawPath();
        lastToken.set(token);
        lastPath.set(path);

        int status = 200;
        if ("bad".equals(token)) {
            status = 401;
        } else if (path.endsWith("/repository/branches/missing")) {
            status = 404;
        }
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
