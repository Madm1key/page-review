# Build, test, and run

> **Verified against code at commit `05bbcef` (2026-10-29).**

## Prerequisites

- Java 11 (the project compiles with `maven.compiler.release=11`).
- Atlassian Plugin SDK 9.11.2 (`atlas-mvn`, `atlas-run`, `atlas-package` on `PATH`).
- Use the SDK's Maven, not plain `mvn`: plain Maven lacks the Atlassian repositories and fails to
  resolve the Confluence platform POM (`Unknown packaging: atlassian-plugin`, missing dependency
  versions).

## Commands

| Command                                           | What it does                                      |
|---------------------------------------------------|---------------------------------------------------|
| `atlas-mvn test`                                  | Compiles and runs all tests                       |
| `atlas-mvn test -Dtest=TokenCipherTest`           | Runs one test class                               |
| `atlas-mvn test -Dtest=TokenCipherTest#roundTrip` | Runs one test method                              |
| `atlas-mvn package`                               | Builds `target/page-review-1.0.0-SNAPSHOT.jar`    |
| `atlas-package`                                   | SDK wrapper for `mvn package`                     |
| `atlas-run`                                       | Starts a dev Confluence with the plugin installed |

Version properties (`confluence.version` 8.5.31, AMPS 9.11.2) live in `pom.xml`.

## Dev instance

`atlas-run` starts Confluence on port 1990, usually at `http://<host>:1990/confluence`. The URL is
derived from the machine hostname in the SDK setup used for this project. The Space Tools page is
then available under Space Tools → Apps.

Logs: most Confluence output goes to the `atlas-run` console. Auxiliary logs live under
`target/confluence/home/logs/`. There is no separate main `atlassian-confluence.log` created by
this SDK setup.

## QuickReload

`enableQuickReload` is enabled in `pom.xml`. While `atlas-run` is running:

- `atlas-mvn package` redeploys the plugin;
- changed classes, descriptor actions, Velocity templates, and i18n resources are picked up
  without restarting Confluence.

Useful loop: edit → `atlas-mvn package` → reload the browser page. A full `atlas-run` restart is
only needed when the dev instance itself is unhealthy.

## Tests

JUnit 4 with plain `assert*`; no Mockito. 23 tests at the verified commit:

| Test class                 | Covers                                                                                   |
|----------------------------|------------------------------------------------------------------------------------------|
| `TokenCipherTest`          | AES-GCM round trip, `v1:` format, corrupt input                                          |
| `SpaceSettingsServiceTest` | Defaults, per-space isolation, encryption, blank-token preservation                      |
| `GitLabRepoTest`           | URL grammar, normalization, derived API base/project path, host allowlist                |
| `GitLabClientTest`         | Path encoding, `PRIVATE-TOKEN`, 401/404 mapping, unreachable GitLab (local `HttpServer`) |

Conventions:

- `SpaceSettingsServiceTest` uses an in-memory `PluginSettingsFactory`/`PluginSettings`
  implementation; follow that pattern for new service tests.
- Tests that touch `pagereview.gitlab.allowed-hosts` clear it in `@After`.
- `GitLabClientTest` uses the JDK `com.sun.net.httpserver.HttpServer` on `127.0.0.1` with an
  ephemeral port, and the package-private `GitLabClient(apiBase, projectPath, token)` constructor.
  Production traffic still goes through `GitLabRepo` and HTTPS.

Run everything before packaging; packaging without tests is possible with `-DskipTests` but not
recommended for changes.

## Packaging notes

- `atlassian-plugin.xml` and other resources are Maven-filtered: `${...}` placeholders resolve
  from `pom.xml` properties at build time.
- Confluence, Spring, SAL, OSGi, and Struts/XWork APIs are `provided` scope and must not be
  bundled.
- The expected artifact is `target/page-review-1.0.0-SNAPSHOT.jar`.

## Sources of truth

- `pom.xml`
- `AGENTS.md` (short build/architecture guide)
- `src/test/java/ru/mts/confluence/`
