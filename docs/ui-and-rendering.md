# UI and page rendering

> **Verified against code at commit `05bbcef` (2026-10-29).**

## Where the page lives

There is no top-level admin page and no custom REST endpoint. The settings UI is a Space Tools
entry:

| Property     | Value                                                   |
|--------------|---------------------------------------------------------|
| Web item key | `page-review-space-tools`                               |
| Section      | `system.space.tools/addons` (appears as **Apps**)       |
| Link         | `/plugins/page-review/settings.action?key=$space.key`   |
| Condition    | `SpacePermissionCondition` with `permission=administer` |
| i18n label   | `page-review.settings.menu`                             |

## Actions and URLs

Namespace `/plugins/page-review`, package extends `default`, default interceptor stack
`validatingStack`.

| Action     | Method       | URL                           | Results                            |
|------------|--------------|-------------------------------|------------------------------------|
| `settings` | `doDefault`  | `settings.action?key=<space>` | `input`, `success` → same template |
| `save`     | `doSave`     | `save.action` (POST)          | `input`, `success` → same template |
| `validate` | `doValidate` | `validate.action` (POST)      | `success` → same template          |

`doSave` and `doValidate` are annotated `@PermittedMethods(HttpMethod.POST)`; a GET returns 405.
The form posts to `save.action`; the **Check connection** button uses
`formaction="validate.action"`, so the same form data, hidden `key`, and XSRF token are sent to the
validation action.

## Template anatomy

`src/main/resources/templates/page-review/space-settings.vm`:

1. An HTML skeleton whose `<head>` contains a critical line:
   `<meta name="decorator" content="main"/>`.
2. A nested pair of `#applyDecorator("root")` calls:
    - outer: `context = space-administration`, `mode = view-space-administration`;
    - inner: `context = spaceadminpanel`, plus `selection`, `selectedSpaceToolsWebItem`,
      `selectedTab`, `helper`, and `title`.
3. An AUI form (`class="aui"`) with `#form_xsrfToken()`, a hidden `key`, status messages, three
   field groups, and two submit buttons.
4. Values are HTML-encoded with `$htmlUtil.htmlEncode`; field errors come from
   `$action.getFieldErrors()`.

### Why the meta decorator is required

Confluence maps `/plugins/*` to the `none` decorator in `WEB-INF/decorators.xml`. A plugin action
therefore receives no automatic page layout. The `<meta name="decorator" content="main"/>` tag is
read by SiteMesh's `PageDecoratorMapper` (it runs before the config mapper), which applies
`decorators/main.vmd`: the real `<html><head>`, the web resources (CSS/JS), the header, and the
sidebar. `main.vmd` also consumes the `<content tag="...">` blocks emitted by the space decorators.

Without the meta tag:

- no stylesheets are loaded and the page looks like raw HTML;
- `<content tag="...">` elements are treated as unknown elements, so their text leaks into the
  page (for example a trailing `Demonstration Space ds true` from the `spacename`, `spacekey`, and
  `spaceaction` tags).

### Tab selection

`SpaceToolsTabsHelper.setSelected` compares the value against `SpaceToolsTabItem.getWebItemKey()`.
Therefore `selection` and `selectedSpaceToolsWebItem` must be the **web-item key**
`page-review-space-tools`, not the link id `page-review-settings`. With the wrong value the
Space Tools tab is not highlighted.

## i18n

All user-facing text is in `page-review.properties` (English) and
`page-review_ru.properties` (Russian). Keep both files in sync.

- The template reads strings with `$action.getText(key)`.
- In Java, `getText(key, new String[]{arg})` performs `MessageFormat` substitution for `{0}`.
  Be careful: `ConfluenceActionSupport.getText(String, String)` is only a fallback helper — it
  does **not** substitute placeholders.
- Error messages are passed through `$htmlUtil.htmlEncode` before rendering.

## Form messages

The template shows:

- `saved` — a green message after a successful save (`page-review.settings.saved`);
- `checked` + `checkOk` — a green or red message after validation, with text from
  `checkMessage` (`page-review.settings.check.ok`, `page-review.settings.check.branch-missing`,
  `page-review.error.gitlab-*`).

Validation never saves; saving never calls GitLab.

## Live verification

With the dev instance running and the plugin enabled:

1. Open Space Tools → Apps → GitLab repository.
2. Inspect the served HTML (browser dev tools or `curl`):
    - the document starts with `<!DOCTYPE html>` and contains `<link rel="stylesheet">` entries;
    - it contains `formaction="validate.action"`;
    - it contains no literal `<content tag=` elements;
    - the Apps tab has the `aui-nav-selected` class.
3. Submit with an unreachable URL (for example `https://127.0.0.1:1/group/repo`), a valid branch,
   and any token: the page re-renders with a red "Cannot connect to GitLab" message.
4. A GET to `validate.action?key=<space>` returns HTTP 405.

## Sources of truth

- `src/main/resources/atlassian-plugin.xml`
- `src/main/resources/templates/page-review/space-settings.vm`
- `src/main/resources/page-review.properties`, `page-review_ru.properties`
- `src/main/java/ru/mts/confluence/web/SpaceSettingsAction.java`
- Confluence's `WEB-INF/decorators.xml`, `WEB-INF/sitemesh.xml`, `decorators/main.vmd`,
  `decorators/space-admin.vmd` inside the installed Confluence webapp (only needed when debugging
  rendering)

## How to verify

- `atlas-mvn test` (no test covers rendering; tests cover the action's inputs and the client).
- The live checklist above, after `atlas-mvn package` and QuickReload.
