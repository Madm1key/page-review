# Settings and secrets

> **Verified against code at commit `05bbcef` (2026-10-29).**

## Data model

`SpaceSettings` is an immutable value object with three fields:

| Field          | Meaning                                                 | Default when nothing is stored          |
|----------------|---------------------------------------------------------|-----------------------------------------|
| `repoUrl`      | HTTPS GitLab repository URL as entered by the admin     | `""` (empty string)                     |
| `targetBranch` | Branch to read/write in future features; validated here | `main` (`SpaceSettings.DEFAULT_BRANCH`) |
| `tokenSet`     | Whether a token ciphertext exists                       | `false`                                 |

The plaintext token is not part of the value object. The template only ever sees `tokenSet`.

## Storage keys

All keys live in SAL `PluginSettings` and share the prefix `ru.mts.confluence.page-review.`.

| Key               | Scope               | Stored value                   |
|-------------------|---------------------|--------------------------------|
| `...repoUrl`      | Per space           | Repository URL string          |
| `...targetBranch` | Per space           | Branch name string             |
| `...token`        | Per space           | Ciphertext (`v1:` format)      |
| `...cipherKey`    | Global (all spaces) | Base64-encoded 32-byte AES key |

Per-space settings are read with `settingsFactory.createSettingsForKey(spaceKey)`; the cipher key
uses `createGlobalSettings()`. Spaces are fully isolated except for the shared key.

`save()` also removes two keys from an earlier schema so they do not linger:
`...enabled` and `...baseFolder`.

## Token lifecycle

- **Save with a non-empty token** — `TokenCipher.encrypt` is called and the ciphertext replaces
  the stored value.
- **Save with a blank/null token** — the existing ciphertext is preserved. This is how "leave
  empty to keep the token" works; there is no way to delete a token through the form.
- **Read** — `get()` returns only `tokenSet`; `getToken(spaceKey)` is the only method that
  decrypts and returns plaintext. It returns `null` when nothing is stored.
- **Validation** — `doValidate` uses the token typed into the form if present, otherwise the
  stored one. The form value is trimmed for the check but is not persisted by validation.
- **Rotation** — enter a new token and save. There is no separate rotate operation.

## Encryption

`TokenCipher` implements:

- Algorithm: `AES/GCM/NoPadding`, 256-bit key.
- IV: 12 random bytes per encryption (`SecureRandom`).
- Tag: 128 bits.
- Serialized format: `"v1:" + Base64(IV ‖ ciphertext)`.

The 32-byte key is generated on first use and stored Base64-encoded in global `PluginSettings`.
If this global key is lost or replaced, every stored token becomes undecryptable; the plugin then
reports a generic error during validation and the admin must re-enter the token.

`getToken` throws a runtime exception when decryption fails (`IllegalStateException` for corrupt
ciphertext or a wrong key, `IllegalArgumentException` for an unknown format); `doValidate` catches
runtime failures while resolving the token and shows the generic error message.

## Security notes

- Never log, render, or document plaintext tokens. Code comments and docs must not contain real
  tokens.
- The view receives only `tokenSet`; do not add a getter that exposes the token to Velocity.
- The cipher key is global, so a key leak affects all spaces. It is not per-space and not
  rotatable through the UI.
- Settings are only editable by space administrators (see `ui-and-rendering.md`); the storage
  itself is not encrypted as a whole — only the token is.

## Sources of truth

- `src/main/java/ru/mts/confluence/settings/SpaceSettings.java`
- `src/main/java/ru/mts/confluence/settings/SpaceSettingsService.java`
- `src/main/java/ru/mts/confluence/settings/TokenCipher.java`

## How to verify

```bash
atlas-mvn test -Dtest=SpaceSettingsServiceTest,TokenCipherTest
```

`SpaceSettingsServiceTest` uses an in-memory `PluginSettingsFactory` and asserts, among other
things, that a blank token preserves the existing ciphertext and that spaces stay isolated.
`TokenCipherTest` covers the round trip and the serialized format.
