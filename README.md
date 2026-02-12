# Gerrit AI Review Agent for Gemini

Implementation of the Gerrit's AI Code Review Agent API on top of Google's Gemini.

Install this plugin to `$GERRIT_SITE/plugins` and enable the Gerrit AI chat to enjoy
a side-by-side collaboration with Gemini on the Change screen.

## Compile and install

### Prerequisites

- Node.js 18+
- npm
- A running Gerrit site (`$SITE_PATH`)

### Compile

```bash
npm install
npm run build
```

The build output is:

- `dist/gerrit-gemini-ai.js`

### Install in Gerrit

Copy the built plugin JavaScript into your Gerrit site's `plugins` directory:

```bash
cp dist/gerrit-gemini-ai.js "$SITE_PATH/plugins/gerrit-gemini-ai.js"
```

### Configure Gemini API key

From the browser DevTools console (while browsing Gerrit UI), set your Gemini API key:

```js
localStorage.setItem('GERRIT_GEMINI_API_KEY', 'YOUR_KEY_HERE')
```

## License

This project is licensed under the **Business Source License 1.1** (BSL 1.1).
This is a "source-available" license that balances free, open-source-style access to the code
with temporary commercial restrictions.

* The full text of the BSL 1.1 is available in the [LICENSE](LICENSE) file in this
  repository.
* If your intended use case falls outside the **Additional Use Grant** and you require a
  commercial license, please contact [GerritForge Sales](https://gerritforge.com/contact).
