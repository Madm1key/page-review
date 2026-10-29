# page-review — wiki index

> Content catalog for the `page-review` wiki. Read this file first, then open only the pages you
> need.
> **Verified against code at commit `05bbcef` (2026-10-29).**

## What this repository is

A server-side Confluence 8.5 plugin (Atlassian Server/Data Center) that stores per-space GitLab
settings — an HTTPS repository URL, a target branch, and an access token — and validates that the
token can read the configured project and branch. It does not export pages and has no REST module
or client-side JavaScript.

## How to use this wiki

- **Queries start here.** Find the relevant page in the catalog, read it, then follow its
  **Sources of truth** into the code if anything is ambiguous.
- **The code is the source layer.** The immutable sources for this wiki are the files under
  `src/`, `pom.xml`, and the relevant Confluence webapp templates. There is no separate `raw/`
  directory because those sources already live in git and are not modified by the wiki.
- **The schema is `AGENTS.md`.** It defines the structure, the update workflow, and the lint
  procedure for this wiki.
- **The log is `docs/log.md`.** It is append-only and records every wiki change.

Suggested reading paths:

| Goal | Read in order |
| --- | --- |
| Understand the plugin | `architecture.md` → `settings-and-secrets.md` → `gitlab-integration.md` → `ui-and-rendering.md` |
| Change behavior | `architecture.md` → `build-test-run.md` → the relevant topic page |
| Debug a problem | `troubleshooting.md` → the relevant topic page |
| Review a change | `decisions.md` → `architecture.md` → the relevant topic page |

## Catalog

### Overview

| Page | Summary |
| --- | --- |
| [`architecture.md`](architecture.md) | Components, request flows, file map, wiring, and invariants of the whole plugin |

### Concepts

| Page | Summary |
| --- | --- |
| [`settings-and-secrets.md`](settings-and-secrets.md) | Storage keys, defaults, AES-256-GCM token encryption, key-loss consequences |
| [`gitlab-integration.md`](gitlab-integration.md) | Accepted repository URLs, host allowlist, GitLab API calls, timeouts, error mapping |
| [`ui-and-rendering.md`](ui-and-rendering.md) | Space Tools entry, XWork actions, Velocity template, SiteMesh decorator requirement, i18n |

### Operations

| Page | Summary |
| --- | --- |
| [`build-test-run.md`](build-test-run.md) | `atlas-mvn` commands, dev instance, QuickReload, test suites and conventions |
| [`troubleshooting.md`](troubleshooting.md) | Symptom → cause → fix for rendering, routing, XSRF, token, and GitLab failures |

### Reference

| Page | Summary |
| --- | --- |
| [`glossary.md`](glossary.md) | Confluence, OSGi, and GitLab terms as used in this repository |

### Decisions

| Page | Summary |
| --- | --- |
| [`decisions.md`](decisions.md) | ADR-style rationale: settings-only scope, server-rendered validation, encryption, HTTPS-only URLs, no redirects, decorator meta tag, branch check |

### Control files

| File | Purpose |
| --- | --- |
| [`log.md`](log.md) | Append-only chronological record of wiki changes |
| [`../AGENTS.md`](../AGENTS.md) | Schema: wiki structure, update workflow, lint rules, maintenance checklist |

## Page conventions

- One topic per page, with a descriptive H1 and self-contained sections.
- Every page ends with **Sources of truth** and, where useful, **How to verify**.
- Metadata lives in the `Verified against code at commit ...` line; YAML frontmatter is optional
  and only added if tooling needs it.
- Links to code use repo-relative paths in backticks, without line numbers.
- No secrets, tokens, or personal data in any page.
