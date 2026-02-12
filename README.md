# Gerrit AI Review Agent for Gemini

Implementation of the Gerrit's AI Code Review Agent API on top of Google's Gemini.

Install this plugin to `$GERRIT_SITE/plugins` and enable the Gerrit AI chat to enjoy
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
git clone --recurse-submodules -b stable-3.11 https://gerrit.googlesource.com/gerrit
git clone https://github.com/GerritForge/ai-review-agent-gemini

cd gerrit/plugins
ln -s ../../ai-review-agent-gemini .
ln -s ai-review-agent-gemini/external_package.json package.json

cd ..
bazelisk build plugins/ai-review-agent-gemini
```

The build output is:

- `bazel-bin/plugins/ai-review-agent-gemini/ai-review-agent-gemini.jar`

### Install in Gerrit

Copy the built plugin JavaScript into your Gerrit site (`$GERRIT_SITE`) plugins' directory:

```bash
cp ai-review-agent-gemini.jar "$GERRIT_SITE/plugins/"
```

### Configure Gemini API key

From the browser DevTools console (while browsing Gerrit UI), set your Gemini API key:

```js
localStorage.setItem('GERRIT_GEMINI_API_KEY', 'YOUR_KEY_HERE')
```