# Glossary

> **Verified against code at commit `05bbcef` (2026-10-29).**

| Term                      | Meaning in this project                                                                                                                                   |
|---------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Space**                 | A Confluence content area. Settings are stored per space key (for example `DS`).                                                                          |
| **Space Tools**           | The space administration menu in Confluence. This plugin adds an entry under its **Apps** section.                                                        |
| **Web item**              | A Confluence plugin module that adds a link to a menu section. Here: `page-review-space-tools` in `system.space.tools/addons`.                            |
| **`linkId`**              | Identifier of a web item's link. Not the same as the web item key; the Space Tools tab helper compares web-item keys.                                     |
| **XWork / action**        | The server-side MVC framework Confluence uses for `.action` URLs. Actions are declared in `atlassian-plugin.xml`.                                         |
| **Velocity**              | The template language used by `space-settings.vm` to render the form.                                                                                     |
| **Decorator**             | A layout template applied by SiteMesh. `main` provides the full HTML document; `space` and `spaceadminpanel` add the space sidebar and Space Tools panel. |
| **SiteMesh**              | Confluence's page-decoration layer. `/plugins/*` is mapped to the `none` decorator unless the page requests one via `<meta name="decorator">`.            |
| **`<content tag="...">`** | A SiteMesh fragment emitted by a decorator and consumed by `main.vmd`. If decoration is missing, its text leaks into the page.                            |
| **`validatingStack`**     | Confluence's XWork interceptor stack used by this plugin's actions. Includes the XSRF interceptor.                                                        |
| **XSRF / `atl_token`**    | Confluence's cross-site request forgery token. `#form_xsrfToken()` renders the hidden field.                                                              |
| **OSGi plugin**           | The deployment unit for Confluence plugins. The key is `ru.mts.confluence.page-review`.                                                                   |
| **Spring Java config**    | `PageReviewJavaConfig` declares beans imported into the OSGi plugin context; `plugin-context.xml` scans the config package.                               |
| **SAL**                   | Atlassian's Shared Application Layer. `PluginSettings` is its key-value persistence API.                                                                  |
| **Global settings**       | `PluginSettingsFactory.createGlobalSettings()`; used only for the shared cipher key.                                                                      |
| **Cipher key**            | The 32-byte AES key stored globally under `ru.mts.confluence.page-review.cipherKey`. Losing it makes stored tokens undecryptable.                         |
| **Token**                 | GitLab personal/project/group access token with the `api` scope, stored encrypted per space and never shown again.                                        |
| **PAT**                   | Personal access token; one kind of GitLab token accepted here.                                                                                            |
| **Project path**          | `group[/subgroup...]/project`, the part of the repository URL after the host. URL-encoded for API calls (`/` → `%2F`).                                    |
| **API base**              | `https://host[:port]/api/v4`, derived by `GitLabRepo`.                                                                                                    |
| **`PRIVATE-TOKEN`**       | Header used to authenticate GitLab API requests.                                                                                                          |
| **QuickReload**           | AMPS feature (`enableQuickReload`) that lets a running `atlas-run` instance pick up a rebuilt plugin without a restart.                                   |
| **AUI**                   | Atlassian User Interface: the CSS classes (`aui-button`, `aui-message`, `field-group`, …) used in the template.                                           |
| **Check connection**      | The validation button/action. Checks project and branch access without saving.                                                                            |
| **i18n**                  | Internationalization through `page-review.properties` and `page-review_ru.properties`. Keep both in sync.                                                 |
