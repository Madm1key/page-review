# AGENTS.md

This file provides guidance to coding agents and engineers working with code in this repository.

## What this is

A server-side Confluence 8.5 plugin (Atlassian Server/Data Center, not Cloud) that stores per-space GitLab settings: an HTTPS repository URL, a target branch, and an access token. The settings page can validate the connection by asking GitLab for the project and the branch. The plugin does not export pages.

## Build and test

Java 11 and the Atlassian Plugin SDK 9.11.2 are required. Use the SDK's Maven wrapper rather than plain `mvn`: plain Maven here lacks the Atlassian repositories and fails to resolve the Confluence platform POM (`Unknown packaging: atlassian-plugin`, missing dependency versions).

    atlas-mvn test                                   # all tests
    atlas-mvn test -Dtest=TokenCipherTest            # one test class
    atlas-mvn test -Dtest=TokenCipherTest#roundTrip  # one test method
    atlas-package                                    # build target/page-review-1.0.0-SNAPSHOT.jar
    atlas-run                                        # launch a dev Confluence with the plugin installed

`atlas-unit-test` and `atlas-package` are thin wrappers around `mvn test` / `mvn package` using the SDK's bundled Maven and repositories. Version properties (`confluence.version` 8.5.31, AMPS 9.11.2) live in `pom.xml`.

`enableQuickReload` is on: while `atlas-run` is running, `atlas-mvn package` redeploys the plugin, and changed classes, descriptor actions, Velocity templates, and i18n are picked up without restarting Confluence.

## Architecture

### Wiring: XML → Spring Java config → OSGi services

- `src/main/resources/atlassian-plugin.xml` declares the i18n bundle, the space-tools web item (space-admin-only via `SpacePermissionCondition`), and an XWork module exposing `settings.action` (`doDefault`), `save.action` (`doSave`), and `validate.action` (`doValidate`), all backed by `SpaceSettingsAction` and rendering the same Velocity template. The actions receive the service through `<external-ref name="spaceSettingsService">spaceSettingsService</external-ref>` (setter injection), so the referenced name must match the Spring bean id.
- `src/main/resources/META-INF/spring/plugin-context.xml` component-scans `ru.mts.confluence.config`.
- `PageReviewJavaConfig` is the only Spring configuration: it imports the OSGi Java-config support beans and registers `PluginSettingsFactory` (via `importOsgiService`) and `SpaceSettingsService`. Register any new OSGi services here.

### Settings and secrets

`SpaceSettingsService` stores per-space data in SAL `PluginSettings` under `ru.mts.confluence.page-review.*` keys. A space with no saved settings reads as an empty repository URL, the `main` branch (`SpaceSettings.DEFAULT_BRANCH`), and no token. `get()` exposes only a `tokenSet` boolean, never the token; `getToken()` is the only path to plaintext. A blank or null token on save preserves the existing ciphertext. The AES-256 key is generated on first use and stored in global `PluginSettings` (`ru.mts.confluence.page-review.cipherKey`), shared across all spaces — if it is lost, stored tokens can no longer be decrypted. `TokenCipher` uses AES/GCM with a random 12-byte IV and a `v1:` prefix (`"v1:" + Base64(IV‖ciphertext)`). `save()` also deletes the obsolete `enabled` and `baseFolder` keys from the earlier schema.

### Validation

- `GitLabRepo.parse` accepts only `https://host[:port]/group[/subgroup...]/repo[.git]` and rejects userinfo, query, and fragment components. When the JVM system property `pagereview.gitlab.allowed-hosts` is set (comma-separated, case-insensitive), the host must be in that list; it is read at call time. The class derives the API base (`https://host[:port]/api/v4`) and project path.
- `SpaceSettingsService.isValidBranch` enforces a subset of `git check-ref-format` rules (no spaces, `..`, `~^:?*[`, leading/trailing `/`, etc.).
- `SpaceSettingsAction.doSave` is POST-only (`@PermittedMethods`), re-validates the repo URL, branch, and token (required on first save), and returns `INPUT` with field errors while preserving `tokenSet`. On success it saves, clears the token field, reloads from the store, and sets `saved`, then renders the same Velocity template.
- `SpaceSettingsAction.doValidate` (the "check connection" button, `validate.action`) is POST-only and checks the values currently in the form without saving. It uses the typed token or, when the field is empty, the stored one, then calls `GitLabClient.checkProject` and `checkBranch`, maps HTTP 401/403/404 and connection failures to i18n messages, and re-renders the template with `checked`/`checkOk`/`checkMessage`. `GitLabClient` uses `java.net.http.HttpClient` and never follows redirects, so the token is only sent to the configured host.

### UI strings and template

All user-facing text lives in `page-review.properties` with the Russian translation in `page-review_ru.properties`; keep both files in sync. The template `templates/page-review/space-settings.vm` reads strings via `$action.getText(...)`, includes `#form_xsrfToken()` in the POST form, and HTML-encodes values with `$htmlUtil.htmlEncode`. The second submit button uses `formaction="validate.action"`, so the same form (and its XSRF token and `key`) posts to the validation action.

When a message needs a `{0}` placeholder, call `getText(key, new String[]{arg})`: `ConfluenceActionSupport.getText(String, String)` is only a fallback helper and does not run `MessageFormat`.

### Page rendering (SiteMesh)

Confluence maps `/plugins/*` to the `none` decorator, so a plugin action page is not wrapped in the standard layout automatically. The `<meta name="decorator" content="main"/>` line in `space-settings.vm` makes SiteMesh's `PageDecoratorMapper` apply `main.vmd`; the nested `#applyDecorator("root")` calls then add the space sidebar and the space-tools panel. Without the meta tag the page is served unstyled and the `<content tag="...">` blocks from the space decorators appear as plain text. `selectedSpaceToolsWebItem` / `selection` must be the web-item key `page-review-space-tools` — not the `linkId` — otherwise the Space tools tab is not highlighted.

## Testing conventions

JUnit 4 with plain `assert*` (no Mockito). `SpaceSettingsServiceTest` uses an in-memory `PluginSettingsFactory`/`PluginSettings` implementation; follow that pattern for new service tests. Tests touching `pagereview.gitlab.allowed-hosts` clear it in `@After`. `GitLabClientTest` starts a local JDK `com.sun.net.httpserver.HttpServer` and covers URL encoding, token transfer, and status mapping.

## Documentation wiki (schema)

Long-form documentation lives in `docs/`. The wiki follows the persistent-wiki pattern: the code
and Confluence assets are the immutable source layer, `docs/` is the LLM-maintained wiki, and this
file is the schema that defines how to maintain it.

**Files**

- `docs/index.md` — content catalog; read it first for any question and update it when pages are
  added, removed, or re-scoped.
- `docs/log.md` — append-only chronological record; never rewrite history, only append entries in
  the form `## [YYYY-MM-DD] <create|update|restructure|lint> | <title>`.
- `docs/*.md` — one topic per page; each ends with **Sources of truth** and, where useful,
  **How to verify**.

**Workflow**

- **Query:** read `docs/index.md`, then only the relevant pages; cite file paths; fall back to the
  code if a page and the code disagree. If an answer is reusable, file it as a new page or extend
  an existing one, then update the index and the log.
- **Ingest/update:** after a behavior change, update every affected page in the same commit,
  update the index, and append an `update` entry to the log. New pages get an index entry and a
  `create` entry.
- **Lint (on request):** check for contradictions between pages, stale claims, orphan pages without
  inbound links, important concepts missing a page, missing cross-references, and gaps. Report
  findings before rewriting.
- **Ownership:** agents maintain the wiki; humans curate tasks and review. The code is the source
  of truth; the wiki records intent, invariants, and non-obvious behavior.

**Rules**

- Verify pages against a named commit (`Verified against code at commit ...`).
- Prefer extending an existing page over creating a near-duplicate; rewrite summaries instead of
  appending new sections indefinitely.
- Keep links relative; reference code by path without line numbers.
- No secrets, tokens, or personal data in the wiki.

## Constraints

- Target Java 11 (`maven.compiler.release=11`); avoid newer language features.
- Confluence, Spring, SAL, OSGi, and Struts/XWork APIs are `provided` scope — never bundle them into the plugin.
- `atlassian-plugin.xml` and other resources are Maven-filtered: `${...}` placeholders resolve from `pom.xml` properties at build time.
