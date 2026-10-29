# GitLab integration

> **Verified against code at commit `05bbcef` (2026-10-29).**

The plugin makes exactly two kinds of GitLab calls, both on demand from the **Check connection**
button. There is no background polling, retry, or page export.

## Accepted repository URLs

`GitLabRepo.parse` accepts:

```
https://host[:port]/group[/subgroup...]/repo[.git]
```

and rejects everything else, including:

- non-HTTPS schemes (no `http://`);
- URLs without a host, or with userinfo (`user:pass@`), query (`?`), or fragment (`#`);
- paths without at least one `/` (a project path needs a group and a project);
- hosts outside the optional allowlist.

Normalization: leading/trailing slashes are removed and a trailing `.git` is stripped.

Derived values:

| Value        | Example (input `https://gitlab.example.com/team/sub/proj.git`) |
|--------------|----------------------------------------------------------------|
| API base     | `https://gitlab.example.com/api/v4`                            |
| Project path | `team/sub/proj`                                                |

Ports are preserved (`https://host:8443/...` → `https://host:8443/api/v4`).

### Host allowlist

If the JVM system property `pagereview.gitlab.allowed-hosts` is set to a comma-separated list,
the host must match one entry (case-insensitive, read at call time, no restart needed for the
argument itself to be re-read). If the property is empty or absent, any host is allowed.

This is the main SSRF control: the action is space-admin-only, but the allowlist lets an
operator restrict egress to known GitLab instances.

## GitLab client

`GitLabClient` uses `java.net.http.HttpClient` (JDK 11) and is constructed with a parsed
`GitLabRepo` and a plaintext token.

| Behavior        | Value                                        |
|-----------------|----------------------------------------------|
| Connect timeout | 5 s                                          |
| Request timeout | 10 s                                         |
| Redirects       | Never followed (`HttpClient.Redirect.NEVER`) |
| Auth header     | `PRIVATE-TOKEN: <token>`                     |
| Accept          | `application/json`                           |
| Success         | Any 2xx status                               |

Why no redirects: a redirect to another host would send `PRIVATE-TOKEN` there. Failing on a 3xx is
intentional.

Endpoints (the project path is URL-encoded, so `/` becomes `%2F`):

- `GET {apiBase}/projects/{projectPath}` — checks that the token can read the project.
- `GET {apiBase}/projects/{projectPath}/repository/branches/{branch}` — checks that the branch is
  visible.

The client does not parse JSON responses. It only needs the status code. A non-2xx status becomes
a `GitLabException` carrying the code; I/O failures become `GitLabException` with status `-1`.

## Validation flow

1. `SpaceSettingsAction.doValidate` parses the repository URL from the form.
2. It validates the branch with `SpaceSettingsService.isValidBranch`.
3. It resolves the effective token: typed value if non-empty, otherwise the stored one; no token
   means "Access token is required".
4. `checkProject()` runs first.
5. `checkBranch(branch)` runs second.
6. The action maps failures to i18n text and re-renders the form with a success or error message.
   Nothing is saved by this action.

## Error mapping

| Source                               | `GitLabException.getStatus()` | i18n key                                    | Shown as                             |
|--------------------------------------|-------------------------------|---------------------------------------------|--------------------------------------|
| Invalid repository URL (before HTTP) | —                             | `page-review.error.bad-repo`                | Invalid repository URL               |
| Invalid branch (before HTTP)         | —                             | `page-review.settings.error.branch`         | Enter a valid Git branch name        |
| No token available                   | —                             | `page-review.settings.error.token-required` | Access token is required             |
| Project or branch call               | 401                           | `page-review.error.gitlab-401`              | Token rejected                       |
| Project or branch call               | 403                           | `page-review.error.gitlab-403`              | Token has no rights                  |
| Project call                         | 404                           | `page-review.error.gitlab-404`              | Repository not found                 |
| Branch call                          | 404                           | `page-review.settings.check.branch-missing` | Branch not found (includes the name) |
| Any call                             | `-1`                          | `page-review.error.gitlab-unreachable`      | Cannot connect                       |
| Any call                             | any other status              | `page-review.error.gitlab-other`            | Error with HTTP code                 |

Both English (`page-review.properties`) and Russian (`page-review_ru.properties`) strings must
exist for every key.

## Security properties

- HTTPS only, enforced by `GitLabRepo`.
- Token sent only to the parsed host; no redirects; no userinfo in URLs.
- Validation is POST-only (`@PermittedMethods(HttpMethod.POST)`) and subject to the Confluence
  XSRF token in the form.
- The action inherits the space-admin permission check from `AbstractSpaceAdminAction`.
- The token is never included in messages, logs, or the rendered page.
- The allowlist property is the operator control for outbound destinations.

## Limitations (intentional)

- No retries and no backoff.
- No JSON parsing: a 2xx response is enough; an error body is not surfaced.
- No proxy configuration in the plugin; the JVM/`HttpClient` defaults apply.
- 3xx responses are treated as errors.
- Self-signed GitLab certificates require the Confluence JVM truststore to trust them.

## Sources of truth

- `src/main/java/ru/mts/confluence/gitlab/GitLabRepo.java`
- `src/main/java/ru/mts/confluence/gitlab/GitLabClient.java`
- `src/main/java/ru/mts/confluence/gitlab/GitLabException.java`
- `src/main/java/ru/mts/confluence/web/SpaceSettingsAction.java` (`doValidate`)
- `src/main/resources/page-review.properties`, `page-review_ru.properties`

## How to verify

```bash
atlas-mvn test -Dtest=GitLabRepoTest,GitLabClientTest
```

`GitLabClientTest` starts a local `com.sun.net.httpserver.HttpServer` on `127.0.0.1` and covers
path encoding, token transfer, 401/404 mapping, and the unreachable case. It uses the
package-private constructor `GitLabClient(apiBase, projectPath, token)`, so tests can use plain
HTTP on localhost; production always goes through `GitLabRepo` and HTTPS.
