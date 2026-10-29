# Decisions

> **Verified against code at commit `05bbcef` (2026-10-29).**
>
> Lightweight ADR log. New decisions get the next number and a date. A superseded decision stays
> in the file with its status changed and a link to the replacement.

## ADR-001 — Settings-only scope; page export removed

- **Status:** accepted
- **Context:** an earlier iteration converted pages to Markdown, created branches/commits, and
  opened merge requests through a REST resource and page-view JavaScript.
- **Decision:** the plugin now only stores per-space GitLab settings and validates connectivity.
  Export code, the REST module, the page button, and the related CSS/JS were removed.
- **Consequences:** a much smaller surface (no browser JS, no REST, no merge-request logic);
  the target branch is stored for future use but only validated today.
- **Alternatives:** keep export behind a feature flag — rejected as unnecessary maintenance for
  an unused feature.

## ADR-002 — Validation is a server-rendered form button

- **Status:** accepted
- **Context:** the old "Test connection" button used `AJS.$.ajax` against a REST endpoint.
- **Decision:** the form has a second submit button with `formaction="validate.action"`. The
  action checks the entered values and re-renders the same template with a success/error message.
- **Consequences:** no client-side state or web-resource module; XSRF and permissions use the
  existing form/action path; a full page reload on each check.
- **Alternatives:** restore a REST + JS button — rejected for extra modules and duplicated
  permission/XSRF handling.

## ADR-003 — AES-256-GCM with one global cipher key

- **Status:** accepted
- **Context:** access tokens must be stored at rest in SAL `PluginSettings`.
- **Decision:** encrypt only the token with AES/GCM (`v1:` + Base64 (IV ‖ ciphertext)); generate one
  32-byte key on first use and keep it in global settings.
- **Consequences:** simple key management, but key loss breaks every space and the key is not
  rotatable through the UI. A leak affects all spaces.
- **Alternatives:** per-space keys (more state, still lost with the same storage); KMS integration (not available in
  this deployment); plaintext (unacceptable).

## ADR-004 — HTTPS-only repository URLs with an optional host allowlist

- **Status:** accepted
- **Context:** the server makes outbound requests to an admin-supplied URL.
- **Decision:** `GitLabRepo.parse` accepts only HTTPS URLs of the form
  `https://host[:port]/group[/subgroup...]/repo[.git]`, rejects userinfo/query/fragment, and
  optionally enforces `pagereview.gitlab.allowed-hosts`.
- **Consequences:** tokens never travel over plain HTTP; operators can restrict egress. Operators
  who need HTTP or another form must change the code.
- **Alternatives:** support `http://` — rejected; free-form URLs — rejected (SSRF risk).

## ADR-005 — GitLab client never follows redirects

- **Status:** accepted
- **Context:** `PRIVATE-TOKEN` is sent on every API request.
- **Decision:** `HttpClient.Redirect.NEVER`. Any non-2xx (including 3xx) is an error.
- **Consequences:** a redirect (wrong base path, HTTP→HTTPS, login portal) fails validation instead
  of leaking the token to another host.
- **Alternatives:** follow same-host redirects — rejected as more logic for little benefit.

## ADR-006 — Get the standard layout via a SiteMesh meta tag

- **Status:** accepted
- **Context:** Confluence maps `/plugins/*` to the `none` decorator, so plugin pages are not
  decorated automatically. The template already used the space decorators.
- **Decision:** add `<meta name="decorator" content="main"/>` so SiteMesh's `PageDecoratorMapper`
  applies `main.vmd`; keep the nested `#applyDecorator("root")` calls for the sidebar and panel.
- **Consequences:** the page gets the normal header, sidebar, CSS, and JS. Removing the tag
  silently breaks styling and leaks `<content>` text.
- **Alternatives:** register a custom plugin decorator — rejected as unnecessary; build the whole
  document by hand — rejected (web resources and theme integration would have to be duplicated).

## ADR-007 — Validation checks both the project and the branch

- **Status:** accepted
- **Context:** a valid token with a typo in the branch would pass a project-only check and fail
  later at the first real operation.
- **Decision:** `checkProject()` then `checkBranch(branch)`; a 404 from the branch endpoint gets a
  dedicated message that includes the branch name.
- **Consequences:** one extra API call per check; clearer diagnostics.
- **Alternatives:** project-only check — rejected as too weak.

## Sources of truth

- `docs/architecture.md`, `docs/settings-and-secrets.md`, `docs/gitlab-integration.md`,
  `docs/ui-and-rendering.md`
- The code paths referenced by those pages.
