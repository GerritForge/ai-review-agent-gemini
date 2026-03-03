# Gerrit AI Review Agent for Gemini

Implementation of the Gerrit's AI Code Review Agent API on top of Google's Gemini.

[Install](#install-in-gerrit) this plugin and enable the Gerrit AI chat to enjoy
a side-by-side collaboration with Gemini on the Change screen.

## License

This project is licensed under the **Business Source License 1.1** (BSL 1.1).
This is a "source-available" license that balances free, open-source-style access to the code
with temporary commercial restrictions.

* The full text of the BSL 1.1 is available in the [LICENSE](LICENSE) file in this
  repository.
* If your intended use case falls outside the **Additional Use Grant** and you require a
  commercial license, please contact [GerritForge Sales](https://gerritforge.com/contact).

## How to build

### Prerequisites

Gerrit v3.14 source code and Bazelisk 7.6.1 or later.

### Compile

```bash
git clone --recurse-submodules https://gerrit.googlesource.com/gerrit
git clone https://gerrit.googlesource.com/plugins/secure-config
git clone https://github.com/GerritForge/ai-review-agent-gemini

cd gerrit/plugins
ln -s ../../secure-config .
ln -s ../../ai-review-agent-gemini .
ln -s ai-review-agent-gemini/external_package.json package.json

cd ..
bazelisk build plugins/ai-review-agent-gemini
```

The build output is:

- `bazel-bin/plugins/ai-review-agent-gemini/ai-review-agent-gemini.jar`

### [Install in Gerrit](#install-in-gerrit)

Copy the built plugin into your Gerrit site (`$GERRIT_SITE`) plugins' directory:

```bash
cp ai-review-agent-gemini.jar "$GERRIT_SITE/plugins/"
```

The AI Review Agent Gemini plugin relies on the
[secure-config](https://gerrit.googlesource.com/plugins/secure-config/) for securing the
Gemini API token into the user's external ids.

The `secure-config.jar` needs to be installed into the (`$GERRIT_SITE`) lib' directory and
the master encryption parameters configured
[according to the secure-config configuration docs](https://gerrit.googlesource.com/plugins/secure-config/#customising-encryption-settings).

```bash
cp secure-config.jar "$GERRIT_SITE/lib/"
```
