# page-review — wiki log

Append-only chronological record of wiki changes. Newest entries go at the bottom; use
`tail docs/log.md` for the latest state and `grep '^## '` to list events.

Entry format: `## [YYYY-MM-DD] <create|update|restructure|lint> | <title>`.

## [2026-10-29] create | Initial LLM wiki

- Created the wiki under `docs/` with architecture, settings and secrets, GitLab integration,
  UI and rendering, build/test/run, troubleshooting, glossary, and decisions pages.
- Added `llms.txt` as a machine-readable entry point.
- Verified all pages against code at commit `05bbcef`.

## [2026-10-30] update | Schema file renamed to AGENTS.md

- Renamed `CLAUDE.md` to `AGENTS.md` (Claude Code loads `AGENTS.md` when the project has no
  `CLAUDE.md`).
- Updated references in the wiki and `llms.txt`.

## [2026-10-30] restructure | Adopt index.md + log.md control files

- Replaced `docs/README.md` with `docs/index.md`: the index is now a content catalog grouped by
  category, with one-line summaries.
- Added `docs/log.md` as the append-only chronological record.
- Moved wiki conventions, update workflow, and the lint procedure into `AGENTS.md` (the schema
  layer), including rules for filing reusable answers back and extending pages before duplicating.
- Updated `llms.txt` and the root `README` to point at `docs/index.md`.
