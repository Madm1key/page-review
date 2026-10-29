# Troubleshooting

> **Verified against code at commit `05bbcef` (2026-10-29).**
>
> Start with the symptom. Each entry lists the likely cause and the fix.

## The settings page is unstyled and shows text like "Demonstration Space ds true"

**Cause:** the SiteMesh `main` decorator is not applied. `/plugins/*` is excluded from automatic
decoration, and the template must request the layout with
`<meta name="decorator" content="main"/>`. Without it, `<content tag="...">` blocks leak their
text (the trailing `ds true` comes from the `spacekey` and `spaceaction` tags).

**Fix:** restore the meta tag in `space-settings.vm`, repackage, and reload. Verify the response
starts with `<!DOCTYPE html>`, contains stylesheet links, and has no literal `<content tag=`.
See [`ui-and-rendering.md`](ui-and-rendering.md).

## The Space Tools tab is not highlighted on the settings page

**Cause:** `selection` / `selectedSpaceToolsWebItem` were set to the link id (`page-review-settings`) instead of the
web-item key (`page-review-space-tools`). Confluence
compares against the web-item key.

**Fix:** use `page-review-space-tools` in both decorator params.

## The menu entry is missing

**Cause:** the user is not a space administrator, the plugin is not enabled, or the web item
failed to load.

**Fix:** check `SpacePermissionCondition` (`administer`), the plugin state, and the descriptor in
`atlassian-plugin.xml`. Remember that the entry appears under Space Tools → **Apps**
(`system.space.tools/addons`).

## `settings.action` returns 404

**Cause:** the plugin is not installed/enabled, the XWork namespace or action name changed, or
QuickReload did not apply the descriptor yet.

**Fix:** check the `atlassian-plugin.xml` XWork section, rebuild (`atlas-mvn package`), and check
the dev console log. The URL must be `/plugins/page-review/settings.action`.

## POST to `validate.action` or `save.action` returns 405

**Cause:** the endpoint was called with GET. Both mutating methods are POST-only via
`@PermittedMethods`.

**Fix:** submit the form (which posts) rather than opening the URL in a browser.

## "XSRF check failed" or a token-expired page

**Cause:** the POST did not include the `atl_token` hidden field, or the session/token expired.

**Fix:** use the rendered form (it contains `#form_xsrfToken()`); reload the page after a session
expiry. Custom clients must send the current `atl_token` and a logged-in cookie.

## "Access token is required" although a token was saved

**Cause:** the stored ciphertext is gone, cannot be decrypted (global key lost/rotated), or the
space key differs from the one used at save time.

**Fix:** enter the token again and save. If every space lost its token at once, the global
`ru.mts.confluence.page-review.cipherKey` was lost — re-enter tokens in each space. There is no
recovery without the key.

## GitLab returns 401

**Cause:** invalid, expired, or revoked token; wrong instance.

**Fix:** issue a personal/project/group access token with the `api` scope and save it. Validation
sends it as `PRIVATE-TOKEN`.

## GitLab returns 403

**Cause:** the token is valid but lacks rights for the project (insufficient role or scope).

**Fix:** grant at least the Developer role and the `api` scope.

## "Repository not found in GitLab (404)" for a URL that exists

**Cause:** wrong group/project path, private project not visible to the token, or a URL that
points at a subgroup/project combination the API cannot resolve.

**Fix:** compare the project path with the GitLab project page. The API receives the path
URL-encoded (slashes become `%2F`).

## "Invalid repository URL" for an apparently valid URL

**Cause:** `GitLabRepo.parse` rejects non-HTTPS schemes, userinfo, query/fragment components,
hosts outside `pagereview.gitlab.allowed-hosts`, and paths without a group segment.

**Fix:** use the exact form `https://host[:port]/group[/subgroup...]/repo[.git]` and check the
allowlist property on the Confluence JVM.

## "Branch ... was not found in the repository"

**Cause:** the branch does not exist or is not visible to the token; a 404 came back from the
branch endpoint.

**Fix:** check the branch name (case-sensitive) or create the branch.

## "Cannot connect to GitLab"

**Cause:** DNS/network/TLS failure, connection refused, timeout, or a 3xx redirect (redirects are
never followed).

**Fix:** check egress from the Confluence JVM, the URL, proxy settings, and the certificate trust
store for private CAs. A 3xx response is intentionally treated as an error.

## Validation succeeded/failed but nothing was saved

**By design.** `validate.action` checks the form values and never writes to the store. Press **Save** to persist.

## `atlas-mvn` works but plain `mvn` fails

**Cause:** plain Maven lacks the Atlassian plugin repositories/platform POM.

**Fix:** always use `atlas-mvn` / `atlas-package` / `atlas-run`.

## Changes are not visible in the running dev instance

**Cause:** only `atlas-mvn test` compiles classes; it does not redeploy the plugin.

**Fix:** run `atlas-mvn package` while `atlas-run` is running (QuickReload is enabled), then
reload the page. Restart `atlas-run` only if the instance is unhealthy.

## IntelliJ skips a breakpoint with "stepping in another thread"

**Cause:** each Confluence HTTP request is handled by a different Tomcat worker thread. While the
debugger is stepping in one thread, a breakpoint hit in another thread is skipped by design, and
IDEA shows the notification.

**Fix:** press Resume (F9) so the debugger is not stepping, then trigger the request; or step
inside the thread that handles the request (Debug → Threads → `http-nio-1990-exec-*`). Use
Suspend → Thread for critical breakpoints.

## Sources of truth

- [`ui-and-rendering.md`](ui-and-rendering.md) for decoration and tab selection
- [`gitlab-integration.md`](gitlab-integration.md) for URL rules and status mapping
- [`settings-and-secrets.md`](settings-and-secrets.md) for token storage and the global key
- the `atlas-run` console and `target/confluence/home/logs/` for runtime logs
