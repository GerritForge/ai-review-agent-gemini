ai-review-agent-gemini Configuration
====================================

The plugin configuration is stored in the `secret.config` file under the section `plugin.ai-review-agent-gemini`.

```ini
  [plugin "ai-review-agent-gemini"]
    password = <encryption-password>
    salt = <base64-encoded-salt>
```

plugin.ai-review-agent-gemini.password
:   The password used to symmetrically encrypt the Gemini tokens stored per-user.

plugin.ai-review-agent-gemini.salt
:   The Base64 encoded salt used in conjunction with the password for `PBEWithMD5AndDES` encryption of Gemini API tokens.

User Tokens
-----------

Tokens provided by users via the `/a/accounts/self/geminiToken` REST endpoint are stored securely within the user's `externalIds`
using the scheme `external:gemini_<accountId>_<token>`.
